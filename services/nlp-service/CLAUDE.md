# nlp-service CLAUDE.md

## 模块概述
nlp-service是HavenButler平台的自然语言处理服务，负责语音指令解析、意图识别、实体提取、多轮对话管理等NLP核心功能。

## 开发指导原则

### 1. 核心设计原则
- **准确性优先**：语音指令识别准确率≥95%
- **快速响应**：指令解析时间<500ms
- **多语言支持**：支持中文、英文等多种语言
- **上下文理解**：支持多轮对话上下文

### 2. 与其他服务的集成

```java
/**
 * 调用AI服务进行意图识别
 */
@Service
public class IntentService {
    @Autowired
    private AiServiceClient aiServiceClient;

    public Intent recognizeIntent(String text) {
        AiRequest request = AiRequest.builder()
            .type(AiTaskType.INTENT_RECOGNITION)
            .input(text)
            .build();

        return aiServiceClient.process(request);
    }
}
```

### 3. 语音指令解析

```java
/**
 * 智能家居指令模板
 */
public enum DeviceCommand {
    TURN_ON("打开|开启|启动", "设备开启"),
    TURN_OFF("关闭|关掉|停止", "设备关闭"),
    ADJUST_BRIGHTNESS("调节|设置.*亮度", "亮度调节"),
    SET_TEMPERATURE("设置.*温度|调到.*度", "温度设置");
}

@Component
public class CommandParser {
    /**
     * 解析语音指令
     */
    public DeviceOperation parseCommand(String text) {
        // 1. 清理文本
        text = cleanText(text);

        // 2. 意图识别
        Intent intent = intentService.recognizeIntent(text);

        // 3. 实体提取
        List<Entity> entities = extractEntities(text);

        // 4. 构建操作对象
        return buildOperation(intent, entities);
    }
}
```

### 4. 开发注意事项

#### 必须做的事
- 支持多种语音输入格式
- 实现意图置信度检查
- 处理模糊指令
- 记录识别失败案例

#### 不能做的事
- 不能忽略低置信度结果
- 不能缓存敏感语音内容
- 不能长时间保留对话上下文
