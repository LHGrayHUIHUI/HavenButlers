# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是HavenButler智能家庭服务平台项目，采用多语言混合架构：

### 🎯 当前开发状态 (2025-01-16)
- **✅ 已完成**：Infrastructure基础设施层 + Gateway网关 + 6个核心微服务
- **🚧 进行中**：文档体系完善、开发面板优化
- **📋 规划中**：多语言适配层、边缘网关、前端应用

### 🏗️ 技术架构详情
- **✅ 核心业务层**：Java 17 + Spring Cloud 2023.0.1（Account、Message、Storage、AI、NLP、File-Manager）
- **✅ 基础设施层**：Infrastructure (Base-Model、Common、Admin)
- **📋 多语言适配层**：Python（IoT设备SDK）、Go（OCR引擎）、C++（ASR语音）
- **📋 前端层**：Vue3 Web端、小程序/APP、智能音箱
- **📋 边缘计算层**：Go + C++家庭边缘网关

## 核心架构层级

### ✅ 已实现层级
1. **基础支撑层**：Infrastructure (Base-Model、Common、Admin)、Nacos配置中心
2. **接入层**：Java Gateway (路由、鉴权、限流)
3. **核心业务层**：6个Java微服务 (Account、Message、Storage、AI、NLP、File-Manager)

### 📋 规划中层级
4. **多语言适配层**：Python IoT、Go OCR、C++ ASR
5. **前端层**：Vue3、小程序/APP、智能音箱交互
6. **边缘计算层**：Go边缘网关、本地处理
7. **外部生态层**：Matter设备、大模型、通知渠道

## BMAD工具使用

项目使用BMAD（Business Methodology and Development）工具链：

### 核心配置
- 配置文件：`.bmad-core/core-config.yaml`
- 文档模式：PRD分片存储在`docs/prd/`，架构文档在`docs/architecture/`
- 代码标准：参考`docs/architecture/coding-standards.md`
- 技术栈：参考`docs/architecture/tech-stack.md`

### 可用代理
使用`/BMad:agents:{agent_name}`调用专业代理：
- `analyst`：业务分析、市场研究、竞争分析
- `architect`：架构设计、技术决策
- `dev`：开发实现、代码编写
- `qa`：测试策略、质量保证
- `pm`：产品管理、需求规划

## 开发规范

### 多语言通信协议
- **高频同步**：gRPC（超时2秒，TLS加密）
- **低频任务**：HTTP/JSON（超时5秒，HMAC签名）
- **流式传输**：TCP Socket（AES-256加密）
- **异步解耦**：RabbitMQ/Kafka

### 数据格式
- 结构化数据：Protobuf全局模型
- 错误格式：`{"code":xxx, "message":"", "traceId":""}`
- TraceID格式：`tr-yyyyMMdd-HHmmss-随机6位`

### 安全要求
- 五层防护：用户→接入→服务→数据→设备
- 加密标准：AES-256、TLS 1.3、HMAC签名
- 权限控制：家庭-房间-设备三级权限

## 常用命令

### 已实现的Java服务

#### 构建和启动
```bash
# 1. 构建基础设施层
cd infrastructure
mvn clean package

# 2. 启动依赖服务
docker-compose up -d

# 3. 启动各微服务
cd gateway && mvn spring-boot:run &                    # 端口8080
cd services/storage-service && mvn spring-boot:run &   # 端口8081
cd services/account-service && mvn spring-boot:run &   # 端口8082
cd services/message-service && mvn spring-boot:run &   # 端口8083
cd services/ai-service && mvn spring-boot:run &        # 端口8084
cd services/nlp-service && mvn spring-boot:run &       # 端口8085
cd services/file-manager-service && mvn spring-boot:run & # 端口8086

# 4. 启动管理面板
cd infrastructure/admin && mvn spring-boot:run &       # 端口8888
```

#### 测试验证
```bash
# 健康检查
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Storage
curl http://localhost:8082/actuator/health  # Account
curl http://localhost:8083/actuator/health  # Message
curl http://localhost:8084/actuator/health  # AI
curl http://localhost:8085/actuator/health  # NLP
curl http://localhost:8086/actuator/health  # File-Manager

# 管理面板
http://localhost:8888 (admin/admin123)
```

