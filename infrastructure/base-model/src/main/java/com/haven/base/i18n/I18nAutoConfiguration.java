package com.haven.base.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;

import java.util.Locale;
import java.util.TimeZone;

/**
 * 国际化自动配置
 * 提供多语言支持的核心配置
 *
 * @author HavenButler
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({I18nProperties.class})
public class I18nAutoConfiguration {

    /**
     * 配置消息源
     */
    @Bean
    public ReloadableResourceBundleMessageSource messageSource(I18nProperties properties, Environment environment) {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        // 设置基础名称
        messageSource.setBasenames("classpath:i18n/messages", "classpath:i18n/validation", "classpath:i18n/error");

        // 设置编码
        messageSource.setDefaultEncoding("UTF-8");

        // 设置缓存时间
        messageSource.setCacheSeconds(properties.getCacheSeconds());

        // 设置是否始终使用消息格式
        messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());

        // 设置是否回退到系统locale
        messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());

        // 设置是否使用代码作为默认消息
        messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());

        log.info(" HavenBase 国际化支持已启用 - 默认语言: {}, 支持语言: {}",
                properties.getDefaultLocale(), properties.getSupportedLocales());

        return messageSource;
    }

    /**
     * 配置国际化工具类
     */
    @Bean
    public I18nUtil i18nUtil(ReloadableResourceBundleMessageSource messageSource, I18nProperties properties) {
        return new I18nUtil(messageSource, properties);
    }

    /**
     * 配置区域解析器
     */
    @Bean
    public AcceptHeaderLocaleResolver localeResolver(I18nProperties properties) {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag(properties.getDefaultLocale()));
        resolver.setSupportedLocales(properties.getSupportedLocaleList());
        return resolver;
    }

    /**
     * 配置时区
     */
    @Bean
    public TimeZone timeZone(I18nProperties properties) {
        return TimeZone.getTimeZone(properties.getDefaultTimeZone());
    }
}