package com.haven.base.common.exception;

import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.utils.FileSizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回标准格式响应
 *
 * @author HavenButler
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResponseWrapper<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常 - Code: {}, Message: {}, TraceID: {}",
                e.getCode(), e.getMessage(), e.getTraceId());
        return ResponseWrapper.error(e.getCode(), e.getMessage(), e.getData());
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseWrapper<?> handleSystemException(SystemException e) {
        log.error("系统异常 - Code: {}, Message: {}, TraceID: {}",
                e.getCode(), e.getMessage(), e.getTraceId(), e);
        return ResponseWrapper.error(e.getCode(), "系统繁忙，请稍后再试");
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseWrapper<?> handleAuthException(AuthException e) {
        log.warn("认证异常 - Code: {}, Message: {}, TraceID: {}",
                e.getCode(), e.getMessage(), e.getTraceId());
        return ResponseWrapper.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<?> handleValidationException(ValidationException e) {
        log.warn("参数校验异常 - Code: {}, Message: {}, TraceID: {}",
                e.getCode(), e.getMessage(), e.getTraceId());
        return ResponseWrapper.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理方法参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        String message = String.join(", ", errors);
        log.warn("参数校验失败: {}", message);
        return ResponseWrapper.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<?> handleBindException(BindException e) {
        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        String message = String.join(", ", errors);
        log.warn("参数绑定失败: {}", message);
        return ResponseWrapper.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<?> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("约束校验失败: {}", message);
        return ResponseWrapper.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = String.format("缺少必要参数: %s", e.getParameterName());
        log.warn("缺少请求参数: {}", e.getParameterName());
        return ResponseWrapper.error(ErrorCode.PARAM_MISSING.getCode(), message);
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseWrapper<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = String.format("不支持的请求方法: %s", e.getMethod());
        log.warn("请求方法不支持: {}", e.getMethod());
        return ResponseWrapper.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseWrapper<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String message = String.format("接口不存在: %s", e.getRequestURL());
        log.warn("接口不存在: {}", e.getRequestURL());
        return ResponseWrapper.error(ErrorCode.DATA_NOT_FOUND.getCode(), message);
    }

    /**
     * 处理文件上传大小超限异常
     * <p>
     * 使用FileSizeUtil工具类格式化文件大小显示，提供更友好的错误消息
     * FileSizeUtil现在已经内置了对异常大小值（maxSize <= 0）的处理逻辑
     *
     * @param e 文件上传大小超限异常
     * @return 错误响应，包含格式化的文件大小限制信息
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        long maxSize = e.getMaxUploadSize();
        // 使用FileSizeUtil工具类格式化文件大小显示
        // 现在FileSizeUtil.formatFileSize已经内置了对maxSize <= 0的处理逻辑
        String sizeStr = FileSizeUtil.formatFileSize(maxSize);
        // 记录日志，包含原始大小值和格式化后的结果
        log.warn("文件上传大小超限: {} bytes ({})", maxSize, sizeStr);

        return ResponseWrapper.error(ErrorCode.FILE_SIZE_EXCEED.getCode(), String.format("文件大小超出限制，最大允许: %s", sizeStr));
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseWrapper<?> handleException(Exception e) {
        log.error("未知异常", e);
        return ResponseWrapper.error(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 处理Throwable
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseWrapper<?> handleThrowable(Throwable e) {
        log.error("系统错误", e);
        return ResponseWrapper.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误，请联系管理员");
    }
}