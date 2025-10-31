package com.haven.base.lock;

import lombok.Data;

import java.util.concurrent.ScheduledFuture;

/**
 * 锁信息类
 * 用于存储分布式锁的详细信息
 *
 * @author HavenButler
 */
@Data
public class LockInfo {

    /**
     * 锁键名
     */
    private String lockKey;

    /**
     * 锁拥有者
     */
    private String owner;

    /**
     * 锁创建时间
     */
    private long createTime;

    /**
     * 锁过期时间
     */
    private long expireTime;

    /**
     * 续租次数
     */
    private int renewCount = 0;

    /**
     * 是否可重入
     */
    private boolean reentrant = false;

    /**
     * 看门狗任务
     */
    private ScheduledFuture<?> watchdogFuture;

    /**
     * 超时时间（秒）
     */
    private long timeout;

    public LockInfo() {
    }

    public LockInfo(String lockKey, String owner, long createTime, long expireTime) {
        this.lockKey = lockKey;
        this.owner = owner;
        this.createTime = createTime;
        this.expireTime = expireTime;
    }

    /**
     * 获取剩余时间（毫秒）
     */
    public long getRemainingTime() {
        return Math.max(0, expireTime - System.currentTimeMillis());
    }

    /**
     * 是否已过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
}