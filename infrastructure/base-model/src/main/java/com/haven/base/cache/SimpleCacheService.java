package com.haven.base.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.haven.base.config.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 简化版多级缓存服务
 * 提供本地缓存(Caffeine) + 分布式缓存(Redis)的基础多级缓存架构
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleCacheService {

    private final CacheProperties cacheProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    // 本地缓存管理器
    private final ConcurrentHashMap<String, Cache<String, Object>> localCaches = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (!cacheProperties.isEnabled()) {
            log.info("多级缓存已禁用");
            return;
        }

        // 初始化默认本地缓存
        initDefaultLocalCache();

        log.info("简化版多级缓存初始化完成，本地缓存启用: {}, 分布式缓存启用: {}",
            cacheProperties.getLocal().isEnabled(),
            cacheProperties.getDistributed().isEnabled());
    }

    /**
     * 获取缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param clazz     值类型
     * @param <T>       泛型类型
     * @return 缓存值
     */
    public <T> Optional<T> get(String cacheName, String key, Class<T> clazz) {
        if (!cacheProperties.isEnabled()) {
            return Optional.empty();
        }

        try {
            // L1缓存：本地缓存
            if (cacheProperties.getLocal().isEnabled()) {
                Cache<String, Object> localCache = getLocalCache(cacheName);
                Object localValue = localCache.getIfPresent(key);
                if (localValue != null) {
                    log.debug("本地缓存命中: {} -> {}", cacheName, key);
                    return Optional.of(clazz.cast(localValue));
                }
            }

            // L2缓存：分布式缓存
            if (cacheProperties.getDistributed().isEnabled()) {
                String redisKey = buildRedisKey(cacheName, key);
                Object distributedValue = redisTemplate.opsForValue().get(redisKey);
                if (distributedValue != null) {
                    log.debug("分布式缓存命中: {} -> {}", cacheName, key);

                    // 回写本地缓存
                    if (cacheProperties.getLocal().isEnabled()) {
                        Cache<String, Object> localCache = getLocalCache(cacheName);
                        localCache.put(key, distributedValue);
                    }

                    return Optional.of(clazz.cast(distributedValue));
                }
            }

            log.debug("缓存未命中: {} -> {}", cacheName, key);
            return Optional.empty();

        } catch (Exception e) {
            log.error("缓存获取失败: {} -> {}", cacheName, key, e);
            return Optional.empty();
        }
    }

    /**
     * 设置缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param <T>       泛型类型
     */
    public <T> void put(String cacheName, String key, T value) {
        if (!cacheProperties.isEnabled()) {
            return;
        }

        try {
            long ttl = cacheProperties.getDistributed().getDefaultTtl();

            // L1缓存：本地缓存
            if (cacheProperties.getLocal().isEnabled()) {
                Cache<String, Object> localCache = getLocalCache(cacheName);
                localCache.put(key, value);
            }

            // L2缓存：分布式缓存
            if (cacheProperties.getDistributed().isEnabled()) {
                String redisKey = buildRedisKey(cacheName, key);
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(redisKey, value, Duration.ofSeconds(ttl));
                } else {
                    redisTemplate.opsForValue().set(redisKey, value);
                }
            }

            log.debug("缓存设置成功: {} -> {}", cacheName, key);

        } catch (Exception e) {
            log.error("缓存设置失败: {} -> {}", cacheName, key, e);
        }
    }

    /**
     * 设置带过期时间的缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     * @param ttl       过期时间（秒）
     * @param <T>       泛型类型
     */
    public <T> void put(String cacheName, String key, T value, long ttl) {
        if (!cacheProperties.isEnabled()) {
            return;
        }

        try {
            // L1缓存：本地缓存
            if (cacheProperties.getLocal().isEnabled()) {
                Cache<String, Object> localCache = getLocalCache(cacheName);
                localCache.put(key, value);
            }

            // L2缓存：分布式缓存
            if (cacheProperties.getDistributed().isEnabled()) {
                String redisKey = buildRedisKey(cacheName, key);
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(redisKey, value, Duration.ofSeconds(ttl));
                } else {
                    redisTemplate.opsForValue().set(redisKey, value);
                }
            }

            log.debug("缓存设置成功: {} -> {}, TTL: {}s", cacheName, key, ttl);

        } catch (Exception e) {
            log.error("缓存设置失败: {} -> {}", cacheName, key, e);
        }
    }

    /**
     * 获取或设置缓存（如果缓存中没有值，则通过supplier获取并设置）
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param clazz     值类型
     * @param supplier  值提供者
     * @param <T>       泛型类型
     * @return 缓存值
     */
    public <T> T getOrPut(String cacheName, String key, Class<T> clazz, Supplier<T> supplier) {
        Optional<T> cachedValue = get(cacheName, key, clazz);
        if (cachedValue.isPresent()) {
            return cachedValue.get();
        }

        T value = supplier.get();
        if (value != null) {
            put(cacheName, key, value);
        } else if (cacheProperties.getStrategy().isEnableCachePenetrationProtection()) {
            // 缓存空值防止缓存穿透
            put(cacheName, key, null, cacheProperties.getStrategy().getNullValueTtl());
        }

        return value;
    }

    /**
     * 删除缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    public void evict(String cacheName, String key) {
        try {
            // L1缓存：本地缓存
            if (cacheProperties.getLocal().isEnabled()) {
                Cache<String, Object> localCache = getLocalCache(cacheName);
                localCache.invalidate(key);
            }

            // L2缓存：分布式缓存
            if (cacheProperties.getDistributed().isEnabled()) {
                String redisKey = buildRedisKey(cacheName, key);
                redisTemplate.delete(redisKey);
            }

            log.debug("缓存删除成功: {} -> {}", cacheName, key);

        } catch (Exception e) {
            log.error("缓存删除失败: {} -> {}", cacheName, key, e);
        }
    }

    /**
     * 清空指定缓存
     *
     * @param cacheName 缓存名称
     */
    public void clear(String cacheName) {
        try {
            // L1缓存：本地缓存
            if (cacheProperties.getLocal().isEnabled()) {
                Cache<String, Object> localCache = getLocalCache(cacheName);
                localCache.invalidateAll();
            }

            // L2缓存：分布式缓存
            if (cacheProperties.getDistributed().isEnabled()) {
                String pattern = buildRedisKey(cacheName, "*");
                redisTemplate.delete(redisTemplate.keys(pattern));
            }

            log.debug("缓存清空成功: {}", cacheName);

        } catch (Exception e) {
            log.error("缓存清空失败: {}", cacheName, e);
        }
    }

    /**
     * 初始化默认本地缓存
     */
    private void initDefaultLocalCache() {
        CacheProperties.LocalConfig config = cacheProperties.getLocal();
        Cache<String, Object> defaultCache = Caffeine.newBuilder()
            .maximumSize(config.getMaximumSize())
            .expireAfterWrite(Duration.ofSeconds(config.getExpireAfterWrite()))
            .expireAfterAccess(Duration.ofSeconds(config.getExpireAfterAccess()))
            .refreshAfterWrite(Duration.ofSeconds(config.getRefreshAfterWrite()))
            .initialCapacity(config.getInitialCapacity())
            .build();

        localCaches.put("default", defaultCache);
    }

    /**
     * 获取或创建本地缓存
     */
    private Cache<String, Object> getLocalCache(String cacheName) {
        return localCaches.computeIfAbsent(cacheName, this::createDefaultLocalCache);
    }

    /**
     * 创建默认本地缓存
     */
    private Cache<String, Object> createDefaultLocalCache(String cacheName) {
        CacheProperties.LocalConfig config = cacheProperties.getLocal();
        return Caffeine.newBuilder()
            .maximumSize(config.getMaximumSize())
            .expireAfterWrite(Duration.ofSeconds(config.getExpireAfterWrite()))
            .expireAfterAccess(Duration.ofSeconds(config.getExpireAfterAccess()))
            .refreshAfterWrite(Duration.ofSeconds(config.getRefreshAfterWrite()))
            .initialCapacity(config.getInitialCapacity())
            .build();
    }

    /**
     * 构建Redis键
     */
    private String buildRedisKey(String cacheName, String key) {
        return "cache:" + cacheName + ":" + key;
    }
}