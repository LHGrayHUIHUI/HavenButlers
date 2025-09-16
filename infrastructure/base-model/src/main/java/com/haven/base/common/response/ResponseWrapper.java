package com.haven.base.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应包装器
 * 所有API响应必须使用此格式
 *
 * @param <T> 响应数据类型
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseWrapper<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码：0表示成功，其他表示失败
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 响应时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 成功响应（无数据）
     */
    public static ResponseWrapper<Void> success() {
        return success(null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ResponseWrapper<T> success(T data) {
        return success("操作成功", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> ResponseWrapper<T> success(String message, T data) {
        return ResponseWrapper.<T>builder()
                .code(0)
                .message(message)
                .data(data)
                .traceId(MDC.get("traceId"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 失败响应（错误码）
     */
    public static ResponseWrapper<?> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 失败响应（错误码和自定义消息）
     */
    public static ResponseWrapper<?> error(ErrorCode errorCode, String message) {
        return error(errorCode.getCode(), message);
    }

    /**
     * 失败响应（自定义错误码和消息）
     */
    public static ResponseWrapper<?> error(int code, String message) {
        return ResponseWrapper.builder()
                .code(code)
                .message(message)
                .traceId(MDC.get("traceId"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 失败响应（带详细数据）
     */
    public static <T> ResponseWrapper<T> error(int code, String message, T data) {
        return ResponseWrapper.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .traceId(MDC.get("traceId"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return this.code == 0;
    }
}