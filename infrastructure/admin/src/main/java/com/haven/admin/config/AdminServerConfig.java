package com.haven.admin.config;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot Admin服务端配置
 *
 * @author HavenButler
 */
@Configuration
@EnableAdminServer
public class AdminServerConfig {
    // Spring Boot Admin Server 的基本配置
    // 安全配置已移至 SecurityConfig.java
}