package com.haven.common.redis;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis缓存管理器
 * 提供高级缓存操作功能
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class RedisCache {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 批量设置缓存
     */
    public void setBatch(Map<String, Object> map, long timeout, TimeUnit unit) {
        map.forEach((key, value) -> {
            set(key, value, timeout, unit);
        });
    }

    /**
     * 批量获取缓存
     */
    public List<String> getBatch(Collection<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 批量删除
     */
    public Long deleteBatch(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 模糊删除（慎用）
     */
    public Long deletePattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            return redisTemplate.delete(keys);
        }
        return 0L;
    }

    /**
     * 设置值
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        String jsonValue = JSON.toJSONString(value);
        redisTemplate.opsForValue().set(key, jsonValue, timeout, unit);
    }

    /**
     * 设置值（如果不存在）
     */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        String jsonValue = JSON.toJSONString(value);
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(key, jsonValue, timeout, unit)
        );
    }

    /**
     * 获取值
     */
    public <T> T get(String key, Class<T> clazz) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? JSON.parseObject(value, clazz) : null;
    }

    /**
     * 获取并删除
     */
    public <T> T getAndDelete(String key, Class<T> clazz) {
        String value = redisTemplate.opsForValue().getAndDelete(key);
        return value != null ? JSON.parseObject(value, clazz) : null;
    }

    /**
     * 获取并设置
     */
    public <T> T getAndSet(String key, Object newValue, Class<T> clazz) {
        String jsonValue = JSON.toJSONString(newValue);
        String oldValue = redisTemplate.opsForValue().getAndSet(key, jsonValue);
        return oldValue != null ? JSON.parseObject(oldValue, clazz) : null;
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 移除过期时间
     */
    public Boolean persist(String key) {
        return redisTemplate.persist(key);
    }

    /**
     * 判断是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 递增
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * Hash操作：设置值
     */
    public void hashSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, JSON.toJSONString(value));
    }

    /**
     * Hash操作：获取值
     */
    public <T> T hashGet(String key, String field, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value != null) {
            return JSON.parseObject(value.toString(), clazz);
        }
        return null;
    }

    /**
     * Hash操作：批量设置
     */
    public void hashSetAll(String key, Map<String, Object> map) {
        Map<String, String> stringMap = map.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> JSON.toJSONString(e.getValue())
            ));
        redisTemplate.opsForHash().putAll(key, stringMap);
    }

    /**
     * Hash操作：获取所有
     */
    public Map<Object, Object> hashGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Hash操作：删除字段
     */
    public Long hashDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * Hash操作：判断字段是否存在
     */
    public Boolean hashHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * List操作：左推
     */
    public Long listLeftPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, JSON.toJSONString(value));
    }

    /**
     * List操作：右推
     */
    public Long listRightPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, JSON.toJSONString(value));
    }

    /**
     * List操作：左弹
     */
    public <T> T listLeftPop(String key, Class<T> clazz) {
        String value = redisTemplate.opsForList().leftPop(key);
        return value != null ? JSON.parseObject(value, clazz) : null;
    }

    /**
     * List操作：右弹
     */
    public <T> T listRightPop(String key, Class<T> clazz) {
        String value = redisTemplate.opsForList().rightPop(key);
        return value != null ? JSON.parseObject(value, clazz) : null;
    }

    /**
     * List操作：获取长度
     */
    public Long listSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * List操作：获取范围
     */
    public <T> List<T> listRange(String key, long start, long end, Class<T> clazz) {
        List<String> list = redisTemplate.opsForList().range(key, start, end);
        if (list != null) {
            return list.stream()
                .map(value -> JSON.parseObject(value, clazz))
                .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Set操作：添加
     */
    public Long setAdd(String key, Object... values) {
        String[] jsonValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            jsonValues[i] = JSON.toJSONString(values[i]);
        }
        return redisTemplate.opsForSet().add(key, jsonValues);
    }

    /**
     * Set操作：移除
     */
    public Long setRemove(String key, Object... values) {
        String[] jsonValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            jsonValues[i] = JSON.toJSONString(values[i]);
        }
        return redisTemplate.opsForSet().remove(key, (Object[]) jsonValues);
    }

    /**
     * Set操作：判断是否存在
     */
    public Boolean setIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, JSON.toJSONString(value));
    }

    /**
     * Set操作：获取所有成员
     */
    public <T> Set<T> setMembers(String key, Class<T> clazz) {
        Set<String> members = redisTemplate.opsForSet().members(key);
        if (members != null) {
            return members.stream()
                .map(value -> JSON.parseObject(value, clazz))
                .collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * Set操作：获取大小
     */
    public Long setSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * ZSet操作：添加
     */
    public Boolean zSetAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, JSON.toJSONString(value), score);
    }

    /**
     * ZSet操作：移除
     */
    public Long zSetRemove(String key, Object... values) {
        String[] jsonValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            jsonValues[i] = JSON.toJSONString(values[i]);
        }
        return redisTemplate.opsForZSet().remove(key, (Object[]) jsonValues);
    }

    /**
     * ZSet操作：获取排名
     */
    public Long zSetRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, JSON.toJSONString(value));
    }

    /**
     * ZSet操作：获取分数
     */
    public Double zSetScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, JSON.toJSONString(value));
    }

    /**
     * ZSet操作：按分数范围获取
     */
    public Set<String> zSetRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }
}