package com.haven.storage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 存储服务配置类
 *
 * @author HavenButler
 */
@Configuration
public class StorageConfiguration {



    /**
     * RestTemplate配置（用于向量数据库HTTP调用）
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}