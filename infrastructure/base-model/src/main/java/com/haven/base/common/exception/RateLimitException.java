package com.haven.base.common.exception;

import com.haven.base.common.response.ErrorCode;

/**
 * 限流异常
 * 用于处理请求频率超限、配额不足等场景
 *
 * @author HavenButler
 */
public class RateLimitException extends BaseException {

    /**
     * 默认限流异常
     */
    public RateLimitException() {
        super(ErrorCode.RATE_LIMIT_ERROR, "请求过于频繁，请稍后重试");
    }

    /**
     * 自定义消息的限流异常
     */
    public RateLimitException(String message) {
        super(ErrorCode.RATE_LIMIT_ERROR, message);
    }

    /**
     * IP限流异常
     */
    public static RateLimitException ip(String ip, int limit, int windowSeconds) {
        return new RateLimitException(
            String.format("IP[%s]请求过于频繁，限制: %d次/%d秒", ip, limit, windowSeconds));
    }

    /**
     * 用户限流异常
     */
    public static RateLimitException user(String userId, int limit, int windowSeconds) {
        return new RateLimitException(
            String.format("用户[%s]请求过于频繁，限制: %d次/%d秒", userId, limit, windowSeconds));
    }

    /**
     * API限流异常
     */
    public static RateLimitException api(String apiPath, int limit, int windowSeconds) {
        return new RateLimitException(
            String.format("接口[%s]请求过于频繁，限制: %d次/%d秒", apiPath, limit, windowSeconds));
    }

    /**
     * 配额不足异常
     */
    public static RateLimitException quota(String resource, long used, long total) {
        return new RateLimitException(
            String.format("资源[%s]配额不足，已使用: %d/%d", resource, used, total));
    }
}