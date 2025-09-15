# OCR Go引擎 开发指南

## 模块概述
OCR Go引擎是HavenButler平台的文字识别服务，提供高性能、高准确率的OCR能力。

## 开发规范

### 1. 代码结构
```
ocr-go/
├── cmd/
│   └── server/
│       └── main.go           # 入口文件
├── internal/
│   ├── engine/              # OCR引擎
│   │   ├── tesseract/       # Tesseract封装
│   │   └── paddle/          # PaddleOCR封装
│   ├── processor/           # 图像处理
│   ├── service/             # 业务服务
│   ├── grpc/                # gRPC服务
│   ├── worker/              # 工作池
│   └── cache/               # 缓存管理
├── pkg/
│   ├── models/              # 数据模型
│   └── utils/               # 工具函数
├── proto/                    # gRPC定义
├── configs/                  # 配置文件
└── docker/                   # Docker配置
```

### 2. OCR引擎封装

#### 引擎接口定义
```go
package engine

import (
    "context"
    "image"
)

// OCREngine OCR引擎接口
type OCREngine interface {
    // 初始化引擎
    Init(config Config) error
    
    // 识别图片
    RecognizeImage(ctx context.Context, img image.Image, opts Options) (*Result, error)
    
    // 识别文件
    RecognizeFile(ctx context.Context, path string, opts Options) (*Result, error)
    
    // 批量识别
    RecognizeBatch(ctx context.Context, images []image.Image, opts Options) ([]*Result, error)
    
    // 释放资源
    Close() error
}

// Result OCR结果
type Result struct {
    Text       string      `json:"text"`       // 识别文本
    Confidence float64     `json:"confidence"` // 置信度
    Boxes      []BoundBox  `json:"boxes"`      // 文本区域
    Language   string      `json:"language"`   // 语言
    TimeMs     int64       `json:"time_ms"`    // 耗时
}

// BoundBox 文本边界框
type BoundBox struct {
    Text       string    `json:"text"`
    Confidence float64   `json:"confidence"`
    Points     []Point   `json:"points"`
}
```

#### Tesseract引擎实现
```go
package tesseract

import (
    "github.com/otiai10/gosseract/v2"
)

type TesseractEngine struct {
    client *gosseract.Client
    config Config
}

func NewTesseractEngine() *TesseractEngine {
    return &TesseractEngine{
        client: gosseract.NewClient(),
    }
}

func (e *TesseractEngine) Init(config Config) error {
    e.config = config
    
    // 设置语言
    if config.Language != "" {
        e.client.SetLanguage(config.Language)
    }
    
    // 设置数据路径
    if config.DataPath != "" {
        e.client.SetTessdataPrefix(config.DataPath)
    }
    
    // 设置页面分割模式
    e.client.SetPageSegMode(gosseract.PSM_AUTO)
    
    return nil
}

func (e *TesseractEngine) RecognizeImage(ctx context.Context, img image.Image, opts Options) (*Result, error) {
    startTime := time.Now()
    
    // 设置图片
    e.client.SetImageFromBytes(imageToBytes(img))
    
    // 执行OCR
    text, err := e.client.Text()
    if err != nil {
        return nil, err
    }
    
    // 获取边界框
    boxes := e.client.GetBoundingBoxes(gosseract.RIL_WORD)
    
    return &Result{
        Text:       text,
        Confidence: e.client.MeanTextConf(),
        Boxes:      convertBoxes(boxes),
        Language:   e.config.Language,
        TimeMs:     time.Since(startTime).Milliseconds(),
    }, nil
}
```

### 3. 图像预处理

```go
package processor

import (
    "gocv.io/x/gocv"
    "image"
)

type ImageProcessor struct {
    mat gocv.Mat
}

// 灰度化
func (p *ImageProcessor) Grayscale() error {
    gocv.CvtColor(p.mat, &p.mat, gocv.ColorBGRToGray)
    return nil
}

// 二值化
func (p *ImageProcessor) Binarize(threshold float32) error {
    gocv.Threshold(p.mat, &p.mat, threshold, 255, gocv.ThresholdBinary)
    return nil
}

// 去噪
func (p *ImageProcessor) Denoise() error {
    gocv.MedianBlur(p.mat, &p.mat, 3)
    return nil
}

// 倾斜矫正
func (p *ImageProcessor) DeskewCorrection() error {
    // 检测直线
    lines := gocv.NewMat()
    gocv.HoughLines(p.mat, &lines, 1, math.Pi/180, 100)
    
    // 计算倾斜角度
    angle := calculateSkewAngle(lines)
    
    // 旋转矫正
    center := image.Point{X: p.mat.Cols() / 2, Y: p.mat.Rows() / 2}
    rotMatrix := gocv.GetRotationMatrix2D(center, angle, 1.0)
    gocv.WarpAffine(p.mat, &p.mat, rotMatrix, image.Point{X: p.mat.Cols(), Y: p.mat.Rows()})
    
    return nil
}

// 文本区域检测
func (p *ImageProcessor) DetectTextRegions() []image.Rectangle {
    // 使用MSER算法检测文本区域
    mser := gocv.NewMSER()
    regions := mser.DetectRegions(p.mat)
    
    var rects []image.Rectangle
    for _, region := range regions {
        rect := gocv.BoundingRect(region)
        rects = append(rects, rect)
    }
    
    return mergeOverlappingRects(rects)
}
```

