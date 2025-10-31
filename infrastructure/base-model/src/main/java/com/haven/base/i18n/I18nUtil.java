package com.haven.base.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 国际化工具类
 * 提供多语言消息获取和格式化功能
 *
 * @author HavenButler
 */
@Slf4j
public class I18nUtil {

    private final MessageSource messageSource;
    private final I18nProperties properties;
    private final ThreadLocal<Locale> currentLocale = new ThreadLocal<>();

    public I18nUtil(MessageSource messageSource, I18nProperties properties) {
        this.messageSource = messageSource;
        this.properties = properties;
    }

    /**
     * 获取国际化消息（使用默认语言）
     */
    public String getMessage(String code) {
        return getMessage(code, null, getDefaultLocale());
    }

    /**
     * 获取国际化消息（使用默认语言）
     */
    public String getMessage(String code, Object[] args) {
        return getMessage(code, args, getDefaultLocale());
    }

    /**
     * 获取国际化消息
     */
    public String getMessage(String code, Locale locale) {
        return getMessage(code, null, locale);
    }

    /**
     * 获取国际化消息（带参数）
     */
    public String getMessage(String code, Object[] args, Locale locale) {
        try {
            if (locale == null) {
                locale = getDefaultLocale();
            }
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("未找到国际化消息: code={}, locale={}", code, locale);
            return properties.isUseCodeAsDefaultMessage() ? code : getDefaultMessage(code, args, locale);
        }
    }

    /**
     * 获取错误码对应的国际化消息
     */
    public String getErrorMessage(String errorCode) {
        return getMessage("error." + errorCode);
    }

    /**
     * 获取错误码对应的国际化消息（带参数）
     */
    public String getErrorMessage(String errorCode, Object[] args) {
        return getMessage("error." + errorCode, args);
    }

    /**
     * 获取错误码对应的国际化消息（指定语言）
     */
    public String getErrorMessage(String errorCode, Object[] args, Locale locale) {
        return getMessage("error." + errorCode, args, locale);
    }

    /**
     * 获取验证消息
     */
    public String getValidationMessage(String validationCode) {
        return getMessage("validation." + validationCode);
    }

    /**
     * 获取验证消息（带参数）
     */
    public String getValidationMessage(String validationCode, Object[] args) {
        return getMessage("validation." + validationCode, args);
    }

    /**
     * 获取配置消息
     */
    public String getConfigMessage(String configCode) {
        return getMessage("config." + configCode);
    }

    /**
     * 获取配置消息（带参数）
     */
    public String getConfigMessage(String configCode, Object[] args) {
        return getMessage("config." + configCode, args);
    }

    /**
     * 格式化消息
     */
    public String formatMessage(String template, Object... args) {
        try {
            return MessageFormat.format(template, args);
        } catch (Exception e) {
            log.warn("消息格式化失败: template={}, args={}", template, args, e);
            return template;
        }
    }

    /**
     * 设置当前线程的语言环境
     */
    public void setCurrentLocale(Locale locale) {
        if (properties.isDynamicSwitching() && properties.isSupported(locale.toLanguageTag())) {
            currentLocale.set(locale);
            log.debug("设置当前语言环境: {}", locale);
        }
    }

    /**
     * 设置当前线程的语言环境
     */
    public void setCurrentLocale(String languageTag) {
        setCurrentLocale(Locale.forLanguageTag(languageTag));
    }

    /**
     * 获取当前线程的语言环境
     */
    public Locale getCurrentLocale() {
        Locale locale = currentLocale.get();
        return locale != null ? locale : getDefaultLocale();
    }

    /**
     * 清除当前线程的语言环境
     */
    public void clearCurrentLocale() {
        currentLocale.remove();
    }

    /**
     * 获取默认语言环境
     */
    public Locale getDefaultLocale() {
        return properties.getDefaultLocaleObj();
    }

    /**
     * 检查是否支持指定语言
     */
    public boolean isSupported(Locale locale) {
        return properties.isSupported(locale.toLanguageTag());
    }

    /**
     * 获取支持的语言列表
     */
    public java.util.List<Locale> getSupportedLocales() {
        return properties.getSupportedLocaleList();
    }

    /**
     * 获取默认时区
     */
    public TimeZone getDefaultTimeZone() {
        return TimeZone.getTimeZone(properties.getDefaultTimeZone());
    }

    /**
     * 根据Accept-Language头解析语言环境
     */
    public Locale resolveLocaleFromAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.trim().isEmpty()) {
            return getDefaultLocale();
        }

        // 解析Accept-Language头
        String[] languages = acceptLanguage.split(",");
        for (String language : languages) {
            String[] parts = language.trim().split(";");
            String localeStr = parts[0].trim();

            Locale locale = Locale.forLanguageTag(localeStr);
            if (isSupported(locale)) {
                return locale;
            }
        }

        return getDefaultLocale();
    }

    /**
     * 获取本地化的日期时间格式
     */
    public String getDateTimePattern() {
        return getMessage("format.datetime", null, getCurrentLocale());
    }

    /**
     * 获取本地化的日期格式
     */
    public String getDatePattern() {
        return getMessage("format.date", null, getCurrentLocale());
    }

    /**
     * 获取本地化的时间格式
     */
    public String getTimePattern() {
        return getMessage("format.time", null, getCurrentLocale());
    }

    /**
     * 获取默认消息
     */
    private String getDefaultMessage(String code, Object[] args, Locale locale) {
        // 尝试使用英语作为后备
        if (!Locale.ENGLISH.equals(locale)) {
            try {
                return messageSource.getMessage(code, args, Locale.ENGLISH);
            } catch (NoSuchMessageException e) {
                // 继续尝试其他方式
            }
        }

        // 如果有参数，使用简单的格式化
        if (args != null && args.length > 0) {
            return formatMessage(code, args);
        }

        // 最后返回代码本身
        return code;
    }
}