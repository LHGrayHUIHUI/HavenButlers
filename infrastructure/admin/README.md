# Admin 管理服务

## 🎯 服务概述

HavenButler平台的核心运维管理中心，提供完整的微服务监控、配置管理、告警通知和系统运维功能。

### 服务定位
- **架构层级**：基础设施层 - 运维管理中心
- **核心职责**：统一的系统管理、监控、配置、运维等后台管理功能
- **业务范围**：服务监控、配置管理、日志查看、告警管理、用户管理、数据统计
- **服务特性**：内部专用、高安全性、实时监控、智能告警

## 🛠️ 技术栈

### 后端技术
- **主开发语言**：Java 17
- **核心框架**：Spring Boot 3.1.0、Spring Boot Admin 3.1.0
- **服务发现**：Spring Cloud Alibaba Nacos 2022.0.0.0
- **服务通信**：OpenFeign + LoadBalancer
- **监控组件**：Prometheus、Micrometer
- **配置中心**：Nacos Config
- **定时任务**：Spring Scheduling

### 监控与运维
- **指标收集**：Prometheus + Micrometer
- **服务发现**：Nacos Discovery
- **配置管理**：Nacos Config
- **告警通知**：多渠道通知（邮件、短信、Webhook、微信、钉钉）
- **链路追踪**：集成TraceID
- **日志管理**：结构化日志 + ELK Stack（规划中）

## 🚀 部署信息

### 基础配置
- **Docker镜像**：`smart-home/admin-service:v1.0.0`
- **服务端口**：8888 (Docker网络内)
- **健康检查**：`/actuator/health`
- **管理端点**：`/actuator/*` (所有端点已暴露)

### 环境变量配置
```bash
# 必需配置
NACOS_ADDR=nacos:8848                    # Nacos服务地址
NACOS_NAMESPACE=havenbutler              # Nacos命名空间
NACOS_GROUP=DEFAULT_GROUP                # Nacos配置组

# 监控配置
PROMETHEUS_URL=http://prometheus:9090    # Prometheus服务地址

# 安全配置
SPRING_SECURITY_USER_NAME=admin          # 管理员用户名
SPRING_SECURITY_USER_PASSWORD=havenbutler2025  # 管理员密码

# 告警配置
ALERT_WEBHOOK_URL=http://your-webhook    # 告警Webhook地址
ALERT_EMAIL_ENABLED=false               # 是否启用邮件告警
ALERT_SMS_ENABLED=false                 # 是否启用短信告警
```

### Docker Compose配置示例
```yaml
version: '3.8'
services:
  admin-service:
    image: smart-home/admin-service:v1.0.0
    container_name: admin-service
    ports:
      - "8888:8888"  # 仅测试环境映射
    environment:
      - NACOS_ADDR=nacos:8848
      - NACOS_NAMESPACE=havenbutler
      - PROMETHEUS_URL=http://prometheus:9090
      - SPRING_SECURITY_USER_PASSWORD=havenbutler2025
    networks:
      - smart-home-network
    depends_on:
      - nacos
      - prometheus
    restart: unless-stopped

networks:
  smart-home-network:
    external: true
```

## 📡 API接口和功能

### 🔐 访问控制
⚠️ **安全提示**：Admin服务仅供内部运维使用，不对外暴露

- **访问方式**：通过VPN或堡垒机访问，或Docker内部网络
- **认证方式**：Spring Security Basic认证
- **Web界面**：`http://admin-service:8888` 或 `http://localhost:8888`（测试）
- **默认账号**：admin / admin123（可通过环境变量修改）

### 🚀 核心API接口

#### 1. 服务管理API
```http
# 获取所有注册的Nacos服务
GET /api/service/nacos/services

# 获取指定服务的实例列表
GET /api/service/nacos/{serviceName}/instances

# 获取服务详细信息（包含Nacos元数据）
GET /api/service/nacos/{serviceName}/details

# 获取服务健康状态
GET /api/service/nacos/{serviceName}/health

# 获取系统整体健康状态
GET /api/service/nacos/system/health

# 临时下线服务实例（维护模式）
POST /api/service/nacos/{serviceName}/deregister?ip={ip}&port={port}

# 重新上线服务实例
POST /api/service/nacos/{serviceName}/register?ip={ip}&port={port}
```

