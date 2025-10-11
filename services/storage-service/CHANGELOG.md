# File Storage Service 更新日志

## [v2.1.0] - 2024-10-11

### 🚀 重大功能扩展

#### 图片画廊功能 🖼️
- **[MAJOR]** 新增智能图片处理引擎，支持多尺寸缩略图自动生成
- **[MAJOR]** 新增EXIF元数据提取功能（拍摄时间、相机信息、GPS位置等）
- **[MAJOR]** 新增图片自动分类系统（按时间、地点、设备、内容分类）
- **[MAJOR]** 新增图片画廊展示接口（网格视图、时间线、分类浏览）
- **[MAJOR]** 新增图片搜索和筛选功能（按标签、日期、地点筛选）

#### 文件分享系统 🔗
- **[MAJOR]** 新增灵活的文件分享机制，支持多种分享模式
- **[MAJOR]** 新增细粒度权限控制（只读、下载、编辑、评论权限）
- **[MAJOR]** 新增分享链接管理（生成、过期、撤销、访问统计）
- **[MAJOR]** 新增分享安全机制（密码保护、访问频率限制、过期管理）
- **[MAJOR]** 新增分享审计日志和访问统计分析

#### 基础组件集成 ⚙️
- **[MAJOR]** 重新集成Haven Base-Model，提供统一的日志和审计能力
- **[MAJOR]** 重新集成Haven Common，利用通用工具类和模型定义
- **[MAJOR]** 新增统一异常处理和错误码管理
- **[MAJOR]** 新增统一监控指标和健康检查

#### 存储架构优化 📁
- **[MAJOR]** 优化按家庭组织的存储结构，每个家庭独立的存储空间
- **[MAJOR]** 新增家庭存储统计和容量监控
- **[MAJOR]** 实现自动数据清理机制（过期分享、临时文件、孤儿文件）
- **[MAJOR]** 新增跨存储后端的文件备份能力

### 🔧 技术改进

#### 图片处理技术栈
```java
// 新增图片处理核心组件
- ImageProcessingEngine: 图片处理引擎
- ThumbnailatorProcessor: 缩略图生成器
- ExifExtractor: EXIF元数据提取器
- ImageClassifier: 图片自动分类器
- ThumbnailCacheManager: 缩略图缓存管理器
```

#### 分享系统架构
```java
// 新增分享系统核心组件
- SharePermissionManager: 分享权限管理器
- ShareTokenService: 分享令牌服务
- ShareAuditService: 分享审计服务
- ShareAnalyticsService: 分享统计分析服务
- ShareSecurityManager: 分享安全管理器
```

#### 基础组件集成
```xml
<!-- 重新集成的核心依赖 -->
<dependency>
    <groupId>com.haven</groupId>
    <artifactId>base-model</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.haven</groupId>
    <artifactId>common</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 新增图片处理依赖 -->
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.19</version>
</dependency>
<dependency>
    <groupId>com.drewnoakes</groupId>
    <artifactId>metadata-extractor</artifactId>
    <version>2.18.0</version>
</dependency>
```

#### API接口扩展
```bash
# 新增图片画廊接口
GET  /api/v1/gallery/images/{fileId}/thumbnail/{size}  # 获取缩略图
GET  /api/v1/gallery/images/{fileId}/exif              # 获取EXIF信息
GET  /api/v1/gallery/family/{familyId}                 # 家庭图片画廊
GET  /api/v1/gallery/family/{familyId}/categories      # 图片分类
GET  /api/v1/gallery/family/{familyId}/timeline        # 时间线视图

# 新增文件分享接口
POST /api/v1/share/files/{fileId}           # 创建分享链接
GET  /api/v1/share/{shareId}                 # 访问分享文件
DELETE /api/v1/share/{shareId}               # 撤销分享
GET  /api/v1/share/{shareId}/stats           # 分享统计
GET  /api/v1/share/my/file/{fileId}          # 我的分享
GET  /api/v1/share/received                  # 收到的分享

# 扩展统计接口
GET  /api/v1/storage/stats/gallery        # 画廊统计
GET  /api/v1/storage/stats/share          # 分享统计
```

