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

## GitHub Packages Maven配置

### 1. 认证配置

在`~/.m2/settings.xml`中添加GitHub认证信息：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <servers>
        <server>
            <id>github</id>
            <username>你的GitHub用户名</username>
            <password>你的GitHub Personal Access Token</password>
        </server>
    </servers>

</settings>
```

**获取GitHub Personal Access Token步骤**：
1. 访问 GitHub > Settings > Developer settings > Personal access tokens > Tokens (classic)
2. 点击 "Generate new token (classic)"
3. 勾选以下权限：
   - ✅ `read:packages` - 读取包权限
   - ✅ `write:packages` - 发布包权限（如果需要）
   - ✅ `repo` - 仓库访问权限
4. 生成token并复制到settings.xml中

### 2. 项目pom.xml配置

在你的微服务项目中配置仓库地址和依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <!-- 添加GitHub Packages仓库 -->
    <repositories>
        <repository>
            <id>github</id>
            <name>GitHub Packages</name>
            <url>https://maven.pkg.github.com/LHGrayHUIHUI/HavenButlers</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <!-- HavenButler base-model依赖 -->
        <dependency>
            <groupId>com.haven</groupId>
            <artifactId>base-model</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- HavenButler common依赖 -->
        <dependency>
            <groupId>com.haven</groupId>
            <artifactId>common</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- 其他依赖... -->
    </dependencies>

</project>
```

### 3. 依赖层级说明

Common模块的依赖关系：
```
你的微服务
    ↓
common (公共组件库)
    ↓
base-model (基础模型库)
    ↓
Spring Boot 3.1.0 + Java 17
```

- **base-model**: 提供基础的响应包装、异常处理、工具类
- **common**: 在base-model基础上提供Redis、安全、消息队列等高级功能
- **你的微服务**: 使用common提供的所有功能

### 4. 自动配置激活

Common模块会自动配置以下组件：
- ✅ **RedisUtils**: Redis工具类（需要Redis连接配置）
- ✅ **RedisCache**: Redis缓存管理器
- ✅ **DistributedLock**: 分布式锁
- ✅ **JwtUtils**: JWT工具类
- ✅ **ThreadPoolUtils**: 线程池工具类
- ✅ **HttpUtils**: HTTP客户端工具
- ✅ **MessageSender**: 消息发送器（需要RabbitMQ配置）

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

## 已实现功能清单

✅ **核心常量**
- CommonConstants: 系统、缓存、消息队列、线程池、安全等常量定义

✅ **Redis组件**
- RedisUtils: 基础Redis操作工具
- RedisCache: 高级缓存管理器（支持批量操作、Hash、List、Set、ZSet）
- DistributedLock: 分布式锁实现（支持可重入、自动续期、重试机制）

✅ **安全组件**
- JwtUtils: JWT令牌生成和验证
- AuthFilter: 认证过滤器

✅ **工具类**
- HttpUtils: HTTP请求工具（GET/POST/PUT/DELETE）
- ThreadPoolUtils: 线程池管理工具

✅ **自动配置**
- CommonAutoConfiguration: Spring Boot自动配置
- 完整的配置属性类
- META-INF/spring.factories自动配置文件

## 集成使用指南

### 1. Maven依赖引入

```xml
<dependency>
    <groupId>com.haven</groupId>
    <artifactId>common</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置文件

在`application.yml`中添加：

```yaml
# 引入common配置
spring:
  profiles:
    include: common

  # Redis配置（如果使用Redis功能）
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms
        max-idle: 8
        min-idle: 0

# Common模块配置
common:
  redis:
    enabled: true                # 启用Redis功能
    default-timeout: 3600        # 默认超时时间（秒）
    key-prefix: "haven:"         # 键前缀
  security:
    enabled: true                # 启用安全功能
    jwt-enabled: true            # 启用JWT
    jwt-expiration: 86400000     # JWT过期时间（毫秒）
    jwt-secret: "your-secret"    # JWT密钥（生产环境请修改）
  thread-pool:
    enabled: true                # 启用线程池
    core-pool-size: 10           # 核心线程数
    max-pool-size: 50            # 最大线程数
    queue-capacity: 100          # 队列容量
    keep-alive-seconds: 60       # 线程存活时间
```

### 3. 使用示例

#### Redis缓存操作

```java
@Service
public class UserService {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisCache redisCache;

    // 基础缓存操作
    public void cacheUser(UserDTO user) {
        String key = "user:" + user.getUserId();
        redisUtils.set(key, user, 3600, TimeUnit.SECONDS);
    }

    public UserDTO getUser(String userId) {
        String key = "user:" + userId;
        return redisUtils.get(key, UserDTO.class);
    }

    // 批量操作
    public void batchCacheUsers(List<UserDTO> users) {
        Map<String, Object> map = new HashMap<>();
        users.forEach(user -> map.put("user:" + user.getUserId(), user));
        redisCache.setBatch(map, 3600, TimeUnit.SECONDS);
    }

