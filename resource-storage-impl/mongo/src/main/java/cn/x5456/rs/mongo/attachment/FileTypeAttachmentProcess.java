package cn.x5456.rs.mongo.attachment;

import cn.hutool.core.io.IORuntimeException;
import cn.x5456.infrastructure.util.FileTypeGuessUtil;
import cn.x5456.rs.attachment.AttachmentProcess;
import cn.x5456.rs.attachment.AttachmentProcessContainer;
import cn.x5456.rs.constant.AttachmentConstant;
import cn.x5456.rs.def.IResourceStorage;
import cn.x5456.rs.entity.FileMetadata;
import cn.x5456.rs.mongo.document.FsResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * @author: dengdh@dist.com.cn
 * @date: 2021/5/13 15:27
 * @description:
 */
@Component
@Slf4j
public class FileTypeAttachmentProcess implements AttachmentProcess<String> {

    private final IResourceStorage mongoResourceStorage;
    private final ReactiveMongoTemplate mongoTemplate;
    private final Scheduler scheduler;

    public FileTypeAttachmentProcess(IResourceStorage mongoResourceStorage, ReactiveMongoTemplate mongoTemplate,
                                     ObjectProvider<Scheduler> schedulerObjectProvider) {
        this.mongoResourceStorage = mongoResourceStorage;
        this.mongoTemplate = mongoTemplate;
        this.scheduler = schedulerObjectProvider.getIfUnique(Schedulers::elastic);
        AttachmentProcessContainer.addProcess(AttachmentConstant.FILE_TYPE, this);
    }

    @Override
    public String process(FileMetadata metadata) {
        Mono<String> zipFileNodeMono = Mono.create(sink -> mongoResourceStorage.downloadFileByFileHash(metadata.getFileHash())
                .subscribe(localFilePath -> {
                    // 猜测文件类型
                    Query query = new Query(Criteria.where(FsResourceInfo.METADATA_ID).is(metadata.getFileHash()));
                    mongoTemplate.findOne(query, FsResourceInfo.class)
                            .publishOn(scheduler)   // 切换到普通线程，不能阻塞 nio 线程（指的不是下面的 block()）
                            .subscribe(resourceInfo -> {
                                try {
                                    // 推测文件的类型，并保存
                                    String fileType = FileTypeGuessUtil.getTypeByPath(localFilePath, resourceInfo.getFileName());
                                    metadata.getAttachments().put(AttachmentConstant.FILE_TYPE, fileType);
                                    mongoTemplate.save(metadata).subscribe();

                                    sink.success(fileType);
                                } catch (IORuntimeException e) {
                                    sink.error(e);
                                }
                            });
                }));

        return zipFileNodeMono.block();
    }
}
