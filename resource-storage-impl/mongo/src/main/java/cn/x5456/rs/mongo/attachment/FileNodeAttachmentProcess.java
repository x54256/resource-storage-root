package cn.x5456.rs.mongo.attachment;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.x5456.infrastructure.util.CompressUtils;
import cn.x5456.infrastructure.util.FileNodeDTO;
import cn.x5456.infrastructure.util.FileNodeUtil;
import cn.x5456.infrastructure.util.FileTypeGuessUtil;
import cn.x5456.rs.attachment.AttachmentProcess;
import cn.x5456.rs.attachment.AttachmentProcessContainer;
import cn.x5456.rs.constant.AttachmentConstant;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.entity.FileMetadata;
import cn.x5456.rs.mongo.document.FsResourceInfo;
import cn.x5456.rs.mongo.dto.ZipFileNode;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/13 15:27
 * @description:
 */
@Component
@Slf4j
public class FileNodeAttachmentProcess implements AttachmentProcess<ZipFileNode> {

    private static final List<String> COMPRESS_PACKAGE_TYPE_LIST = ImmutableList.of(
            ArchiveStreamFactory.TAR, ArchiveStreamFactory.ZIP, ArchiveStreamFactory.SEVEN_Z
    );

    private final IResourceStorage mongoResourceStorage;
    private final ReactiveMongoTemplate mongoTemplate;

    public FileNodeAttachmentProcess(IResourceStorage mongoResourceStorage, ReactiveMongoTemplate mongoTemplate) {
        this.mongoResourceStorage = mongoResourceStorage;
        this.mongoTemplate = mongoTemplate;
        AttachmentProcessContainer.addProcess(AttachmentConstant.FILE_NODE, this);
    }

    @Override
    public ZipFileNode process(FileMetadata metadata) {
        mongoResourceStorage.downloadFileByFileHash(metadata.getFileHash())
                .subscribe(localFilePath -> {
                    Query query = new Query(Criteria.where("fileHash").is(metadata.getFileHash()));
                    FsResourceInfo resourceInfo = mongoTemplate.findOne(query, FsResourceInfo.class).block();

                    String fileType = FileTypeGuessUtil.getTypeByPath(localFilePath, resourceInfo.getFileName());
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
                        mongoTemplate.save(metadata).subscribe((x) -> {
                            log.info("压缩包文件解析成功！");
                            // 删除解压出来的压缩包
                            FileUtil.del(extractPath);
                        });

                    }
                });

        return null;
    }
}
