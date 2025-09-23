# Storage Service API集成指南

## 🎯 服务概述

Storage Service 是 HavenButler 平台的核心数据存储代理服务，为所有微服务提供统一的数据访问接口。

- **服务名称**: storage-service
- **端口**: 8081
- **访问方式**: HTTP/REST API + TCP代理
- **网关路由**: `/api/v1/storage/*`

## 🏗️ 架构特点

- **双重接入方式**: TCP协议代理 + REST API接口
- **数据隔离**: 基于 familyId 的多租户数据隔离
- **多存储支持**: PostgreSQL、MongoDB、Redis、MinIO
- **安全保障**: Service-Key + JWT 双重认证
- **链路追踪**: 全链路 TraceID 支持

## 🔧 微服务集成配置

### 1. 添加依赖 (Java 微服务)

#### Maven 依赖
```xml
<dependencies>
  <!-- Base-Model 统一日志基础设施 -->
  <dependency>
    <groupId>com.haven</groupId>
    <artifactId>base-model</artifactId>
    <version>1.0.0</version>
  </dependency>

  <!-- Spring Cloud LoadBalancer -->
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
  </dependency>
</dependencies>
```

### 2. 配置方式选择

#### 方式一：TCP代理访问（推荐）
```yaml
# application.yml - 通过TCP代理访问数据库
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://storage-service:5433/smarthome  # 代理端口
    username: ${POSTGRESQL_USER:postgres}
    password: ${POSTGRESQL_PASSWORD:password}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2

  data:
    mongodb:
      host: storage-service
      port: 27018              # MongoDB代理端口
      database: smarthome

  redis:
    host: storage-service
    port: 6380               # Redis代理端口
    password: ${REDIS_PASSWORD:password}

# 统一日志配置
haven:
  logging:
    enabled: true
    async: true
    aspect:
      enabled: true

storage:
  service:
    url: http://storage-service:8081
```

#### 方式二：REST API访问
```yaml
# application.yml - 通过REST API访问
storage:
  service:
    url: http://storage-service:8081
  client:
    service-key: ${SERVICE_KEY:key_service_xxx}
    timeout: 5000
    retry:
      max-attempts: 3
      delay: 1000

# 统一日志配置
haven:
  logging:
    enabled: true
    async: true
    storage-service-url: http://storage-service:8081
```

## 🔐 认证方式

### 1. TCP代理认证
TCP代理使用标准数据库认证，无需额外配置Service-Key。

### 2. REST API认证

#### 服务间认证
```http
POST /api/v1/storage/postgresql/query
Service-Key: key_account_xxx
X-Family-ID: family123
X-Trace-ID: tr-20240101-120000-123456
Content-Type: application/json
```

#### 用户认证
```http
POST /api/v1/storage/files/upload
Authorization: Bearer <jwt_token>
X-Family-ID: family123
X-Trace-ID: tr-20240101-120000-123456
Content-Type: multipart/form-data
```

## 📋 API接口文档

### 1. 数据存储接口

#### PostgreSQL数据操作
```bash
# 查询数据
curl -X POST http://storage-service:8081/api/v1/storage/postgresql/query \
  -H "Service-Key: key_account_xxx" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "table": "users",
    "filters": {
      "status": "active"
    },
    "options": {
      "limit": 10,
      "orderBy": "created_at DESC"
    }
  }'

# 插入数据
curl -X POST http://storage-service:8081/api/v1/storage/postgresql/insert \
  -H "Service-Key: key_account_xxx" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "table": "users",
    "data": {
      "username": "张三",
      "email": "zhangsan@example.com",
      "status": "active"
    }
  }'

# 更新数据
curl -X POST http://storage-service:8081/api/v1/storage/postgresql/update \
  -H "Service-Key: key_account_xxx" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "table": "users",
    "filters": {
      "id": 123
    },
    "data": {
      "status": "inactive"
    }
  }'

# 删除数据
curl -X POST http://storage-service:8081/api/v1/storage/postgresql/delete \
  -H "Service-Key: key_account_xxx" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "table": "users",
    "filters": {
      "id": 123
    }
  }'
```

#### MongoDB文档操作
```bash
# 查询文档
curl -X POST http://storage-service:8081/api/v1/storage/mongodb/find \
  -H "Service-Key: key_device_xxx" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "collection": "device_states",
    "filters": {
      "deviceId": "device_001",
      "timestamp": {
        "$gte": "2024-01-15T00:00:00Z"
      }
    },
    "options": {
      "sort": {"timestamp": -1},
      "limit": 100
    }
  }'

# 插入文档
curl -X POST http://storage-service:8081/api/v1/storage/mongodb/insert \
  -H "Service-Key: key_device_xxx" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "collection": "device_states",
    "data": {
      "deviceId": "device_001",
      "status": "online",
      "temperature": 25.5,
      "humidity": 60,
      "timestamp": "2024-01-15T10:30:00Z"
    }
  }'
```

