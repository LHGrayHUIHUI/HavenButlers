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

### 核心业务层（Java）
- **框架**：Spring Cloud/Spring Boot
- **服务**：Account、Message、Storage、AI、NLP
- **数据库**：MySQL、MongoDB、Redis
- **消息队列**：RabbitMQ

### 多语言适配层
- **Python**：IoT设备SDK适配、数据预处理
- **Go**：OCR图像识别、高并发处理
- **C++**：ASR语音识别、低延迟引擎

### 前端技术
- **Web**：Vue3 + TypeScript
- **移动端**：小程序、原生APP
- **交互**：智能音箱语音控制

## 📁 项目结构

```
HavenButler/
├── docs/                    # 架构和需求文档
├── gateway/                 # API网关服务
├── services/               # 核心业务服务
│   ├── account-service/    # 账户管理
│   ├── message-service/    # 消息通知
│   ├── storage-service/    # 数据存储
│   ├── ai-service/        # AI模型接入
│   └── nlp-service/       # 自然语言处理
├── adapters/              # 多语言适配层
│   ├── iot-python/        # Python IoT适配
│   ├── ocr-go/           # Go OCR引擎
│   └── asr-cpp/          # C++ ASR引擎
├── edge-gateway/          # 边缘网关
├── frontend/             # 前端项目
└── infrastructure/       # 基础设施配置
```

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

# 安装依赖（根据具体服务）
make install-deps
```

### 2. 本地开发
```bash
# 启动基础设施
docker-compose up -d mysql redis rabbitmq

# 启动核心服务
make start-services

# 启动前端
make start-frontend
```

### 3. 测试验证
```bash
# 运行单元测试
make test

# 运行集成测试
make test-integration

# 性能测试
make test-performance
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