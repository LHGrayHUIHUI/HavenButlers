# HavenButler 智能家庭服务平台

> 构建"核心统一、边缘灵活、安全可控"的智能家庭技术体系

## 🏠 项目简介

HavenButler是一个企业级智能家庭服务平台，采用多语言混合架构，覆盖"设备接入→智能交互→数据管理→安全防护"全链路。

### 核心特性

- 🔧 **多语言架构**：Java核心业务 + Python/Go/C++场景适配
- 🛡️ **五层安全防护**：用户→接入→服务→数据→设备全链路安全
- 🌐 **Matter协议支持**：兼容主流智能设备品牌
- 🎙️ **语音交互**：支持方言识别和语音控制
- 📱 **多端覆盖**：Web、小程序、APP、智能音箱
- 🏡 **边缘计算**：家庭网关支持断网运行

## 🏗️ 架构概览

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  前端交互层  │    │   接入层    │    │ 核心业务层  │
│ Vue3/小程序 │ -> │Java Gateway │ -> │ Java服务群  │
│   智能音箱   │    │ Go边缘网关  │    │Account/AI等 │
└─────────────┘    └─────────────┘    └─────────────┘
                            │
                    ┌─────────────┐    ┌─────────────┐
                    │多语言适配层 │    │ 基础支撑层  │
                    │Python/Go/C++│    │Common/Admin │
                    │IoT/OCR/ASR  │    │   Nacos    │
                    └─────────────┘    └─────────────┘
```

## 🚀 技术栈

### 核心业务层（Java 17）
- **框架**：Spring Cloud 2023.0.1 + Spring Boot 3.1.0
- **已完成服务**：Gateway、Storage、Account、Message、AI、NLP、File-Manager
- **基础设施**：Infrastructure (Base-Model、Common、Admin)
- **数据库**：MySQL、MongoDB、Redis、MinIO
- **消息队列**：RabbitMQ
- **服务注册**：Nacos 2.3.0

### 多语言适配层（规划中）
- **Python**：IoT设备SDK适配、数据预处理
- **Go**：OCR图像识别、高并发处理
- **C++**：ASR语音识别、低延迟引擎

### 前端技术（规划中）
- **Web**：Vue3 + TypeScript
- **移动端**：小程序、原生APP
- **交互**：智能音箱语音控制

### 监控运维
- **服务监控**：Spring Boot Admin 3.1.0
- **指标收集**：Prometheus + Grafana
- **日志管理**：ELK Stack
- **链路追踪**：Jaeger/Zipkin

## 📁 项目结构

```
HavenButler/
├── docs/                    # 架构和需求文档
├── dev-dashboard/          # 开发面板和进度跟踪
├── gateway/                # ✅ API网关服务 (已完成)
├── services/              # ✅ 核心业务服务 (已完成)
│   ├── account-service/   # ✅ 账户管理 - 用户认证、权限控制
│   ├── message-service/   # ✅ 消息通知 - 多渠道消息发送
│   ├── storage-service/   # ✅ PaaS存储平台 - DBaaS+文件存储+向量数据库
│   ├── ai-service/       # ✅ AI模型接入 - 大模型调用
│   ├── nlp-service/      # ✅ 自然语言处理 - 语音指令解析
│   └── file-manager-service/ # ✅ 文件管理 - 文件上传下载
├── infrastructure/        # ✅ 基础设施 (已完成)
│   ├── base-model/       # ✅ 基础模型 - 统一响应、异常处理
│   ├── common/           # ✅ 公共组件 - Redis、MQ、JWT等
│   └── admin/            # ✅ 管理服务 - 监控、配置、日志
├── adapters/             # 🚧 多语言适配层 (规划中)
│   ├── iot-python/       # Python IoT适配
│   ├── ocr-go/          # Go OCR引擎
│   └── asr-cpp/         # C++ ASR引擎
├── edge-gateway/         # 🚧 边缘网关 (规划中)
└── frontend/            # 🚧 前端项目 (规划中)
```

## 📊 开发进度

### ✅ 已完成模块 (2025-01-16)
- **Infrastructure基础设施层**：100% 完成
  - Base-Model：统一响应、异常处理、链路追踪
  - Common：Redis、消息队列、JWT、限流组件
  - Admin：监控面板、配置管理、日志分析
- **Gateway网关层**：100% 完成
  - 路由配置、负载均衡、鉴权过滤
- **Services核心业务层**：100% 完成
  - 6个核心微服务，完整的Infrastructure集成
  - 统一的dev-panel开发面板

### 🚧 进行中模块
- **文档体系**：持续完善技术文档和开发指南

### 📋 计划中模块
- **多语言适配层**：Python IoT、Go OCR、C++ ASR
- **边缘网关**：Go语言本地处理
- **前端应用**：Vue3 Web端、小程序、APP

## 🔧 开发环境

### 必备工具
- **Java 17+**（核心服务）
- **Python 3.9+**（IoT适配）
- **Go 1.21+**（OCR/边缘网关）
- **GCC 11+**（ASR引擎）
- **Node.js 18+**（前端）
- **Docker & Docker Compose**

### BMAD工具链
项目使用BMAD方法论进行开发管理：
```bash
# 启动业务分析代理
/BMad:agents:analyst

# 启动架构设计代理  
/BMad:agents:architect

