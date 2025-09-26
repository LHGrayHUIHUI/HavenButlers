package com.haven.common.redis;

import com.haven.base.utils.JsonUtil;
import com.haven.common.core.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类 - 基于base-model规范
 * 统一JsonUtil序列化，禁用keys命令，支持键前缀配置
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model缓存规范
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
                log.debug("Redis GET: key={}, result=null", fullKey);
                return null;
            }

            T result = JsonUtil.fromJson(value, clazz);
            log.debug("Redis GET: key={}, found=true", fullKey);
            return result;
        } catch (Exception e) {
            log.error("Redis GET失败: key={}, error={}", fullKey, e.getMessage());
            return null;
        }
    }

    /**
     * 获取字符串值 - 无需序列化
     */
    public String getString(String key) {
        String fullKey = buildKey(key);
        return redisTemplate.opsForValue().get(fullKey);
    }

    /**
     * 设置字符串值 - 无需序列化
     */
    public void setString(String key, String value, long timeout, TimeUnit unit) {
        String fullKey = buildKey(key);
        redisTemplate.opsForValue().set(fullKey, value, timeout, unit);
    }

    /**
     * 删除键
     */
    public Boolean delete(String key) {
        String fullKey = buildKey(key);
        return redisTemplate.delete(fullKey);
    }

    /**
     * 批量删除键
     */
    public Long delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return 0L;
        }

        Collection<String> fullKeys = Arrays.stream(keys)
            .map(this::buildKey)
            .toList();
        return redisTemplate.delete(fullKeys);
    }

    /**
     * 判断键是否存在
     */
    public Boolean hasKey(String key) {
        String fullKey = buildKey(key);
        return redisTemplate.hasKey(fullKey);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        String fullKey = buildKey(key);
        return redisTemplate.expire(fullKey, timeout, unit);
    }

    /**
     * 获取过期时间（秒）
     */
    public Long getExpire(String key) {
        String fullKey = buildKey(key);
        return redisTemplate.getExpire(fullKey);
    }

    /**
     * 数值递增 - 区分数值键和JSON键
     */
    public Long increment(String key) {
        String fullKey = buildKey(key);
        return redisTemplate.opsForValue().increment(fullKey);
    }

    /**
     * 数值递增指定值
     */
    public Long increment(String key, long delta) {
        String fullKey = buildKey(key);
        return redisTemplate.opsForValue().increment(fullKey, delta);
    }

    /**
     * 数值递减
     */
    public Long decrement(String key) {
        String fullKey = buildKey(key);
        return redisTemplate.opsForValue().decrement(fullKey);
    }

    /**
     * 数值递减指定值
     */
    public Long decrement(String key, long delta) {
        String fullKey = buildKey(key);
        return redisTemplate.opsForValue().decrement(fullKey, delta);
    }

    /**
     * 使用SCAN命令搜索键 - 替代keys命令避免阻塞
     */
    public Set<String> scan(String pattern, int limit) {
        String fullPattern = buildKey(pattern);
        Set<String> keys = new HashSet<>();

        ScanOptions options = ScanOptions.scanOptions()
            .match(fullPattern)
            .count(limit)
            .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext() && keys.size() < limit) {
                String fullKey = cursor.next();
                // 返回时移除前缀
                String originalKey = removeKeyPrefix(fullKey);
                keys.add(originalKey);
            }
        } catch (Exception e) {
            log.error("Redis SCAN失败: pattern={}, error={}", fullPattern, e.getMessage());
        }

        return keys;
    }

    /**
     * 构建完整键名 - 添加前缀
     */
    private String buildKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Redis键不能为空");
        }

        if (key.startsWith(keyPrefix)) {
            return key;
        }

        return keyPrefix + key;
    }

    /**
     * 移除键前缀
     */
    private String removeKeyPrefix(String fullKey) {
        if (fullKey != null && fullKey.startsWith(keyPrefix)) {
            return fullKey.substring(keyPrefix.length());
        }
        return fullKey;
    }
}