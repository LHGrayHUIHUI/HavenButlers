package com.haven.common.config;

import com.haven.common.redis.DistributedLock;
import com.haven.common.redis.RedisCache;
import com.haven.common.redis.RedisUtils;
import com.haven.common.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * Common模块自动配置类
 *
 * @author HavenButler
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = "com.haven.common")
@EnableConfigurationProperties(CommonAutoConfiguration.CommonProperties.class)
public class CommonAutoConfiguration {

    private final CommonProperties properties;

    public CommonAutoConfiguration(CommonProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        log.info("Common公共组件库已加载，版本: {}", properties.getVersion());
        log.info("配置详情: Redis={}, Security={}, ThreadPool={}",
                properties.getRedis().isEnabled(),
                properties.getSecurity().isEnabled(),
                properties.getThreadPool().isEnabled());
    }

    /**
     * RestTemplate配置
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Redis模板配置
     */
    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Redis工具类
     */
    @Bean
    @ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public RedisUtils redisUtils() {
        log.info("注册Redis工具类");
        return new RedisUtils();
    }

    /**
     * Redis缓存管理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public RedisCache redisCache() {
        log.info("注册Redis缓存管理器");
        return new RedisCache();
    }

    /**
     * 分布式锁
     */
    @Bean
    @ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public DistributedLock distributedLock() {
        log.info("注册分布式锁");
        return new DistributedLock();
    }

    /**
     * JWT工具类
     */
    @Bean
    @ConditionalOnProperty(prefix = "common.security", name = "jwt-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public JwtUtils jwtUtils() {
        log.info("注册JWT工具类");
        return new JwtUtils();
    }

    /**
     * 配置属性类
     */
    @ConfigurationProperties(prefix = "common")
    public static class CommonProperties {

        private String version = "1.0.0";
        private RedisProperties redis = new RedisProperties();
        private SecurityProperties security = new SecurityProperties();
        private ThreadPoolProperties threadPool = new ThreadPoolProperties();
        private MqProperties mq = new MqProperties();

        // Getters and Setters
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public RedisProperties getRedis() {
            return redis;
        }

        public void setRedis(RedisProperties redis) {
            this.redis = redis;
        }

        public SecurityProperties getSecurity() {
            return security;
        }

        public void setSecurity(SecurityProperties security) {
            this.security = security;
        }

        public ThreadPoolProperties getThreadPool() {
            return threadPool;
        }

        public void setThreadPool(ThreadPoolProperties threadPool) {
            this.threadPool = threadPool;
        }

        public MqProperties getMq() {
            return mq;
        }

        public void setMq(MqProperties mq) {
            this.mq = mq;
        }

        /**
         * Redis配置
         */
        public static class RedisProperties {
            private boolean enabled = true;
            private long defaultTimeout = 3600;
            private String keyPrefix = "haven:";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public long getDefaultTimeout() {
                return defaultTimeout;
            }

            public void setDefaultTimeout(long defaultTimeout) {
                this.defaultTimeout = defaultTimeout;
            }

            public String getKeyPrefix() {
                return keyPrefix;
            }

            public void setKeyPrefix(String keyPrefix) {
                this.keyPrefix = keyPrefix;
            }
        }

        /**
         * 安全配置
         */
        public static class SecurityProperties {
            private boolean enabled = true;
            private boolean jwtEnabled = true;
            private long jwtExpiration = 86400000;
            private String jwtSecret = "HavenButlerSecret";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public boolean isJwtEnabled() {
                return jwtEnabled;
            }

            public void setJwtEnabled(boolean jwtEnabled) {
                this.jwtEnabled = jwtEnabled;
            }

            public long getJwtExpiration() {
                return jwtExpiration;
            }

            public void setJwtExpiration(long jwtExpiration) {
                this.jwtExpiration = jwtExpiration;
            }

            public String getJwtSecret() {
                return jwtSecret;
            }

            public void setJwtSecret(String jwtSecret) {
                this.jwtSecret = jwtSecret;
            }
        }

        /**
         * 线程池配置
         */
        public static class ThreadPoolProperties {
            private boolean enabled = true;
            private int corePoolSize = 10;
            private int maxPoolSize = 50;
            private int queueCapacity = 100;
            private int keepAliveSeconds = 60;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getCorePoolSize() {
                return corePoolSize;
            }

            public void setCorePoolSize(int corePoolSize) {
                this.corePoolSize = corePoolSize;
            }

            public int getMaxPoolSize() {
                return maxPoolSize;
            }

            public void setMaxPoolSize(int maxPoolSize) {
                this.maxPoolSize = maxPoolSize;
            }

            public int getQueueCapacity() {
                return queueCapacity;
            }

            public void setQueueCapacity(int queueCapacity) {
                this.queueCapacity = queueCapacity;
            }

            public int getKeepAliveSeconds() {
                return keepAliveSeconds;
            }

            public void setKeepAliveSeconds(int keepAliveSeconds) {
                this.keepAliveSeconds = keepAliveSeconds;
            }
        }

        /**
         * 消息队列配置
         */
        public static class MqProperties {
            private boolean enabled = false;
            private String exchangePrefix = "haven.";
            private String queuePrefix = "haven.";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getExchangePrefix() {
                return exchangePrefix;
            }

            public void setExchangePrefix(String exchangePrefix) {
                this.exchangePrefix = exchangePrefix;
            }

            public String getQueuePrefix() {
                return queuePrefix;
            }

            public void setQueuePrefix(String queuePrefix) {
                this.queuePrefix = queuePrefix;
            }
        }
    }
}