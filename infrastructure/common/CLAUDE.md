# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是HavenButler智能家居平台的Common公共组件库，位于基础设施层，为所有Java微服务提供统一的工具类、缓存管理、安全组件等公共功能。

## 🏗️ 架构全景图

### HavenButler整体架构依赖链
```
┌─────────────────────────────────────────────────────────────┐
│                    HavenButler智能家居平台                      │
├─────────────────────────────────────────────────────────────┤
│  🌐 网关层                                                    │
│  ├── gateway-service (Spring Cloud Gateway)                 │
│  │   ├── 依赖: base-model + common                          │
│  │   ├── 功能: 路由、鉴权、限流、监控                           │
│  │   └── 端口: 8080                                         │
├─────────────────────────────────────────────────────────────┤
│  💼 核心业务层 (所有服务都依赖base-model + common)              │
│  ├── account-service (8082) - 用户认证、权限管理               │
│  ├── storage-service (8081) - 统一数据存储                    │
│  │   ├── MySQL: 关系型数据                                   │
│  │   ├── MongoDB: 文档数据                                   │
│  │   ├── Redis: 缓存数据                                     │
│  │   └── MinIO: 文件存储                                     │
│  ├── message-service (8083) - 消息通知                       │
│  ├── ai-service (8084) - AI智能分析                          │
│  ├── nlp-service (8085) - 自然语言处理                       │
│  └── file-manager-service (8086) - 文件管理                  │
├─────────────────────────────────────────────────────────────┤
│  🏗️ 基础设施层                                                │
│  ├── 📦 common (本模块) - 公共组件库                          │
│  │   ├── RedisUtils + DistributedLock                       │
│  │   ├── JwtUtils + AuthFilter                              │
│  │   ├── HttpUtils + ThreadPoolUtils                        │
│  │   ├── MessageSender + RabbitMQ配置                        │
│  │   └── 统一异常处理 + TraceID管理                           │
│  ├── 📋 base-model - 基础模型和响应封装                       │
│  │   ├── ResponseWrapper统一响应格式                          │
│  │   ├── 通用异常定义和处理                                   │
│  │   └── 基础工具类和配置                                     │
│  └── 🎛️ admin-service (8888) - 管理控制台                    │
├─────────────────────────────────────────────────────────────┤
│  🔧 外部依赖服务                                              │
│  ├── Nacos (8848) - 服务发现 + 配置中心                      │
│  ├── Redis (6379) - 缓存 + 会话 + 分布式锁                   │
│  ├── PostgreSQL (5432) - 主数据库                           │
│  ├── MongoDB (27017) - 文档数据库                           │
│  ├── MinIO (9000/9001) - 对象存储                           │
│  └── RabbitMQ (5672/15672) - 消息队列                      │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈矩阵
| 层级 | 框架技术 | 版本 | 主要职责 |
|------|----------|------|----------|
| **Gateway** | Spring Cloud Gateway | 2023.0.1 | 统一网关、负载均衡 |
| **Business** | Spring Boot | 3.1.0 | 业务逻辑、REST API |
| **Infrastructure** | Spring Boot | 3.1.0 | 公共组件、工具类库 |
| **Config** | Nacos | 2.3.0 | 配置管理、服务发现 |
| **Cache** | Redis + Lettuce | 7.0+ | 缓存、会话、分布式锁 |
| **Database** | PostgreSQL + MongoDB | 15+ / 6.0+ | 数据持久化 |
| **Storage** | MinIO | 最新 | 文件对象存储 |
| **MQ** | RabbitMQ | 3.12+ | 异步消息处理 |

### Common模块核心架构
```
com.haven.common/
├── 🔧 config/
│   └── CommonAutoConfiguration    # Spring Boot自动配置入口
├── 📊 core/constants/
│   └── CommonConstants           # 系统级常量(HTTP头、缓存键、MQ配置等)
├── 🔐 redis/
│   ├── RedisUtils               # 基础缓存操作
│   ├── RedisCache               # 高级批量操作
│   └── DistributedLock          # 分布式锁(重入+续期+重试)
├── 🛡️ security/
│   └── JwtUtils                # JWT生成验证刷新
├── 🔨 utils/
│   ├── HttpUtils               # HTTP客户端封装
│   ├── ThreadPoolUtils         # 线程池管理
│   └── IdGenerator             # TraceID生成器
├── 📨 mq/
│   ├── RabbitMqConfig          # RabbitMQ自动配置
│   └── MessageSender           # 消息发送工具
├── 🌐 web/filter/
│   └── AuthFilter              # 认证过滤器
├── 🎯 aspect/
│   └── RateLimitAspect         # 限流切面
└── 🚨 exception/
    └── CommonException         # 统一异常处理
