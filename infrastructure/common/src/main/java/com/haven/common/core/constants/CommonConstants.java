package com.haven.common.core.constants;

/**
 * 公共常量定义
 *
 * @author HavenButler
 */
public final class CommonConstants {

    private CommonConstants() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * 系统常量
     */
    public static final class System {
        public static final String NAME = "HavenButler";
        public static final String VERSION = "1.0.0";
        public static final String DEFAULT_CHARSET = "UTF-8";
        public static final String DEFAULT_LOCALE = "zh_CN";
    }

    /**
     * HTTP请求头
     */
    public static final class Header {
        public static final String TRACE_ID = "X-Trace-ID";
        public static final String FAMILY_ID = "X-Family-ID";
        public static final String USER_ID = "X-User-ID";
        public static final String DEVICE_ID = "X-Device-ID";
        public static final String TOKEN = "X-Auth-Token";
        public static final String SIGNATURE = "X-Signature";
        public static final String TIMESTAMP = "X-Timestamp";
        public static final String NONCE = "X-Nonce";
        public static final String APP_VERSION = "X-App-Version";
        public static final String PLATFORM = "X-Platform";
        public static final String LANGUAGE = "Accept-Language";
    }

    /**
     * 缓存键前缀
     */
    public static final class Cache {
        public static final String PREFIX = "haven:";
        public static final String SESSION_PREFIX = PREFIX + "session:";
        public static final String USER_PREFIX = PREFIX + "user:";
        public static final String FAMILY_PREFIX = PREFIX + "family:";
        public static final String DEVICE_PREFIX = PREFIX + "device:";
        public static final String TOKEN_PREFIX = PREFIX + "token:";
        public static final String LOCK_PREFIX = PREFIX + "lock:";
        public static final String RATE_LIMIT_PREFIX = PREFIX + "rate:";
        public static final String VERIFY_CODE_PREFIX = PREFIX + "verify:";
        public static final String CONFIG_PREFIX = PREFIX + "config:";

        public static final long DEFAULT_TIMEOUT = 3600L;
        public static final long SESSION_TIMEOUT = 7200L;
        public static final long TOKEN_TIMEOUT = 86400L;
        public static final long VERIFY_CODE_TIMEOUT = 300L;
        public static final long LOCK_TIMEOUT = 30L;
    }

    /**
     * 消息队列
     */
    public static final class MQ {
        // 交换机
        public static final String EXCHANGE_DIRECT = "haven.direct";
        public static final String EXCHANGE_TOPIC = "haven.topic";
        public static final String EXCHANGE_FANOUT = "haven.fanout";
        public static final String EXCHANGE_DELAYED = "haven.delayed";

        // 队列
        public static final String QUEUE_DEVICE_EVENT = "device.event";
        public static final String QUEUE_USER_MESSAGE = "user.message";
        public static final String QUEUE_SYSTEM_LOG = "system.log";
        public static final String QUEUE_ALERT_NOTIFY = "alert.notify";

        // 路由键
        public static final String ROUTING_DEVICE = "device.*";
        public static final String ROUTING_USER = "user.*";
        public static final String ROUTING_SYSTEM = "system.*";
        public static final String ROUTING_ALERT = "alert.*";
    }

    /**
     * 线程池
     */
    public static final class ThreadPool {
        public static final int CORE_POOL_SIZE = 10;
        public static final int MAX_POOL_SIZE = 50;
        public static final int QUEUE_CAPACITY = 100;
        public static final int KEEP_ALIVE_SECONDS = 60;
        public static final String NAME_PREFIX = "haven-thread-";
    }

    /**
     * 安全相关
     */
    public static final class Security {
        public static final String ALGORITHM_AES = "AES";
        public static final String ALGORITHM_RSA = "RSA";
        public static final String ALGORITHM_HMAC = "HmacSHA256";
        public static final String ALGORITHM_MD5 = "MD5";
        public static final String ALGORITHM_SHA256 = "SHA-256";

        public static final int PASSWORD_MIN_LENGTH = 8;
        public static final int PASSWORD_MAX_LENGTH = 32;
        public static final int VERIFY_CODE_LENGTH = 6;
        public static final int TOKEN_LENGTH = 32;
    }

    /**
     * 分页常量
     */
    public static final class Page {
        public static final int DEFAULT_PAGE = 1;
        public static final int DEFAULT_SIZE = 20;
        public static final int MAX_SIZE = 100;
        public static final int MIN_SIZE = 1;
    }

    /**
     * 响应状态
     */
    public static final class Status {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAIL = "FAIL";
        public static final String ERROR = "ERROR";
        public static final String PENDING = "PENDING";
        public static final String PROCESSING = "PROCESSING";
    }
}