package com.haven.base.common.exception;

import com.haven.base.common.response.ErrorCode;

/**
 * 系统异常
 * 用于处理系统级别的异常
 *
 * @author HavenButler
 */
public class SystemException extends BaseException {

    public SystemException(String message) {
        super(ErrorCode.SYSTEM_ERROR.getCode(), message);
    }

    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SystemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public SystemException(String message, Throwable cause) {
        super(ErrorCode.SYSTEM_ERROR.getCode(), message, null, cause);
    }
}