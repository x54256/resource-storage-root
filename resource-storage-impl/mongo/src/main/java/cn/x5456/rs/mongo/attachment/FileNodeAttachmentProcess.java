package cn.x5456.rs.mongo.attachment;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
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
    private final Scheduler scheduler;

    public FileNodeAttachmentProcess(IResourceStorage mongoResourceStorage, ReactiveMongoTemplate mongoTemplate,
                                     ObjectProvider<Scheduler> schedulerObjectProvider) {
        this.mongoResourceStorage = mongoResourceStorage;
        this.mongoTemplate = mongoTemplate;
        this.scheduler = schedulerObjectProvider.getIfUnique(Schedulers::elastic);
        AttachmentProcessContainer.addProcess(AttachmentConstant.FILE_NODE, this);
    }

    @Override
    public ZipFileNode process(FileMetadata metadata) {
        Mono<ZipFileNode> zipFileNodeMono = Mono.create(sink -> mongoResourceStorage.downloadFileByFileHash(metadata.getFileHash())
                .subscribe(localFilePath -> {
                    // ??????????????????
                    Query query = new Query(Criteria.where(FsResourceInfo.METADATA_ID).is(metadata.getFileHash()));
                    mongoTemplate.findOne(query, FsResourceInfo.class)
                            .publishOn(scheduler)   // ???????????????????????????????????? nio ?????????????????????????????? block()???
                            .subscribe(resourceInfo -> {
                                try {
                                    // ?????????????????????????????????
                                    String fileType = FileTypeGuessUtil.getTypeByPath(localFilePath, resourceInfo.getFileName());
                                    metadata.getAttachments().put(AttachmentConstant.FILE_TYPE, fileType);
                                    mongoTemplate.save(metadata).subscribe();

                                    // ??????????????????????????????????????????????????????
                                    if (!COMPRESS_PACKAGE_TYPE_LIST.contains(fileType)) {
                                        sink.error(new RuntimeException("??????????????????????????????????????????"));
                                        return;
                                    }

                                    // ???????????????
                                    String extractPath = CompressUtils.extract(localFilePath);
                                    FileNodeDTO fileNode = FileNodeUtil.getFileNode(extractPath, node -> {
                                        String path = IdUtil.objectId();
                                        // TODO: 2021/5/14 ?????????????????????????????????????????????
                                        mongoResourceStorage.uploadFile(node.getPath().toString(), path).block();
                                        node.addAttachment(ZipFileNode.PATH, path);
                                    });

                                    // ??? fileNode ????????? zipFileNode
                                    ZipFileNode zipFileNode = this.convert(fileNode);
                                    metadata.getAttachments().put(AttachmentConstant.FILE_NODE, zipFileNode);
                                    mongoTemplate.save(metadata).subscribe(x -> log.info("??????????????????????????????"));

                                    // ??????????????????????????????
                                    FileUtil.del(extractPath);

                                    sink.success(zipFileNode);
                                } catch (IORuntimeException e) {
                                    sink.error(e);
                                }
                            });
                }));

        return zipFileNodeMono.block();
    }

    private ZipFileNode convert(FileNodeDTO fileNode) {
        ZipFileNode zipFileNode = BeanUtil.toBean(fileNode.getAttachments(), ZipFileNode.class);
        BeanUtil.copyProperties(fileNode, zipFileNode);

        List<ZipFileNode> children = new ArrayList<>();
        for (FileNodeDTO child : fileNode.getChildren()) {
            children.add(this.convert(child));
        }
        zipFileNode.setChildren(children);

        return zipFileNode;
    }
}
