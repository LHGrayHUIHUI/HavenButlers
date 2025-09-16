# Services层Infrastructure集成说明

## 概述

所有services层的微服务都已集成HavenButler平台的Infrastructure基础设施层，包括base-model基础模型和common公共组件库。

## 集成的基础模块

### 1. base-model模块
- **文档地址**：[infrastructure/base-model/README.md](../infrastructure/base-model/README.md)
- **版本**：1.0.0
- **提供功能**：
  - 统一响应格式（ResponseWrapper）
  - 全局异常处理（BusinessException、SystemException）
  - 链路追踪（TraceID自动生成和传递）
  - 错误码规范（ErrorCode）
  - 基础实体模型（BaseEntity、分页模型、DTOs）
  - 工具类（加密、JSON、日期、校验）
  - 自定义注解（@TraceLog、@RateLimit、@Encrypt）

### 2. common模块
- **文档地址**：[infrastructure/common/README.md](../infrastructure/common/README.md)
- **版本**：1.0.0
- **提供功能**：
  - Redis工具（RedisUtils、RedisCache、分布式锁）
  - 消息队列（RabbitMQ配置和MessageSender）
  - 安全组件（JwtUtils、AuthFilter）
  - ID生成器（雪花算法）
  - HTTP工具（HttpUtils）
  - 线程池（ThreadPoolUtils）
  - 限流切面（RateLimitAspect）

## 各服务集成状态

| 服务名称 | 集成状态 | 启动类 | 特殊配置 |
|---------|---------|--------|---------|
| gateway-service | ✅ 已完成 | GatewayApplication | 响应式编程，使用WebFlux |
| storage-service | ✅ 已完成 | StorageServiceApplication | 多数据源配置 |
| account-service | ✅ 已完成 | AccountServiceApplication | Spring Security集成 |
| ai-service | ✅ 已完成 | AiServiceApplication | 异步处理配置 |
| message-service | ✅ 已完成 | MessageServiceApplication | 消息队列配置 |
| nlp-service | ✅ 已完成 | NlpServiceApplication | 大模型调用配置 |
| file-manager-service | ✅ 已完成 | FileManagerServiceApplication | MinIO文件存储 |

## 标准集成方式

### 1. Maven依赖配置

```xml
<!-- 所有服务的pom.xml都包含 -->
<dependencies>
    <!-- Infrastructure基础模块 -->
    <dependency>
        <groupId>com.haven</groupId>
        <artifactId>base-model</artifactId>
        <version>1.0.0</version>
    </dependency>

    <dependency>
        <groupId>com.haven</groupId>
        <artifactId>common</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- 其他依赖... -->
</dependencies>
```

### 2. 启动类配置

```java
@SpringBootApplication
@EnableDiscoveryClient
@Import({BaseModelAutoConfiguration.class, CommonAutoConfiguration.class})
public class XxxServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(XxxServiceApplication.class, args);
    }
}
```

### 3. application.yml配置

```yaml
# 基础模块配置
base:
  exception:
    enabled: true  # 启用全局异常处理
  trace:
    enabled: true  # 启用链路追踪
  response:
    include-timestamp: true
    include-trace-id: true

# 公共组件配置
common:
  redis:
    enabled: true
    key-prefix: "service-name:"  # 每个服务使用不同前缀
    default-timeout: 3600
  security:
    jwt-enabled: true  # 根据需要启用
    jwt-secret: ${JWT_SECRET}
  thread-pool:
    enabled: true
    core-pool-size: 10
    max-pool-size: 50
```

## 使用示例

### 1. 控制器使用统一响应

```java
@RestController
@RequestMapping("/api/v1/service")
public class ServiceController {

    @GetMapping("/data")
    public ResponseWrapper<DataDTO> getData(@RequestParam String id) {
        // 自动处理异常、生成TraceID
        DataDTO data = service.getData(id);
        return ResponseWrapper.success(data);
    }

    @PostMapping("/save")
    @TraceLog("保存数据")  // 自动记录日志
    @RateLimit(window = 60, limit = 100)  // 限流
    public ResponseWrapper<String> saveData(@RequestBody @Valid DataRequest request) {
        String id = service.save(request);
        return ResponseWrapper.success("保存成功", id);
    }
}
```

### 2. 服务层使用异常

```java
@Service
public class BusinessService {

    public void processData(String id) {
        // 使用预定义错误码
        if (data == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }

        // 自定义错误信息
        if (!valid) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数格式不正确");
        }
    }
}
```

### 3. 使用Redis缓存

```java
@Service
public class CacheService {

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private DistributedLock distributedLock;

    public Object getDataWithCache(String key) {
        // 先从缓存获取
        Object data = redisCache.get(key, Object.class);
        if (data != null) {
            return data;
        }

        // 使用分布式锁防止缓存击穿
        String lockId = distributedLock.tryLock("lock:" + key, 5, TimeUnit.SECONDS);
        if (lockId != null) {
            try {
                // 双重检查
                data = redisCache.get(key, Object.class);
                if (data == null) {
                    data = loadFromDatabase(key);
                    redisCache.set(key, data, 3600);
                }
            } finally {
                distributedLock.unlock("lock:" + key, lockId);
            }
        }

        return data;
    }
}
```

### 4. 使用消息队列

```java
@Service
public class MessageService {

    @Autowired
    private MessageSender messageSender;

    public void sendNotification(String userId, NotificationDTO notification) {
        // 发送到消息队列
        messageSender.sendNotification(userId, notification);

        // 发送延迟消息
        messageSender.sendDelay("exchange", "routing.key", data, 5000);
    }
}
```

## 最佳实践

### 1. 异常处理
- 使用BusinessException处理业务异常
- 使用SystemException处理系统异常
- 不要捕获并忽略异常
- 异常信息要清晰明确

### 2. 日志记录
- 使用@TraceLog注解记录关键操作
- 日志中包含TraceID便于链路追踪
- 不要在日志中打印敏感信息

### 3. 缓存使用
- 合理设置缓存过期时间
- 使用分布式锁防止缓存击穿
- 注意缓存一致性问题

### 4. 限流保护
- 对外API使用@RateLimit注解
- 根据业务设置合理的限流阈值
- 区分用户级别和IP级别限流

## 故障排查

### 1. TraceID未生成
- 检查base.trace.enabled配置
- 确认TraceIdInterceptor已注册

### 2. 全局异常处理不生效
- 检查base.exception.enabled配置
- 确认@Import包含BaseModelAutoConfiguration

### 3. Redis连接失败
- 检查Redis服务是否启动
- 确认Redis连接配置正确
- 查看common.redis.enabled配置

### 4. 消息队列发送失败
- 检查RabbitMQ服务状态
- 确认交换机和队列已创建
- 查看common.mq.enabled配置

## 版本兼容性

| Infrastructure版本 | Spring Boot版本 | Java版本 |
|-------------------|----------------|----------|
| 1.0.0 | 3.1.0 | 17+ |

## 更新历史

- **2025-01-16**：完成所有services层服务的Infrastructure集成
- **2025-01-15**：Infrastructure基础设施层开发完成

## 联系支持

- 架构问题：查看 [infrastructure/README.md](../infrastructure/README.md)
- 集成问题：参考各服务的README.md文档
- 技术支持：tech@havenbutler.com