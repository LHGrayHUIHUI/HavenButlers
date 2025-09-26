package com.haven.base.cache;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 缓存服务接口
 * 提供统一的缓存操作抽象，支持多种缓存实现（Redis、Caffeine、Ehcache等）
 *
 * @author HavenButler
 */
public interface CacheService {

    // ========== 基础缓存操作 ==========

    /**
     * 设置缓存
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     */
    <T> void set(String key, T value, Duration ttl);

    /**
     * 设置缓存（永不过期）
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    <T> void set(String key, T value);

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键列表
     */
    void delete(String... keys);

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置过期时间
     *
     * @param key 缓存键
     * @param ttl 过期时间
     * @return 是否设置成功
     */
    boolean expire(String key, Duration ttl);

    /**
     * 获取剩余过期时间
     *
     * @param key 缓存键
     * @return 剩余时间（秒），-1表示永不过期，-2表示键不存在
     */
    long getExpire(String key);

    // ========== 原子操作 ==========

    /**
     * 原子递增
     *
     * @param key 缓存键
     * @param delta 增量
     * @return 递增后的值
     */
    long increment(String key, long delta);

    /**
     * 原子递增（默认增量为1）
     *
     * @param key 缓存键
     * @return 递增后的值
     */
    long increment(String key);

    /**
     * 原子递减
     *
     * @param key 缓存键
     * @param delta 减量
     * @return 递减后的值
     */
    long decrement(String key, long delta);

    // ========== 批量操作 ==========

    /**
     * 批量获取
     *
     * @param keys 缓存键列表
     * @param type 值类型
     * @return 缓存值映射
     */
    <T> Map<String, T> multiGet(List<String> keys, Class<T> type);

    /**
     * 批量设置
     *
     * @param keyValueMap 键值映射
     * @param ttl 过期时间
     */
    <T> void multiSet(Map<String, T> keyValueMap, Duration ttl);

    // ========== 集合操作 ==========

    /**
     * 向Set中添加元素
     *
     * @param key 缓存键
     * @param values 值列表
     * @return 成功添加的元素数量
     */
    <T> long setAdd(String key, T... values);

    /**
     * 从Set中移除元素
     *
     * @param key 缓存键
     * @param values 值列表
     * @return 成功移除的元素数量
     */
    <T> long setRemove(String key, T... values);

    /**
     * 获取Set中所有元素
     *
     * @param key 缓存键
     * @param type 元素类型
     * @return 集合元素
     */
    <T> Set<T> setMembers(String key, Class<T> type);

    /**
     * 判断元素是否在Set中
     *
     * @param key 缓存键
     * @param value 值
     * @return 是否存在
     */
    <T> boolean setIsMember(String key, T value);

    // ========== 列表操作 ==========

    /**
     * 向List左侧添加元素
     *
     * @param key 缓存键
     * @param values 值列表
     * @return 列表长度
     */
    <T> long listLeftPush(String key, T... values);

    /**
     * 向List右侧添加元素
     *
     * @param key 缓存键
     * @param values 值列表
     * @return 列表长度
     */
    <T> long listRightPush(String key, T... values);

    /**
     * 从List左侧弹出元素
     *
     * @param key 缓存键
     * @param type 元素类型
     * @return 弹出的元素
     */
    <T> Optional<T> listLeftPop(String key, Class<T> type);

    /**
     * 从List右侧弹出元素
     *
     * @param key 缓存键
     * @param type 元素类型
     * @return 弹出的元素
     */
    <T> Optional<T> listRightPop(String key, Class<T> type);

    /**
     * 获取List指定范围的元素
     *
     * @param key 缓存键
     * @param start 开始索引
     * @param end 结束索引
     * @param type 元素类型
     * @return 元素列表
     */
    <T> List<T> listRange(String key, long start, long end, Class<T> type);

    // ========== 哈希操作 ==========

    /**
     * 向Hash中设置字段
     *
     * @param key 缓存键
     * @param field 字段名
     * @param value 字段值
     */
    <T> void hashSet(String key, String field, T value);

    /**
     * 从Hash中获取字段值
     *
     * @param key 缓存键
     * @param field 字段名
     * @param type 值类型
     * @return 字段值
     */
    <T> Optional<T> hashGet(String key, String field, Class<T> type);

    /**
     * 从Hash中删除字段
     *
     * @param key 缓存键
     * @param fields 字段名列表
     * @return 成功删除的字段数量
     */
    long hashDelete(String key, String... fields);

    /**
     * 获取Hash所有字段和值
     *
     * @param key 缓存键
     * @param type 值类型
     * @return 字段值映射
     */
    <T> Map<String, T> hashGetAll(String key, Class<T> type);

    // ========== 模式匹配 ==========

    /**
     * 按模式匹配键
     *
     * @param pattern 匹配模式（支持*和?通配符）
     * @return 匹配的键列表
     */
    Set<String> keys(String pattern);

    /**
     * 按模式删除缓存
     *
     * @param pattern 匹配模式
     * @return 删除的键数量
     */
    long deleteByPattern(String pattern);

    // ========== 管理操作 ==========

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    CacheStats getStats();

    /**
     * 缓存统计信息
     */
    class CacheStats {
        private long hitCount;      // 命中次数
        private long missCount;     // 未命中次数
        private long totalCount;    // 总访问次数
        private double hitRate;     // 命中率
        private long keyCount;      // 键总数
        private long memoryUsage;   // 内存使用（字节）

        // Getters and Setters
        public long getHitCount() { return hitCount; }
        public void setHitCount(long hitCount) { this.hitCount = hitCount; }

        public long getMissCount() { return missCount; }
        public void setMissCount(long missCount) { this.missCount = missCount; }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }

        public long getKeyCount() { return keyCount; }
        public void setKeyCount(long keyCount) { this.keyCount = keyCount; }

        public long getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
    }
}