# storage-service CLAUDE.md

## 模块概述
storage-service是HavenButler平台的数据存储核心服务，所有微服务的数据操作都必须通过此服务。作为数据访问的唯一入口，必须保证高性能、高可用和数据安全。

## 开发指导原则

### 1. 核心设计原则
- **单一职责**：只负责数据存储，不包含业务逻辑
- **数据隔离**：严格的多租户数据隔离
- **统一接口**：为不同存储提供统一的访问接口
- **性能优先**：缓存、异步、批量操作

### 2. 严格的访问控制

#### 2.1 服务认证
```java
/**
 * 每个微服务都有唯一的API Key
 * 请求必须包含：
 * 1. Service-Key: 服务密钥
 * 2. JWT Token: 用户令牌
 * 3. Family-ID: 家庭标识
 */
@Component
public class ServiceAuthFilter {
    // 服务密钥映射
    private static final Map<String, String> SERVICE_KEYS = Map.of(
        "account-service", "key_account_xxx",
        "message-service", "key_message_xxx",
        "ai-service", "key_ai_xxx"
    );
    
    public boolean authenticate(String serviceKey) {
        // 验证服务合法性
    }
}
```

#### 2.2 数据权限隔离
```java
/**
 * 基于Family ID的数据隔离
 * 所有查询自动添加family_id条件
 */
@Aspect
public class DataIsolationAspect {
    @Before("@annotation(DataIsolation)")
    public void addFamilyFilter(JoinPoint point) {
        // 自动注入family_id查询条件
        // 防止跨家庭数据访问
    }
}
```

### 3. 多存储适配实现

#### 3.1 统一数据模型
```java
/**
 * 统一的数据操作请求
 */
public class StorageRequest {
    private String storageType;    // mysql/mongo/redis/minio
    private String operation;       // insert/update/delete/query
    private String collection;      // 表名/集合名
    private Map<String, Object> data;    // 数据内容
    private Map<String, Object> filter;  // 查询条件
    private String familyId;        // 家庭ID（必需）
    private String traceId;         // 链路追踪ID
}
```

#### 3.2 适配器模式
```java
/**
 * 存储适配器接口
 */
public interface StorageAdapter {
    StorageResponse execute(StorageRequest request);
    boolean supports(String storageType);
}

/**
 * MySQL适配器实现
 */
@Component
public class MySQLAdapter implements StorageAdapter {
    @Override
    public StorageResponse execute(StorageRequest request) {
        // 1. 参数验证
        // 2. SQL防注入检查
        // 3. 数据加密（如需要）
        // 4. 执行操作
        // 5. 审计日志
        // 6. 返回结果
    }
}
```

### 4. 数据加密实现

#### 4.1 字段级加密
```java
/**
 * 敏感字段自动加密
 * 使用AES-256-GCM算法
 */
@Component
public class FieldEncryptor {
    // 需要加密的字段
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "phone", "idCard", "bankCard", "email"
    );
    
    public Map<String, Object> encryptFields(Map<String, Object> data) {
        for (String field : SENSITIVE_FIELDS) {
            if (data.containsKey(field)) {
                data.put(field, encrypt(data.get(field)));
            }
        }
        return data;
    }
    
    private String encrypt(Object value) {
        // AES-256-GCM加密实现
        // 密钥从KMS获取
    }
}
```

#### 4.2 密钥管理
```yaml
密钥管理策略:
  - 主密钥: 存储在KMS中
  - 数据密钥: 使用主密钥加密，存储在配置中心
  - 密钥轮换: 每90天自动轮换
  - 密钥备份: 多地备份，防止丢失
```

### 5. 缓存策略实现

