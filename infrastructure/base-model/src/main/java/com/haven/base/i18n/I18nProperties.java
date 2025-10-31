package com.haven.base.i18n;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 国际化配置属性
 * 控制国际化行为和特性
 *
 * @author HavenButler
 */
@Data
@ConfigurationProperties(prefix = "haven.base.i18n")
public class I18nProperties {

    /**
     * 是否启用国际化
     */
    private boolean enabled = true;

    /**
     * 默认语言
     */
    private String defaultLocale = "zh-CN";

    /**
     * 支持的语言列表
     */
    private List<String> supportedLocales = new ArrayList<>();

    /**
     * 默认时区
     */
    private String defaultTimeZone = "Asia/Shanghai";

    /**
     * 消息缓存时间（秒）
     */
    private int cacheSeconds = 3600;

    /**
     * 是否总是使用消息格式
     */
    private boolean alwaysUseMessageFormat = true;

    /**
     * 是否回退到系统locale
     */
    private boolean fallbackToSystemLocale = true;

    /**
     * 是否使用代码作为默认消息
     */
    private boolean useCodeAsDefaultMessage = false;

    /**
     * 语言参数名
     */
    private String languageParameter = "lang";

    /**
     * 是否支持动态切换语言
     */
    private boolean dynamicSwitching = true;

    /**
     * 是否在响应头中包含语言信息
     */
    private boolean includeLanguageInHeader = true;

    /**
     * 语言响应头名称
     */
    private String languageHeaderName = "Content-Language";

    /**
     * 配置初始化
     */
    public I18nProperties() {
        // 默认支持中文和英文
        supportedLocales.add("zh-CN");
        supportedLocales.add("en-US");
    }

    /**
     * 获取支持的语言列表
     */
    public List<Locale> getSupportedLocaleList() {
        List<Locale> locales = new ArrayList<>();
        for (String localeStr : supportedLocales) {
            locales.add(Locale.forLanguageTag(localeStr));
        }
        return locales;
    }

    /**
     * 检查是否支持指定语言
     */
    public boolean isSupported(String locale) {
        return supportedLocales.contains(locale);
    }

    /**
     * 获取默认Locale对象
     */
    public Locale getDefaultLocaleObj() {
        return Locale.forLanguageTag(defaultLocale);
    }
}