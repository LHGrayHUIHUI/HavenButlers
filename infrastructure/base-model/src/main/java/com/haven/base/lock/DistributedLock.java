package com.haven.base.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁接口
 * 定义分布式锁的标准操作，支持可重入、自动续期、超时控制等特性
 *
 * @author HavenButler
 */
public interface DistributedLock {

    /**
     * 尝试获取锁（带超时时间）
     *
     * @param lockKey 锁键名
     * @param timeout 超时时间
     * @return 是否成功获取锁
     */
    boolean tryLock(String lockKey, Duration timeout);

    /**
     * 尝试获取锁（使用默认超时时间）
     *
     * @param lockKey 锁键名
     * @return 是否成功获取锁
     */
    boolean tryLock(String lockKey);

    /**
     * 释放锁
     *
     * @param lockKey 锁键名
     * @return 是否成功释放锁
     */
    boolean unlock(String lockKey);

    /**
     * 强制释放锁（不检查锁的拥有者）
     *
     * @param lockKey 锁键名
     * @return 是否成功释放锁
     */
    boolean forceUnlock(String lockKey);

    /**
     * 检查锁是否存在
     *
     * @param lockKey 锁键名
     * @return 锁是否存在
     */
    boolean isLocked(String lockKey);

    /**
     * 获取锁的过期时间
     *
     * @param lockKey 锁键名
     * @return 过期时间（秒），-1表示永不过期，-2表示锁不存在
     */
    long getLockExpire(String lockKey);

    /**
     * 续租锁
     *
     * @param lockKey       锁键名
     * @param additionalTime 额外的时间
     * @return 是否成功续租
     */
    boolean renewLock(String lockKey, Duration additionalTime);

    /**
     * 执行带锁的任务（带返回值）
     *
     * @param lockKey  锁键名
     * @param timeout  超时时间
     * @param action   要执行的任务
     * @param <T>      返回值类型
     * @return 任务执行结果
     * @throws LockException 锁异常
     */
    <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> action) throws LockException;

    /**
     * 执行带锁的任务（无返回值）
     *
     * @param lockKey  锁键名
     * @param timeout  超时时间
     * @param action   要执行的任务
     * @throws LockException 锁异常
     */
    void executeWithLock(String lockKey, Duration timeout, Runnable action) throws LockException;

    /**
     * 执行带锁的任务（使用默认超时时间）
     *
     * @param lockKey  锁键名
     * @param action   要执行的任务
     * @param <T>      返回值类型
     * @return 任务执行结果
     * @throws LockException 锁异常
     */
    <T> T executeWithLock(String lockKey, Supplier<T> action) throws LockException;

    /**
     * 按模式批量释放锁
     *
     * @param pattern 锁键模式
     * @return 释放的锁数量
     */
    long unlockByPattern(String pattern);

    /**
     * 获取锁信息
     *
     * @param lockKey 锁键名
     * @return 锁信息
     */
    LockInfo getLockInfo(String lockKey);

    /**
     * 分布式锁异常
     */
    class LockException extends Exception {
        public LockException(String message) {
            super(message);
        }

        public LockException(String message, Throwable cause) {
            super(message, cause);
        }

        public static LockException timeout(String lockKey, long timeoutMs) {
            return new LockException(String.format("获取锁超时: key=%s, timeout=%dms", lockKey, timeoutMs));
        }
    }
}