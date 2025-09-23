package com.haven.base.logging.aspect;

import com.haven.base.logging.annotation.LogOperation;
import com.haven.base.logging.client.LogClient;
import com.haven.base.logging.model.LogEvent;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * 日志切面
 * 自动处理@LogOperation注解的方法，记录操作日志
 *
 * @author HavenButler
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private LogClient logClient;

    /**
     * 环绕通知：处理@LogOperation注解
     */
    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint point, LogOperation logOperation) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = point.getSignature().getName();
        String className = point.getTarget().getClass().getSimpleName();

        // 获取请求信息
        RequestInfo requestInfo = getRequestInfo();

        // 构建操作描述
        String description = logOperation.description().isEmpty() ?
                className + "." + methodName : logOperation.description();

        try {
            // 执行原方法
            Object result = point.proceed();

            // 记录成功日志
            recordSuccessLog(logOperation, requestInfo, description, startTime, point.getArgs(), result);

            return result;

        } catch (Throwable throwable) {
            // 记录失败日志
            if (logOperation.logOnError()) {
                recordErrorLog(logOperation, requestInfo, description, startTime, throwable);
            }
            throw throwable;
        }
    }

    /**
     * 记录成功日志
     */
    private void recordSuccessLog(LogOperation logOperation, RequestInfo requestInfo,
                                 String description, long startTime, Object[] args, Object result) {
        try {
            CompletableFuture<Void> logFuture = null;

            switch (logOperation.logType()) {
                case OPERATION:
                    if (logOperation.recordExecutionTime()) {
                        logFuture = logClient.logOperationWithTiming(
                                requestInfo.familyId, requestInfo.userId,
                                logOperation.operationType(), description, startTime);
                    } else {
                        logFuture = logClient.logOperation(
                                requestInfo.familyId, requestInfo.userId,
                                logOperation.operationType(), description);
                    }
                    break;

                case SECURITY:
                    logFuture = logClient.logSecurityEvent(
                            requestInfo.familyId, requestInfo.clientIP,
                            logOperation.operationType(), logOperation.riskLevel(), description);
                    break;

                case PERFORMANCE:
                    String metricName = logOperation.metricName().isEmpty() ?
                            "response_time_" + logOperation.operationType() : logOperation.metricName();
                    double metricValue = System.currentTimeMillis() - startTime;
                    logFuture = logClient.logPerformanceMetric(metricName, metricValue, logOperation.metricUnit());
                    break;

                case BUSINESS:
                    logFuture = logClient.logBusiness(
                            requestInfo.familyId, requestInfo.userId,
                            logOperation.businessModule(), logOperation.businessScenario(), description);
                    break;

                case SYSTEM:
                    // 系统日志暂时使用操作日志
                    logFuture = logClient.logOperation(
                            requestInfo.familyId, requestInfo.userId,
                            logOperation.operationType(), description);
                    break;

                case ERROR:
                    // 成功情况下不记录错误日志
                    break;
            }

            // 记录详细信息（如果启用）
            if (logFuture != null) {
                if (logOperation.recordParams() || logOperation.recordResult()) {
                    logFuture = logFuture.thenCompose(v -> recordDetailedInfo(
                            logOperation, requestInfo, args, result));
                }

                // 同步等待（如果不是异步模式）
                if (!logOperation.async()) {
                    logFuture.get();
                }
            }

        } catch (Exception e) {
            log.error("记录成功日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录失败日志
     */
    private void recordErrorLog(LogOperation logOperation, RequestInfo requestInfo,
                               String description, long startTime, Throwable throwable) {
        try {
            // 记录错误日志
            CompletableFuture<Void> errorLogFuture = logClient.logError(
                    requestInfo.familyId, requestInfo.userId,
                    logOperation.operationType(),
                    throwable.getClass().getSimpleName(),
                    throwable.getMessage());

            // 如果是安全相关的错误，同时记录安全日志
            if (logOperation.logType() == LogEvent.LogType.SECURITY ||
                isSecurityRelatedError(throwable)) {
                CompletableFuture<Void> securityLogFuture = logClient.logSecurityEvent(
                        requestInfo.familyId, requestInfo.clientIP,
                        "ERROR_" + logOperation.operationType(),
                        LogEvent.RiskLevel.HIGH,
                        "操作失败: " + description + ", 错误: " + throwable.getMessage());

                errorLogFuture = CompletableFuture.allOf(errorLogFuture, securityLogFuture);
            }

            // 同步等待（如果不是异步模式）
            if (!logOperation.async()) {
                errorLogFuture.get();
            }

        } catch (Exception e) {
            log.error("记录错误日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录详细信息
     */
    private CompletableFuture<Void> recordDetailedInfo(LogOperation logOperation, RequestInfo requestInfo,
                                                      Object[] args, Object result) {
        return CompletableFuture.runAsync(() -> {
            try {
                LogEvent detailEvent = LogEvent.builder()
                        .logType(LogEvent.LogType.OPERATION)
                        .serviceName(logClient.getCurrentServiceName())
                        .familyId(requestInfo.familyId)
                        .userId(requestInfo.userId)
                        .clientIP(requestInfo.clientIP)
                        .operationType(logOperation.operationType() + "_DETAIL")
                        .operationDescription("详细信息记录")
                        .traceId(TraceIdUtil.getCurrentOrGenerate())
                        .build();

                // 记录参数
                if (logOperation.recordParams() && args != null && args.length > 0) {
                    String paramsInfo = sanitizeParams(Arrays.toString(args));
                    detailEvent.addMetadata("params", paramsInfo);
                }

                // 记录返回值
                if (logOperation.recordResult() && result != null) {
                    String resultInfo = sanitizeResult(result.toString());
                    detailEvent.addMetadata("result", resultInfo);
                }

                // 发送详细日志
                logClient.sendCustomLog(detailEvent, "operation");

            } catch (Exception e) {
                log.error("记录详细信息失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 获取请求信息
     */
    private RequestInfo getRequestInfo() {
        RequestInfo info = new RequestInfo();

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // 获取客户端IP
                info.clientIP = getClientIP(request);

                // 获取用户ID（从Header或Token中）
                info.userId = request.getHeader("User-ID");
                if (info.userId == null) {
                    info.userId = request.getHeader("X-User-ID");
                }

                // 获取家庭ID
                info.familyId = request.getHeader("Family-ID");
                if (info.familyId == null) {
                    info.familyId = request.getHeader("X-Family-ID");
                }

                // 获取TraceId
                info.traceId = request.getHeader("Trace-ID");
                if (info.traceId == null) {
                    info.traceId = TraceIdUtil.getCurrentOrGenerate();
                }
            }
        } catch (Exception e) {
            log.debug("获取请求信息失败: {}", e.getMessage());
        }

        // 设置默认值
        if (info.clientIP == null) info.clientIP = "unknown";
        if (info.userId == null) info.userId = "system";
        if (info.familyId == null) info.familyId = "default";
        if (info.traceId == null) info.traceId = TraceIdUtil.getCurrentOrGenerate();

        return info;
    }

    /**
     * 获取真实客户端IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 判断是否为安全相关错误
     */
    private boolean isSecurityRelatedError(Throwable throwable) {
        String errorClass = throwable.getClass().getSimpleName().toLowerCase();
        return errorClass.contains("security") ||
               errorClass.contains("auth") ||
               errorClass.contains("access") ||
               errorClass.contains("permission") ||
               errorClass.contains("forbidden");
    }

    /**
     * 脱敏参数信息
     */
    private String sanitizeParams(String params) {
        if (params == null) return "";

        // 移除敏感信息
        return params.replaceAll("password=[^,\\]]+", "password=***")
                    .replaceAll("token=[^,\\]]+", "token=***")
                    .replaceAll("key=[^,\\]]+", "key=***")
                    .replaceAll("secret=[^,\\]]+", "secret=***");
    }

    /**
     * 脱敏返回值信息
     */
    private String sanitizeResult(String result) {
        if (result == null) return "";

        // 限制长度，避免日志过长
        if (result.length() > 1000) {
            result = result.substring(0, 1000) + "...";
        }

        return sanitizeParams(result);
    }

    /**
     * 请求信息封装类
     */
    private static class RequestInfo {
        String familyId;
        String userId;
        String clientIP;
        String traceId;
    }
}