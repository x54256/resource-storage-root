package cn.x5456.rs.mongo.cleanstrategy;

import cn.hutool.core.util.ReflectUtil;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author yujx
 * @date 2021/04/30 10:27
 */
public final class MongoSocketTimeoutHolder {

    /**
     * 超过连接超时时间多少倍进行删除
     */
    private static final double cardinalNumber = 2;

    private static long socketTimeout;

    @Autowired
    public void init(ReactiveMongoTemplate mongoTemplate) {
        ReactiveMongoDatabaseFactory mongoDatabaseFactory =
                (ReactiveMongoDatabaseFactory) ReflectUtil.getFieldValue(mongoTemplate, "mongoDatabaseFactory");
        MongoClient mongo = (MongoClient) ReflectUtil.getFieldValue(mongoDatabaseFactory, "mongo");
        MongoClientSettings settings = mongo.getSettings();
        SocketSettings socketSettings = settings.getSocketSettings();
        // 默认 socket-timeout 的默认配置为0，也就是没有限制。如果为 0 我们将其的 socketTimeOut 设置为 5min；
        // 注：超大文件请使用分片上传。
        socketTimeout = socketSettings.getReadTimeout(SECONDS) == 0 ?
                TimeUnit.MINUTES.toSeconds(5) : socketSettings.getReadTimeout(SECONDS);
    }

    public static long getCleanTimeout() {
        return (long) (socketTimeout * cardinalNumber);
    }

    public static long getCleanTimeout(TimeUnit timeUnit) {
        return timeUnit.convert(getCleanTimeout(), SECONDS);
    }
}