#### Redis缓存操作
```bash
# 设置缓存
curl -X POST http://storage-service:8081/api/v1/storage/redis/set \
  -H "Service-Key: key_account_xxx" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "session:user123",
    "value": {
      "userId": "user123",
      "username": "张三",
      "loginTime": "2024-01-15T10:30:00Z"
    },
    "ttl": 3600
  }'

# 获取缓存
curl -X POST http://storage-service:8081/api/v1/storage/redis/get \
  -H "Service-Key: key_account_xxx" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "session:user123"
  }'

# 删除缓存
curl -X POST http://storage-service:8081/api/v1/storage/redis/delete \
  -H "Service-Key: key_account_xxx" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "session:user123"
  }'
```

### 2. 统一日志接口

#### 日志接收接口
```bash
# 统一日志入口
curl -X POST http://storage-service:8081/api/v1/logs/unified \
  -H "X-Family-ID: family123" \
  -H "X-Trace-ID: tr-20240101-120000-123456" \
  -H "Content-Type: application/json" \
  -d '{
    "logType": "OPERATION",
    "operationType": "USER_LOGIN",
    "description": "用户登录",
    "userId": "user123",
    "serviceId": "account-service",
    "executionTime": 150,
    "riskLevel": "LOW"
  }'

# 按类型接收日志
curl -X POST http://storage-service:8081/api/v1/logs/operation \
  -H "X-Family-ID: family123" \
  -H "X-Trace-ID: tr-20240101-120000-123456" \
  -H "Content-Type: application/json" \
  -d '{
    "operationType": "DATA_EXPORT",
    "description": "用户导出数据",
    "userId": "user123",
    "serviceId": "account-service",
    "executionTime": 2500,
    "riskLevel": "MEDIUM"
  }'

# 批量日志接收
curl -X POST http://storage-service:8081/api/v1/logs/performance/batch \
  -H "X-Family-ID: family123" \
  -H "X-Trace-ID: tr-20240101-120000-123456" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "operationType": "API_CALL",
      "description": "API调用性能记录",
      "executionTime": 120
    },
    {
      "operationType": "DB_QUERY",
      "description": "数据库查询性能",
      "executionTime": 80
    }
  ]'
```

#### 日志查询接口
```bash
# 查询操作日志
curl -X GET "http://storage-service:8081/api/v1/logs/operations?page=0&size=20&operationType=USER_LOGIN" \
  -H "X-Family-ID: family123"

# 获取安全事件统计
curl -X GET "http://storage-service:8081/api/v1/logs/security/stats?days=7" \
  -H "X-Family-ID: family123"

# 获取性能指标统计
curl -X GET "http://storage-service:8081/api/v1/logs/performance/stats?hours=24" \
  -H "X-Family-ID: family123"

# 实时监控指标
curl -X GET "http://storage-service:8081/api/v1/logs/metrics/realtime" \
  -H "X-Family-ID: family123"
```

### 3. 文件管理接口

#### 文件操作
```bash
# 文件上传
curl -X POST http://storage-service:8081/api/v1/storage/files/upload \
  -H "Authorization: Bearer <jwt_token>" \
  -H "X-Family-ID: family123" \
  -F "file=@/path/to/file.jpg" \
  -F "category=image" \
  -F "tags=profile,avatar"

# 文件下载
curl -X GET http://storage-service:8081/api/v1/storage/files/download/file123 \
  -H "Authorization: Bearer <jwt_token>" \
  -H "X-Family-ID: family123" \
  -o downloaded_file.jpg

# 文件信息查询
curl -X GET http://storage-service:8081/api/v1/storage/files/info/file123 \
  -H "Authorization: Bearer <jwt_token>" \
  -H "X-Family-ID: family123"

# 删除文件
curl -X DELETE http://storage-service:8081/api/v1/storage/files/file123 \
  -H "Authorization: Bearer <jwt_token>" \
  -H "X-Family-ID: family123"
```

#### 文件管理
```bash
# 文件列表
curl -X GET "http://storage-service:8081/api/v1/storage/files/list?page=0&size=20&category=image" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "X-Family-ID: family123"

# 文件搜索
curl -X POST http://storage-service:8081/api/v1/storage/files/search \
  -H "Authorization: Bearer <jwt_token>" \
  -H "X-Family-ID: family123" \
  -H "Content-Type: application/json" \
  -d '{
    "keywords": "avatar profile",
    "category": "image",
    "dateRange": {
      "start": "2024-01-01",
      "end": "2024-01-31"
    }
  }'
```

## 💻 Java微服务集成示例

### 1. 使用TCP代理的标准数据库操作

