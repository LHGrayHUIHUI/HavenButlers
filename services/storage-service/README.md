# HavenButler Storage Service

## 服务定位
- **架构层级**：核心业务层
- **核心职责**：为HavenButler平台提供统一的数据存储和管理服务
- **业务范围**：数据库连接管理、文件存储、知识库构建、向量标签服务

## 服务概述

HavenButler Storage Service 是一个专为个人和家庭设计的基础存储服务，提供四大核心功能：

### 🎯 核心功能

1. **数据库连接管理** - 为所有个人项目提供统一的数据库连接服务
2. **家庭文件存储** - 基于家庭ID的文件存储和管理功能
3. **个人知识库构建** - 支持文档向量化的知识库管理系统
4. **向量标签服务** - 为文件生成向量标签，支持语义搜索和大模型知识库构建

### 💡 设计特色

- **家庭隔离**：严格的数据隔离，确保家庭数据安全
- **多存储支持**：支持本地文件存储、MinIO对象存储、云存储（阿里云OSS、腾讯云COS、AWS S3）
- **存储适配器模式**：可动态切换存储方式，无需重启服务
- **向量化支持**：内置文档向量化和语义搜索功能
- **统一接口**：RESTful API设计，提供统一的访问入口
- **配置驱动**：通过配置文件灵活配置存储方式和参数

## 技术栈
- **主开发语言**：Java 17
- **核心框架**：Spring Cloud 2023.0.1, Spring Boot 3.1.0
- **通信协议**：HTTP/JSON (REST API)
- **数据存储**：PostgreSQL（数据库）、本地文件系统/MinIO/云存储（文件）、Redis（缓存）

## 快速开始

### 环境要求
- Java 17+
- Maven 3.6+

### 启动服务
```bash
# 构建项目
mvn clean package

# 启动服务
java -jar target/storage-service-1.0.0.jar

# 或使用 Maven 插件启动
mvn spring-boot:run
```

### 健康检查
```bash
curl http://localhost:8080/api/v1/storage/health
```

## 存储配置

### 存储方式选择

service-storage支持三种存储方式，可通过配置文件动态切换：

#### 1. 本地文件存储（默认）
```yaml
storage:
  file:
    storage-type: local
    local:
      base-path: /data/haven-storage  # 存储根目录
      auto-create-dirs: true          # 自动创建目录
      max-file-size: 100MB           # 最大文件大小
      allowed-extensions: "pdf,doc,docx,txt,jpg,jpeg,png,gif,mp4,avi,mp3,wav,zip,rar"
```

#### 2. MinIO对象存储
```yaml
storage:
  file:
    storage-type: minio
    minio:
      bucket-prefix: "family"         # 桶名前缀
      auto-create-bucket: true        # 自动创建桶

# MinIO连接配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  secure: false
```

#### 3. 云存储
```yaml
storage:
  file:
    storage-type: cloud
    cloud:
      provider: aliyun                # 云服务商：aliyun | tencent | aws | huawei
      region: cn-hangzhou
      access-key: your-access-key
      secret-key: your-secret-key
      bucket: your-bucket-name
```

### 数据库配置

service-storage使用PostgreSQL作为主数据库：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smarthome
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
```

### 动态切换存储方式

服务支持运行时动态切换存储方式：

```http
POST /api/v1/storage/files/switch-storage
Content-Type: application/json

{
  "storageType": "minio"  # local | minio | cloud
}
```

### 存储适配器状态检查

```http
GET /api/v1/storage/files/storage-status
```

返回结果：
```json
{
  "currentStorageType": "local",
  "isHealthy": true,
  "supportedTypes": ["local", "minio", "cloud"]
}
```

## API 接口文档

### 服务地址
- **开发环境**：http://localhost:8080
- **API前缀**：/api/v1/storage

### 1. 数据库连接管理 API

#### 获取项目数据库连接
```http
GET /api/v1/storage/database/connection/{projectId}?familyId={familyId}
```

#### 创建项目数据库
```http
POST /api/v1/storage/database/project
Content-Type: application/json

