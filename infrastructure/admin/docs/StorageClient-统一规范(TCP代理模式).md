# StorageClient 统一规范（TCP 代理模式）

> 目标：为 HavenButler 各微服务提供一套与“Storage 数据库/Redis TCP 代理”对接的统一客户端抽象，屏蔽 Nacos 发现、凭证加载、TLS、安全与动态变更细节，确保接入一致性与可观测性。

## 1. 设计原则
- 数据面走原生协议（JDBC/Mongo/Redis），不经 HTTP；Storage 以 TCP 代理/路由实现读写分离与高可用。
- 配置集中在 Nacos：逻辑库/命名空间、代理主机:端口、池参数、TLS 策略、凭证（经环境/KMS 注入）。
- 动态刷新：Nacos 变更后驱动连接参数更新（新建连接生效、旧连接平滑回收）。
- 安全合规：支持 mTLS/服务 Token；日志与指标接入 Micrometer；敏感值不落盘。

规范清单（TL;DR）
- 线程安全：实现必须是线程安全的，连接工厂与缓存使用并发容器；
- 生命周期：支持优雅刷新（双通道构建新连接池、旧池在空闲后关闭）；
- 超时与重试：默认连接超时 ≤1s、读超时 ≤2s；禁止在客户端做写重试（由上游补偿）；
- 指标：输出连接池活跃/空闲、获取等待耗时、执行耗时、错误率；
- 覆盖顺序：环境变量 > Nacos > 本地默认；
- 日志：禁止输出明文凭证/URI；变更仅输出摘要（host:port 更替）。

## 2. 统一术语
- 逻辑库 `logicalName`：userdb/analytics/...，映射到某类数据库（postgres/mysql/mongodb）。
- 命名空间 `namespace`：Redis 的逻辑命名空间（session/cache/...）。
- 代理端点 `endpoint`：`host:port`（例如 `storage-pg-proxy:5432`）。

## 3. 接口能力（建议放置于 base-model/common）

### 3.1 Java 接口示例（规范原型）
```java
package com.haven.storage;

import java.util.Map;
import javax.sql.DataSource;

/**
 * StorageClient - 统一存储代理客户端（TCP模式）
 *
 * 作用：
 * 1) 为应用提供按“逻辑库/命名空间”的连接工厂（JDBC/Mongo/Redis）
 * 2) 封装 Nacos 发现、凭证加载、TLS 配置与动态刷新
 * 3) 输出连接/池/错误等观测指标
 */
public interface StorageClient {

    /**
     * 获取 JDBC DataSource（读写分离由代理负责）
     * @param logicalName 逻辑库名（如 userdb）
     * @return 已初始化的数据源，内部基于代理 host:port 构造 JDBC URL
     */
    DataSource getDataSource(String logicalName);

    /**
     * 获取 Redis 连接 URI（或集群配置），建议配合 Lettuce/Jedis 使用
     * @param namespace 命名空间（如 cache/session）
     * @return 标准 redis:// 或 rediss:// URI
     */
    String getRedisUri(String namespace);

    /**
     * 获取 Mongo 连接 URI（或 ClientSettings），建议配合 Mongo 驱动使用
     * @param logicalName 逻辑库名（如 analytics）
     * @return 标准 mongodb:// 或 mongodb+srv:// URI
     */
    String getMongoUri(String logicalName);

    /**
     * 获取 TLS 配置（如启用 mTLS）
     * @return TLS 相关配置（keystore/truststore 路径与密码等）
     */
    TlsConfig getTlsConfig();

    /**
     * 订阅配置变更（Nacos 更新后回调），由实现方保证回调幂等
     */
    void registerChangeListener(ConfigChangeListener listener);

    /**
     * 手动触发刷新（容错场景）
     */
    void refresh();

    /** TLS 配置数据结构 */
    class TlsConfig {
        public final boolean enabled;
        public final String keyStorePath;
        public final String keyStorePassword;
        public final String trustStorePath;
        public final String trustStorePassword;
        public TlsConfig(boolean enabled, String ksp, String kspw, String tsp, String tspw) {
            this.enabled = enabled; this.keyStorePath = ksp; this.keyStorePassword = kspw; this.trustStorePath = tsp; this.trustStorePassword = tspw;
        }
    }

    /** 配置变更监听 */
    interface ConfigChangeListener {
        /**
         * 当指定逻辑库/命名空间的端点、凭证或 TLS 配置发生变化时触发
         * @param scope 作用域（logicalName/namespace）
         * @param details 变化详情（key-value）
         */
        void onChanged(String scope, Map<String, String> details);
    }
}
```

