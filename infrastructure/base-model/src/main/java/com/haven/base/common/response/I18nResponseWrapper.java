package com.haven.base.common.response;

import com.haven.base.i18n.I18nUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 国际化响应包装器
 * 支持多语言响应消息
 *
 * @author HavenButler
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class I18nResponseWrapper<T> extends ResponseWrapper<T> {

    // Getter and Setter
    /**
     * 语言信息
     */
    private String language;

    /**
     * 语言标签
     */
    private String locale;

    /**
     * 私有构造函数
     */
    private I18nResponseWrapper() {
        super();
    }

    /**
     * 创建成功响应（国际化）
     */
    public static <T> I18nResponseWrapper<T> success(I18nUtil i18nUtil, T data) {
        return success(i18nUtil, null, data, (Locale) null);
    }

    /**
     * 创建成功响应（国际化，带消息）
     */
    public static <T> I18nResponseWrapper<T> success(I18nUtil i18nUtil, String messageKey, T data) {
        return success(i18nUtil, messageKey, data, (Object[]) null);
    }

    /**
     * 创建成功响应（国际化，带消息和参数）
     */
    public static <T> I18nResponseWrapper<T> success(I18nUtil i18nUtil, String messageKey, T data, Object[] args) {
        String message = messageKey != null ? i18nUtil.getMessage(messageKey, args) : i18nUtil.getMessage("success.operation");
        I18nResponseWrapper<T> response = new I18nResponseWrapper<>();
        response.setCode(0);
        response.setMessage(message);
        response.setData(data);
        response.setLanguage(i18nUtil.getCurrentLocale().toLanguageTag());
        response.setLocale(i18nUtil.getCurrentLocale().getDisplayName());
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * 创建成功响应（指定语言）
     */
    public static <T> I18nResponseWrapper<T> success(I18nUtil i18nUtil, String messageKey, T data, Locale locale) {
        String message = messageKey != null ? i18nUtil.getMessage(messageKey, null, locale) : i18nUtil.getMessage("success.operation", null, locale);
        I18nResponseWrapper<T> response = new I18nResponseWrapper<>();
        response.setCode(0);
        response.setMessage(message);
        response.setData(data);
        response.setLanguage(locale != null ? locale.toLanguageTag() : i18nUtil.getCurrentLocale().toLanguageTag());
        response.setLocale(locale != null ? locale.getDisplayName() : i18nUtil.getCurrentLocale().getDisplayName());
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * 创建失败响应（国际化）
     */
    public static I18nResponseWrapper<?> error(I18nUtil i18nUtil, ErrorCode errorCode) {
        String message = i18nUtil.getErrorMessage(String.valueOf(errorCode.getCode()), null);
        I18nResponseWrapper<Object> response = new I18nResponseWrapper<>();
        response.setCode(errorCode.getCode());
        response.setMessage(message);
        response.setLanguage(i18nUtil.getCurrentLocale().toLanguageTag());
        response.setLocale(i18nUtil.getCurrentLocale().getDisplayName());
        response.setTimestamp(java.time.LocalDateTime.now());
        return response;
    }

    /**
     * 创建失败响应（国际化，带自定义消息参数）
     */
    public static I18nResponseWrapper<?> error(I18nUtil i18nUtil, ErrorCode errorCode, Object[] args) {
        String message = i18nUtil.getErrorMessage(String.valueOf(errorCode.getCode()), args);
        I18nResponseWrapper<Object> response = new I18nResponseWrapper<>();
        response.setCode(errorCode.getCode());
        response.setMessage(message);
        response.setLanguage(i18nUtil.getCurrentLocale().toLanguageTag());
        response.setLocale(i18nUtil.getCurrentLocale().getDisplayName());
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * 创建失败响应（国际化，指定语言）
     */
    public static I18nResponseWrapper<?> error(I18nUtil i18nUtil, ErrorCode errorCode, Locale locale) {
        String message = i18nUtil.getErrorMessage(String.valueOf(errorCode.getCode()), null, locale);
        I18nResponseWrapper<Object> response = new I18nResponseWrapper<>();
        response.setCode(errorCode.getCode());
        response.setMessage(message);
        response.setLanguage(locale != null ? locale.toLanguageTag() : i18nUtil.getCurrentLocale().toLanguageTag());
        response.setLocale(locale != null ? locale.getDisplayName() : i18nUtil.getCurrentLocale().getDisplayName());
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * 转换为标准ResponseWrapper（用于兼容）
     */
    public ResponseWrapper<T> toResponseWrapper() {
        ResponseWrapper<T> wrapper = new ResponseWrapper<>();
        wrapper.setCode(this.getCode());
        wrapper.setMessage(this.getMessage());
        wrapper.setData(this.getData());
        wrapper.setTraceId(this.getTraceId());
        wrapper.setTimestamp(this.getTimestamp());
        return wrapper;
    }
}