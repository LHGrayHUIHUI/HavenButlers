# ASR C++引擎

## 服务定位
- **架构层级**：多语言适配层
- **核心职责**：提供高性能、低延迟的语音识别服务，支持实时语音转文字和离线识别
- **业务范围**：语音识别、语音指令、声纹识别、语言检测、情绪分析

## 技术栈
- **主开发语言**：C++ 17
- **构建工具**：CMake 3.20+
- **Web框架**：Drogon Framework
- **ASR引擎**：Whisper.cpp、Kaldi、DeepSpeech
- **音频处理**：PortAudio、libsndfile
- **通信协议**：gRPC（与Java服务通信）、WebRTC（实时流）
- **深度学习**：ONNX Runtime、LibTorch

## 部署信息
- **Docker镜像**：`smart-home/asr-cpp:v1.0.0`
- **内部端口**：8092 (Docker网络内)
- **WebSocket端口**：8093 (实时语音流)
- **健康检查**：`/health`
- **环境变量**：
  ```bash
  GATEWAY_URL=http://gateway:8080
  STORAGE_SERVICE_URL=http://storage-service:8080
  MODEL_PATH=/models/whisper
  MAX_CONCURRENT_STREAMS=100
  ENABLE_GPU=false
  AUDIO_SAMPLE_RATE=16000
  ```

## 接口信息

### 内部服务调用
- **通信协议**：gRPC + TLS
- **Proto定义**：`proto/asr_service.proto`
- **主要接口**：
  ```protobuf
  service ASRService {
    rpc RecognizeAudio(AudioRequest) returns (TranscriptResult);
    rpc RecognizeStream(stream AudioChunk) returns (stream TranscriptResult);
    rpc DetectVoiceActivity(AudioRequest) returns (VADResult);
    rpc IdentifySpeaker(AudioRequest) returns (SpeakerResult);
    rpc AnalyzeEmotion(AudioRequest) returns (EmotionResult);
  }
  ```

### WebSocket实时接口
- **连接地址**：`ws://asr-cpp:8093/stream`
- **消息格式**：JSON + Base64音频
- **实时转写**：边说边转，延迟<500ms

## ASR能力

### 语音识别
- **中文识别**：普通话/方言，准确率>95%
- **英文识别**：美式/英式，准确率>97%
- **多语种**：支持50+语言
- **混合语言**：中英混合识别
- **实时转写**：流式输出，低延迟

### 高级功能
- **声纹识别**：说话人识别和验证
- **语音活动检测**：VAD，区分语音/非语音
- **关键词唤醒**：自定义唤醒词
- **情绪分析**：识别喜怒哀乐
- **语速检测**：识别说话速度

### 降噪处理
- **环境噪音抑制**
- **回声消除**
- **自动增益控制**
- **音频增强**

## 音频处理流程

```cpp
// 音频处理管道
class AudioPipeline {
public:
    // 音频采样
    void resample(int targetRate);
    
    // 噪音抑制
    void denoise();
    
    // VAD检测
    std::vector<Segment> detectVoiceActivity();
    
    // 特征提取
    FeatureMatrix extractFeatures();
    
    // 模型推理
    TranscriptResult inference(const FeatureMatrix& features);
};
```

## 依赖关系
- **直接依赖**：
  - `storage-service:v1.0.0` - 音频/结果存储
  - `nlp-service:v1.0.0` - 文本后处理
- **被依赖方**：
  - `ai-service` - 语音交互
  - `gateway-service` - API路由

## 数据访问规范
⚠️ **严禁直接连接数据库** - 所有数据操作必须通过 `storage-service` 接口

## 性能指标
- 实时率：RTF < 0.3 (处理时间/音频时长)
- 延迟：< 500ms (端到端)
- 并发流：100路同时处理
- 准确率：> 95% (安静环境)
- 内存占用：< 1GB (单模型)

## 优化策略

### 模型优化
```cpp
class ModelOptimizer {
public:
    // 量化
    void quantize(Model& model, QuantType type);
    
    // 剪枝
    void prune(Model& model, float sparsity);
    
    // 蒸馏
    void distill(Model& teacher, Model& student);
    
    // ONNX转换
    void exportONNX(const Model& model, const std::string& path);
};
```

### 并发处理
- **线程池**：多线程处理音频流
- **批处理**：批量推理优化
- **流水线**：分阶段并行处理

### GPU加速（可选）
- CUDA支持
- TensorRT优化
- OpenVINO支持

## 独立测试部署
```bash
# 启动最小依赖环境
docker-compose -f docker/test-compose.yml up -d

# 启动ASR服务
docker run -d \
  --name asr-cpp \
  --network smart-home-network \
  -v /models:/models \
  smart-home/asr-cpp:v1.0.0

# 测试语音识别
curl -X POST http://localhost:8092/asr/recognize \
  -F "audio=@test.wav" \
  -H "Content-Type: multipart/form-data"

# 测试WebSocket实时识别
wscat -c ws://localhost:8093/stream
```

## Docker网络配置
- **网络名称**：smart-home-network
- **容器名称**：asr-cpp
- **端口映射**：仅测试环境临时开放
  - localhost:8092->8092 (HTTP/gRPC)
  - localhost:8093->8093 (WebSocket)
- **内部通信**：其他服务通过 `asr-cpp:8092` 访问

## 测试要求
### 单元测试
```bash
mkdir build && cd build
cmake .. -DBUILD_TESTS=ON
make
ctest --output-on-failure
# 覆盖率要求：≥75%
```

### 性能测试
```bash
./benchmark/asr_benchmark
```

### Docker集成测试
```bash
# 必须在Docker环境中测试
docker-compose -f docker/integration-test.yml up --abort-on-container-exit
```

## 监控和日志
- **关键指标**：识别准确率、RTF、延迟、并发数
- **日志级别**：INFO (识别任务), ERROR (识别失败)
- **TraceID**：所有请求必须携带并传递
- **Prometheus指标**：提供/metrics端点

## 故障排查
1. **识别率低**：检查音频采样率和噪音水平
2. **延迟高**：优化模型或启用GPU
3. **内存泄漏**：检查音频缓冲区管理
4. **模型加载失败**：确认模型文件路径

## 更新历史
- v1.0.0 (2025-01-15): 初始版本，支持基本语音识别