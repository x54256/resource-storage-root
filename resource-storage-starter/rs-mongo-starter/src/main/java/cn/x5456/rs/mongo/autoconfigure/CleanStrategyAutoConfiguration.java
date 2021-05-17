package cn.x5456.rs.mongo.autoconfigure;

import cn.x5456.rs.cleanstrategy.CleanUnUploadedTempStrategy;
import cn.x5456.rs.mongo.cleanstrategy.SchedulerCleanStrategy;
import com.mongodb.reactivestreams.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import reactor.core.scheduler.Scheduler;

/**
 * @author yujx
 * @date 2021/05/11 10:01
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({MongoClient.class, ReactiveMongoTemplate.class})
@ConditionalOnBean({ReactiveMongoTemplate.class, DefaultDataBufferFactory.class, ReactiveGridFsTemplate.class})
@AutoConfigureAfter(RedisCleanStrategyAutoConfiguration.class)
public class CleanStrategyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CleanUnUploadedTempStrategy.class)
//    @ConditionalOnProperty(prefix = "x5456.rs.clean", name = "strategy", havingValue = "scheduler", matchIfMissing = true)
    public CleanUnUploadedTempStrategy schedulerCleanStrategy(ReactiveMongoTemplate mongoTemplate, ObjectProvider<Scheduler> schedulerObjectProvider) {
        log.info("启用【定时清理】策略~");
        return new SchedulerCleanStrategy(mongoTemplate, schedulerObjectProvider);
    }

}
