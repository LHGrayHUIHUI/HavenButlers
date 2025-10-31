package com.haven.base.monitoring;

import com.haven.base.config.SentryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化版监控服务
 * 提供基础的异常记录和性能监控功能
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleMonitoringService {

    private final SentryProperties sentryProperties;

    // 用户上下文缓存
    private final ThreadLocal<UserContext> userContext = new ThreadLocal<>();

    // 自定义标签缓存
    private final Map<String, String> globalTags = new ConcurrentHashMap<>();

    /**
     * 记录异常
     *
     * @param exception 异常对象
     * @param context   上下文信息
     */
    public void recordException(Exception exception, Map<String, Object> context) {
        if (!sentryProperties.isEnabled()) {
            log.warn("Sentry监控未启用，异常仅记录到日志: {}", exception.getMessage());
            return;
        }

        try {
            if (shouldFilterException(exception)) {
                log.debug("异常被过滤: {}", exception.getClass().getName());
                return;
            }

            // 构建异常信息
            Map<String, Object> exceptionContext = new HashMap<>();
            exceptionContext.put("exception", exception.getClass().getName());
            exceptionContext.put("message", exception.getMessage());
            exceptionContext.put("stackTrace", getStackTrace(exception));

            if (context != null) {
                exceptionContext.putAll(context);
            }

            // 添加用户上下文
            UserContext userCtx = userContext.get();
            if (userCtx != null) {
                exceptionContext.put("userId", userCtx.getUserId());
                exceptionContext.put("username", userCtx.getUsername());
            }

            // 添加全局标签
            globalTags.forEach(exceptionContext::put);

            // 记录到日志
            log.error("监控异常记录: {} | 上下文: {}", exception.getMessage(), exceptionContext);

        } catch (Exception e) {
            log.error("记录监控异常失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录错误消息
     *
     * @param message 错误消息
     * @param context 上下文信息
     */
    public void recordError(String message, Map<String, Object> context) {
        if (!sentryProperties.isEnabled()) {
            log.warn("Sentry监控未启用，错误仅记录到日志: {}", message);
            return;
        }

        try {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("message", message);
            errorContext.put("level", "ERROR");

            if (context != null) {
                errorContext.putAll(context);
            }

            globalTags.forEach(errorContext::put);

            log.error("监控错误记录: {} | 上下文: {}", message, errorContext);

        } catch (Exception e) {
            log.error("记录监控错误失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录警告消息
     *
     * @param message 警告消息
     * @param context 上下文信息
     */
    public void recordWarning(String message, Map<String, Object> context) {
        if (!sentryProperties.isEnabled()) {
            log.warn("Sentry监控未启用，警告仅记录到日志: {}", message);
            return;
        }

        try {
            Map<String, Object> warningContext = new HashMap<>();
            warningContext.put("message", message);
            warningContext.put("level", "WARNING");

            if (context != null) {
                warningContext.putAll(context);
            }

            globalTags.forEach(warningContext::put);

            log.warn("监控警告记录: {} | 上下文: {}", message, warningContext);

        } catch (Exception e) {
            log.error("记录监控警告失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 设置用户上下文
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param email    邮箱
     */
    public void setUserContext(String userId, String username, String email) {
        UserContext context = new UserContext();
        context.setUserId(userId);
        context.setUsername(username);
        context.setEmail(email);
        userContext.set(context);

        log.debug("设置监控用户上下文: {}", userId);
    }

    /**
     * 清除用户上下文
     */
    public void clearUserContext() {
        userContext.remove();
        log.debug("清除监控用户上下文");
    }

    /**
     * 设置全局标签
     *
     * @param key   标签键
     * @param value 标签值
     */
    public void setGlobalTag(String key, String value) {
        globalTags.put(key, value);
        log.debug("设置监控全局标签: {} = {}", key, value);
    }

    /**
     * 添加自定义上下文
     *
     * @param key   上下文键
     * @param value 上下文值
     */
    public void setContext(String key, Object value) {
        log.debug("设置监控上下文: {} = {}", key, value);
    }

    /**
     * 记录性能指标
     *
     * @param operation 操作名称
     * @param duration  耗时（毫秒）
     * @param tags     标签
     */
    public void recordPerformance(String operation, long duration, Map<String, String> tags) {
        if (!sentryProperties.isEnabled() || !sentryProperties.getPerformance().isEnabled()) {
            return;
        }

        try {
            // 检查是否为慢操作
            long threshold = operation.startsWith("database") ?
                sentryProperties.getPerformance().getSlowQueryThreshold() :
                sentryProperties.getPerformance().getSlowRequestThreshold();

            if (duration > threshold) {
                Map<String, Object> context = new HashMap<>();
                context.put("operation", operation);
                context.put("duration", duration);
                context.put("threshold", threshold);
                context.put("level", "PERFORMANCE_WARNING");

                if (tags != null) {
                    context.putAll(tags);
                }

                recordWarning("性能告警: " + operation + " 耗时 " + duration + "ms", context);
            }

        } catch (Exception e) {
            log.error("记录性能指标失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 判断异常是否应该被过滤
     */
    private boolean shouldFilterException(Exception exception) {
        String exceptionName = exception.getClass().getName();

        // 检查排除列表
        for (String excluded : sentryProperties.getExceptionFilter().getExcludedExceptions()) {
            if (exceptionName.equals(excluded)) {
                return true;
            }
        }

        // 检查包含列表（如果配置了）
        if (sentryProperties.getExceptionFilter().getIncludedExceptions().length > 0) {
            for (String included : sentryProperties.getExceptionFilter().getIncludedExceptions()) {
                if (exceptionName.equals(included)) {
                    return false;
                }
            }
            return true; // 如果配置了包含列表但不在其中，则过滤
        }

        return false;
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception exception) {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            exception.printStackTrace(pw);
            String stackTrace = sw.toString();

            // 限制堆栈信息长度
            if (stackTrace.length() > 2000) {
                stackTrace = stackTrace.substring(0, 2000) + "...";
            }

            return stackTrace;
        } catch (Exception e) {
            return "Failed to get stack trace: " + e.getMessage();
        }
    }

    /**
     * 用户上下文内部类
     */
    private static class UserContext {
        private String userId;
        private String username;
        private String email;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}