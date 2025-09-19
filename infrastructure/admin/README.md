# HavenButler Admin 管理服务

## 🎯 服务概述

HavenButler Admin管理服务是智能家庭平台的**运维管理中心**，提供以下核心能力：

- **📊 微服务监控**：Spring Boot Admin + Prometheus集成
- **🔍 服务发现管理**：基于Nacos的服务注册与配置
- **⚡ 健康状态检查**：自动化服务健康监控和告警
- **📋 日志管理**：集中化日志查看和检索
- **⚙️ 配置管理**：动态配置更新和管理

## 🚀 快速启动

### Docker部署（推荐）

```bash
# 进入admin服务目录
cd /Users/yjlh/Documents/code/HavenButler/infrastructure/admin

# 启动服务（包含Nacos、Prometheus、Admin）
docker-compose up -d

# 验证服务
curl http://localhost:8888/actuator/health
```

### 访问地址

| 服务 | 地址 | 账号密码 | 功能 |
|-----|------|---------|------|
| Admin管理面板 | http://localhost:8888 | admin/havenbutler2025 | 微服务监控管理 |
| Nacos控制台 | http://localhost:8848/nacos | nacos/nacos | 服务发现配置中心 |
| Prometheus | http://localhost:9090 | 无需认证 | 指标数据查询 |

## 🔌 其他服务接入指南

### 1. 服务注册到Admin监控

在其他微服务的 `application.yml` 中添加：

```yaml
spring:
  application:
    name: your-service-name
  boot:
    admin:
      client:
        url: http://admin-service:8888  # Docker网络内地址
        instance:
          service-base-url: http://your-service:8080
        # 认证信息（Docker环境自动配置）
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848
        namespace: havenbutler
        group: DEFAULT_GROUP

# 开启健康检查端点
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: always
```

### 2. API调用方式

#### 获取所有服务状态
```bash
# 获取Nacos中注册的所有服务
curl -X GET "http://localhost:8888/api/service/nacos/services" \
  -H "Authorization: Basic YWRtaW46aGF2ZW5idXRsZXIyMDI1"

# 获取系统整体健康状态
curl -X GET "http://localhost:8888/api/service/nacos/system/health" \
  -H "Authorization: Basic YWRtaW46aGF2ZW5idXRsZXIyMDI1"
```

#### 服务实例管理
```bash
# 临时下线服务实例（维护模式）
curl -X POST "http://localhost:8888/api/service/nacos/account-service/deregister?ip=192.168.1.100&port=8080"

# 重新上线服务实例
curl -X POST "http://localhost:8888/api/service/nacos/account-service/register?ip=192.168.1.100&port=8080"
```

### 3. 服务健康检查规范

其他服务需要实现的健康检查端点：

```java
// 必需的健康检查端点
@RestController
public class HealthController {

    @GetMapping("/actuator/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "service", "your-service-name",
            "version", "1.0.0"
        );
    }
}
```

## ⚙️ 配置说明

### 核心配置文件

#### application-docker.yml 主要配置
```yaml
# 服务端口
server:
  port: 8888

# Spring Boot Admin配置
spring:
  boot:
    admin:
      ui:
        title: "HavenButler管理中心"
        brand: "HavenButler管理控制台"
      discovery:
        enabled: true  # 启用Nacos服务发现

# 安全认证配置
spring:
  security:
    user:
      name: ${SPRING_SECURITY_USER_NAME:admin}
      password: ${SPRING_SECURITY_USER_PASSWORD:havenbutler2025}
      roles: ${SPRING_SECURITY_USER_ROLES:ADMIN}

# Nacos配置中心
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:nacos:8848}
        namespace: ${NACOS_NAMESPACE:havenbutler}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
```