{
  "projectId": "my-project",
  "familyId": "family-123",
  "projectName": "个人博客系统",
  "databaseType": "mysql",
  "creatorUserId": "user-456"
}
```

#### 获取家庭所有项目
```http
GET /api/v1/storage/database/projects?familyId={familyId}
```

### 2. 文件存储 API

#### 上传文件
```http
POST /api/v1/storage/files/upload
Content-Type: multipart/form-data

familyId: family-123
folderPath: /documents
uploaderUserId: user-456
file: [文件内容]
```

#### 下载文件
```http
GET /api/v1/storage/files/download/{fileId}?familyId={familyId}
```

#### 获取文件列表
```http
GET /api/v1/storage/files/list?familyId={familyId}&folderPath=/documents
```

#### 搜索文件
```http
GET /api/v1/storage/files/search?familyId={familyId}&keyword=会议记录
```

#### 删除文件
```http
DELETE /api/v1/storage/files/{fileId}?familyId={familyId}&userId={userId}
```

#### 获取存储统计
```http
GET /api/v1/storage/files/stats?familyId={familyId}
```

### 3. 知识库 API

#### 创建知识库
```http
POST /api/v1/storage/knowledge/bases
Content-Type: application/json

{
  "familyId": "family-123",
  "name": "技术文档库",
  "description": "收集技术相关文档",
  "category": "技术",
  "creatorUserId": "user-456"
}
```

#### 添加文档到知识库
```http
POST /api/v1/storage/knowledge/bases/{knowledgeBaseId}/documents
Content-Type: application/json

{
  "title": "Spring Boot 使用指南",
  "content": "详细的Spring Boot开发文档内容...",
  "sourceUrl": "https://spring.io/guides",
  "userId": "user-456",
  "tags": ["spring", "java", "后端"]
}
```

#### 知识库搜索
```http
POST /api/v1/storage/knowledge/bases/{knowledgeBaseId}/search
Content-Type: application/json

{
  "query": "如何配置数据库连接",
  "topK": 5
}
```

#### 获取知识库列表
```http
GET /api/v1/storage/knowledge/bases?familyId={familyId}&userId={userId}
```

#### 删除知识库
```http
DELETE /api/v1/storage/knowledge/bases/{knowledgeBaseId}?userId={userId}
```

#### 获取知识库统计
```http
GET /api/v1/storage/knowledge/bases/{knowledgeBaseId}/stats
```

### 4. 向量标签 API

#### 生成文件向量标签
```http
POST /api/v1/storage/vector-tags/generate
Content-Type: application/json

{
  "fileId": "file-789",
  "familyId": "family-123",
  "content": "这是一份关于人工智能技术的研究报告...",
  "fileName": "AI研究报告.pdf",
  "fileType": "pdf",
  "userId": "user-456"
}
```

#### 向量相似度搜索
```http
POST /api/v1/storage/vector-tags/search
Content-Type: application/json

{
  "familyId": "family-123",
  "query": "人工智能算法",
  "topK": 10,
  "userId": "user-456"
}
```

#### 获取文件标签
```http
GET /api/v1/storage/vector-tags/files/{fileId}?familyId={familyId}
```

#### 获取标签统计
```http
GET /api/v1/storage/vector-tags/stats?familyId={familyId}
```

#### 删除文件标签
```http
DELETE /api/v1/storage/vector-tags/files/{fileId}?familyId={familyId}
```

## 使用场景示例

### 场景1：个人项目数据库管理
```javascript
// 1. 为新项目创建数据库连接
const createDb = await fetch('/api/v1/storage/database/project', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    projectId: 'my-blog',
    familyId: 'family-123',
    projectName: '个人博客',
    databaseType: 'mysql',
    creatorUserId: 'user-456'
  })
});

// 2. 获取数据库连接信息
const dbInfo = await fetch('/api/v1/storage/database/connection/my-blog?familyId=family-123');
```

### 场景2：文件存储和智能搜索
```javascript
// 1. 上传文件
const formData = new FormData();
formData.append('familyId', 'family-123');
formData.append('folderPath', '/documents');
formData.append('uploaderUserId', 'user-456');
formData.append('file', file);

const uploadResult = await fetch('/api/v1/storage/files/upload', {
  method: 'POST',
  body: formData
});

