package cn.x5456.rs.mongo;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import cn.x5456.rs.def.BigFileUploader;
import cn.x5456.rs.entity.ResourceInfo;
import cn.x5456.rs.mongo.document.FsResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static cn.x5456.rs.constant.DataBufferConstant.DEFAULT_CHUNK_SIZE;

/**
 * @author yujx
 * @date 2021/05/08 10:28
 */
@Slf4j
@SpringBootTest(classes = BootstrapConfig.class)
@RunWith(SpringRunner.class)
public class MongoResourceStorageTest {

    private MongoResourceStorage mongoResourceStorage;

    private ReactiveMongoTemplate mongoTemplate;

    private BigFileUploader bigFileUploader;

    private DataBufferFactory dataBufferFactory;

    @Autowired
    public void init(DataBufferFactory dataBufferFactory, ReactiveMongoTemplate mongoTemplate, ReactiveGridFsTemplate gridFsTemplate,
                     ApplicationEventPublisher eventPublisher, ObjectProvider<Scheduler> schedulerObjectProvider) {
        this.mongoResourceStorage =
                new MongoResourceStorage(dataBufferFactory, mongoTemplate, gridFsTemplate, eventPublisher, schedulerObjectProvider);
        this.mongoTemplate = mongoTemplate;
        this.bigFileUploader = mongoResourceStorage.getBigFileUploader();
        this.dataBufferFactory = dataBufferFactory;
    }

    private final String absoluteFileName = "test.txt";
    String path = IdUtil.simpleUUID();

    @Test
    public void uploadFile() {
        // 理论上不会触发这种情况的，因为这个方法第一个执行
        this.deleteIfExist();

        String localFilePath = FileUtil.getAbsolutePath(absoluteFileName);
        ResourceInfo fsResourceInfo = mongoResourceStorage.uploadFile(localFilePath, path).block();
        log.info("fsResourceInfo：「{}」", fsResourceInfo);

        Query query = Query.query(Criteria.where(FsResourceInfo.PATH).is(fsResourceInfo.getId()));
        Assert.assertEquals(fsResourceInfo, mongoTemplate.findOne(query, FsResourceInfo.class).block());
    }

    private void deleteIfExist() {
        Query query = Query.query(Criteria.where(FsResourceInfo.PATH).is(path));
        FsResourceInfo resourceInfo = mongoTemplate.findOne(query, FsResourceInfo.class).block();
        if (resourceInfo != null) {
            this.deleteFile();
        }
    }

    @Test
    public void downloadFile() {
        this.checkFileIsUpload();

        // 随机生成一个本地保存的路径
        String localFilePath = FileUtil.getAbsolutePath(IdUtil.simpleUUID() + ".txt");
        log.info("localFilePath：「{}」", localFilePath);

        Boolean block = mongoResourceStorage.downloadFile(localFilePath, path).block();
        Assert.assertTrue(block);
    }

    private void checkFileIsUpload() {
        Query query = Query.query(Criteria.where(FsResourceInfo.PATH).is(path));
        FsResourceInfo resourceInfo = mongoTemplate.findOne(query, FsResourceInfo.class).block();
        if (resourceInfo == null) {
            this.uploadFile();
        }

        mongoResourceStorage.cleanLocalTemp();
    }

    @Test
    public void downloadFileDataBuffer() {
        this.checkFileIsUpload();

        Pair<String, Flux<DataBuffer>> pair = mongoResourceStorage.downloadFileDataBuffer(path).block();
        Assert.assertEquals(absoluteFileName, pair.getKey());
        Assert.assertNotNull(absoluteFileName, pair.getValue().blockLast());
    }

    @Test
    public void deleteFile() {
        this.checkFileIsUpload();

        Assert.assertTrue(mongoResourceStorage.deleteFile(path).block());
        Query query = Query.query(Criteria.where(FsResourceInfo.PATH).is(path));
        FsResourceInfo resourceInfo = mongoTemplate.findOne(query, FsResourceInfo.class).block();
        Assert.assertNull(resourceInfo);
    }

