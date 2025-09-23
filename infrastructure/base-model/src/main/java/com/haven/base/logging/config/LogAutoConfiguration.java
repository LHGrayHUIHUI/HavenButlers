package com.haven.base.logging.config;

import com.haven.base.logging.aspect.LogAspect;
import com.haven.base.logging.client.LogClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

/**
 * 日志系统自动配置类
 * 其他微服务引入base-model依赖后，自动配置日志功能
 *
 * 配置属性：
 * - haven.logging.enabled: 是否启用日志系统（默认true）
 * - haven.logging.async: 是否启用异步日志（默认true）
 * - storage.service.url: storage-service的地址
 *
 * @author HavenButler
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "haven.logging.enabled", havingValue = "true", matchIfMissing = true)
@Import({LogAsyncConfig.class})
public class LogAutoConfiguration {

    /**
     * 自动配置LogClient
     */
    @Bean
    @ConditionalOnMissingBean
    public LogClient logClient(RestTemplate restTemplate) {
        log.info("自动配置LogClient - HavenButler统一日志系统");
        return new LogClient();
    }

    /**
     * 自动配置日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(name = "haven.logging.aspect.enabled", havingValue = "true", matchIfMissing = true)
    public LogAspect logAspect() {
        log.info("自动配置LogAspect - 启用@LogOperation注解支持");
        return new LogAspect();
    }

    /**
     * 自动配置RestTemplate（如果不存在）
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        log.info("自动配置RestTemplate - 用于日志系统通信");
        RestTemplate restTemplate = new RestTemplate();

        // 设置连接超时和读取超时
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("User-Agent", "HavenButler-LogClient/1.0");
            return execution.execute(request, body);
        });

        return restTemplate;
    }
}