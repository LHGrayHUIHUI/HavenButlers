package com.haven.base.aspect;

import com.haven.base.i18n.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化切面
 * 自动处理请求的语言环境设置和清理
 *
 * @author HavenButler
 */
@Slf4j
@Aspect
@Component
@ConditionalOnWebApplication
public class I18nAspect {

    @Autowired
    private I18nUtil i18nUtil;

    /**
     * 环绕通知 - 处理Controller方法的国际化
     * 注意：这里简化实现，实际Web环境中的语言解析需要通过Filter或Interceptor处理
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object handleI18n(ProceedingJoinPoint joinPoint) throws Throwable {
        // 设置默认语言环境
        Locale locale = i18nUtil.getDefaultLocale();
        i18nUtil.setCurrentLocale(locale);

        try {
            log.debug("设置请求语言环境: {} (默认语言)", locale);
            return joinPoint.proceed();
        } finally {
            // 清理当前线程的语言环境
            i18nUtil.clearCurrentLocale();
            log.debug("清理请求语言环境: {}", locale);
        }
    }
}