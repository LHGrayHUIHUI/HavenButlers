# HavenButler Admin - 智能家居平台管理中心

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-blue.svg)](https://spring.io/projects/spring-cloud)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

</div>

## 🎯 项目介绍

HavenButler Admin 是一个基于 Spring Boot Admin 构建的微服务管理平台，为 HavenButler 智能家居生态系统提供统一的运维管理能力。它能够实时监控各个微服务的运行状态，提供配置管理、健康检查、性能监控等核心功能。

### 核心价值

- **统一监控**：一站式监控所有微服务的运行状态
- **智能告警**：基于规则的自动告警和通知
- **配置中心**：集成 Nacos 实现动态配置管理
- **环境管理**：支持开发、测试、生产环境的快速切换

## ✨ 功能特性

### 🔍 服务监控
- 实时服务健康状态监控
- JVM 内存、线程、GC 指标
- HTTP 请求统计和响应时间分析
- 服务依赖关系可视化

### ⚡ 告警管理
- 自定义告警规则配置
- 多渠道告警通知（邮件、Webhook、短信）
- 告警历史记录和统计分析
- 智能告警静默和聚合

### 🔧 配置管理
- 集成 Nacos 配置中心
- 配置版本管理和回滚
- 配置热更新无需重启
- 多环境配置隔离

### 📊 性能分析
- Prometheus 指标集成
- 自定义业务指标收集
- 性能趋势分析和预测
- 慢查询和异常追踪

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- Docker & Docker Compose（可选）
- 2GB+ 可用内存

### 一键启动

```bash
# 克隆项目
git clone https://github.com/your-org/havenbutler.git
cd havenbutler/infrastructure/admin

# 方式1：Docker Compose 启动（推荐）
docker-compose up -d

# 方式2：本地开发模式
mvn spring-boot:run

# 初始化配置（首次运行）
./setup-nacos.sh
```

### 访问服务

启动成功后，可通过以下地址访问：

| 服务 | 地址 | 默认账号 | 说明 |
|------|------|---------|------|
| Admin 控制台 | http://localhost:8888 | admin/admin123 | 管理监控面板 |
| Nacos 控制台 | http://localhost:8848/nacos | nacos/nacos | 配置中心 |
| Prometheus | http://localhost:9090 | - | 指标监控 |

## 📡 API 接口

### 服务管理 API

```bash
# 获取所有服务列表
GET /api/service/list

# 获取服务详情
GET /api/service/{serviceName}

# 获取服务指标
GET /api/service/{serviceName}/metrics

# 重启服务实例
POST /api/service/{serviceName}/restart

# 服务健康检查
GET /api/service/{serviceName}/health
```

### 环境管理 API

```bash
# 获取当前环境
GET /api/environment/current

# 获取可用环境列表
GET /api/environment/available

# 切换环境（需要管理员权限）
POST /api/environment/switch
{
  "environment": "test"
}
```

### 告警管理 API

```bash
# 获取告警规则列表
GET /api/alert/rules

# 创建告警规则
POST /api/alert/rules

# 获取告警历史
GET /api/alert/history

# 确认告警
PUT /api/alert/{alertId}/acknowledge
```

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────┐
│                  前端展示层                   │
│         (Spring Boot Admin UI)               │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│              Admin Server                    │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐       │
│  │ 监控模块 │ │ 告警模块 │ │ 配置模块 │       │
│  └─────────┘ └─────────┘ └─────────┘       │
└─────────────────┬───────────────────────────┘
                  │
        ┌─────────┼─────────┬──────────┐
        │         │         │          │
┌───────▼──┐ ┌───▼───┐ ┌──▼───┐ ┌────▼────┐
│  Nacos   │ │Actuator│ │Metrics│ │Discovery│
│ 配置中心  │ │ 端点   │ │ 指标  │ │服务发现  │
└──────────┘ └────────┘ └──────┘ └─────────┘
```

## 📦 技术栈

- **核心框架**：Spring Boot 3.1.0, Spring Cloud 2023.0.1
- **监控组件**：Spring Boot Admin 3.1.0
- **配置中心**：Nacos 2.3.0
- **指标收集**：Micrometer, Prometheus
- **服务发现**：Spring Cloud Discovery
- **安全认证**：Spring Security
- **容器化**：Docker, Docker Compose

## 🔧 配置说明

### 核心配置项

```yaml
# application.yml
server:
  port: 8888  # 服务端口

spring:
  application:
    name: admin-service

  # 安全配置
  security:
    user:
      name: ${ADMIN_USER:admin}
      password: ${ADMIN_PASSWORD:admin123}

  # Nacos 配置
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:public}
      config:
        server-addr: ${NACOS_ADDR:localhost:8848}
        file-extension: yml

# 监控端点配置
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `NACOS_ADDR` | localhost:8848 | Nacos 服务地址 |
| `NACOS_NAMESPACE` | public | Nacos 命名空间 |
| `ADMIN_USER` | admin | 管理员用户名 |
| `ADMIN_PASSWORD` | admin123 | 管理员密码 |
| `ENVIRONMENT` | dev | 运行环境（dev/test/prod） |

## 📈 监控指标

系统会自动收集以下指标：

- **系统指标**：CPU 使用率、内存使用、磁盘 IO
- **JVM 指标**：堆内存、GC 次数、线程数
- **HTTP 指标**：请求数、响应时间、错误率
- **业务指标**：服务调用量、数据处理量、任务执行情况

## 🛡️ 安全特性

- 基于 Spring Security 的身份认证
- 细粒度的角色权限控制
- API 接口签名验证
- 敏感配置加密存储
- 操作审计日志记录

## 📝 开发指南

### 本地开发

```bash
# 安装依赖
mvn clean install

# 运行测试
mvn test

# 构建 JAR
mvn clean package

# 运行服务
java -jar target/admin-service-1.0.0.jar
```

### Docker 构建

```bash
# 构建镜像（使用多阶段构建优化大小）
docker build -f Dockerfile.multi-stage -t haven/admin-service:latest .

# 运行容器
docker run -d \
  --name admin-service \
  -p 8888:8888 \
  -e NACOS_ADDR=nacos:8848 \
  haven/admin-service:latest
```

## 🤝 贡献指南

我们欢迎任何形式的贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详情。

### 提交规范

- feat: 新功能
- fix: 修复问题
- docs: 文档更新
- style: 代码格式调整
- refactor: 代码重构
- test: 测试相关
- chore: 构建或辅助工具变更

## 📄 许可证

本项目采用 Apache License 2.0 许可证。详见 [LICENSE](LICENSE) 文件。

## 🔗 相关链接

- [HavenButler 主项目](https://github.com/your-org/havenbutler)
- [API 文档](http://localhost:8888/swagger-ui.html)
- [问题反馈](https://github.com/your-org/havenbutler/issues)
- [更新日志](CHANGELOG.md)

## 👥 团队

- 架构设计：HavenButler Team
- 主要开发：Admin Service Contributors
- 技术支持：support@havenbutler.com

---

<div align="center">
Made with ❤️ by HavenButler Team
</div>