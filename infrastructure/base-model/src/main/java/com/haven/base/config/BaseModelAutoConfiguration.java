package com.haven.base.config;

import com.haven.base.aspect.TraceLogAspect;
import com.haven.base.common.exception.GlobalExceptionHandler;
import com.haven.base.interceptor.TraceIdInterceptor;
import com.haven.base.client.ServiceClient;
import com.haven.base.cache.CacheService;
import com.haven.base.lock.DistributedLock;
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
 * 或通过spring.factories自动配置
 *
 * @author HavenButler
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = "com.haven.base")
@EnableConfigurationProperties(BaseModelAutoConfiguration.BaseModelProperties.class)
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
    @ConditionalOnProperty(prefix = "base-model.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public TraceIdInterceptor traceIdInterceptor() {
        log.info("注册TraceID拦截器");
        return new TraceIdInterceptor();
    }

    /**
     * 全局异常处理器
     * 统一处理所有未捕获异常并返回标准格式响应
     */
    @Bean
    @ConditionalOnProperty(prefix = "base-model.exception", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        log.info("注册全局异常处理器");
        return new GlobalExceptionHandler();
    }

    /**
     * 日志追踪切面
     * 支持@TraceLog注解的方法自动记录执行日志
     */
    @Bean
    @ConditionalOnProperty(prefix = "base-model.log", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public TraceLogAspect traceLogAspect() {
        log.info("注册日志追踪切面");
        return new TraceLogAspect();
    }

    /**
     * RestTemplate Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        log.info("注册RestTemplate");
        return new RestTemplate();
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