#### 2. 告警管理API
```http
# 获取告警列表（支持分页和过滤）
GET /api/alert/list?serviceName={service}&level={level}&status={status}

# 获取告警详情
GET /api/alert/{alertId}

# 处理告警
POST /api/alert/{alertId}/handle

# 忽略告警
POST /api/alert/{alertId}/ignore

# 获取告警规则列表
GET /api/alert/rules?serviceName={service}&enabled={true/false}

# 创建告警规则
POST /api/alert/rule

# 更新告警规则
PUT /api/alert/rule/{ruleId}

# 删除告警规则
DELETE /api/alert/rule/{ruleId}

# 启用/禁用告警规则
PUT /api/alert/rule/{ruleId}/enable?enabled={true/false}

# 获取告警统计
GET /api/alert/statistics

# 测试告警规则
POST /api/alert/rule/test

# 批量处理告警
POST /api/alert/batch/handle
```

#### 3. Spring Boot Admin端点
```http
# Spring Boot Admin主界面
GET /

# 应用列表
GET /instances

# 应用详情
GET /instances/{instanceId}

# 健康检查端点
GET /actuator/health

# 指标端点
GET /actuator/metrics

# Prometheus指标
GET /actuator/prometheus

# 配置属性
GET /actuator/configprops

# 环境信息
GET /actuator/env
```

### 🎯 主要功能模块

#### 1. 🔍 服务监控
- **实时服务发现**：通过Nacos自动发现所有注册的微服务
- **健康状态监控**：实时监控服务实例的健康状态
- **服务实例管理**：支持临时下线/上线服务实例进行维护
- **服务详情查看**：查看服务的元数据、集群信息等
- **系统整体监控**：提供系统级的健康状态评估

#### 2. ⚙️ 配置管理
- **Spring Boot Admin UI**：通过Web界面管理微服务
- **配置属性查看**：查看各服务的配置属性和环境变量
- **动态配置支持**：集成Nacos Config（API层面）
- **配置变更追踪**：支持配置变更的审计和追踪

#### 3. 📊 监控和指标
- **Prometheus集成**：所有管理端点暴露，支持指标收集
- **JVM监控**：内存、GC、线程等JVM级别的监控
- **HTTP请求监控**：请求数量、响应时间、错误率等
- **自定义指标**：支持业务自定义指标的收集和展示

#### 4. 🚨 告警管理
- **智能告警规则**：基于Prometheus指标的灵活告警规则
- **多渠道通知**：支持邮件、短信、Webhook、微信、钉钉通知
- **告警抑制**：避免重复告警的智能抑制机制
- **告警统计分析**：按级别、状态、服务的统计分析
- **定时检查**：每分钟自动检查告警规则

#### 5. 🛡️ 系统管理
- **访问控制**：基于Spring Security的认证和授权
- **操作审计**：所有管理操作的日志记录
- **TraceID支持**：完整的链路追踪标识
- **安全配置**：支持环境变量配置敏感信息

## 监控指标

### 系统级指标
```java
// JVM指标
jvm.memory.used       // 内存使用
jvm.memory.max        // 最大内存
jvm.gc.pause         // GC暂停时间
jvm.threads.live      // 活跃线程数

// 系统指标
system.cpu.usage      // CPU使用率
system.load.average   // 系统负载
disk.free            // 磁盘空间
process.uptime       // 运行时间
```

### 业务指标
```java
// HTTP请求
http.server.requests.count    // 请求数
http.server.requests.errors   // 错误数
http.server.requests.duration // 响应时间

// 数据库
jdbc.connections.active  // 活跃连接
jdbc.connections.idle    // 空闲连接

// 缓存
cache.hits              // 缓存命中
cache.misses            // 缓存未命中
```

## 告警规则

### 系统告警
- CPU使用率 > 80%，持续5分钟
- 内存使用率 > 85%，持续3分钟
- 磁盘使用率 > 90%
- 服务不可用

