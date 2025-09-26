package com.haven.common.redis;

import com.haven.base.utils.JsonUtil;
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
 * Redis高级缓存管理器 - 基于base-model规范
 * 统一JsonUtil序列化，禁用keys命令，支持批量操作
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model缓存规范
 */
@Slf4j
@Component
public class RedisCache {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisUtils redisUtils;  // 复用键前缀和TTL配置

    @Value("${base-model.cache.key-prefix:haven:}")
    private String keyPrefix;

    @Value("${base-model.cache.default-ttl:3600}")
    private long defaultTtl;

    /**
     * 批量设置缓存 - 使用JsonUtil序列化
     */
    public void setBatch(Map<String, Object> map, long timeout, TimeUnit unit) {
        if (map == null || map.isEmpty()) {
            return;
        }

        map.forEach((key, value) -> {
            try {
                redisUtils.set(key, value, timeout, unit);
            } catch (Exception e) {
                log.error("批量设置缓存失败: key={}, error={}", key, e.getMessage());
            }
        });

        log.debug("批量设置缓存完成: size={}, ttl={}s", map.size(), unit.toSeconds(timeout));
    }

    /**
     * 批量设置缓存 - 使用默认TTL
     */
    public void setBatch(Map<String, Object> map) {
        setBatch(map, defaultTtl, TimeUnit.SECONDS);
    }

    /**
     * 批量获取缓存 - 返回对象类型
     */
    public <T> Map<String, T> getBatch(Collection<String> keys, Class<T> clazz) {
        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, T> result = new HashMap<>();
        keys.forEach(key -> {
            try {
                T value = redisUtils.get(key, clazz);
                if (value != null) {
                    result.put(key, value);
                }
            } catch (Exception e) {
                log.error("批量获取缓存失败: key={}, error={}", key, e.getMessage());
            }
        });

        return result;
    }

    /**
     * 批量获取原始字符串
     */
    public Map<String, String> getBatchStrings(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }

        // 构建完整键名
        Collection<String> fullKeys = keys.stream()
            .map(this::buildKey)
            .collect(Collectors.toList());

        List<String> values = redisTemplate.opsForValue().multiGet(fullKeys);

        Map<String, String> result = new HashMap<>();
        Iterator<String> keyIter = keys.iterator();
        Iterator<String> valueIter = values != null ? values.iterator() : Collections.emptyIterator();

        while (keyIter.hasNext() && valueIter.hasNext()) {
            String key = keyIter.next();
            String value = valueIter.next();
            if (value != null) {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * 批量删除
     */
    public Long deleteBatch(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }

        return redisUtils.delete(keys.toArray(new String[0]));
    }

    /**
     * 模糊删除 - 使用SCAN替代keys命令（安全版本）
     */
    public Long deletePattern(String pattern, int limit) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return 0L;
        }

        Set<String> keys = scanKeys(pattern, limit);
        if (keys.isEmpty()) {
            log.debug("模糊删除未找到匹配键: pattern={}", pattern);
            return 0L;
        }

        Long deleteCount = redisUtils.delete(keys.toArray(new String[0]));
        log.info("模糊删除完成: pattern={}, deleted={}", pattern, deleteCount);

