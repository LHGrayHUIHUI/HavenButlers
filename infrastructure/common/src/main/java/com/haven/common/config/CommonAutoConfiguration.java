package com.haven.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;

/**
 * Common模块自动配置类 - 基于base-model规范
 * 支持starter化和配置桥接
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model自动装配规范
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = {
    "com.haven.common.aspect",
    "com.haven.common.cache",
    "com.haven.common.health",
    "com.haven.common.metrics",
    "com.haven.common.mq",
    "com.haven.common.redis",
    "com.haven.common.security",
    "com.haven.common.web",
    "com.haven.common.config"
})
@EnableConfigurationProperties({
    CommonAutoConfiguration.CommonProperties.class
})
public class CommonAutoConfiguration {

    public CommonAutoConfiguration() {
    }

    @PostConstruct
    public void init() {
        log.info("🚀 HavenButler Common模块已加载");
        log.info("   版本: 2.0.0");
        log.info("   特性: base-model规范对齐, starter化, 配置桥接");
        log.info("   组件: DistributedLock, RateLimit, MessageSender, RedisCache, RedisUtils");
    }

    /**
     * Redis模板配置 - 兼容配置
     */
    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "base-model.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }


    /**
     * Common模块配置属性类 - 简化版本
     */
    @ConfigurationProperties(prefix = "haven.common")
    public static class CommonProperties {

        private String version = "2.0.0";
        private boolean enableMetrics = true;
        private boolean enableHealthCheck = true;
        private boolean enableConfigBridge = true;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public boolean isEnableMetrics() {
            return enableMetrics;
        }

        public void setEnableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
        }

        public boolean isEnableHealthCheck() {
            return enableHealthCheck;
        }

        public void setEnableHealthCheck(boolean enableHealthCheck) {
            this.enableHealthCheck = enableHealthCheck;
        }

        public boolean isEnableConfigBridge() {
            return enableConfigBridge;
        }

        public void setEnableConfigBridge(boolean enableConfigBridge) {
            this.enableConfigBridge = enableConfigBridge;
        }
    }
}