```

## 常用命令

### Maven构建命令
```bash
# 清理并编译
mvn clean compile

# 运行所有测试
mvn test

# 生成测试覆盖率报告
mvn jacoco:report

# 打包（不执行测试）
mvn package -DskipTests

# 安装到本地Maven仓库
mvn install

# 发布到GitHub Packages（需要配置认证）
mvn deploy
```

### GitHub Packages配置
需要在`~/.m2/settings.xml`中配置GitHub认证：
```xml
<server>
    <id>github</id>
    <username>你的GitHub用户名</username>
    <password>你的Personal_Access_Token</password>
</server>
```

Token需要`read:packages`和`write:packages`权限。

### 本地开发验证
```bash
# 检查代码格式
mvn checkstyle:check

# 生成JavaDoc
mvn javadoc:javadoc

# 查看依赖树
mvn dependency:tree

# 分析依赖冲突
mvn dependency:analyze
```

## ⚙️ 配置最佳实践

### 分层配置策略
基于Nacos配置中心的分层配置模式：
```yaml
# 1. 全局基础配置 (nacos: havenbutler-common.yml)
spring:
  profiles:
    include:
      - base           # 引入base-model配置
      - common         # 引入common配置
  config:
    import:
      - optional:nacos:havenbutler-common.yml    # 全平台公共配置
      - optional:nacos:${spring.application.name}.yml  # 服务专用配置
```

### 环境配置矩阵
| 配置项 | 开发环境 | 测试环境 | 生产环境 | 说明 |
|--------|----------|----------|----------|------|
| **common.redis.enabled** | `true` | `true` | `true` | Redis功能开关 |
| **common.redis.key-prefix** | `"dev:"` | `"test:"` | `"prod:"` | 环境隔离前缀 |
| **common.security.jwt-secret** | 默认测试密钥 | 16位随机密钥 | 32位强密钥 | JWT签名密钥 |
| **common.security.jwt-expiration** | `86400000` | `3600000` | `28800000` | Token有效期(毫秒) |
| **common.thread-pool.core-pool-size** | `5` | `10` | `20` | 核心线程数 |
| **common.thread-pool.max-pool-size** | `10` | `30` | `100` | 最大线程数 |

### 自动配置激活机制
通过`src/main/resources/META-INF/spring.factories`自动注册：
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.haven.common.config.CommonAutoConfiguration
```

### 完整配置模板
#### 开发环境 (application.yml)
```yaml
# ===========================================
# HavenButler微服务标准配置模板 - 开发环境
# ===========================================
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: your-service-name

  # 配置导入策略 - Nacos配置优先级高于本地
  config:
    import:
      - optional:nacos:havenbutler-common.yml  # 全局公共配置
      - optional:nacos:${spring.application.name}.yml  # 服务专用配置

  # 引入公共配置
  profiles:
    include:
      - base      # base-model配置
      - common    # common配置

  # Nacos服务发现和配置
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:public}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        enabled: true
        metadata:
          version: 1.0.0
          environment: ${ENVIRONMENT:dev}
      config:
        server-addr: ${NACOS_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:public}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        file-extension: yml
        enabled: true
        shared-configs:
          - data-id: havenbutler-common.yml
            group: DEFAULT_GROUP
            refresh: true

  # Redis配置 (基于common组件)
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# Common组件配置
common:
  redis:
    enabled: true
    default-timeout: 3600
    key-prefix: "haven:dev:"      # 开发环境前缀
  security:
    jwt-enabled: true
    jwt-expiration: 86400000      # 24小时
    jwt-secret: "DevHavenButlerSecret"  # 开发环境密钥
  thread-pool:
    enabled: true
    core-pool-size: 5
    max-pool-size: 10
    queue-capacity: 50
  mq:
    enabled: false                # 本地开发可选关闭MQ

# Actuator监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

# 日志配置
logging:
  level:
    com.haven: DEBUG
    com.haven.common: DEBUG
    org.springframework.cloud.gateway: DEBUG
    root: INFO
```

