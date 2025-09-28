package com.haven.admin.web;

import com.haven.admin.common.AdminResponse;
import com.haven.admin.common.AdminException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Admin服务全局异常处理器
 *
 * 作用：
 * - 提供统一的异常处理机制，确保所有异常都返回 AdminResponse 格式
 * - 作为"兜底保护网"，防止异常落到 Spring 默认的 BasicErrorController
 * - 保证前后端约定的响应协议一致性，避免返回 HTML 或原始 JSON
 * - 统一错误码和错误信息的格式化，便于客户端处理
 *
 * 设计原则：
 * - 不暴露内部堆栈信息，仅返回用户友好的错误描述
 * - 区分不同类型的异常，返回相应的HTTP状态码和业务错误码
 * - 记录详细的错误日志，便于开发者排查问题
 * - 支持国际化和多语言错误消息（预留扩展点）
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice  // 暂时移除包限制以便查看所有错误
public class AdminGlobalExceptionHandler {

    /**
     * 处理业务逻辑异常
     *
     * 适用场景：
     * - 资源不存在（如告警ID不存在）
     * - 业务规则校验失败
     * - 状态不符合预期等业务异常
     *
     * @param e 业务异常
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AdminResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return AdminResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 Admin 自定义异常
     *
     * 适用场景：
     * - 控制器层显式抛出的 AdminException（例如 AdminController 中 try/catch -> throw AdminException）
     * - 使用异常内携带的业务错误码
     */
    @ExceptionHandler(AdminException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public AdminResponse<Void> handleAdminException(AdminException e) {
        int code = e.getCode() > 0 ? e.getCode() : 50000;
        log.error("Admin异常: code={}, message={}", code, e.getMessage());
        return AdminResponse.error(code, e.getMessage());
    }

    /**
     * 处理参数校验异常
     *
     * 适用场景：
     * - 请求参数格式错误
     * - 必填参数缺失
     * - 参数值不符合约束条件
     *
     * @param e 参数异常
     * @return 统一格式的错误响应
     */
    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AdminResponse<Void> handleParameterException(IllegalArgumentException e) {
        log.warn("参数校验异常: {}", e.getMessage());
        return AdminResponse.error(40001, "请求参数错误: " + e.getMessage());
    }

    /**
     * 处理资源未找到异常
     *
     * 适用场景：
     * - 查询的告警、规则、服务等不存在
     * - 访问的API端点不存在
     *
     * @param e 运行时异常（通常来自服务层的资源查找失败）
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public AdminResponse<Void> handleRuntimeException(RuntimeException e) {
        String message = e.getMessage();

        // 根据异常消息判断错误类型，提供更准确的错误码
        if (message != null && (message.contains("不存在") || message.contains("未找到"))) {
            log.warn("资源未找到异常: {}", message);
            return AdminResponse.error(40404, message);
        }

        // 其他运行时异常作为内部错误处理
        log.error("运行时异常: {}", message, e);
        return AdminResponse.error(50000, "系统繁忙，请稍后重试");
    }

    /**
     * 处理系统级异常
     *
     * 适用场景：
     * - 数据库连接异常
     * - 网络超时异常
     * - 外部服务不可用
     * - 其他未预期的系统异常
     *
     * 这是最后的兜底处理器，确保所有异常都有统一的响应格式
     *
     * @param e 系统异常
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public AdminResponse<Void> handleSystemException(Exception e) {
        // 详细记录异常信息用于调试
        log.error("系统异常详情 - 类型: {}, 消息: {}, 请求URI: {}",
            e.getClass().getSimpleName(),
            e.getMessage(),
            getCurrentRequestURI(),
            e);

        // 根据异常类型提供更具体的错误信息
        String errorMessage = String.format("系统异常: %s - %s",
            e.getClass().getSimpleName(),
            e.getMessage() != null ? e.getMessage() : "未知错误");

        return AdminResponse.error(50000, errorMessage);
    }

    /**
     * 获取当前请求URI用于调试
     */
    private String getCurrentRequestURI() {
        try {
            return org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes()
                .getAttribute(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST, 0)
                .toString();
        } catch (Exception ex) {
            return "无法获取请求URI";
        }
    }
}
