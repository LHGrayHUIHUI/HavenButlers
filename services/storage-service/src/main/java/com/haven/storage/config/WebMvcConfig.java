package com.haven.storage.config;

import com.haven.storage.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类 - 注册拦截器
 * <p>
 * 功能：
 * - 注册存储服务拦截器
 * - 配置拦截路径
 * - 设置拦截器执行顺序
 *
 * @author HavenButler
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("注册存储服务拦截器");
        // 注册拦截器，并配置拦截路径
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/api/v1/storage/**")  // 拦截所有存储API路径
                .excludePathPatterns(
                        "/api/v1/storage/actuator/**",    // 排除监控端点
                        "/api/v1/storage/health",        // 排除健康检查
                        "/api/v1/storage/metrics",       // 排除指标查询
                        "/swagger-*/**",                // 排除Swagger文档
                        "/v3/api-docs/**"             // 排除OpenAPI文档
                )
                .order(1);  // 设置拦截器执行顺序
    }
}