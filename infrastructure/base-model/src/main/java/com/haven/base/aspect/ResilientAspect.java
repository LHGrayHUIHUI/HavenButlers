package com.haven.base.aspect;

import com.haven.base.annotation.Resilient;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.resilience.SimpleResilienceService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 容错保护切面
 * 处理@Resilient注解的方法调用
 *
 * @author HavenButler
 */
@Slf4j
@Aspect
@Component
public class ResilientAspect {

    @Autowired
    private SimpleResilienceService resilienceService;

    @Around("@annotation(resilient)")
    public Object around(ProceedingJoinPoint joinPoint, Resilient resilient) throws Throwable {
        String serviceName = getServiceName(joinPoint, resilient);

        try {
            // 获取方法签名
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 创建业务逻辑Supplier
            Supplier<Object> supplier = () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable t) {
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    } else {
                        throw new RuntimeException(t);
                    }
                }
            };

            // 根据配置决定是否异步执行
            if (resilient.async()) {
                CompletableFuture<ResponseWrapper<Object>> future =
                    resilienceService.executeAsyncWithResilience(serviceName, supplier);

                // 如果方法返回的是CompletableFuture，直接返回
                if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
                    return future;
                } else {
                    // 否则等待结果
                    return future.get();
                }
            } else {
                return resilienceService.executeWithResilience(serviceName, supplier);
            }

        } catch (Exception e) {
            log.error("容错处理失败，服务: {}, 方法: {}", serviceName, joinPoint.getSignature().getName(), e);
            throw e;
        }
    }

    /**
     * 获取服务名称
     */
    private String getServiceName(ProceedingJoinPoint joinPoint, Resilient resilient) {
        String serviceName = resilient.value();

        if (serviceName.isEmpty()) {
            // 使用类名作为服务名
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            serviceName = className + "." + methodName;
        }

        log.debug("服务 {} 执行容错保护", serviceName);
        return serviceName;
    }
}