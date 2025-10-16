package com.haven.storage.exception;

import com.haven.base.common.exception.AuthException;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.exception.ValidationException;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 文件存储异常处理器
 * <p>
 * 专门处理文件存储相关的异常，提供标准化的错误响应格式。
 * 替代Controller中的重复try-catch代码，提升代码可维护性。
 * <p>
 * 🎯 功能特性：
 * - 统一的文件异常处理逻辑，避免代码重复
 * - 结构化的日志记录，便于问题排查
 * - 标准化的错误响应格式
 * - 支持链路追踪，便于分布式系统调试
 * - 详细的文件上传上下文信息记录
 * <p>
 * 💡 设计优势：
 * - 集中管理文件异常处理逻辑
 * - Controller层代码更简洁，专注于业务逻辑
 * - 异常处理一致性保证
 * - 便于统一调整错误码和消息格式
 *
 * @author HavenButler
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.haven.storage.controller")
public class FileStorageExceptionHandler {

    /**
     * 处理参数验证异常
     * <p>
     * 当请求参数不符合要求时触发，通常返回400状态码
     *
     * @param e 参数验证异常
     * @return 标准化的错误响应
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseBody
    public ResponseWrapper<Void> handleValidationException(ValidationException e) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.warn("参数验证失败: traceId={}, error={}", traceId, e.getMessage(), e);

        return ResponseWrapper.error(40001, "参数验证失败: " + e.getMessage(), null);
    }

    /**
     * 处理权限异常
     * <p>
     * 当用户权限不足或认证失败时触发，通常返回401状态码
     *
     * @param e 权限异常
     * @return 标准化的错误响应
     */
    @ExceptionHandler(AuthException.class)
    @ResponseBody
    public ResponseWrapper<Void> handleAuthException(AuthException e) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.warn("权限验证失败: traceId={}, error={}", traceId, e.getMessage(), e);

        return ResponseWrapper.error(40101, "权限验证失败", null);
    }

    /**
     * 处理业务异常
     * <p>
     * 当业务逻辑处理失败时触发，通常返回500状态码
     *
     * @param e 业务异常
     * @return 标准化的错误响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseWrapper<Void> handleBusinessException(BusinessException e) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.error("业务处理异常: traceId={}, error={}", traceId, e.getMessage(), e);

        return ResponseWrapper.error(50001, "业务处理失败: " + e.getMessage(), null);
    }

    /**
     * 处理文件上传相关异常
     * <p>
     * 专门处理文件上传过程中的异常，提供更详细的上下文信息
     *
     * @param e 文件上传异常
     * @return 标准化的错误响应
     */
    @ExceptionHandler(FileUploadException.class)
    @ResponseBody
    public ResponseWrapper<Void> handleFileUploadException(FileUploadException e) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.error("文件上传异常: traceId={}, familyId={}, userId={}, fileName={}, error={}",
                traceId, e.getFamilyId(), e.getUserId(), e.getFileName(), e.getMessage(), e);

        return ResponseWrapper.error(50003, "文件上传失败: " + e.getMessage(), null);
    }

    /**
     * 处理系统异常（兜底处理）
     * <p>
     * 当出现未预期的系统异常时触发，返回通用的错误信息
     * 避免暴露内部系统细节，保证系统安全性
     *
     * @param e 系统异常
     * @return 标准化的错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseWrapper<Void> handleSystemException(Exception e) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.error("系统异常: traceId={}, exceptionType={}, error={}",
                traceId, e.getClass().getSimpleName(), e.getMessage(), e);

        return ResponseWrapper.error(50002, "系统异常，请稍后重试", null);
    }

}