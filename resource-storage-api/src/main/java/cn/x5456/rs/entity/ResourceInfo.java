package cn.x5456.rs.entity;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author yujx
 * @date 2021/05/08 09:47
 */
public interface ResourceInfo {

    /**
     * @return id
     */
    @ApiModelProperty("资源id，又名 path")
    String getId();

    /**
     * @return 获取当前文件的名字
     */
    @ApiModelProperty("资源文件名")
    String getFileName();

    /**
     * @return 获取当前文件的 hash 值
     */
    @ApiModelProperty("当前文件的 hash 值")
    String getFileHash();

    /**
     * {@link FileMetadata#getId()}
     *
     * @deprecated fileHash 就是 metadataId
     */
    @Deprecated
    @ApiModelProperty("已废弃，请勿使用")
    String getMetadataId();

    /**
     * 获取文件的 Mime Type
     */
    String getMimeType();

    /**
     * 获取文件类型
     */
    String getFileType();
}