#### 生产环境核心配置差异
```yaml
# 生产环境关键配置调整
common:
  redis:
    key-prefix: "haven:prod:"     # 生产环境前缀
    default-timeout: 7200         # 增加缓存时间
  security:
    jwt-secret: "${JWT_SECRET}"   # 从环境变量读取，32位强密钥
    jwt-expiration: 28800000      # 8小时有效期
  thread-pool:
    core-pool-size: 20            # 生产环境增加线程数
    max-pool-size: 100
    queue-capacity: 200

# 生产环境日志
logging:
  level:
    com.haven: INFO              # 生产环境减少日志输出
    com.haven.common: WARN
    root: WARN
```

### 配置优先级和覆盖规则
```
高 ← ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ → 低
环境变量 > Nacos服务配置 > Nacos公共配置 > 本地配置文件
```

### 配置验证检查清单
✅ **上线前必检项**:
1. JWT密钥已修改且≥32位
2. Redis前缀包含环境标识
3. 数据库连接使用环境变量
4. 敏感信息全部配置在Nacos中
5. 生产环境日志级别≥INFO
6. 线程池大小适配服务器配置

## 核心功能模块

### 1. 常量管理
`CommonConstants`类提供系统级常量：
- HTTP请求头标准定义（TraceID、FamilyID等）
- 缓存键前缀和超时配置
- MQ交换机和队列命名规范
- 安全算法和长度限制

### 2. Redis工具链
- **RedisUtils**: 基础缓存CRUD操作
- **RedisCache**: 高级批量操作（Hash、List、Set、ZSet）
- **DistributedLock**: 分布式锁（支持重入、自动续期、重试）

### 3. 安全组件
- **JwtUtils**: JWT生成、验证、刷新
- **AuthFilter**: 统一认证过滤器

### 4. 通用工具
- **HttpUtils**: HTTP客户端封装（GET/POST/PUT/DELETE）
- **ThreadPoolUtils**: 线程池管理和异步任务执行
- **IdGenerator**: TraceID生成器

## 开发最佳实践

### 新增工具类规范
1. 使用`final`类声明
2. 私有构造函数防止实例化
3. 方法必须是静态的
4. 线程安全考虑（使用ThreadLocal）
5. 完整的JavaDoc注释（中文）

### 常量定义规范
- 按功能分组使用嵌套静态类
- 使用有意义的命名
- 避免魔法数字

### 异常处理规范
- 继承`CommonException`基类
- 携带TraceID信息
- 提供错误码和消息

### 版本管理
- 遵循语义化版本（SemVer）
- 向后兼容原则
- 重大变更需要文档说明

## 测试策略

### 单元测试
- 使用JUnit 5
- 覆盖率要求≥90%
- Mock外部依赖

### 集成测试
- 使用TestContainers
- Redis集成测试
- 端到端场景验证

## 故障排查

### 常见问题
1. **依赖下载失败**: 检查GitHub Packages认证配置
2. **自动配置不生效**: 确认包扫描路径包含`com.haven`
3. **Redis连接失败**: 验证Redis服务状态和连接配置
4. **JWT验证失败**: 检查密钥配置一致性

### 日志调试
启用DEBUG日志查看组件加载过程：
```yaml
logging:
  level:
    com.haven.common: DEBUG
```

## 部署注意事项

### 环境要求
- Java 17+
- Maven 3.8+
- Redis 5.0+（如果使用缓存功能）
- RabbitMQ 3.8+（如果使用MQ功能）

### 生产环境配置
- 修改默认JWT密钥
- 调整Redis连接池参数
- 配置适当的线程池大小
- 启用生产环境日志级别

### 监控指标
- Redis连接池使用率
- 分布式锁获取成功率
- JWT验证性能
- 线程池队列长度

## 🔗 集成使用指导

### 标准微服务集成流程

#### 1️⃣ 项目结构准备
```bash
your-service/
├── pom.xml                        # Maven配置
├── src/main/
│   ├── java/com/haven/yourservice/
│   │   ├── YourServiceApplication.java    # 启动类
│   │   ├── config/                # 服务专用配置
│   │   ├── controller/            # REST控制器
│   │   └── service/               # 业务服务层
│   └── resources/
│       ├── application.yml        # 本地配置
│       └── application-docker.yml # Docker配置
```