#### 开发面板查看
```bash
# 查看各服务开发状态
cat services/account-service/dev-panel.md
cat services/ai-service/dev-panel.md
cat services/nlp-service/dev-panel.md
cat services/message-service/dev-panel.md
cat services/storage-service/dev-panel.md
cat services/file-manager-service/dev-panel.md

# 查看项目总览
cat dev-dashboard/project-overview.md
```

### Python适配层
```bash
# 安装依赖
pip install -r requirements.txt
# 运行
python main.py
# 测试
pytest
```

### Go服务
```bash
# 构建
go build -o bin/service-name
# 运行
./bin/service-name
# 测试
go test ./...
```

### 前端Vue3
```bash
# 安装依赖
npm install
# 开发服务器
npm run dev
# 构建
npm run build
# 测试
npm run test
```

## 项目结构规划

基于架构文档，预期目录结构：
```
HavenButler/
├── docs/                           # 文档目录
│   ├── architecture/               # 架构文档
│   ├── prd/                       # 产品需求文档
│   └── stories/                   # 用户故事
├── gateway/                       # Java Gateway服务
├── services/                      # Java核心服务
│   ├── account-service/           # 账户服务
│   ├── message-service/           # 消息服务
│   ├── storage-service/           # 存储服务
│   ├── ai-service/               # AI服务
│   └── nlp-service/              # NLP服务
├── adapters/                     # 多语言适配层
│   ├── iot-python/               # Python IoT适配
│   ├── ocr-go/                   # Go OCR引擎
│   └── asr-cpp/                  # C++ ASR引擎
├── edge-gateway/                 # Go边缘网关
├── frontend/                     # 前端项目
│   ├── web-vue3/                 # Vue3 Web端
│   ├── miniprogram/              # 小程序
│   └── mobile-app/               # 移动APP
└── infrastructure/               # 基础设施
    ├── common/                   # 公共组件
    ├── admin/                    # 管理服务
    └── config/                   # 配置管理
```

## 开发注意事项

1. **多语言协作**：严格遵循通信协议，必须携带TraceID
2. **安全优先**：所有服务间通信需加密，敏感数据使用AES-256
3. **文档优先**：代码变更前先更新架构文档
4. **测试覆盖**：核心业务逻辑测试覆盖率>80%
5. **性能要求**：设备控制响应<2秒，语音转文字<1秒

## 🔄 开发工作流程规范

### 强制性开发流程

所有开发工作必须严格遵循以下流程，不得跳过任何环节：

#### 1. 需求分析阶段
```bash
# 使用BMAD分析代理
/BMad:agents:analyst
```
- 必须先使用业务分析代理进行需求分析
- 输出需求分析报告到 `docs/requirements/`
- 评估技术可行性和业务价值
- 识别潜在风险和依赖关系

#### 2. 架构设计阶段
```bash
# 使用BMAD架构代理
/BMad:agents:architect
```
- 基于需求设计技术方案
- 更新架构文档到 `docs/architecture/`
- 评估多语言服务间通信方案
- 制定安全策略和性能指标

#### 3. 开发实现阶段
```bash
# 使用BMAD开发代理
/BMad:agents:dev
```

**3.1 分支管理**
```bash
# 从main分支创建功能分支
git checkout main
git pull origin main
git checkout -b feature/服务名-功能描述-YYYYMMDD

# 分支命名规范
feature/account-rbac-20240601     # 新功能
bugfix/nlp-timeout-20240601       # Bug修复
hotfix/security-patch-20240601    # 紧急修复
refactor/storage-optimization-20240601  # 重构
```

**3.2 开发前置检查**
- [ ] 确认架构设计已完成并评审通过
- [ ] 创建对应的微服务目录结构
- [ ] 编写服务README.md文档
- [ ] 配置开发环境和依赖

**3.3 编码规范**
- 严格遵循各语言编码标准（详见各服务coding-standards.md）
- 所有函数和类必须添加中文注释（注释密度≥30%）
- 敏感信息禁止硬编码，必须使用配置管理
- API接口必须包含完整的参数校验和错误处理

#### 4. 测试验证阶段
```bash
# 使用BMAD质量保证代理
/BMad:agents:qa
```

**4.1 单元测试要求**
- 核心业务逻辑测试覆盖率 ≥ 80%
- 边界条件和异常场景必须覆盖
- Mock外部依赖服务
- 所有测试必须可重复运行

**4.2 集成测试要求**
- 多语言服务间通信测试
- 数据一致性验证
- 性能基线测试（响应时间<2秒）
- 安全漏洞扫描

