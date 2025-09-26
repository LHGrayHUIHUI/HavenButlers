package com.haven.base.lock;

import com.haven.base.common.exception.BaseException;
import com.haven.base.common.response.ErrorCode;

/**
 * 锁操作异常
 * 分布式锁操作过程中的各种异常
 *
 * @author HavenButler
 */
public class LockException extends BaseException {

    /**
     * 获取锁超时异常
     */
    public LockException(String lockKey, String message) {
        super(ErrorCode.SYSTEM_ERROR, String.format("锁操作失败[%s]: %s", lockKey, message));
    }

    /**
     * 获取锁超时异常
     */
    public static LockException timeout(String lockKey, long timeoutMs) {
        return new LockException(lockKey, String.format("获取锁超时，等待时间: %dms", timeoutMs));
    }

    /**
     * 释放锁失败异常
     */
    public static LockException unlockFailed(String lockKey, String reason) {
        return new LockException(lockKey, String.format("释放锁失败: %s", reason));
    }

    /**
     * 锁已被其他线程持有异常
     */
    public static LockException alreadyLocked(String lockKey, String owner) {
        return new LockException(lockKey, String.format("锁已被占用，持有者: %s", owner));
    }

    /**
     * 锁不存在异常
     */
    public static LockException notExists(String lockKey) {
        return new LockException(lockKey, "锁不存在");
    }

    /**
     * 续租失败异常
     */
    public static LockException renewFailed(String lockKey, String reason) {
        return new LockException(lockKey, String.format("续租失败: %s", reason));
    }

    /**
     * 无权操作异常（不是锁的拥有者）
     */
    public static LockException notOwner(String lockKey, String currentOwner, String requestOwner) {
        return new LockException(lockKey,
            String.format("无权操作，当前拥有者: %s，请求者: %s", currentOwner, requestOwner));
    }
}