    @Test
    public void getFileName() {
        this.checkFileIsUpload();

        Assert.assertEquals(absoluteFileName, mongoResourceStorage.getFileName(path).block());
    }


    // ========= 大文件

    String chunk1Name = "chunk1.tmp";
    String chunk2Name = "chunk2.tmp";

    String mergeChunkName = "mergechunk.tmp";
    String hash = SecureUtil.sha256(new File(FileUtil.getAbsolutePath(mergeChunkName)));

    String pathBig = IdUtil.simpleUUID();


    @Test
    public void uploadFileChunk() {
        this.delete();

        String chunk1Path = FileUtil.getAbsolutePath(chunk1Name);
        FileSystemResource chunk1Resource = new FileSystemResource(chunk1Path);
        Boolean chunk1Result = bigFileUploader.uploadFileChunk(hash, 0, DataBufferUtils.read(
                chunk1Resource, dataBufferFactory, DEFAULT_CHUNK_SIZE)).block();
        Assert.assertTrue(chunk1Result);

        String chunk2Path = FileUtil.getAbsolutePath(chunk2Name);
        FileSystemResource chunk2Resource = new FileSystemResource(chunk2Path);
        Boolean chunk2Result = bigFileUploader.uploadFileChunk(hash, 1, DataBufferUtils.read(
                chunk2Resource, dataBufferFactory, DEFAULT_CHUNK_SIZE)).block();
        Assert.assertTrue(chunk2Result);
    }

    private void delete() {
        mongoResourceStorage.deleteFile(pathBig).onErrorReturn(false).block();
        this.uploadError();
    }

    // 同时上传两个相同的
    @Test
    public void uploadFileChunkSame() throws InterruptedException {
        String chunk1Path = FileUtil.getAbsolutePath(chunk1Name);
        FileSystemResource chunk1Resource = new FileSystemResource(chunk1Path);

        CountDownLatch latch = new CountDownLatch(2);

        bigFileUploader.uploadFileChunk(hash, 0, DataBufferUtils.read(
                chunk1Resource, dataBufferFactory, DEFAULT_CHUNK_SIZE))
                .doOnTerminate(latch::countDown)
                .subscribe(x -> log.info("x：「{}」", x));

        bigFileUploader.uploadFileChunk(hash, 0, DataBufferUtils.read(
                chunk1Resource, dataBufferFactory, DEFAULT_CHUNK_SIZE))
                .doOnTerminate(latch::countDown)
                .subscribe(x -> log.info("x：「{}」", x));

        latch.await();
    }

    @Test
    public void uploadCompleted() {
        this.uploadFileChunk();

        ResourceInfo block = bigFileUploader.uploadCompleted(hash, mergeChunkName, 2, pathBig).block();
        Assert.assertNotNull(block);
    }

    @Test
    public void secondPass() {
        this.uploadCompleted();

        Assert.assertTrue(bigFileUploader.isExist(hash).block());
    }

    @Test
    public void uploadError() {
        Assert.assertTrue(bigFileUploader.uploadError(hash).block());
    }

    @Test
    public void testBigDownload() {
        mongoResourceStorage.cleanLocalTemp();
        this.uploadCompleted();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mongoResourceStorage.cleanLocalTemp();
        mongoResourceStorage.downloadFile(pathBig).block();
    }

    @Test
    public void testTransferTo() {
        mongoResourceStorage.cleanLocalTemp();
        this.uploadCompleted();
//        mongoResourceStorage.cleanLocalTemp();
        bigFileUploader.transferTo(hash, Paths.get("/Users/x5456/Desktop/1.txt")).block();
    }

    @Test
    public void testFileLock() {
        mongoResourceStorage.cleanLocalTemp();
        this.uploadCompleted();
        mongoResourceStorage.downloadFile(pathBig).subscribe();
        mongoResourceStorage.downloadFile(pathBig).block();
    }
}
