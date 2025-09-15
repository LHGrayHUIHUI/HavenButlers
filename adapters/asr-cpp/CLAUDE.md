# ASR C++引擎 开发指南

## 模块概述  
ASR C++引擎是HavenButler平台的语音识别核心服务，提供高性能、低延迟的实时语音转文字能力。

## 开发规范

### 1. 代码结构
```
asr-cpp/
├── src/
│   ├── core/                # 核心引擎
│   │   ├── whisper/         # Whisper.cpp封装
│   │   ├── kaldi/           # Kaldi封装
│   │   └── deepspeech/      # DeepSpeech封装
│   ├── audio/               # 音频处理
│   ├── feature/             # 特征提取
│   ├── models/              # 模型管理
│   ├── server/              # 服务器
│   │   ├── grpc/            # gRPC服务
│   │   └── websocket/       # WebSocket服务
│   └── utils/               # 工具类
├── include/                  # 头文件
├── proto/                    # gRPC定义
├── models/                   # 模型文件
├── test/                     # 测试代码
└── benchmark/                # 性能测试
```

### 2. ASR引擎接口设计

```cpp
#pragma once
#include <memory>
#include <vector>
#include <string>

namespace asr {

// 识别结果
struct TranscriptResult {
    std::string text;           // 识别文本
    float confidence;            // 置信度
    int64_t start_time_ms;       // 开始时间
    int64_t end_time_ms;         // 结束时间
    std::string language;        // 语言
    std::vector<WordInfo> words; // 词级别信息
};

// ASR引擎基类
class ASREngine {
public:
    virtual ~ASREngine() = default;
    
    // 初始化引擎
    virtual bool Initialize(const Config& config) = 0;
    
    // 离线识别
    virtual TranscriptResult Recognize(
        const int16_t* audio_data,
        size_t num_samples,
        int sample_rate
    ) = 0;
    
    // 流式识别
    virtual void StartStream() = 0;
    virtual TranscriptResult ProcessChunk(
        const int16_t* chunk,
        size_t chunk_size
    ) = 0;
    virtual TranscriptResult EndStream() = 0;
    
    // 释放资源
    virtual void Release() = 0;
};

// 引擎工厂
class ASREngineFactory {
public:
    static std::unique_ptr<ASREngine> Create(EngineType type);
};

} // namespace asr
```

### 3. Whisper.cpp集成

```cpp
#include "whisper.h"
#include "asr_engine.h"

namespace asr {

class WhisperEngine : public ASREngine {
private:
    struct whisper_context* ctx_ = nullptr;
    struct whisper_full_params params_;
    
public:
    WhisperEngine() = default;
    ~WhisperEngine() override {
        Release();
    }
    
    bool Initialize(const Config& config) override {
        // 加载模型
        ctx_ = whisper_init_from_file(config.model_path.c_str());
        if (!ctx_) {
            LOG(ERROR) << "Failed to load model: " << config.model_path;
            return false;
        }
        
        // 设置参数
        params_ = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        params_.n_threads = config.num_threads;
        params_.language = config.language.c_str();
        params_.translate = config.translate;
        params_.no_timestamps = false;
        
        return true;
    }
    
    TranscriptResult Recognize(
        const int16_t* audio_data,
        size_t num_samples,
        int sample_rate
    ) override {
        // 转换为float
        std::vector<float> audio_f32(num_samples);
        for (size_t i = 0; i < num_samples; i++) {
            audio_f32[i] = audio_data[i] / 32768.0f;
        }
        
        // 重采样到16kHz
        if (sample_rate != WHISPER_SAMPLE_RATE) {
            audio_f32 = Resample(audio_f32, sample_rate, WHISPER_SAMPLE_RATE);
        }
        
        // 执行识别
        auto start = std::chrono::high_resolution_clock::now();
        
        int ret = whisper_full(
            ctx_,
            params_,
            audio_f32.data(),
            audio_f32.size()
        );
        
        if (ret != 0) {
            LOG(ERROR) << "Whisper recognition failed";
            return {};
        }
        
        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
        
        // 构建结果
        TranscriptResult result;
        result.text = GetFullText();
        result.confidence = CalculateConfidence();
        result.language = whisper_lang_str(whisper_full_lang_id(ctx_));
        
        LOG(INFO) << "Recognition took " << duration.count() << " ms";
        
        return result;
    }
    
private:
    std::string GetFullText() {
        std::string text;
        const int n_segments = whisper_full_n_segments(ctx_);
        
        for (int i = 0; i < n_segments; ++i) {
            const char* segment_text = whisper_full_get_segment_text(ctx_, i);
            text += segment_text;
        }
        
        return text;
    }
    
    float CalculateConfidence() {
        float total_prob = 0.0f;
        int count = 0;
        
        const int n_segments = whisper_full_n_segments(ctx_);
        for (int i = 0; i < n_segments; ++i) {
            const int n_tokens = whisper_full_n_tokens(ctx_, i);
            for (int j = 0; j < n_tokens; ++j) {
                auto token_data = whisper_full_get_token_data(ctx_, i, j);
                total_prob += token_data.p;
                count++;
            }
        }
        
        return count > 0 ? total_prob / count : 0.0f;
    }
};

} // namespace asr
```