#### docker-compose.yml 部署配置
```yaml
services:
  nacos:
    image: nacos/nacos-server:v2.3.0-slim
    environment:
      - MODE=standalone
      - SPRING_DATASOURCE_PLATFORM=derby
      # 关键：显式禁用MySQL避免启动失败
      - MYSQL_SERVICE_HOST=
      - MYSQL_SERVICE_DB_NAME=
    ports:
      - "8848:8848"
    networks:
      - smart-home-network

  admin-service:
    image: smart-home/admin-service:v1.0.0
    environment:
      - NACOS_ADDR=nacos:8848
      - PROMETHEUS_URL=http://prometheus:9090
    ports:
      - "8888:8888"
    depends_on:
      - nacos
    networks:
      - smart-home-network
```

### 环境变量配置

| 变量名 | 默认值 | 说明 |
|-------|--------|------|
| NACOS_ADDR | nacos:8848 | Nacos服务地址 |
| NACOS_NAMESPACE | havenbutler | Nacos命名空间 |
| SPRING_SECURITY_USER_NAME | admin | 管理员用户名 |
| SPRING_SECURITY_USER_PASSWORD | havenbutler2025 | 管理员密码 |
| PROMETHEUS_URL | http://prometheus:9090 | Prometheus地址 |

## 📊 监控能力

### 自动监控指标

- **JVM指标**：内存使用、GC情况、线程状态
- **应用指标**：HTTP请求量、响应时间、错误率
- **系统指标**：CPU使用率、磁盘空间、网络IO
- **业务指标**：自定义业务监控点

### 告警规则

Admin服务内置告警规则：
- 服务下线告警
- 响应时间超过2秒告警
- 错误率超过5%告警
- JVM内存使用超过85%告警

## 🔧 故障排查

### 常见问题

1. **Nacos启动失败**
   ```
   错误：java.net.UnknownHostException: ${MYSQL_SERVICE_HOST}
   解决：检查docker-compose.yml中MySQL环境变量设置
   ```

2. **Bean冲突错误**
   ```
   错误：The bean 'nacosServiceManager' could not be registered
   解决：已通过allow-bean-definition-overriding=true配置解决
   ```

3. **服务无法访问**
   ```bash
   # 检查容器状态
   docker-compose ps

   # 查看服务日志
   docker-compose logs admin-service
   docker-compose logs nacos
   ```

### 日志查看

```bash
# 实时查看Admin服务日志
docker-compose logs -f admin-service

# 查看Nacos日志
docker-compose logs -f nacos

# 查看所有服务日志
docker-compose logs -f
```

## 🔄 版本更新

### v1.0.1 (2024-09-19)
- 修复Nacos MySQL配置问题，改用Derby内嵌数据库
- 解决Spring Bean冲突，添加AdminNacosServiceManager
- 更新密码配置为havenbutler2025
- 完善Docker网络配置

### v1.0.0 (2024-09-18)
- 初始版本发布
- 集成Spring Boot Admin + Nacos + Prometheus
- 提供基础服务监控和管理功能

## 🏗️ 技术架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Admin Web UI  │    │  Nacos Console  │    │  Prometheus UI  │
│   (Port 8888)   │    │   (Port 8848)   │    │   (Port 9090)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌─────────────────────────────────────────────────┐
         │            Admin Service                        │
         │  ┌─────────────────┐  ┌─────────────────────┐   │
         │  │ Service Monitor │  │ Nacos Integration   │   │
         │  └─────────────────┘  └─────────────────────┘   │
         │  ┌─────────────────┐  ┌─────────────────────┐   │
         │  │ Health Check    │  │ Metrics Collection  │   │
         │  └─────────────────┘  └─────────────────────┘   │
         └─────────────────────────────────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
    ┌─────────┐          ┌─────────────┐        ┌─────────────┐
    │Service A│          │  Service B  │        │  Service C  │
    │(8081)   │          │   (8082)    │        │   (8083)    │
    └─────────┘          └─────────────┘        └─────────────┘
```

通过这个重新整理的README，其他服务开发者可以：
1. 快速了解Admin服务的核心功能
2. 按照接入指南快速集成监控
3. 根据配置说明正确设置环境
4. 利用故障排查部分解决常见问题