# Storage Service 更新日志

## [v1.1.0] - 2024-09-16

### ✨ 新功能

#### 多存储适配器支持
- **[MAJOR]** 新增存储适配器模式，支持本地、MinIO、云存储三种方式
- 新增 `LocalStorageAdapter` - 本地文件系统存储
- 新增 `MinIOStorageAdapter` - MinIO对象存储（需要MinIO服务）
- 新增 `CloudStorageAdapter` - 云存储支持（阿里云OSS、腾讯云COS、AWS S3、华为云OBS框架）

#### 动态存储切换
- **[MAJOR]** 支持运行时动态切换存储方式，无需重启服务
- 新增 `POST /api/v1/storage/files/switch-storage` 接口
- 新增 `GET /api/v1/storage/files/storage-status` 状态查询接口
- 新增 `GET /api/v1/storage/files/access-url/{fileId}` URL生成接口

#### 配置增强
- **[MAJOR]** 新增多存储配置支持
- 支持通过 `storage.file.storage-type` 配置存储方式
- 新增本地存储详细配置（路径、文件大小限制、文件类型限制）
- 新增MinIO对象存储配置（桶前缀、自动创建桶等）
- 新增云存储配置框架（支持多云提供商）

### 🔧 技术改进

#### 架构重构
- **[MAJOR]** 重构 `FamilyFileStorageService` 使用适配器模式
- 新增 `StorageAdapter` 接口统一存储操作
- 新增 `MinIOConfig` 自动配置类
- 新增存储健康检查和监控能力

#### 数据库优化
- **[BREAKING]** 确认使用PostgreSQL作为主数据库（非MySQL）
- 优化 `DatabaseConnectionService` 添加家庭项目管理
- 新增 `getFamilyProjects()` 和 `createProjectDatabase()` 方法

#### API接口增强
- 更新 `StorageController` 支持新的存储管理接口
- 优化文件上传/下载接口，兼容多存储方式
- 新增存储统计信息中的存储类型和健康状态

### 📚 文档更新

- **[MAJOR]** 完整更新 README.md，新增存储配置指南
- 新增多存储方式配置示例
- 新增动态切换存储的API文档
- 新增存储适配器状态检查文档

### 🛠 配置变更

#### 新增配置项
```yaml
storage:
  file:
    storage-type: local  # local | minio | cloud
    local:
      base-path: /data/haven-storage
      auto-create-dirs: true
      max-file-size: 100MB
      allowed-extensions: "pdf,doc,docx,txt,jpg,jpeg,png,gif,mp4,avi,mp3,wav,zip,rar"
    minio:
      bucket-prefix: "family"
      auto-create-bucket: true
    cloud:
      provider: aliyun  # aliyun | tencent | aws | huawei
      region: cn-hangzhou
      access-key: your-access-key
      secret-key: your-secret-key
      bucket: your-bucket-name

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  secure: false
```

### 💔 破坏性变更

- **[BREAKING]** `FamilyFileStorageService.getFamilyFiles()` 方法签名变更，移除了 `requesterUserId` 参数
- **[BREAKING]** `FamilyFileStorageService.searchFiles()` 方法签名变更，移除了 `requesterUserId` 参数
- **[BREAKING]** `FamilyFileStorageService.getStorageStats()` 更名为 `getFamilyStorageStats()`

### 🐛 修复

- 修复 `FileDeleteResult` 缺少 `failure()` 静态方法
- 修复 `FamilyStorageStats` 缺少存储类型和健康状态字段
- 修复 `DatabaseConnectionService` 缺少家庭项目管理方法

### 📋 TODO（后续版本）

- [ ] 完善云存储适配器的具体实现（当前为框架代码）
- [ ] 添加存储容量配额管理
- [ ] 添加文件自动备份和同步功能
- [ ] 添加存储成本分析和优化建议
- [ ] 添加文件访问权限控制

---

## [v1.0.0] - 2024-06-01

### ✨ 初始版本

- 基础的数据库连接管理服务
- 家庭文件存储功能（本地存储）
- 个人知识库构建功能
- 向量标签服务
- 基于familyId的数据隔离
- RESTful API接口设计