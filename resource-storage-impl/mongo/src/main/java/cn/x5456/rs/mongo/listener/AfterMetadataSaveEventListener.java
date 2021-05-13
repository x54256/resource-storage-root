package cn.x5456.rs.mongo.listener;

import cn.hutool.core.util.IdUtil;
import cn.x5456.infrastructure.util.CompressUtils;
import cn.x5456.infrastructure.util.FileNodeDTO;
import cn.x5456.infrastructure.util.FileNodeUtil;
import cn.x5456.infrastructure.util.FileTypeGuessUtil;
import cn.x5456.rs.mongo.MongoResourceStorage;
import cn.x5456.rs.mongo.document.FsFileMetadata;
import cn.x5456.rs.mongo.listener.event.AfterMetadataSaveEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import java.util.Map;

/**
 * 解析压缩文件的监听器
 *
 * @author yujx
 * @date 2021/05/11 16:07
 */
@Slf4j
public class AfterMetadataSaveEventListener implements ApplicationListener<AfterMetadataSaveEvent> {

    // TODO: 2021/5/11 有密码的怎么办

    private final ReactiveMongoTemplate mongoTemplate;

    private final MongoResourceStorage mongoResourceStorage;

    public AfterMetadataSaveEventListener(ReactiveMongoTemplate mongoTemplate, MongoResourceStorage mongoResourceStorage) {
        this.mongoTemplate = mongoTemplate;
        this.mongoResourceStorage = mongoResourceStorage;
    }

    @Override
    public void onApplicationEvent(AfterMetadataSaveEvent event) {
        /*
        0. 下载文件（通过 api）
        1. 判断文件类型需不需要处理
        2. 如果需要处理则保存一些（进度）信息
        4. 解析文件，保存 fileContentMetadata

         */
        log.info("监听处理开始======");
        String fileName = event.getFileName();
        FsFileMetadata metadata = event.getSource();

        mongoResourceStorage.downloadFileByFileHash(metadata.getFileHash())
                .subscribe(localFilePath -> {
                    String fileType = FileTypeGuessUtil.getTypeByPath(localFilePath, fileName);
                    metadata.setFileType(fileType);
                    mongoTemplate.save(metadata).subscribe();

                    if ("zip".equals(fileType)) {
                        String extractPath = CompressUtils.extract(localFilePath);
                        FileNodeDTO fileNode = FileNodeUtil.getFileNode(extractPath, node -> {
                            String path = IdUtil.objectId();
                            mongoResourceStorage.uploadFile(localFilePath, path).subscribe();
                            node.getAttachments().put("path", path);
                        });

                        Map<String, Object> map = metadata.getFileContentMetadata();
                        map.put("fileNode", fileNode);

                        mongoTemplate.save(metadata).subscribe((x) -> log.info("结束"));
                    }
                });


//        String fileType = FileTypeGuessUtil.getTypeByPath(officialPath);
//        MongoResourceStorage.this.getFileMetadata(fileHash)
//                .subscribe(metadata -> {
//                    metadata.setFileType(fileType);
//                    mongoTemplate.save(metadata).subscribe();
//                });
//        return officialPath;
    }
}