### 业务告警
- API错误率 > 5%，1分钟内
- 响应时间 > 2秒
- 数据库连接池耗尽
- 消息队列堆积 > 10000

## 数据存储

### 监控数据
- **Prometheus**：时序数据存储
- **保留时间**：30天
- **采样频率**：15秒

### 日志数据
- **Elasticsearch**：日志存储
- **索引策略**：按天分索引
- **保留时间**：90天

### 链路数据
- **Jaeger**：链路数据存储
- **采样率**：1% (正常流量), 100% (异常流量)
- **保留时间**：7天

## 安全措施

### 访问控制
- **IP白名单**：只允许内网IP访问
- **双因素认证**：管理员登录需要验证码
- **操作审计**：所有操作记录日志
- **敏感操作确认**：删除、修改等需二次确认

### 数据安全
- **数据脱敏**：敏感信息展示脱敏
- **加密传输**：TLS 1.3
- **备份策略**：每日备份配置和数据

## 前端界面

### 技术栈
- Vue 3.4 + TypeScript
- Element Plus UI框架
- ECharts 图表库
- Axios HTTP客户端

### 主要页面
1. **Dashboard**：系统总览
2. **服务监控**：微服务状态
3. **日志查询**：日志搜索和分析
4. **链路追踪**：调用链路展示
5. **配置管理**：动态配置
6. **告警管理**：告警规则和历史
7. **系统设置**：用户和权限

## 集成组件

### Spring Boot Admin
```java
@Configuration
@EnableAdminServer
public class AdminServerConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {
        return http
            .authorizeExchange()
            .pathMatchers("/actuator/**").permitAll()
            .anyExchange().authenticated()
            .and()
            .formLogin()
            .and()
            .csrf().disable()
            .build();
    }
}
```

### Prometheus集成
```yaml
management:
  endpoints:
    web:
      exposure:
        include: '*'
  metrics:
    export:
      prometheus:
        enabled: true
  prometheus:
    metrics:
      export:
        enabled: true
```

## 🚀 快速开始

### 本地开发启动
```bash
# 1. 确保Java 17环境
java -version

# 2. 启动依赖服务（Nacos）
docker run -d --name nacos -p 8848:8848 nacos/nacos-server:v2.3.0

# 3. 编译和启动
cd infrastructure/admin
mvn clean compile
mvn spring-boot:run

# 4. 访问管理界面
open http://localhost:8888
# 账号：admin/admin123
```

### Docker独立部署
```bash
# 1. 构建镜像
mvn clean package
docker build -t smart-home/admin-service:v1.0.0 .

# 2. 创建网络
docker network create smart-home-network

# 3. 启动Nacos（依赖服务）
docker run -d --name nacos \
  --network smart-home-network \
  -p 8848:8848 \
  nacos/nacos-server:v2.3.0

# 4. 启动Admin服务
docker run -d --name admin-service \
  --network smart-home-network \
  -p 8888:8888 \
  -e NACOS_ADDR=nacos:8848 \
  -e NACOS_NAMESPACE=havenbutler \
  -e SPRING_SECURITY_USER_PASSWORD=havenbutler2025 \
  smart-home/admin-service:v1.0.0

# 5. 访问管理界面
open http://localhost:8888
# 账号：admin/havenbutler2025
```

### 生产环境部署
```bash
# 使用完整的docker-compose.yml
docker-compose -f docker/production-compose.yml up -d

# 或使用Kubernetes
kubectl apply -f k8s/admin-service.yaml
```

## Docker网络配置
- **网络名称**：smart-home-network
- **容器名称**：admin-service
- **端口映射**：
  - 生产环境：不映射，通过堡垒机访问
  - 测试环境：localhost:8888->8888
- **内部通信**：其他服务通过 `admin-service:8888` 访问

## 注意事项
- Admin服务不应该对外暴露
- 使用独立的管理员账号体系
- 定期清理历史监控数据
- 敏感操作需要多重确认
- 所有操作都要记录审计日志

