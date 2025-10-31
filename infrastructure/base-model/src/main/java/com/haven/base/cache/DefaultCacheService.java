package com.haven.base.cache;

import com.haven.base.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// 移除@Component注解，改由BaseModelAutoConfiguration中@Bean方式注册

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认缓存服务实现
 * 基于内存的简单缓存实现，适合开发和测试环境
 * 生产环境建议使用Redis等专业缓存组件
 *
 * @author HavenButler
 */
// 移除@Component注解，改由BaseModelAutoConfiguration中@Bean方式注册
public class DefaultCacheService implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCacheService.class);

    /**
     * 缓存存储
     */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 统计信息
     */
    private long hitCount = 0;
    private long missCount = 0;

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        long expireTime = ttl != null ? System.currentTimeMillis() + ttl.toMillis() : -1;
        cache.put(key, new CacheEntry(value, expireTime));
        log.debug("缓存设置: key={}, ttl={}", key, ttl);
    }

    @Override
    public <T> void set(String key, T value) {
        set(key, value, null);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            missCount++;
            log.debug("缓存未命中: key={}", key);
            return Optional.empty();
        }

        if (entry.isExpired()) {
            cache.remove(key);
            missCount++;
            log.debug("缓存已过期: key={}", key);
            return Optional.empty();
        }

        hitCount++;
        log.debug("缓存命中: key={}", key);

        try {
            if (type.isInstance(entry.getValue())) {
                return Optional.of(type.cast(entry.getValue()));
            }

            // 尝试JSON转换
            if (entry.getValue() instanceof String && !type.equals(String.class)) {
                T converted = JsonUtil.fromJson((String) entry.getValue(), type);
                return Optional.ofNullable(converted);
            }

            return Optional.of(type.cast(entry.getValue()));
        } catch (Exception e) {
            log.warn("缓存值类型转换失败: key={}, type={}, error={}", key, type, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
        log.debug("缓存删除: key={}", key);
    }

    @Override
    public void delete(String... keys) {
        for (String key : keys) {
            delete(key);
        }
    }

    @Override
    public boolean exists(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            return false;
        }

        long expireTime = ttl != null ? System.currentTimeMillis() + ttl.toMillis() : -1;
        cache.put(key, new CacheEntry(entry.getValue(), expireTime));
        return true;
    }

    @Override
    public long getExpire(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return -2; // 键不存在
        }
        if (entry.getExpireTime() == -1) {
            return -1; // 永不过期
        }
        long remaining = (entry.getExpireTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }

    @Override
    public long increment(String key, long delta) {
        CacheEntry entry = cache.get(key);
        long currentValue = 0;

        if (entry != null && !entry.isExpired()) {
            if (entry.getValue() instanceof Number) {
                currentValue = ((Number) entry.getValue()).longValue();
            }
        }

        long newValue = currentValue + delta;
        set(key, newValue);
        return newValue;
    }

    @Override
    public long increment(String key) {
        return increment(key, 1);
    }

    @Override
    public long decrement(String key, long delta) {
        return increment(key, -delta);
    }

    @Override
    public <T> Map<String, T> multiGet(List<String> keys, Class<T> type) {
        Map<String, T> result = new HashMap<>();
        for (String key : keys) {
            get(key, type).ifPresent(value -> result.put(key, value));
        }
        return result;
    }

    @Override
    public <T> void multiSet(Map<String, T> keyValueMap, Duration ttl) {
        keyValueMap.forEach((key, value) -> set(key, value, ttl));
    }

    @Override
    public <T> long setAdd(String key, T... values) {
        Set<T> set = getOrCreateSet(key);
        int originalSize = set.size();
        Collections.addAll(set, values);
        cache.put(key, new CacheEntry(set, -1));
        return set.size() - originalSize;
    }

    @Override
    public <T> long setRemove(String key, T... values) {
        Set<T> set = getOrCreateSet(key);
        int originalSize = set.size();
        for (T value : values) {
            set.remove(value);
        }
        cache.put(key, new CacheEntry(set, -1));
        return originalSize - set.size();
    }

    @Override
    public <T> Set<T> setMembers(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            return new HashSet<>();
        }

        if (entry.getValue() instanceof Set) {
            return (Set<T>) entry.getValue();
        }

        return new HashSet<>();
    }

    @Override
    public <T> boolean setIsMember(String key, T value) {
        Set<T> set = setMembers(key, (Class<T>) value.getClass());
        return set.contains(value);
    }

    @Override
    public <T> long listLeftPush(String key, T... values) {
        List<T> list = getOrCreateList(key);
        Collections.addAll(list, values);
        cache.put(key, new CacheEntry(list, -1));
        return list.size();
    }

    @Override
    public <T> long listRightPush(String key, T... values) {
        List<T> list = getOrCreateList(key);
        for (int i = values.length - 1; i >= 0; i--) {
            list.add(0, values[i]);
        }
        cache.put(key, new CacheEntry(list, -1));
        return list.size();
    }

    @Override
    public <T> Optional<T> listLeftPop(String key, Class<T> type) {
        List<T> list = getOrCreateList(key);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        T value = list.remove(0);
        cache.put(key, new CacheEntry(list, -1));
        return Optional.ofNullable(value);
    }

    @Override
    public <T> Optional<T> listRightPop(String key, Class<T> type) {
        List<T> list = getOrCreateList(key);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        T value = list.remove(list.size() - 1);
        cache.put(key, new CacheEntry(list, -1));
        return Optional.ofNullable(value);
    }

    @Override
    public <T> List<T> listRange(String key, long start, long end, Class<T> type) {
        List<T> list = getOrCreateList(key);
        int size = list.size();

        if (start < 0) start = size + start;
        if (end < 0) end = size + end;

        start = Math.max(0, start);
        end = Math.min(size - 1, end);

        if (start > end) {
            return new ArrayList<>();
        }

        return list.subList((int) start, (int) end + 1);
    }

    @Override
    public <T> void hashSet(String key, String field, T value) {
        Map<String, T> hash = getOrCreateHash(key);
        hash.put(field, value);
        cache.put(key, new CacheEntry(hash, -1));
    }

    @Override
    public <T> Optional<T> hashGet(String key, String field, Class<T> type) {
        Map<String, T> hash = getOrCreateHash(key);
        T value = hash.get(field);
        return Optional.ofNullable(value);
    }

    @Override
    public long hashDelete(String key, String... fields) {
        Map<String, Object> hash = getOrCreateHash(key);
        long deletedCount = 0;
        for (String field : fields) {
            if (hash.remove(field) != null) {
                deletedCount++;
            }
        }
        cache.put(key, new CacheEntry(hash, -1));
        return deletedCount;
    }

    @Override
    public <T> Map<String, T> hashGetAll(String key, Class<T> type) {
        return getOrCreateHash(key);
    }

    @Override
    public Set<String> keys(String pattern) {
        // 简单的通配符匹配
        String regex = pattern.replace("*", ".*").replace("?", ".");
        return cache.keySet().stream()
                .filter(key -> key.matches(regex))
                .collect(Collectors.toSet());
    }

    @Override
    public long deleteByPattern(String pattern) {
        Set<String> keysToDelete = keys(pattern);
        keysToDelete.forEach(this::delete);
        return keysToDelete.size();
    }

    @Override
    public void clear() {
        cache.clear();
        hitCount = 0;
        missCount = 0;
        log.info("缓存已清空");
    }

    @Override
    public CacheStats getStats() {
        CacheStats stats = new CacheStats();
        stats.setHitCount(hitCount);
        stats.setMissCount(missCount);
        stats.setTotalCount(hitCount + missCount);
        stats.setHitRate(stats.getTotalCount() > 0 ? (double) hitCount / stats.getTotalCount() : 0);
        stats.setKeyCount(cache.size());
        stats.setMemoryUsage(0); // 内存缓存难以准确计算
        return stats;
    }

    // 辅助方法

    @SuppressWarnings("unchecked")
    private <T> Set<T> getOrCreateSet(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired() && entry.getValue() instanceof Set) {
            return (Set<T>) entry.getValue();
        }
        return new HashSet<>();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getOrCreateList(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired() && entry.getValue() instanceof List) {
            return (List<T>) entry.getValue();
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> getOrCreateHash(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired() && entry.getValue() instanceof Map) {
            return (Map<String, T>) entry.getValue();
        }
        return new HashMap<>();
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final Object value;
        private final long expireTime;

        public CacheEntry(Object value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public Object getValue() {
            return value;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public boolean isExpired() {
            return expireTime != -1 && System.currentTimeMillis() > expireTime;
        }
    }
}