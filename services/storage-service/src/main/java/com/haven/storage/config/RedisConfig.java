package com.haven.storage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis缓存配置
 * <p>
 * 配置文件元数据缓存，提升文件访问性能：
 * - 文件元数据缓存（快速查找）
 * - 用户权限缓存（减少数据库查询）
 * - 分享链接缓存（提升访问速度）
 * - 统计数据缓存（减少聚合计算）
 *
 * @author HavenButler
 */
@Configuration
@EnableCaching
@ConditionalOnClass(RedisTemplate.class)
public class RedisConfig {

    @Value("${redis.host:localhost}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Value("${redis.password:}")
    private String redisPassword;

    @Value("${redis.database:0}")
    private int redisDatabase;

    /**
     * 从Nacos配置读取Redis连接参数
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);
            if (redisPassword != null && !redisPassword.isEmpty()) {
                config.setPassword(redisPassword);
            }
            config.setDatabase(redisDatabase);

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();

            return factory;
        } catch (Exception e) {
            // 如果连接失败，返回一个可以优雅处理错误的工厂
            RedisStandaloneConfiguration fallbackConfig = new RedisStandaloneConfiguration();
            fallbackConfig.setHostName("localhost");
            fallbackConfig.setPort(6379);
            return new LettuceConnectionFactory(fallbackConfig);
        }
    }

    /**
     * 配置 RedisTemplate
     * 使用字符串序列化器作为 key，JSON 序列化器作为 value
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 设置 key 的序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 设置 value 的序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 设置默认序列化器
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());

        // 初始化模板
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 缓存管理器配置
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 文件元数据缓存配置 - 7天过期
        RedisCacheConfiguration fileMetadataCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(7))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 用户权限缓存配置 - 1小时过期
        RedisCacheConfiguration userPermissionCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 分享链接缓存配置 - 根据分享过期时间动态设置
        RedisCacheConfiguration shareLinkCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1))  // 默认1天，实际根据分享设置动态调整
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 统计数据缓存配置 - 30分钟过期
        RedisCacheConfiguration statisticsCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1))
                        .serializeKeysWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                        .disableCachingNullValues())
                .withCacheConfiguration("fileMetadata", fileMetadataCacheConfig)
                .withCacheConfiguration("userPermissions", userPermissionCacheConfig)
                .withCacheConfiguration("shareLinks", shareLinkCacheConfig)
                .withCacheConfiguration("statistics", statisticsCacheConfig)
                .build();
    }
}