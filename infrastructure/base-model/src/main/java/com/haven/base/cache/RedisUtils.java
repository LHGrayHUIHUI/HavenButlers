package com.haven.base.cache;

import com.haven.base.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类（从common模块精简和迁移）
 * 提供基础的Redis操作功能
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Component
public class RedisUtils {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${base-model.cache.key-prefix:haven:}")
    private String keyPrefix;

    @Value("${base-model.cache.default-ttl:3600}")
    private long defaultTtl;

    /**
     * 构建完整的键名
     */
    private String buildKey(String key) {
        return keyPrefix + key;
    }

    /**
     * 设置值 - 使用统一JsonUtil序列化
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            redisTemplate.opsForValue().set(fullKey, jsonValue, timeout, unit);
            log.debug("Redis SET: key={}, ttl={}s", fullKey, unit.toSeconds(timeout));
        } catch (Exception e) {
            log.error("Redis SET失败: key={}, error={}", fullKey, e.getMessage());
            throw new RuntimeException("Redis设置值失败", e);
        }
    }

    /**
     * 设置值 - 使用默认TTL
     */
    public void set(String key, Object value) {
        set(key, value, defaultTtl, TimeUnit.SECONDS);
    }

    /**
     * 获取值 - 使用统一JsonUtil反序列化
     */
    public <T> T get(String key, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            String value = redisTemplate.opsForValue().get(fullKey);
            if (value == null) {
                return null;
            }
            log.debug("Redis GET: key={}", fullKey);
            return JsonUtil.fromJson(value, clazz);
        } catch (Exception e) {
            log.error("Redis GET失败: key={}, error={}", fullKey, e.getMessage());
            return null;
        }
    }

    /**
     * 获取字符串值
     */
    public String get(String key) {
        String fullKey = buildKey(key);
        try {
            String value = redisTemplate.opsForValue().get(fullKey);
            log.debug("Redis GET: key={}", fullKey);
            return value;
        } catch (Exception e) {
            log.error("Redis GET失败: key={}, error={}", fullKey, e.getMessage());
            return null;
        }
    }

    /**
     * 删除键
     */
    public Boolean delete(String key) {
        String fullKey = buildKey(key);
        try {
            Boolean result = redisTemplate.delete(fullKey);
            log.debug("Redis DELETE: key={}, result={}", fullKey, result);
            return result;
        } catch (Exception e) {
            log.error("Redis DELETE失败: key={}, error={}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * 检查键是否存在
     */
    public Boolean hasKey(String key) {
        String fullKey = buildKey(key);
        try {
            Boolean result = redisTemplate.hasKey(fullKey);
            log.debug("Redis EXISTS: key={}, result={}", fullKey, result);
            return result;
        } catch (Exception e) {
            log.error("Redis EXISTS失败: key={}, error={}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        String fullKey = buildKey(key);
        try {
            Boolean result = redisTemplate.expire(fullKey, timeout, unit);
            log.debug("Redis EXPIRE: key={}, ttl={}s, result={}", fullKey, unit.toSeconds(timeout), result);
            return result;
        } catch (Exception e) {
            log.error("Redis EXPIRE失败: key={}, error={}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * 获取剩余过期时间
     */
    public Long getExpire(String key) {
        String fullKey = buildKey(key);
        try {
            Long result = redisTemplate.getExpire(fullKey, TimeUnit.SECONDS);
            log.debug("Redis TTL: key={}, ttl={}", fullKey, result);
            return result;
        } catch (Exception e) {
            log.error("Redis TTL失败: key={}, error={}", fullKey, e.getMessage());
            return -1L;
        }
    }

    /**
     * 原子递增
     */
    public Long increment(String key) {
        String fullKey = buildKey(key);
        try {
            Long result = redisTemplate.opsForValue().increment(fullKey);
            log.debug("Redis INCR: key={}, result={}", fullKey, result);
            return result;
        } catch (Exception e) {
            log.error("Redis INCR失败: key={}, error={}", fullKey, e.getMessage());
            return 0L;
        }
    }

    /**
     * 原子递增（指定步长）
     */
    public Long increment(String key, long delta) {
        String fullKey = buildKey(key);
        try {
            Long result = redisTemplate.opsForValue().increment(fullKey, delta);
            log.debug("Redis INCRBY: key={}, delta={}, result={}", fullKey, delta, result);
            return result;
        } catch (Exception e) {
            log.error("Redis INCRBY失败: key={}, delta={}, error={}", fullKey, delta, e.getMessage());
            return 0L;
        }
    }

    /**
     * 原子递减
     */
    public Long decrement(String key) {
        String fullKey = buildKey(key);
        try {
            Long result = redisTemplate.opsForValue().decrement(fullKey);
            log.debug("Redis DECR: key={}, result={}", fullKey, result);
            return result;
        } catch (Exception e) {
            log.error("Redis DECR失败: key={}, error={}", fullKey, e.getMessage());
            return 0L;
        }
    }

    /**
     * 原子递减（指定步长）
     */
    public Long decrement(String key, long delta) {
        String fullKey = buildKey(key);
        try {
            Long result = redisTemplate.opsForValue().decrement(fullKey, delta);
            log.debug("Redis DECRBY: key={}, delta={}, result={}", fullKey, delta, result);
            return result;
        } catch (Exception e) {
            log.error("Redis DECRBY失败: key={}, delta={}, error={}", fullKey, delta, e.getMessage());
            return 0L;
        }
    }

    /**
     * 获取匹配模式的所有键
     */
    public Set<String> keys(String pattern) {
        String fullPattern = buildKey(pattern);
        try {
            Set<String> result = redisTemplate.keys(fullPattern);
            log.debug("Redis KEYS: pattern={}, count={}", fullPattern, result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("Redis KEYS失败: pattern={}, error={}", fullPattern, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 分布式锁 - 简单实现
     */
    public Boolean tryLock(String lockKey, String lockValue, long expireTime) {
        String fullKey = buildKey(lockKey);
        String luaScript =
            "if redis.call('get', KEYS[1]) == false then " +
            "  return redis.call('setex', KEYS[1], ARGV[1], ARGV[2]) " +
            "else " +
            "  return false " +
            "end";

        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>(luaScript, Boolean.class);
        try {
            Boolean result = redisTemplate.execute(script,
                Collections.singletonList(fullKey),
                String.valueOf(expireTime),
                lockValue);
            log.debug("Redis TRY_LOCK: key={}, value={}, ttl={}s, result={}",
                     fullKey, lockValue, expireTime, result);
            return result;
        } catch (Exception e) {
            log.error("Redis TRY_LOCK失败: key={}, error={}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * 释放分布式锁 - 简单实现
     */
    public Boolean releaseLock(String lockKey, String lockValue) {
        String fullKey = buildKey(lockKey);
        String luaScript =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        try {
            Long result = redisTemplate.execute(script,
                Collections.singletonList(fullKey),
                lockValue);
            log.debug("Redis RELEASE_LOCK: key={}, value={}, result={}",
                     fullKey, lockValue, result);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Redis RELEASE_LOCK失败: key={}, error={}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * 根据模式删除键
     *
     * @param pattern 键模式
     * @return 删除的键数量
     */
    public Long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys("*" + pattern + "*");
            if (keys != null && !keys.isEmpty()) {
                Long count = redisTemplate.delete(keys);
                log.debug("Redis DELETE_BY_PATTERN: pattern={}, keys={}, count={}",
                         pattern, keys.size(), count);
                return count;
            }
            return 0L;
        } catch (Exception e) {
            log.error("Redis DELETE_BY_PATTERN失败: pattern={}, error={}", pattern, e.getMessage());
            return 0L;
        }
    }
}