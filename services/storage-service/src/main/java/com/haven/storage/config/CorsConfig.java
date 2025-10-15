package com.haven.storage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局CORS配置
 * <p>
 * 🎯 功能特性：
 * - 统一管理所有API的跨域访问策略
 * - 支持开发环境和生产环境的不同配置
 * - 提供灵活的源、方法、头部控制
 * <p>
 * 💡 配置说明：
 * - 开发环境：允许所有源，便于本地调试
 * - 生产环境：建议指定具体的前端域名
 * - 支持凭证传递：可根据需要开启Cookie/Authorization
 * <p>
 * 🔧 使用方式：
 * - 自动应用到所有控制器接口
 * - 优先级低于@CrossOrigin注解
 * - 可与具体Controller的注解共存
 *
 * @author HavenButler
 */
@Configuration
public class CorsConfig {

    /**
     * 配置全局CORS过滤器
     * <p>
     * 提供统一的跨域访问策略，覆盖所有API端点
     *
     * @return 配置好的CORS过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建CORS配置
        CorsConfiguration config = new CorsConfiguration();

        // 2. 配置允许的源（重要：安全考虑）
        // 开发环境配置 - 允许常用本地开发端口
        config.addAllowedOrigin("http://localhost:8080");    // 本地静态服务器
        config.addAllowedOrigin("http://localhost:63342");   // IDE内置服务器
        config.addAllowedOrigin("http://127.0.0.1:8080");    // 本地IP访问
        config.addAllowedOrigin("http://127.0.0.1:63342");   // 本地IP访问

        // 生产环境应该使用具体域名，例如：
        // config.addAllowedOrigin("https://your-frontend-domain.com");

        // 3. 配置允许的HTTP方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("PATCH");

        // 4. 配置允许的请求头
        config.addAllowedHeader("*");

        // 5. 配置是否允许凭证（Cookie、Authorization等）
        // 注意：当allowCredentials=true时，不能使用通配符源
        config.setAllowCredentials(false);

        // 6. 配置预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        // 7. 配置暴露的响应头（可选）
        // config.addExposedHeader("X-Total-Count");
        // config.addExposedHeader("X-Trace-Id");

        // 8. 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}