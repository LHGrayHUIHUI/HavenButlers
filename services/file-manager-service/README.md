# file-manager-service 文件管理服务

## 服务定位
- **架构层级**：核心业务层
- **核心职责**：文件上传下载、存储管理、图片处理、文件安全扫描
- **业务范围**：文件存储、多媒体处理、安全管控、访问控制

## 技术栈
- **主开发语言**：Java 17
- **核心框架**：Spring Cloud 2023.0.1, Spring Boot 3.1.0
- **基础组件**：集成 infrastructure/base-model 和 infrastructure/common
- **通信协议**：HTTP/JSON (外部), gRPC (内部)
- **存储系统**：MinIO、本地文件系统
- **数据存储**：通过 storage-service 统一访问

## 部署信息
- **Docker镜像**：`smart-home/file-manager-service:v1.0.0`
- **内部端口**：8080 (Docker网络内)
- **健康检查**：`/actuator/health`
- **环境变量**：
  ```
  NACOS_ADDR=nacos:8848
  STORAGE_SERVICE_URL=http://storage-service:8080
  MINIO_ENDPOINT=http://minio:9000
  MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY}
  MINIO_SECRET_KEY=${MINIO_SECRET_KEY}
  FILE_UPLOAD_PATH=/uploads
  MAX_FILE_SIZE=50MB
  ```

## Infrastructure集成

### 依赖的基础模块
本服务集成了以下infrastructure基础模块：

1. **base-model模块** - [查看文档](../../infrastructure/base-model/README.md)
   - 统一响应格式：所有API返回ResponseWrapper
   - 全局异常处理：文件上传异常、存储异常处理
   - 链路追踪：文件操作全链路追踪
   - 基础实体：文件信息、上传记录

2. **common模块** - [查看文档](../../infrastructure/common/README.md)
   - Redis工具：文件信息缓存、上传状态缓存
   - 消息队列：异步文件处理任务
   - 线程池：并发处理文件上传和处理
   - 限流组件：防止文件上传滥用

### 配置说明
```yaml
# application.yml中已配置
base:
  exception:
    enabled: true  # 启用全局异常处理
  trace:
    enabled: true  # 启用链路追踪

common:
  redis:
    enabled: true
    key-prefix: "file:"
  mq:
    enabled: true  # 启用消息队列处理文件任务
  thread-pool:
    enabled: true
    core-pool-size: 20  # 文件处理需要更多线程
    max-pool-size: 50
```

### 使用示例
```java
@Service
public class FileUploadService {
    @Autowired
    private MessageSender messageSender;

    @TraceLog("文件上传")
    @RateLimit(window = 60, limit = 20)  // 限制每分钟20次上传
    public ResponseWrapper<FileInfo> uploadFile(MultipartFile file, String familyId) {
        // 1. 基础验证
        validateFile(file);

        // 2. 异步处理文件
        String taskId = IdGenerator.generateUuid();
        FileProcessTask task = new FileProcessTask(taskId, file, familyId);
        messageSender.sendFileTask(task, 7);  // 中等优先级

        // 3. 返回处理状态
        return ResponseWrapper.success("文件上传中", taskId);
    }
}
```

## 更新历史
- v1.0.0 (2025-01-16): 初始版本，基础文件管理功能，集成infrastructure基础模块