#### 5.1 多级缓存
```java
/**
 * L1: 本地缓存（Caffeine）
 * L2: Redis缓存
 * L3: 数据库
 */
@Component
public class MultiLevelCache {
    private final Cache<String, Object> localCache;  // Caffeine
    private final RedisTemplate<String, Object> redisTemplate;
    
    public Object get(String key) {
        // 1. 查本地缓存
        Object value = localCache.getIfPresent(key);
        if (value != null) return value;
        
        // 2. 查Redis缓存
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            localCache.put(key, value);
            return value;
        }
        
        // 3. 查数据库
        value = loadFromDatabase(key);
        if (value != null) {
            redisTemplate.opsForValue().set(key, value, 10, TimeUnit.MINUTES);
            localCache.put(key, value);
        }
        return value;
    }
}
```

#### 5.2 缓存更新策略
- **Cache Aside**：更新数据库后删除缓存
- **Write Through**：同时更新缓存和数据库
- **Write Behind**：先更新缓存，异步更新数据库

### 6. 批量操作优化

```java
/**
 * 批量操作优化
 * 减少网络往返，提高吞吐量
 */
@Component
public class BatchProcessor {
    private static final int BATCH_SIZE = 1000;
    
    public void batchInsert(List<Map<String, Object>> dataList) {
        // 分批处理，避免内存溢出
        Lists.partition(dataList, BATCH_SIZE)
            .parallelStream()
            .forEach(batch -> {
                // 批量插入
                jdbcTemplate.batchUpdate(sql, batch);
            });
    }
}
```

### 7. 分库分表策略

```java
/**
 * 基于family_id的分库分表
 */
@Component
public class ShardingStrategy {
    private static final int DB_COUNT = 8;
    private static final int TABLE_COUNT = 32;
    
    public String getDatabase(String familyId) {
        int hash = familyId.hashCode();
        int dbIndex = Math.abs(hash % DB_COUNT);
        return "smart_home_" + dbIndex;
    }
    
    public String getTable(String baseTable, String familyId) {
        int hash = familyId.hashCode();
        int tableIndex = Math.abs(hash % TABLE_COUNT);
        return baseTable + "_" + tableIndex;
    }
}
```

### 8. 数据备份实现

```java
/**
 * 自动备份任务
 */
@Component
public class BackupTask {
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
    public void dailyBackup() {
        // 1. MySQL全量备份
        backupMySQL();
        
        // 2. MongoDB备份
        backupMongoDB();
        
        // 3. 压缩加密
        compressAndEncrypt();
        
        // 4. 上传到MinIO
        uploadToMinIO();
        
        // 5. 清理过期备份
        cleanOldBackups();
    }
}
```

### 9. 监控和告警

```java
/**
 * 关键指标监控
 */
@Component
public class MetricsCollector {
    private final MeterRegistry registry;
    
    // 查询性能监控
    @Timed("storage.query.time")
    public Object query(StorageRequest request) {
        // 记录查询耗时
    }
    
    // 连接池监控
    @Scheduled(fixedRate = 60000)
    public void collectPoolMetrics() {
        // 记录连接池状态
        registry.gauge("db.connections.active", dataSource.getActiveConnections());
        registry.gauge("db.connections.idle", dataSource.getIdleConnections());
    }
}
```

### 10. 故障处理

#### 10.1 熔断降级
```java
@Component
public class CircuitBreakerConfig {
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(3)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build())
            .build());
    }
}
```

#### 10.2 故障转移
```yaml
主从切换:
  - 主库故障检测: 心跳超时3次
  - 自动切换: 提升从库为主库
  - 数据同步: 检查数据一致性
  - 服务恢复: 更新连接配置
```

### 11. 开发注意事项

#### 必须做的事
- 所有操作必须记录审计日志
- 敏感数据必须加密存储
- 必须进行SQL注入防护
- 必须实现数据隔离
- 必须进行权限校验

#### 不能做的事
- 不能暴露数据库连接信息
- 不能在日志中打印敏感数据
- 不能跨家庭查询数据
- 不能绕过缓存直接查库
- 不能执行未经验证的SQL

### 12. 性能调优建议
- 使用连接池，避免频繁创建连接
- 批量操作代替单条操作
- 异步处理非关键路径
- 合理使用索引
- 定期分析慢查询