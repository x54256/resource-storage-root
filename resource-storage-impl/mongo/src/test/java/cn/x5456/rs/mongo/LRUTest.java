package cn.x5456.rs.mongo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.junit.Test;

/**
 * @author yujx
 * @date 2021/05/11 10:51
 */
public class LRUTest {

    private final Cache<String, Integer> cache = CacheBuilder.newBuilder()
            .maximumSize(2) // 设置缓存的最大容量
            .concurrencyLevel(10) // 设置并发级别为 10
            .recordStats() // 开启缓存统计
            .build();

    @Test
    public void test() {
        System.out.println("cache.size() = " + cache.size());
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);
        System.out.println("cache.size() = " + cache.size());

        // 2、3
        cache.asMap().forEach((k, v) -> {
            System.out.println("k = " + k);
            System.out.println("v = " + v);
        });
    }

    @Test
    public void test2() {
        System.out.println("cache.size() = " + cache.size());
        cache.put("1", 1);
        cache.put("2", 2);

        System.out.println("cache.get(\"1\") = " + cache.getIfPresent("1"));

        cache.put("3", 3);
        System.out.println("cache.size() = " + cache.size());

        // 1、3
        cache.asMap().forEach((k, v) -> {
            System.out.println("k = " + k);
            System.out.println("v = " + v);
        });
    }
}
