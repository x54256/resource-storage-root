package cn.x5456.rs.entity;

/**
 * @author yujx
 * @date 2021/05/08 09:42
 */
public interface FilesInfo {

    /**
     * @return 当前整个文件的第几片
     */
    Integer getChunk();

    /**
     * @return 获取当前片文件在服务器上的唯一标识
     */
    String getFilesId();

    /**
     * @return 当前片的大小
     */
    Long getChunkSize();
}
