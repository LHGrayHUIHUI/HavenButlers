package com.haven.base.aspect;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.JsonUtil;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 日志追踪切面
 * 拦截@TraceLog注解的方法，记录执行日志
 *
 * @author HavenButler
 */
@Slf4j
@Aspect
@Component
public class TraceLogAspect {

    @Around("@annotation(traceLog)")
    public Object around(ProceedingJoinPoint point, TraceLog traceLog) throws Throwable {
        // 获取TraceID
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        // 获取方法信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        // 记录开始日志
        long startTime = System.currentTimeMillis();
        logStart(traceLog, className, methodName, point.getArgs(), traceId);

        Object result = null;
        try {
            // 执行方法
            result = point.proceed();

            // 记录成功日志
            long duration = System.currentTimeMillis() - startTime;
            logSuccess(traceLog, className, methodName, result, duration, traceId);

            return result;
        } catch (Exception e) {
            // 记录异常日志
            long duration = System.currentTimeMillis() - startTime;
            logError(traceLog, className, methodName, e, duration, traceId);
            throw e;
        } finally {
            // 清理MDC
            if (traceId.equals(TraceIdUtil.getCurrent())) {
                TraceIdUtil.clear();
            }
        }
    }

    /**
     * 记录方法开始日志
     */
    private void logStart(TraceLog traceLog, String className, String methodName,
                          Object[] args, String traceId) {
        if (!traceLog.logParams()) {
            log.info("[{}] 开始执行: {}.{}, 操作: {}, TraceID: {}",
                    traceLog.module(), className, methodName,
                    traceLog.value(), traceId);
            return;
        }

        // 过滤敏感参数
        String params = formatParams(args, traceLog.ignoreParamIndexes());

        log.info("[{}] 开始执行: {}.{}, 操作: {}, 参数: {}, TraceID: {}",
                traceLog.module(), className, methodName,
                traceLog.value(), params, traceId);
    }

    /**
     * 记录方法成功日志
     */
    private void logSuccess(TraceLog traceLog, String className, String methodName,
                            Object result, long duration, String traceId) {
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(String.format("[%s] 执行成功: %s.%s, 操作: %s",
                traceLog.module(), className, methodName, traceLog.value()));

        if (traceLog.logTime()) {
            logMsg.append(String.format(", 耗时: %dms", duration));
        }

        if (traceLog.logResult() && result != null) {
            String resultStr = JsonUtil.toJson(result);
            if (resultStr != null && resultStr.length() > 500) {
                resultStr = resultStr.substring(0, 500) + "...";
            }
            logMsg.append(String.format(", 结果: %s", resultStr));
        }

        logMsg.append(String.format(", TraceID: %s", traceId));

        log.info(logMsg.toString());
    }

    /**
     * 记录方法异常日志
     */
    private void logError(TraceLog traceLog, String className, String methodName,
                         Exception e, long duration, String traceId) {
        log.error("[{}] 执行失败: {}.{}, 操作: {}, 耗时: {}ms, 异常: {}, TraceID: {}",
                traceLog.module(), className, methodName,
                traceLog.value(), duration, e.getMessage(), traceId, e);
    }

    /**
     * 格式化参数
     */
    private String formatParams(Object[] args, int[] ignoreIndexes) {
        if (args == null || args.length == 0) {
            return "无参数";
        }

        return IntStream.range(0, args.length)
                .mapToObj(i -> {
                    // 检查是否需要忽略
                    if (Arrays.stream(ignoreIndexes).anyMatch(idx -> idx == i)) {
                        return "******";
                    }

                    // 转换为JSON字符串
                    Object arg = args[i];
                    if (arg == null) {
                        return "null";
                    }

                    try {
                        String json = JsonUtil.toJson(arg);
                        if (json != null && json.length() > 200) {
                            json = json.substring(0, 200) + "...";
                        }
                        return json;
                    } catch (Exception e) {
                        return arg.toString();
                    }
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }
}