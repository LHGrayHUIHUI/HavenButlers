package com.haven.storage.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO客户端配置
 *
 * 功能特性：
 * - MinIO客户端自动配置
 * - 连接参数验证
 * - 健康检查支持
 * - 条件装配（仅当存储类型为minio时生效）
 *
 * @author HavenButler
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "storage.file.storage-type", havingValue = "minio", matchIfMissing = false)
public class MinIOConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.secure:false}")
    private boolean secure;

    /**
     * 创建MinIO客户端Bean
     */
    @Bean
    public MinioClient minioClient() {
        try {
            // 参数验证
            if (endpoint == null || endpoint.trim().isEmpty()) {
                throw new IllegalArgumentException("MinIO endpoint不能为空");
            }
            if (accessKey == null || accessKey.trim().isEmpty()) {
                throw new IllegalArgumentException("MinIO access-key不能为空");
            }
            if (secretKey == null || secretKey.trim().isEmpty()) {
                throw new IllegalArgumentException("MinIO secret-key不能为空");
            }

            // 创建MinIO客户端
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 测试连接
            try {
                client.listBuckets();
                log.info("MinIO客户端初始化成功：endpoint={}, secure={}", endpoint, secure);
            } catch (Exception e) {
                log.warn("MinIO连接测试失败，但客户端已创建：{}", e.getMessage());
            }

            return client;

        } catch (Exception e) {
            log.error("MinIO客户端初始化失败：{}", e.getMessage());
            throw new RuntimeException("MinIO客户端初始化失败", e);
        }
    }
}