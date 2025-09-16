package com.haven.storage.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.access-key}")
    private String minioAccessKey;

    @Value("${minio.secret-key}")
    private String minioSecretKey;

    /**
     * MinIO客户端配置
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    /**
     * RestTemplate配置（用于向量数据库HTTP调用）
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}