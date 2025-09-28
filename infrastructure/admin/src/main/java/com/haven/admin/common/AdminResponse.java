package com.haven.admin.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Admin服务统一响应包装类
 *
 * 替代base-model的ResponseWrapper，专为Admin服务设计
 * 支持Spring Boot Admin和API调用的响应格式
 *
 * @author HavenButler
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminResponse<T> {

    private int code;
    private String message;
    private T data;
    private String timestamp;
    private boolean success;

    public AdminResponse() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public AdminResponse(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = code == 200;
    }

    // 成功响应
    public static <T> AdminResponse<T> success(T data) {
        return new AdminResponse<>(200, "操作成功", data);
    }

    public static <T> AdminResponse<T> success(String message, T data) {
        return new AdminResponse<>(200, message, data);
    }

    // 无参数成功响应（仅返回成功状态）
    public static AdminResponse<Void> success() {
        return new AdminResponse<>(200, "操作成功", null);
    }

    // 错误响应
    public static <T> AdminResponse<T> error(int code, String message) {
        return new AdminResponse<>(code, message, null);
    }

    public static <T> AdminResponse<T> error(int code, String message, T data) {
        return new AdminResponse<>(code, message, data);
    }

    public static <T> AdminResponse<T> error(String message) {
        return new AdminResponse<>(500, message, null);
    }

    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}