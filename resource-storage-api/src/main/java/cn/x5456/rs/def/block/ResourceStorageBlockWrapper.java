package cn.x5456.rs.def.block;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.IdUtil;
import cn.x5456.rs.def.BigFileUploader;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.def.UploadProgress;
import cn.x5456.rs.entity.ResourceInfo;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;

import java.io.*;
import java.util.List;

import static cn.x5456.rs.constant.DataBufferConstant.DEFAULT_CHUNK_SIZE;

/**
 * 将非阻塞 api 包装成阻塞 api
 *
 * @author yujx
 * @date 2021/05/07 15:59
 */
public class ResourceStorageBlockWrapper implements IBlockResourceStorage {

    private final DataBufferFactory dataBufferFactory;

    private final IResourceStorage resourceStorage;

    private static final String LOCAL_TEMP_PATH;

    static {
        LOCAL_TEMP_PATH = System.getProperty("java.io.tmpdir") + File.separator + "cn.x5456.rs.block" + File.separator;
        // 每次重启删除所有文件
        FileUtil.del(LOCAL_TEMP_PATH);
        FileUtil.mkdir(LOCAL_TEMP_PATH);
    }

    public ResourceStorageBlockWrapper(IResourceStorage resourceStorage) {
        this(new DefaultDataBufferFactory(), resourceStorage);
    }

    public ResourceStorageBlockWrapper(DataBufferFactory dataBufferFactory, IResourceStorage resourceStorage) {
        this.dataBufferFactory = dataBufferFactory;
        this.resourceStorage = resourceStorage;
    }

    /**
     * 上传文件到文件服务
     *
     * @param localFilePath 本地文件路径
     * @param path          服务上存储的标识
     * @return 是否上传成功
     */
    public ResourceInfo uploadFile(String localFilePath, String path) {
        return resourceStorage.uploadFile(localFilePath, path).block();
    }

    /**
     * 上传文件到文件服务
     *
     * @param localFilePath 本地文件路径
     * @param fileName      文件名
     * @param path          服务上存储的标识
     * @return 是否上传成功
     */
    public ResourceInfo uploadFile(String localFilePath, String fileName, String path) {
        return resourceStorage.uploadFile(localFilePath, fileName, path).block();
    }

    /**
     * 上传文件流到文件服务
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名
     * @param path        服务上存储的标识
     * @return 是否上传成功
     */
    public ResourceInfo uploadFile(InputStream inputStream, String fileName, String path) {
        Flux<DataBuffer> dataBufferFlux = DataBufferUtils.readInputStream(
                () -> inputStream, dataBufferFactory, DEFAULT_CHUNK_SIZE);
        return resourceStorage.uploadFile(dataBufferFlux, fileName, path).block();
    }

    /**
     * 从文件服务下载文件
     *
     * @param localFilePath 本地文件路径
     * @param path          服务上存储的标识
     * @return 是否下载成功
     */
    @CanIgnoreReturnValue
    public Boolean downloadFile(String localFilePath, String path) {
        return resourceStorage.downloadFile(localFilePath, path).block();
    }

    /**
     * 从文件服务中获取文件
     *
     * @param path 服务上存储的标识
     * @return Pair key-文件名，value-输出流
     */
    public Pair<String, OutputStream> downloadFile(String path) {
        String fileName = this.getFileName(path);
        String localFilePath = LOCAL_TEMP_PATH + IdUtil.simpleUUID() + File.separator + fileName;
        // 下载文件
        this.downloadFile(localFilePath, path);
        try {
            return new Pair<>(fileName, new FileOutputStream(localFilePath));
        } catch (FileNotFoundException e) {
            // 不可能发生
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除文件服务上的文件（引用计数）
     *
     * @param path 服务上存储的标识
     * @return 是否删除成功
     */
    public Boolean deleteFile(String path) {
        return resourceStorage.deleteFile(path).block();
    }

    /**
     * 通过path获取文件名
     *
     * @param path 服务上存储的标识
     * @return 文件名【包括后缀】
     */
    public String getFileName(String path) {
        return resourceStorage.getFileName(path).block();
    }

    public BlockBigFileUploader getBigFileUploader() {
        return new BlockBigFileUploaderImpl(resourceStorage.getBigFileUploader());
    }

    class BlockBigFileUploaderImpl implements BlockBigFileUploader {

        BigFileUploader bigFileUploader;

        public BlockBigFileUploaderImpl(BigFileUploader bigFileUploader) {
            this.bigFileUploader = bigFileUploader;
        }

        /**
         * 是否已经存在
         *
         * @param fileHash 文件 hash
         * @return 文件是否已在服务中存在
         */
        public Boolean isExist(String fileHash) {
            return bigFileUploader.isExist(fileHash).block();
        }

        /**
         * 大文件秒传
         *
         * @param fileHash 文件 hash
         * @param fileName 文件名
         * @param path     服务上存储的标识
         * @return 是否上传成功
         */
        public ResourceInfo secondPass(String fileHash, String fileName, String path) {
            return bigFileUploader.secondPass(fileHash, fileName, path).block();
        }

        /**
         * 上传每一片文件
         * 1. 检查 hash 是否存在，避免重复上传
         * 2. 上传时检查上传进度，如果已经在上传那就不上传了
         *
         * @param fileHash    文件 hash
         * @param chunk       第几片
         * @param inputStream 当前片的输入流
         * @return 是否上传成功
         */
        public Boolean uploadFileChunk(String fileHash, int chunk, InputStream inputStream) {
            Flux<DataBuffer> dataBufferFlux = DataBufferUtils.readInputStream(
                    () -> inputStream, dataBufferFactory, DEFAULT_CHUNK_SIZE);
            return bigFileUploader.uploadFileChunk(fileHash, chunk, dataBufferFlux).block();
        }

        /**
         * 获取上传进度 {1: 上传完成， 0: 上传中，2：失败}  1  2  4  8  与运算
         *
         * @param fileHash 文件 hash
         * @return 上传进度
         */
        public List<Pair<Integer, UploadProgress>> uploadProgress(String fileHash) {
            return bigFileUploader.uploadProgress(fileHash).collectList().block();
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
        public Boolean uploadCompleted(String fileHash, String fileName, int totalNumberOfChunks, String path) {
            return bigFileUploader.uploadCompleted(fileHash, fileName, totalNumberOfChunks, path).block();
        }

        /**
         * 上传失败，清理缓存表
         *
         * @param fileHash 文件 hash
         * @return 操作是否成功
         */
        public Boolean uploadError(String fileHash) {
            return bigFileUploader.uploadError(fileHash).block();
        }
    }
}
