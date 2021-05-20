package cn.x5456.rs.mongo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileMode;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.x5456.infrastructure.util.FileChannelUtil;
import cn.x5456.rs.attachment.AttachmentProcessContainer;
import cn.x5456.rs.def.BigFileUploader;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.def.UploadProgress;
import cn.x5456.rs.entity.ResourceInfo;
import cn.x5456.rs.mongo.document.FsFileMetadata;
import cn.x5456.rs.mongo.document.FsFileTemp;
import cn.x5456.rs.mongo.document.FsResourceInfo;
import cn.x5456.rs.mongo.listener.event.AfterMetadataSaveEvent;
import com.google.common.annotations.Beta;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.mongodb.client.result.DeleteResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static cn.x5456.rs.constant.DataBufferConstant.DEFAULT_CHUNK_SIZE;

/**
 * @author yujx
 * @date 2021/04/26 09:20
 */
@Slf4j
public class MongoResourceStorage implements IResourceStorage {

    /**
     * 文件锁的后缀
     */
    private static final String LOCK_SUFFIX = ".lock";

    /**
     * 缓存的后缀
     * <p>
     * download 方法下载时的临时文件，为什么不直接下载到 official 文件中呢？
     * 为了防止下载到一半服务器"停电"，再次启动的时候把没下完的文件当做正式文件
     */
    private static final String TMP_SUFFIX = ".tmp";

    /**
     * 正式的后缀
     * <p>
     * 可以下载的正式文件，创建正式文件的时候必须获取文件锁 {@link MongoResourceStorage#LOCK_SUFFIX}
     */
    private static final String OFFICIAL_SUFFIX = ".official";

    /**
     * 临时文件的后缀
     * <p>
     * 通过 DataBufferFlux 上传的时候临时保存的文件后缀
     */
    private static final String ENDURANCE_SUFFIX = ".endurance";

    /**
     * 大文件上传时碎片持久化的后缀
     * <p>
     * 为了降低 {@link BigFileUploaderImpl#endurance(java.lang.String)} 与 mongo 的交互次数，能利用缓存就是用缓存，
     * 所以在第一次上传完碎片文件之后，将其后缀改为 .chunk，以便后续 {@link BigFileUploaderImpl#endurance(java.lang.String)} 方法使用
     */
    private static final String CHUNK_SUFFIX = ".chunk";

    private static final String LOCAL_TEMP_PATH;

    static {
        LOCAL_TEMP_PATH = System.getProperty("java.io.tmpdir") + File.separator + "cn.x5456.rs" + File.separator;
        FileUtil.mkdir(LOCAL_TEMP_PATH);

        // 每次重启清理未"转正"的文件（包括 LOCK_SUFFIX、TMP_SUFFIX 和 ENDURANCE_SUFFIX）
        File[] ls = FileUtil.ls(LOCAL_TEMP_PATH);
        for (File file : ls) {
            if (file.isFile()) {
                String suffix = "." + FileUtil.getSuffix(file);
                if (suffix.equals(OFFICIAL_SUFFIX)) {
                    continue;
                }
            }
            FileUtil.del(file);
        }
    }

    /**
     * 构建一个 LRU 缓存实例
     */
    private final Cache<String, FsFileMetadata> cache = CacheBuilder.newBuilder()
            .maximumSize(100) // 设置缓存的最大容量
            .concurrencyLevel(10) // 设置并发级别为 10
            .recordStats() // 开启缓存统计
            .build();

    private final BigFileUploader INSTANCE = new BigFileUploaderImpl();

    private final DataBufferFactory dataBufferFactory;

    private final ReactiveMongoTemplate mongoTemplate;

    private final ReactiveGridFsTemplate gridFsTemplate;

    private final Scheduler scheduler;

    private final ApplicationEventPublisher eventPublisher;

    public MongoResourceStorage(DataBufferFactory dataBufferFactory, ReactiveMongoTemplate mongoTemplate,
                                ReactiveGridFsTemplate gridFsTemplate, ApplicationEventPublisher eventPublisher,
                                ObjectProvider<Scheduler> schedulerObjectProvider) {
        this.dataBufferFactory = dataBufferFactory;
        this.mongoTemplate = mongoTemplate;
        this.gridFsTemplate = gridFsTemplate;
        this.eventPublisher = eventPublisher;
        this.scheduler = schedulerObjectProvider.getIfUnique(Schedulers::elastic);

        // 启动清理本地文件缓存策略
        new CleanLocalFileCache().start();
    }

    /**
     * 上传文件到文件服务
     *
     * @param localFilePath 本地文件路径
     * @param path          服务上存储的标识
     * @return 是否上传成功
     */
    @Override
    public Mono<ResourceInfo> uploadFile(String localFilePath, String path) {
        String fileName = FileUtil.getName(localFilePath);
        return this.uploadFile(localFilePath, fileName, path);
    }

    /**
     * 上传文件到文件服务
     * <p>
     * 要是上传了一半线程挂了，或者服务器挂了怎么办，状态已经不会改变了但他又在那占着坑
     * 策略：
     * 1. 定时任务监测 metadata 和 temp 表，当其超过 mongo 连接超时时间 * 2 的时候，则记录日志并删除
     * 2. redis 过期键提醒，可以检测是否引入 redis，如果引入默认用这个。
     *
     * @param localFilePath 本地文件路径
     * @param fileName      文件名
     * @param path          服务上存储的标识
     * @return 是否上传成功
     */
    @Override
    public Mono<ResourceInfo> uploadFile(String localFilePath, String fileName, String path) {
        return this.doUploadFile(localFilePath, fileName, path, false);
    }

