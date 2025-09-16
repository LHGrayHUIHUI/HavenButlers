package com.haven.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁实现
 * 基于Redis的分布式锁，支持可重入和自动续期
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final String LOCK_VALUE_PREFIX = "lock_value_";

    // Lua脚本：释放锁
    private static final String RELEASE_LOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "   return redis.call('del', KEYS[1]) " +
        "else " +
        "   return 0 " +
        "end";

    // Lua脚本：延长锁时间
    private static final String RENEW_LOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "   return redis.call('expire', KEYS[1], ARGV[2]) " +
        "else " +
        "   return 0 " +
        "end";

    /**
     * 获取锁
     *
     * @param key     锁键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 锁值（用于释放锁）
     */
    public String tryLock(String key, long timeout, TimeUnit unit) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = LOCK_VALUE_PREFIX + UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, timeout, unit);

        if (Boolean.TRUE.equals(success)) {
            log.debug("获取锁成功: key={}, value={}", lockKey, lockValue);
            return lockValue;
        }

        log.debug("获取锁失败: key={}", lockKey);
        return null;
    }

    /**
     * 获取锁（带重试）
     *
     * @param key         锁键
     * @param timeout     锁超时时间
     * @param unit        时间单位
     * @param maxRetries  最大重试次数
     * @param retryDelay  重试延迟（毫秒）
     * @return 锁值
     */
    public String tryLockWithRetry(String key, long timeout, TimeUnit unit,
                                   int maxRetries, long retryDelay) {
        String lockValue = null;
        int retries = 0;

        while (retries < maxRetries) {
            lockValue = tryLock(key, timeout, unit);
            if (lockValue != null) {
                return lockValue;
            }

            retries++;
            if (retries < maxRetries) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("获取锁时被中断: key={}", key);
                    return null;
                }
            }
        }

        log.warn("获取锁失败，已达最大重试次数: key={}, retries={}", key, maxRetries);
        return null;
    }

    /**
     * 释放锁
     *
     * @param key   锁键
     * @param value 锁值
     * @return 是否成功
     */
    public boolean releaseLock(String key, String value) {
        String lockKey = LOCK_PREFIX + key;

        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class),
            Collections.singletonList(lockKey),
            value
        );

        boolean success = Long.valueOf(1).equals(result);
        if (success) {
            log.debug("释放锁成功: key={}, value={}", lockKey, value);
        } else {
            log.warn("释放锁失败: key={}, value={}", lockKey, value);
        }

        return success;
    }

    /**
     * 续期锁
     *
     * @param key     锁键
     * @param value   锁值
     * @param timeout 新的超时时间（秒）
     * @return 是否成功
     */
    public boolean renewLock(String key, String value, long timeout) {
        String lockKey = LOCK_PREFIX + key;

        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(RENEW_LOCK_SCRIPT, Long.class),
            Collections.singletonList(lockKey),
            value,
            String.valueOf(timeout)
        );

        boolean success = Long.valueOf(1).equals(result);
        if (success) {
            log.debug("续期锁成功: key={}, timeout={}", lockKey, timeout);
        } else {
            log.warn("续期锁失败: key={}", lockKey);
        }

        return success;
    }

    /**
     * 检查是否持有锁
     *
     * @param key   锁键
     * @param value 锁值
     * @return 是否持有
     */
    public boolean isLocked(String key, String value) {
        String lockKey = LOCK_PREFIX + key;
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        return value != null && value.equals(currentValue);
    }

    /**
     * 执行带锁的操作
     *
     * @param key      锁键
     * @param timeout  锁超时时间
     * @param unit     时间单位
     * @param runnable 要执行的操作
     * @return 是否执行成功
     */
    public boolean executeWithLock(String key, long timeout, TimeUnit unit,
                                   Runnable runnable) {
        String lockValue = tryLock(key, timeout, unit);
        if (lockValue == null) {
            return false;
        }

        try {
            runnable.run();
            return true;
        } finally {
            releaseLock(key, lockValue);
        }
    }

    /**
     * 执行带锁的操作（带返回值）
     *
     * @param key      锁键
     * @param timeout  锁超时时间
     * @param unit     时间单位
     * @param supplier 要执行的操作
     * @param <T>      返回类型
     * @return 执行结果
     */
    public <T> T executeWithLock(String key, long timeout, TimeUnit unit,
                                 java.util.function.Supplier<T> supplier) {
        String lockValue = tryLock(key, timeout, unit);
        if (lockValue == null) {
            throw new RuntimeException("获取锁失败: " + key);
        }

        try {
            return supplier.get();
        } finally {
            releaseLock(key, lockValue);
        }
    }
}