#### 配置项扩展
**新增图片处理配置**：
```yaml
image-processing:
  enabled: true
  thumbnail:
    enabled: true
    sizes:
      - name: small
        width: 200
        height: 200
        quality: 0.8
      - name: medium
        width: 400
        height: 400
        quality: 0.85
      - name: large
        width: 800
        height: 800
        quality: 0.9
  exif:
    enabled: true
    extract-gps: true
    extract-camera-info: true
    extract-date: true
  auto-classification:
    enabled: true
    by-date: true
    by-location: true
    by-event: true
```

**新增分享配置**：
```yaml
share:
  enabled: true
  default-permissions: READ_ONLY
  max-expire-hours: 720
  password-min-length: 6
  share-url-prefix: "https://haven.example.com/share"
  security:
    rate-limit: 100
    tracking-enabled: true
    encryption-enabled: true
```

**家庭存储配置**：
```yaml
storage:
  family-organization: true
  auto-cleanup:
    enabled: true
    expired-shares: true
    temp-thumbnails: true
    orphan-files: true
    schedule: "0 0 2 * * ?"  # 每天凌晨2点
```

### 📚 文档更新

- **[MAJOR]** 完整重写README.md，新增图片画廊和分享功能介绍
- **[MAJOR]** 重写CLAUDE.md开发指导，集成base-model和common组件
- 新增图片处理最佳实践指南
- 新增文件分享安全配置指南
- 新增性能优化和监控建议
- 新增家庭存储结构说明

### 💔 破坏性变更

#### 依赖变更
- **[BREAKING]** 重新引入base-model和common依赖（从v2.0.0中移除的）
- **[BREAKING]** 新增图片处理相关依赖（thumbnailator、metadata-extractor）
- **[BREAKING]** 调整Docker镜像名称从v2.0.0升级到v2.1.0

#### 配置变更
- **[BREAKING]** 新增图片处理配置项（image-processing.*）
- **[BREAKING]** 新增分享系统配置项（share.*）
- **[BREAKING]** 新增家庭存储组织配置项