## 故障排查
1. **服务监控不到**：检查服务是否注册到Nacos
2. **日志查询失败**：检查Elasticsearch连接
3. **指标没有数据**：检查Prometheus配置
4. **链路不完整**：检查TraceID传递

## 🔗 其他服务集成指南

### 微服务接入Admin监控

#### 1. 添加依赖（其他服务）
```xml
<!-- pom.xml -->
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-client</artifactId>
    <version>3.1.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- 如果使用Nacos -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

#### 2. 配置服务注册（其他服务的application.yml）
```yaml
spring:
  application:
    name: your-service-name

  # 如果使用Nacos自动发现（推荐）
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:havenbutler}

  # 或直接配置Admin服务地址
  boot:
    admin:
      client:
        url: http://admin-service:8888
        username: admin
        password: admin123

# 暴露监控端点
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: always
```

#### 3. 启用服务发现（其他服务的启动类）
```java
@SpringBootApplication
@EnableDiscoveryClient  // 启用Nacos服务发现
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

### 告警规则配置示例

#### 创建CPU告警规则
```bash
curl -X POST http://admin-service:8888/api/alert/rule \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -d '{
    "name": "高CPU使用率告警",
    "description": "当CPU使用率超过80%时触发告警",
    "serviceName": "account-service",
    "metricName": "system_cpu_usage",
    "operator": "GREATER_THAN",
    "threshold": 0.8,
    "window": 300,
    "level": "WARNING",
    "messageTemplate": "服务 ${serviceName} CPU使用率 ${currentValue}% 超过阈值 ${threshold}%",
    "enabled": true,
    "notifyType": "WEBHOOK",
    "notifyConfig": {
      "webhook_url": "http://your-webhook-url"
    }
  }'
```

#### 创建内存告警规则
```bash
curl -X POST http://admin-service:8888/api/alert/rule \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -d '{
    "name": "高内存使用率告警",
    "serviceName": "storage-service",
    "metricName": "jvm_memory_used_bytes",
    "operator": "GREATER_THAN",
    "threshold": 858993459.2,
    "level": "CRITICAL",
    "notifyType": "EMAIL",
    "notifyConfig": {
      "email": "admin@example.com"
    }
  }'
```

### 监控数据查询

#### 获取服务健康状态
```bash
# 查看所有注册的服务
curl -u admin:admin123 http://admin-service:8888/api/service/nacos/services

# 查看特定服务的健康状态
curl -u admin:admin123 http://admin-service:8888/api/service/nacos/account-service/health

# 查看系统整体健康状态
curl -u admin:admin123 http://admin-service:8888/api/service/nacos/system/health
```

#### 查看告警信息
```bash
# 获取所有告警
curl -u admin:admin123 http://admin-service:8888/api/alert/list

# 获取特定服务的告警
curl -u admin:admin123 "http://admin-service:8888/api/alert/list?serviceName=account-service"

# 获取告警统计
curl -u admin:admin123 http://admin-service:8888/api/alert/statistics
```

## 🔧 开发和扩展

### 自定义告警规则

```java
// 示例：自定义业务指标告警
@Component
public class CustomAlertRules {

    @Autowired
    private AlertService alertService;

    @PostConstruct
    public void createBusinessAlerts() {
        // 创建用户登录失败率告警
        AlertRule loginFailureRule = new AlertRule();
        loginFailureRule.setName("登录失败率过高");
        loginFailureRule.setServiceName("account-service");
        loginFailureRule.setMetricName("login_failure_rate");
        loginFailureRule.setOperator(AlertRule.Operator.GREATER_THAN);
        loginFailureRule.setThreshold(0.1); // 10%失败率
        loginFailureRule.setLevel(AlertRule.AlertLevel.WARNING);
        loginFailureRule.setNotifyType(AlertRule.NotifyType.WEBHOOK);

        alertService.createAlertRule(loginFailureRule);
    }
}
```

### 集成外部监控系统

