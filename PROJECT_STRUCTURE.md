# HavenButler 项目结构说明

## 项目创建完成情况

### ✅ 已完成的内容

#### 1. 项目总体开发面板
- ✅ `/dev-dashboard/project-overview.md` - 项目总体进度跟踪
- ✅ `/dev-dashboard/task-matrix.md` - 任务依赖关系矩阵  
- ✅ `/dev-dashboard/milestones.md` - 里程碑跟踪

#### 2. 基础模块 (base-model)
- ✅ `/infrastructure/base-model/README.md` - 服务说明文档（包含业务流程图）
- ✅ `/infrastructure/base-model/CLAUDE.md` - 开发指导文档
- ✅ `/infrastructure/base-model/dev-panel.md` - 模块开发面板

#### 3. 网关服务 (gateway)
- ✅ `/gateway/README.md` - 服务说明文档（包含业务流程图）
- ✅ `/gateway/CLAUDE.md` - 开发指导文档
- ✅ `/gateway/dev-panel.md` - 服务开发面板

#### 4. 核心业务服务
- ✅ `/services/account-service/README.md` - 账户服务文档（包含业务流程图）
- ✅ `/services/storage-service/README.md` - 存储服务文档（包含业务流程图）
- ✅ `/services/storage-service/CLAUDE.md` - 存储服务开发指导

### 📁 项目目录结构

```
HavenButler/
├── dev-dashboard/                    # 项目总体开发面板
│   ├── project-overview.md          # 项目整体进度跟踪
│   ├── task-matrix.md               # 任务关联矩阵
│   └── milestones.md                # 里程碑跟踪
│
├── dev-logs/                        # 开发日志目录
│   └── 2025-01-15/                 # 按日期组织的日志
│
├── infrastructure/                  # 基础设施层
│   └── base-model/                 # 基础模块（所有Java服务依赖）
│       ├── README.md               # 模块说明（含流程图）
│       ├── CLAUDE.md              # 开发指导
│       └── dev-panel.md           # 开发面板
│
├── gateway/                        # 网关服务（统一入口）
│   ├── README.md                  # 服务说明（含流程图）
│   ├── CLAUDE.md                  # 开发指导
│   └── dev-panel.md               # 开发面板
│
├── services/                       # 核心业务服务
│   ├── account-service/           # 账户服务
│   │   ├── README.md             # 服务说明（含流程图）
│   │   ├── CLAUDE.md             # 开发指导
│   │   └── dev-panel.md          # 开发面板
│   │
│   ├── storage-service/           # 存储服务（数据访问层）
│   │   ├── README.md             # 服务说明（含流程图）
│   │   ├── CLAUDE.md             # 开发指导
│   │   └── dev-panel.md          # 开发面板
│   │
│   ├── message-service/           # 消息服务
│   ├── ai-service/               # AI服务
│   ├── nlp-service/              # NLP服务
│   └── file-manager-service/     # 文件管理服务
│
└── docs/                          # 项目文档
    ├── 智能家庭服务平台 - 全架构设计文档.md
    └── 要求文档.md
```

## 核心特性说明

### 1. 统一数据访问层
- **所有微服务禁止直接访问数据库**
- 必须通过 `storage-service` 进行所有数据操作
- storage-service 提供 MySQL、MongoDB、Redis、MinIO 的统一访问接口

### 2. 基础模块集成 (base-model)
所有Java微服务都必须依赖 base-model，它提供：
- 统一响应体 (ResponseWrapper)
- 全局异常处理
- TraceID生成和管理
- 加密解密工具 (AES-256)
- 通用数据模型 (DeviceDTO、UserDTO、FamilyDTO)

### 3. 网关统一入口
- 所有外部请求必须通过 gateway
- 仅 gateway 对外暴露端口 (9783)
- 负责路由、鉴权、限流、WAF防护

### 4. 三级权限模型
在 account-service 中实现：
- **家庭级**：管理员、成员、访客
- **房间级**：所有者、使用者、无权限
- **设备级**：完全控制、使用、只读、无权限

### 5. 业务流程图
每个服务的 README.md 都包含详细的 Mermaid 流程图：
- 展示服务内部的业务处理流程
- 标明与其他服务的交互关系
- 便于理解服务架构和数据流向

## 开发规范要点

### 1. 文档规范
- 每个服务必须包含：README.md、CLAUDE.md、dev-panel.md
- README.md 必须包含业务流程图
- CLAUDE.md 提供详细的开发指导
- dev-panel.md 跟踪开发进度

### 2. 通信规范
- 内部服务通信使用 gRPC (高频) 或 HTTP/JSON (低频)
- 所有请求必须携带 TraceID
- 敏感数据传输使用 AES-256 加密

### 3. 部署规范
- 所有服务容器化部署 (Docker)
- 使用统一网络 smart-home-network
- 仅 gateway 映射宿主机端口

### 4. 测试要求
- 单元测试覆盖率 ≥80%
- 必须在 Docker 环境中进行集成测试
- 性能测试基线：响应时间 <2秒

## 下一步开发建议

1. **完成其他服务的文档**
   - message-service
   - ai-service
   - nlp-service
   - file-manager-service

2. **开始代码实现**
   - 先实现 base-model 基础组件
   - 然后实现 storage-service（最关键）
   - 接着实现 account-service 和 gateway
   - 最后实现其他业务服务

3. **搭建开发环境**
   - 部署 Nacos 配置中心
   - 搭建 MySQL、MongoDB、Redis、MinIO
   - 配置 Docker 网络环境

4. **建立CI/CD流程**
   - 配置自动化测试
   - 设置代码质量检查
   - 建立自动部署流程