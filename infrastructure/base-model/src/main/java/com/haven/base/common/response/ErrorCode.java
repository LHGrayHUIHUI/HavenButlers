package com.haven.base.common.response;

import lombok.Getter;

/**
 * 错误码枚举
 * 错误码规范：
 * 1xxxx - 系统级错误
 * 2xxxx - 认证授权错误
 * 3xxxx - 参数校验错误
 * 4xxxx - 业务逻辑错误
 * 5xxxx - 第三方服务错误
 *
 * @author HavenButler
 */
@Getter
public enum ErrorCode {

    // ========== 成功响应 ==========
    SUCCESS(0, "成功"),

    // ========== 系统级错误 10000-19999 ==========
    SYSTEM_ERROR(10000, "系统内部错误"),
    SERVICE_UNAVAILABLE(10001, "服务不可用"),
    NETWORK_ERROR(10002, "网络异常"),
    DATABASE_ERROR(10003, "数据库错误"),
    CACHE_ERROR(10004, "缓存错误"),
    MESSAGE_QUEUE_ERROR(10005, "消息队列错误"),
    FILE_UPLOAD_ERROR(10006, "文件上传失败"),
    FILE_DOWNLOAD_ERROR(10007, "文件下载失败"),
    CONFIG_ERROR(10008, "配置错误"),
    RATE_LIMIT_ERROR(10009, "请求过于频繁"),
    RATE_LIMIT_EXCEEDED(10010, "超过访问限制"),

    // ========== 认证授权错误 20000-29999 ==========
    UNAUTHORIZED(20000, "未登录或登录已过期"),
    FORBIDDEN(20001, "没有权限访问"),
    ACCOUNT_NOT_FOUND(20002, "账号不存在"),
    ACCOUNT_DISABLED(20003, "账号已被禁用"),
    ACCOUNT_LOCKED(20004, "账号已被锁定"),
    PASSWORD_ERROR(20005, "密码错误"),
    TOKEN_INVALID(20006, "令牌无效"),
    TOKEN_EXPIRED(20007, "令牌已过期"),
    SIGNATURE_ERROR(20008, "签名验证失败"),
    FAMILY_NOT_EXIST(20009, "家庭不存在"),
    USER_NOT_IN_FAMILY(20010, "用户不在该家庭中"),
    PERMISSION_DENIED(20011, "权限不足"),

    // ========== 参数校验错误 30000-39999 ==========
    PARAM_ERROR(30000, "参数错误"),
    PARAM_MISSING(30001, "缺少必要参数"),
    PARAM_TYPE_ERROR(30002, "参数类型错误"),
    PARAM_FORMAT_ERROR(30003, "参数格式错误"),
    PARAM_OUT_OF_RANGE(30004, "参数超出范围"),
    PARAM_DUPLICATE(30005, "参数重复"),
    PARAM_ILLEGAL(30006, "参数非法"),
    FILE_TYPE_ERROR(30007, "文件类型不支持"),
    FILE_SIZE_EXCEED(30008, "文件大小超出限制"),

    // ========== 业务逻辑错误 40000-49999 ==========
    BUSINESS_ERROR(40000, "业务处理失败"),
    DATA_NOT_FOUND(40001, "数据不存在"),
    DATA_ALREADY_EXISTS(40002, "数据已存在"),
    DATA_VERSION_ERROR(40003, "数据版本冲突"),
    STATUS_ERROR(40004, "状态错误"),
    OPERATION_NOT_ALLOWED(40005, "操作不允许"),
    BALANCE_INSUFFICIENT(40006, "余额不足"),
    QUOTA_EXCEEDED(40007, "配额超限"),

    // 设备相关错误
    DEVICE_OFFLINE(41001, "设备离线"),
    DEVICE_NOT_FOUND(41002, "设备不存在"),
    DEVICE_ALREADY_BOUND(41003, "设备已被绑定"),
    DEVICE_CONTROL_FAILED(41004, "设备控制失败"),
    DEVICE_NOT_SUPPORTED(41005, "设备类型不支持"),

    // 家庭相关错误
    FAMILY_MEMBER_LIMIT(42001, "家庭成员数量已达上限"),
    FAMILY_DEVICE_LIMIT(42002, "家庭设备数量已达上限"),
    FAMILY_OWNER_CANNOT_LEAVE(42003, "家庭创建者不能退出家庭"),

    // 场景相关错误
    SCENE_NOT_FOUND(43001, "场景不存在"),
    SCENE_EXECUTE_FAILED(43002, "场景执行失败"),
    SCENE_CONFIG_ERROR(43003, "场景配置错误"),

    // ========== 第三方服务错误 50000-59999 ==========
    EXTERNAL_SERVICE_ERROR(50000, "外部服务错误"),
    EXTERNAL_SERVICE_TIMEOUT(50001, "外部服务超时"),
    AI_SERVICE_ERROR(50002, "AI服务错误"),
    SMS_SEND_FAILED(50003, "短信发送失败"),
    EMAIL_SEND_FAILED(50004, "邮件发送失败"),
    PAYMENT_FAILED(50005, "支付失败"),
    THIRD_PARTY_AUTH_FAILED(50006, "第三方认证失败");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造函数
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据错误码获取错误枚举
     */
    public static ErrorCode getByCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }
}