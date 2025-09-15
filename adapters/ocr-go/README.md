# OCR Go引擎

## 服务定位
- **架构层级**：多语言适配层
- **核心职责**：提供高性能的OCR文字识别服务，支持图片、文档、实时视频流的文字提取
- **业务范围**：文档识别、表格解析、身份证识别、车牌识别、场景文字识别

## 技术栈
- **主开发语言**：Go 1.21+
- **Web框架**：Gin
- **OCR引擎**：Tesseract 5.0、PaddleOCR
- **图像处理**：OpenCV-Go、imaging
- **通信协议**：gRPC（与Java服务通信）
- **消息队列**：RabbitMQ（异步任务）

## 部署信息
- **Docker镜像**：`smart-home/ocr-go:v1.0.0`
- **内部端口**：8091 (Docker网络内)
- **健康检查**：`/health`
- **环境变量**：
  ```bash
  GATEWAY_URL=http://gateway:8080
  STORAGE_SERVICE_URL=http://storage-service:8080
  TESSERACT_DATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata
  MAX_WORKERS=10
  ENABLE_GPU=false
  ```

## 接口信息

### 内部服务调用
- **通信协议**：gRPC + TLS
- **Proto定义**：`proto/ocr_service.proto`
- **主要接口**：
  ```protobuf
  service OCRService {
    rpc RecognizeImage(ImageRequest) returns (TextResult);
    rpc RecognizeDocument(DocumentRequest) returns (DocumentResult);
    rpc RecognizeVideo(stream VideoFrame) returns (stream TextResult);
    rpc ExtractTable(ImageRequest) returns (TableResult);
    rpc RecognizeIDCard(ImageRequest) returns (IDCardResult);
  }
  ```

### 异步任务接口
- **批量OCR**：通过RabbitMQ接收任务
- **结果回调**：Webhook或消息队列
- **任务状态查询**：`GET /task/{task_id}`

## OCR能力

### 文字识别
- **中文识别**：简体/繁体，准确率>98%
- **英文识别**：大小写、数字、符号
- **多语种**：支持70+语言
- **手写体**：中英文手写识别

### 结构化识别
- **表格识别**：自动解析表格结构
- **版面分析**：标题、正文、图片区分
- **公式识别**：数学/化学公式

### 证件识别
- **身份证**：姓名、证号、地址等
- **银行卡**：卡号、有效期
- **车牌**：各类车牌格式
- **发票**：增值税发票、机打发票

## 图像预处理

```go
// 图像预处理流程
type ImagePreprocessor struct {
    // 灰度化
    Grayscale() error
    // 二值化
    Binarize(threshold int) error
    // 去噪
    Denoise() error
    // 倾斜矫正
    DeskewCorrection() error
    // 边缘检测
    EdgeDetection() error
    // 文本区域定位
    TextRegionDetection() []Rectangle
}
```

## 依赖关系
- **直接依赖**：
  - `storage-service:v1.0.0` - 图片/结果存储
  - `file-manager-service:v1.0.0` - 文件管理
- **被依赖方**：
  - `ai-service` - 文档理解
  - `nlp-service` - 文本分析

## 数据访问规范
⚠️ **严禁直接连接数据库** - 所有数据操作必须通过 `storage-service` 接口

## 性能指标
- 单张图片识别：< 200ms (1080p)
- 批量处理：100张/分钟
- 并发能力：50请求/秒
- 视频实时OCR：30fps
- 准确率：>95%（标准文档）

## 优化策略

### 并发处理
```go
type WorkerPool struct {
    workers   int
    taskQueue chan Task
    results   chan Result
}

func (wp *WorkerPool) Process() {
    for i := 0; i < wp.workers; i++ {
        go wp.worker()
    }
}
```

### 缓存机制
- **结果缓存**：Redis缓存识别结果
- **模型缓存**：预加载常用模型
- **LRU策略**：淘汰不常用缓存

### GPU加速（可选）
- CUDA支持
- OpenCL支持
- 批量处理优化

## 独立测试部署
```bash
# 启动最小依赖环境
docker-compose -f docker/test-compose.yml up -d

# 启动OCR服务
docker run -d \
  --name ocr-go \
  --network smart-home-network \
  -v /tmp/tesseract:/usr/share/tesseract-ocr \
  smart-home/ocr-go:v1.0.0

# 测试OCR功能
curl -X POST http://localhost:8091/ocr/image \
  -F "image=@test.jpg" \
  -H "Content-Type: multipart/form-data"
```

## Docker网络配置
- **网络名称**：smart-home-network
- **容器名称**：ocr-go
- **端口映射**：仅测试环境临时开放 localhost:8091->8091
- **内部通信**：其他服务通过 `ocr-go:8091` 访问

## 测试要求
### 单元测试
```bash
go test ./... -v -cover
# 覆盖率要求：≥75%
```

### 基准测试
```bash
go test -bench=. -benchmem
```

### Docker集成测试
```bash
# 必须在Docker环境中测试
docker-compose -f docker/integration-test.yml up --abort-on-container-exit
```

## 监控和日志
- **关键指标**：识别准确率、处理时间、队列长度
- **日志级别**：INFO (OCR任务), ERROR (识别失败)
- **TraceID**：所有请求必须携带并传递
- **Prometheus指标**：提供/metrics端点

## 故障排查
1. **识别率低**：检查图片质量和预处理参数
2. **处理缓慢**：增加Worker数量或启用GPU
3. **内存泄漏**：检查Goroutine泄漏
4. **模型加载失败**：确认tessdata路径

## 更新历史
- v1.0.0 (2025-01-15): 初始版本，支持基本OCR功能