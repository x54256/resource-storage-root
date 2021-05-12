package cn.x5456.rs.mongo.cleanstrategy;

import cn.x5456.rs.cleanstrategy.CleanUnUploadedTempStrategy;
import cn.x5456.rs.def.UploadProgress;
import cn.x5456.rs.mongo.document.FsFileMetadata;
import cn.x5456.rs.mongo.document.FsFileTemp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 定时清理未上传完成的文件缓存
 *
 * @author yujx
 * @date 2021/04/30 10:09
 */
@Slf4j
public class SchedulerCleanStrategy implements CleanUnUploadedTempStrategy {

    private Scheduler scheduler;

    private ReactiveMongoTemplate mongoTemplate;

    private final AtomicLong n = new AtomicLong(1);

    public SchedulerCleanStrategy(ReactiveMongoTemplate mongoTemplate, ObjectProvider<Scheduler> schedulerObjectProvider) {
        this.mongoTemplate = mongoTemplate;
        this.scheduler = schedulerObjectProvider.getIfUnique(Schedulers::elastic);
        this.start();
    }

    public void start() {
        scheduler.schedulePeriodically(this::clean, 1000, MongoSocketTimeoutHolder.getCleanTimeout(), TimeUnit.SECONDS);
    }

    public void clean() {
        log.info("正在执行清理操作，第「{}」次。", n.getAndIncrement());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateTime = now.plusNanos(-1 * MongoSocketTimeoutHolder.getCleanTimeout(TimeUnit.NANOSECONDS));

        this.cleanFsFileTemp(dateTime);
        this.cleanFileMetadata(dateTime);
    }

    private void cleanFsFileTemp(LocalDateTime dateTime) {
        // 清理 FsFileTemp
        Criteria criteria = Criteria.where(FsFileTemp.UPLOAD_PROGRESS).is(UploadProgress.UPLOADING)
                .and(FsFileTemp.CREAT_TIME).lte(dateTime);
        mongoTemplate.find(Query.query(criteria), FsFileTemp.class)
                .groupBy(FsFileTemp::getFileHash)
                .subscribe(groupedFlux -> {

                    // 获取 hash
                    String hash = groupedFlux.key();

                    // 获取查出来的数量
                    long countOfUploading = groupedFlux.toStream().count();

                    // 清理元数据表，查询 FsFileTemp 中碎片信息（无论上传是否完成）是否与我们查出来的【不满足条件】的碎片数量一样，如果一样则删除元数据表记录
                    Criteria c1 = Criteria.where(FsFileTemp.FILE_HASH).is(hash);
                    Long count = mongoTemplate.count(Query.query(c1), FsFileTemp.class).block();
                    if (Objects.equals(count, countOfUploading)) {
                        Criteria c = Criteria.where(FsFileMetadata.FILE_HASH).is(hash)
                                .and(FsFileMetadata.UPLOAD_PROGRESS).is(UploadProgress.UPLOADING)
                                .and(FsFileMetadata.MULTIPART_UPLOAD).is(true);
                        mongoTemplate.findOne(Query.query(c), FsFileMetadata.class)
                                .subscribe(metadata -> {
                                    log.info("metadata：「{}」", metadata);
                                    mongoTemplate.remove(metadata).subscribe();
                                });
                    }

                    // 清理缓存表
                    groupedFlux.subscribe(temp -> {
                        log.info("temp：「{}」", temp);
                        mongoTemplate.remove(temp).subscribe();
                    });
                });
    }

    private void cleanFileMetadata(LocalDateTime dateTime) {
        // 清理 FsFileTemp
        Criteria criteria = Criteria.where(FsFileMetadata.UPLOAD_PROGRESS).is(UploadProgress.UPLOADING)
                .and(FsFileMetadata.MULTIPART_UPLOAD).is(false)
                .and(FsFileMetadata.CREAT_TIME).lte(dateTime);

        mongoTemplate.find(Query.query(criteria), FsFileTemp.class)
                .subscribe(metadata -> {
                    log.info("metadata：「{}」", metadata);
                    mongoTemplate.remove(metadata).subscribe();
                });
    }
}
