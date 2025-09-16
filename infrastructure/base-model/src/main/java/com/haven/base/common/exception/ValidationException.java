package com.haven.base.common.exception;

import com.haven.base.common.response.ErrorCode;

/**
 * 参数校验异常
 * 用于处理参数校验相关的异常
 *
 * @author HavenButler
 */
public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ValidationException(String field, String message) {
        super(ErrorCode.PARAM_ERROR.getCode(),
              String.format("参数[%s]校验失败: %s", field, message));
    }

    public ValidationException(ErrorCode errorCode, Object data) {
        super(errorCode, data);
    }
}