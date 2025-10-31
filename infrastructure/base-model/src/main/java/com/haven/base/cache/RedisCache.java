package com.haven.base.cache;

import com.haven.base.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis高级缓存管理器（从common模块迁移和精简）
 * 提供批量操作、Hash、List、Set、ZSet等高级功能
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCache {

    private final RedisUtils redisUtils;
    private final StringRedisTemplate redisTemplate;

    @Value("${base-model.cache.key-prefix:haven:}")
    private String keyPrefix;

    /**
     * 批量设置
     */
    public void setBatch(Map<String, Object> keyValues, long timeout, TimeUnit unit) {
        if (keyValues == null || keyValues.isEmpty()) {
            return;
        }

        try {
            Map<String, String> stringMap = new HashMap<>();
            keyValues.forEach((key, value) -> {
                String fullKey = buildKey(key);
                String jsonValue = JsonUtil.toJson(value);
                stringMap.put(fullKey, jsonValue);
            });

            redisTemplate.opsForValue().multiSet(stringMap);

            // 为所有键设置过期时间
            stringMap.keySet().forEach(key ->
                redisTemplate.expire(key, timeout, unit));

            log.debug("Redis批量SET: count={}, ttl={}s", keyValues.size(), unit.toSeconds(timeout));
        } catch (Exception e) {
            log.error("Redis批量SET失败: count={}, error={}", keyValues.size(), e.getMessage());
            throw new RuntimeException("批量设置值失败", e);
        }
    }

    /**
     * 批量获取
     */
    public <T> Map<String, T> getBatch(Collection<String> keys, Class<T> clazz) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> fullKeys = keys.stream()
                    .map(this::buildKey)
                    .collect(Collectors.toList());

            List<String> values = redisTemplate.opsForValue().multiGet(fullKeys);

            Map<String, T> result = new HashMap<>();
            Iterator<String> keyIter = keys.iterator();
            Iterator<String> valueIter = values.iterator();

            while (keyIter.hasNext() && valueIter.hasNext()) {
                String key = keyIter.next();
                String value = valueIter.next();
                if (value != null) {
                    T obj = JsonUtil.fromJson(value, clazz);
                    if (obj != null) {
                        result.put(key, obj);
                    }
                }
            }

            log.debug("Redis批量GET: request={}, hit={}", keys.size(), result.size());
            return result;
        } catch (Exception e) {
            log.error("Redis批量GET失败: count={}, error={}", keys.size(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Hash操作 - 设置字段
     */
    public void hSet(String key, String field, Object value) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            redisTemplate.opsForHash().put(fullKey, field, jsonValue);
            log.debug("Redis HSET: key={}, field={}", fullKey, field);
        } catch (Exception e) {
            log.error("Redis HSET失败: key={}, field={}, error={}", fullKey, field, e.getMessage());
            throw new RuntimeException("Hash设置字段失败", e);
        }
    }

    /**
     * Hash操作 - 获取字段
     */
    public <T> T hGet(String key, String field, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            Object value = redisTemplate.opsForHash().get(fullKey, field);
            if (value == null) {
                return null;
            }
            log.debug("Redis HGET: key={}, field={}", fullKey, field);
            return JsonUtil.fromJson(value.toString(), clazz);
        } catch (Exception e) {
            log.error("Redis HGET失败: key={}, field={}, error={}", fullKey, field, e.getMessage());
            return null;
        }
    }

    /**
     * Hash操作 - 获取所有字段
     */
    public <T> Map<String, T> hGetAll(String key, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(fullKey);
            if (entries.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, T> result = new HashMap<>();
            entries.forEach((field, value) -> {
                try {
                    T obj = JsonUtil.fromJson(value.toString(), clazz);
                    if (obj != null) {
                        result.put(field.toString(), obj);
                    }
                } catch (Exception e) {
                    log.warn("Hash字段反序列化失败: key={}, field={}, value={}",
                            fullKey, field, value);
                }
            });

            log.debug("Redis HGETALL: key={}, count={}", fullKey, result.size());
            return result;
        } catch (Exception e) {
            log.error("Redis HGETALL失败: key={}, error={}", fullKey, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * List操作 - 左推入
     */
    public void lPush(String key, Object value) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            redisTemplate.opsForList().leftPush(fullKey, jsonValue);
            log.debug("Redis LPUSH: key={}", fullKey);
        } catch (Exception e) {
            log.error("Redis LPUSH失败: key={}, error={}", fullKey, e.getMessage());
            throw new RuntimeException("List左推入失败", e);
        }
    }

    /**
     * List操作 - 右弹出
     */
    public <T> T rPop(String key, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            String value = redisTemplate.opsForList().rightPop(fullKey);
            if (value == null) {
                return null;
            }
            log.debug("Redis RPOP: key={}", fullKey);
            return JsonUtil.fromJson(value, clazz);
        } catch (Exception e) {
            log.error("Redis RPOP失败: key={}, error={}", fullKey, e.getMessage());
            return null;
        }
    }

    /**
     * List操作 - 获取列表范围
     */
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            List<String> values = redisTemplate.opsForList().range(fullKey, start, end);
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }

            List<T> result = new ArrayList<>();
            for (String value : values) {
                try {
                    T obj = JsonUtil.fromJson(value, clazz);
                    if (obj != null) {
                        result.add(obj);
                    }
                } catch (Exception e) {
                    log.warn("List元素反序列化失败: key={}, value={}", fullKey, value);
                }
            }

            log.debug("Redis LRANGE: key={}, start={}, end={}, count={}",
                     fullKey, start, end, result.size());
            return result;
        } catch (Exception e) {
            log.error("Redis LRANGE失败: key={}, error={}", fullKey, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Set操作 - 添加成员
     */
    public void sAdd(String key, Object value) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            redisTemplate.opsForSet().add(fullKey, jsonValue);
            log.debug("Redis SADD: key={}", fullKey);
        } catch (Exception e) {
            log.error("Redis SADD失败: key={}, error={}", fullKey, e.getMessage());
            throw new RuntimeException("Set添加成员失败", e);
        }
    }

    /**
     * Set操作 - 获取所有成员
     */
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            Set<String> values = redisTemplate.opsForSet().members(fullKey);
            if (values == null || values.isEmpty()) {
                return Collections.emptySet();
            }

            Set<T> result = new HashSet<>();
            for (String value : values) {
                try {
                    T obj = JsonUtil.fromJson(value, clazz);
                    if (obj != null) {
                        result.add(obj);
                    }
                } catch (Exception e) {
                    log.warn("Set成员反序列化失败: key={}, value={}", fullKey, value);
                }
            }

            log.debug("Redis SMEMBERS: key={}, count={}", fullKey, result.size());
            return result;
        } catch (Exception e) {
            log.error("Redis SMEMBERS失败: key={}, error={}", fullKey, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * ZSet操作 - 添加成员
     */
    public void zAdd(String key, Object value, double score) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            redisTemplate.opsForZSet().add(fullKey, jsonValue, score);
            log.debug("Redis ZADD: key={}, score={}", fullKey, score);
        } catch (Exception e) {
            log.error("Redis ZADD失败: key={}, score={}, error={}", fullKey, score, e.getMessage());
            throw new RuntimeException("ZSet添加成员失败", e);
        }
    }

    /**
     * ZSet操作 - 获取范围
     */
    public <T> List<T> zRange(String key, long start, long end, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            Set<String> values = redisTemplate.opsForZSet().range(fullKey, start, end);
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }

            List<T> result = new ArrayList<>();
            for (String value : values) {
                try {
                    T obj = JsonUtil.fromJson(value, clazz);
                    if (obj != null) {
                        result.add(obj);
                    }
                } catch (Exception e) {
                    log.warn("ZSet成员反序列化失败: key={}, value={}", fullKey, value);
                }
            }

            log.debug("Redis ZRANGE: key={}, start={}, end={}, count={}",
                     fullKey, start, end, result.size());
            return result;
        } catch (Exception e) {
            log.error("Redis ZRANGE失败: key={}, error={}", fullKey, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 删除键
     */
    public Boolean delete(String key) {
        return redisUtils.delete(key);
    }

    /**
     * 检查键是否存在
     */
    public Boolean hasKey(String key) {
        return redisUtils.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisUtils.expire(key, timeout, unit);
    }

    /**
     * 获取剩余过期时间
     */
    public Long getExpire(String key) {
        return redisUtils.getExpire(key);
    }

    /**
     * 构建完整的键名
     */
    private String buildKey(String key) {
        return keyPrefix + key;
    }
}