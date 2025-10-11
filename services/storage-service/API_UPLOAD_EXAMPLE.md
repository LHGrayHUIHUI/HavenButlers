# 增强文件上传API使用指南

## 📋 概述

本文档介绍了HavenButler存储服务的增强文件上传API，支持完整的权限设置、元数据配置和异步处理功能。

## 🚀 核心特性

- **完整权限控制**：支持PRIVATE、FAMILY、PUBLIC三种权限级别
- **智能元数据**：自动文件类型检测、标签管理
- **异步处理**：缩略图生成、OCR识别、向量标签
- **多重验证**：文件类型、大小、路径安全验证
- **链路追踪**：完整的请求日志和错误追踪

## 📚 API接口

### 基础信息

- **接口地址**：`POST /api/v1/storage/files/upload`
- **请求类型**：`multipart/form-data`
- **认证方式**：JWT Token
- **响应格式**：JSON

### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| familyId | String | 是 | 家庭ID（数据隔离） | `family_123` |
| folderPath | String | 否 | 文件夹路径，默认"/" | `/photos/2024/` |
| file | MultipartFile | 是 | 上传的文件 | - |
| uploaderUserId | String | 是 | 上传用户ID | `user_456` |
| accessLevel | AccessLevel | 是 | 权限级别 | `FAMILY` |
| description | String | 否 | 文件描述 | `家庭聚会照片` |
| tags | List<String> | 否 | 文件标签 | `["家庭", "聚会"]` |
| ownerId | String | 否 | 文件所有者ID（默认使用上传者） | `user_456` |
| generateThumbnail | Boolean | 否 | 是否生成缩略图 | `true` |
| enableOCR | Boolean | 否 | 是否启用OCR识别 | `false` |

### 权限级别说明

| 级别 | 说明 | 访问权限 |
|------|------|----------|
| PRIVATE | 私有文件 | 仅文件所有者可访问 |
| FAMILY | 家庭文件 | 家庭成员可访问 |
| PUBLIC | 公共文件 | 所有登录用户可访问 |

## 📝 使用示例

### 1. 基础上传（家庭照片）

```bash
curl -X POST "http://localhost:8081/api/v1/storage/files/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "familyId=family_123" \
  -F "folderPath=/photos/2024/" \
  -F "file=@/path/to/photo.jpg" \
  -F "uploaderUserId=user_456" \
  -F "accessLevel=FAMILY" \
  -F "description=春节家庭聚会" \
  -F "tags=[\"家庭\", \"聚会\", \"春节\"]"
```

### 2. 私有文档上传（启用OCR）

```bash
curl -X POST "http://localhost:8081/api/v1/storage/files/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "familyId=family_123" \
  -F "folderPath=/documents/contracts/" \
  -F "file=@/path/to/contract.pdf" \
  -F "uploaderUserId=user_456" \
  -F "accessLevel=PRIVATE" \
  -F "description=租房合同" \
  -F "tags=[\"合同\", \"租房\"]" \
  -F "enableOCR=true"
```

### 3. 公共资源上传（生成缩略图）

```bash
curl -X POST "http://localhost:8081/api/v1/storage/files/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "familyId=family_123" \
  -F "folderPath=/resources/templates/" \
  -F "file=@/path/to/template.png" \
  -F "uploaderUserId=user_456" \
  -F "accessLevel=PUBLIC" \
  -F "description=设计模板" \
  -F "tags=[\"模板\", \"设计\"]" \
  -F "generateThumbnail=true"
```

## 📊 响应结果

### 成功响应示例