    // Hash操作
    public void cacheUserField(String userId, String field, Object value) {
        String key = "user:hash:" + userId;
        redisCache.hashSet(key, field, value);
    }

    // List操作
    public void addUserAction(String userId, String action) {
        String key = "user:actions:" + userId;
        redisCache.listRightPush(key, action);
    }

    // Set操作
    public void addUserTag(String userId, String tag) {
        String key = "user:tags:" + userId;
        redisCache.setAdd(key, tag);
    }

    // ZSet操作（排行榜）
    public void updateUserScore(String userId, double score) {
        redisCache.zSetAdd("user:ranking", userId, score);
    }
}
```

#### 分布式锁

```java
@Service
public class OrderService {

    @Autowired
    private DistributedLock distributedLock;

    public void createOrder(String orderId) {
        String lockKey = "order:create:" + orderId;

        // 方式1：手动获取和释放锁
        String lockValue = distributedLock.tryLock(lockKey, 30, TimeUnit.SECONDS);
        if (lockValue == null) {
            throw new BusinessException("获取锁失败");
        }

        try {
            // 业务逻辑
            processOrder(orderId);

            // 如果需要续期
            distributedLock.renewLock(lockKey, lockValue, 30);

        } finally {
            distributedLock.releaseLock(lockKey, lockValue);
        }
    }

    public void quickProcess(String id) {
        // 方式2：使用Lambda表达式
        boolean success = distributedLock.executeWithLock(
            "quick:" + id,
            10, TimeUnit.SECONDS,
            () -> {
                // 业务逻辑
                doQuickProcess(id);
            }
        );

        if (!success) {
            throw new BusinessException("处理失败");
        }
    }

    public String processWithResult(String id) {
        // 方式3：带返回值的执行
        return distributedLock.executeWithLock(
            "result:" + id,
            10, TimeUnit.SECONDS,
            () -> {
                // 业务逻辑
                return "Result: " + id;
            }
        );
    }

    public void retryProcess(String id) {
        // 方式4：带重试的锁
        String lockValue = distributedLock.tryLockWithRetry(
            "retry:" + id,
            30, TimeUnit.SECONDS,
            3,  // 最大重试3次
            1000  // 重试间隔1秒
        );

        if (lockValue == null) {
            throw new BusinessException("多次重试后仍无法获取锁");
        }

        try {
            // 业务逻辑
        } finally {
            distributedLock.releaseLock("retry:" + id, lockValue);
        }
    }
}
```

#### JWT使用

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseWrapper<Map<String, String>> login(@RequestBody LoginDTO loginDTO) {
        // 验证用户名密码
        UserDTO user = validateUser(loginDTO);

        // 生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("familyId", user.getCurrentFamilyId());
        claims.put("roles", user.getRoles());

        String token = jwtUtils.generateToken(user.getUserId(), claims);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());

        return ResponseWrapper.success(result);
    }

    @GetMapping("/validate")
    public ResponseWrapper<Boolean> validateToken(
            @RequestHeader("X-Auth-Token") String token) {

        boolean valid = jwtUtils.validateToken(token) &&
                       !jwtUtils.isTokenExpired(token);

        return ResponseWrapper.success(valid);
    }

    @PostMapping("/refresh")
    public ResponseWrapper<String> refreshToken(
            @RequestHeader("X-Auth-Token") String oldToken) {

        if (!jwtUtils.validateToken(oldToken)) {
            throw new AuthException("无效的令牌");
        }

        String newToken = jwtUtils.refreshToken(oldToken);
        return ResponseWrapper.success(newToken);
    }

    @GetMapping("/user-info")
    public ResponseWrapper<Map<String, Object>> getUserInfo(
            @RequestHeader("X-Auth-Token") String token) {

        Claims claims = jwtUtils.getClaimsFromToken(token);
        if (claims == null) {
            throw new AuthException("无法解析令牌");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", claims.getSubject());
        userInfo.put("username", claims.get("username"));
        userInfo.put("familyId", claims.get("familyId"));
        userInfo.put("roles", claims.get("roles"));
        userInfo.put("expiration", claims.getExpiration());

        return ResponseWrapper.success(userInfo);
    }
}
```

#### 线程池使用

```java
@Service
public class AsyncService {

    @PostConstruct
    public void init() {
        // 创建不同类型的线程池
        ThreadPoolUtils.createFixedThreadPool("data-process", 10);
        ThreadPoolUtils.createCachedThreadPool("io-tasks");
        ThreadPoolUtils.createScheduledThreadPool("scheduled", 5);
    }

    public void asyncProcess(List<String> dataList) {
        // 异步执行任务
        ThreadPoolUtils.executeAsync("data-process", () -> {
            dataList.forEach(this::processData);
        });
    }

    public CompletableFuture<String> asyncCompute(String input) {
        // 异步计算并返回结果
        return ThreadPoolUtils.submitAsync("data-process", () -> {
            // 复杂计算
            return "Result: " + input;
        });
    }

    public void batchProcess(List<String> items) {
        ExecutorService executor = ThreadPoolUtils.getThreadPool("data-process");

        List<CompletableFuture<Void>> futures = items.stream()
            .map(item -> CompletableFuture.runAsync(
                () -> processItem(item), executor))
            .collect(Collectors.toList());

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();
    }

    @PreDestroy
    public void cleanup() {
        // 关闭线程池
        ThreadPoolUtils.shutdownAll();
    }
}
```

