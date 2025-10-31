package com.haven.base.i18n;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

/**
 * 基于Accept头的语言环境解析器
 * 解析HTTP请求中的Accept-Language头
 *
 * @author HavenButler
 */
@Slf4j
public class AcceptHeaderLocaleResolver {

    private Locale defaultLocale;
    private List<Locale> supportedLocales;

    /**
     * 解析语言环境
     */
    public Locale resolveLocale(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.trim().isEmpty()) {
            return defaultLocale;
        }

        // 解析Accept-Language头
        String[] languages = acceptLanguage.split(",");
        for (String language : languages) {
            String[] parts = language.trim().split(";");
            String localeStr = parts[0].trim();

            Locale locale = Locale.forLanguageTag(localeStr);
            if (isSupported(locale)) {
                log.debug("解析到语言环境: {} (来自Accept-Language: {})", locale, acceptLanguage);
                return locale;
            }
        }

        log.debug("未找到支持的语言环境，使用默认语言: {}", defaultLocale);
        return defaultLocale;
    }

    /**
     * 检查是否支持指定语言
     */
    private boolean isSupported(Locale locale) {
        if (supportedLocales == null || supportedLocales.isEmpty()) {
            return false;
        }

        // 直接匹配
        if (supportedLocales.contains(locale)) {
            return true;
        }

        // 检查语言匹配（忽略国家）
        String language = locale.getLanguage();
        for (Locale supportedLocale : supportedLocales) {
            if (supportedLocale.getLanguage().equals(language)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 设置默认语言环境
     */
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    /**
     * 设置支持的语言列表
     */
    public void setSupportedLocales(List<Locale> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    /**
     * 获取默认语言环境
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * 获取支持的语言列表
     */
    public List<Locale> getSupportedLocales() {
        return supportedLocales;
    }
}