```yaml
# 集成Grafana
grafana:
  url: http://grafana:3000
  api-key: your-api-key

# 集成Prometheus
prometheus:
  url: http://prometheus:9090

# 集成ELK
elasticsearch:
  hosts:
    - http://elasticsearch:9200
  username: elastic
  password: changeme
```

## ❓ 常见问题

### Q1: 服务无法发现其他微服务？
**A**: 检查以下配置：
1. 确保Nacos服务正常运行：`curl http://nacos:8848/nacos/`
2. 检查namespace配置是否一致：`havenbutler`
3. 确认其他服务已注册到Nacos：访问Nacos控制台查看服务列表
4. 检查网络连通性：`ping nacos`

### Q2: 告警不触发？
**A**: 排查步骤：
1. 检查告警规则是否启用：`GET /api/alert/rules`
2. 确认Prometheus连接正常：检查`prometheus.url`配置
3. 验证指标名称正确：访问`/actuator/prometheus`查看可用指标
4. 检查阈值设置是否合理

### Q3: 无法访问管理界面？
**A**: 检查以下项：
1. 服务是否正常启动：`curl http://admin-service:8888/actuator/health`
2. 账号密码是否正确：默认`admin/admin123`
3. 网络端口是否开放：`netstat -tlnp | grep 8888`
4. Docker网络配置是否正确

### Q4: 如何配置生产环境的安全性？
**A**: 建议配置：
```yaml
# 修改默认密码
SPRING_SECURITY_USER_PASSWORD=your-strong-password

# 限制访问IP（通过网络策略）
# 仅允许运维网段访问

# 启用HTTPS（通过反向代理）
# 使用Nginx或网关进行SSL终止
```

### Q5: 如何扩展更多监控指标？
**A**: 扩展步骤：
1. 在业务服务中添加自定义指标：
```java
@Component
public class CustomMetrics {
    private final Counter loginAttempts = Counter.builder("login_attempts_total")
        .description("Total login attempts")
        .register(Metrics.globalRegistry);

    public void recordLogin() {
        loginAttempts.increment();
    }
}
```

2. 在Admin服务中创建对应的告警规则
3. 通过Prometheus查询验证指标

## 📈 性能优化

### 内存优化
```bash
# JVM调优参数示例
JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 告警频率控制
```yaml
# 告警抑制配置
alert:
  suppression:
    min-interval: 300  # 最小告警间隔（秒）
    max-alerts-per-hour: 10  # 每小时最大告警数
```

## 🔒 安全最佳实践

1. **网络隔离**：Admin服务应仅在内网访问
2. **强密码**：生产环境必须修改默认密码
3. **访问审计**：启用操作日志记录
4. **定期更新**：及时更新依赖版本
5. **备份策略**：定期备份告警规则和配置

## 📊 监控建议

### 关键指标监控
- **服务可用性**：健康检查成功率 > 99.9%
- **响应时间**：API响应时间 < 500ms
- **错误率**：HTTP错误率 < 1%
- **资源使用**：CPU < 70%, 内存 < 80%

### 告警级别建议
- **CRITICAL**：服务不可用、数据丢失
- **WARNING**：性能下降、资源紧张
- **INFO**：状态变更、定期报告

## 🚀 版本历史

### v1.0.0 (2025-09-18)
- ✅ 集成Nacos服务发现和配置管理
- ✅ 完整的Spring Boot Admin监控功能
- ✅ Prometheus指标收集和暴露
- ✅ 智能告警规则和多渠道通知
- ✅ 服务健康监控和管理API
- ✅ Docker和本地环境双重支持
- ✅ 安全认证和访问控制
- ✅ TraceID链路追踪支持

### 未来规划
- 🔄 集成ELK Stack日志管理
- 🔄 Jaeger链路追踪集成
- 🔄 Vue3前端管理界面
- 🔄 Kubernetes集群监控
- 🔄 AI智能运维功能

---

## 📞 技术支持

- **项目地址**: HavenButler/infrastructure/admin
- **问题反馈**: 通过项目Issue提交
- **文档更新**: 同步更新README.md

**admin服务是HavenButler平台的运维管理核心，为整个微服务架构提供统一的监控、告警和管理能力！** 🎯