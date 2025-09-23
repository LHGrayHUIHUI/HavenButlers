package com.haven.base.logging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 日志系统异步配置
 * 为日志记录提供独立的线程池，避免阻塞业务线程
 *
 * @author HavenButler
 */
@Configuration
@EnableAsync
public class LogAsyncConfig {

    /**
     * 日志专用线程池
     * 特点：
     * 1. 独立线程池，不影响业务处理
     * 2. 队列较大，能缓存大量日志
     * 3. 超时处理，避免日志系统故障影响业务
     */
    @Bean("logExecutor")
    public Executor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：适中，保证基本处理能力
        executor.setCorePoolSize(5);

        // 最大线程数：较大，应对高峰期
        executor.setMaxPoolSize(20);

        // 队列容量：很大，避免日志丢失
        executor.setQueueCapacity(10000);

        // 线程名前缀
        executor.setThreadNamePrefix("LogAsync-");

        // 拒绝策略：调用者运行，确保日志不丢失
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // 线程空闲时间
        executor.setKeepAliveSeconds(60);

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待任务完成后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * 性能监控线程池
     * 用于收集性能指标，频率较高
     */
    @Bean("performanceExecutor")
    public Executor performanceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("Performance-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setKeepAliveSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * 安全事件线程池
     * 安全事件优先级高，需要及时处理
     */
    @Bean("securityExecutor")
    public Executor securityExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("Security-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(60);

        executor.initialize();
        return executor;
    }
}