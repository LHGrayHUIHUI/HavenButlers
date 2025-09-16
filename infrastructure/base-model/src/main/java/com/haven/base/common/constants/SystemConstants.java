package com.haven.base.common.constants;

/**
 * 系统常量定义
 *
 * @author HavenButler
 */
public final class SystemConstants {

    private SystemConstants() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * 系统名称
     */
    public static final String SYSTEM_NAME = "HavenButler";

    /**
     * 系统版本
     */
    public static final String VERSION = "1.0.0";

    /**
     * TraceID相关常量
     */
    public static final class TraceId {
        public static final String PREFIX = "tr-";
        public static final String PATTERN = "yyyyMMdd-HHmmss";
        public static final String HEADER = "X-Trace-ID";
        public static final String MDC_KEY = "traceId";
        public static final int RANDOM_LENGTH = 6;
    }

    /**
     * HTTP请求头常量
     */
    public static final class Header {
        public static final String TRACE_ID = "X-Trace-ID";
        public static final String FAMILY_ID = "X-Family-ID";
        public static final String USER_ID = "X-User-ID";
        public static final String TOKEN = "X-Auth-Token";
        public static final String SIGNATURE = "X-Signature";
        public static final String TIMESTAMP = "X-Timestamp";
        public static final String DEVICE_ID = "X-Device-ID";
        public static final String APP_VERSION = "X-App-Version";
        public static final String PLATFORM = "X-Platform";
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

        public static final long DEFAULT_TIMEOUT = 3600L; // 默认过期时间（秒）
        public static final long SESSION_TIMEOUT = 7200L; // 会话过期时间（秒）
        public static final long TOKEN_TIMEOUT = 86400L;  // Token过期时间（秒）
    }

    /**
     * 分页常量
     */
    public static final class Page {
        public static final int DEFAULT_PAGE = 1;
        public static final int DEFAULT_SIZE = 20;
        public static final int MAX_SIZE = 100;
    }

    /**
     * 文件相关常量
     */
    public static final class File {
        public static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
        public static final String[] ALLOWED_IMAGE_TYPES = {
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
        };
        public static final String[] ALLOWED_VIDEO_TYPES = {
            "mp4", "avi", "mov", "wmv", "flv", "mkv"
        };
        public static final String[] ALLOWED_DOCUMENT_TYPES = {
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"
        };
    }

    /**
     * 设备相关常量
     */
    public static final class Device {
        public static final int MAX_DEVICES_PER_FAMILY = 50;
        public static final int MAX_DEVICES_PER_ROOM = 20;
        public static final long OFFLINE_TIMEOUT = 300L; // 5分钟无心跳判定离线
        public static final long HEARTBEAT_INTERVAL = 60L; // 心跳间隔（秒）
    }

    /**
     * 家庭相关常量
     */
    public static final class Family {
        public static final int MAX_MEMBERS = 20;
        public static final int MAX_ROOMS = 10;
        public static final int MAX_SCENES = 30;
    }

    /**
     * 安全相关常量
     */
    public static final class Security {
        public static final String ALGORITHM_AES = "AES";
        public static final String ALGORITHM_RSA = "RSA";
        public static final String ALGORITHM_HMAC = "HmacSHA256";
        public static final int PASSWORD_MIN_LENGTH = 8;
        public static final int PASSWORD_MAX_LENGTH = 32;
        public static final int MAX_LOGIN_ATTEMPTS = 5;
        public static final long ACCOUNT_LOCK_DURATION = 1800L; // 30分钟
    }
}