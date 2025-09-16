package com.haven.base.common.exception;

import com.haven.base.common.response.ErrorCode;

/**
 * 认证授权异常
 * 用于处理认证和授权相关的异常
 *
 * @author HavenButler
 */
public class AuthException extends BaseException {

    public AuthException(String message) {
        super(ErrorCode.UNAUTHORIZED.getCode(), message);
    }

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AuthException(ErrorCode errorCode, Object data) {
        super(errorCode, data);
    }
}