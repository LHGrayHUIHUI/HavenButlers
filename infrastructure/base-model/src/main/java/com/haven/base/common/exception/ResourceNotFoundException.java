package com.haven.base.common.exception;

import com.haven.base.common.response.ErrorCode;

/**
 * 资源不存在异常
 * 用于处理数据不存在、文件不存在等场景
 *
 * @author HavenButler
 */
public class ResourceNotFoundException extends BaseException {

    /**
     * 通用资源不存在异常
     */
    public ResourceNotFoundException(String resource, String id) {
        super(ErrorCode.DATA_NOT_FOUND, String.format("%s[%s]不存在", resource, id));
    }

    /**
     * 自定义消息的资源不存在异常
     */
    public ResourceNotFoundException(String message) {
        super(ErrorCode.DATA_NOT_FOUND, message);
    }

    /**
     * 用户不存在异常
     */
    public static ResourceNotFoundException user(String userId) {
        return new ResourceNotFoundException("用户", userId);
    }

    /**
     * 设备不存在异常
     */
    public static ResourceNotFoundException device(String deviceId) {
        return new ResourceNotFoundException("设备", deviceId);
    }

    /**
     * 家庭不存在异常
     */
    public static ResourceNotFoundException family(String familyId) {
        return new ResourceNotFoundException("家庭", familyId);
    }

    /**
     * 文件不存在异常
     */
    public static ResourceNotFoundException file(String filePath) {
        return new ResourceNotFoundException("文件", filePath);
    }

    /**
     * 服务不存在异常
     */
    public static ResourceNotFoundException service(String serviceName) {
        return new ResourceNotFoundException("服务", serviceName);
    }

    /**
     * 配置不存在异常
     */
    public static ResourceNotFoundException config(String configKey) {
        return new ResourceNotFoundException("配置", configKey);
    }
}