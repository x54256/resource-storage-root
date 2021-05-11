package cn.x5456.rs.autoconfigure;

import cn.x5456.rs.mongo.cleanstrategy.redis.CacheExpiredListener;
import cn.x5456.rs.mongo.cleanstrategy.redis.MongoAfterSaveEventListener;
import cn.x5456.rs.mongo.cleanstrategy.redis.RedisCacheInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * 通过 redis 通知机制清理未上传完成的文件缓存
 *
 * @author yujx
 * @date 2021/04/30 10:09
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableRedisRepositories.class)
@ConditionalOnProperty(prefix = "x5456.rs.clean", name = "strategy", havingValue = "redis", matchIfMissing = true)
@EnableRedisRepositories(value = "cn.x5456.rs.mongo.cleanstrategy.redis", enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
@AutoConfigureAfter({RsMongoAutoConfiguration.class, RedisAutoConfiguration.class})
public class RedisCleanStrategyAutoConfiguration {

    public RedisCleanStrategyAutoConfiguration() {
        log.info("启用【redis 清理】策略~");
    }

    @Bean
    public MongoAfterSaveEventListener mongoAfterSaveEventListener() {
        return new MongoAfterSaveEventListener();
    }

    @Bean
    public CacheExpiredListener cacheExpiredListener() {
        return new CacheExpiredListener();
    }

    /**
     * Configuration of scheduled job for cleaning up expired sessions.
     */
    @EnableScheduling
    @Configuration(proxyBeanMethods = false)
    static class CacheCleanupConfiguration implements SchedulingConfigurer {

        private final StringRedisTemplate redis;

        public CacheCleanupConfiguration(StringRedisTemplate redis) {
            this.redis = redis;
        }

        @Override
        public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
            // 每分钟执行一次
            taskRegistrar.addCronTask(this::cleanExpiredCaches, "0 * * * * *");
        }

        private void cleanExpiredCaches() {
            long now = System.currentTimeMillis();
            long prevMin = this.roundDownMinute(now);

            log.info("Cleaning up caches expiring at " + new Date(prevMin));

            String expirationKey = this.getExpirationKey(prevMin);
            Set<String> sessionsToExpire = this.redis.boundSetOps(expirationKey).members();
            this.redis.delete(expirationKey);
            if (sessionsToExpire != null) {
                for (Object session : sessionsToExpire) {
                    String sessionKey = (String) session;
                    this.touch(sessionKey);
                }
            }
        }

        private void touch(String key) {
            this.redis.hasKey(key);
        }

        private String getExpirationKey(long prevMin) {
            // rs:files:caches:expirationOfNextMinute:1620357780000
            return RedisCacheInfo.PREFIX + ":" + RedisCacheInfo.EXPIRATION_OF_NEXT_MINUTE + ":" + prevMin;
        }

        private long roundDownMinute(long timeInMs) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(timeInMs);
            date.clear(Calendar.SECOND);
            date.clear(Calendar.MILLISECOND);
            return date.getTimeInMillis();
        }
    }
}
