package cn.x5456.rs.def;

import cn.hutool.core.lang.Pair;
import cn.x5456.rs.entity.ResourceInfo;
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

    // ============================== 分片上传大文件异步 api


    BigFileUploader getBigFileUploader();


    // TODO: 2021/4/25 小文件下载的时候用 0 拷贝 https://www.cnblogs.com/-wenli/p/13380616.html
}
