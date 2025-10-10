package com.haven.common.redis;

import com.haven.common.exception.CommonException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.TraceIdUtil;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式锁实现 - 基于base-model规范
 * 支持可重入、看门狗自动续期、指标收集
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model分布式锁规范
 */
@Slf4j
@Component
public class DistributedCommonLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${base-model.distributed-lock.key-prefix:haven:lock:}")
    private String lockPrefix;

    @Value("${base-model.distributed-lock.default-timeout:30}")
    private long defaultTimeout;

    @Value("${base-model.distributed-lock.auto-renew:true}")
    private boolean autoRenew;

    @Value("${base-model.distributed-lock.renew-interval:10}")
    private long renewInterval;

    @Value("${base-model.distributed-lock.watchdog-timeout:100}")
    private long watchdogTimeout;

    // 线程本地存储：可重入计数器
    private final ThreadLocal<Map<String, ReentrantLockInfo>> threadLocks =
        ThreadLocal.withInitial(ConcurrentHashMap::new);

    // 看门狗定时器
    private final ScheduledExecutorService watchdog =
        Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "DistributedLock-Watchdog");
            t.setDaemon(true);
            return t;
        });

    // 统计指标
    private final AtomicLong lockAcquired = new AtomicLong(0);
    private final AtomicLong lockFailed = new AtomicLong(0);
    private final AtomicLong lockWaiting = new AtomicLong(0);
    private final AtomicLong lockRenewed = new AtomicLong(0);

    // 可重入锁信息
    private static class ReentrantLockInfo {
        final String lockValue;
        final String lockKey;
        int count;
        ScheduledFuture<?> watchdogTask;

        ReentrantLockInfo(String lockKey, String lockValue) {
            this.lockKey = lockKey;
            this.lockValue = lockValue;
            this.count = 1;
        }
    }

    // Lua脚本：可重入加锁
    private static final String REENTRANT_LOCK_SCRIPT =
        "local key = KEYS[1] " +
        "local value = ARGV[1] " +
        "local ttl = tonumber(ARGV[2]) " +
        "local current = redis.call('get', key) " +
        "if current == false then " +
        "   redis.call('setex', key, ttl, value) " +
        "   return 1 " +
        "elseif current == value then " +
        "   redis.call('expire', key, ttl) " +
        "   return 1 " +
        "else " +
        "   return 0 " +
        "end";

    // Lua脚本：可重入释放锁
    private static final String REENTRANT_RELEASE_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "   return redis.call('del', KEYS[1]) " +
        "else " +
        "   return 0 " +
        "end";

    // Lua脚本：续期锁
    private static final String RENEW_LOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "   return redis.call('expire', KEYS[1], ARGV[2]) " +
        "else " +
        "   return 0 " +
        "end";

    /**
     * 尝试获取锁 - 支持可重入
     *
     * @param key     锁键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否成功获取锁
     */
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        return tryLock(key, timeout, unit, false);
    }

    /**
     * 尝试获取锁 - 默认超时时间
     */
    public boolean tryLock(String key) {
        return tryLock(key, defaultTimeout, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取锁 - 核心实现
     */
    private boolean tryLock(String key, long timeout, TimeUnit unit, boolean isWaiting) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("锁键不能为空");
        }

        String lockKey = buildLockKey(key);
        String traceId = TraceIdUtil.getCurrent();

        if (isWaiting) {
            lockWaiting.incrementAndGet();
        }

        try {
            Map<String, ReentrantLockInfo> threadLockMap = threadLocks.get();
            ReentrantLockInfo existingLock = threadLockMap.get(lockKey);

            // 检查可重入
            if (existingLock != null) {
                existingLock.count++;
                // 续期现有锁
                boolean renewed = renewLockInternal(existingLock.lockKey,
                    existingLock.lockValue, unit.toSeconds(timeout));
                if (renewed) {
                    log.debug("可重入获取锁成功: key={}, count={}, traceId={}",
                             lockKey, existingLock.count, traceId);
                    return true;
                } else {
                    // 续期失败，可能锁已过期，清理本地状态
                    threadLockMap.remove(lockKey);
                    stopWatchdog(existingLock);
                }
            }

            // 尝试获取新锁
            String lockValue = generateLockValue();
            boolean acquired = acquireLockWithScript(lockKey, lockValue, unit.toSeconds(timeout));

            if (acquired) {
                // 创建锁信息
                ReentrantLockInfo lockInfo = new ReentrantLockInfo(lockKey, lockValue);
                threadLockMap.put(lockKey, lockInfo);

                // 启动看门狗
                if (autoRenew) {
                    startWatchdog(lockInfo);
                }

                lockAcquired.incrementAndGet();
                log.debug("获取锁成功: key={}, value={}, traceId={}",
                         lockKey, lockValue, traceId);
                return true;
            } else {
                lockFailed.incrementAndGet();
                log.debug("获取锁失败: key={}, traceId={}", lockKey, traceId);
                return false;
            }

        } catch (Exception e) {
            lockFailed.incrementAndGet();
            log.error("获取锁异常: key={}, traceId={}", lockKey, traceId, e);
            throw new CommonException(ErrorCode.SYSTEM_ERROR.getCode(), "获取分布式锁失败", e);
        } finally {
            if (isWaiting) {
                lockWaiting.decrementAndGet();
            }
        }
    }

    /**
     * 获取锁（带重试）
     *
     * @param key         锁键
     * @param timeout     锁超时时间
     * @param unit        时间单位
     * @param maxRetries  最大重试次数
     * @param retryDelay  重试延迟（毫秒）
     * @return 是否成功获取锁
     */
    public boolean tryLockWithRetry(String key, long timeout, TimeUnit unit,
                                    int maxRetries, long retryDelay) {
        int retries = 0;
        while (retries < maxRetries) {
            boolean acquired = tryLock(key, timeout, unit, retries > 0);
            if (acquired) {
                return true;
            }

            retries++;
            if (retries < maxRetries) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("获取锁时被中断: key={}, traceId={}", key, TraceIdUtil.getCurrent());
                    return false;
                }
            }
        }

        log.warn("获取锁失败，已达最大重试次数: key={}, retries={}, traceId={}",
                key, maxRetries, TraceIdUtil.getCurrent());
        return false;
    }

    /**
     * 释放锁 - 支持可重入
     *
     * @param key 锁键
     * @return 是否成功
     */
    public boolean releaseLock(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("锁键不能为空");
        }

        String lockKey = buildLockKey(key);
        String traceId = TraceIdUtil.getCurrent();

        try {
            Map<String, ReentrantLockInfo> threadLockMap = threadLocks.get();
            ReentrantLockInfo lockInfo = threadLockMap.get(lockKey);

            if (lockInfo == null) {
                log.warn("尝试释放未持有的锁: key={}, traceId={}", lockKey, traceId);
                return false;
            }

            // 递减可重入计数
            lockInfo.count--;

            if (lockInfo.count > 0) {
                // 仍有重入层级，只续期不释放
                boolean renewed = renewLockInternal(lockInfo.lockKey,
                    lockInfo.lockValue, defaultTimeout);
                log.debug("可重入释放锁，剩余层级: key={}, count={}, traceId={}",
                         lockKey, lockInfo.count, traceId);
                return renewed;
            }

            // 最后一层，真正释放锁
            boolean released = releaseLockWithScript(lockInfo.lockKey, lockInfo.lockValue);

            if (released) {
                // 停止看门狗
                stopWatchdog(lockInfo);
                // 清理线程本地存储
                threadLockMap.remove(lockKey);
                log.debug("释放锁成功: key={}, value={}, traceId={}",
                         lockKey, lockInfo.lockValue, traceId);
                return true;
            } else {
                log.warn("释放锁失败: key={}, value={}, traceId={}",
                        lockKey, lockInfo.lockValue, traceId);
                return false;
            }

        } catch (Exception e) {
            log.error("释放锁异常: key={}, traceId={}", lockKey, traceId, e);
            throw new CommonException(ErrorCode.SYSTEM_ERROR.getCode(), "释放分布式锁失败", e);
        }
    }

    /**
     * 检查是否持有锁
     *
     * @param key 锁键
     * @return 是否持有
     */
    public boolean isLocked(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        String lockKey = buildLockKey(key);
        Map<String, ReentrantLockInfo> threadLockMap = threadLocks.get();
        ReentrantLockInfo lockInfo = threadLockMap.get(lockKey);

        if (lockInfo == null) {
            return false;
        }

        // 验证Redis中的锁是否仍然有效
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        boolean valid = lockInfo.lockValue.equals(currentValue);

        if (!valid) {
            // Redis中的锁已失效，清理本地状态
            threadLockMap.remove(lockKey);
            stopWatchdog(lockInfo);
        }

        return valid;
    }

    /**
     * 执行带锁的操作
     *
     * @param key      锁键
     * @param timeout  锁超时时间
     * @param unit     时间单位
     * @param runnable 要执行的操作
     * @return 是否执行成功
     */
    public boolean executeWithLock(String key, long timeout, TimeUnit unit,
                                   Runnable runnable) {
        boolean acquired = tryLock(key, timeout, unit);
        if (!acquired) {
            return false;
        }

        try {
            runnable.run();
            return true;
        } finally {
            releaseLock(key);
        }
    }

    /**
     * 执行带锁的操作（带返回值）
     *
     * @param key      锁键
     * @param timeout  锁超时时间
     * @param unit     时间单位
     * @param supplier 要执行的操作
     * @param <T>      返回类型
     * @return 执行结果
     */
    public <T> T executeWithLock(String key, long timeout, TimeUnit unit,
                                 java.util.function.Supplier<T> supplier) {
        boolean acquired = tryLock(key, timeout, unit);
        if (!acquired) {
            throw new CommonException(ErrorCode.SYSTEM_ERROR.getCode(),
                "获取分布式锁失败: " + key);
        }

        try {
            return supplier.get();
        } finally {
            releaseLock(key);
        }
    }

    /**
     * 获取锁统计信息
     */
    public Map<String, Long> getMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("lockAcquired", lockAcquired.get());
        metrics.put("lockFailed", lockFailed.get());
        metrics.put("lockWaiting", lockWaiting.get());
        metrics.put("lockRenewed", lockRenewed.get());
        return metrics;
    }

    /**
     * 关闭看门狗线程池
     */
    @PreDestroy
    public void destroy() {
        try {
            watchdog.shutdown();
            if (!watchdog.awaitTermination(5, TimeUnit.SECONDS)) {
                watchdog.shutdownNow();
            }
        } catch (InterruptedException e) {
            watchdog.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 构建锁键
     */
    private String buildLockKey(String key) {
        return lockPrefix + key;
    }

    /**
     * 生成锁值
     */
    private String generateLockValue() {
        return Thread.currentThread().getId() + ":" + UUID.randomUUID();
    }

    /**
     * 使用Lua脚本获取锁
     */
    private boolean acquireLockWithScript(String lockKey, String lockValue, long timeoutSeconds) {
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(REENTRANT_LOCK_SCRIPT, Long.class),
            Collections.singletonList(lockKey),
            lockValue,
            String.valueOf(timeoutSeconds)
        );
        return Long.valueOf(1).equals(result);
    }

    /**
     * 使用Lua脚本释放锁
     */
    private boolean releaseLockWithScript(String lockKey, String lockValue) {
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(REENTRANT_RELEASE_SCRIPT, Long.class),
            Collections.singletonList(lockKey),
            lockValue
        );
        return Long.valueOf(1).equals(result);
    }

    /**
     * 续期锁 - 内部方法
     */
    private boolean renewLockInternal(String lockKey, String lockValue, long timeoutSeconds) {
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(RENEW_LOCK_SCRIPT, Long.class),
            Collections.singletonList(lockKey),
            lockValue,
            String.valueOf(timeoutSeconds)
        );

        boolean success = Long.valueOf(1).equals(result);
        if (success) {
            lockRenewed.incrementAndGet();
        }
        return success;
    }

    /**
     * 启动看门狗
     */
    private void startWatchdog(ReentrantLockInfo lockInfo) {
        if (lockInfo.watchdogTask != null) {
            return; // 已经启动
        }

        lockInfo.watchdogTask = watchdog.scheduleAtFixedRate(() -> {
            try {
                boolean renewed = renewLockInternal(lockInfo.lockKey,
                    lockInfo.lockValue, defaultTimeout);
                if (!renewed) {
                    log.warn("看门狗续期失败，锁可能已过期: key={}", lockInfo.lockKey);
                    // 清理本地状态
                    Map<String, ReentrantLockInfo> threadLockMap = threadLocks.get();
                    threadLockMap.remove(lockInfo.lockKey);
                    stopWatchdog(lockInfo);
                } else {
                    log.debug("看门狗续期成功: key={}", lockInfo.lockKey);
                }
            } catch (Exception e) {
                log.error("看门狗续期异常: key={}", lockInfo.lockKey, e);
            }
        }, renewInterval, renewInterval, TimeUnit.SECONDS);
    }

    /**
     * 停止看门狗
     */
    private void stopWatchdog(ReentrantLockInfo lockInfo) {
        if (lockInfo.watchdogTask != null && !lockInfo.watchdogTask.isCancelled()) {
            lockInfo.watchdogTask.cancel(false);
            lockInfo.watchdogTask = null;
        }
    }
}