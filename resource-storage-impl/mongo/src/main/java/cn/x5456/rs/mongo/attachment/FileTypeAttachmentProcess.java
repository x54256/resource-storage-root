package cn.x5456.rs.mongo.attachment;

import cn.hutool.core.util.ArrayUtil;
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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

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

    public FileTypeAttachmentProcess(IResourceStorage mongoResourceStorage, ReactiveMongoTemplate mongoTemplate,
                                     ObjectProvider<Scheduler> schedulerObjectProvider) {
        this.mongoResourceStorage = mongoResourceStorage;
        this.mongoTemplate = mongoTemplate;
        AttachmentProcessContainer.addProcess(AttachmentConstant.FILE_TYPE, this);
    }

    @Override
    public String process(FileMetadata metadata, String... args) {
        Mono<String> fileTypeMono = Mono.create(sink -> mongoResourceStorage.downloadFileByFileHash(metadata.getFileHash())
                .subscribe(localFilePath -> {
                    String fileName = null;
                    if (ArrayUtil.isNotEmpty(args)) {
                        fileName = args[0];
                    }

                    // 猜测文件类型
                    String fileType = FileTypeGuessUtil.getTypeByPath(localFilePath, fileName);
                    metadata.getAttachments().put(AttachmentConstant.FILE_TYPE, fileType);
                    mongoTemplate.save(metadata).subscribe();
                    sink.success(fileType);
                }));

        return fileTypeMono.block();
    }

    private Mono<? extends FsResourceInfo> func() {
        log.info("mongoResourceStorage：「{}」", mongoResourceStorage);
        return null;
    }
}