### 4. 工作池实现

```go
package worker

import (
    "context"
    "sync"
)

type WorkerPool struct {
    workers   int
    taskQueue chan Task
    results   chan Result
    wg        sync.WaitGroup
    ctx       context.Context
    cancel    context.CancelFunc
}

type Task struct {
    ID    string
    Image image.Image
    Opts  Options
}

type Result struct {
    ID    string
    Data  *OCRResult
    Error error
}

func NewWorkerPool(workers int) *WorkerPool {
    ctx, cancel := context.WithCancel(context.Background())
    return &WorkerPool{
        workers:   workers,
        taskQueue: make(chan Task, workers*2),
        results:   make(chan Result, workers*2),
        ctx:       ctx,
        cancel:    cancel,
    }
}

func (wp *WorkerPool) Start() {
    for i := 0; i < wp.workers; i++ {
        wp.wg.Add(1)
        go wp.worker(i)
    }
}

func (wp *WorkerPool) worker(id int) {
    defer wp.wg.Done()
    
    // 创建OCR引擎实例
    engine := createEngine()
    defer engine.Close()
    
    for {
        select {
        case <-wp.ctx.Done():
            return
        case task := <-wp.taskQueue:
            result := wp.processTask(engine, task)
            wp.results <- result
        }
    }
}

func (wp *WorkerPool) processTask(engine OCREngine, task Task) Result {
    // 预处理图像
    processed := preprocessImage(task.Image)
    
    // 执行OCR
    ocrResult, err := engine.RecognizeImage(wp.ctx, processed, task.Opts)
    
    return Result{
        ID:    task.ID,
        Data:  ocrResult,
        Error: err,
    }
}

func (wp *WorkerPool) Submit(task Task) {
    wp.taskQueue <- task
}

func (wp *WorkerPool) GetResult() <-chan Result {
    return wp.results
}

func (wp *WorkerPool) Stop() {
    wp.cancel()
    wp.wg.Wait()
    close(wp.taskQueue)
    close(wp.results)
}
```

### 5. gRPC服务实现

```go
package grpc

import (
    "context"
    pb "ocr-go/proto"
    "google.golang.org/grpc"
)

type OCRServer struct {
    pb.UnimplementedOCRServiceServer
    service *OCRService
    pool    *WorkerPool
}

func NewOCRServer(service *OCRService) *OCRServer {
    return &OCRServer{
        service: service,
        pool:    NewWorkerPool(10),
    }
}

func (s *OCRServer) RecognizeImage(ctx context.Context, req *pb.ImageRequest) (*pb.TextResult, error) {
    // 解码图片
    img, err := decodeImage(req.ImageData)
    if err != nil {
        return nil, err
    }
    
    // 执行OCR
    result, err := s.service.RecognizeImage(ctx, img, convertOptions(req.Options))
    if err != nil {
        return nil, err
    }
    
    return &pb.TextResult{
        Text:       result.Text,
        Confidence: result.Confidence,
        Boxes:      convertBoxesToProto(result.Boxes),
        TimeMs:     result.TimeMs,
    }, nil
}

// 流式视频OCR
func (s *OCRServer) RecognizeVideo(stream pb.OCRService_RecognizeVideoServer) error {
    for {
        frame, err := stream.Recv()
        if err == io.EOF {
            return nil
        }
        if err != nil {
            return err
        }
        
        // 异步处理视频帧
        go func(f *pb.VideoFrame) {
            result := s.processVideoFrame(f)
            stream.Send(result)
        }(frame)
    }
}
```

### 6. 缓存管理

