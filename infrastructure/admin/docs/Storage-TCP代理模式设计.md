# Storage - TCP 代理模式设计

> 目标：以“TCP 代理/路由”方式统一承接多数据库（Postgres/MySQL/MongoDB）与 Redis 访问，提供读写分离、连接池复用、高可用切换、鉴权与审计，避免业务服务直连物理节点。

## 1. 定位与收益
- 唯一数据出口：所有微服务通过 Storage 暴露的代理端点接入，无需感知底层拓扑变更。
- 连接收敛：集中池化与路由，减少上游连接数量与抖动。
- 读写分离/就近：按规则将读请求下发到从库或次优节点，写请求直达主库。
- 可观测与审计：统一采集连接/时延/错误率/慢查询，记录关键操作审计日志。

## 2. 数据面 vs 控制面
- 数据面（Data Plane）：原生协议（JDBC/Mongo/Redis）经代理端口传输，面向业务查询与命令执行。
- 控制面（Control Plane）：Nacos 配置、健康检查、指标暴露、审计管道；必要时仅 Admin/运维访问。

## 3. 组件与端口规划（容器示例）
| 协议 | 组件建议 | 服务名 | 端口 | 说明 |
|------|---------|--------|------|------|
| Postgres | Pgpool-II（读写分离）/PgBouncer+HAProxy | `storage-pg-proxy` | 5432 | 读写分离/连接池 |
| MySQL | ProxySQL | `storage-mysql-proxy` | 3306 | 读写分离/路由 |
| MongoDB | Mongos（分片/路由层） | `storage-mongo-router` | 27017 | ReadPreference 支持 |
| Redis | Twemproxy/原生 Cluster/Sentinel | `storage-redis-proxy` | 6379 | Cluster/Sentinel 自愈 |

容器网络：全部加入 `haven-network`；不对外暴露端口。

## 4. 路由与读写策略
- Postgres：推荐 Pgpool-II（`load_balance_mode`、`master_slave_mode`），或 PgBouncer + HAProxy（HA 但不读写分离）。
- MySQL：ProxySQL 基于 `mysql_query_rules` 做读写分离（按 `SELECT`/事务标记/注释路由）。
- MongoDB：通过 `readPreference=secondaryPreferred` 等参数控制读路由，写默认到 Primary。
- Redis：优先使用 Cluster；若 Sentinels，客户端感知主从切换或经代理重定向。

## 5. 鉴权与安全
- 网络边界：仅 `haven-network` 内可访问；如需额外隔离，可用 Docker 子网与防火墙策略。
- mTLS（建议）：
  - 代理校验客户端证书（颁发给受信服务）；
  - 应用侧配置 keystore/truststore（见 Nacos 键位 `storage.tls.*`）。
- 凭证管理：数据库/Redis 账户仅注入到代理；业务服务仅使用代理端的最小权限凭证。

## 6. 健康检查与高可用
- 健康检查：
  - 代理 → 后端节点：定时探测/慢查询采样；
  - Admin 聚合：将 DB/Redis/ObjectStorage 健康合并入 `actuator/health` 组件。
- 高可用：
  - 主从漂移/节点故障时，代理自动摘除异常节点，写路由到新主；
  - 配置侧用 Nacos 发布新拓扑，客户端（StorageClient）刷新连接。

## 7. Nacos 配置映射（storage-service.yml）
```yaml
storage:
  databases:
    - name: userdb
      type: postgres
      proxy-endpoint: storage-pg-proxy:5432
      database: userdb
      pool: { max-size: 50, min-idle: 5 }
      tls: { enabled: true }
    - name: analytics
      type: mongodb
      proxy-endpoint: storage-mongo-router:27017
      database: analytics
      options: { readPreference: secondaryPreferred }
  redis:
    mode: cluster
    proxy-endpoint: storage-redis-proxy:6379
    password: ${REDIS_PASSWORD}
  tls:
    enabled: true
    keystore: ${TLS_KEYSTORE_PATH}
    keystore-password: ${TLS_KEYSTORE_PASSWORD}
    truststore: ${TLS_TRUSTSTORE_PATH}
    truststore-password: ${TLS_TRUSTSTORE_PASSWORD}
```

## 8. 客户端接入（统一由 StorageClient 提供）
- JDBC：`DataSource ds = storageClient.getDataSource("userdb");`
- Mongo：`String uri = storageClient.getMongoUri("analytics");`
- Redis：`String uri = storageClient.getRedisUri("cache");`

细节见《StorageClient-统一规范(TCP代理模式).md》。

## 9. 观测与审计
- 指标：连接池活跃/空闲、请求时延 P50/P95/P99、错误率、慢查询数（按库/表/命令级别）。
- 日志：审计（操作者/操作/参数摘要/耗时/结果）、错误日志、慢查询日志（阈值可配）。

## 10. 失败恢复与回滚
- Nacos 回滚：通过历史版本回滚代理端点或池配置；
- 客户端刷新：StorageClient 监听变更，双通道策略新建连接，旧连接自然回收；
- 手动触发：暴露管理 API `/internal/storage/refresh`（仅内网、需鉴权）。

## 11. 风险与边界
- 读写分离一致性：强一致读取需走主（Header/连接属性）；写后读建议会话内同路由或加延迟检测。
- Proxy 单点：代理组件需至少双实例（Active/Active）并置于同网络；可由 DNS RR 或 Client LB 切流。
- 复杂事务：跨库分布式事务不建议；采用幂等、补偿与最终一致性模式。

---

参考：Nacos-统一配置与服务发现设计.md、StorageClient-统一规范(TCP代理模式).md、各数据库/代理官方文档。

