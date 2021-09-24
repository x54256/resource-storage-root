package cn.x5456.rs.def;

import cn.hutool.core.lang.Pair;
import cn.x5456.rs.entity.FileMetadata;
import cn.x5456.rs.entity.ResourceInfo;
import com.google.common.annotations.Beta;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 从服务（mongo/ftp）获取文件资源接口
 *
 * @author yujx
 * @date 2019/03/23 14:36
 */
public interface IResourceStorage {

    // ============================== 异步 api

    /**
     * 上传文件到文件服务
     *
     * @param localFilePath 本地文件路径
     * @param path          服务上存储的标识
     * @return 是否上传成功
     */
    Mono<ResourceInfo> uploadFile(String localFilePath, String path);

    /**
     * 上传文件到文件服务
     *
     * @param localFilePath 本地文件路径
     * @param fileName      文件名
     * @param path          服务上存储的标识
     * @return 是否上传成功
     */
    Mono<ResourceInfo> uploadFile(String localFilePath, String fileName, String path);

    /**
     * dataBufferFlux 方式上传文件到文件服务
     *
     * @param dataBufferFlux dataBufferFlux
     * @param fileName       文件名
     * @param path           服务上存储的标识
     * @return 是否上传成功
     */
    Mono<ResourceInfo> uploadFile(Flux<DataBuffer> dataBufferFlux, String fileName, String path);

    /**
     * 从文件服务下载文件
     *
     * @param localFilePath 本地文件路径
     * @param path          服务上存储的标识
     * @return 是否下载成功
     */
    Mono<Boolean> downloadFile(String localFilePath, String path);

    /**
     * 从文件服务中获取文件  Pair<String, byte[]>
     *
     * @param path 服务上存储的标识
     * @return Pair key-文件名，value-dataBufferFlux
     */
    Mono<Pair<String, Flux<DataBuffer>>> downloadFileDataBuffer(String path);

    /**
     * 从文件服务中获取文件
     *
     * @param path 服务上存储的标识
     * @return Pair key-文件名，value-文件在本地的缓存路径
     */
    Mono<Pair<ResourceInfo, String>> downloadFile(String path);

    /**
     * 根据文件 hash 从文件服务中获取文件
     *
     * @param fileHash 文件 hash
     * @return 文件在本地的缓存路径
     */
    Mono<String> downloadFileByFileHash(String fileHash);

    /**
     * 删除文件服务上的文件（引用计数）
     *
     * @param path 服务上存储的标识
     * @return 是否删除成功
     */
    Mono<Boolean> deleteFile(String path);

    /**
     * 通过path获取文件名
     *
     * @param path 服务上存储的标识
     * @return 文件名【包括后缀】
     */
    Mono<String> getFileName(String path);

    /**
     * 通过path获取文件的 hash
     *
     * @param path 服务上存储的标识
     * @return 文件 hash
     */
    Mono<String> getFileHashByPath(String path);

    /**
     * 通过 path 获取对应的资源文件信息
     *
     * @param path 服务上存储的标识
     * @return 资源文件信息
     */
    Mono<? extends ResourceInfo> getResourceInfoByPath(String path);

    /**
     * 通过 path 获取对应的文件元数据信息
     *
     * @param path 服务上存储的标识
     * @return 文件元数据信息
     */
    Mono<? extends FileMetadata> getFileMetadataByPath(String path);

    // ============================== 分片上传大文件异步 api


    BigFileUploader getBigFileUploader();

    // ============================== other api

    /**
     * 获取附件信息
     *
     * @param path   服务上存储的标识
     * @param key    附件信息key
     * @return 附件信息
     */
    <T> Mono<T> getAttachment(String path, String key);

    /**
     * 获取附件信息
     *
     * @param key 附件信息key
     * @param <T> 需要转换的类型
     */
    <T> Mono<T> getAttachmentByHash(String fileHash, String key);

    // ============================== 辅助开发使用的接口，慎用

    /**
     * 清除本地所有缓存
     */
    @Beta
    void cleanLocalTemp();

    /**
     * 删库
     */
    @Beta
    void dropMongoDatabase();

    // 2021/4/25 小文件下载的时候用 0 拷贝 https://www.cnblogs.com/-wenli/p/13380616.html
}