        return deleteCount;
    }

    /**
     * 模糊删除 - 默认限制100个键
     */
    public Long deletePattern(String pattern) {
        return deletePattern(pattern, 100);
    }

    /**
     * 设置值 - 委托给RedisUtils
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisUtils.set(key, value, timeout, unit);
    }

    /**
     * 设置值 - 使用默认TTL
     */
    public void set(String key, Object value) {
        redisUtils.set(key, value);
    }

    /**
     * 设置值（如果不存在）- 使用JsonUtil序列化
     */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            Boolean result = redisTemplate.opsForValue().setIfAbsent(fullKey, jsonValue, timeout, unit);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("条件设置缓存失败: key={}, error={}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * 获取值 - 委托给RedisUtils
     */
    public <T> T get(String key, Class<T> clazz) {
        return redisUtils.get(key, clazz);
    }

    /**
     * 获取并删除 - 使用JsonUtil序列化
     */
    public <T> T getAndDelete(String key, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            String value = redisTemplate.opsForValue().getAndDelete(fullKey);
            return value != null ? JsonUtil.fromJson(value, clazz) : null;
        } catch (Exception e) {
            log.error("获取并删除失败: key={}, error={}", fullKey, e.getMessage());
            return null;
        }
    }

    /**
     * 获取并设置 - 使用JsonUtil序列化
     */
    public <T> T getAndSet(String key, Object newValue, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(newValue);
            String oldValue = redisTemplate.opsForValue().getAndSet(fullKey, jsonValue);
            return oldValue != null ? JsonUtil.fromJson(oldValue, clazz) : null;
        } catch (Exception e) {
            log.error("获取并设置失败: key={}, error={}", fullKey, e.getMessage());
            return null;
        }
    }

    /**
     * 获取过期时间 - 委托给RedisUtils
     */
    public Long getExpire(String key) {
        return redisUtils.getExpire(key);
    }

    /**
     * 设置过期时间 - 委托给RedisUtils
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisUtils.expire(key, timeout, unit);
    }

    /**
     * 移除过期时间
     */
    public Boolean persist(String key) {
        String fullKey = buildKey(key);
        return redisTemplate.persist(fullKey);
    }

    /**
     * 判断是否存在 - 委托给RedisUtils
     */
    public Boolean hasKey(String key) {
        return redisUtils.hasKey(key);
    }

    // =========================== Hash操作 ===========================

    /**
     * Hash设置字段
     */
    public void hashSet(String key, String field, Object value) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            redisTemplate.opsForHash().put(fullKey, field, jsonValue);
        } catch (Exception e) {
            log.error("Hash设置字段失败: key={}, field={}, error={}", fullKey, field, e.getMessage());
        }
    }

    /**
     * Hash获取字段
     */
    public <T> T hashGet(String key, String field, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            Object value = redisTemplate.opsForHash().get(fullKey, field);
            return value != null ? JsonUtil.fromJson(value.toString(), clazz) : null;
        } catch (Exception e) {
            log.error("Hash获取字段失败: key={}, field={}, error={}", fullKey, field, e.getMessage());
            return null;
        }
    }

    /**
     * Hash删除字段
     */
    public Long hashDelete(String key, String... fields) {
        String fullKey = buildKey(key);
        return redisTemplate.opsForHash().delete(fullKey, (Object[]) fields);
    }

    /**
     * Hash获取所有字段
     */
    public <T> Map<String, T> hashGetAll(String key, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(fullKey);
            Map<String, T> result = new HashMap<>();

            entries.forEach((field, value) -> {
                try {
                    T deserializedValue = JsonUtil.fromJson(value.toString(), clazz);
                    result.put(field.toString(), deserializedValue);
                } catch (Exception e) {
                    log.error("Hash反序列化字段失败: key={}, field={}", fullKey, field);
                }
            });

            return result;
        } catch (Exception e) {
            log.error("Hash获取所有字段失败: key={}, error={}", fullKey, e.getMessage());
            return new HashMap<>();
        }
    }

    // =========================== List操作 ===========================

    /**
     * List左侧推入
     */
    public Long listLeftPush(String key, Object value) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            return redisTemplate.opsForList().leftPush(fullKey, jsonValue);
        } catch (Exception e) {
            log.error("List左侧推入失败: key={}, error={}", fullKey, e.getMessage());
            return 0L;
        }
    }

    /**
     * List右侧推入
     */
    public Long listRightPush(String key, Object value) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            return redisTemplate.opsForList().rightPush(fullKey, jsonValue);
        } catch (Exception e) {
            log.error("List右侧推入失败: key={}, error={}", fullKey, e.getMessage());
            return 0L;
        }
    }

    /**
     * List左侧弹出
     */
    public <T> T listLeftPop(String key, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            String value = redisTemplate.opsForList().leftPop(fullKey);
            return value != null ? JsonUtil.fromJson(value, clazz) : null;
        } catch (Exception e) {
            log.error("List左侧弹出失败: key={}, error={}", fullKey, e.getMessage());
            return null;
        }
    }

    /**
     * List范围获取
     */
    public <T> List<T> listRange(String key, long start, long end, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            List<String> values = redisTemplate.opsForList().range(fullKey, start, end);
            if (values == null) {
                return new ArrayList<>();
            }

            return values.stream()
                .map(value -> {
                    try {
                        return JsonUtil.fromJson(value, clazz);
                    } catch (Exception e) {
                        log.error("List元素反序列化失败: key={}, value={}", fullKey, value);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("List范围获取失败: key={}, error={}", fullKey, e.getMessage());
            return new ArrayList<>();
        }
    }

    // =========================== Set操作 ===========================

    /**
     * Set添加成员
     */
    public Long setAdd(String key, Object... values) {
        String fullKey = buildKey(key);
        try {
            String[] jsonValues = Arrays.stream(values)
                .map(value -> {
                    try {
                        return JsonUtil.toJson(value);
                    } catch (Exception e) {
                        log.error("Set成员序列化失败: value={}", value);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(String[]::new);

            return redisTemplate.opsForSet().add(fullKey, jsonValues);
        } catch (Exception e) {
            log.error("Set添加成员失败: key={}, error={}", fullKey, e.getMessage());
            return 0L;
        }
    }

    /**
     * Set获取所有成员
     */
    public <T> Set<T> setMembers(String key, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            Set<String> members = redisTemplate.opsForSet().members(fullKey);
            if (members == null) {
                return new HashSet<>();
            }

            return members.stream()
                .map(member -> {
                    try {
                        return JsonUtil.fromJson(member, clazz);
                    } catch (Exception e) {
                        log.error("Set成员反序列化失败: key={}, member={}", fullKey, member);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Set获取成员失败: key={}, error={}", fullKey, e.getMessage());
            return new HashSet<>();
        }
    }

    // =========================== ZSet操作 ===========================

    /**
     * ZSet添加成员
     */
    public Boolean zSetAdd(String key, Object value, double score) {
        String fullKey = buildKey(key);
        try {
            String jsonValue = JsonUtil.toJson(value);
            return redisTemplate.opsForZSet().add(fullKey, jsonValue, score);
        } catch (Exception e) {
            log.error("ZSet添加成员失败: key={}, error={}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * ZSet范围获取（按分数）
     */
    public <T> Set<T> zSetRangeByScore(String key, double min, double max, Class<T> clazz) {
        String fullKey = buildKey(key);
        try {
            Set<String> members = redisTemplate.opsForZSet().rangeByScore(fullKey, min, max);
            if (members == null) {
                return new LinkedHashSet<>();
            }

            return members.stream()
                .map(member -> {
                    try {
                        return JsonUtil.fromJson(member, clazz);
                    } catch (Exception e) {
                        log.error("ZSet成员反序列化失败: key={}, member={}", fullKey, member);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception e) {
            log.error("ZSet范围获取失败: key={}, error={}", fullKey, e.getMessage());
            return new LinkedHashSet<>();
        }
    }

    // =========================== 辅助方法 ===========================

    /**
     * 使用SCAN搜索键 - 替代keys命令
     */
    private Set<String> scanKeys(String pattern, int limit) {
        return redisUtils.scan(pattern, limit);
    }

    /**
     * 构建完整键名
     */
    private String buildKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Redis键不能为空");
        }
        return key.startsWith(keyPrefix) ? key : keyPrefix + key;
    }
}