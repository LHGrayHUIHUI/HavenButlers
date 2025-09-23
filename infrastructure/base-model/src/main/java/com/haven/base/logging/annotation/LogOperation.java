package com.haven.base.logging.annotation;

import com.haven.base.logging.model.LogEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * 在方法上使用此注解，自动记录操作日志
 *
 * 使用示例：
 * <pre>
 * {@code
 * @LogOperation(
 *     operationType = "USER_LOGIN",
 *     description = "用户登录",
 *     logLevel = LogEvent.LogType.OPERATION
 * )
 * public LoginResult login(LoginRequest request) {
 *     // 业务逻辑
 * }
 * }
 * </pre>
 *
 * @author HavenButler
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogOperation {

    /**
     * 操作类型
     * 如：USER_LOGIN, FILE_UPLOAD, DEVICE_CONTROL
     */
    String operationType();

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 日志类型
     */
    LogEvent.LogType logType() default LogEvent.LogType.OPERATION;

    /**
     * 风险级别（仅安全日志有效）
     */
    LogEvent.RiskLevel riskLevel() default LogEvent.RiskLevel.LOW;

    /**
     * 是否记录执行时间
     */
    boolean recordExecutionTime() default true;

    /**
     * 是否记录方法参数
     */
    boolean recordParams() default false;

    /**
     * 是否记录返回值
     */
    boolean recordResult() default false;

    /**
     * 业务模块（仅业务日志有效）
     */
    String businessModule() default "";

    /**
     * 业务场景（仅业务日志有效）
     */
    String businessScenario() default "";

    /**
     * 指标名称（仅性能日志有效）
     */
    String metricName() default "";

    /**
     * 指标单位（仅性能日志有效）
     */
    String metricUnit() default "ms";

    /**
     * 是否异步记录（默认异步）
     */
    boolean async() default true;

    /**
     * 失败时是否记录错误日志
     */
    boolean logOnError() default true;
}