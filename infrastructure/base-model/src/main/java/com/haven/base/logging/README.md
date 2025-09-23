# HavenButler统一日志系统使用指南

## 🎯 概述

HavenButler统一日志系统基于base-model模块，为所有微服务提供统一的日志记录能力。日志最终存储在storage-service中，支持五种类型的日志：操作日志、安全日志、性能日志、业务日志、错误日志。

## 🏗️ 架构设计

```
微服务A  ┐
微服务B  ├── 继承base-model ── LogClient ── HTTP API ──> storage-service
微服务C  ┘                   (异步发送)                    (统一存储)
```

### 核心组件

1. **LogEvent**: 统一的日志事件模型
2. **LogClient**: 日志发送客户端，封装所有日志操作
3. **LogAspect**: AOP切面，支持注解式日志记录
4. **LogAutoConfiguration**: 自动配置，微服务零配置接入

## 🚀 快速开始

### 1. 添加依赖

微服务pom.xml中已包含base-model依赖，无需额外配置：

```xml
<dependency>
    <groupId>com.haven</groupId>
    <artifactId>base-model</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置属性

application.yml中添加配置：

```yaml
# 日志系统配置（可选，有默认值）
haven:
  logging:
    enabled: true              # 启用日志系统
    async: true               # 异步模式
    aspect:
      enabled: true           # 启用AOP切面

# storage-service地址
storage:
  service:
    url: http://storage-service:8081

# 服务名称（用于标识日志来源）
spring:
  application:
    name: account-service
```

## 💡 使用方式

### 方式一：注解式日志（推荐）

最简单的使用方式，在方法上添加@LogOperation注解：

```java
@RestController
public class AccountController {

    // 基础用法
    @LogOperation(
        operationType = "USER_LOGIN",
        description = "用户登录"
    )
    public LoginResult login(@RequestBody LoginRequest request) {
        // 业务逻辑
        return loginService.login(request);
    }

    // 记录执行时间和参数
    @LogOperation(
        operationType = "USER_REGISTER",
        description = "用户注册",
        recordExecutionTime = true,
        recordParams = true
    )
    public RegisterResult register(@RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    // 安全日志
    @LogOperation(
        operationType = "PASSWORD_CHANGE",
        description = "密码修改",
        logType = LogEvent.LogType.SECURITY,
        riskLevel = LogEvent.RiskLevel.MEDIUM
    )
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }

    // 业务日志
    @LogOperation(
        operationType = "PROFILE_UPDATE",
        description = "更新用户资料",
        logType = LogEvent.LogType.BUSINESS,
        businessModule = "USER_MANAGEMENT",
        businessScenario = "PROFILE_EDIT"
    )
    public void updateProfile(@RequestBody UpdateProfileRequest request) {
        userService.updateProfile(request);
    }
}
```

### 方式二：编程式日志

通过注入LogClient手动记录日志：

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
            logClient.logOperationWithTiming(
                familyId, userId,
                "DEVICE_CONTROL",
                "控制设备: " + deviceId + " 执行: " + command,
                startTime
            );

            // 记录业务日志
            logClient.logDeviceOperation(familyId, userId, deviceId, command, "成功");

        } catch (Exception e) {
            // 记录错误日志
            logClient.logException(familyId, userId, "DEVICE_CONTROL", e);
            throw e;
        }
    }

    public void monitorPerformance() {
        // 记录性能指标
        double responseTime = measureResponseTime();
        logClient.logResponseTime("device_status_check", (long) responseTime);

        // 记录QPS
        int currentQPS = getCurrentQPS();
        logClient.logQPS("device_control", currentQPS);
    }

    public void handleSecurityEvent(String familyId, String clientIP, String eventType) {
        // 记录安全事件
        logClient.logSecurityEvent(
            familyId, clientIP, eventType,
            LogEvent.RiskLevel.HIGH,
            "检测到异常设备访问"
        );
    }
}
```

### 方式三：构建自定义日志

使用LogEvent.builder()构建复杂日志：

```java
@Service
public class FileService {

    @Autowired
    private LogClient logClient;

    public void uploadFile(String familyId, String userId, MultipartFile file) {
        // 构建复杂日志事件
        LogEvent logEvent = LogEvent.createOperationLog(
            "file-manager-service", familyId, userId,
            "FILE_UPLOAD", "文件上传"
        )
        .addMetadata("fileName", file.getOriginalFilename())
        .addMetadata("fileSize", file.getSize())
        .addMetadata("contentType", file.getContentType())
        .addTag("fileType", getFileType(file))
        .addTag("uploadSource", "web")
        .withExecutionTime(System.currentTimeMillis());

        // 发送自定义日志
        logClient.sendCustomLog(logEvent, "operation");
    }
}
```

## 📊 日志类型详解

### 1. 操作日志（OPERATION）
记录用户操作和系统操作：

```java
// 用户操作
logClient.logOperation(familyId, userId, "USER_LOGIN", "用户登录");

// 系统操作
logClient.logOperation("system", null, "SYSTEM_STARTUP", "系统启动");

// 带执行时间
logClient.logOperationWithTiming(familyId, userId, "DATABASE_QUERY", "数据库查询", startTime);

// 带结果状态
logClient.logOperationResult(familyId, userId, "FILE_UPLOAD", "文件上传",
    LogEvent.ResultStatus.SUCCESS, null);
```

### 2. 安全日志（SECURITY）
记录安全相关事件：

