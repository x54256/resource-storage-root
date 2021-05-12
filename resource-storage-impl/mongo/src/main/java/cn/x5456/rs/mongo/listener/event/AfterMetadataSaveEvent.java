package cn.x5456.rs.mongo.listener.event;

import cn.x5456.rs.mongo.document.FsFileMetadata;
import org.springframework.context.ApplicationEvent;

/**
 * @author yujx
 * @date 2021/05/11 16:03
 */
public class AfterMetadataSaveEvent extends ApplicationEvent {

    private String fileName;

    public AfterMetadataSaveEvent(FsFileMetadata metadata) {
        this(metadata, null);
    }

    public AfterMetadataSaveEvent(FsFileMetadata metadata, String fileName) {
        super(metadata);
        this.fileName = fileName;
    }

    @Override
    public FsFileMetadata getSource() {
        return (FsFileMetadata) super.getSource();
    }

    public String getFileName() {
        return fileName;
    }
}
