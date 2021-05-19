package cn.x5456.infrastructure.util;

import cn.hutool.core.lang.hash.Hash32;
import cn.hutool.core.util.HashUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 一致性Hash算法（线程安全版）
 * 算法详解：http://blog.csdn.net/sparkliang/article/details/5279393
 * 算法实现：https://weblogs.java.net/blog/2007/11/27/consistent-hashing
 * <p>
 * 注：使用 {@link ConcurrentConsistentHash} 的节点对象必须重写以下方法
 * <p>
 * {@link #equals(java.lang.Object)}
 * {@link #hashCode()}
 * {@link #toString()}
 *
 * @param <T> 节点类型
 * @author xiaoleilu
 */
@Slf4j
public class ConcurrentConsistentHash<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Hash计算对象，用于自定义hash算法
     */
    Hash32<Object> hashFunc;
    /**
     * 复制的节点个数
     */
    private final int numberOfReplicas;
    /**
     * 一致性Hash环
     */
    private final SortedMap<Integer, T> circle = new TreeMap<>();

    /**
     * 读写锁
     */
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    /**
     * 构造，使用Java默认的Hash算法
     *
     * @param numberOfReplicas 复制的节点个数，增加每个节点的复制节点有利于负载均衡
     * @param nodes            节点对象
     */
    public ConcurrentConsistentHash(int numberOfReplicas, Collection<? extends T> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        this.hashFunc = key -> {
            //默认使用FNV1hash算法
            return HashUtil.fnvHash(key.toString());
        };
        //初始化节点
        addAll(nodes);
    }

    /**
     * 构造
     *
     * @param hashFunc         hash算法对象
     * @param numberOfReplicas 复制的节点个数，增加每个节点的复制节点有利于负载均衡
     * @param nodes            节点对象
     */
    public ConcurrentConsistentHash(Hash32<Object> hashFunc, int numberOfReplicas, Collection<T> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        this.hashFunc = hashFunc;
        //初始化节点
        addAll(nodes);
    }

    /**
     * 增加节点<br>
     * 每增加一个节点，就会在闭环上增加给定复制节点数<br>
     * 例如复制节点数是2，则每调用此方法一次，增加两个虚拟节点，这两个节点指向同一Node
     * 由于hash算法会调用node的toString方法，故按照toString去重
     *
     * @param node 节点对象
     */
    public void add(T node) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < numberOfReplicas; i++) {
                circle.put(hashFunc.hash32(node.toString() + i), node);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量增加节点<br>
     * 每增加一个节点，就会在闭环上增加给定复制节点数<br>
     * 例如复制节点数是2，则每调用此方法一次，增加两个虚拟节点，这两个节点指向同一Node
     * 由于hash算法会调用node的toString方法，故按照toString去重
     *
     * @param nodes 节点对象列表
     */
    public void addAll(Collection<? extends T> nodes) {
        lock.writeLock().lock();
        try {
            for (T node : nodes) {
                add(node);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 移除节点的同时移除相应的虚拟节点
     *
     * @param node 节点对象
     */
    public void remove(T node) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < numberOfReplicas; i++) {
                circle.remove(hashFunc.hash32(node.toString() + i));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 移除所有虚拟节点
     */
    public void removeAll() {
        lock.writeLock().lock();
        try {
            circle.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空原有的虚拟节点，替换为传入的虚拟节点
     *
     * @param nodes 节点对象列表
     */
    public void reset(Collection<? extends T> nodes) {
        lock.writeLock().lock();
        try {
            // 先取差集，如果没变则直接返回
            HashSet<T> nodeSet = new HashSet<>(circle.values());
            HashSet<T> inputNodeSet = new HashSet<>(nodes);
            if (nodeSet.size() == inputNodeSet.size()) {
                if (nodeSet.containsAll(inputNodeSet)) {
                    return;
                }
            }

            removeAll();
            addAll(nodes);

            log.info("service change：「{}」", nodes);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获得一个最近的顺时针节点
     *
     * @param key 为给定键取Hash，取得顺时针方向上最近的一个虚拟节点对应的实际节点
     * @return 节点对象
     */
    public T get(Object key) {
        lock.readLock().lock();
        try {
            if (circle.isEmpty()) {
                return null;
            }
            int hash = hashFunc.hash32(key);
            if (!circle.containsKey(hash)) {
                SortedMap<Integer, T> tailMap = circle.tailMap(hash);    //返回此映射的部分视图，其键大于等于 hash
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }
            //正好命中
            return circle.get(hash);
        } finally {
            lock.readLock().unlock();
        }
    }
}