```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "success": true,
    "fileId": "file_abc123def456",
    "familyId": "family_123",
    "fileName": "春节家庭聚会_1704110400000.jpg",
    "originalName": "IMG_20240101_120000.jpg",
    "fileSize": 2048576,
    "fileType": "image",
    "accessLevel": "FAMILY",
    "ownerId": "user_456",
    "uploaderUserId": "user_456",
    "storagePath": "family/family_123/images/2024/01/01/file_abc123def456",
    "folderPath": "/photos/2024/",
    "tags": ["家庭", "聚会", "春节"],
    "description": "春节家庭聚会",
    "uploadTime": "2024-01-01T12:00:00",
    "lastAccessTime": "2024-01-01T12:00:00",
    "accessCount": 0,
    "storageType": "minio",
    "thumbnailPath": "thumbnail/family_123/small/file_abc123def456",
    "hasThumbnail": true,
    "accessUrl": "http://minio:9000/bucket/file_abc123def456?expires=3600",
    "traceId": "tr-20240101-120000-123456"
  },
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

### 错误响应示例

```json
{
  "code": 40001,
  "message": "文件上传失败: 文件大小不能超过100MB",
  "data": null,
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

## 🔧 高级功能

### 1. 文件类型检测

系统自动检测文件类型并分类：

| 类型 | 支持格式 | 后续处理 |
|------|----------|----------|
| 图片 | jpg, png, gif, webp等 | 缩略图生成 |
| 文档 | pdf, doc, txt等 | OCR识别 |
| 视频 | mp4, avi, mov等 | 视频截图 |
| 音频 | mp3, wav, flac等 | 音频分析 |
| 压缩包 | zip, rar, 7z等 | 解压缩验证 |

### 2. 异步处理任务

上传完成后自动触发以下任务：

- **缩略图生成**：图片文件自动生成多尺寸缩略图
- **OCR识别**：文档文件自动提取文字内容
- **向量标签**：基于文件内容生成智能标签
- **存储优化**：自动选择最佳存储路径

### 3. 权限验证流程

```
1. 验证用户登录状态
2. 检查用户家庭权限
3. 验证目标路径访问权限
4. 设置文件权限级别
5. 记录权限变更审计
```

## 🛡️ 安全限制

### 文件大小限制
- **单个文件**：最大100MB
- **批量上传**：每次最多10个文件
- **家庭存储**：总容量10GB

### 文件类型限制
- **禁止类型**：可执行文件、脚本文件
- **危险文件**：.exe, .bat, .sh, .ps1等
- **路径遍历**：禁止"../"等路径跳转

### 频率限制
- **上传频率**：每分钟最多50次请求
- **用户限制**：每小时最多500MB上传量
- **家庭限制**：每小时最多1GB上传量

## 📝 最佳实践

### 1. 权限设置建议

```bash
# 家庭照片 - 使用FAMILY级别
accessLevel=FAMILY

# 个人文档 - 使用PRIVATE级别
accessLevel=PRIVATE

# 公共资源 - 使用PUBLIC级别
accessLevel=PUBLIC
```

### 2. 文件夹组织

```bash
# 推荐的文件夹结构
/photos/2024/01/     # 按年月组织照片
/documents/contracts/ # 合同文档
/resources/templates/  # 公共模板
/videos/family/        # 家庭视频
/music/personal/       # 个人音乐
```

### 3. 标签管理

```bash
# 建议的标签规范
["家庭", "聚会", "春节"]           # 场景标签
["合同", "租房", "重要"]           # 重要性标签
["模板", "设计", "PPT"]            # 类型标签
["工作", "项目", "2024Q1"]        # 项目标签
```

## 🔍 故障排查

### 常见错误及解决方案

| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 40001 | 文件大小不能超过100MB | 压缩文件或分片上传 |
| 40002 | 不支持的文件类型 | 转换为支持的格式 |
| 40003 | 家庭ID不能为空 | 检查用户登录状态 |
| 40004 | 文件夹路径格式不正确 | 使用"/"开头的路径 |
| 50001 | 存储服务异常 | 检查存储配置状态 |

### 日志查询

```bash
# 查看上传日志
grep "增强文件上传" /var/log/storage-service/app.log

# 查看错误日志
grep "ERROR" /var/log/storage-service/app.log

# 查看TraceID日志
grep "tr-20240101-120000-123456" /var/log/storage-service/app.log
```

## 📞 技术支持

如遇到问题，请联系技术支持团队：

- **GitHub Issues**：[项目地址](https://github.com/HavenButler/storage-service)
- **技术文档**：[API文档](https://docs.havenbutler.com/storage)
- **客服邮箱**：support@havenbutler.com

---

*最后更新：2024年1月1日*