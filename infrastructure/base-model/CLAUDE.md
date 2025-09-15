# base-model CLAUDE.md

## 模块概述
base-model是HavenButler平台的基础模块，为所有Java微服务提供统一的基础能力支撑。本模块是其他所有服务的依赖基础，任何改动都需要慎重考虑兼容性。

## 开发指导原则

### 1. 设计原则
- **最小依赖原则**：base-model应尽量减少外部依赖，只包含必要的Spring Boot基础组件
- **向后兼容原则**：任何API变更都必须保持向后兼容，避免破坏依赖服务
- **高内聚低耦合**：各功能模块相互独立，可按需引用
- **可配置化**：所有功能都应该可以通过配置开关控制

### 2. 代码组织结构
```
src/main/java/com/haven/base/
├── common/                      # 通用组件
│   ├── response/               # 响应体相关
│   │   ├── ResponseWrapper.java
│   │   └── ErrorCode.java
│   ├── exception/              # 异常体系
│   │   ├── BaseException.java
│   │   ├── BusinessException.java
│   │   └── SystemException.java
│   └── constants/              # 常量定义
│       ├── SecurityConstants.java
│       └── SystemConstants.java
├── utils/                      # 工具类
│   ├── TraceIdUtil.java      # TraceID工具
│   ├── EncryptUtil.java      # 加密工具
│   ├── JsonUtil.java         # JSON工具
│   └── ValidationUtil.java   # 校验工具
├── model/                      # 通用模型
│   ├── dto/                   # 数据传输对象
│   │   ├── DeviceDTO.java
│   │   ├── UserDTO.java
│   │   └── FamilyDTO.java
│   └── entity/                # 基础实体
│       └── BaseEntity.java
├── annotation/                 # 自定义注解
│   ├── TraceLog.java
│   ├── RateLimit.java
│   └── Permission.java
├── aspect/                     # 切面实现
│   ├── TraceLogAspect.java
│   └── RateLimitAspect.java
└── config/                     # 自动配置
    └── BaseModelAutoConfiguration.java
```

### 3. 核心功能实现要求

#### 3.1 TraceID生成规范
```java
/**
 * TraceID格式：tr-yyyyMMdd-HHmmss-随机6位
 * 示例：tr-20250115-143022-a3b5c7
 */
public class TraceIdUtil {
    private static final String PREFIX = "tr";
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    
    /**
     * 生成TraceID
     * 所有服务间调用必须携带此ID用于链路追踪
     */
    public static String generate() {
        // 实现代码
    }
}
```

#### 3.2 统一响应格式
```java
/**
 * 统一响应包装器
 * 所有API响应必须使用此格式
 */
public class ResponseWrapper<T> {
    private int code;           // 响应码：0表示成功
    private String message;      // 响应消息
    private T data;             // 响应数据
    private String traceId;     // 链路追踪ID
    private long timestamp;     // 响应时间戳
    
    // 成功响应快捷方法
    public static <T> ResponseWrapper<T> success(T data) {
        // 实现代码
    }
    
    // 失败响应快捷方法
    public static ResponseWrapper<?> error(ErrorCode errorCode) {
        // 实现代码
    }
}
```

#### 3.3 加密工具要求
- 必须支持AES-256加密算法
- 密钥通过配置中心管理，不能硬编码
- 提供加密、解密、签名验证等方法
- 支持对敏感字段自动加密（通过注解）

### 4. 与其他服务的集成

#### 4.1 storage-service集成
- 所有服务的数据操作都通过storage-service
- base-model提供storage-service的通用请求/响应模型
- 定义数据操作的标准接口规范

#### 4.2 配置中心集成
- base-model需要从Nacos读取全局配置
- 提供配置刷新机制的基础支持
- 敏感配置的加密解密支持

### 5. 测试要求
- 单元测试覆盖率必须≥90%
- 每个工具类都要有完整的测试用例
- 异常场景必须充分测试
- 性能测试：工具类方法执行时间<10ms

### 6. 性能优化建议
- 使用对象池管理频繁创建的对象
- 工具类使用静态方法，避免实例化
- 合理使用缓存，减少重复计算
- 日志级别可配置，生产环境避免DEBUG日志

### 7. 安全要求
- 加密密钥不能存储在代码中
- 敏感信息（密码、token）不能打印到日志
- SQL注入、XSS等安全防护要内置
- 提供请求签名验证机制

### 8. 版本发布流程
1. 修改前先评估影响范围
2. 重大变更需要发布beta版本先行测试
3. 发布新版本时更新所有依赖服务的版本号
4. 维护详细的变更日志（CHANGELOG.md）

### 9. 常见问题处理

#### Q: 如何添加新的错误码？
A: 在ErrorCode枚举中添加，遵循错误码规范（1xxxx系统级，2xxxx认证级等）

#### Q: 如何扩展新的工具类？
A: 在utils包下创建，必须是静态方法，添加完整的JavaDoc注释

#### Q: 如何处理循环依赖？
A: base-model不应该依赖任何业务服务，只提供基础能力

### 10. 代码提交规范
- feat: 新功能
- fix: 修复bug
- refactor: 重构代码
- test: 添加测试
- docs: 文档更新
- perf: 性能优化

提交信息示例：
```
feat: 添加分布式锁工具类

- 基于Redis实现分布式锁
- 支持可重入锁
- 自动续期机制
```