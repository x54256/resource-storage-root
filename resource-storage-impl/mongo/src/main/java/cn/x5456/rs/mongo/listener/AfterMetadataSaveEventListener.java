package cn.x5456.rs.mongo.listener;

import cn.x5456.infrastructure.util.FileTypeGuessUtil;
import cn.x5456.rs.constant.AttachmentConstant;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.mongo.document.FsFileMetadata;
import cn.x5456.rs.mongo.listener.event.AfterMetadataSaveEvent;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 解析压缩文件的监听器
 *
 * @author yujx
 * @date 2021/05/11 16:07
 */
@Slf4j
@Component
public class AfterMetadataSaveEventListener implements ApplicationListener<AfterMetadataSaveEvent> {

    private static final List<String> COMPRESS_PACKAGE_TYPE_LIST = ImmutableList.of(
            ArchiveStreamFactory.TAR, ArchiveStreamFactory.ZIP, ArchiveStreamFactory.SEVEN_Z
    );

    // 2021/5/11 有密码的怎么办，用户调用 cn.x5456.rs.def.IResourceStorage.getAttachment 时抛出异常

    private final IResourceStorage mongoResourceStorage;

    public AfterMetadataSaveEventListener(IResourceStorage mongoResourceStorage) {
        this.mongoResourceStorage = mongoResourceStorage;
    }

    @Override
    public void onApplicationEvent(AfterMetadataSaveEvent event) {
        /*
        0. 下载文件（通过 api）
        1. 判断文件类型需不需要处理
        2. 如果需要处理则保存一些（进度）信息
        4. 解析文件，保存 attachments
         */
        String fileName = event.getFileName();
        FsFileMetadata metadata = event.getSource();

        mongoResourceStorage.downloadFileByFileHash(metadata.getFileHash())
                .subscribe(localFilePath -> {
                    String fileType = FileTypeGuessUtil.getTypeByPath(localFilePath, fileName);
                    if (COMPRESS_PACKAGE_TYPE_LIST.contains(fileType)) {
                        mongoResourceStorage.getAttachmentByHash(metadata.getFileHash(), AttachmentConstant.FILE_NODE).subscribe();
                    }
                });
    }
}
