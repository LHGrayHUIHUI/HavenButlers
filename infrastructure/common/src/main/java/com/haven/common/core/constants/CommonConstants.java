package com.haven.common.core.constants;

/**
 * Common模块专用常量定义 - 去重后版本
 * 移除与base-model重复的常量，仅保留Common模块特有常量
 *
 * @author HavenButler
 * @version 2.0.0 - 常量去重版本
 */
public final class CommonConstants {

    private CommonConstants() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * Common模块HTTP请求头 - 补充base-model未定义的
     */
    public static final class Header {
        // 认证相关请求头
        public static final String TOKEN = "X-Auth-Token";
        public static final String USER_ID = "X-User-ID";
        public static final String FAMILY_ID = "X-Family-ID";

        // Common模块特有的请求头
        public static final String APP_VERSION = "X-App-Version";
        public static final String PLATFORM = "X-Platform";
        public static final String LANGUAGE = "Accept-Language";
    }

    /**
     * Common模块缓存键前缀 - 业务特定前缀
     */
    public static final class Cache {
        // Common模块特有的缓存前缀
        public static final String VERIFY_CODE_PREFIX = "haven:verify:";
        public static final String CONFIG_PREFIX = "haven:config:";

        // Common模块特有的超时时间
        public static final long VERIFY_CODE_TIMEOUT = 300L;
    }

    /**
     * Common模块消息队列 - 业务特定队列
     */
    public static final class MQ {
        // Common模块特有的队列名称
        public static final String QUEUE_DEVICE_EVENT = "device.event";
        public static final String QUEUE_USER_MESSAGE = "user.message";
        public static final String QUEUE_SYSTEM_LOG = "system.log";
        public static final String QUEUE_ALERT_NOTIFY = "alert.notify";

        // Common模块特有的路由键
        public static final String ROUTING_DEVICE = "device.*";
        public static final String ROUTING_USER = "user.*";
        public static final String ROUTING_SYSTEM = "system.*";
        public static final String ROUTING_ALERT = "alert.*";
    }

    /**
     * Common模块线程池 - 默认配置
     */
    public static final class ThreadPool {
        public static final int CORE_POOL_SIZE = 10;
        public static final int MAX_POOL_SIZE = 50;
        public static final int QUEUE_CAPACITY = 100;
        public static final int KEEP_ALIVE_SECONDS = 60;
        public static final String NAME_PREFIX = "haven-thread-";
    }

    /**
     * Common模块安全相关 - 补充配置
     */
    public static final class Security {
        // 验证码和Token长度配置
        public static final int VERIFY_CODE_LENGTH = 6;
        public static final int TOKEN_LENGTH = 32;
    }

    /**
     * Common模块分页常量 - 默认配置
     */
    public static final class Page {
        public static final int DEFAULT_PAGE = 1;
        public static final int DEFAULT_SIZE = 20;
        public static final int MAX_SIZE = 100;
        public static final int MIN_SIZE = 1;
    }

    /**
     * Common模块特有状态
     */
    public static final class Status {
        public static final String PENDING = "PENDING";
        public static final String PROCESSING = "PROCESSING";
    }

    /**
     * 配置迁移提示 - 帮助开发者迁移到新配置
     */
    public static final class Migration {
        public static final String REDIS_CONFIG_HINT = "请将 common.redis.* 配置迁移到 spring.data.redis.* + base-model.cache.*";
        public static final String MQ_CONFIG_HINT = "请将 common.mq.* 配置迁移到 base-model.messaging.*";
        public static final String SECURITY_CONFIG_HINT = "请将 common.security.* 配置迁移到 base-model.security.*";
        public static final String MIGRATION_DOC_URL = "docs/configuration-migration.md";
    }
}