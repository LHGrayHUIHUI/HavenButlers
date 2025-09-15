# Common 公共组件库 开发指南

## 模块概述
Common公共组件库是HavenButler平台的基础设施，为所有Java服务提供统一的工具类、常量定义和公共组件。

## 开发规范

### 1. 模块划分原则
- **单一职责**：每个模块只负责一类功能
- **最小依赖**：减少模块间依赖
- **稳定性优先**：公共组件必须稳定可靠
- **向后兼容**：更新不能破坏现有功能

### 2. 代码组织结构

```java
// 常量类组织
package com.havenbutler.common.constants;

public final class CommonConstants {
    private CommonConstants() {
        throw new AssertionError("No instances for you!");
    }
    
    // 按功能分组
    public static final class System {
        public static final String NAME = "HavenButler";
        public static final String VERSION = "1.0.0";
    }
    
    public static final class Http {
        public static final String HEADER_TRACE_ID = "X-Trace-ID";
        public static final String HEADER_FAMILY_ID = "X-Family-ID";
    }
    
    public static final class Cache {
        public static final String PREFIX = "haven:";
        public static final long DEFAULT_TIMEOUT = 3600L;
    }
}
```

### 3. 异常处理规范

```java
package com.havenbutler.common.exception;

// 业务异常基类
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final String message;
    private final String traceId;
    private final Map<String, Object> data;
    
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }
    
    public BusinessException(ErrorCode errorCode, Map<String, Object> data) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.traceId = MDC.get("traceId");
        this.data = data;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.traceId = MDC.get("traceId");
        this.data = null;
    }
}

// 全局异常处理器
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseWrapper<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}, traceId={}", 
                 e.getCode(), e.getMessage(), e.getTraceId());
        return ResponseWrapper.error(e.getCode(), e.getMessage(), e.getData());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseWrapper<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors()
            .stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return ResponseWrapper.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseWrapper<Void> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ResponseWrapper.error(ErrorCode.INTERNAL_ERROR);
    }
}
```

### 4. 工具类开发规范

```java
package com.havenbutler.common.utils;

// 工具类必须是final类，私有构造函数
public final class DateUtils {
    private DateUtils() {
        throw new AssertionError();
    }
    
    // 线程安全的日期格式化
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    
    public static String format(Date date) {
        return date == null ? null : DATE_FORMAT.get().format(date);
    }
    
    public static Date parse(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        try {
            return DATE_FORMAT.get().parse(dateStr);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr, e);
        }
    }
    
    // 使用Java 8时间API
    public static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : 
               LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
```

### 5. 配置类规范

```java
package com.havenbutler.common.config;

@Configuration
@EnableConfigurationProperties
public class CommonAutoConfiguration {
    
    // 条件注入
    @Bean
    @ConditionalOnMissingBean
    public TraceIdGenerator traceIdGenerator() {
        return new TraceIdGenerator();
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "haven.redis", name = "enabled", havingValue = "true")
    public RedisUtils redisUtils(StringRedisTemplate redisTemplate) {
        return new RedisUtils(redisTemplate);
    }
    
    // 配置属性类
    @ConfigurationProperties(prefix = "haven.common")
    @Data
    public static class CommonProperties {
        private boolean enableTraceId = true;
        private boolean enableGlobalException = true;
        private int maxRetryTimes = 3;
        private long retryInterval = 1000L;
    }
}
```

### 6. 注解定义

```java
package com.havenbutler.common.annotation;

// 限流注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 限流键
     */
    String key() default "";
    
    /**
     * 时间窗口（秒）
     */
    int window() default 60;
    
    /**
     * 限流次数
     */
    int limit() default 100;
    
    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁";
}

// 限流切面
@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    @Autowired
    private RedisUtils redisUtils;
    
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = generateKey(point, rateLimit);
        
        // 使用Redis实现限流
        Long count = redisUtils.increment(key);
        if (count == 1) {
            redisUtils.expire(key, rateLimit.window());
        }
        
        if (count > rateLimit.limit()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, rateLimit.message());
        }
        
        return point.proceed();
    }
    
    private String generateKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        if (StringUtils.isNotBlank(rateLimit.key())) {
            return rateLimit.key();
        }
        
        // 默认使用类名+方法名
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = point.getSignature().getName();
        return String.format("rate_limit:%s:%s", className, methodName);
    }
}
```

