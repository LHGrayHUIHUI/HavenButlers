package com.haven.base.lock;

import com.haven.base.cache.RedisUtils;
import com.haven.base.common.exception.BaseException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.TraceIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Redis分布式锁实现（从common模块迁移和精简）
 * 支持可重入、看门狗自动续期、指标收集
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock implements DistributedLock {
    private final RedisUtils redisUtils;
    private final StringRedisTemplate redisTemplate;

    @Value("${base-model.distributed-lock.key-prefix:haven:lock:}")
    private String lockKeyPrefix;

    @Value("${base-model.distributed-lock.default-timeout:30}")
    private long defaultTimeout;

    @Value("${base-model.distributed-lock.watchdog-interval:10}")
    private long watchdogInterval;

    @Value("${base-model.distributed-lock.max-retry-times:3}")
    private int maxRetryTimes;

    private final Map<String, LockInfo> lockInfoMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchdogExecutor = Executors.newScheduledThreadPool(1);
    private final AtomicLong lockAcquiredCount = new AtomicLong(0);
    private final AtomicLong lockReleasedCount = new AtomicLong(0);
    private final AtomicLong lockRenewedCount = new AtomicLong(0);
    private final AtomicLong lockFailedCount = new AtomicLong(0);

    @Override
    public boolean tryLock(String lockKey, Duration timeout) {
        String traceId = TraceIdUtil.getCurrent();
        String fullLockKey = buildLockKey(lockKey);
        String lockValue = UUID.randomUUID().toString();

        long timeoutSeconds = timeout.toSeconds();
        long endTime = System.currentTimeMillis() + timeout.toMillis();

        int retryCount = 0;
        while (System.currentTimeMillis() < endTime) {
            try {
                Boolean success = redisUtils.tryLock(fullLockKey, lockValue, timeoutSeconds);
                if (Boolean.TRUE.equals(success)) {
                    // 锁获取成功，记录锁信息并启动看门狗
                    LockInfo lockInfo = new LockInfo(lockKey, lockValue, timeout.toMillis(), System.currentTimeMillis());
                    lockInfoMap.put(fullLockKey, lockInfo);
                    startWatchdog(fullLockKey, lockInfo);

                    lockAcquiredCount.incrementAndGet();
                    log.info("分布式锁获取成功: key={}, value={}, timeout={}s, traceId={}",
                            fullLockKey, lockValue, timeoutSeconds, traceId);
                    return true;
                }

                retryCount++;
                if (retryCount >= maxRetryTimes) {
                    log.warn("分布式锁获取失败，已达最大重试次数: key={}, retry={}, traceId={}",
                            fullLockKey, retryCount, traceId);
                    break;
                }

                // 等待一段时间后重试
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("分布式锁获取被中断: key={}, traceId={}", fullLockKey, traceId);
                return false;
            } catch (Exception e) {
                log.error("分布式锁获取异常: key={}, traceId={}, error={}",
                        fullLockKey, traceId, e.getMessage());
            }
        }

        lockFailedCount.incrementAndGet();
        return false;
    }

    @Override
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, Duration.ofSeconds(defaultTimeout));
    }

    @Override
    public boolean unlock(String lockKey) {
        String traceId = TraceIdUtil.getCurrent();
        String fullLockKey = buildLockKey(lockKey);
        LockInfo lockInfo = lockInfoMap.get(fullLockKey);

        if (lockInfo == null) {
            log.warn("分布式锁释放失败，锁不存在: key={}, traceId={}", fullLockKey, traceId);
            return false;
        }

        String lockValue = lockInfo.getOwner();

        try {
            Boolean success = redisUtils.releaseLock(fullLockKey, lockValue);
            if (Boolean.TRUE.equals(success)) {
                // 移除锁信息和停止看门狗
                lockInfoMap.remove(fullLockKey);
                stopWatchdog(fullLockKey);

                lockReleasedCount.incrementAndGet();
                log.info("分布式锁释放成功: key={}, value={}, traceId={}", fullLockKey, lockValue, traceId);
                return true;
            } else {
                log.warn("分布式锁释放失败，可能锁已过期或不是持有者: key={}, value={}, traceId={}",
                        fullLockKey, lockValue, traceId);
                return false;
            }
        } catch (Exception e) {
            log.error("分布式锁释放异常: key={}, value={}, traceId={}, error={}",
                    fullLockKey, lockValue, traceId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean forceUnlock(String lockKey) {
        String traceId = TraceIdUtil.getCurrent();
        String fullLockKey = buildLockKey(lockKey);

        try {
            redisUtils.delete(fullLockKey);
            lockInfoMap.remove(fullLockKey);
            stopWatchdog(fullLockKey);
            log.warn("强制释放分布式锁: key={}, traceId={}", fullLockKey, traceId);
            return true;
        } catch (Exception e) {
            log.error("强制释放分布式锁异常: key={}, traceId={}, error={}",
                    fullLockKey, traceId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        String fullLockKey = buildLockKey(lockKey);
        return redisUtils.hasKey(lockKey);
    }

    @Override
    public long getLockExpire(String lockKey) {
        String fullLockKey = buildLockKey(lockKey);
        return redisUtils.getExpire(lockKey);
    }

    @Override
    public boolean renewLock(String lockKey, Duration additionalTime) {
        String traceId = TraceIdUtil.getCurrent();
        String fullLockKey = buildLockKey(lockKey);
        LockInfo lockInfo = lockInfoMap.get(fullLockKey);

        if (lockInfo == null) {
            log.warn("分布式锁续租失败，锁不存在: key={}, traceId={}", fullLockKey, traceId);
            return false;
        }

        try {
            redisUtils.expire(lockKey, additionalTime.getSeconds(), TimeUnit.SECONDS);
            lockInfo.setExpireTime(System.currentTimeMillis() + additionalTime.toMillis());
            lockRenewedCount.incrementAndGet();
            log.debug("分布式锁续租成功: key={}, additionalTime={}", fullLockKey, additionalTime);
            return true;
        } catch (Exception e) {
            log.error("分布式锁续租异常: key={}, traceId={}, error={}",
                    fullLockKey, traceId, e.getMessage());
            return false;
        }
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> action) throws LockException {
        if (!tryLock(lockKey, timeout)) {
            throw LockException.timeout(lockKey, timeout.toMillis());
        }

        try {
            T result = action.get();
            log.debug("带锁操作执行成功: key={}", lockKey);
            return result;
        } catch (Exception e) {
            log.error("带锁操作执行失败: key={}, error={}", lockKey, e.getMessage(), e);
            throw e;
        } finally {
            try {
                unlock(lockKey);
            } catch (Exception e) {
                log.error("释放锁异常: key={}, error={}", lockKey, e.getMessage(), e);
            }
        }
    }

    @Override
    public void executeWithLock(String lockKey, Duration timeout, Runnable action) throws LockException {
        executeWithLock(lockKey, timeout, () -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action) throws LockException {
        return executeWithLock(lockKey, Duration.ofSeconds(defaultTimeout), action);
    }

    @Override
    public long unlockByPattern(String pattern) {
        String fullPattern = lockKeyPrefix + pattern;
        try {
            long count = redisUtils.deleteByPattern(pattern);
            log.info("批量释放锁: pattern={}, count={}", fullPattern, count);
            return count;
        } catch (Exception e) {
            log.error("批量释放锁异常: pattern={}, error={}", fullPattern, e.getMessage());
            return 0;
        }
    }

    @Override
    public LockInfo getLockInfo(String lockKey) {
        String fullLockKey = buildLockKey(lockKey);
        return lockInfoMap.get(fullLockKey);
    }

    /**
     * 启动看门狗
     */
    private void startWatchdog(String lockKey, LockInfo lockInfo) {
        ScheduledFuture<?> watchdogFuture = watchdogExecutor.scheduleAtFixedRate(() -> {
            try {
                // 检查锁是否仍然存在
                if (redisUtils.hasKey(lockKey.replace(lockKeyPrefix, ""))) {
                    // 延长锁的过期时间
                    redisUtils.expire(lockKey.replace(lockKeyPrefix, ""), (int) lockInfo.getTimeout() / 1000, TimeUnit.SECONDS);
                    lockRenewedCount.incrementAndGet();
                    log.debug("分布式锁自动续期: key={}", lockKey);
                } else {
                    // 锁已不存在，停止看门狗
                    stopWatchdog(lockKey);
                }
            } catch (Exception e) {
                log.error("分布式锁看门狗异常: key={}, error={}", lockKey, e.getMessage());
            }
        }, watchdogInterval, watchdogInterval, TimeUnit.SECONDS);

        lockInfo.setWatchdogFuture(watchdogFuture);
    }

    /**
     * 停止看门狗
     */
    private void stopWatchdog(String lockKey) {
        LockInfo lockInfo = lockInfoMap.get(lockKey);
        if (lockInfo != null && lockInfo.getWatchdogFuture() != null) {
            lockInfo.getWatchdogFuture().cancel(true);
        }
    }

    /**
     * 构建锁键名
     */
    private String buildLockKey(String lockKey) {
        return lockKeyPrefix + lockKey;
    }

    /**
     * 获取锁指标
     */
    public Map<String, Long> getMetrics() {
        Map<String, Long> metrics = new ConcurrentHashMap<>();
        metrics.put("lockAcquired", lockAcquiredCount.get());
        metrics.put("lockReleased", lockReleasedCount.get());
        metrics.put("lockRenewed", lockRenewedCount.get());
        metrics.put("lockFailed", lockFailedCount.get());
        metrics.put("activeLocks", (long) lockInfoMap.size());
        return metrics;
    }

    /**
     * 重置指标
     */
    public void resetMetrics() {
        lockAcquiredCount.set(0);
        lockReleasedCount.set(0);
        lockRenewedCount.set(0);
        lockFailedCount.set(0);
        log.info("分布式锁指标已重置");
    }

    /**
     * 销毁时清理
     */
    @PreDestroy
    public void destroy() {
        log.info("正在销毁分布式锁组件，清理活跃锁: {}", lockInfoMap.size());
        watchdogExecutor.shutdown();
        try {
            if (!watchdogExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                watchdogExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            watchdogExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}