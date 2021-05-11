package cn.x5456.rs.mongo.cleanstrategy.redis;

import cn.x5456.rs.def.UploadProgress;
import cn.x5456.rs.mongo.document.FsFileMetadata;
import cn.x5456.rs.mongo.document.FsFileTemp;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;

import java.util.Objects;

@Slf4j
public class CacheExpiredListener implements ApplicationListener<RedisKeyExpiredEvent<?>> {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Override
    public void onApplicationEvent(@NotNull RedisKeyExpiredEvent<?> event) {
        log.info("RedisKeyExpiredEvent：「{}」", event);
        Object value = event.getValue();
        if (!(value instanceof RedisCacheInfo)) {
            return;
        }

        RedisCacheInfo redisCacheInfo = (RedisCacheInfo) value;
        if (redisCacheInfo.getIsTemp()) {
            this.cleanFsFileTemp(redisCacheInfo);
        } else {
            this.cleanFileMetadata(redisCacheInfo);
        }
    }

    private void cleanFileMetadata(RedisCacheInfo redisCacheInfo) {
        Criteria criteria = Criteria.where(FsFileMetadata.FILE_HASH).is(redisCacheInfo.getFileHash())
                .and(FsFileMetadata.MULTIPART_UPLOAD).is(false)
                .and(FsFileMetadata.UPLOAD_PROGRESS).is(UploadProgress.UPLOADING);

        mongoTemplate.findOne(Query.query(criteria), FsFileMetadata.class).subscribe(x -> mongoTemplate.remove(x).subscribe());
    }

    private void cleanFsFileTemp(RedisCacheInfo redisCacheInfo) {
        Criteria criteria = Criteria.where(FsFileTemp.ID).is(redisCacheInfo.getId())
                .and(FsFileMetadata.UPLOAD_PROGRESS).is(UploadProgress.UPLOADING);

        mongoTemplate.findOne(Query.query(criteria), FsFileTemp.class)
                .subscribe(temp -> {
                    // 如果查找到了则删除
                    mongoTemplate.remove(temp).subscribe();

                    // 查询 FsFileTemp 中是否还有碎片信息（无论上传是否完成），如果没有则删除 metadata 表数据
                    Criteria c = Criteria.where(FsFileTemp.FILE_HASH).is(redisCacheInfo.getFileHash());
                    mongoTemplate.count(Query.query(c), FsFileTemp.class).subscribe(count -> {
                        if (Objects.equals(count, 0L)) {
                            log.debug("由于 FsFileTemp 全部被清除，删除 metadata 表中 id 为「{}」的数据", redisCacheInfo.getFileHash());

                            Criteria c1 = Criteria.where(FsFileMetadata.FILE_HASH).is(redisCacheInfo.getFileHash())
                                    .and(FsFileMetadata.UPLOAD_PROGRESS).is(UploadProgress.UPLOADING)
                                    .and(FsFileMetadata.MULTIPART_UPLOAD).is(true);
                            mongoTemplate.findOne(Query.query(c1), FsFileMetadata.class)
                                    .subscribe(x -> mongoTemplate.remove(x).subscribe());
                        }
                    });
                });
    }
}