// 2. 为文件生成向量标签
const tagResult = await fetch('/api/v1/storage/vector-tags/generate', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    fileId: uploadResult.fileId,
    familyId: 'family-123',
    content: '文档内容...',
    userId: 'user-456'
  })
});

// 3. 基于语义搜索文件
const searchResult = await fetch('/api/v1/storage/vector-tags/search', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    familyId: 'family-123',
    query: '技术文档',
    topK: 5
  })
});
```

### 场景3：构建个人知识库
```javascript
// 1. 创建知识库
const kb = await fetch('/api/v1/storage/knowledge/bases', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    familyId: 'family-123',
    name: '技术学习笔记',
    description: '个人技术学习和研究笔记',
    category: '学习',
    creatorUserId: 'user-456'
  })
});

// 2. 添加文档到知识库
const doc = await fetch(`/api/v1/storage/knowledge/bases/${kb.knowledgeBaseId}/documents`, {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    title: 'Docker 容器化部署指南',
    content: '详细的Docker使用教程...',
    userId: 'user-456',
    tags: ['docker', '容器', '部署']
  })
});

// 3. 知识库语义搜索
const searchKB = await fetch(`/api/v1/storage/knowledge/bases/${kb.knowledgeBaseId}/search`, {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    query: '如何部署容器',
    topK: 3
  })
});
```

## 数据模型

### 主要数据结构

```java
// 数据库连接信息
class DatabaseConnectionInfo {
    String projectId;           // 项目ID
    String familyId;            // 家庭ID
    String projectName;         // 项目名称
    String databaseType;        // 数据库类型
    String jdbcUrl;            // JDBC连接URL
    String username;           // 用户名
    String password;           // 密码（加密）
}

// 文件元数据
class FileMetadata {
    String fileId;             // 文件ID
    String familyId;           // 家庭ID
    String fileName;           // 文件名
    String filePath;           // 文件路径
    Long fileSize;             // 文件大小
    String contentType;        // 内容类型
    String uploaderUserId;     // 上传者ID
    LocalDateTime uploadTime;  // 上传时间
}

// 知识库
class KnowledgeBase {
    String knowledgeBaseId;    // 知识库ID
    String familyId;           // 家庭ID
    String name;               // 知识库名称
    String description;        // 描述
    String category;           // 分类
    Integer documentCount;     // 文档数量
    Integer vectorCount;       // 向量数量
}

// 向量标签
class VectorTag {
    String tagId;              // 标签ID
    String fileId;             // 文件ID
    String familyId;           // 家庭ID
    String tagName;            // 标签名称
    List<Double> tagVector;    // 标签向量
    Double similarityScore;    // 相似度分数
    TagType tagType;           // 标签类型
}
```

## 配置参数

### application.yml 配置示例
```yaml
server:
  port: 8080

# 数据库连接配置
database:
  mysql:
    base-url: "jdbc:mysql://localhost:3306"
    username: "root"
    password: "password"
  connection-pool:
    max-active: 20
    initial-size: 5

# 文件存储配置
storage:
  file:
    base-path: "/data/family-storage"
    max-file-size: 100MB
    allowed-types: "pdf,doc,docx,txt,jpg,png,mp4"

# 知识库配置
knowledge:
  embedding:
    model: "text-embedding-ada-002"
  chunk:
    size: 512
  vector:
    dimension: 1536

# 向量标签配置
vectortag:
  similarity:
    threshold: 0.7
  max:
    tags: 10
