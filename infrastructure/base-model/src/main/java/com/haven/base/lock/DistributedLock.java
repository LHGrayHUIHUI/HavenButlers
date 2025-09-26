package com.haven.base.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁接口
 * 提供分布式环境下的锁机制，支持自动释放、可重入、超时等特性
 *
 * @author HavenButler
 */
public interface DistributedLock {

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁键
     * @param timeout 超时时间
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey, Duration timeout);

    /**
     * 尝试获取锁（默认超时30秒）
     *
     * @param lockKey 锁键
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey);

    /**
     * 释放锁
     *
     * @param lockKey 锁键
     * @return 是否释放成功
     */
    boolean unlock(String lockKey);

    /**
     * 强制释放锁（管理员操作）
     *
     * @param lockKey 锁键
     * @return 是否释放成功
     */
    boolean forceUnlock(String lockKey);

    /**
     * 检查锁是否存在
     *
     * @param lockKey 锁键
     * @return 是否存在
     */
    boolean isLocked(String lockKey);

    /**
     * 获取锁的剩余时间
     *
     * @param lockKey 锁键
     * @return 剩余时间（秒），-1表示永不过期，0表示锁不存在
     */
    long getLockExpire(String lockKey);

    /**
     * 续租锁（延长过期时间）
     *
     * @param lockKey 锁键
     * @param additionalTime 延长时间
     * @return 是否续租成功
     */
    boolean renewLock(String lockKey, Duration additionalTime);

    /**
     * 执行带锁的操作
     *
     * @param lockKey 锁键
     * @param timeout 获取锁超时时间
     * @param action 要执行的操作
     * @return 操作结果
     * @throws LockException 锁操作异常
     */
    <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> action) throws LockException;

    /**
     * 执行带锁的操作（无返回值）
     *
     * @param lockKey 锁键
     * @param timeout 获取锁超时时间
     * @param action 要执行的操作
     * @throws LockException 锁操作异常
     */
    void executeWithLock(String lockKey, Duration timeout, Runnable action) throws LockException;

    /**
     * 执行带锁的操作（默认超时30秒）
     *
     * @param lockKey 锁键
     * @param action 要执行的操作
     * @return 操作结果
     * @throws LockException 锁操作异常
     */
    <T> T executeWithLock(String lockKey, Supplier<T> action) throws LockException;

    /**
     * 批量释放锁（按模式匹配）
     *
     * @param pattern 锁键模式（支持*通配符）
     * @return 释放的锁数量
     */
    long unlockByPattern(String pattern);

    /**
     * 获取锁信息
     *
     * @param lockKey 锁键
     * @return 锁信息
     */
    LockInfo getLockInfo(String lockKey);

    /**
     * 锁信息
     */
    class LockInfo {
        private String lockKey;         // 锁键
        private String owner;           // 锁拥有者
        private long createTime;        // 创建时间
        private long expireTime;        // 过期时间
        private int renewCount;         // 续租次数
        private boolean reentrant;      // 是否可重入

        // Constructors
        public LockInfo() {}

        public LockInfo(String lockKey, String owner, long createTime, long expireTime) {
            this.lockKey = lockKey;
            this.owner = owner;
            this.createTime = createTime;
            this.expireTime = expireTime;
        }

        // Getters and Setters
        public String getLockKey() { return lockKey; }
        public void setLockKey(String lockKey) { this.lockKey = lockKey; }

        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }

        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }

        public long getExpireTime() { return expireTime; }
        public void setExpireTime(long expireTime) { this.expireTime = expireTime; }

        public int getRenewCount() { return renewCount; }
        public void setRenewCount(int renewCount) { this.renewCount = renewCount; }

        public boolean isReentrant() { return reentrant; }
        public void setReentrant(boolean reentrant) { this.reentrant = reentrant; }

        /**
         * 检查锁是否过期
         */
        public boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }

        /**
         * 获取剩余时间（毫秒）
         */
        public long getRemainingTime() {
            if (expireTime <= 0) return -1; // 永不过期
            long remaining = expireTime - System.currentTimeMillis();
            return Math.max(0, remaining);
        }
    }
}