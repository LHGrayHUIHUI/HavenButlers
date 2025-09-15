# Common 公共组件库

## 服务定位
- **架构层级**：基础设施层
- **核心职责**：提供所有Java服务的公共组件、工具类、常量定义和通用配置
- **业务范围**：工具类库、常量定义、注解定义、配置类、拦截器、过滤器

## 技术栈
- **主开发语言**：Java 17
- **构建工具**：Maven 3.8+
- **核心依赖**：
  - Spring Boot Starter
  - Apache Commons
  - Guava
  - Jackson
  - Lombok
  - SLF4J + Logback

## 模块结构

```
common/
├── common-core/              # 核心公共模块
│   ├── constants/            # 常量定义
│   ├── enums/                # 枚举定义
│   ├── exceptions/           # 异常定义
│   └── dto/                  # 数据传输对象
├── common-utils/             # 工具类模块
│   ├── crypto/               # 加密工具
│   ├── json/                 # JSON工具
│   ├── date/                 # 日期工具
│   └── validation/           # 校验工具
├── common-redis/             # Redis工具模块
├── common-mq/                # 消息队列模块
├── common-security/          # 安全模块
└── common-web/               # Web公共模块
```

## 核心组件

### 1. 常量定义
```java
public class CommonConstants {
    // 系统常量
    public static final String SYSTEM_NAME = "HavenButler";
    public static final String VERSION = "1.0.0";
    
    // TraceID格式
    public static final String TRACE_ID_PREFIX = "tr-";
    public static final String TRACE_ID_PATTERN = "yyyyMMdd-HHmmss";
    
    // 请求头
    public static final String HEADER_TRACE_ID = "X-Trace-ID";
    public static final String HEADER_FAMILY_ID = "X-Family-ID";
    public static final String HEADER_USER_ID = "X-User-ID";
    
    // 缓存键前缀
    public static final String CACHE_PREFIX = "haven:";
    public static final String SESSION_PREFIX = "session:";
    public static final String DEVICE_PREFIX = "device:";
}
```

### 2. 统一异常处理
```java
// 业务异常基类
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;
    private final String traceId;
    
    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }
}

// 错误码枚举
public enum ErrorCode {
    SUCCESS(0, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),
    
    // 业务错误码 1000-9999
    DEVICE_OFFLINE(1001, "设备离线"),
    DEVICE_NOT_FOUND(1002, "设备不存在"),
    FAMILY_NOT_EXIST(2001, "家庭不存在"),
    USER_NOT_IN_FAMILY(2002, "用户不在该家庭中");
    
    private final int code;
    private final String message;
}
```

### 3. 加密工具类
```java
public class CryptoUtils {
    // AES加密
    public static String encryptAES(String data, String key) {
        // AES-256-GCM加密实现
    }
    
    // RSA加密
    public static String encryptRSA(String data, String publicKey) {
        // RSA加密实现
    }
    
    // HMAC签名
    public static String signHMAC(String data, String secret) {
        // HMAC-SHA256签名
    }
    
    // 密码加密
    public static String hashPassword(String password) {
        // BCrypt加密
    }
}
```

### 4. TraceID生成器
```java
@Component
public class TraceIdGenerator {
    private static final String DATE_FORMAT = "yyyyMMdd-HHmmss";
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random RANDOM = new Random();
    
    public String generate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String timestamp = sdf.format(new Date());
        String random = generateRandomString(6);
        return CommonConstants.TRACE_ID_PREFIX + timestamp + "-" + random;
    }
    
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
```

### 5. Redis工具类
```java
@Component
public class RedisUtils {
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 设置值
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, JSON.toJSONString(value), timeout, unit);
    }
    
    // 获取值
    public <T> T get(String key, Class<T> clazz) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? JSON.parseObject(value, clazz) : null;
    }
    
    // 分布式锁
    public boolean tryLock(String key, String value, long timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
    }
    
    // 释放锁
    public boolean releaseLock(String key, String value) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                       "return redis.call('del', KEYS[1]) else return 0 end";
        return redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(key),
            value
        ) == 1L;
    }
}
```

### 6. 校验工具
```java
public class ValidationUtils {
    // 手机号校验
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
    
    // 邮箱校验
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$");
    }
    
    // 身份证校验
    public static boolean isValidIdCard(String idCard) {
        // 18位身份证校验
        return IdCardValidator.isValidIdCard(idCard);
    }
    
    // IP地址校验
    public static boolean isValidIp(String ip) {
        return InetAddressValidator.getInstance().isValid(ip);
    }
}
```

### 7. 全局拦截器
```java
@Component
public class TraceIdInterceptor implements HandlerInterceptor {
    @Autowired
    private TraceIdGenerator traceIdGenerator;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取或生成TraceID
        String traceId = request.getHeader(CommonConstants.HEADER_TRACE_ID);
        if (StringUtils.isBlank(traceId)) {
            traceId = traceIdGenerator.generate();
        }
        
        // 设置到MDC
        MDC.put("traceId", traceId);
        
        // 设置到响应头
        response.setHeader(CommonConstants.HEADER_TRACE_ID, traceId);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.clear();
    }
}
```

### 8. 日志工具
```java
@Component
public class LogUtils {
    private static final Logger logger = LoggerFactory.getLogger(LogUtils.class);
    
    public void logRequest(HttpServletRequest request) {
        logger.info("Request: {} {} from {}, TraceId: {}",
            request.getMethod(),
            request.getRequestURI(),
            request.getRemoteAddr(),
            MDC.get("traceId")
        );
    }
    
    public void logResponse(int status, long duration) {
        logger.info("Response: status={}, duration={}ms, TraceId: {}",
            status,
            duration,
            MDC.get("traceId")
        );
    }
    
    public void logError(String message, Throwable throwable) {
        logger.error("Error: {}, TraceId: {}", message, MDC.get("traceId"), throwable);
    }
}
```

## Maven依赖

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- 工具类 -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>
    
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    
    <!-- JSON -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
    </dependency>
    
    <!-- 加密 -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
    </dependency>
</dependencies>
```

## 使用方式

其他服务通过Maven依赖引入：

```xml
<dependency>
    <groupId>com.havenbutler</groupId>
    <artifactId>common-core</artifactId>
    <version>${common.version}</version>
</dependency>

<dependency>
    <groupId>com.havenbutler</groupId>
    <artifactId>common-utils</artifactId>
    <version>${common.version}</version>
</dependency>
```

## 注意事项
- 公共组件必须保持稳定，避免频繁变更
- 新增功能需要充分测试
- 版本更新需要通知所有依赖服务
- 不要引入重量级依赖

## 更新历史
- v1.0.0 (2025-01-15): 初始版本，基础公共组件