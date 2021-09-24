package cn.x5456.rs.attachment;

import cn.x5456.rs.entity.FileMetadata;

/**
 * {@link FileMetadata#getAttachments()} 中内容的生成器
 *
 * @author yujx
 * @date 2021/05/13 15:00
 */
public interface AttachmentProcess<T> {

    T process(FileMetadata metadata, String ... args);
}