### 4. 音频处理

```cpp
#include <portaudio.h>
#include <sndfile.h>

namespace asr {

class AudioProcessor {
private:
    PaStream* stream_ = nullptr;
    std::vector<float> buffer_;
    
public:
    // 初始化PortAudio
    bool Initialize() {
        PaError err = Pa_Initialize();
        if (err != paNoError) {
            LOG(ERROR) << "PortAudio init failed: " << Pa_GetErrorText(err);
            return false;
        }
        return true;
    }
    
    // 开始录音
    bool StartRecording(int sample_rate, int channels) {
        PaStreamParameters inputParams;
        inputParams.device = Pa_GetDefaultInputDevice();
        inputParams.channelCount = channels;
        inputParams.sampleFormat = paFloat32;
        inputParams.suggestedLatency = Pa_GetDeviceInfo(inputParams.device)->defaultLowInputLatency;
        inputParams.hostApiSpecificStreamInfo = nullptr;
        
        PaError err = Pa_OpenStream(
            &stream_,
            &inputParams,
            nullptr,
            sample_rate,
            256,  // frames per buffer
            paClipOff,
            AudioCallback,
            this
        );
        
        if (err != paNoError) {
            LOG(ERROR) << "Failed to open stream: " << Pa_GetErrorText(err);
            return false;
        }
        
        err = Pa_StartStream(stream_);
        if (err != paNoError) {
            LOG(ERROR) << "Failed to start stream: " << Pa_GetErrorText(err);
            return false;
        }
        
        return true;
    }
    
    // 音频回调
    static int AudioCallback(
        const void* inputBuffer,
        void* outputBuffer,
        unsigned long framesPerBuffer,
        const PaStreamCallbackTimeInfo* timeInfo,
        PaStreamCallbackFlags statusFlags,
        void* userData
    ) {
        auto* processor = static_cast<AudioProcessor*>(userData);
        const auto* input = static_cast<const float*>(inputBuffer);
        
        // 将数据添加到缓冲区
        processor->buffer_.insert(
            processor->buffer_.end(),
            input,
            input + framesPerBuffer
        );
        
        return paContinue;
    }
    
    // VAD (语音活动检测)
    bool DetectVoiceActivity(const float* audio, size_t size) {
        // 计算能量
        float energy = 0.0f;
        for (size_t i = 0; i < size; ++i) {
            energy += audio[i] * audio[i];
        }
        energy = std::sqrt(energy / size);
        
        // 简单的阈值判断
        const float threshold = 0.01f;
        return energy > threshold;
    }
    
    // 降噪
    void Denoise(float* audio, size_t size) {
        // 使用RNNoise或其他降噪算法
        // 这里是简化的均值滤波
        const int window_size = 5;
        for (size_t i = window_size; i < size - window_size; ++i) {
            float sum = 0.0f;
            for (int j = -window_size; j <= window_size; ++j) {
                sum += audio[i + j];
            }
            audio[i] = sum / (2 * window_size + 1);
        }
    }
};

} // namespace asr
```

### 5. 实时流处理