```go
package cache

import (
    "github.com/go-redis/redis/v8"
    "time"
)

type CacheManager struct {
    client *redis.Client
    ttl    time.Duration
}

func NewCacheManager(addr string, ttl time.Duration) *CacheManager {
    client := redis.NewClient(&redis.Options{
        Addr: addr,
    })
    
    return &CacheManager{
        client: client,
        ttl:    ttl,
    }
}

// 缓存OCR结果
func (c *CacheManager) SetResult(key string, result *OCRResult) error {
    data, err := json.Marshal(result)
    if err != nil {
        return err
    }
    
    return c.client.Set(ctx, key, data, c.ttl).Err()
}

// 获取缓存结果
func (c *CacheManager) GetResult(key string) (*OCRResult, error) {
    data, err := c.client.Get(ctx, key).Bytes()
    if err != nil {
        return nil, err
    }
    
    var result OCRResult
    err = json.Unmarshal(data, &result)
    return &result, err
}

// 生成缓存键
func GenerateCacheKey(imageHash string, opts Options) string {
    h := sha256.New()
    h.Write([]byte(imageHash))
    h.Write([]byte(fmt.Sprintf("%+v", opts)))
    return hex.EncodeToString(h.Sum(nil))
}
```

### 7. 性能监控

```go
package metrics

import (
    "github.com/prometheus/client_golang/prometheus"
    "github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
    ocrRequests = prometheus.NewCounterVec(
        prometheus.CounterOpts{
            Name: "ocr_requests_total",
            Help: "Total number of OCR requests",
        },
        []string{"type", "status"},
    )
    
    ocrDuration = prometheus.NewHistogramVec(
        prometheus.HistogramOpts{
            Name:    "ocr_duration_seconds",
            Help:    "OCR processing duration",
            Buckets: prometheus.DefBuckets,
        },
        []string{"type"},
    )
    
    ocrAccuracy = prometheus.NewGaugeVec(
        prometheus.GaugeOpts{
            Name: "ocr_accuracy_percent",
            Help: "OCR accuracy percentage",
        },
        []string{"engine"},
    )
)

func init() {
    prometheus.MustRegister(ocrRequests)
    prometheus.MustRegister(ocrDuration)
    prometheus.MustRegister(ocrAccuracy)
}

func RecordOCRRequest(reqType string, status string) {
    ocrRequests.WithLabelValues(reqType, status).Inc()
}

func RecordOCRDuration(reqType string, duration float64) {
    ocrDuration.WithLabelValues(reqType).Observe(duration)
}
```

### 8. 测试策略

```go
// ocr_test.go
package ocr

import (
    "testing"
    "image"
)

func TestTesseractEngine(t *testing.T) {
    engine := NewTesseractEngine()
    err := engine.Init(Config{
        Language: "eng+chi_sim",
        DataPath: "/usr/share/tesseract-ocr/5/tessdata",
    })
    
    if err != nil {
        t.Fatalf("Failed to init engine: %v", err)
    }
    
    // 加载测试图片
    img := loadTestImage("testdata/sample.jpg")
    
    // 执行OCR
    result, err := engine.RecognizeImage(context.Background(), img, Options{})
    if err != nil {
        t.Fatalf("Failed to recognize: %v", err)
    }
    
    // 验证结果
    if result.Text == "" {
        t.Error("Empty text result")
    }
    
    if result.Confidence < 0.8 {
        t.Errorf("Low confidence: %f", result.Confidence)
    }
}

func BenchmarkOCR(b *testing.B) {
    engine := NewTesseractEngine()
    engine.Init(defaultConfig)
    img := loadTestImage("testdata/sample.jpg")
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        engine.RecognizeImage(context.Background(), img, Options{})
    }
}
```

## 开发注意事项

1. **并发安全**：确保所有共享资源的线程安全
2. **内存管理**：及时释放图像内存，避免泄漏
3. **错误处理**：完善的错误处理和重试机制
4. **性能优化**：使用工作池和缓存
5. **数据存储**：严禁直接访问数据库，必须通过storage-service

## 常用命令

```bash
# 依赖管理
go mod download
go mod tidy

# 构建
go build -o bin/ocr-server cmd/server/main.go

# 运行
./bin/ocr-server -config configs/config.yaml

# 测试
go test ./... -v -cover

# 基准测试
go test -bench=. -benchmem

# Docker构建
docker build -t smart-home/ocr-go:v1.0.0 .

# Docker运行
docker run -d --name ocr-go --network smart-home-network smart-home/ocr-go:v1.0.0
```