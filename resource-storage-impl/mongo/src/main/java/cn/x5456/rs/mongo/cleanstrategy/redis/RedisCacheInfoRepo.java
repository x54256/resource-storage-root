package cn.x5456.rs.mongo.cleanstrategy.redis;

import org.springframework.data.repository.CrudRepository;

/**
 * @author yujx
 * @date 2021/05/07 10:42
 */
public interface RedisCacheInfoRepo extends CrudRepository<RedisCacheInfo, String> {
}