package com.haven.base.common.exception;

import com.haven.base.common.response.ErrorCode;
import lombok.Getter;
import org.slf4j.MDC;

/**
 * 基础异常类
 * 所有自定义异常的父类
 *
 * @author HavenButler
 */
@Getter
public class BaseException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 链路追踪ID
     */
    private final String traceId;

    /**
     * 额外数据
     */
    private final Object data;

    public BaseException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public BaseException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message, null);
    }

    public BaseException(ErrorCode errorCode, Object data) {
        this(errorCode.getCode(), errorCode.getMessage(), data);
    }

    public BaseException(int code, String message) {
        this(code, message, null);
    }

    public BaseException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get("traceId");
    }

    public BaseException(ErrorCode errorCode, Throwable cause) {
        this(errorCode.getCode(), errorCode.getMessage(), null, cause);
    }

    public BaseException(int code, String message, Object data, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get("traceId");
    }
}