### 3.2 Nacos 键位（参考）
```yaml
storage:
  databases:
    - name: userdb
      type: postgres
      proxy-endpoint: storage-pg-proxy:5432   # TCP 代理端点
      database: userdb
      pool: { max-size: 50, min-idle: 5 }
      tls: { enabled: true }
  redis:
    mode: cluster
    proxy-endpoint: storage-redis-proxy:6379
    password: ${REDIS_PASSWORD}
  mongo:
    - name: analytics
      proxy-endpoint: storage-mongo-router:27017
      database: analytics
      options: { readPreference: secondaryPreferred }
  tls:
    enabled: true
    keystore: ${TLS_KEYSTORE_PATH}
    keystore-password: ${TLS_KEYSTORE_PASSWORD}
    truststore: ${TLS_TRUSTSTORE_PATH}
    truststore-password: ${TLS_TRUSTSTORE_PASSWORD}
```

## 4. 典型接入（Spring Boot）
- JDBC：
```java
@Bean
public DataSource userDataSource(StorageClient storageClient) {
    return storageClient.getDataSource("userdb");
}
```

- Redis（Lettuce）：
```java
@Bean
public RedisConnectionFactory redisConnectionFactory(StorageClient client) {
    String uri = client.getRedisUri("cache");
    return new LettuceConnectionFactory(RedisURI.create(uri));
}
```

- Mongo：
```java
@Bean
public MongoClient mongoClient(StorageClient client) {
    String uri = client.getMongoUri("analytics");
    return MongoClients.create(uri);
}
```

## 5. 变更与回滚
- StorageClient 监听 Nacos 配置；变更时新连接生效、旧连接按池策略回收。
- 代理端滚动变更（PG/MySQL/Mongo/Redis 节点变更）对上游透明。
- 回滚依据 Nacos 历史版本；客户端仅需等待池内连接自然失效。

错误处理与退避（建议）
- 连接失败：快速失败并熔断 30–60s；定时半开探测恢复；
- 命令失败：只读类可按指数退避重试 1–2 次（幂等前提）；写入类禁止客户端重试；
- 刷新失败：维持旧配置，记录审计与指标；
- 指标：`storage.client.connect.errors`, `storage.client.exec.errors`, `storage.client.refresh.errors`。

## 6. 安全与观测
- mTLS/Token 限制来源服务；连接级指标（池、时延、错误率）上报 Micrometer/Prometheus。
- 敏感配置仅环境/KMS 注入；禁止打印明文。

Bean 命名（建议）
- `DataSource`：`dataSource_<logicalName>`（如 `dataSource_userdb`）
- `RedisConnectionFactory`：`redisCF_<namespace>`（如 `redisCF_cache`）
- `MongoClient`：`mongo_<logicalName>`（如 `mongo_analytics`）

指标命名（建议）
- 连接池：`storage.pool.active`、`storage.pool.idle`、`storage.pool.wait`（直方图）
- 执行耗时：`storage.exec.latency`（tag：db/operation）
- 错误率：`storage.exec.errors`（tag：db/type）

---

实现约束（最小）
- 使用 HikariCP/Lettuce/Mongo Java Driver；
- Nacos 监听使用命名空间/分组 + DataId 精确匹配；
- 刷新采用“先新后旧”策略，避免在用连接被强关；
- 仅在 base-model/common 实现，业务服务不重复造轮子。

---

备注：本文件定义“规范接口与键位”，建议在 `base-model/common` 提供默认实现；`admin` 仅引用接口以保持解耦。