#### 2️⃣ Maven依赖配置
```xml
<!-- pom.xml完整配置模板 -->
<project>
    <!-- GitHub Packages仓库配置 -->
    <repositories>
        <repository>
            <id>github</id>
            <name>GitHub Packages</name>
            <url>https://maven.pkg.github.com/LHGrayHUIHUI/HavenButlers</url>
            <releases><enabled>true</enabled></releases>
            <snapshots><enabled>false</enabled></snapshots>
        </repository>
    </repositories>

    <dependencies>
        <!-- 🎯 核心依赖：按顺序引入 -->
        <!-- 1. Base Model - 基础响应封装 -->
        <dependency>
            <groupId>com.haven</groupId>
            <artifactId>base-model</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- 2. Common - 公共组件库(包含Redis、JWT、工具类) -->
        <dependency>
            <groupId>com.haven</groupId>
            <artifactId>common</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- 3. Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 4. Nacos服务发现 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
            <version>2023.0.1.0</version>
        </dependency>

        <!-- 5. Nacos配置中心 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
            <version>2023.0.1.0</version>
        </dependency>
    </dependencies>
</project>
```

#### 3️⃣ 启动类配置
```java
package com.haven.yourservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 微服务启动类标准模板
 *
 * ⚠️ 重要：包扫描必须包含com.haven才能自动装配common组件
 */
@SpringBootApplication(scanBasePackages = {
    "com.haven.yourservice",    // 本服务包
    "com.haven.common",         // Common组件自动装配
    "com.haven.base"            // Base-Model组件自动装配
})
@EnableDiscoveryClient          // 启用Nacos服务发现
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

#### 4️⃣ 配置文件标准模板
使用前面提供的完整配置模板，关键调整：
```yaml
spring:
  application:
    name: your-service-name    # ⚠️ 修改为实际服务名

server:
  port: ${SERVER_PORT:8080}    # ⚠️ 修改为分配的端口
```

### 核心组件使用示例

#### 🔐 Redis缓存使用
```java
@RestController
@RequestMapping("/api/v1/cache")
public class CacheController {

    // 自动注入Common组件
    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisCache redisCache;

    @PostMapping("/user/{userId}")
    public ResponseWrapper<String> cacheUser(@PathVariable String userId,
                                           @RequestBody UserDTO user) {
        // 基础缓存操作
        String cacheKey = "user:" + userId;
        redisUtils.set(cacheKey, user, 3600, TimeUnit.SECONDS);

        // 批量操作
        Map<String, Object> batchData = new HashMap<>();
        batchData.put("user:profile:" + userId, user.getProfile());
        batchData.put("user:settings:" + userId, user.getSettings());
        redisCache.setBatch(batchData, 3600, TimeUnit.SECONDS);

        return ResponseWrapper.success("缓存成功");
    }

    @GetMapping("/user/{userId}")
    public ResponseWrapper<UserDTO> getUser(@PathVariable String userId) {
        String cacheKey = "user:" + userId;
        UserDTO user = redisUtils.get(cacheKey, UserDTO.class);

        if (user == null) {
            return ResponseWrapper.error("用户缓存不存在");
        }

        return ResponseWrapper.success(user);
    }
}
```

#### 🔒 分布式锁使用
```java
@Service
public class OrderService {

    @Autowired
    private DistributedLock distributedLock;

    /**
     * 创建订单 - 使用分布式锁防止重复创建
     */
    public ResponseWrapper<String> createOrder(String orderId) {
        String lockKey = "order:create:" + orderId;

        // 方式1：手动管理锁
        String lockValue = distributedLock.tryLock(lockKey, 30, TimeUnit.SECONDS);
        if (lockValue == null) {
            return ResponseWrapper.error("系统繁忙，请稍后重试");
        }

        try {
            // 业务逻辑
            processOrderCreation(orderId);
            return ResponseWrapper.success("订单创建成功");

        } finally {
            distributedLock.releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 批量处理 - 使用Lambda表达式简化锁管理
     */
    public ResponseWrapper<Void> batchProcess(List<String> orderIds) {
        boolean success = distributedLock.executeWithLock(
            "batch:process",
            60, TimeUnit.SECONDS,
            () -> {
                orderIds.forEach(this::processOrderCreation);
            }
        );

        if (!success) {
            return ResponseWrapper.error("获取处理锁失败");
        }

        return ResponseWrapper.success();
    }
}
```

#### 🛡️ JWT认证使用
```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseWrapper<Map<String, String>> login(@RequestBody LoginDTO loginDTO) {
        // 验证用户凭据
        UserDTO user = validateUserCredentials(loginDTO);
        if (user == null) {
            return ResponseWrapper.error("用户名或密码错误");
        }

        // 生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("familyId", user.getFamilyId());
        claims.put("roles", user.getRoles());

        String token = jwtUtils.generateToken(user.getUserId(), claims);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());
        result.put("expireTime", String.valueOf(System.currentTimeMillis() + 86400000));

        return ResponseWrapper.success(result);
    }