# 启动开发代理
/BMad:agents:dev
```

## 🛡️ 安全特性

### 五层防护网
1. **用户层**：多因素认证、行为审计
2. **接入层**：WAF防护、流量控制、HTTPS强制
3. **服务层**：三级权限控制、服务间加密
4. **数据层**：AES-256加密、KMS密钥管理
5. **设备层**：双因素认证、固件签名校验

### 通信安全
- **高频同步**：gRPC + TLS加密
- **低频任务**：HTTP + HMAC签名
- **流式传输**：TCP Socket + AES-256
- **TraceID追踪**：`tr-yyyyMMdd-HHmmss-随机6位`

## 📋 快速开始

### 1. 环境准备
```bash
# 克隆项目
git clone https://github.com/your-org/HavenButler.git
cd HavenButler

# 安装Java开发环境
# - Java 17+
# - Maven 3.8+
# - Docker & Docker Compose
```

### 2. 本地开发
```bash
# 1. 构建基础设施层
cd infrastructure
mvn clean package

# 2. 启动依赖服务 (MySQL, Redis, RabbitMQ, Nacos, MinIO)
docker-compose up -d

# 3. 启动核心服务
cd ../gateway && mvn spring-boot:run &
cd ../services/storage-service && mvn spring-boot:run &
cd ../services/account-service && mvn spring-boot:run &
cd ../services/message-service && mvn spring-boot:run &
cd ../services/ai-service && mvn spring-boot:run &
cd ../services/nlp-service && mvn spring-boot:run &
cd ../services/file-manager-service && mvn spring-boot:run &

# 4. 启动管理面板
cd ../infrastructure/admin && mvn spring-boot:run &
```

### 3. 验证部署
```bash
# 检查服务状态
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Storage
curl http://localhost:8082/actuator/health  # Account
curl http://localhost:8083/actuator/health  # Message
curl http://localhost:8084/actuator/health  # AI
curl http://localhost:8085/actuator/health  # NLP
curl http://localhost:8086/actuator/health  # File Manager

# 访问管理面板
http://localhost:8888 (admin/admin123)
```

### 4. 开发面板
```bash
# 查看各服务开发状态
cat services/*/dev-panel.md
cat dev-dashboard/project-overview.md
```

## 🏷️ 枚举设计

### 用户权限枚举系统

#### 1. 系统级枚举 (UserRole)
用户在系统中的权限等级，与家庭无关：

```java
USER (level 1)      // 普通用户 - 基础系统功能
POWER_USER (level 5) // 高级用户 - 增强功能权限
ADMIN (level 9)     // 系统管理员 - 系统管理权限
SUPER_ADMIN (level 10) // 超级管理员 - 最高权限
```

#### 2. 用户状态枚举 (UserStatus)
用户账户的状态管理：

```java
ACTIVE   // 激活状态 - 正常使用
INACTIVE // 未激活状态 - 待验证或暂停
LOCKED   // 锁定状态 - 安全限制
```

#### 3. 家庭角色枚举 (FamilyRole)
用户在特定家庭中的权限层级：

```java
GUEST (level 10)    // 访客 - 临时访问权限
MEMBER (level 50)   // 家庭成员 - 标准家庭权限
ADMIN (level 100)   // 家庭管理员 - 家庭管理权限
OWNER (level 150)   // 家庭所有者 - 最高家庭权限
```

#### 4. 家庭成员状态枚举 (FamilyMemberStatus)
用户在家庭中的参与状态：

```java
ACTIVE   // 活跃状态 - 正常参与家庭活动，可访问家庭数据
AWAY     // 暂离状态 - 暂时离开但保留成员身份，仍可访问数据
DISABLED // 禁用状态 - 被管理员禁用，无法访问家庭数据
LEFT     // 已离开状态 - 主动离开家庭，失去所有权限
```

### 权限设计原则

1. **层级分离**：系统权限与家庭权限完全独立，避免混淆
2. **权限继承**：高级别权限包含低级别权限
3. **状态控制**：用户状态和成员状态独立管理，精细化控制
4. **安全边界**：家庭权限仅在家庭范围内有效，系统权限影响全局

### 使用示例

```java
// 用户注册时设置系统角色和状态
user.setRoles(UserRole.USER.getCode());           // 默认普通用户
user.setUserStatus(UserStatus.ACTIVE);           // 默认激活状态

// 加入家庭时设置家庭角色和成员状态
familyMember.setFamilyRole(FamilyRole.MEMBER);    // 默认家庭成员
familyMember.setMemberStatus(FamilyMemberStatus.ACTIVE); // 默认活跃状态

// 权限检查逻辑
public boolean canAccessFamilyData(Long userId, Long familyId) {
    FamilyMember member = getFamilyMember(userId, familyId);
    // 检查成员状态是否允许访问
    return member != null && member.getMemberStatus().canAccessFamilyData();
}
```

## 🎯 核心业务场景

### 场景1：语音控制智能设备
```
老人说话 -> 智能音箱 -> 边缘网关ASR -> 云端NLP -> 设备控制 -> APP通知
```

### 场景2：图像识别设备录入
```
拍照上传 -> OCR识别 -> 设备信息提取 -> 自动录入 -> Matter协议连接
```

## 🤝 贡献指南

1. **Fork项目** 并创建功能分支
2. **遵循代码规范** 检查`docs/architecture/coding-standards.md`
3. **添加测试** 确保覆盖率>80%
4. **更新文档** 同步修改架构文档
5. **提交PR** 包含清晰的变更描述

## 📖 文档

- [架构设计文档](docs/智能家庭服务平台%20-%20全架构设计文档.md)
- [开发者指南](CLAUDE.md)
- [BMAD工具使用](.bmad-core/user-guide.md)

## 📞 联系我们

- **技术支持**：[LHGray@163.com]
- **产品咨询**：[LHGray@163.com]
- **Bug报告**：[GitHub Issues]

---

⭐ 如果这个项目对您有帮助，请给我们一个star！