#### API变更
- **[BREAKING]** 新增图片画廊相关API路径（/api/v1/gallery/*）
- **[BREAKING]** 新增分享相关API路径（/api/v1/share/*）
- **[BREAKING]** 扩展存储统计接口，增加画廊和分享统计

### 🐛 修复
- 修复图片缩略图生成的内存溢出问题
- 修复分享链接绕过权限验证的安全漏洞
- 修复EXIF数据解析异常的处理问题
- 修复跨家庭数据访问的安全漏洞
- 修复文件分类逻辑错误

### 🚀 性能优化
- 实现多级缓存策略（Redis + 本地缓存）
- 异步图片处理，不阻塞主上传流程
- 智能缩略图预加载机制
- 分享链接访问频率限制
- 家庭数据批量清理优化

### 📋 TODO（后续版本）

#### v2.2.0 计划
- [ ] 实现云存储适配器（阿里云OSS、腾讯云COS、华为云OBS）
- [ ] 添加文件版本管理和历史记录
- [ ] 实现图片OCR识别功能
- [ ] 添加AI图片标签和智能分类
- [ ] 实现文件批量操作和批量分享

#### v2.3.0 计划
- [ ] 实现文件同步和备份功能
- [ ] 添加文件全文搜索支持
- [ ] 实现访问统计分析仪表板
- [ ] 添加存储成本分析和优化建议
- [ ] 实现文件权限细粒度控制

#### v3.0.0 规划
- [ ] 云原生部署优化（Kubernetes）
- [ ] 实现分布式文件系统支持
- [ ] 添加边缘缓存支持
- [ ] 实现智能存储策略选择
- [ ] 支持大规模文件存储和分发

---

## [v2.0.0] - 2024-10-11

### 🚀 重大架构重构

#### 服务定位变更
- **[BREAKING]** 服务从 `storage-service` 重构为 `file-storage-service`，专注于文件存储功能
- **[BREAKING]** 移除数据库代理功能（PostgreSQL、MongoDB、Redis访问代理）
- **[BREAKING]** 移除统一日志系统功能（日志收集、处理、分析）
- **[BREAKING]** 重新定位为HavenButler智能家庭文件管理与知识库系统的存储基础

#### 新增核心功能
- **[MAJOR]** 实现多存储适配器架构（本地文件系统、MinIO、S3兼容云存储）
- **[MAJOR]** 新增统一文件存储接口（StorageService）
- **[MAJOR]** 实现基于Family ID的严格文件数据隔离
- **[MAJOR]** 新增文件访问权限控制（基于JWT和用户角色）
- **[MAJOR]** 支持流式文件上传下载，避免内存溢出
- **[MAJOR]** 新增文件类型安全检查和病毒扫描机制

#### 存储适配器系统
- 新增 `LocalStorageAdapter` - 本地文件系统存储适配器
- 新增 `S3StorageAdapter` - S3兼容存储适配器（支持MinIO、AWS S3、阿里云OSS等）
- 新增 `StorageServiceFactory` - 动态存储适配器选择工厂
- 新增 `StorageHealthMonitor` - 存储后端健康检查和监控

#### 安全与权限
- 实现文件访问权限控制（文件所有者、家庭管理员、成员、访客）
- 新增文件类型白名单和黑名单验证
- 实现基于Family ID的文件数据隔离
- 新增文件操作审计日志
- 支持预签名URL，减少直接文件访问风险

#### 性能优化
- 实现大文件流式处理（上传、下载）
- 新增异步文件处理队列（格式转换、缩略图生成、病毒扫描）
- 实现多级缓存策略（本地缓存、Redis缓存）
- 新增存储容量监控和告警机制
- 支持文件压缩和分块传输

#### 知识库集成支持
- 新增Apache Tika集成，支持文件内容提取
- 为Knowledge Index Service提供文件访问接口
- 支持文件版本管理基础框架
- 新增文件元数据管理和标签系统

### 🔧 技术改进

#### 架构重构
- **[BREAKING]** 重构项目包结构，移除数据库代理相关包
- **[BREAKING]** 重构API接口，从 `/api/v1/storage/*` 改为 `/api/v1/files/*`
- **[BREAKING]** 重构配置文件，移除数据库相关配置，新增存储适配器配置
- 新增模块化设计，支持插件式存储适配器扩展

#### API接口变更
```bash
# 新增文件管理接口
POST /api/v1/files/upload                 # 文件上传
GET  /api/v1/files/{fileId}/download      # 文件下载
DELETE /api/v1/files/{fileId}             # 删除文件
GET  /api/v1/files/{fileId}/info          # 获取文件信息

# 新增存储管理接口
GET  /api/v1/storage/status               # 存储状态
POST /api/v1/storage/switch               # 切换存储方式
GET  /api/v1/storage/{fileId}/url         # 生成访问URL

# 移除的接口（BREAKING CHANGE）
DELETE /api/v1/storage/{type}/{operation} # 数据库操作接口
POST /api/v1/proxy/{database}             # 数据库代理接口
POST /api/v1/logs/unified                 # 日志接口
```

#### 配置变更
**新增配置项**：
```yaml
storage:
  type: local  # local | s3 | cloud
  local:
    base-path: /data/haven-storage
    auto-create-dirs: true
    max-file-size: 100MB
    allowed-extensions: "pdf,doc,docx,txt,jpg,jpeg,png,gif"
  s3:
    bucket-prefix: "family"
    auto-create-bucket: true
    region: us-east-1

file-security:
  enabled: true
  virus-scan: true
  allowed-extensions: "pdf,doc,docx,txt,jpg,jpeg,png,gif"
  blocked-mime-types: "application/x-executable"

file-processing:
  async: true
  generate-thumbnails: true
  extract-metadata: true
  knowledge-indexing: true
```

**移除配置项**：
- `spring.datasource.*` - 数据库连接配置
- `storage.mongodb.*` - MongoDB配置
- `storage.redis.*` - Redis配置
- `haven.logging.*` - 统一日志配置

#### 依赖变更
**新增依赖**：
```xml
<!-- 文件存储 -->
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-s3</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
</dependency>

<!-- 安全验证 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-imaging</artifactId>
</dependency>
```

**移除依赖**：
```xml
<!-- 数据库相关 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

<!-- 日志相关 -->
<dependency>
    <groupId>com.haven</groupId>
    <artifactId>base-model</artifactId>
</dependency>
```

### 📚 文档更新

- **[MAJOR]** 完整重写README.md，聚焦文件存储功能
- **[MAJOR]** 重写CLAUDE.md开发指导，适配新架构
- 新增存储适配器配置指南
- 新增文件安全控制最佳实践
- 新增性能优化建议
- 新增知识库集成指南

### 💔 破坏性变更

#### 接口变更
- **[BREAKING]** 移除所有数据库代理相关接口
- **[BREAKING]** 移除所有日志系统相关接口
- **[BREAKING]** API路径从 `/api/v1/storage/*` 更改为 `/api/v1/files/*`
- **[BREAKING]** 服务端口从 8081 更改为 8086

#### 依赖变更
- **[BREAKING]** 移除JPA、MongoDB、Redis等数据库依赖
- **[BREAKING]** 移除base-model统一日志依赖
- **[BREAKING]** 新增S3 SDK、Apache Tika等文件处理依赖

#### 配置变更
- **[BREAKING]** 移除数据库连接配置
- **[BREAKING]** 移除日志系统配置
- **[BREAKING]** 新增存储适配器配置
- **[BREAKING]** 新增文件安全配置

### 🐛 修复
- 修复大文件上传内存溢出问题
- 修复跨家庭文件访问安全漏洞
- 修复文件类型验证绕过问题
- 修复存储路径穿越漏洞

### 📋 TODO（后续版本）

#### v2.1.0 计划 (已完成)
- [x] 实现图片画廊功能（缩略图、EXIF、分类展示）
- [x] 新增文件分享系统（权限控制、链接管理）
- [x] 重新集成base-model和common基础组件
- [x] 优化按家庭组织的存储结构
- [x] 增强图片处理和元数据提取能力
- [x] 完善分享权限和安全机制
- [x] 新增分享统计和审计功能

#### v2.2.0 计划
- [ ] 实现云存储适配器（阿里云OSS、腾讯云COS、华为云OBS）
- [ ] 添加文件版本管理和历史记录
- [ ] 实现文件共享和协作功能
- [ ] 添加文件智能分析和推荐

#### v2.3.0 计划
- [ ] 实现文件同步和备份功能
- [ ] 添加文件全文搜索支持
- [ ] 实现文件访问统计分析
- [ ] 添加存储成本分析和优化建议

#### v3.0.0 规划
- [ ] 云原生部署优化（Kubernetes）
- [ ] 实现分布式文件系统支持
- [ ] 添加边缘缓存支持
- [ ] 实现智能存储策略选择

---

## [v1.1.0] - 2024-09-16

### ✨ 新功能（原版本功能，已移除）

#### 多存储适配器支持
- 新增存储适配器模式，支持本地、MinIO两种方式
- 新增 `LocalStorageAdapter` - 本地文件系统存储
- 新增 `MinIOStorageAdapter` - MinIO对象存储
- 支持运行时动态切换存储方式

#### 动态存储切换
- 支持运行时动态切换存储方式，无需重启服务
- 新增 `POST /api/v1/storage/files/switch-storage` 接口
- 新增 `GET /api/v1/storage/files/storage-status` 状态查询接口

### 📋 已移除功能
**注意**：v1.1.0版本的所有功能在v2.0.0架构重构中已被重新设计或移除

---

## [v1.0.0] - 2024-06-01

### ✨ 初始版本（已废弃）

- 基础的数据库连接管理服务
- 家庭文件存储功能（本地存储）
- 个人知识库构建功能
- 向量标签服务
- 基于familyId的数据隔离
- RESTful API接口设计

**注意**：v1.0.0版本为多功能存储服务，在v2.0.0中已完全重构为纯文件存储服务