    /**
     * dataBufferFlux 方式上传文件到文件服务
     *
     * @param dataBufferFlux dataBufferFlux
     * @param fileName       文件名
     * @param path           服务上存储的标识
     * @return 是否上传成功
     */
    @Override
    public Mono<ResourceInfo> uploadFile(Flux<DataBuffer> dataBufferFlux, String fileName, String path) {
        // 2021/5/8 如果这个 endurancePath 是我们创建的可以进行 mv，如果是用户提供的则 cp 到缓存文件夹，如果时间长不使用交给自动清理工具
        // 注：这个路径是随机的，每次上传都会创建一个这样的临时文件
        // TODO: 2021/5/8 自动清理工具可以获取系统的磁盘使用情况，当超过阈值（可以设置）再进行清理
        String endurancePath = LOCAL_TEMP_PATH + IdUtil.fastUUID() + ENDURANCE_SUFFIX;
        // TODO: 2021/5/19 在写入文件的时候计算出 hash 值，mmp 这个工具类没给流钩子做不了，那就整好自己重写一个扩展吧，用 MapperByteBuffer
        return DataBufferUtils.write(dataBufferFlux, Paths.get(endurancePath), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                .then(this.doUploadFile(endurancePath, fileName, path, true));
    }

    /**
     * dataBufferFlux 方式上传文件到文件服务
     *
     * @param endurancePath 本地持久化的路径
     * @param fileName      文件名
     * @param path          服务上存储的标识
     * @param mv            是否需要删除持久化的缓存文件
     * @return 是否上传成功
     */
    @NotNull
    private Mono<ResourceInfo> doUploadFile(String endurancePath, String fileName, String path, boolean mv) {
        Flux<DataBuffer> dataBufferFlux = DataBufferUtils.read(new FileSystemResource(endurancePath), dataBufferFactory, DEFAULT_CHUNK_SIZE);
        // 是否是新上传的标识
        AtomicBoolean upload = new AtomicBoolean(false);
        return this.calcFileHashCode(dataBufferFlux)
                .flatMap(fileHash -> this.getReadyMetadata(fileHash)
                        .switchIfEmpty(this.doUploadFile(fileHash, dataBufferFlux)
                                // 如果是新上传的则复制到本地缓存文件夹
                                .doOnSuccess(metadata -> {

                                    log.info("文件「{}」上传成功！", fileName);

                                    // 上传完成后设置为 true
                                    upload.set(true);

                                    String officialPath = this.getFileOfficialPath(fileHash);
                                    String lockPath = this.getLockPath(fileHash);
                                    // 检查 officialPath 是否存在
                                    if (FileUtil.exist(officialPath)) {
                                        return;
                                    }

                                    // 尝试获取锁，
                                    // 注：这个地方进行了线程切换，从 nio 线程切换到了 elastic 线程，从而导致 mv 和 delete 操作在不同的线程中执行
                                    // 而且确实这个地方不能在 nio 线程做，因为 mv、cp、发布事件这些是阻塞的，最好不要再 nio 线程执行。
                                    this.tryLock(lockPath).subscribe(lockFile -> {
                                        try {
                                            // 获取到锁，再次检查 officialPath 是否存在，如果不存在再进行文件的下载工作
                                            if (FileUtil.exist(officialPath)) {
                                                return;
                                            }
                                            if (mv) {
                                                FileUtil.move(Paths.get(endurancePath), Paths.get(officialPath), true);
                                            } else {
                                                FileUtil.copyFile(endurancePath, officialPath, StandardCopyOption.REPLACE_EXISTING);
                                            }
                                            cache.put(officialPath, metadata);

                                            // 发布事件
                                            eventPublisher.publishEvent(new AfterMetadataSaveEvent(metadata, fileName));

                                        } finally {
                                            // 关闭锁文件和文件锁
                                            IoUtil.close(lockFile);
                                            // 删除锁文件
                                            FileUtil.del(lockPath);
                                        }
                                    });
                                })
                        )
                        .flatMap(m -> this.insertResource(m, fileName, path))
                        // 当文件引用建立成功之后删除缓存文件
                        .doOnTerminate(() -> {
                            if (mv && !upload.get()) {
                                FileUtil.del(endurancePath);
                            }
                        })
                );
    }

    @NotNull
    private Mono<String> calcFileHashCode(Flux<DataBuffer> dataBufferFlux) {
        return Mono.create(sink -> {
            // 计算文件的 hash 值
            // main
            MessageDigest digest = SecureUtil.createMessageDigest(DigestAlgorithm.SHA256.getValue());
            // data buffer thread
            dataBufferFlux
                    .doOnNext(dataBuffer -> digest.update(dataBuffer.asByteBuffer()))
                    .doOnComplete(() -> {
                        byte[] data = digest.digest();
                        String hex = HexUtil.encodeHexStr(data);
                        log.info("计算出的 hash 值为：「{}」", hex);
                        sink.success(hex);
                    }).subscribe();
        });
    }

    @NotNull
    private Mono<FsResourceInfo> insertResource(FsFileMetadata metadata, String fileName, String path) {
        FsResourceInfo fsResourceInfo = new FsResourceInfo();
        fsResourceInfo.setId(path);
        fsResourceInfo.setFileName(fileName);
        fsResourceInfo.setFileHash(metadata.getFileHash());
        fsResourceInfo.setMetadataId(metadata.getId());

        return mongoTemplate.insert(fsResourceInfo);
    }

    @NotNull
    private Mono<FsFileMetadata> getFileMetadata(String fileHash) {
        return mongoTemplate.findOne(Query.query(Criteria.where(FsFileMetadata.FILE_HASH).is(fileHash)), FsFileMetadata.class);
    }

    @NotNull
    private Mono<FsFileMetadata> doUploadFile(String fileHash, Flux<DataBuffer> dataBufferFlux) {
        return Mono.create(sink -> {
            // 尝试保存文件元数据信息
            this.insertFileMetadata(fileHash, false)
                    // 如果保存失败，则证明数据库中已经有了 hash 值为 fileHash 的数据，那么取出并返回
                    .doOnError(DuplicateKeyException.class, ex -> this.getReadyMetadata(fileHash).subscribe(sink::success))
                    // 如果保存成功，则上传文件，完善文件元数据信息
                    .flatMap(m -> gridFsTemplate.store(dataBufferFlux, fileHash)
                            .doOnError(ex -> {
                                // 如果上传时遇到错误，则删除当前上传文件的元数据，留给下一次上传
                                mongoTemplate.remove(m).subscribe();
                                sink.error(ex);
                            })
                            .flatMap(objectId -> {
                                m.setUploadProgress(UploadProgress.UPLOAD_COMPLETED);
                                m.setTotalNumberOfChunks(1);
                                m.setFilesInfoList(Lists.newArrayList(
                                        new FsFileMetadata.FsFilesInfo(0, objectId.toHexString(), this.getChunkSize(dataBufferFlux))));
                                return mongoTemplate.save(m);
                            })).subscribe(sink::success);
        });
    }

    @NotNull
    private Mono<FsFileMetadata> insertFileMetadata(String fileHash, boolean multipartUpload) throws DuplicateKeyException {
        // main
        FsFileMetadata fileMetadata = new FsFileMetadata();
        fileMetadata.setId(fileHash);
        fileMetadata.setFileHash(fileHash);
        fileMetadata.setUploadProgress(UploadProgress.UPLOADING);
        fileMetadata.setCreatTime(LocalDateTime.now());
        fileMetadata.setMultipartUpload(multipartUpload);

        // reactive mongo thread
        // 尝试保存文件元数据信息
        return mongoTemplate.insert(fileMetadata);
    }


    private Long getChunkSize(Flux<DataBuffer> dataBufferFlux) {
        return dataBufferFlux.reduce(0L, (a, b) -> a += b.readableByteCount()).block();
    }

    /**
     * 从文件服务下载文件
     *
     * @param localFilePath 本地文件路径
     * @param path          服务上存储的标识
     * @return 是否下载成功
     */
    @Override
    public Mono<Boolean> downloadFile(String localFilePath, String path) {
        return this.getResourceInfo(path)
                .switchIfEmpty(Mono.error(new RuntimeException(StrUtil.format("输入的 path：「{}」不正确！", path))))
                .flatMap(r -> {
                    String fileHash = r.getFileHash();
                    return this.downloadFileByFileHash(fileHash).map(srcPath -> {
                        FileUtil.copyFile(srcPath, localFilePath, StandardCopyOption.REPLACE_EXISTING);
                        return true;
                    });
                });
    }

    /**
     * 根据文件 hash 从文件服务中获取文件
     *
     * @param fileHash 文件 hash
     * @return 文件在本地的缓存路径
     */
    @Override
    public Mono<String> downloadFileByFileHash(String fileHash) {
        return Mono.create(sink -> {
            // 先从缓存中获取，节省一次查询
            FsFileMetadata metadata = cache.getIfPresent(fileHash);
            if (Objects.nonNull(metadata)) {
                // 拼接本地缓存路径，格式：缓存目录/hashcode.official
                String officialPath = this.getFileOfficialPath(metadata.getFileHash());
                if (FileUtil.exist(officialPath)) {
                    sink.success(officialPath);
                    return;
                }
            }

            this.getReadyMetadata(fileHash)
                    .subscribe(m -> {
                        // 拼接本地缓存路径，格式：缓存目录/hashcode.official
                        String officialPath = this.getFileOfficialPath(m.getFileHash());
                        if (FileUtil.exist(officialPath)) {
                            sink.success(officialPath);
                        } else {
                            // 2021/4/28 why????? 为啥要新开一个线程（这块是个死锁）
                            // 假设当前代码运行的线程为 N2-2，我们进入 doDownload() 方法，里面有一个循环，也是使用 N2-2 线程发送两个请求
                            // （应该是做了判断，判断当前线程是不是 EventLoopGroup 中的线程，如果不是才会进行线程的切换），可能 mongo 内部
                            // 有一个机制就是请求线程与接收线程绑定，即第一个请求用 N2-2 接收，第二个请求用 N2-3 接收，因为我们 for 循环之后
                            // 调用了 latch.await(); 将 N2-2 阻塞住了，所以当消息来了之后 N2-2 无法接收，所以程序一直无法停止。
                            // 所以，我们不能让 nio 线程阻塞，那就需要在调用时重新创建一个线程了。
                            this.doDownload(m).subscribe(sink::success);
                        }
                    });
        });
    }

    private String getFileOfficialPath(String fileHash) {
        return LOCAL_TEMP_PATH + fileHash + OFFICIAL_SUFFIX;
    }

    // 2021/4/30 这个方法可以改成 repeatWhenEmpty 多重试几次要是还不行那就抛异常，让他等会
    @NotNull
    private Mono<FsFileMetadata> getReadyMetadata(String fileHash) {
        RetryBackoffSpec retryBackoffSpec = Retry.backoff(5, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(2));
        return this.getFileMetadata(fileHash)
                .flatMap(metadata -> {
                    if (Objects.equals(metadata.getUploadProgress(), UploadProgress.UPLOAD_COMPLETED)) {
                        return Mono.just(metadata);
                    }
                    return Mono.error(new RuntimeException("文件上传中，请稍后再试！"));
                })
                .retryWhen(retryBackoffSpec)
                .onErrorResume((ex) -> Mono.error(ex.getCause()));
    }

    @NotNull
    private Mono<String> doDownload(FsFileMetadata metadata) {

        String fileHash = metadata.getFileHash();
        String fileTempPath = this.getFileTempPath(fileHash);
        String officialPath = this.getFileOfficialPath(fileHash);
        String lockPath = this.getLockPath(fileHash);

        return this.merge(metadata, fileTempPath, officialPath, lockPath,
                (randomAccessFile, filesInfo) -> this.doDownloadChunk(filesInfo, randomAccessFile).then())
                // 将线程从 nio 线程切换到 scheduler
                .subscribeOn(scheduler);
    }

    @NotNull
    private String getFileTempPath(String fileHash) {
        return LOCAL_TEMP_PATH + fileHash + TMP_SUFFIX;
    }

    /**
     * 注：调用这个方法不能使用 nio 线程（无所谓了，tryLock 方法会进行线程的切换的）
     */
    @NotNull
    private Mono<String> merge(FsFileMetadata metadata, String tempPath, String officialPath, String lockPath,
                               BiFunction<RandomAccessFile, FsFileMetadata.FsFilesInfo, Mono<Void>> function) {
        return Mono.create(sink -> {
            /*
             双重检查锁（文件锁）

             0. while(true) 尝试加锁
             1. 检查 official 文件是否存在
             2. 如果不存在尝试对 lock 文件加锁
             3. 如果加锁成功，则执行接下来的逻辑，
                 逻辑执行成功则文件名"转正"并释放锁
                 执行失败，则删除 tmp 文件并释放锁
             4. 如果加锁失败，则 while(true) 等待
             5. 等待之后获取成功，检查 official 文件是否存在，回到 1

             注：如果下载过程中挂了，再次重启时会清理文件夹，不用考虑
             */

            // 检查 officialPath 是否存在
            if (FileUtil.exist(officialPath)) {
                sink.success(officialPath);
                return;
            }

            // 尝试获取锁
            this.tryLock(lockPath).subscribe(lockFile -> {
                try {
                    // 获取到锁，再次检查 officialPath 是否存在，如果不存在再进行文件的下载工作
                    if (FileUtil.exist(officialPath)) {
                        sink.success(officialPath);
                        return;
                    }

                    // 开始下载，获取每一片的信息，排序
                    List<FsFileMetadata.FsFilesInfo> fsFilesInfoList = metadata.getFilesInfoList();
                    fsFilesInfoList.sort(Comparator.comparingInt(FsFileMetadata.FsFilesInfo::getChunk));

                    long index = 0;
                    CountDownLatch latch = new CountDownLatch(fsFilesInfoList.size());
                    AtomicBoolean isSuccess = new AtomicBoolean(true);
                    try {
                        // 下载每一片文件到 tempPath
                        for (FsFileMetadata.FsFilesInfo fsFilesInfo : fsFilesInfoList) {
                            RandomAccessFile randomAccessFile = new RandomAccessFile(tempPath, "rw");
                            randomAccessFile.seek(index);
                            index += fsFilesInfo.getChunkSize();

                            function.apply(randomAccessFile, fsFilesInfo)
                                    .onErrorResume(ex -> {
                                        isSuccess.set(false);
                                        sink.error(ex);
                                        return Mono.empty();
                                    }) // TODO: 2021/4/30 没测试，不知道好不好使
                                    .doOnTerminate(() -> {
                                        try {
                                            randomAccessFile.close();
                                        } catch (IOException e) {
                                            sink.error(e);
                                        } finally {
                                            latch.countDown();
                                        }
                                    })
                                    .subscribe();
                        }

                        // 阻塞住，等待全部线程下载完成
                        log.info("latch：「{}」", latch);
                        latch.await();

                        if (isSuccess.get()) {
                            // 文件更名（"转正"）
                            FileUtil.move(Paths.get(tempPath), Paths.get(officialPath), true);
                            log.info("文件「{}」下载完成！", officialPath);
                            // 写入 LCU 缓存
                            cache.put(officialPath, metadata);
                            sink.success(officialPath);
                        } else {
                            // 下载失败，删除缓存文件
                            FileUtil.del(tempPath);
                        }
                    } catch (Exception e) {
                        sink.error(e);
                    }
                } finally {
                    // 关闭锁文件和文件锁
                    IoUtil.close(lockFile);
                    // 删除锁文件
                    FileUtil.del(lockPath);
                }
            });
        });
    }

    @NotNull
    private String getLockPath(String fileHash) {
        return LOCAL_TEMP_PATH + fileHash + LOCK_SUFFIX;
    }

    @NotNull
    private Mono<RandomAccessFile> tryLock(String lockPath) {
        return Mono.create(sink -> {
            RandomAccessFile lockFile = FileUtil.createRandomAccessFile(new File(lockPath), FileMode.rw);

            Disposable disposable = scheduler.schedulePeriodically(() -> {
                try {
                    // 尝试获取锁
                    FileLock lock = lockFile.getChannel().tryLock();
                    log.info("文件锁获取成功：「{}」，lock：「{}」", lockPath, lock);
                    // 获取成功则返回 lockFile，关闭 lockFile 的时候会自动释放锁
                    sink.success(lockFile);
                } catch (OverlappingFileLockException | IOException ex) {
                    log.info("当前线程文件锁获取失败，等待重试，异常为「{}」，报错信息为：「{}」", ex.getClass().getSimpleName(), ex.getMessage());
                }
            }, 0, 1, TimeUnit.SECONDS);
            // 当结束的时候关闭定时任务
            sink.onDispose(disposable);
        });
    }

    @NotNull
    private Flux<DataBuffer> doDownloadChunk(FsFileMetadata.FsFilesInfo fsFilesInfo, RandomAccessFile randomAccessFile) {
        return gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fsFilesInfo.getFilesId())))
                .flatMap(gridFsTemplate::getResource)
                .map(ReactiveGridFsResource::getDownloadStream)
                .flux()
                .log()
                .flatMap(dataBufferFlux -> DataBufferUtils.write(dataBufferFlux, randomAccessFile.getChannel()));
    }

    /**
     * 从文件服务中获取文件
     *
     * @param path 服务上存储的标识
     * @return Pair key-文件名，value-dataBufferFlux
     */
    @Override
    public Mono<Pair<String, Flux<DataBuffer>>> downloadFileDataBuffer(String path) {
        return this.getResourceInfo(path)
                .switchIfEmpty(Mono.error(new RuntimeException(StrUtil.format("输入的 path：「{}」不正确！", path))))
                .flatMap(r -> {
                    String fileHash = r.getFileHash();
                    String fileName = r.getFileName();

                    return this.downloadFileByFileHash(fileHash).map(localFilePath -> {
                        Flux<DataBuffer> read = DataBufferUtils.read(new FileSystemResource(localFilePath), dataBufferFactory, DEFAULT_CHUNK_SIZE);
                        return new Pair<>(fileName, read);
                    });
                });
    }

    @NotNull
    private Mono<FsResourceInfo> getResourceInfo(String path) {
        return mongoTemplate.findOne(Query.query(Criteria.where(FsResourceInfo.PATH).is(path)), FsResourceInfo.class);
    }

    /**
     * 从文件服务中获取文件
     *
     * @param path 服务上存储的标识
     * @return Pair key-文件名，value-文件在本地的缓存路径
     */
    @Override
    public Mono<Pair<String, String>> downloadFile(String path) {
        return this.getResourceInfo(path)
                .switchIfEmpty(Mono.error(new RuntimeException(StrUtil.format("输入的 path：「{}」不正确！", path))))
                .flatMap(r -> {
                    String fileHash = r.getFileHash();
                    String fileName = r.getFileName();
                    return this.downloadFileByFileHash(fileHash).map(localFilePath -> new Pair<>(fileName, localFilePath));
                });
    }

    /**
     * 删除文件服务上的文件
     *
     * @param path 服务上存储的标识
     * @return 是否删除成功
     */
    @Override
    public Mono<Boolean> deleteFile(String path) {
        return mongoTemplate.remove(this.getResourceInfo(path)).map(DeleteResult::wasAcknowledged)
                .switchIfEmpty(Mono.error(new RuntimeException("输入的 path 不存在！")));
    }

    /**
     * 通过path获取文件名
     *
     * @param path 服务上存储的标识
     * @return 文件名【包括后缀】
     */
    @Override
    public Mono<String> getFileName(String path) {
        return this.getResourceInfo(path).map(FsResourceInfo::getFileName);
    }

    /**
     * 通过path获取文件的 hash
     *
     * @param path 服务上存储的标识
     * @return 文件 hash
     */
    @Override
    public Mono<String> getFileHashByPath(String path) {
        return this.getResourceInfo(path)
                .switchIfEmpty(Mono.error(new RuntimeException(StrUtil.format("输入的 path：「{}」不正确！", path))))
                .map(FsResourceInfo::getFileHash);
    }

    @Override
    public BigFileUploader getBigFileUploader() {
        return INSTANCE;
    }

    /**
     * 获取附件信息
     * <p>
     * 1. 第一次添加 metadata 的时候新开一个线程进行处理获取结果
     * 2. 第一次调用的时候，检查是否已经存在，如果不存在则处理获取结果再保存
     *
     * @param path   服务上存储的标识
     * @param key    附件信息key
     * @param tClass 需要转换的类型
     * @return 附件信息
     */
    @Override
    public <T> Mono<T> getAttachment(String path, String key, Class<T> tClass) {
        /*
        1. 先查询元数据表，查看是否已经解析过了
        2. 如果没解析就从 container 获取 process 调用获取结果（把结果强转为 T 类型）
        3. add 到 metadata 的 attachment 中，save
        4. return
         */
        // 获取文件信息
        return this.getResourceInfo(path)
                .switchIfEmpty(Mono.error(new RuntimeException(StrUtil.format("输入的 path：「{}」不正确！", path))))
                .flatMap(fsResourceInfo -> this.getAttachmentByHash(fsResourceInfo.getFileHash(), key, tClass));
    }

    /**
     * 获取附件信息
     *
     * @param fileHash 文件 hash
     * @param key      附件信息key
     * @param tClass   需要转换的类型
     * @return 附件信息
     */
    @Override
    public <T> Mono<T> getAttachmentByHash(String fileHash, String key, Class<T> tClass) {
        return this.getReadyMetadata(fileHash)
                .switchIfEmpty(Mono.error(new RuntimeException("传入的文件 hash 还未上传！")))
                .publishOn(scheduler)   // 下面的操作 AttachmentProcessContainer#getProcess 是阻塞的，所以要切换到普通线程
                .flatMap(fsFileMetadata -> {
                    // 判断是否已经存在
                    Object o = fsFileMetadata.getAttachments().get(key);
                    if (o != null) {
                        return Mono.just((T) o);
                    }

                    // 解析
                    if (AttachmentProcessContainer.getProcess(key) == null) {
                        return Mono.error(new RuntimeException("未找到传入 key 的处理器，请添加！"));
                    }
                    Object fileNode = AttachmentProcessContainer.getProcess(key).process(fsFileMetadata);
                    if (fileNode != null) {
                        return Mono.just((T) fileNode);
                    }

                    // 否则可能是解析出来的就是 null 直接返回 Mono.empty()
                    return Mono.empty();
                });
    }

    /**
     * 清除本地所有缓存
     */
    @Beta
    @Override
    public void cleanLocalTemp() {
        FileUtil.del(LOCAL_TEMP_PATH);
        FileUtil.mkdir(LOCAL_TEMP_PATH);
    }

    /**
     * 删库
     */
    @Beta
    @Override
    public void dropMongoDatabase() {
        mongoTemplate.dropCollection("fs.chunks").subscribe();
        mongoTemplate.dropCollection("fs.files").subscribe();
        mongoTemplate.dropCollection("fs.metadata").subscribe();
        mongoTemplate.dropCollection("fs.resource").subscribe();
        mongoTemplate.dropCollection("fs.temp").subscribe();
    }

    class BigFileUploaderImpl implements BigFileUploader {

        /**
         * 是否已经存在（秒传）
         *
         * @param fileHash 文件 hash
         * @return 文件是否已在服务中存在
         */
        @Override
        public Mono<Boolean> isExist(String fileHash) {
            return MongoResourceStorage.this.getFileMetadata(fileHash).map(m -> true)
                    // 查不到数据时返回的是一个空的 Mono，并不会调用 map()，所以需要使用 defaultIfEmpty 返回值
                    .defaultIfEmpty(false);
        }

        /**
         * 大文件秒传
         *
         * @param fileHash 文件 hash
         * @param fileName 文件名
         * @param path     服务上存储的标识
         * @return 是否上传成功
         */
        @Override
        public Mono<ResourceInfo> secondPass(String fileHash, String fileName, String path) {
            return MongoResourceStorage.this.getReadyMetadata(fileHash)
                    .switchIfEmpty(Mono.error(new RuntimeException("传入的文件 hash 不存在！")))
                    .flatMap(m -> MongoResourceStorage.this.insertResource(m, fileName, path));
        }

        /**
         * 上传每一片文件
         *
         * @param fileHash       文件 hash
         * @param chunk          第几片
         * @param dataBufferFlux 当前片的 dataBufferFlux
         * @return 是否上传成功，上传成功返回 true，当发现已经有其他线程上传过了返回 false
         * @throws RuntimeException 上传文件时失败
         */
        @Override
        public Mono<Boolean> uploadFileChunk(String fileHash, int chunk, Flux<DataBuffer> dataBufferFlux) {
            // 拼接本地缓存路径
            // 注：这个路径每次都是随机生成的
            String endurancePath = LOCAL_TEMP_PATH + fileHash + File.separator + IdUtil.fastUUID() + ENDURANCE_SUFFIX;
            FileUtil.touch(endurancePath);
            return DataBufferUtils.write(dataBufferFlux, Paths.get(endurancePath), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                    .then(this.doUploadFileChunk(fileHash, chunk, endurancePath));
        }

        @NotNull
        private Mono<Boolean> doUploadFileChunk(String fileHash, int chunk, String endurancePath) {

            /*
            1. 检查文件 hash 是否存在
            2. 如果不存在，则在 fs.metadata 表创建一个
            3. 如果创建失败，则证明存在，则从 fs.metadata 中取出

            4. 去 fs.temp 表创建当前分片的缓存
            5. 如果创建成功，则执行上传逻辑，保存到 fs.temp 表中，返回 true
            6. 如果创建失败，则证明已经有了一个线程抢先上传了，返回 false
             */
            Flux<DataBuffer> dataBufferFlux = DataBufferUtils.read(new FileSystemResource(endurancePath), dataBufferFactory, DEFAULT_CHUNK_SIZE);
            return MongoResourceStorage.this.getFileMetadata(fileHash)
                    .switchIfEmpty(this.createOrGet(fileHash))
                    .flatMap(metadata -> this.insertChunkTempInfoV2(fileHash, chunk))
                    .flatMap(temp -> gridFsTemplate.store(dataBufferFlux, fileHash + "_" + chunk)
                            .doOnError(ex -> {
                                // 上传失败，删除缓存表信息
                                mongoTemplate.remove(temp).subscribe();
                            })
                            .flatMap(objectId -> {
                                temp.setFilesId(objectId.toHexString());
                                temp.setUploadProgress(UploadProgress.UPLOAD_COMPLETED);
                                temp.setChunkSize(MongoResourceStorage.this.getChunkSize(dataBufferFlux));
                                return mongoTemplate.save(temp);
                            })
                            .doOnSuccess(t -> {
                                // 第一次上传碎片成功的时候将缓存文件转存到指定目录下
                                String targetPath = LOCAL_TEMP_PATH + fileHash + File.separator + chunk + CHUNK_SUFFIX;
                                FileUtil.move(Paths.get(endurancePath), Paths.get(targetPath), true);
                            })
                    )
                    .map(fsFileTemp -> true)
                    /*
                    当遇到 DuplicateKeyException 异常时说明已经有一个线程在上传了，所以停止上传，返回 false
                    当流为空的时候，说明被布隆过滤器过滤掉了，所以也返回 false
                     */
                    .onErrorReturn(DuplicateKeyException.class, false)
                    .defaultIfEmpty(false)
                    .doOnTerminate(() -> FileUtil.del(endurancePath));
        }

        private Mono<FsFileMetadata> createOrGet(String fileHash) {
            // 尝试保存文件元数据信息
            return MongoResourceStorage.this.insertFileMetadata(fileHash, true)
                    // 如果保存失败，则证明数据库中已经有了 hash 值为 fileHash 的数据，那么取出并返回
                    .onErrorResume(DuplicateKeyException.class, ex -> MongoResourceStorage.this.getFileMetadata(fileHash));
        }

        // 方案二：为他计算出一个唯一的 id(hash 算法) + 把 save 换成 insert
        // 2021/4/29 小想法，把 id 设置成一样的呢？ 会不会把 id 的索引删了 -> 测试结果，不会
        // TODO: 2021/5/6 id 和唯一索引有啥区别
        private Mono<FsFileTemp> insertChunkTempInfoV2(String fileHash, int chunk) throws DuplicateKeyException {
            String key = fileHash + "_" + chunk;

            // 尝试添加一条记录
            FsFileTemp fsFileTemp = new FsFileTemp();
            fsFileTemp.setId(SecureUtil.sha256(key));
            fsFileTemp.setFileHash(fileHash);
            fsFileTemp.setChunk(chunk);
            fsFileTemp.setUploadProgress(UploadProgress.UPLOADING);
            fsFileTemp.setCreatTime(LocalDateTime.now());
            return mongoTemplate.insert(fsFileTemp);
        }

        /**
         * 获取上传进度 {1: 上传完成， 0: 上传中，2：失败}
         *
         * @param fileHash 文件 hash
         * @return 上传进度
         */
        @Override
        public Flux<Pair<Integer, UploadProgress>> uploadProgress(String fileHash) {
            Criteria criteria = Criteria.where(FsFileTemp.FILE_HASH).is(fileHash);
            return mongoTemplate.find(Query.query(criteria), FsFileTemp.class)
                    .map(temp -> new Pair<>(temp.getChunk(), temp.getUploadProgress()));
        }

        /**
         * 全部上传完成，该接口具有幂等性
         *
         * @param fileHash            文件 hash
         * @param fileName            文件名
         * @param totalNumberOfChunks 当前文件一共多少片
         * @param path                服务上存储的标识
         * @return 操作是否成功
         */
        @Override
        public Mono<ResourceInfo> uploadCompleted(String fileHash, String fileName, int totalNumberOfChunks, String path) {
            /*
            1. 校验传入的片数是否与系统中的片数相同
            2. 根据 fileHash 查出元数据信息，完善元数据信息
            3. 在 fs.resource 添加引用
            4. 删除缓存表数据
             */


            // 查询上传成功的
            Criteria criteria = Criteria.where(FsFileTemp.FILE_HASH).is(fileHash)
                    .and(FsFileTemp.UPLOAD_PROGRESS).is(UploadProgress.UPLOAD_COMPLETED);
            Query query = Query.query(criteria);

            return mongoTemplate.find(query, FsFileTemp.class)
                    .switchIfEmpty(Mono.error(new RuntimeException("输入的 fileHash 有误或者已经上传完成！")))
                    .collectList()
                    .flatMap(tempList -> {
                                if (tempList.size() != totalNumberOfChunks) {
                                    return Mono.error(new RuntimeException("传入的片数与服务器中的片数不符，请检查或稍后再试！"));
                                }

                                return MongoResourceStorage.this.getFileMetadata(fileHash)
                                        // 根据 fileHash 查出元数据信息，完善元数据信息
                                        .flatMap(metadata -> {
                                            metadata.setTotalNumberOfChunks(totalNumberOfChunks);
                                            metadata.setFilesInfoList(
                                                    tempList.stream().map(x -> BeanUtil.copyProperties(x, FsFileMetadata.FsFilesInfo.class)).collect(Collectors.toList()));
                                            metadata.setUploadProgress(UploadProgress.UPLOAD_COMPLETED);
                                            return mongoTemplate.save(metadata);
                                        })
                                        // 合并成功
                                        .doOnSuccess(metadata -> {
                                            // 删除缓存表数据
                                            this.cleanTemp(fileHash).subscribe();
                                            // 持久化到正式路径
                                            this.endurance(fileHash)
                                                    .then(Mono.fromRunnable(() -> {
                                                        // 发布事件
                                                        eventPublisher.publishEvent(new AfterMetadataSaveEvent(metadata, fileName));
                                                    }))
                                                    .subscribe();
                                        })
                                        // 在 fs.resource 添加引用
                                        .flatMap(metadata -> MongoResourceStorage.this.insertResource(metadata, fileName, path));
                            }
                    );
        }

        /**
         * 上传失败，清理缓存表
         *
         * @param fileHash 文件 hash
         * @return 操作是否成功
         */
        @Override
        public Mono<Boolean> uploadError(String fileHash) {
            return Mono.create(sink -> {
                // 清理缓存表
                this.cleanTemp(fileHash).subscribe();
                // 清理元数据表
                Criteria c = Criteria.where(FsFileMetadata.FILE_HASH).is(fileHash);
                mongoTemplate.findOne(Query.query(c), FsFileMetadata.class)
                        .subscribe(m -> mongoTemplate.remove(m).subscribe());
                // 本地缓存暂时不清理了
                sink.success(true);
            });
        }

        @NotNull
        private Mono<Boolean> cleanTemp(String fileHash) {
            // 清理缓存表
            Criteria criteria = Criteria.where(FsFileTemp.FILE_HASH).is(fileHash);
            return mongoTemplate.find(Query.query(criteria), FsFileTemp.class)
                    .flatMap(mongoTemplate::remove)
                    .collectList()
                    .map(deleteResults -> true)
                    .switchIfEmpty(Mono.just(false));
        }

        /**
         * 本地碎片合并，如果不存在则去 mongo 下载
         *
         * @param fileHash 文件 hash
         * @return 本地缓存路径（official）
         */
        @Override
        public Mono<String> endurance(String fileHash) {

            // 拼接整个文件的缓存路径，通过文件锁保证只有一个"请求"进行文件的下载
            String tempPath = MongoResourceStorage.this.getFileTempPath(fileHash);
            // 获取正式路径
            String officialPath = MongoResourceStorage.this.getFileOfficialPath(fileHash);
            // 获取锁文件路径
            String lockPath = MongoResourceStorage.this.getLockPath(fileHash);
            if (FileUtil.exist(officialPath)) {
                return Mono.just(officialPath);
            }

            // 拼接碎片缓存文件夹
            String enduranceDir = LOCAL_TEMP_PATH + fileHash + File.separator;

            return MongoResourceStorage.this.getReadyMetadata(fileHash)
                    .publishOn(scheduler)   // 从 nio 线程切换到 scheduler 线程
                    .flatMap(metadata -> MongoResourceStorage.this.merge(metadata, tempPath, officialPath, lockPath,
                            ((randomAccessFile, filesInfo) -> {
                                // 拼接当前碎片的本地缓存路径
                                String endurancePath = enduranceDir + filesInfo.getChunk() + CHUNK_SUFFIX;
                                // 如果本地已经存在了则直接从本地获取
                                if (FileUtil.exist(endurancePath)) {
                                    return Mono.create(sink -> scheduler.schedule(() -> {
                                        try {
                                            FileChannelUtil.transferFrom(endurancePath, randomAccessFile.getChannel());
                                            // 2021/5/9 复制之后删除该文件或文件夹
                                            FileUtil.del(endurancePath);
                                            sink.success();
                                        } catch (IORuntimeException e) {
                                            sink.error(e);
                                        }
                                    }));
//                                    Flux<DataBuffer> read = DataBufferUtils.read(new FileSystemResource(endurancePath), dataBufferFactory, DEFAULT_CHUNK_SIZE);
//                                    return DataBufferUtils.write(read, randomAccessFile.getChannel())
//                                            .doOnTerminate(() -> {
//                                                // 2021/5/9 复制之后删除该文件或文件夹
//                                                FileUtil.del(endurancePath);
//                                            })
//                                            .then();
                                } else {
                                    // 如果本地不存在，则从 mongo 下载
                                    return MongoResourceStorage.this.doDownloadChunk(filesInfo, randomAccessFile).then();
                                }
                            })
                    ))
                    .then(Mono.just(officialPath));
        }

        /**
         * 持久化到指定路径
         *
         * @param fileHash 文件 hash
         * @param dest     目标路径
         */
        @Override
        public Mono<Void> transferTo(String fileHash, Path dest) {
            return this.endurance(fileHash)
                    .flatMap(officialPath -> {
                        FileUtil.copyFile(Paths.get(officialPath), dest, StandardCopyOption.REPLACE_EXISTING);
                        return Mono.empty();
                    });
        }
    }


    /**
     * 清理本地文件缓存
     * <p>
     * 注：只清理 .official 后缀的文件
     * <p>
     * 假如重启服务了，LCU 缓存被清理了怎么办？清理的时间间隔长一点。
     */
    class CleanLocalFileCache {

        public void start() {
            scheduler.schedulePeriodically(() -> {
                log.info("正在清理本地文件缓存！");
                Set<String> frequentlyUsedPath = cache.asMap().keySet();
                File tempDir = new File(LOCAL_TEMP_PATH);
                File[] files = tempDir.listFiles(file -> {
                    if (file.isFile()) {
                        return file.getName().endsWith(OFFICIAL_SUFFIX);
                    }
                    return false;
                });

                Arrays.stream(files).forEach(file -> {
                    if (!frequentlyUsedPath.contains(file.getAbsolutePath())) {
                        FileUtil.del(file);
                    }
                });
            }, 12, 12, TimeUnit.HOURS);
        }
    }
}
