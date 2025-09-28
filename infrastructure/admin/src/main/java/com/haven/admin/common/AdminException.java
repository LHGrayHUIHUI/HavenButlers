package com.haven.admin.common;

/**
 * Admin服务业务异常类
 *
 * 替代base-model的BusinessException，专为Admin服务设计
 *
 * @author HavenButler
 */
public class AdminException extends RuntimeException {

    private int code;

    public AdminException(String message) {
        super(message);
        this.code = 500;
    }

    public AdminException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AdminException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}