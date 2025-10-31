package com.haven.base.resilience;

import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.config.ResilienceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 简化版容错服务
 * 提供基础的熔断、重试、超时控制功能
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleResilienceService {

    private final ResilienceProperties resilienceProperties;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * 带容错保护的执行方法
     *
     * @param serviceName 服务名称
     * @param supplier    业务逻辑
     * @param <T>         返回类型
     * @return 执行结果
     */
    @SuppressWarnings("unchecked")
    public <T> ResponseWrapper<T> executeWithResilience(String serviceName, Supplier<T> supplier) {
        try {
            // 基础超时控制
            if (resilienceProperties.getTimeLimiter().isEnabled()) {
                return (ResponseWrapper<T>) executeWithTimeout(serviceName, supplier);
            }

            // 基础重试机制
            if (resilienceProperties.getRetry().isEnabled()) {
                return (ResponseWrapper<T>) executeWithRetry(serviceName, supplier);
            }

            T result = supplier.get();
            return ResponseWrapper.success(result);

        } catch (Exception e) {
            return handleException(serviceName, e);
        }
    }

    /**
     * 异步执行带容错保护
     *
     * @param serviceName 服务名称
     * @param supplier    业务逻辑
     * @param <T>         返回类型
     * @return 异步结果
     */
    public <T> CompletableFuture<ResponseWrapper<T>> executeAsyncWithResilience(String serviceName, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> executeWithResilience(serviceName, supplier));
    }

    /**
     * 带超时控制的执行
     */
    private <T> ResponseWrapper<?> executeWithTimeout(String serviceName, Supplier<T> supplier) {
        try {
            long timeoutSeconds = resilienceProperties.getTimeLimiter().getTimeoutDuration();
            CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier);

            T result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            return ResponseWrapper.success(result);

        } catch (java.util.concurrent.TimeoutException e) {
            log.error("服务 {} 调用超时", serviceName);
            return ResponseWrapper.error(ErrorCode.SYSTEM_ERROR, "服务调用超时");
        } catch (Exception e) {
            log.error("服务 {} 调用失败: {}", serviceName, e.getMessage(), e);
            return ResponseWrapper.error(ErrorCode.SYSTEM_ERROR, "服务调用失败");
        }
    }

    /**
     * 带重试的执行
     */
    private <T> ResponseWrapper<?> executeWithRetry(String serviceName, Supplier<T> supplier) {
        ResilienceProperties.RetryConfig config = resilienceProperties.getRetry();
        int maxAttempts = config.getMaxAttempts();
        long waitDuration = config.getWaitDuration();

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("服务 {} 第 {} 次尝试调用", serviceName, attempt);
                T result = supplier.get();

                if (attempt > 1) {
                    log.info("服务 {} 重试成功，总尝试次数: {}", serviceName, attempt);
                }

                return ResponseWrapper.success(result);

            } catch (Exception e) {
                lastException = e;
                log.warn("服务 {} 第 {} 次尝试失败: {}", serviceName, attempt, e.getMessage());

                // 最后一次尝试，不再等待
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(waitDuration);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("服务 {} 重试失败，总尝试次数: {}", serviceName, maxAttempts);
        return handleException(serviceName, lastException);
    }

    /**
     * 处理异常
     */
    @SuppressWarnings("unchecked")
    private <T> ResponseWrapper<T> handleException(String serviceName, Exception e) {
        log.error("服务 {} 调用失败: {}", serviceName, e.getMessage(), e);

        if (e instanceof java.util.concurrent.TimeoutException) {
            return (ResponseWrapper<T>) ResponseWrapper.error(ErrorCode.SYSTEM_ERROR, "服务调用超时");
        } else if (e instanceof java.lang.IllegalArgumentException) {
            return (ResponseWrapper<T>) ResponseWrapper.error(ErrorCode.PARAM_ERROR, "参数错误: " + e.getMessage());
        } else {
            return (ResponseWrapper<T>) ResponseWrapper.error(ErrorCode.SYSTEM_ERROR, "系统异常: " + e.getMessage());
        }
    }

    /**
     * 基础熔断器状态跟踪
     */
    private final java.util.concurrent.ConcurrentHashMap<String, CircuitBreakerState> circuitBreakerStates =
        new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 简单的熔断器状态检查
     */
    private boolean isCircuitBreakerOpen(String serviceName) {
        if (!resilienceProperties.getCircuitBreaker().isEnabled()) {
            return false;
        }

        CircuitBreakerState state = circuitBreakerStates.computeIfAbsent(serviceName,
            k -> new CircuitBreakerState());

        return state.isOpen();
    }

    /**
     * 简单的熔断器内部状态类
     */
    private static class CircuitBreakerState {
        private volatile int failureCount = 0;
        private volatile long lastFailureTime = 0;
        private volatile boolean isOpen = false;

        private static final int FAILURE_THRESHOLD = 5;
        private static final long RECOVERY_TIMEOUT_MS = 60000; // 1分钟

        public boolean isOpen() {
            if (!isOpen) {
                return false;
            }

            // 检查是否可以尝试恢复
            if (System.currentTimeMillis() - lastFailureTime > RECOVERY_TIMEOUT_MS) {
                isOpen = false;
                failureCount = 0;
                return false;
            }

            return true;
        }

        public void recordSuccess() {
            failureCount = 0;
            isOpen = false;
        }

        public void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();

            if (failureCount >= FAILURE_THRESHOLD) {
                isOpen = true;
            }
        }
    }
}