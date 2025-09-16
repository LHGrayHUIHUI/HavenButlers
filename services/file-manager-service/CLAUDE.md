# file-manager-service CLAUDE.md

## 模块概述
file-manager-service是HavenButler平台的文件管理服务，负责文件上传、下载、存储、压缩、图片处理等文件相关功能。

## 开发指导原则

### 1. 核心设计原则
- **安全存储**：文件加密存储，防止泄露
- **格式支持**：支持多种文件格式
- **大文件处理**：分片上传、断点续传
- **访问控制**：基于权限的文件访问

### 2. 文件上传处理

```java
@RestController
public class FileUploadController {
    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public ResponseWrapper<FileInfo> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("familyId") String familyId) {
        
        // 1. 文件格式检查
        validateFileType(file);
        
        // 2. 文件大小检查
        validateFileSize(file);
        
        // 3. 病毒扫描
        scanFile(file);
        
        // 4. 上传到MinIO
        String fileId = minioService.upload(file, familyId);
        
        return ResponseWrapper.success(fileInfo);
    }
}
```

### 3. 图片处理

```java
@Service
public class ImageProcessor {
    /**
     * 图片压缩
     */
    public void compressImage(String fileId, int quality) {
        // 1. 下载原图
        InputStream input = minioService.download(fileId);
        
        // 2. 压缩处理
        BufferedImage compressed = compress(input, quality);
        
        // 3. 上传压缩图
        minioService.upload(compressed, fileId + "_compressed");
    }
}
```

### 4. 开发注意事项

#### 必须做的事
- 文件类型白名单检查
- 文件大小限制
- 病毒扫描
- 访问权限控制

#### 不能做的事
- 不能存储恶意文件
- 不能无限制上传
- 不能泄露文件内容