```

## 开发说明

### 当前实现状态
- ✅ **API接口**：完整的REST API设计
- ✅ **数据模型**：完整的数据结构定义
- ✅ **业务逻辑**：核心服务功能实现
- 🔄 **数据存储**：当前使用内存存储（开发阶段）
- 🔄 **向量化**：当前使用模拟向量（开发阶段）

### 基础设施集成说明

本服务已完全集成HavenButler平台的基础设施：

#### 1. base-model 集成
- ✅ **实体继承**：所有核心实体继承`BaseEntity`，获得统一的通用字段和方法
- ✅ **统一响应**：所有API使用`ResponseWrapper<T>`统一响应格式
- ✅ **链路追踪**：使用`@TraceLog`注解和`TraceIdUtil`工具
- ✅ **异常处理**：继承全局异常处理体系

#### 2. common 模块集成
- ✅ **Redis支持**：集成`RedisUtils`进行缓存管理
- ✅ **加密工具**：使用`EncryptUtil`进行数据加密
- ✅ **工具类**：复用common模块的各种工具类

#### 3. Mock 实现
为了快速开发和测试，当前版本采用了以下模拟实现：

1. **数据存储**：使用 ConcurrentHashMap 模拟数据库存储
2. **文件存储**：使用内存byte数组模拟文件系统
3. **向量生成**：使用基于文本hash的伪随机向量
4. **相似度计算**：真实的余弦相似度算法

### 生产环境迁移
后续迁移到生产环境时需要：

1. **数据库集成**：
   - 替换为真实的MySQL/MongoDB连接
   - 实现数据持久化和事务支持

2. **文件存储**：
   - 集成MinIO对象存储
   - 实现文件上传下载

3. **向量化服务**：
   - 集成OpenAI Embedding API
   - 或使用开源的文本向量化模型

4. **性能优化**：
   - 添加Redis缓存
   - 实现异步处理
   - 数据库分库分表

## 错误处理

### 错误响应格式
```json
{
  "success": false,
  "errorMessage": "错误描述",
  "errorCode": "ERROR_CODE",
  "traceId": "tr-20240601-100000-123456"
}
```

### 常见错误码
- `INVALID_PARAMETERS`: 参数验证失败
- `FAMILY_NOT_FOUND`: 家庭不存在
- `FILE_NOT_FOUND`: 文件不存在
- `KNOWLEDGE_BASE_NOT_FOUND`: 知识库不存在
- `DATABASE_CONNECTION_FAILED`: 数据库连接失败

## 监控和日志

### 关键指标
- API响应时间
- 文件上传下载成功率
- 知识库搜索准确率
- 向量相似度计算性能

### 日志格式
所有操作都会记录TraceID，支持完整的请求链路追踪。

## 架构优化记录

### 🔄 2025-01-16 重大重构
基于base-model和common模块的深度优化：

#### 删除过时代码
- ❌ 删除冗余的`StorageController`（老版本）
- ❌ 删除过时的存储适配器相关代码（`StorageAdapter`、`StorageRequest`、`StorageResponse`等）
- ❌ 删除PaaS相关的过度设计代码（`PaaSController`、`PaaSRequest`等）
- ❌ 删除重复的工具类和安全类（复用base-model的）

#### 基础设施集成
- ✅ 所有实体继承`BaseEntity`（FileMetadata、DatabaseConnectionInfo、KnowledgeBase、VectorTag等）
- ✅ API响应统一使用`ResponseWrapper<T>`
- ✅ 集成`RedisUtils`进行缓存管理
- ✅ 使用`EncryptUtil`进行数据加密
- ✅ 服务类使用`@RequiredArgsConstructor`和依赖注入

#### 项目结构优化
```
当前精简结构：
src/main/java/com/haven/storage/
├── api/                    # 统一API层
│   ├── StorageController   # 四大核心服务API
│   └── StorageHealthInfo   # 健康检查
├── database/               # 数据库连接服务
├── file/                   # 家庭文件存储服务
├── knowledge/              # 个人知识库服务
├── vectortag/              # 向量标签服务
└── config/                 # 配置类
```

## 版本历史

### v1.0.0 (当前版本) - 2025-01-16
- ✅ 四大核心服务实现
- ✅ 统一API接口
- ✅ 家庭数据隔离
- ✅ 向量标签功能
- ✅ 完整的基础设施集成
- ✅ 代码结构优化和冗余清理

### 后续版本规划
- v1.1.0: 数据库持久化支持
- v1.2.0: 真实向量化集成
- v1.3.0: 性能优化和缓存
- v2.0.0: 分布式部署支持

## 联系方式
- **项目仓库**：HavenButler/services/storage-service
- **问题反馈**：请在项目仓库提交Issue
- **技术讨论**：欢迎在Discussion区域交流

---
**HavenButler Storage Service** - 专为个人和家庭设计的智能存储解决方案