package cn.x5456.rs.entity;

import cn.x5456.rs.def.UploadProgress;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author yujx
 * @date 2021/05/08 09:40
 */
public interface FileMetadata {

    /**
     * @return id
     */
    String getId();

    /**
     * @return 获取当前文件的唯一 hash 值，也可以使用 hash 值作为 id
     */
    String getFileHash();

    /**
     * @return 获取当前文件一共多少片
     */
    Integer getTotalNumberOfChunks();

    /**
     * @return 获取当前文件所有片的信息
     */
    List<? extends FilesInfo> getFilesInfoList();

    /**
     * @return 获取上传进度
     */
    UploadProgress getUploadProgress();

    /**
     * @return 创建时间
     */
    LocalDateTime getCreatTime();

    /**
     * @return 是否是分片上传
     */
    Boolean getMultipartUpload();

    /**
     * @return 获取附件
     */
    Map<String, Object> getAttachments();
}
