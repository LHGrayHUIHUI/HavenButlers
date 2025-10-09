package com.haven.admin.storage;

import javax.sql.DataSource;
import java.util.Map;

/**
 * StorageClient - 统一存储代理客户端（TCP模式）接口规范
 *
 * 说明：
 * - 本接口仅作为“规范原型”放置于 admin 工程，实际落地建议在 base-model/common 提供可复用实现。
 * - 数据面走原生协议（JDBC/Mongo/Redis），不经 HTTP；Storage 以 TCP 代理/路由实现读写分离与高可用。
 * - 建议通过 Nacos 加载代理端点、凭证、TLS 策略，并支持动态刷新。
 */
public interface StorageClient {

    /**
     * 获取 JDBC DataSource（读写分离由代理负责）
     *
     * @param logicalName 逻辑库名（如 userdb）
     * @return 已初始化的数据源，内部基于代理 host:port 构造 JDBC URL
     */
    DataSource getDataSource(String logicalName);

    /**
     * 获取 Redis 连接 URI（或集群配置），建议配合 Lettuce/Jedis 使用
     *
     * @param namespace 命名空间（如 cache/session）
     * @return 标准 redis:// 或 rediss:// URI
     */
    String getRedisUri(String namespace);

    /**
     * 获取 Mongo 连接 URI（或 ClientSettings），建议配合 Mongo 驱动使用
     *
     * @param logicalName 逻辑库名（如 analytics）
     * @return 标准 mongodb:// 或 mongodb+srv:// URI
     */
    String getMongoUri(String logicalName);

    /**
     * 获取 TLS 配置（如启用 mTLS）
     *
     * @return TLS 相关配置（keystore/truststore 路径与密码等）
     */
    TlsConfig getTlsConfig();

    /**
     * 订阅配置变更（Nacos 更新后回调），由实现方保证回调幂等
     *
     * @param listener 配置变更监听器
     */
    void registerChangeListener(ConfigChangeListener listener);

    /**
     * 手动触发刷新（容错场景）
     */
    void refresh();

    /**
     * TLS 配置数据结构
     */
    class TlsConfig {
        public final boolean enabled;               // 是否启用 TLS/mTLS
        public final String keyStorePath;           // 客户端证书库路径
        public final String keyStorePassword;       // 客户端证书库密码
        public final String trustStorePath;         // 信任库路径
        public final String trustStorePassword;     // 信任库密码

        public TlsConfig(boolean enabled,
                         String keyStorePath,
                         String keyStorePassword,
                         String trustStorePath,
                         String trustStorePassword) {
            this.enabled = enabled;
            this.keyStorePath = keyStorePath;
            this.keyStorePassword = keyStorePassword;
            this.trustStorePath = trustStorePath;
            this.trustStorePassword = trustStorePassword;
        }
    }

    /**
     * 配置变更监听
     */
    interface ConfigChangeListener {
        /**
         * 当指定逻辑库/命名空间的端点、凭证或 TLS 配置发生变化时触发
         *
         * @param scope   作用域（logicalName/namespace）
         * @param details 变化详情（key-value）
         */
        void onChanged(String scope, Map<String, String> details);
    }
}

