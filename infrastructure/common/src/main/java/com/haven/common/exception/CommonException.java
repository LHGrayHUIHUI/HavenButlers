package com.haven.common.exception;

import com.haven.base.common.exception.BaseException;
import com.haven.base.common.response.ErrorCode;

/**
 * Common模块通用异常
 */
public class CommonException extends BaseException {

    /**
     * Redis操作异常
     */
    public static class RedisException extends CommonException {
        public RedisException(String message) {
            super(ErrorCode.CACHE_ERROR.getCode(), message);
        }

        public RedisException(String message, Throwable cause) {
            super(ErrorCode.CACHE_ERROR.getCode(), message, cause);
        }
    }

    /**
     * 分布式锁异常
     */
    public static class LockException extends CommonException {
        public LockException(String message) {
            super(ErrorCode.SYSTEM_ERROR.getCode(), message);
        }

        public LockException(String message, Throwable cause) {
            super(ErrorCode.SYSTEM_ERROR.getCode(), message, cause);
        }
    }

    /**
     * 消息队列异常
     */
    public static class MqException extends CommonException {
        public MqException(String message) {
            super(ErrorCode.MESSAGE_QUEUE_ERROR.getCode(), message);
        }

        public MqException(String message, Throwable cause) {
            super(ErrorCode.MESSAGE_QUEUE_ERROR.getCode(), message, cause);
        }
    }

    /**
     * HTTP调用异常
     */
    public static class HttpException extends CommonException {
        public HttpException(String message) {
            super(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode(), message);
        }

        public HttpException(String message, Throwable cause) {
            super(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode(), message, cause);
        }
    }

    /**
     * 配置异常
     */
    public static class ConfigException extends CommonException {
        public ConfigException(String message) {
            super(ErrorCode.CONFIG_ERROR.getCode(), message);
        }

        public ConfigException(String message, Throwable cause) {
            super(ErrorCode.CONFIG_ERROR.getCode(), message, cause);
        }
    }

    /**
     * 构造函数
     */
    public CommonException(Integer code, String message) {
        super(code, message);
    }

    public CommonException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public CommonException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CommonException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}