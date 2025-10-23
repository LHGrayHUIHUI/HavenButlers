package com.haven.base.config;

import com.haven.base.aspect.*;
import com.haven.base.cache.*;
import com.haven.base.client.*;
import com.haven.base.common.exception.GlobalExceptionHandler;
import com.haven.base.config.*;
import com.haven.base.configuration.DefaultDynamicConfigManager;
import com.haven.base.interceptor.TraceIdInterceptor;
import com.haven.base.lock.*;
import com.haven.base.messaging.*;
import com.haven.base.monitor.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;

/**
 * BaseModel自动配置类
 * 提供基础功能的自动配置，包括：
 * - TraceID拦截器：为每个请求生成链路追踪ID
 * - 全局异常处理器：统一异常响应格式
 * - 日志追踪切面：自动记录方法执行日志
 * - 基础组件扫描：自动注册工具类和模型类
 *
 * 使用方式：
 * <pre>{@code @Import(BaseModelAutoConfiguration.class)}</pre>
 * 或通过Spring Boot 3的AutoConfiguration.imports自动配置
 *
 * @author HavenButler
 */
@Slf4j
@Configuration
// 移除ComponentScan，改用@Bean方式注册避免Bean冲突
@EnableConfigurationProperties({BaseModelAutoConfiguration.BaseModelProperties.class, ServiceClientProperties.class})
public class BaseModelAutoConfiguration implements WebMvcConfigurer {

    private final BaseModelProperties properties;

    public BaseModelAutoConfiguration(BaseModelProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        log.info("BaseModel自动配置已加载，版本: {}", properties.getVersion());
        log.info("配置详情: TraceID={}, 全局异常处理={}, 日志追踪={}",
                properties.getTrace().isEnabled(),
                properties.getException().isEnabled(),
                properties.getLog().isEnabled());
    }

    /**
     * TraceID拦截器
     * 为每个HTTP请求自动生成TraceID
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "base-model.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TraceIdInterceptor traceIdInterceptor() {
        log.info("注册TraceID拦截器");
        return new TraceIdInterceptor();
    }

    /**
     * 全局异常处理器
     * 统一处理所有未捕获异常并返回标准格式响应
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "base-model.exception", name = "enabled", havingValue = "true", matchIfMissing = true)
    
    public GlobalExceptionHandler globalExceptionHandler() {
        log.info("注册全局异常处理器");
        return new GlobalExceptionHandler();
    }

    /**
     * 日志追踪切面
     * 支持@TraceLog注解的方法自动记录执行日志
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "base-model.log", name = "enabled", havingValue = "true", matchIfMissing = true)
    
    public TraceLogAspect traceLogAspect() {
        log.info("注册日志追踪切面");
        return new TraceLogAspect();
    }

    /**
     * RestTemplate Bean
     * 支持超时和连接池配置
     */
    @Bean
    @ConditionalOnMissingBean

    public RestTemplate restTemplate(ServiceClientProperties serviceClientProperties) {
        ServiceClientProperties.ConnectionPool poolConfig = serviceClientProperties.getConnectionPool();

        log.info("注册RestTemplate，超时配置: connect={}ms, read={}ms, 连接池配置: maxTotal={}, maxPerRoute={}",
                serviceClientProperties.getConnectTimeout(),
                serviceClientProperties.getReadTimeout(),
                poolConfig.getMaxTotal(),
                poolConfig.getMaxPerRoute());

        try {
            // 创建连接池管理器
            org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager connectionManager =
                    new org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager();

            // 设置连接池参数
            connectionManager.setMaxTotal(poolConfig.getMaxTotal());
            connectionManager.setDefaultMaxPerRoute(poolConfig.getMaxPerRoute());
            // 使用新的方法设置连接验证
            connectionManager.setValidateAfterInactivity(
                    org.apache.hc.core5.util.TimeValue.ofMilliseconds(poolConfig.getValidateAfterInactivity()));

            // 创建请求配置
            org.apache.hc.client5.http.config.RequestConfig requestConfig =
                    org.apache.hc.client5.http.config.RequestConfig.custom()
                            .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(serviceClientProperties.getConnectTimeout()))
                            .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(serviceClientProperties.getReadTimeout()))
                            .build();

