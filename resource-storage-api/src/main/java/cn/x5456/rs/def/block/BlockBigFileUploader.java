package cn.x5456.rs.def.block;

import cn.hutool.core.lang.Pair;
import cn.x5456.rs.def.UploadProgress;
import cn.x5456.rs.entity.ResourceInfo;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface BlockBigFileUploader {

    /**
     * 是否已经存在
     *
     * @param fileHash 文件 hash
     * @return 文件是否已在服务中存在
     */
    Boolean isExist(String fileHash);

    /**
     * 大文件秒传
     *
     * @param fileHash 文件 hash
     * @param fileName 文件名
     * @param path     服务上存储的标识
     * @return 是否上传成功
     */
    ResourceInfo secondPass(String fileHash, String fileName, String path);

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
    Boolean uploadFileChunk(String fileHash, int chunk, InputStream inputStream);

    /**
     * 获取上传进度 {1: 上传完成， 0: 上传中，2：失败}  1  2  4  8  与运算
     *
     * @param fileHash 文件 hash
     * @return 上传进度
     */
    List<Pair<Integer, UploadProgress>> uploadProgress(String fileHash);

    /**
     * 全部上传完成，该接口具有幂等性
     *
     * @param fileHash            文件 hash
     * @param fileName            文件名
     * @param totalNumberOfChunks 当前文件一共多少片
     * @param path                服务上存储的标识
     * @return 操作是否成功
     */
    Boolean uploadCompleted(String fileHash, String fileName, int totalNumberOfChunks, String path);

    /**
     * 上传失败，清理缓存表
     *
     * @param fileHash 文件 hash
     * @return 操作是否成功
     */
    Boolean uploadError(String fileHash);

    /**
     * 本地碎片合并，如果不存在则去 mongo 下载，最好配合 hash 环负载算法使用
     *
     * @param fileHash 文件 hash
     * @return 本地缓存路径（official）
     */
    String endurance(String fileHash);

    /**
     * 持久化到指定路径
     *
     * @param fileHash 文件 hash
     * @param dest     目标路径
     */
    void transferTo(String fileHash, Path dest);
}