    @GetMapping("/validate")
    public ResponseWrapper<Map<String, Object>> validateToken(HttpServletRequest request) {
        String token = request.getHeader(CommonConstants.Header.TOKEN);

        if (!jwtUtils.validateToken(token) || jwtUtils.isTokenExpired(token)) {
            return ResponseWrapper.error("令牌无效或已过期");
        }

        Claims claims = jwtUtils.getClaimsFromToken(token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", claims.getSubject());
        userInfo.put("username", claims.get("username"));
        userInfo.put("familyId", claims.get("familyId"));

        return ResponseWrapper.success(userInfo);
    }
}
```

#### 📨 HTTP调用使用
```java
@Service
public class ExternalService {

    @Autowired
    private HttpUtils httpUtils;

    /**
     * 调用其他微服务API
     */
    public UserDTO getUserFromAccountService(String userId) {
        try {
            // 构建请求头（包含TraceID传递）
            Map<String, String> headers = new HashMap<>();
            headers.put(CommonConstants.Header.TRACE_ID, MDC.get("traceId"));
            headers.put(CommonConstants.Header.TOKEN, getCurrentUserToken());

            // 调用account-service
            String url = "http://account-service:8082/api/v1/users/" + userId;
            String response = httpUtils.get(url, null, headers);

            // 解析响应
            ResponseWrapper<UserDTO> wrapper = JSON.parseObject(response,
                new TypeReference<ResponseWrapper<UserDTO>>() {});

            if (wrapper.isSuccess()) {
                return wrapper.getData();
            } else {
                log.warn("获取用户信息失败: {}", wrapper.getMessage());
                return null;
            }

        } catch (Exception e) {
            log.error("调用account-service异常", e);
            return null;
        }
    }

    /**
     * 发送POST请求到存储服务
     */
    public boolean saveDataToStorage(Object data) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put(CommonConstants.Header.TRACE_ID, MDC.get("traceId"));

            String jsonData = JSON.toJSONString(data);
            String url = "http://storage-service:8081/api/v1/data";
            String response = httpUtils.postJson(url, jsonData, headers);

            ResponseWrapper<Void> wrapper = JSON.parseObject(response, ResponseWrapper.class);
            return wrapper.isSuccess();

        } catch (Exception e) {
            log.error("存储数据异常", e);
            return false;
        }
    }
}
```

### 集成验证清单

#### ✅ 启动验证
1. 查看启动日志确认common组件加载：
```bash
grep "Common公共组件库已加载" logs/application.log
grep "注册Redis工具类" logs/application.log
grep "注册JWT工具类" logs/application.log
```

2. 健康检查：
```bash
curl http://localhost:8080/actuator/health
```

3. Nacos注册验证：
```bash
# 访问Nacos控制台确认服务已注册
http://nacos:8848/nacos
```

#### ✅ 功能验证
```bash
# 验证Redis连接
curl -X POST http://localhost:8080/api/v1/cache/test \
     -H "Content-Type: application/json" \
     -d '{"key":"test","value":"success"}'

# 验证JWT功能
curl -X POST http://localhost:8080/api/v1/auth/test \
     -H "Content-Type: application/json"

# 验证TraceID传递
curl -H "X-Trace-ID: tr-test-123456" \
     http://localhost:8080/api/v1/health
```

#### ⚠️ 常见集成问题

**问题1：组件未自动装配**
```java
// 错误：启动类包扫描范围不包含common
@SpringBootApplication  // 默认只扫描当前包及子包
public class YourServiceApplication { }

// 正确：明确指定扫描范围
@SpringBootApplication(scanBasePackages = {"com.haven.yourservice", "com.haven.common"})
public class YourServiceApplication { }
```

**问题2：配置不生效**
```yaml
# 错误：没有引入common配置
spring:
  application:
    name: your-service

# 正确：引入公共配置
spring:
  profiles:
    include:
      - base
      - common
  application:
    name: your-service
```

**问题3：依赖冲突**
```bash
# 检查依赖树找出冲突
mvn dependency:tree | grep -A 5 -B 5 "conflicts"

# 排除冲突依赖
<dependency>
    <groupId>com.haven</groupId>
    <artifactId>common</artifactId>
    <version>1.0.0</version>
    <exclusions>
        <exclusion>
            <groupId>conflicted.group</groupId>
            <artifactId>conflicted.artifact</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

## 更新历史
- **v1.0.0** (2025-01-16): 初始版本发布，包含Redis、安全、工具类等核心功能