            // 创建HttpClient
            org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient =
                    org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                            .setConnectionManager(connectionManager)
                            .setDefaultRequestConfig(requestConfig)
                            .evictIdleConnections(org.apache.hc.core5.util.TimeValue.ofSeconds(poolConfig.getIdleTimeout()))
                            .build();

            // 创建带连接池的RequestFactory
            org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory =
                    new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(factory);

            log.info("RestTemplate连接池配置成功");
            return restTemplate;

        } catch (Exception e) {
            log.warn("RestTemplate连接池配置失败，使用默认配置: {}", e.getMessage());

            // 降级到简单超时配置
            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory =
                    new org.springframework.http.client.HttpComponentsClientHttpRequestFactory();
            factory.setConnectTimeout(serviceClientProperties.getConnectTimeout());
            factory.setConnectionRequestTimeout(serviceClientProperties.getReadTimeout());
            restTemplate.setRequestFactory(factory);
            return restTemplate;
        }
    }

    // ========== 微服务架构组件 ==========

    /**
     * 服务发现组件
     */
    @Bean
    @ConditionalOnMissingBean
    
    public ServiceDiscovery serviceDiscovery() {
        log.info("注册默认服务发现组件");
        return new DefaultServiceDiscovery();
    }

    /**
     * 服务调用客户端
     */
    @Bean
    @ConditionalOnMissingBean
    
    @ConditionalOnProperty(prefix = "base-model.service-client", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ServiceClient serviceClient(RestTemplate restTemplate, ServiceDiscovery serviceDiscovery,
                                      ServiceClientProperties serviceClientProperties) {
        log.info("注册服务调用客户端");
        return new ServiceClient(restTemplate, serviceDiscovery, serviceClientProperties);
    }

    /**
     * 缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    
    @ConditionalOnProperty(prefix = "base-model.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheService cacheService() {
        log.info("注册默认缓存服务（内存实现）");
        return new DefaultCacheService();
    }

    /**
     * 分布式锁
     */
    @Bean
    @ConditionalOnMissingBean
    
    @ConditionalOnProperty(prefix = "base-model.distributed-lock", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DistributedLock distributedLock(CacheService cacheService) {
        log.info("注册分布式锁服务");
        return new DefaultDistributedLock(cacheService);
    }

    /**
     * 动态配置管理
     */
    @Bean
    @ConditionalOnMissingBean
    
    @ConditionalOnProperty(prefix = "base-model.dynamic-config", name = "enabled", havingValue = "true", matchIfMissing = true)
    public com.haven.base.configuration.DynamicConfigManager dynamicConfigManager(org.springframework.core.env.Environment environment) {
        log.info("注册动态配置管理器");
        return new DefaultDynamicConfigManager(environment);
    }

    /**
     * 指标收集器
     */
    @Bean
    @ConditionalOnMissingBean
    
    @ConditionalOnProperty(prefix = "base-model.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public com.haven.base.monitor.MetricsCollector metricsCollector() {
        log.info("注册指标收集器");
        return new DefaultMetricsCollector();
    }

    /**
     * 消息生产者
     */
    @Bean
    @ConditionalOnMissingBean
    
    @ConditionalOnProperty(prefix = "base-model.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MessageProducer messageProducer() {
        log.info("注册默认消息生产者");
        return new DefaultMessageProducer();
    }

    // ========== 切面组件 ==========

    /**
     * 限流切面
     */
    @Bean
    @ConditionalOnMissingBean
    
    @ConditionalOnProperty(prefix = "base-model.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitAspect rateLimitAspect(CacheService cacheService) {
        log.info("注册限流切面");
        return new RateLimitAspect(cacheService);
    }

    /**
     * 权限校验切面
     */
    @Bean
    @ConditionalOnMissingBean
    
    @ConditionalOnProperty(prefix = "base-model.permission", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PermissionAspect permissionAspect() {
        log.info("注册权限校验切面");
        return new PermissionAspect();
    }

    /**
     * 加密通知
     */
    @Bean
    @ConditionalOnMissingBean
    
    @ConditionalOnProperty(prefix = "base-model.encrypt", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EncryptAdvice encryptAdvice(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        log.info("注册加密响应通知");
        return new EncryptAdvice(objectMapper);
    }

    /**
     * 注册TraceID拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (properties.getTrace().isEnabled()) {
            registry.addInterceptor(traceIdInterceptor())
                    .addPathPatterns("/**")
                    .excludePathPatterns(properties.getTrace().getExcludePaths())
                    .order(1);
        }
    }

    /**
     * 配置属性类
     */
    @ConfigurationProperties(prefix = "base-model")
    public static class BaseModelProperties {

        /**
         * 版本号
         */
        private String version = "1.0.0";

        /**
         * TraceID配置
         */
        private TraceProperties trace = new TraceProperties();

        /**
         * 异常处理配置
         */
        private ExceptionProperties exception = new ExceptionProperties();

        /**
         * 日志配置
         */
        private LogProperties log = new LogProperties();

        /**
         * 响应配置
         */
        private ResponseProperties response = new ResponseProperties();

        /**
         * 加密配置
         */
        private EncryptProperties encrypt = new EncryptProperties();

        // Getters and Setters
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public TraceProperties getTrace() {
            return trace;
        }

        public void setTrace(TraceProperties trace) {
            this.trace = trace;
        }

        public ExceptionProperties getException() {
            return exception;
        }

        public void setException(ExceptionProperties exception) {
            this.exception = exception;
        }

        public LogProperties getLog() {
            return log;
        }

        public void setLog(LogProperties log) {
            this.log = log;
        }

        public ResponseProperties getResponse() {
            return response;
        }

        public void setResponse(ResponseProperties response) {
            this.response = response;
        }

        public EncryptProperties getEncrypt() {
            return encrypt;
        }

        public void setEncrypt(EncryptProperties encrypt) {
            this.encrypt = encrypt;
        }

        /**
         * TraceID配置
         */
        public static class TraceProperties {
            private boolean enabled = true;
            private String prefix = "tr";
            private String[] excludePaths = {"/health", "/actuator/**"};

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getPrefix() {
                return prefix;
            }

            public void setPrefix(String prefix) {
                this.prefix = prefix;
            }

            public String[] getExcludePaths() {
                return excludePaths;
            }

            public void setExcludePaths(String[] excludePaths) {
                this.excludePaths = excludePaths;
            }
        }

        /**
         * 异常处理配置
         */
        public static class ExceptionProperties {
            private boolean enabled = true;
            private boolean includeStackTrace = false;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public boolean isIncludeStackTrace() {
                return includeStackTrace;
            }

            public void setIncludeStackTrace(boolean includeStackTrace) {
                this.includeStackTrace = includeStackTrace;
            }
        }

        /**
         * 日志配置
         */
        public static class LogProperties {
            private boolean enabled = true;
            private String level = "INFO";
            private String format = "JSON";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getLevel() {
                return level;
            }

            public void setLevel(String level) {
                this.level = level;
            }

            public String getFormat() {
                return format;
            }

            public void setFormat(String format) {
                this.format = format;
            }
        }

        /**
         * 响应配置
         */
        public static class ResponseProperties {
            private boolean includeTimestamp = true;
            private boolean includeTraceId = true;

            public boolean isIncludeTimestamp() {
                return includeTimestamp;
            }

            public void setIncludeTimestamp(boolean includeTimestamp) {
                this.includeTimestamp = includeTimestamp;
            }

            public boolean isIncludeTraceId() {
                return includeTraceId;
            }

            public void setIncludeTraceId(boolean includeTraceId) {
                this.includeTraceId = includeTraceId;
            }
        }

        /**
         * 加密配置
         */
        public static class EncryptProperties {
            private boolean enabled = true;
            private String algorithm = "AES";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getAlgorithm() {
                return algorithm;
            }

            public void setAlgorithm(String algorithm) {
                this.algorithm = algorithm;
            }
        }
    }
}