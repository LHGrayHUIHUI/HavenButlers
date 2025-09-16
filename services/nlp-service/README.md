# nlp-service 自然语言处理服务

## 服务定位
- **架构层级**：核心业务层
- **核心职责**：语音指令解析、意图识别、实体提取、多轮对话管理
- **业务范围**：自然语言处理、语音控制理解、智能对话

## 技术栈
- **主开发语言**：Java 17
- **核心框架**：Spring Cloud 2023.0.1, Spring Boot 3.1.0
- **基础组件**：集成 infrastructure/base-model 和 infrastructure/common
- **通信协议**：gRPC (内部), HTTP/JSON (外部)
- **数据存储**：通过 storage-service 统一访问

## 部署信息
- **Docker镜像**：`smart-home/nlp-service:v1.0.0`
- **内部端口**：8080 (Docker网络内)
- **健康检查**：`/actuator/health`
- **环境变量**：
  ```
  NACOS_ADDR=nacos:8848
  STORAGE_SERVICE_URL=http://storage-service:8080
  AI_SERVICE_URL=http://ai-service:8080
  NLP_MODEL_PATH=/models
  ```

## Infrastructure集成

### 依赖的基础模块
本服务集成了以下infrastructure基础模块：

1. **base-model模块** - [查看文档](../../infrastructure/base-model/README.md)
   - 统一响应格式：所有API返回ResponseWrapper
   - 全局异常处理：NLP处理异常、模型加载异常
   - 链路追踪：指令处理全链路追踪
   - 基础实体：对话上下文、意图识别结果

2. **common模块** - [查看文档](../../infrastructure/common/README.md)
   - Redis工具：对话上下文缓存、模型结果缓存
   - 消息队列：异步处理NLP任务
   - 线程池：并发处理多个语音指令
   - 限流组件：防止NLP服务过载

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
    key-prefix: "nlp:"
  mq:
    enabled: true  # 启用消息队列处理NLP任务
  thread-pool:
    enabled: true
    core-pool-size: 10  # NLP任务处理线程池
```

### 使用示例
```java
@Service
public class VoiceCommandService {
    @Autowired
    private MessageSender messageSender;

    @TraceLog("处理语音指令")
    @RateLimit(window = 60, limit = 50)  // 限制每分钟50次请求
    public ResponseWrapper<CommandResult> processVoiceCommand(VoiceCommandRequest request) {
        // 1. 异步处理语音指令
        messageSender.sendNlpTask(request, 8);  // 高优先级

        // 2. 返回处理ID
        String taskId = IdGenerator.generateUuid();
        return ResponseWrapper.success("指令已接收", taskId);
    }
}
```

## 更新历史
- v1.0.0 (2025-01-16): 初始版本，基础NLP功能，集成infrastructure基础模块