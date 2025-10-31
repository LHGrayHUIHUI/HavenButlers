package com.haven.storage;

import com.haven.base.config.BaseModelAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * 存储服务启动类
 *
 * @author HavenButler
 */
@SpringBootApplication
@Import(BaseModelAutoConfiguration.class)
public class StorageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageServiceApplication.class, args);
        System.out.println("===============================================");
        System.out.println("📁 HavenButler 存储服务启动成功！");
        System.out.println("🔧 服务功能：文件存储管理、图片处理、缩略图生成、分享管理");
        System.out.println("📊 健康检查：http://localhost:8081/actuator/health");
        System.out.println("📖 API文档：http://localhost:8081/swagger-ui.html");
        System.out.println("===============================================");
    }
}