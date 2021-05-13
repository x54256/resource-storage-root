package cn.x5456.rs.mongo.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.x5456.infrastructure.util.CompressUtils;
import cn.x5456.infrastructure.util.FileNodeDTO;
import cn.x5456.infrastructure.util.FileNodeUtil;
import cn.x5456.infrastructure.util.FileTypeGuessUtil;
import cn.x5456.rs.constant.AttachmentConstant;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.mongo.document.FsFileMetadata;
import cn.x5456.rs.mongo.dto.ZipFileNode;
import cn.x5456.rs.mongo.listener.event.AfterMetadataSaveEvent;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
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

    // TODO: 2021/5/11 有密码的怎么办

    private final ReactiveMongoTemplate mongoTemplate;

    private final IResourceStorage mongoResourceStorage;

    public AfterMetadataSaveEventListener(ReactiveMongoTemplate mongoTemplate, IResourceStorage mongoResourceStorage) {
        this.mongoTemplate = mongoTemplate;
        this.mongoResourceStorage = mongoResourceStorage;
    }

    @Override
    public void onApplicationEvent(AfterMetadataSaveEvent event) {
        /*
        0. 下载文件（通过 api）
        1. 判断文件类型需不需要处理
        todo 2. 如果需要处理则保存一些（进度）信息
        4. 解析文件，保存 attachments
         */
        String fileName = event.getFileName();
        FsFileMetadata metadata = event.getSource();

        /*mongoResourceStorage.downloadFileByFileHash(metadata.getFileHash())
                .subscribe(localFilePath -> {
                    String fileType = FileTypeGuessUtil.getTypeByPath(localFilePath, fileName);
                    metadata.getAttachments().put(AttachmentConstant.FILE_TYPE, fileType);
                    mongoTemplate.save(metadata).subscribe();

                    if (COMPRESS_PACKAGE_TYPE_LIST.contains(fileType)) {
                        String extractPath = CompressUtils.extract(localFilePath);
                        FileNodeDTO fileNode = FileNodeUtil.getFileNode(extractPath, node -> {
                            String path = IdUtil.objectId();
                            mongoResourceStorage.uploadFile(localFilePath, path).subscribe();
                            node.addAttachment(ZipFileNode.PATH, path);
                        });

                        // 将 fileNode 映射为 zipFileNode
                        ZipFileNode zipFileNode = BeanUtil.toBean(fileNode.getAttachments(), ZipFileNode.class);
                        BeanUtil.copyProperties(fileNode, zipFileNode);

                        metadata.getAttachments().put(AttachmentConstant.FILE_NODE, zipFileNode);
                        mongoTemplate.save(metadata).subscribe(m -> {
                            log.info("压缩包文件解析成功！");
                            // 删除解压出来的压缩包
                            FileUtil.del(extractPath);
                        });
                    }
                });*/
    }
}