```cpp
#include <thread>
#include <queue>
#include <mutex>
#include <condition_variable>

namespace asr {

class StreamProcessor {
private:
    std::unique_ptr<ASREngine> engine_;
    std::queue<AudioChunk> audio_queue_;
    std::mutex queue_mutex_;
    std::condition_variable queue_cv_;
    std::thread worker_thread_;
    std::atomic<bool> running_{false};
    
public:
    void Start() {
        running_ = true;
        worker_thread_ = std::thread([this] { ProcessLoop(); });
    }
    
    void Stop() {
        running_ = false;
        queue_cv_.notify_all();
        if (worker_thread_.joinable()) {
            worker_thread_.join();
        }
    }
    
    void AddAudioChunk(const AudioChunk& chunk) {
        {
            std::lock_guard<std::mutex> lock(queue_mutex_);
            audio_queue_.push(chunk);
        }
        queue_cv_.notify_one();
    }
    
private:
    void ProcessLoop() {
        engine_->StartStream();
        
        while (running_) {
            std::unique_lock<std::mutex> lock(queue_mutex_);
            queue_cv_.wait(lock, [this] {
                return !audio_queue_.empty() || !running_;
            });
            
            while (!audio_queue_.empty()) {
                auto chunk = audio_queue_.front();
                audio_queue_.pop();
                lock.unlock();
                
                // 处理音频块
                auto result = engine_->ProcessChunk(
                    chunk.data.data(),
                    chunk.data.size()
                );
                
                // 发送结果
                if (!result.text.empty()) {
                    SendResult(result);
                }
                
                lock.lock();
            }
        }
        
        auto final_result = engine_->EndStream();
        if (!final_result.text.empty()) {
            SendResult(final_result);
        }
    }
    
    void SendResult(const TranscriptResult& result) {
        // 通过WebSocket或gRPC发送结果
        LOG(INFO) << "Transcript: " << result.text;
    }
};

} // namespace asr
```

### 6. WebSocket服务

```cpp
#include <drogon/drogon.h>
#include <drogon/WebSocketController.h>

namespace asr {

class WebSocketHandler : public drogon::WebSocketController<WebSocketHandler> {
public:
    void handleNewMessage(
        const drogon::WebSocketConnectionPtr& wsConnPtr,
        std::string&& message,
        const drogon::WebSocketMessageType& type
    ) override {
        if (type == drogon::WebSocketMessageType::Binary) {
            // 处理音频数据
            ProcessAudioData(wsConnPtr, message);
        } else if (type == drogon::WebSocketMessageType::Text) {
            // 处理控制命令
            ProcessCommand(wsConnPtr, message);
        }
    }
    
    void handleNewConnection(
        const drogon::HttpRequestPtr& req,
        const drogon::WebSocketConnectionPtr& wsConnPtr
    ) override {
        LOG(INFO) << "New WebSocket connection from " 
                  << wsConnPtr->peerAddr().toIpPort();
        
        // 为每个连接创建独立的处理器
        auto processor = std::make_shared<StreamProcessor>();
        processor->Start();
        
        connections_[wsConnPtr] = processor;
    }
    
    void handleConnectionClosed(
        const drogon::WebSocketConnectionPtr& wsConnPtr
    ) override {
        LOG(INFO) << "WebSocket connection closed";
        
        auto it = connections_.find(wsConnPtr);
        if (it != connections_.end()) {
            it->second->Stop();
            connections_.erase(it);
        }
    }
    
private:
    std::map<drogon::WebSocketConnectionPtr, 
             std::shared_ptr<StreamProcessor>> connections_;
    
    void ProcessAudioData(
        const drogon::WebSocketConnectionPtr& wsConnPtr,
        const std::string& data
    ) {
        auto it = connections_.find(wsConnPtr);
        if (it == connections_.end()) {
            return;
        }
        
        // 解析音频数据
        AudioChunk chunk;
        chunk.data.resize(data.size() / sizeof(int16_t));
        std::memcpy(chunk.data.data(), data.data(), data.size());
        
        // 添加到处理队列
        it->second->AddAudioChunk(chunk);
    }
    
    void ProcessCommand(
        const drogon::WebSocketConnectionPtr& wsConnPtr,
        const std::string& command
    ) {
        Json::Value json;
        Json::Reader reader;
        
        if (!reader.parse(command, json)) {
            wsConnPtr->send("{\"error\": \"Invalid JSON\"}");
            return;
        }
        
        std::string cmd = json["command"].asString();
        
        if (cmd == "start") {
            // 开始识别
        } else if (cmd == "stop") {
            // 停止识别
        } else if (cmd == "config") {
            // 更新配置
        }
    }
};

} // namespace asr
```

