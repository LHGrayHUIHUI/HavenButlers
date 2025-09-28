package com.haven.admin.web;

/**
 * 业务异常类
 *
 * 用于封装业务逻辑中的预期异常，携带具体的错误码和错误信息。
 * 相比直接抛RuntimeException，BusinessException更加语义化，
 * 便于全局异常处理器区分不同类型的异常并提供相应的处理策略。
 *
 * 使用场景：
 * - 资源不存在：如告警ID、规则ID不存在
 * - 业务规则校验失败：如状态不允许的操作
 * - 参数校验失败：如必填参数缺失
 * - 权限不足：如访问受限资源
 *
 * 错误码规范：
 * - 40xxx: 客户端错误（参数错误、资源不存在等）
 * - 50xxx: 服务端错误（系统异常、外部依赖异常等）
 *
 * @author HavenButler
 * @version 1.0.0
 */
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码
     *
     * 用于区分不同类型的业务异常，便于客户端进行针对性处理
     */
    private final int code;

    /**
     * 构造业务异常
     *
     * @param code 错误码，建议使用标准的HTTP状态码或自定义的业务错误码
     * @param message 错误信息，应当是用户友好的描述
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造带原因的业务异常
     *
     * @param code 错误码
     * @param message 错误信息
     * @param cause 原始异常，用于保留异常链
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取业务错误码
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 重写toString方法，提供更详细的异常信息
     *
     * @return 格式化的异常信息
     */
    @Override
    public String toString() {
        return String.format("BusinessException{code=%d, message='%s'}", code, getMessage());
    }
}