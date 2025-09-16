package com.haven.common.utils;

import com.haven.common.core.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 线程池工具类
 * 提供各种线程池的创建和管理
 *
 * @author HavenButler
 */
@Slf4j
public final class ThreadPoolUtils {

    private static final ConcurrentHashMap<String, ExecutorService> POOL_MAP = new ConcurrentHashMap<>();

    private ThreadPoolUtils() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * 创建固定大小的线程池
     */
    public static ExecutorService createFixedThreadPool(String name, int nThreads) {
        return POOL_MAP.computeIfAbsent(name, k ->
            new ThreadPoolExecutor(
                nThreads,
                nThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory(name),
                new ThreadPoolExecutor.CallerRunsPolicy()
            )
        );
    }

    /**
     * 创建缓存线程池
     */
    public static ExecutorService createCachedThreadPool(String name) {
        return POOL_MAP.computeIfAbsent(name, k ->
            new ThreadPoolExecutor(
                0,
                Integer.MAX_VALUE,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory(name),
                new ThreadPoolExecutor.CallerRunsPolicy()
            )
        );
    }

    /**
     * 创建单线程池
     */
    public static ExecutorService createSingleThreadExecutor(String name) {
        return POOL_MAP.computeIfAbsent(name, k ->
            new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory(name),
                new ThreadPoolExecutor.CallerRunsPolicy()
            )
        );
    }

    /**
     * 创建定时任务线程池
     */
    public static ScheduledExecutorService createScheduledThreadPool(String name, int corePoolSize) {
        return (ScheduledExecutorService) POOL_MAP.computeIfAbsent(name, k ->
            Executors.newScheduledThreadPool(corePoolSize, new NamedThreadFactory(name))
        );
    }

    /**
     * 创建自定义线程池
     */
    public static ExecutorService createCustomThreadPool(String name,
                                                         int corePoolSize,
                                                         int maximumPoolSize,
                                                         long keepAliveTime,
                                                         TimeUnit unit,
                                                         int queueCapacity) {
        return POOL_MAP.computeIfAbsent(name, k ->
            new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                new LinkedBlockingQueue<>(queueCapacity),
                new NamedThreadFactory(name),
                new ThreadPoolExecutor.CallerRunsPolicy()
            )
        );
    }

    /**
     * 创建默认线程池
     */
    public static ExecutorService createDefaultThreadPool(String name) {
        return createCustomThreadPool(
            name,
            CommonConstants.ThreadPool.CORE_POOL_SIZE,
            CommonConstants.ThreadPool.MAX_POOL_SIZE,
            CommonConstants.ThreadPool.KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            CommonConstants.ThreadPool.QUEUE_CAPACITY
        );
    }

    /**
     * 获取线程池
     */
    public static ExecutorService getThreadPool(String name) {
        return POOL_MAP.get(name);
    }

    /**
     * 关闭线程池
     */
    public static void shutdown(String name) {
        ExecutorService pool = POOL_MAP.remove(name);
        if (pool != null) {
            shutdownExecutor(pool, name);
        }
    }

    /**
     * 关闭所有线程池
     */
    public static void shutdownAll() {
        POOL_MAP.forEach((name, pool) -> shutdownExecutor(pool, name));
        POOL_MAP.clear();
    }

    /**
     * 安全关闭线程池
     */
    private static void shutdownExecutor(ExecutorService executor, String name) {
        log.info("关闭线程池: {}", name);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("线程池 {} 未能在60秒内正常关闭，强制关闭", name);
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("线程池 {} 未能关闭", name);
                }
            }
        } catch (InterruptedException e) {
            log.error("关闭线程池 {} 时被中断", name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 执行异步任务
     */
    public static CompletableFuture<Void> executeAsync(String poolName, Runnable task) {
        ExecutorService executor = getThreadPool(poolName);
        if (executor == null) {
            executor = createDefaultThreadPool(poolName);
        }
        return CompletableFuture.runAsync(task, executor);
    }

    /**
     * 执行异步任务（带返回值）
     */
    public static <T> CompletableFuture<T> submitAsync(String poolName, Callable<T> task) {
        ExecutorService executor = getThreadPool(poolName);
        if (executor == null) {
            executor = createDefaultThreadPool(poolName);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /**
     * 命名线程工厂
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private int counter = 0;

        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = defaultFactory.newThread(r);
            thread.setName(namePrefix + "-" + counter++);
            return thread;
        }
    }

    /**
     * 获取线程池状态信息
     */
    public static String getPoolStatus(String name) {
        ExecutorService executor = POOL_MAP.get(name);
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
            return String.format(
                "线程池 %s 状态: 核心线程数=%d, 最大线程数=%d, 当前线程数=%d, " +
                "活跃线程数=%d, 已完成任务数=%d, 任务队列大小=%d",
                name,
                pool.getCorePoolSize(),
                pool.getMaximumPoolSize(),
                pool.getPoolSize(),
                pool.getActiveCount(),
                pool.getCompletedTaskCount(),
                pool.getQueue().size()
            );
        }
        return "线程池 " + name + " 不存在或不是ThreadPoolExecutor类型";
    }
}