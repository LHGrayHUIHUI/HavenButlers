# HavenButler Admin - 智能家居平台管理中心

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
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

### 一键启动（Docker 内网推荐）

```bash
# 克隆项目
git clone https://github.com/your-org/havenbutler.git
cd havenbutler/infrastructure/admin

# 预创建内部网络（docker-compose.yml 依赖该外部网络）
docker network create haven-network || true

# 方式1：Docker Compose 启动（推荐）
docker-compose up -d

# 方式2：本地开发模式
mvn spring-boot:run

# 初始化配置（首次运行，按需）
# 参考：nacos-configs/README.md 与 docs/Nacos-统一配置与服务发现设计.md
# 如需将本地配置同步到 Nacos，可使用 nacos-configs/sync-config.sh
# 示例：./nacos-configs/sync-config.sh -n havenbutler-dev -a http://localhost:8848
```

### 访问服务

启动成功后，可通过以下地址访问：

| 服务 | 地址 | 默认账号 | 说明 |
|------|------|---------|------|
| Admin 控制台 | http://localhost:8888 | admin/admin123 | 管理监控面板 |
| Nacos 控制台 | http://localhost:8848/nacos | nacos/nacos | 配置中心 |
| Prometheus | http://localhost:9090 | - | 指标监控 |

## 📡 API 接口（与代码对齐）

### 服务管理 API

```bash
# 获取所有服务列表
GET /api/service/list

# 获取服务详情
GET /api/service/{serviceName}

# 获取服务指标
GET /api/service/{serviceName}/metrics

# 服务控制
POST /api/service/{serviceName}/restart
POST /api/service/{serviceName}/stop
POST /api/service/{serviceName}/start

# 服务健康检查
GET /api/service/{serviceName}/health
POST /api/service/health-check

# 服务日志
GET /api/service/{serviceName}/logs

# 服务依赖关系
GET /api/service/dependencies

# Nacos 辅助接口
GET /api/service/nacos/services
GET /api/service/nacos/{serviceName}/instances
GET /api/service/nacos/{serviceName}/details
GET /api/service/nacos/{serviceName}/health
GET /api/service/nacos/system/health
POST /api/service/nacos/{serviceName}/deregister
POST /api/service/nacos/{serviceName}/register
```

### 环境管理 API（路径参数 + 刷新）

```bash
# 获取当前环境
GET /api/environment/current

# 获取可用环境列表
GET /api/environment/available

# 获取当前配置信息
GET /api/environment/config

# 切换环境（通过路径参数，需要管理员权限）
POST /api/environment/switch/{environment}

# 刷新环境配置（从Nacos重新加载）
POST /api/environment/refresh
```

### 告警管理 API

```bash
# 告警列表与详情（支持过滤：serviceName, level, status, startTime, endTime）
GET /api/alert/list
GET /api/alert/{alertId}

# 告警处理
POST /api/alert/{alertId}/handle?handler=xxx&remark=xxx
POST /api/alert/{alertId}/ignore?reason=xxx

# 批量处理告警
POST /api/alert/batch/handle

# 告警统计（可指定时间范围）
GET /api/alert/statistics

# 告警规则管理
GET /api/alert/rules?serviceName=xxx&enabled=true
POST /api/alert/rule
PUT /api/alert/rule/{ruleId}
DELETE /api/alert/rule/{ruleId}
PUT /api/alert/rule/{ruleId}/enable?enabled=true

# 告警规则测试
POST /api/alert/rule/test
```

### 健康监控与实时数据 API（支持 SSE）

```bash
# 服务总览（支持状态过滤和名称搜索）
GET /api/service/overview?status=UP&search=account
GET /api/service/overview/{serviceName}

# 实时健康状态流 (SSE) - 每5秒推送更新
GET /api/service/stream/health

# SSE 连接统计
GET /api/service/stream/stats
```

> 📖 健康监控 UI 与实施说明：见 README-HEALTH-UI.md:1
> 📖 SSE 健康监控流可实现实时状态更新，避免频繁轮询，连接超时设置为 5 分钟

