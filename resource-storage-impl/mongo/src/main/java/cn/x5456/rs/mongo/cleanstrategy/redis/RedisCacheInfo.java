package cn.x5456.rs.mongo.cleanstrategy.redis;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@Data
@Builder
@RedisHash(value = RedisCacheInfo.PREFIX)
public class RedisCacheInfo implements Serializable {

    // 这个名字不能带":"，否则会有部分键不会自动清理
    public static final String PREFIX = "rs";

    public static final String EXPIRATION_OF_NEXT_MINUTE = "expirationOfNextMinute";

    private String id;
    private String fileHash;

    // 过期时间，秒为单位
    @TimeToLive
    private Long ttl;

    // (当前时间戳 + 过期时间)并且进位到下一分钟的时间戳
    @Indexed
    private Long expirationOfNextMinute;

    // 只有是 temp 表的数据这个参数为 true
    private Boolean isTemp;
}