**4.3 测试报告**
- 生成测试报告到 `docs/test-reports/`
- 记录性能指标和安全扫描结果
- 标注已知问题和风险点

#### 5. 代码审查阶段
- 至少2人Review代码
- 架构师必须审查架构相关变更
- 安全工程师审查安全相关代码
- 通过所有CI/CD检查

#### 6. 部署发布阶段
- 预发布环境验证
- 生产环境灰度发布
- 监控关键指标
- 回滚预案准备

### 开发面板与修改记录管理

#### 面板结构要求
```
HavenButler/
├── dev-dashboard/              # 总开发面板
│   ├── project-overview.md     # 项目整体进度
│   ├── task-matrix.md         # 任务关联矩阵
│   └── milestones.md          # 里程碑跟踪
├── dev-logs/                  # 修改记录根目录
│   ├── 2024-06-01/           # 按日期分组
│   │   ├── account-service/   # 按服务分类
│   │   ├── nlp-service/      
│   │   └── gateway-service/
│   └── 2024-06-02/
└── services/                  # 各微服务目录
    ├── account-service/
    │   ├── dev-panel.md      # 服务独立面板
    │   └── README.md         # 服务说明文档
    └── ...
```

#### 总面板管理规范
**dev-dashboard/project-overview.md** 必须包含：
```markdown
| 任务ID | 所属服务 | 负责人 | 状态 | 计划完成时间 | 实际进度 | 阻塞点 |
|--------|----------|--------|------|-------------|----------|--------|
| T001   | account  | 张三   | 进行中| 2024-06-15  | 60%      | 无     |
| T002   | nlp      | 李四   | 阻塞  | 2024-06-10  | 30%      | 依赖T001|
```

#### 修改记录规范
每日修改记录 `dev-logs/YYYY-MM-DD/service-name/changes.md` 格式：
```markdown
## 2024-06-01 account-service 修改记录
- **修改人**：张三
- **关联任务**：T001
- **修改内容**：实现用户权限RBAC模型
- **文件变更**：src/main/java/com/haven/account/rbac/
- **测试结果**：单元测试通过，集成测试待执行
- **影响范围**：无跨服务影响
```

#### 跨服务变更管理
- 涉及通信协议调整时，必须在总面板标注影响范围
- 同步通知所有关联服务开发人员
- 重大更新需同步更新根目录README.md的更新日志章节

#### Dashboard必备功能
1. **服务健康监控**
   - 各微服务运行状态
   - 响应时间和吞吐量
   - 错误率统计

2. **多语言服务监控**
   - Java服务：JVM内存、GC、线程池
   - Python服务：进程状态、内存使用
   - Go服务：Goroutine数量、内存分配
   - C++服务：CPU使用率、内存泄漏检测

3. **业务指标监控**
   - 设备在线率
   - 用户活跃度
   - 语音识别成功率
   - OCR识别准确率

4. **安全监控**
   - 登录异常检测
   - API调用频率监控
   - 数据访问审计

#### 技术实现
```bash
# 监控技术栈
Prometheus + Grafana + AlertManager
ELK Stack (Elasticsearch + Logstash + Kibana)
Jaeger (分布式链路追踪)
```

## 📋 微服务开发规范

### 目录结构标准

每个微服务必须按以下结构组织：

```
service-name/
├── README.md                    # 服务说明文档（必需）
├── CHANGELOG.md                 # 变更日志
├── src/                        # 源代码
│   ├── main/                   # 主要代码
│   │   ├── java/com/haven/     # Java源码
│   │   └── resources/          # 配置文件
│   └── test/                   # 测试代码
├── docs/                       # 服务文档
│   ├── api.md                  # API文档
│   ├── deployment.md           # 部署文档
│   └── troubleshooting.md      # 故障排查
├── scripts/                    # 脚本文件
│   ├── build.sh               # 构建脚本
│   ├── deploy.sh              # 部署脚本
│   └── test.sh                # 测试脚本
├── configs/                    # 配置文件
│   ├── dev/                   # 开发环境
│   ├── test/                  # 测试环境
│   └── prod/                  # 生产环境
├── docker/                     # Docker相关
│   ├── Dockerfile
│   └── docker-compose.yml
└── tests/                      # 集成测试
    ├── integration/
    └── performance/
```

### 微服务README.md强制模板

每个微服务目录必须包含 `dev-panel.md` 和 `README.md`：

