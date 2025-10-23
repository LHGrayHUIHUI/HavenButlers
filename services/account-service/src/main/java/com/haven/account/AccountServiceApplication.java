package com.haven.account;

import com.haven.base.config.BaseModelAutoConfiguration;
import com.haven.common.config.CommonAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * Account Service 启动类
 *
 * 负责用户认证授权、家庭权限管理、设备访问控制
 * 作为HavenButler平台的安全第一道防线
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import({BaseModelAutoConfiguration.class, CommonAutoConfiguration.class})
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
        System.out.println("===============================================");
        System.out.println("🏠 HavenButler 账户服务启动成功！");
        System.out.println("🔧 服务功能：用户认证授权、家庭权限管理、设备访问控制");
        System.out.println("📊 健康检查：http://localhost:8082/actuator/health");
        System.out.println("📖 API文档：http://localhost:8082/swagger-ui.html");
        System.out.println("===============================================");
    }
}