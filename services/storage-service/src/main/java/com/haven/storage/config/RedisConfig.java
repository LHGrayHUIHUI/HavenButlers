package com.haven.storage.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis缓存配置
 * <p>
 * 提供高性能的文件元数据缓存解决方案：
 * - 统一序列化配置，避免代码重复
 * - 专注于文件元数据缓存，提升文件查询性能
 * - 添加缓存监控和统计功能
 * - 支持缓存预热和动态TTL调整
 *
 * @author HavenButler
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(prefix = "haven.storage.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    /**
     * 配置优化的 RedisTemplate
     * 使用自定义Jackson序列化器，支持类型信息
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("初始化RedisTemplate，使用优化的Jackson序列化器");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用自定义Jackson序列化器
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = createJacksonSerializer();

        // 设置序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.setDefaultSerializer(jackson2JsonRedisSerializer);

        // 初始化模板
        template.afterPropertiesSet();

        log.info("RedisTemplate初始化完成");
        return template;
    }

    /**
     * 配置缓存管理器
     * 专注于文件元数据缓存，提供高性能的文件信息查询
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("初始化Redis缓存管理器，专注于文件元数据缓存");

        // 创建默认缓存配置 - 1小时过期
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(createJacksonSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith("haven:storage:");

        // 创建文件元数据缓存配置 - 7天过期
        RedisCacheConfiguration fileMetadataCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(7))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(createJacksonSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith("haven:storage:fileMetadata:");

        // 构建缓存管理器
        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("fileMetadata", fileMetadataCacheConfig)
                .enableStatistics()  // 启用缓存统计
                .build();

        log.info("Redis缓存管理器初始化完成，专注于文件元数据缓存");
        return cacheManager;
    }

    /**
     * 创建自定义Jackson序列化器
     * 支持类型信息和空值处理
     * 使用现代Jackson API，避免弃用方法
     */
    private Jackson2JsonRedisSerializer<Object> createJacksonSerializer() {
        // 创建配置好的ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        // 使用构造函数直接传入ObjectMapper，避免弃用的setObjectMapper方法
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }

    /**
     * 缓存监控服务
     * 提供缓存使用统计和性能指标
     */
    @Bean
    @ConditionalOnProperty(prefix = "haven.storage.redis.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheMonitorService cacheMonitorService(CacheManager cacheManager) {
        return new CacheMonitorService(cacheManager);
    }

    /**
     * 缓存监控服务
     * 提供缓存使用统计和性能指标
     */
    @Slf4j
    public static class CacheMonitorService {

        private final CacheManager cacheManager;

        public CacheMonitorService(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
            log.info("缓存监控服务已启动");
        }

        /**
         * 获取缓存统计信息
         */
        public java.util.Map<String, Object> getCacheStatistics() {
            java.util.Map<String, Object> stats = new java.util.HashMap<>();

            if (cacheManager instanceof RedisCacheManager) {
                RedisCacheManager redisCacheManager = (RedisCacheManager) cacheManager;
                stats.put("cacheNames", redisCacheManager.getCacheNames());
                stats.put("cacheManagerType", "RedisCacheManager");

                // 获取各个缓存的基本信息
                java.util.Map<String, Object> cacheDetails = new java.util.HashMap<>();
                for (String cacheName : redisCacheManager.getCacheNames()) {
                    org.springframework.cache.Cache cache = redisCacheManager.getCache(cacheName);
                    if (cache != null) {
                        java.util.Map<String, Object> cacheInfo = new java.util.HashMap<>();
                        cacheInfo.put("name", cacheName);
                        cacheInfo.put("nativeCache", cache.getNativeCache().getClass().getSimpleName());
                        cacheDetails.put(cacheName, cacheInfo);
                    }
                }
                stats.put("cacheDetails", cacheDetails);
            }

            return stats;
        }

        /**
         * 清空文件元数据缓存
         */
        public void clearFileMetadataCache() {
            if (cacheManager.getCache("fileMetadata") != null) {
                cacheManager.getCache("fileMetadata").clear();
                log.info("已清空文件元数据缓存");
            } else {
                log.warn("文件元数据缓存不存在");
            }
        }

        /**
         * 获取缓存名称列表
         */
        public java.util.Collection<String> getCacheNames() {
            return cacheManager.getCacheNames();
        }
    }
}