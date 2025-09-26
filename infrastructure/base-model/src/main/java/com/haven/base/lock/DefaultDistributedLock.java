package com.haven.base.lock;

import com.haven.base.cache.CacheService;
import com.haven.base.utils.TraceIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 默认分布式锁实现
 * 基于缓存服务的分布式锁，支持自动释放、可重入、续租等特性
 *
 * @author HavenButler
 */
@Slf4j
@Component
@ConditionalOnMissingBean(DistributedLock.class)
@RequiredArgsConstructor
public class DefaultDistributedLock implements DistributedLock {

    private final CacheService cacheService;

    /**
     * 默认锁超时时间
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 锁键前缀
     */
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 当前线程持有的锁信息
     */
    private static final ThreadLocal<String> CURRENT_LOCK_OWNER = new ThreadLocal<>();

    @Override
    public boolean tryLock(String lockKey, Duration timeout) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        String lockOwner = generateLockOwner();
        long timeoutMs = timeout.toMillis();

        log.debug("尝试获取锁: key={}, owner={}, timeout={}ms", fullLockKey, lockOwner, timeoutMs);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeoutMs;

        while (System.currentTimeMillis() < endTime) {
            // 尝试设置锁
            if (setLockIfAbsent(fullLockKey, lockOwner, timeout)) {
                CURRENT_LOCK_OWNER.set(lockOwner);
                log.info("获取锁成功: key={}, owner={}", fullLockKey, lockOwner);
                return true;
            }

            // 检查是否是可重入锁
            LockInfo existingLock = getLockInfo(lockKey);
            if (existingLock != null && lockOwner.equals(existingLock.getOwner())) {
                // 可重入，更新锁信息
                existingLock.setRenewCount(existingLock.getRenewCount() + 1);
                existingLock.setReentrant(true);
                saveLockInfo(fullLockKey, existingLock, timeout);
                log.debug("可重入锁: key={}, owner={}, count={}", fullLockKey, lockOwner, existingLock.getRenewCount());
                return true;
            }

            // 短暂等待后重试
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("获取锁被中断: key={}", fullLockKey);
                return false;
            }
        }

        log.warn("获取锁超时: key={}, timeout={}ms", fullLockKey, timeoutMs);
        return false;
    }

    @Override
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_TIMEOUT);
    }

    @Override
    public boolean unlock(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        String currentOwner = CURRENT_LOCK_OWNER.get();

        if (currentOwner == null) {
            log.warn("释放锁失败: 当前线程未持有锁 key={}", fullLockKey);
            return false;
        }

        LockInfo lockInfo = getLockInfo(lockKey);
        if (lockInfo == null) {
            log.warn("释放锁失败: 锁不存在 key={}", fullLockKey);
            CURRENT_LOCK_OWNER.remove();
            return false;
        }

        if (!currentOwner.equals(lockInfo.getOwner())) {
            log.warn("释放锁失败: 不是锁的拥有者 key={}, current={}, owner={}",
                    fullLockKey, currentOwner, lockInfo.getOwner());
            return false;
        }

        // 处理可重入锁
        if (lockInfo.isReentrant() && lockInfo.getRenewCount() > 0) {
            lockInfo.setRenewCount(lockInfo.getRenewCount() - 1);
            saveLockInfo(fullLockKey, lockInfo, Duration.ofMillis(lockInfo.getRemainingTime()));
            log.debug("可重入锁递减: key={}, count={}", fullLockKey, lockInfo.getRenewCount());
            return true;
        }

        // 删除锁
        cacheService.delete(fullLockKey);
        CURRENT_LOCK_OWNER.remove();
        log.info("释放锁成功: key={}, owner={}", fullLockKey, currentOwner);
        return true;
    }

    @Override
    public boolean forceUnlock(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        cacheService.delete(fullLockKey);
        CURRENT_LOCK_OWNER.remove();
        log.warn("强制释放锁: key={}", fullLockKey);
        return true;
    }

    @Override
    public boolean isLocked(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        return cacheService.exists(fullLockKey);
    }

    @Override
    public long getLockExpire(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        return cacheService.getExpire(fullLockKey);
    }

    @Override
    public boolean renewLock(String lockKey, Duration additionalTime) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        String currentOwner = CURRENT_LOCK_OWNER.get();

        if (currentOwner == null) {
            log.warn("续租失败: 当前线程未持有锁 key={}", fullLockKey);
            return false;
        }

        LockInfo lockInfo = getLockInfo(lockKey);
        if (lockInfo == null || !currentOwner.equals(lockInfo.getOwner())) {
            log.warn("续租失败: 不是锁的拥有者 key={}", fullLockKey);
            return false;
        }

        lockInfo.setRenewCount(lockInfo.getRenewCount() + 1);
        lockInfo.setExpireTime(System.currentTimeMillis() + additionalTime.toMillis());

        saveLockInfo(fullLockKey, lockInfo, additionalTime);
        log.debug("续租成功: key={}, additionalTime={}", fullLockKey, additionalTime);
        return true;
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
        return executeWithLock(lockKey, DEFAULT_TIMEOUT, action);
    }

    @Override
    public long unlockByPattern(String pattern) {
        String fullPattern = LOCK_PREFIX + pattern;
        return cacheService.deleteByPattern(fullPattern);
    }

    @Override
    public LockInfo getLockInfo(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        return cacheService.get(fullLockKey, LockInfo.class).orElse(null);
    }

    /**
     * 设置锁（如果不存在）
     */
    private boolean setLockIfAbsent(String lockKey, String lockOwner, Duration timeout) {
        if (cacheService.exists(lockKey)) {
            return false;
        }

        LockInfo lockInfo = new LockInfo(
                lockKey,
                lockOwner,
                System.currentTimeMillis(),
                System.currentTimeMillis() + timeout.toMillis()
        );

        cacheService.set(lockKey, lockInfo, timeout);
        return cacheService.exists(lockKey);
    }

    /**
     * 保存锁信息
     */
    private void saveLockInfo(String lockKey, LockInfo lockInfo, Duration timeout) {
        cacheService.set(lockKey, lockInfo, timeout);
    }

    /**
     * 生成锁拥有者标识
     */
    private String generateLockOwner() {
        String traceId = TraceIdUtil.getCurrent();
        String threadName = Thread.currentThread().getName();
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        if (traceId != null) {
            return String.format("%s-%s-%s", traceId, threadName, uuid);
        } else {
            return String.format("%s-%s-%d", threadName, uuid, Thread.currentThread().getId());
        }
    }
}