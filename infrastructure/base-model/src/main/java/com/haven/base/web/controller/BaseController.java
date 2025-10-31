package com.haven.base.web.controller;

import com.haven.base.common.model.PageRequest;
import com.haven.base.common.model.PageResult;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.function.Supplier;

/**
 * 基础控制器类
 * 提供统一的响应格式、异常处理、参数校验等功能
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
public abstract class BaseController {

    /**
     * 成功响应（带数据）
     */
    protected <T> ResponseWrapper<T> success(T data) {
        return ResponseWrapper.success(data);
    }

    /**
     * 成功响应（无数据）
     */
    protected ResponseWrapper<Void> success() {
        return ResponseWrapper.success(null);
    }

    /**
     * 分页响应
     */
    protected <T> ResponseWrapper<PageResult<T>> page(PageResult<T> page) {
        return ResponseWrapper.success(page);
    }

    /**
     * 错误响应
     */
    protected ResponseWrapper<Void> error(ErrorCode errorCode, String message) {
        return ResponseWrapper.<Void>error(errorCode.getCode(), message);
    }

    /**
     * 错误响应（使用错误码默认消息）
     */
    protected ResponseWrapper<Void> error(ErrorCode errorCode) {
        return ResponseWrapper.<Void>error(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 错误响应（自定义错误码和消息）
     */
    protected ResponseWrapper<Void> error(Integer code, String message) {
        return ResponseWrapper.<Void>error(code, message);
    }

    /**
     * 执行操作并统一异常处理
     */
    protected <T> ResponseWrapper<T> execute(Supplier<T> operation) {
        String traceId = TraceIdUtil.getCurrent();
        try {
            T result = operation.get();
            log.debug("操作执行成功: traceId={}", traceId);
            return success(result);
        } catch (Exception e) {
            log.error("操作执行失败: traceId={}, error={}", traceId, e.getMessage(), e);
            return  ResponseWrapper.error(ErrorCode.SYSTEM_ERROR.getCode(), "操作失败: " + e.getMessage(),null);
        }
    }

    /**
     * 执行无返回值操作并统一异常处理
     */
    protected ResponseWrapper<Void> executeVoid(Runnable operation) {
        String traceId = TraceIdUtil.getCurrent();
        try {
            operation.run();
            log.debug("操作执行成功: traceId={}", traceId);
            return success();
        } catch (Exception e) {
            log.error("操作执行失败: traceId={}, error={}", traceId, e.getMessage(), e);
            return error(ErrorCode.SYSTEM_ERROR, "操作失败: " + e.getMessage());
        }
    }

    /**
     * 验证请求参数
     */
    protected void validateRequest(@Valid Object request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                    .orElse("参数校验失败");

            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * 验证分页参数
     */
    protected PageRequest validatePageRequest(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        pageRequest.validate();
        return pageRequest;
    }

    /**
     * 验证分页参数（带排序）
     */
    protected PageRequest validatePageRequest(int page, int size, String sort, String direction) {
        PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
        pageRequest.validate();
        return pageRequest;
    }

    /**
     * 检查字符串是否为空
     */
    protected boolean isEmpty(String str) {
        return !StringUtils.hasText(str);
    }

    /**
     * 检查集合是否为空
     */
    protected boolean isEmpty(List<?> list) {
        return CollectionUtils.isEmpty(list);
    }

    /**
     * 检查对象是否为空
     */
    protected boolean isEmpty(Object obj) {
        return obj == null;
    }

    /**
     * 获取请求头信息
     */
    protected String getHeader(String headerName,
                              @RequestHeader(required = false) String headerValue) {
        return headerValue;
    }

    /**
     * 记录操作日志
     */
    protected void logOperation(String operation, Object... params) {
        String traceId = TraceIdUtil.getCurrent();
        log.info("操作日志: operation={}, params={}, traceId={}", operation, params, traceId);
    }

    /**
     * 记录调试信息
     */
    protected void logDebug(String message, Object... params) {
        String traceId = TraceIdUtil.getCurrent();
        log.debug("调试信息: message={}, params={}, traceId={}", message, params, traceId);
    }

    /**
     * 记录警告信息
     */
    protected void logWarn(String message, Object... params) {
        String traceId = TraceIdUtil.getCurrent();
        log.warn("警告信息: message={}, params={}, traceId={}", message, params, traceId);
    }

    /**
     * 记录错误信息
     */
    protected void logError(String message, Throwable throwable) {
        String traceId = TraceIdUtil.getCurrent();
        log.error("错误信息: message={}, traceId={}", message, traceId, throwable);
    }

    // ==================== RESTful API 标准响应方法 ====================

    /**
     * GET /resource - 获取资源列表
     */
    protected <T> ResponseWrapper<List<T>> listAll(Supplier<List<T>> supplier) {
        return execute(supplier);
    }

    /**
     * GET /resource/{id} - 获取单个资源
     */
    protected <T> ResponseWrapper<T> getById(Supplier<T> supplier) {
        return execute(supplier);
    }

    /**
     * GET /resource/page - 分页获取资源
     */
    protected <T> ResponseWrapper<PageResult<T>> page(Supplier<PageResult<T>> supplier) {
        return execute(supplier);
    }

    /**
     * POST /resource - 创建资源
     */
    protected <T> ResponseWrapper<T> create(Supplier<T> supplier) {
        return execute(supplier);
    }

    /**
     * PUT /resource/{id} - 更新资源
     */
    protected <T> ResponseWrapper<T> update(Supplier<T> supplier) {
        return execute(supplier);
    }

    /**
     * DELETE /resource/{id} - 删除资源
     */
    protected ResponseWrapper<Void> delete(Runnable runnable) {
        return executeVoid(runnable);
    }
}