### 7. 拦截器配置

```java
package com.havenbutler.common.interceptor;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    
    @Autowired
    private TraceIdInterceptor traceIdInterceptor;
    
    @Autowired
    private AuthInterceptor authInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // TraceID拦截器，所有路径
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**")
                .order(1);
        
        // 认证拦截器，排除公开路径
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/api/v1/account/login",
                    "/api/v1/account/register",
                    "/health",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                )
                .order(2);
    }
}
```

### 8. 分布式锁实现

```java
package com.havenbutler.common.lock;

@Component
@Slf4j
public class DistributedLock {
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 获取锁
     */
    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(key, value, timeout, unit);
        return Boolean.TRUE.equals(success);
    }
    
    /**
     * 释放锁
     */
    public boolean releaseLock(String key, String value) {
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(key),
            value
        );
        
        return Long.valueOf(1).equals(result);
    }
    
    /**
     * 带重试的锁获取
     */
    public boolean tryLockWithRetry(String key, String value, 
                                    long timeout, TimeUnit unit, 
                                    int maxRetries, long retryInterval) {
        int retries = 0;
        while (retries < maxRetries) {
            if (tryLock(key, value, timeout, unit)) {
                return true;
            }
            retries++;
            if (retries < maxRetries) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}
```

### 9. 消息队列工具

```java
package com.havenbutler.common.mq;

@Component
@Slf4j
public class MessageQueueUtils {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 发送消息
     */
    public void send(String exchange, String routingKey, Object message) {
        Message msg = MessageBuilder
            .withBody(JSON.toJSONBytes(message))
            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
            .setHeader("traceId", MDC.get("traceId"))
            .build();
        
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
        log.info("Send message to {}:{}, traceId: {}", 
                 exchange, routingKey, MDC.get("traceId"));
    }
    
    /**
     * 延迟消息
     */
    public void sendDelay(String exchange, String routingKey, 
                         Object message, long delay) {
        Message msg = MessageBuilder
            .withBody(JSON.toJSONBytes(message))
            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
            .setHeader("x-delay", delay)
            .setHeader("traceId", MDC.get("traceId"))
            .build();
        
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
    }
}
```

### 10. 单元测试

```java
package com.havenbutler.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TraceIdGeneratorTest {
    private TraceIdGenerator generator = new TraceIdGenerator();
    
    @Test
    void testGenerateTraceId() {
        String traceId = generator.generate();
        
        assertNotNull(traceId);
        assertTrue(traceId.startsWith("tr-"));
        assertEquals(26, traceId.length()); // tr- + 15位时间 + - + 6位随机
        
        // 测试唯一性
        String traceId2 = generator.generate();
        assertNotEquals(traceId, traceId2);
    }
}

class CryptoUtilsTest {
    @Test
    void testAESEncryption() {
        String data = "sensitive data";
        String key = "1234567890123456";
        
        String encrypted = CryptoUtils.encryptAES(data, key);
        assertNotNull(encrypted);
        assertNotEquals(data, encrypted);
        
        String decrypted = CryptoUtils.decryptAES(encrypted, key);
        assertEquals(data, decrypted);
    }
    
    @Test
    void testPasswordHash() {
        String password = "password123";
        
        String hash1 = CryptoUtils.hashPassword(password);
        String hash2 = CryptoUtils.hashPassword(password);
        
        // BCrypt每次生成不同的hash
        assertNotEquals(hash1, hash2);
        
        // 但都能验证通过
        assertTrue(CryptoUtils.verifyPassword(password, hash1));
        assertTrue(CryptoUtils.verifyPassword(password, hash2));
    }
}
```

## 开发注意事项

1. **版本管理**：使用语义化版本，严格遵守SemVer规范
2. **单元测试**：所有公共组件必须有单元测试，覆盖率>90%
3. **文档完善**：每个公共类和方法都要有JavaDoc
4. **向后兼容**：不要随意删除或修改公开API
5. **性能考虑**：公共组件会被大量调用，注意性能优化

## 常用命令

```bash
# 构建
mvn clean package

# 安装到本地仓库
mvn clean install

# 发布到私服
mvn clean deploy

# 运行测试
mvn test

# 生成测试覆盖率报告
mvn jacoco:report

# 检查代码规范
mvn checkstyle:check
```