#### HTTP工具使用

```java
@Service
public class ExternalApiService {

    public String callExternalApi() {
        // GET请求
        String result = HttpUtils.get("https://api.example.com/data");

        // GET请求带参数
        Map<String, String> params = new HashMap<>();
        params.put("key", "value");
        params.put("page", "1");
        result = HttpUtils.get("https://api.example.com/search", params);

        // POST JSON
        UserDTO user = new UserDTO();
        String json = JsonUtil.toJson(user);
        result = HttpUtils.postJson("https://api.example.com/users", json);

        // POST表单
        Map<String, String> formData = new HashMap<>();
        formData.put("username", "test");
        formData.put("password", "123456");
        result = HttpUtils.postForm("https://api.example.com/login", formData);

        // 带请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        headers.put("X-Custom-Header", "value");
        result = HttpUtils.get("https://api.example.com/protected", null, headers);

        return result;
    }
}
```

## 注意事项

1. **Redis配置**：使用Redis功能前需要配置Redis连接信息
2. **JWT密钥**：生产环境必须修改默认的JWT密钥
3. **线程池**：使用后记得关闭线程池释放资源
4. **分布式锁**：注意设置合理的超时时间
5. **性能优化**：Redis批量操作比单个操作效率高

## 故障排查

### Redis连接失败
- 检查Redis服务是否启动
- 确认连接配置正确
- 检查防火墙和网络连接

### JWT验证失败
- 检查令牌格式是否正确
- 确认密钥配置一致
- 检查令牌是否过期

### 分布式锁获取失败
- 检查Redis连接状态
- 调整锁超时时间
- 使用重试机制

### 常见问题排查

#### 问题1：依赖下载失败
```
错误：Could not find artifact com.haven:common:jar:1.0.0
```
**解决方案**：
1. 检查 `~/.m2/settings.xml` 中的GitHub认证配置
2. 确认Personal Access Token具有 `read:packages` 权限
3. 验证仓库URL是否正确：`https://maven.pkg.github.com/LHGrayHUIHUI/HavenButlers`

#### 问题2：自动配置不生效
```
错误：No bean of type 'RedisUtils' found
```
**解决方案**：
1. 确认启动类包路径包含 `com.haven` 或添加 `@ComponentScan("com.haven")`
2. 检查相关配置是否启用：`common.redis.enabled=true`
3. 确认依赖正确导入且没有冲突

#### 问题3：Redis连接失败
```
错误：Unable to connect to Redis
```
**解决方案**：
1. 检查Redis服务是否启动：`redis-cli ping`
2. 验证连接配置：host、port、password等
3. 检查防火墙和网络连接
4. 确认Redis配置格式正确

#### 问题4：JWT验证失败
```
错误：JWT signature does not match locally computed signature
```
**解决方案**：
1. 检查所有服务使用相同的JWT密钥
2. 确认密钥配置正确且没有特殊字符问题
3. 验证时间同步（JWT有时间敏感性）
4. 检查算法是否一致（默认使用HS256）

#### 问题5：分布式锁获取失败
```
错误：无法获取分布式锁
```
**解决方案**：
1. 检查Redis连接状态
2. 调整锁超时时间（避免设置过短）
3. 使用重试机制：`tryLockWithRetry`
4. 检查锁键是否冲突
5. 监控锁使用情况，避免死锁

### 技术支持

如果在集成过程中遇到问题，请按以下步骤排查：

1. **查看启动日志**：确认common相关组件是否正常加载
2. **检查配置文件**：对比本文档中的配置示例
3. **验证依赖版本**：确保使用的是正确的common版本
4. **环境检查**：确认Java版本≥17，Maven版本≥3.6
5. **网络检查**：确认能访问GitHub Packages和Redis/RabbitMQ等服务

更多技术细节请参考源码中的JavaDoc注释和单元测试用例。

### 版本兼容性

| Common版本 | Base-Model版本 | Spring Boot版本 | Java版本 |
|------------|----------------|-----------------|----------|
| 1.0.0      | 1.0.0         | 3.1.0          | 17+      |

**升级注意事项**：
- Spring Boot 3.x 使用 Jakarta EE，不兼容 javax 命名空间
- Java 17 是最低要求版本
- Redis客户端使用Lettuce，配置与Jedis略有不同

## 更新历史
- v1.0.0 (2025-01-16): 初始版本发布，完整实现Redis、安全、工具类等功能