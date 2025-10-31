package com.haven.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 多级缓存配置
 * 支持本地缓存(Caffeine)和分布式缓存(Redis)的多级缓存架构
 *
 * @author HavenButler
 */
@Data
@Component
@ConfigurationProperties(prefix = "base-model.cache")
public class CacheProperties {

    /**
     * 是否启用缓存
     */
    private boolean enabled = true;

    /**
     * 本地缓存配置
     */
    private LocalConfig local = new LocalConfig();

    /**
     * 分布式缓存配置
     */
    private DistributedConfig distributed = new DistributedConfig();

    /**
     * 缓存策略配置
     */
    private StrategyConfig strategy = new StrategyConfig();

    /**
     * 预定义缓存配置
     */
    private Map<String, CacheConfig> caches = new HashMap<>();

    @Data
    public static class LocalConfig {
        /**
         * 是否启用本地缓存
         */
        private boolean enabled = true;

        /**
         * 最大条目数
         */
        private long maximumSize = 10000L;

        /**
         * 写入后过期时间（秒）
         */
        private long expireAfterWrite = 3600L;

        /**
         * 访问后过期时间（秒）
         */
        private long expireAfterAccess = 1800L;

        /**
         * 刷新后过期时间（秒）
         */
        private long refreshAfterWrite = 1800L;

        /**
         * 初始容量
         */
        private int initialCapacity = 1000;

        /**
         * 并发级别
         */
        private int concurrencyLevel = 4;

        /**
         * 弱引用键
         */
        private boolean weakKeys = false;

        /**
         * 弱引用值
         */
        private boolean weakValues = false;

        /**
         * 软引用值
         */
        private boolean softValues = false;

        /**
         * 记录统计信息
         */
        private boolean recordStats = true;
    }

    @Data
    public static class DistributedConfig {
        /**
         * 是否启用分布式缓存
         */
        private boolean enabled = true;

        /**
         * 默认过期时间（秒）
         */
        private long defaultTtl = 3600L;

        /**
         * 最大空闲时间（秒）
         */
        private long maxIdleTime = 1800L;

        /**
         * 连接超时时间（毫秒）
         */
        private long connectTimeout = 5000L;

        /**
         * 读取超时时间（毫秒）
         */
        private long readTimeout = 3000L;

        /**
         * 写入超时时间（毫秒）
         */
        private long writeTimeout = 3000L;

        /**
         * 连接池配置
         */
        private PoolConfig pool = new PoolConfig();

        /**
         * Redis集群配置
         */
        private ClusterConfig cluster = new ClusterConfig();
    }

    @Data
    public static class PoolConfig {
        /**
         * 最大连接数
         */
        private int maxTotal = 200;

        /**
         * 最大空闲连接数
         */
        private int maxIdle = 50;

        /**
         * 最小空闲连接数
         */
        private int minIdle = 10;

        /**
         * 获取连接超时时间（毫秒）
         */
        private long maxWaitMillis = 5000L;

        /**
         * 验证连接是否有效
         */
        private boolean testOnBorrow = true;

        /**
         * 验证连接是否有效（空闲时）
         */
        private boolean testWhileIdle = true;

        /**
         * 验证间隔时间（毫秒）
         */
        private long timeBetweenEvictionRunsMillis = 60000L;
    }

    @Data
    public static class ClusterConfig {
        /**
         * 是否启用集群模式
         */
        private boolean enabled = false;

        /**
         * 集群节点
         */
        private String[] nodes = {};

        /**
         * 最大重定向次数
         */
        private int maxRedirects = 3;

        /**
         * 密码
         */
        private String password = "";

        /**
         * 数据库索引
         */
        private int database = 0;
    }

    @Data
    public static class StrategyConfig {
        /**
         * 默认缓存策略
         */
        private CacheStrategy defaultStrategy = CacheStrategy.CACHE_THROUGH;

        /**
         * 缓存穿透保护
         */
        private boolean enableCachePenetrationProtection = true;

        /**
         * 缓存击穿保护
         */
        private boolean enableCacheBreakdownProtection = true;

        /**
         * 缓存雪崩保护
         */
        private boolean enableCacheAvalancheProtection = true;

        /**
         * 空值缓存过期时间（秒）
         */
        private long nullValueTtl = 300L;

        /**
         * 热点数据预热
         */
        private boolean enableWarmUp = false;

        /**
         * 异步刷新缓存
         */
        private boolean enableAsyncRefresh = true;
    }

    @Data
    public static class CacheConfig {
        /**
         * 缓存名称
         */
        private String name;

        /**
         * 本地缓存配置
         */
        private LocalConfig local;

        /**
         * 分布式缓存配置
         */
        private DistributedConfig distributed;

        /**
         * 缓存策略
         */
        private CacheStrategy strategy;

        /**
         * 过期时间（秒）
         */
        private long ttl;

        /**
         * 最大条目数
         */
        private long maximumSize;
    }

    /**
     * 缓存策略枚举
     */
    public enum CacheStrategy {
        /**
         * 缓存穿透：先查缓存，没有则查数据库，然后写入缓存
         */
        CACHE_THROUGH,

        /**
         * 旁路缓存：应用程序维护缓存和数据库
         */
        CACHE_ASIDE,

        /**
         * 写穿缓存：同时更新缓存和数据库
         */
        WRITE_THROUGH,

        /**
         * 写回缓存：先更新缓存，异步更新数据库
         */
        WRITE_BACK,

        /**
         * 只读缓存：只从缓存读取数据
         */
        READ_ONLY
    }
}