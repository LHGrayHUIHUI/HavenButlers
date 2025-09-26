package com.haven.admin;

import com.haven.base.config.BaseModelAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import de.codecentric.boot.admin.server.config.EnableAdminServer;

/**
 * Admin管理服务启动类
 *
 * 功能：
 * - 系统管理和运维监控中心
 * - 集成Nacos服务发现和配置管理
 * - 提供Spring Boot Admin监控界面
 * - 支持Prometheus指标收集
 * - 提供服务管理和告警功能
 *
 * @author HavenButler
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@Import(BaseModelAutoConfiguration.class)
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}