package com.haven.base.common.exception;

import com.haven.base.common.response.ErrorCode;

/**
 * 业务异常
 * 用于处理业务逻辑相关的异常
 *
 * @author HavenButler
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(ErrorCode.BUSINESS_ERROR.getCode(), message);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, Object data) {
        super(errorCode, data);
    }

    public BusinessException(int code, String message) {
        super(code, message);
    }

    public BusinessException(int code, String message, Object data) {
        super(code, message, data);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public BusinessException(int code, String message, Object data, Throwable cause) {
        super(code, message, data, cause);
    }
}