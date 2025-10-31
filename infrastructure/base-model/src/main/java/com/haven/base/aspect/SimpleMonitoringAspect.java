package com.haven.base.aspect;

import com.haven.base.annotation.TraceLog;
import com.haven.base.monitoring.SimpleMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 简化监控切面
 * 自动记录方法执行异常和性能指标
 *
 * @author HavenButler
 */
@Slf4j
@Aspect
@Component
public class SimpleMonitoringAspect {

    @Autowired
    private SimpleMonitoringService monitoringService;

    /**
     * 监控所有带有@TraceLog注解的方法
     */
    @Around("@annotation(traceLog)")
    public Object aroundTraceLog(ProceedingJoinPoint joinPoint, TraceLog traceLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String operation = className + "." + methodName;

        try {
            // 执行方法
            Object result = joinPoint.proceed();

            // 记录性能指标
            long duration = System.currentTimeMillis() - startTime;
            Map<String, String> tags = new HashMap<>();
            tags.put("method", methodName);
            tags.put("class", className);
            tags.put("module", traceLog.module());

            monitoringService.recordPerformance(operation, duration, tags);

            log.info("方法执行成功: {}, 耗时: {}ms", traceLog.value(), duration);
            return result;

        } catch (Exception e) {
            // 记录异常
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> context = new HashMap<>();
            context.put("method", methodName);
            context.put("class", className);
            context.put("module", traceLog.module());
            context.put("duration", duration);
            context.put("operation", operation);

            monitoringService.recordException(e, context);
            log.error("方法执行异常: {}, 错误: {}, 耗时: {}ms", traceLog.value(), e.getMessage(), duration);

            // 重新抛出异常
            throw e;
        }
    }

    /**
     * 监控所有Controller方法
     */
    @Around("execution(* com.haven..controller..*(..))")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String operation = "controller." + className + "." + methodName;

        try {
            Object result = joinPoint.proceed();

            // 记录Controller性能
            long duration = System.currentTimeMillis() - startTime;
            Map<String, String> tags = new HashMap<>();
            tags.put("type", "controller");
            tags.put("method", methodName);
            tags.put("class", className);

            monitoringService.recordPerformance(operation, duration, tags);

            return result;

        } catch (Exception e) {
            // 记录Controller异常
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> context = new HashMap<>();
            context.put("type", "controller");
            context.put("method", methodName);
            context.put("class", className);
            context.put("duration", duration);
            context.put("operation", operation);

            monitoringService.recordException(e, context);

            throw e;
        }
    }
}