### 7. 性能优化

```cpp
namespace asr {

// SIMD优化的特征提取
#include <immintrin.h>

void ExtractMFCC_AVX2(
    const float* audio,
    size_t num_samples,
    float* features
) {
    // 使用AVX2指令集加速
FFT
    // ...
}

// 多线程并行处理
class ParallelProcessor {
private:
    std::vector<std::thread> workers_;
    ThreadPool thread_pool_;
    
public:
    void ProcessBatch(
        const std::vector<AudioFile>& files,
        std::vector<TranscriptResult>& results
    ) {
        results.resize(files.size());
        
        std::vector<std::future<TranscriptResult>> futures;
        
        for (size_t i = 0; i < files.size(); ++i) {
            futures.push_back(
                thread_pool_.enqueue([this, &files, i] {
                    return ProcessFile(files[i]);
                })
            );
        }
        
        for (size_t i = 0; i < futures.size(); ++i) {
            results[i] = futures[i].get();
        }
    }
};

} // namespace asr
```

### 8. 测试

```cpp
#include <gtest/gtest.h>

namespace asr {

class ASREngineTest : public ::testing::Test {
protected:
    std::unique_ptr<ASREngine> engine_;
    
    void SetUp() override {
        engine_ = ASREngineFactory::Create(EngineType::WHISPER);
        
        Config config;
        config.model_path = "models/whisper-base.bin";
        config.language = "en";
        config.num_threads = 4;
        
        ASSERT_TRUE(engine_->Initialize(config));
    }
};

TEST_F(ASREngineTest, RecognizeEnglish) {
    // 加载测试音频
    auto audio = LoadTestAudio("test_data/english.wav");
    
    // 执行识别
    auto result = engine_->Recognize(
        audio.data(),
        audio.size(),
        16000
    );
    
    // 验证结果
    EXPECT_FALSE(result.text.empty());
    EXPECT_GT(result.confidence, 0.8f);
    EXPECT_EQ(result.language, "en");
}

TEST_F(ASREngineTest, StreamRecognition) {
    auto audio = LoadTestAudio("test_data/long_audio.wav");
    
    engine_->StartStream();
    
    // 分块处理
    const size_t chunk_size = 16000;  // 1秒
    for (size_t i = 0; i < audio.size(); i += chunk_size) {
        size_t actual_size = std::min(chunk_size, audio.size() - i);
        
        auto result = engine_->ProcessChunk(
            audio.data() + i,
            actual_size
        );
        
        if (!result.text.empty()) {
            LOG(INFO) << "Partial: " << result.text;
        }
    }
    
    auto final_result = engine_->EndStream();
    EXPECT_FALSE(final_result.text.empty());
}

// 性能测试
TEST_F(ASREngineTest, BenchmarkRTF) {
    auto audio = LoadTestAudio("test_data/benchmark.wav");
    
    auto start = std::chrono::high_resolution_clock::now();
    
    auto result = engine_->Recognize(
        audio.data(),
        audio.size(),
        16000
    );
    
    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
    
    float audio_duration_ms = (audio.size() / 16.0f);  // 16kHz
    float rtf = duration.count() / audio_duration_ms;
    
    LOG(INFO) << "RTF: " << rtf;
    EXPECT_LT(rtf, 0.3f);  // RTF < 0.3
}

} // namespace asr
```

## 开发注意事项

1. **内存管理**：使用RAII和智能指针
2. **线程安全**：注意多线程访问共享资源
3. **性能优化**：使用SIMD指令和并行处理
4. **错误处理**：完善的异常处理
5. **数据存储**：严禁直接访问数据库，必须通过storage-service

## 常用命令

```bash
# 构建
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)

# 运行
./asr_server --config ../configs/config.yaml

# 测试
ctest --output-on-failure

# 性能测试
./benchmark/asr_benchmark

# Docker构建
docker build -t smart-home/asr-cpp:v1.0.0 .

# Docker运行
docker run -d --name asr-cpp --network smart-home-network smart-home/asr-cpp:v1.0.0
```