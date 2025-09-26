package com.haven.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置桥接器 - 兼容性支持
 * 将旧的common.*配置映射到新的base-model.*配置
 * 并提供迁移警告提示
 *
 * @author HavenButler
 * @version 2.0.0 - 配置迁移桥接
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "haven.bridge")
public class ConfigurationBridge {

    private final Environment environment;

    // 配置映射表：旧键 -> 新键
    private static final Map<String, String> CONFIG_MAPPING = new HashMap<>();

    static {
        // Redis/缓存配置映射
        CONFIG_MAPPING.put("common.redis.enabled", "spring.data.redis.enabled");
        CONFIG_MAPPING.put("common.redis.host", "spring.data.redis.host");
        CONFIG_MAPPING.put("common.redis.port", "spring.data.redis.port");
        CONFIG_MAPPING.put("common.redis.database", "spring.data.redis.database");
        CONFIG_MAPPING.put("common.redis.password", "spring.data.redis.password");
        CONFIG_MAPPING.put("common.redis.timeout", "spring.data.redis.timeout");
        CONFIG_MAPPING.put("common.redis.key-prefix", "base-model.cache.key-prefix");
        CONFIG_MAPPING.put("common.redis.default-ttl", "base-model.cache.default-ttl");

        // MQ配置映射
        CONFIG_MAPPING.put("common.mq.enabled", "base-model.messaging.enabled");
        CONFIG_MAPPING.put("common.mq.host", "spring.rabbitmq.host");
        CONFIG_MAPPING.put("common.mq.port", "spring.rabbitmq.port");
        CONFIG_MAPPING.put("common.mq.username", "spring.rabbitmq.username");
        CONFIG_MAPPING.put("common.mq.password", "spring.rabbitmq.password");
        CONFIG_MAPPING.put("common.mq.virtual-host", "spring.rabbitmq.virtual-host");
        CONFIG_MAPPING.put("common.mq.timeout", "base-model.messaging.default-timeout");
        CONFIG_MAPPING.put("common.mq.retry-times", "base-model.messaging.max-retry");

        // 安全配置映射
        CONFIG_MAPPING.put("common.security.jwt-secret", "base-model.security.jwt-secret");
        CONFIG_MAPPING.put("common.security.jwt-expiration", "base-model.security.jwt-expiration");
        CONFIG_MAPPING.put("common.security.jwt-clock-skew", "base-model.security.jwt-clock-skew");

        // 分布式锁配置映射
        CONFIG_MAPPING.put("common.distributed-lock.timeout", "base-model.distributed-lock.default-timeout");
        CONFIG_MAPPING.put("common.distributed-lock.renew-interval", "base-model.distributed-lock.renew-interval");
        CONFIG_MAPPING.put("common.distributed-lock.auto-renew", "base-model.distributed-lock.auto-renew");

        // 线程池配置映射
        CONFIG_MAPPING.put("common.thread-pool.core-size", "spring.task.execution.pool.core-size");
        CONFIG_MAPPING.put("common.thread-pool.max-size", "spring.task.execution.pool.max-size");
        CONFIG_MAPPING.put("common.thread-pool.queue-capacity", "spring.task.execution.pool.queue-capacity");
        CONFIG_MAPPING.put("common.thread-pool.keep-alive", "spring.task.execution.pool.keep-alive");
    }

    public ConfigurationBridge(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void checkDeprecatedConfigurations() {
        log.info("🔍 开始检查已弃用的配置项...");

        boolean hasDeprecatedConfig = false;
        Map<String, String> foundDeprecated = new HashMap<>();

        for (Map.Entry<String, String> entry : CONFIG_MAPPING.entrySet()) {
            String oldKey = entry.getKey();
            String newKey = entry.getValue();

            if (environment.containsProperty(oldKey)) {
                hasDeprecatedConfig = true;
                String oldValue = environment.getProperty(oldKey);
                foundDeprecated.put(oldKey, newKey);

                log.warn("⚠️ 发现已弃用的配置: {} = {} -> 建议迁移到: {}",
                        oldKey, oldValue, newKey);
            }
        }

        if (hasDeprecatedConfig) {
            log.warn("📋 配置迁移建议:");
            foundDeprecated.forEach((oldKey, newKey) ->
                log.warn("   {} -> {}", oldKey, newKey));

            log.warn("🎯 迁移指南:");
            log.warn("   1. 请将上述配置更新到新的键名");
            log.warn("   2. 旧配置将在下一个主版本中移除");
            log.warn("   3. 详细迁移指南请参考: docs/configuration-migration.md");

        } else {
            log.info("✅ 未发现已弃用的配置项，配置检查通过");
        }
    }

    /**
     * 获取配置值，支持向后兼容
     */
    public String getConfigValue(String newKey) {
        // 先尝试新配置
        String value = environment.getProperty(newKey);
        if (value != null) {
            return value;
        }

        // 查找对应的旧配置
        for (Map.Entry<String, String> entry : CONFIG_MAPPING.entrySet()) {
            if (entry.getValue().equals(newKey)) {
                String oldKey = entry.getKey();
                String oldValue = environment.getProperty(oldKey);
                if (oldValue != null) {
                    log.warn("⚠️ 使用已弃用的配置: {} -> {}", oldKey, newKey);
                    return oldValue;
                }
            }
        }

        return null;
    }

    /**
     * 获取配置值，带默认值
     */
    public String getConfigValue(String newKey, String defaultValue) {
        String value = getConfigValue(newKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取配置映射表
     */
    public static Map<String, String> getConfigMapping() {
        return new HashMap<>(CONFIG_MAPPING);
    }
}