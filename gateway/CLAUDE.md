# Gateway CLAUDE.md

## 模块概述
Gateway是HavenButler平台的统一入口服务，所有外部请求必须经过网关进行路由、鉴权、限流等处理后才能访问内部微服务。作为系统的第一道防线，必须保证高可用性和安全性。

## 开发指导原则

### 1. 核心设计原则
- **高可用原则**：网关必须支持集群部署，单点故障不影响整体服务
- **安全第一原则**：所有请求必须经过安全检查，防止恶意攻击
- **性能优先原则**：网关处理时间控制在50ms以内
- **可观测性原则**：完整的日志、监控、链路追踪

### 2. 与storage-service的集成
```yaml
# 所有需要持久化的数据必须通过storage-service
# 网关不直接连接数据库
存储需求:
  - JWT黑名单: 通过storage-service的Redis接口
  - 限流计数器: 通过storage-service的Redis接口
  - 访问日志: 通过storage-service的MongoDB接口
  - 路由配置: 从Nacos配置中心读取
```

### 3. 安全防护实施

#### 3.1 WAF规则配置
```java
/**
 * WAF防护规则
 * 必须拦截的攻击类型：
 * 1. SQL注入
 * 2. XSS攻击
 * 3. 路径遍历
 * 4. 命令注入
 */
public class WafFilter {
    // SQL注入检测正则
    private static final String SQL_INJECTION_PATTERN = 
        ".*(\\b(SELECT|UPDATE|DELETE|INSERT|DROP|UNION|ALTER)\\b).*";
    
    // XSS攻击检测
    private static final String XSS_PATTERN = 
        ".*(<script|javascript:|onerror=|onclick=).*";
}
```

#### 3.2 JWT鉴权流程
```java
/**
 * JWT鉴权核心流程
 * 1. 从Header提取Token
 * 2. 验证Token签名
 * 3. 检查Token是否在黑名单
 * 4. 解析用户权限信息
 * 5. 将用户信息注入到请求头
 */
public class JwtAuthFilter {
    // Token有效期：2小时
    private static final long ACCESS_TOKEN_VALIDITY = 2 * 60 * 60 * 1000;
    // Refresh Token有效期：7天
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000;
}
```

### 4. 限流策略实现

#### 4.1 多级限流
```yaml
限流级别:
  全局限流:
    - QPS: 10000
    - 突发流量: 15000
  用户级限流:
    - 普通用户: 100/分钟
    - VIP用户: 500/分钟
  IP级限流:
    - 单IP: 200/分钟
  API级限流:
    - AI接口: 10/分钟
    - 普通查询: 100/分钟
```

#### 4.2 限流算法选择
- 令牌桶算法：适用于允许突发流量的场景
- 漏桶算法：适用于平滑限流场景
- 滑动窗口：精确控制时间窗口内的请求数

### 5. 路由配置管理

#### 5.1 路由规则示例
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: account-service
          uri: lb://account-service
          predicates:
            - Path=/api/v1/account/**
          filters:
            - RewritePath=/api/v1/account/(?<segment>.*), /$\{segment}
            - AddRequestHeader=X-Service-Name, account
            - RateLimit=100, 200, #{@userKeyResolver}
```

#### 5.2 动态路由更新
- 监听Nacos配置变更事件
- 热更新路由规则，无需重启
- 路由变更审计日志

### 6. 与其他服务的交互

#### 6.1 调用account-service进行权限验证
```java
/**
 * 对于需要特殊权限的接口，需要调用account-service验证
 * 例如：管理员接口、敏感数据接口
 */
@Component
public class PermissionFilter {
    @Autowired
    private AccountServiceClient accountClient;
    
    public Mono<Boolean> checkPermission(String userId, String resource) {
        return accountClient.checkUserPermission(userId, resource);
    }
}
```

#### 6.2 日志发送到storage-service
```java
/**
 * 访问日志异步发送到storage-service
 * 包含：请求路径、用户ID、响应时间、状态码等
 */
@Component
public class AccessLogFilter {
    @Autowired
    private StorageServiceClient storageClient;
    
    public void logAccess(AccessLog log) {
        // 异步发送，避免影响主流程
        CompletableFuture.runAsync(() -> 
            storageClient.saveAccessLog(log)
        );
    }
}
```

### 7. 性能优化策略

#### 7.1 缓存使用
- 路由信息缓存：5分钟
- 用户权限缓存：10分钟
- JWT解析结果缓存：Token有效期内

#### 7.2 异步处理
- 日志记录异步化
- 非关键路径异步执行
- 使用WebFlux响应式编程

#### 7.3 连接池优化
```yaml
# HTTP连接池配置
http-client:
  pool:
    max-connections: 1000
    max-idle-time: 30s
    max-life-time: 60s
    eviction-interval: 30s
```

### 8. 监控告警配置

#### 8.1 关键指标
- 请求QPS
- 平均响应时间
- 错误率
- 限流触发次数
- JWT验证失败次数

#### 8.2 告警规则
```yaml
告警规则:
  - 错误率 > 1%: P2级告警
  - 响应时间 > 200ms: P3级告警
  - 服务不可用: P1级告警
  - QPS > 8000: 预警通知
```

### 9. 故障处理预案

#### 9.1 下游服务故障
```java
/**
 * 熔断降级策略
 */
@Component
public class CircuitBreakerConfig {
    // 失败率超过50%触发熔断
    private static final float FAILURE_RATE_THRESHOLD = 50.0f;
    // 熔断时间窗口：10秒
    private static final int SLIDING_WINDOW_SIZE = 10;
}
```

#### 9.2 网关过载保护
- 触发限流保护
- 拒绝低优先级请求
- 返回503状态码

### 10. 开发注意事项

#### 10.1 不要在网关做的事
- 复杂的业务逻辑处理
- 大量的数据转换
- 同步的阻塞调用
- 直接访问数据库

#### 10.2 必须在网关做的事
- 统一的安全检查
- 请求路由分发
- 协议转换
- 限流熔断
- 链路追踪注入

### 11. 测试要点

#### 11.1 性能测试
```bash
# 使用 Gatling 进行压测
# 目标：10000 QPS，P99延迟 < 100ms
```

#### 11.2 安全测试
```bash
# OWASP ZAP 扫描
# SQL注入测试
# XSS攻击测试
# DDoS攻击模拟
```

#### 11.3 容错测试
- 下游服务宕机测试
- 网络延迟测试
- 熔断器触发测试

### 12. 部署建议
- 至少部署3个实例保证高可用
- 使用Nginx或F5做前置负载均衡
- 配置健康检查和自动重启
- 监控告警必须配置