### 管理信息 API

```bash
# 系统健康状态
GET /api/admin/health

# 系统指标
GET /api/admin/metrics

# 服务状态
GET /api/admin/services
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

## 📦 技术栈（版本与设计）

- **核心框架**：Spring Boot 3.2.0, Spring Cloud 2023.0.1
- **监控组件**：Spring Boot Admin 3.1.0
- **配置中心**：Nacos 2.3.0
- **指标收集**：Micrometer, Prometheus
- **服务发现**：Spring Cloud Discovery
- **安全认证**：Spring Security
- **容器化**：Docker, Docker Compose

## 🔧 配置说明（简要）

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
        # 生产环境建议: health,info,metrics,prometheus
        # 开发环境可以: health,info,metrics,prometheus,env,configprops
        include: health,info,metrics,prometheus
```

### 环境变量（常用）

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

## 📝 开发指南（本地与容器）

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

### Docker 构建（可选）

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

## ⚠️ YAML 配置规范与常见坑（强制）

### 重要配置原则

1. **避免重复的根级配置键**
   - ❌ 错误示例：文件中出现多个 `spring:` 根节点
   ```yaml
   spring:
     application:
       name: admin-service

   # ... 其他配置 ...

   spring:  # ❌ 这会覆盖上面的 spring 配置！
     cloud:
       nacos: ...
   ```

   - ✅ 正确示例：所有 spring 配置合并到同一个节点
   ```yaml
   spring:
     application:
       name: admin-service
     cloud:
       nacos: ...
   ```

2. **环境变量优先级**
   - 生产环境敏感配置必须通过环境变量注入
   - JWT密钥、数据库密码等不得硬编码

3. **配置键规范迁移**
   - 统一使用 `base-model.*` 配置键
   - 旧的 `common.*` 配置键已废弃

### 依赖版本矩阵（与 pom.xml 一致）

| 组件 | 当前版本 | 兼容性 |
|------|----------|--------|
| Spring Boot | 3.2.0 | ✅ |
| Spring Cloud | 2023.0.1 | ✅ |
| Spring Boot Admin | 3.1.0 | ✅ |
| Spring Cloud Alibaba | 2023.0.1.0 | ✅ |
| base-model | 1.0.0 | ✅ |
| common | 1.0.0 | ✅ |

### 升级路线

- **稳定路线**: Boot 3.1.x + Cloud 2022.0.x
- **当前路线**: Boot 3.2.x + Cloud 2023.0.x ⬅️ 我们在这里
- **未来路线**: Boot 3.3.x + Cloud 2023.0.x

## 🤝 贡献指南（简要）

我们欢迎任何形式的贡献！如提交变更，请在 PR 中说明：影响范围、测试步骤、配置变更（含环境变量）与安全影响面。

### 提交规范（建议）

- feat: 新功能
- fix: 修复问题
- docs: 文档更新
- style: 代码格式调整
- refactor: 代码重构
- test: 测试相关
- chore: 构建或辅助工具变更

## 🔗 相关链接

- [HavenButler 主项目](https://github.com/your-org/havenbutler)
- API 文档：暂未集成 OpenAPI（后续版本将添加）
- Nacos 架构与规范：docs/Nacos-统一配置与服务发现设计.md:1
- Storage TCP 代理设计：docs/Storage-TCP代理模式设计.md:1
- StorageClient 统一规范：docs/StorageClient-统一规范(TCP代理模式).md:1
- 管理端（Vue3）开发与部署：docs/管理端-Vue3-开发与部署.md:1
- 管理端 API 规范：docs/管理端-API规范.md:1
- Nacos 配置脚本与说明：nacos-configs/README.md:1

## 👥 团队

- 架构设计：HavenButler Team
- 主要开发：Admin Service Contributors
- 技术支持：support@havenbutler.com

---

<div align="center">
Made with ❤️ by HavenButler Team
</div>