```java
// 认证失败
logClient.logAuthFailure(familyId, clientIP, userId, "密码错误");

// 访问拒绝
logClient.logAccessDenied(familyId, clientIP, userId, "/admin/users");

// 危险操作拦截
logClient.logDangerousOperationBlocked(familyId, clientIP, userId, "DELETE * FROM users");

// 自定义安全事件
logClient.logSecurityEvent(familyId, clientIP, "SUSPICIOUS_LOGIN",
    LogEvent.RiskLevel.HIGH, "异常登录行为");
```

### 3. 性能日志（PERFORMANCE）
记录系统性能指标：

```java
// 响应时间
logClient.logResponseTime("user_query", 150L);

// QPS统计
logClient.logQPS("api_call", 1200);

// 错误率
logClient.logErrorRate("payment_process", 2.5);

// 自定义性能指标
logClient.logPerformanceMetric("memory_usage", 85.6, "percent");
```

### 4. 业务日志（BUSINESS）
记录业务流程和状态：

```java
// 业务操作
logClient.logBusiness(familyId, userId, "ORDER_MANAGEMENT", "ORDER_CREATE", "创建订单");

// 设备操作
logClient.logDeviceOperation(familyId, userId, "device123", "开启", "成功");

// 文件操作
logClient.logFileOperation(familyId, userId, "document.pdf", "下载", "成功");
```

### 5. 错误日志（ERROR）
记录系统错误和异常：

```java
// 记录异常
logClient.logException(familyId, userId, "DATABASE_CONNECTION", exception);

// 记录错误
logClient.logError(familyId, userId, "PAYMENT_PROCESS", "E001", "支付网关超时");
```

## 🔧 高级功能

### 1. 批量日志
提高性能，减少网络调用：

```java
List<LogEvent> logEvents = Arrays.asList(
    LogEvent.createOperationLog("service", familyId, userId, "OP1", "操作1"),
    LogEvent.createOperationLog("service", familyId, userId, "OP2", "操作2")
);

logClient.batchSendLogs(logEvents, "operation");
```

### 2. 链路追踪
自动传递TraceID：

```java
// 自动获取当前TraceID
logClient.log(familyId, userId, "API_CALL", "接口调用");

// 使用自定义TraceID
logClient.logWithTrace(familyId, userId, "ASYNC_TASK", "异步任务", customTraceId);
```

### 3. 元数据和标签
丰富日志内容：

```java
LogEvent event = LogEvent.createOperationLog(...)
    .addMetadata("requestId", requestId)
    .addMetadata("userAgent", userAgent)
    .addTag("environment", "production")
    .addTag("version", "1.2.3");
```

## 📈 最佳实践

### 1. 命名规范
```java
// 操作类型命名：动词_名词
"USER_LOGIN", "FILE_UPLOAD", "DEVICE_CONTROL", "ORDER_CREATE"

// 业务模块命名：功能_管理
"USER_MANAGEMENT", "DEVICE_CONTROL", "FILE_MANAGEMENT"

// 业务场景命名：具体场景
"USER_REGISTER", "PASSWORD_RESET", "PROFILE_UPDATE"
```

### 2. 敏感信息处理
```java
// ❌ 错误：记录敏感信息
logClient.log(familyId, userId, "LOGIN", "用户密码: " + password);

// ✅ 正确：不记录敏感信息
logClient.log(familyId, userId, "LOGIN", "用户登录成功");

// ✅ 正确：脱敏处理
logClient.log(familyId, userId, "LOGIN", "手机号: " + maskPhone(phone));
```

### 3. 性能考虑
```java
// ✅ 推荐：使用异步日志
@LogOperation(operationType = "QUERY", async = true)

// ✅ 推荐：批量发送
logClient.batchSendLogs(logEvents, "operation");

// ⚠️ 注意：避免在循环中记录大量日志
for (Device device : devices) {
    // 考虑批量处理或采样记录
}
```

### 4. 错误处理
```java
// ✅ 日志记录不应该影响业务流程
try {
    // 业务逻辑
    businessLogic();
} catch (BusinessException e) {
    // 记录日志但不影响异常传播
    logClient.logException(familyId, userId, "BUSINESS_ERROR", e);
    throw e; // 继续抛出业务异常
}
```

## 🔍 查询和分析

日志记录后，可通过storage-service的API进行查询：

```bash
# 查询操作日志
GET /api/v1/logs/operations?familyId=family123&startTime=2024-01-01T00:00:00

# 查询安全事件
GET /api/v1/logs/security/stats?riskLevel=HIGH

# 查询性能指标
GET /api/v1/logs/performance/stats?metricName=response_time
```

## 🚨 注意事项

1. **familyId必需**：所有业务日志都需要提供familyId，用于数据隔离
2. **异步特性**：日志记录是异步的，不保证立即可查
3. **网络依赖**：依赖storage-service，服务不可用时日志会丢失
4. **存储容量**：大量日志会占用存储空间，注意清理策略
5. **性能影响**：虽然是异步，但过量日志仍会影响性能

## 📝 配置参考

完整的application.yml配置：

```yaml
haven:
  logging:
    enabled: true
    async: true
    aspect:
      enabled: true
      record-params: false      # 全局是否记录参数
      record-result: false      # 全局是否记录返回值
    client:
      batch-size: 100          # 批量发送大小
      timeout: 5000            # 超时时间（毫秒）
      retry-times: 3           # 重试次数

storage:
  service:
    url: http://storage-service:8081
    timeout: 10000

# 异步线程池配置
spring:
  task:
    execution:
      pool:
        core-size: 5
        max-size: 20
        queue-capacity: 10000
```

通过以上文档，各微服务开发人员可以快速接入统一日志系统，实现标准化的日志记录和管理。