# Admin 管理服务

## 服务定位
- **架构层级**：基础设施层
- **核心职责**：提供系统管理、监控、配置、运维等后台管理功能
- **业务范围**：服务监控、配置管理、日志查看、系统设置、用户管理、数据统计

## 技术栈
- **主开发语言**：Java 17
- **核心框架**：Spring Boot 3.1.0、Spring Boot Admin 3.1.0
- **监控组件**：Prometheus、Grafana、Micrometer
- **日志系统**：ELK Stack (Elasticsearch + Logstash + Kibana)
- **链路追踪**：Jaeger / Zipkin
- **配置中心**：Nacos
- **前端框架**：Vue3 + Element Plus

## 部署信息
- **Docker镜像**：`smart-home/admin-service:v1.0.0`
- **内部端口**：8888 (Docker网络内)
- **健康检查**：`/actuator/health`
- **环境变量**：
  ```bash
  NACOS_ADDR=nacos:8848
  STORAGE_SERVICE_URL=http://storage-service:8080
  GATEWAY_URL=http://gateway:8080
  PROMETHEUS_URL=http://prometheus:9090
  ELASTICSEARCH_URL=http://elasticsearch:9200
  ```

## 接口信息

### 管理接口
⚠️ **注意**：Admin服务不对外暴露，仅供内部运维使用

- **访问方式**：通过VPN或堡垒机访问
- **认证方式**：独立的管理员账号体系
- **Web界面**：`http://admin-service:8888`

### 主要功能模块

#### 1. 服务监控
- **服务列表**：实时显示所有微服务状态
- **健康检查**：各服务健康状态监控
- **指标监控**：CPU、内存、线程、GC等
- **告警管理**：告警规则配置和通知

#### 2. 配置管理
- **配置中心**：Nacos配置管理UI
- **动态配置**：实时修改配置不重启
- **配置历史**：配置变更记录和回滚
- **配置同步**：多环境配置同步

#### 3. 日志管理
- **日志查询**：全文搜索和结构化查询
- **日志分析**：错误统计、访问分析
- **日志告警**：异常日志告警
- **日志存档**：历史日志归档

#### 4. 链路追踪
- **调用链路**：完整请求链路可视化
- **性能分析**：各节点耗时分析
- **依赖关系**：服务依赖图谱
- **异常追踪**：异常请求快速定位

#### 5. 系统管理
- **用户管理**：管理员账号管理
- **权限管理**：RBAC权限模型
- **操作审计**：所有操作日志记录
- **系统设置**：全局参数配置

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

## 独立测试部署
```bash
# 启动依赖服务
docker-compose -f docker/admin-compose.yml up -d

# 启动Admin服务
docker run -d \
  --name admin-service \
  --network smart-home-network \
  -p 8888:8888 \
  smart-home/admin-service:v1.0.0

# 访问管理界面
http://localhost:8888
# 默认账号：admin/admin123
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

## 开发指南

### 快速集成

```java
// 1. 添加依赖
<dependency>
    <groupId>com.haven</groupId>
    <artifactId>admin</artifactId>
    <version>1.0.0</version>
</dependency>

// 2. 配置Admin客户端
@Configuration
public class AdminClientConfig {
    @Bean
    public ApplicationRunner adminRegistrar() {
        return args -> {
            // 自动注册到Admin服务
        };
    }
}
```

### 核心API

```java
// 服务管理API
@Autowired
private ServiceManageService serviceManageService;

// 获取服务列表
List<ServiceInfo> services = serviceManageService.getAllServices();

// 获取服务健康状态
Map<String, Object> health = serviceManageService.getServiceHealth("account-service");

// 告警服务API
@Autowired
private AlertService alertService;

// 创建告警规则
AlertRule rule = new AlertRule();
rule.setName("高CPU告警");
rule.setMetricName("cpu.usage");
rule.setThreshold(0.8);
alertService.createAlertRule(rule);
```

## 更新历史
- v1.0.0 (2025-01-15): 初始版本，基础管理功能