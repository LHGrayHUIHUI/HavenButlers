package com.haven.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果模型
 *
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 成功响应
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success(String message) {
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> failure(String message) {
        return Result.<T>builder()
                .code(500)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应（自定义错误码）
     */
    public static <T> Result<T> failure(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 参数错误响应
     */
    public static <T> Result<T> badRequest(String message) {
        return Result.<T>builder()
                .code(400)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 未授权响应
     */
    public static <T> Result<T> unauthorized(String message) {
        return Result.<T>builder()
                .code(401)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 禁止访问响应
     */
    public static <T> Result<T> forbidden(String message) {
        return Result.<T>builder()
                .code(403)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 资源未找到响应
     */
    public static <T> Result<T> notFound(String message) {
        return Result.<T>builder()
                .code(404)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}