#### dev-panel.md 格式
```markdown
# [服务名] 开发面板

## 当前任务进度
| 任务ID | 任务描述 | 开发状态 | 完成度 | 预计完成 | 阻塞点 |
|--------|----------|----------|--------|----------|--------|
| T001   | RBAC权限模型 | 进行中 | 70% | 06-15 | 无 |

## 开发日志快速链接
- [2024-06-01 修改记录](../../dev-logs/2024-06-01/account-service/)
- [2024-05-31 修改记录](../../dev-logs/2024-05-31/account-service/)

## 当前阻塞问题
- 无

## 下一步计划
- 完成权限验证逻辑
- 编写集成测试用例
```

#### README.md 强制模板
```markdown
# [服务名称]

## 服务定位
- **架构层级**：核心业务层/多语言适配层/接入层/基础支撑层
- **核心职责**：[对应架构文档中的定义]
- **业务范围**：[具体负责的业务功能]

## 技术栈
- **主开发语言**：Java 17
- **核心框架**：Spring Cloud 2023.0.1, Spring Boot 3.1.0
- **通信协议**：gRPC (内部), HTTP/JSON (外部)
- **数据存储**：通过 storage-service 统一访问

## 部署信息
- **Docker镜像**：`smart-home/account-service:v1.0.0`
- **内部端口**：8080 (Docker网络内)
- **健康检查**：`/actuator/health`
- **环境变量**：
  ```
  NACOS_ADDR=nacos:8848
  STORAGE_SERVICE_URL=http://storage-service:8080
  GATEWAY_SERVICE_URL=http://gateway:8080
  ```

## 接口信息
### 对外暴露API
- **路由方式**：通过网关路由 `/api/v1/account/*`
- **接口文档**：`http://gateway-ip:8080/swagger/account`
- **主要接口**：
  - `POST /api/v1/account/login` - 用户登录
  - `GET /api/v1/account/profile` - 获取用户信息

### 内部服务调用
- **通信协议**：gRPC + TLS
- **Proto定义**：`src/main/proto/account.proto`
- **调用方式**：`AccountServiceGrpc.newBlockingStub(channel)`

## 依赖关系
- **直接依赖**：
  - `storage-service:v1.0.0` (必需) - 数据存储
  - `nacos:2.3.0` (必需) - 配置中心
- **被依赖方**：
  - `nlp-service` - 权限验证
  - `gateway-service` - 用户认证

## 独立测试部署
```bash
# 启动最小依赖环境
docker-compose -f docker/test-compose.yml up -d

# 构建和启动服务
./scripts/build.sh
./scripts/test-deploy.sh

# 验证服务状态
curl http://localhost:8080/actuator/health
```

## Docker网络配置
- **网络名称**：smart-home-network
- **容器名称**：account-service
- **端口映射**：仅测试环境临时开放 localhost:8081->8080
- **内部通信**：其他服务通过 `account-service:8080` 访问

## 数据访问规范
⚠️ **严禁直接连接数据库** - 所有数据操作必须通过 `storage-service` 接口

支持的数据操作：
- 用户信息 CRUD：`POST /storage/api/v1/user`
- 权限数据查询：`GET /storage/api/v1/permissions`
- 会话管理：`POST /storage/api/v1/sessions`

## 测试要求
### 单元测试
```bash
mvn test
# 覆盖率要求：≥80%
```

### Docker集成测试
```bash
# 必须在Docker环境中测试
docker-compose -f docker/integration-test.yml up --abort-on-container-exit
```

### 测试环境配置
- 临时端口映射需在此README注明限制条件
- 测试数据通过 `tests/data/` 目录管理
- 集成测试结果输出到 `tests/reports/`

## 监控和日志
- **关键指标**：登录成功率、权限验证延迟、用户会话数
- **日志级别**：INFO (业务操作), ERROR (系统异常)
- **TraceID**：所有请求必须携带并传递

## 故障排查
1. **服务启动失败**：检查 Nacos 连接和环境变量
2. **权限验证异常**：查看 storage-service 连接状态
3. **性能问题**：监控 JVM 内存和 GC 情况

## 更新历史
- v1.0.0 (2024-06-01): 初始版本，基础认证功能
```

### 特殊服务要求

#### storage-service 额外文档要求
```markdown
## 数据存储支持
- **MySQL**：用户数据、权限信息
- **MongoDB**：设备状态、日志数据  
- **Redis**：会话缓存、临时数据
- **MinIO**：文件存储、图片数据

## 访问接口规范
- **认证方式**：JWT Token + HMAC签名
- **权限校验**：基于家庭ID的数据隔离
- **加密策略**：敏感数据AES-256加密，密钥通过KMS管理

## API接口列表
- `POST /storage/api/v1/mysql` - MySQL数据操作
- `POST /storage/api/v1/mongo` - MongoDB文档操作
- `GET /storage/api/v1/redis/{key}` - Redis缓存操作
- `POST /storage/api/v1/files` - 文件上传下载
```

#### gateway-service 额外文档要求
```markdown
## 路由配置汇总
| 服务名称 | 路由前缀 | 目标地址 | 鉴权要求 |
|----------|----------|----------|----------|
| account  | /api/v1/account/* | account-service:8080 | JWT必需 |
| nlp      | /api/v1/nlp/*     | nlp-service:8080     | JWT必需 |

## 鉴权规则
- 公开接口：`/api/v1/account/login`, `/api/v1/health`
- 需要登录：所有其他接口
- 管理员权限：`/api/v1/admin/*`
```

## 📊 测试和日志要求

### 测试分层策略

#### 1. 单元测试（Unit Test）
```bash
# Java服务
mvn test
# 覆盖率要求：≥80%
# 位置：src/test/java/

# Python服务  
pytest --cov=src --cov-report=html
# 覆盖率要求：≥75%
# 位置：tests/unit/

# Go服务
go test -coverprofile=coverage.out ./...
# 覆盖率要求：≥75%
# 位置：*_test.go
```

#### 2. 集成测试（Integration Test）
```bash
# 服务间通信测试
# 数据库集成测试
# 外部API集成测试
# 位置：tests/integration/
```

#### 3. 端到端测试（E2E Test）
```bash
# 完整业务流程测试
# 用户场景验证
# 位置：tests/e2e/
```

#### 4. 性能测试（Performance Test）
```bash
# 负载测试
# 压力测试
# 并发测试
# 位置：tests/performance/
```

### Docker测试环境要求

⚠️ **强制要求**：除基础单元测试外，所有微服务测试必须在Docker环境中进行

#### Docker测试配置
每个服务必须提供：
```
service-name/
├── docker/
│   ├── test-compose.yml        # 独立测试环境
│   ├── integration-test.yml    # 集成测试环境
│   └── Dockerfile.test         # 测试专用镜像
```

#### 网络隔离测试
- 所有服务加入 `smart-home-test-network`
- 仅gateway-service映射宿主机端口进行测试
- 其他服务通过容器名进行内部通信测试
- 测试端口映射需在服务README.md中明确标注限制条件

#### 集成测试流程
```bash
# 1. 启动测试环境
docker-compose -f docker/integration-test.yml up -d

# 2. 等待所有服务就绪
./scripts/wait-for-services.sh

# 3. 执行集成测试
docker-compose exec service-name ./scripts/run-integration-tests.sh

# 4. 收集测试报告
docker cp service-name:/app/tests/reports ./test-results/

# 5. 清理测试环境
docker-compose -f docker/integration-test.yml down -v
```

### 统一日志规范

#### 日志格式标准
```json
{
  "timestamp": "2024-06-01T10:00:00.000Z",
  "level": "INFO",
  "service": "account-service",
  "traceId": "tr-20240601-100000-123456",
  "userId": "user123",
  "operation": "user.login",
  "message": "用户登录成功",
  "duration": 150,
  "metadata": {
    "ip": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "deviceId": "device456"
  }
}
```

#### 日志级别使用
- **ERROR**：系统错误、异常情况
- **WARN**：潜在问题、性能警告
- **INFO**：关键业务操作、状态变更
- **DEBUG**：调试信息（仅开发环境）

#### 敏感信息保护
- 禁止记录密码、令牌等敏感信息
- 手机号、身份证等脱敏处理
- 设备ID仅记录部分信息

### 监控告警规则

#### 系统级告警
- CPU使用率 > 80%（持续5分钟）
- 内存使用率 > 85%（持续3分钟）
- 磁盘使用率 > 90%

#### 服务级告警
- 接口响应时间 > 2秒
- 错误率 > 5%（1分钟内）
- 服务不可用

#### 业务级告警
- 设备离线率 > 20%
- 语音识别失败率 > 10%
- 用户登录失败率 > 15%

## 技术债务管理

项目初期，需要重点关注：
- 建立CI/CD流水线
- 配置多语言服务监控
- 实现统一日志格式
- 搭建安全扫描工具链