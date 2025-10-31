package com.haven.account.config;

import com.haven.account.security.AccountAuthFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 账户服务安全配置
 * 注册账户服务专用的认证过滤器
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class SecurityConfig {

    /**
     * 注册账户服务专用的认证过滤器
     * 设置最高优先级，确保优先于默认过滤器执行
     */
    @Bean
    @Order(1) // 设置最高优先级
    public FilterRegistrationBean<AccountAuthFilter> accountAuthFilterRegistration(AccountAuthFilter accountAuthFilter) {
        FilterRegistrationBean<AccountAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(accountAuthFilter);
        registration.setName("accountAuthFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(1); // 过滤器执行顺序
        registration.setAsyncSupported(true);

        log.info("账户服务认证过滤器已注册 - 优先级: 1");
        return registration;
    }
}