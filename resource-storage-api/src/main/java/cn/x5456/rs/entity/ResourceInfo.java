package cn.x5456.rs.entity;

/**
 * @author yujx
 * @date 2021/05/08 09:47
 */
public interface ResourceInfo {

    /**
     * @return id
     */
    String getId();

    /**
     * @return 获取当前文件的名字
     */
    String getFileName();

    /**
     * @return 获取当前文件的 hash 值
     */
    String getFileHash();

    /**
     * {@link FileMetadata#getId()}
     */
    String getMetadataId();
}
