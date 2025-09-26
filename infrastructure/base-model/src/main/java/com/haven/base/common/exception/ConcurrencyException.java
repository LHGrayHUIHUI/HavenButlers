package com.haven.base.common.exception;

import com.haven.base.common.response.ErrorCode;

/**
 * 并发异常
 * 用于处理数据并发更新、乐观锁冲突等场景
 *
 * @author HavenButler
 */
public class ConcurrencyException extends BaseException {

    /**
     * 默认并发异常
     */
    public ConcurrencyException() {
        super(ErrorCode.DATA_VERSION_ERROR, "数据并发更新冲突，请刷新后重试");
    }

    /**
     * 自定义消息的并发异常
     */
    public ConcurrencyException(String message) {
        super(ErrorCode.DATA_VERSION_ERROR, message);
    }

    /**
     * 乐观锁冲突异常
     */
    public static ConcurrencyException optimisticLock(String resource, Object version) {
        return new ConcurrencyException(
            String.format("资源[%s]版本冲突，当前版本: %s", resource, version));
    }

    /**
     * 数据已被修改异常
     */
    public static ConcurrencyException dataModified(String resource, String id) {
        return new ConcurrencyException(
            String.format("资源[%s:%s]已被其他用户修改，请刷新后重试", resource, id));
    }

    /**
     * 重复操作异常
     */
    public static ConcurrencyException duplicateOperation(String operation) {
        return new ConcurrencyException(
            String.format("操作[%s]正在处理中，请勿重复提交", operation));
    }
}