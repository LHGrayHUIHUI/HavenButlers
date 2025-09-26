package com.haven.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单缓存服务
 *
 * 提供基于内存的简单缓存功能，带TTL支持
 * 用于减少对外部服务的频繁调用
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Service
public class SimpleCacheService {

    /**
     * 缓存数据结构
     */
    private static class CacheEntry {
        private final Object value;
        private final LocalDateTime expireTime;

        public CacheEntry(Object value, int ttlSeconds) {
            this.value = value;
            this.expireTime = LocalDateTime.now().plus(ttlSeconds, ChronoUnit.SECONDS);
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expireTime);
        }

        public Object getValue() {
            return value;
        }
    }

    // 缓存存储
    private final Map<String, CacheEntry> cacheStore = new ConcurrentHashMap<>();

    /**
     * 存储缓存数据
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttlSeconds TTL时间（秒）
     */
    public void put(String key, Object value, int ttlSeconds) {
        if (key == null || value == null) {
            return;
        }

        cacheStore.put(key, new CacheEntry(value, ttlSeconds));
        log.debug("缓存数据已存储: {}, TTL: {}秒", key, ttlSeconds);
    }

    /**
     * 获取缓存数据
     *
     * @param key 缓存键
     * @param <T> 返回值类型
     * @return 缓存值，如果不存在或已过期返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key == null) {
            return null;
        }

        CacheEntry entry = cacheStore.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cacheStore.remove(key);
            log.debug("缓存数据已过期并移除: {}", key);
            return null;
        }

        log.debug("缓存数据命中: {}", key);
        return (T) entry.getValue();
    }

    /**
     * 删除缓存数据
     *
     * @param key 缓存键
     */
    public void remove(String key) {
        if (key != null) {
            cacheStore.remove(key);
            log.debug("缓存数据已删除: {}", key);
        }
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        int size = cacheStore.size();
        cacheStore.clear();
        log.info("已清空所有缓存数据，共 {} 条", size);
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        long totalEntries = cacheStore.size();
        long expiredEntries = cacheStore.values().stream()
                .mapToLong(entry -> entry.isExpired() ? 1 : 0)
                .sum();

        return Map.of(
                "totalEntries", totalEntries,
                "validEntries", totalEntries - expiredEntries,
                "expiredEntries", expiredEntries
        );
    }

    /**
     * 清理过期缓存
     * 通常由定时任务调用
     */
    public void cleanupExpiredEntries() {
        int removed = 0;
        var iterator = cacheStore.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            log.info("清理过期缓存条目: {} 条", removed);
        }
    }

    /**
     * 判断缓存是否存在且未过期
     *
     * @param key 缓存键
     * @return 是否存在有效缓存
     */
    public boolean exists(String key) {
        return get(key) != null;
    }

    /**
     * 获取或计算缓存值
     * 如果缓存不存在或已过期，则使用提供的计算函数生成新值并缓存
     *
     * @param key 缓存键
     * @param ttlSeconds TTL时间（秒）
     * @param supplier 值计算函数
     * @param <T> 返回值类型
     * @return 缓存值或新计算的值
     */
    public <T> T computeIfAbsent(String key, int ttlSeconds, java.util.function.Supplier<T> supplier) {
        T cached = get(key);
        if (cached != null) {
            return cached;
        }

        try {
            T computed = supplier.get();
            if (computed != null) {
                put(key, computed, ttlSeconds);
            }
            return computed;
        } catch (Exception e) {
            log.error("计算缓存值失败, key: {}", key, e);
            return null;
        }
    }
}