```java
@Service
@RequiredArgsConstructor
public class UserService {

    // 直接使用标准Spring Data JPA
    private final UserRepository userRepository;

    // 自动通过TCP代理访问数据库，无需修改代码
    public User createUser(CreateUserRequest request) {
        User user = User.builder()
            .familyId(request.getFamilyId())
            .username(request.getUsername())
            .email(request.getEmail())
            .build();

        return userRepository.save(user);  // 通过代理端口:5433访问
    }

    public List<User> findActiveUsers(String familyId) {
        return userRepository.findByFamilyIdAndStatus(familyId, "active");
    }
}
```

### 2. 使用REST API的数据操作

```java
@Service
@RequiredArgsConstructor
public class StorageClientService {

    private final RestTemplate restTemplate;

    @Value("${storage.service.url}")
    private String storageServiceUrl;

    @Value("${storage.client.service-key}")
    private String serviceKey;

    public <T> T queryData(String table, Map<String, Object> filters, Class<T> responseType) {
        Map<String, Object> requestBody = Map.of(
            "table", table,
            "filters", filters
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Service-Key", serviceKey);
        headers.set("X-Family-ID", getCurrentFamilyId());
        headers.set("X-Trace-ID", getCurrentTraceId());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        return restTemplate.postForObject(
            storageServiceUrl + "/api/v1/storage/postgresql/query",
            entity,
            responseType
        );
    }

    public void insertData(String table, Map<String, Object> data) {
        Map<String, Object> requestBody = Map.of(
            "table", table,
            "data", data
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Service-Key", serviceKey);
        headers.set("X-Family-ID", getCurrentFamilyId());
        headers.set("X-Trace-ID", getCurrentTraceId());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        restTemplate.postForObject(
            storageServiceUrl + "/api/v1/storage/postgresql/insert",
            entity,
            Map.class
        );
    }
}
```

### 3. 统一日志集成

#### 注解式日志
```java
@RestController
public class AccountController {

    @LogOperation(
        operationType = "USER_LOGIN",
        description = "用户登录",
        recordExecutionTime = true
    )
    @PostMapping("/login")
    public LoginResult login(@RequestBody LoginRequest request) {
        // 自动记录操作日志到storage-service
        return loginService.login(request);
    }

    @LogOperation(
        operationType = "PASSWORD_CHANGE",
        description = "密码修改",
        logType = LogEvent.LogType.SECURITY,
        riskLevel = LogEvent.RiskLevel.MEDIUM
    )
    @PostMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }
}
```

#### 编程式日志
```java
@Service
public class DeviceService {

    @Autowired
    private LogClient logClient;

    public void controlDevice(String familyId, String userId, String deviceId, String command) {
        long startTime = System.currentTimeMillis();

        try {
            // 业务逻辑
            deviceManager.sendCommand(deviceId, command);

            // 记录成功日志
            logClient.logOperationWithTiming(familyId, userId, "DEVICE_CONTROL",
                "控制设备: " + deviceId, startTime);

        } catch (Exception e) {
            // 记录错误日志
            logClient.logError(familyId, userId, "DEVICE_CONTROL",
                "设备控制失败: " + e.getMessage(), e);
            throw e;
        }
    }
}
```

## 🔍 健康检查和监控

### 服务健康检查
```bash
# 服务整体健康状态
curl http://storage-service:8081/actuator/health

# 数据存储健康状态
curl http://storage-service:8081/api/v1/storage/health

# TCP代理健康状态
curl http://storage-service:8081/api/v1/proxy/health

# 日志系统健康状态
curl http://storage-service:8081/api/v1/logs/health
```

### 监控指标
```bash
# Prometheus指标
curl http://storage-service:8081/actuator/prometheus

# 自定义指标
curl http://storage-service:8081/actuator/metrics/storage.request.count
curl http://storage-service:8081/actuator/metrics/proxy.connections.active
curl http://storage-service:8081/actuator/metrics/logs.processing.rate
```

## ⚠️ 重要注意事项

### 1. 数据安全
- **强制familyId**: 所有数据操作必须包含familyId进行数据隔离
- **服务认证**: 必须使用正确的Service-Key进行服务间认证
- **权限校验**: 基于JWT的用户权限验证

### 2. 性能限制
- **全局QPS**: 1000次/秒
- **单Family QPS**: 100次/秒
- **文件上传**: 最大500MB
- **单次查询**: 最大1000条记录

### 3. 最佳实践
- **优先使用TCP代理**: 性能更好，功能更完整
- **异步日志记录**: 不会阻塞业务流程
- **连接池配置**: 合理配置数据库连接池大小
- **错误处理**: 实现完善的错误处理和重试机制

### 4. 故障处理
- **连接超时**: 检查网络连接和服务状态
- **认证失败**: 验证Service-Key和JWT Token
- **数据隔离**: 确保familyId正确传递
- **性能问题**: 检查连接池配置和缓存策略

---

**Storage Service API集成指南** - 为HavenButler平台提供完整的数据访问和日志服务 🚀