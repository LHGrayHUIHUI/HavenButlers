package com.haven.filemanager;

import com.haven.base.config.BaseModelAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 文件管理服务启动类
 *
 * @author HavenButler
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import({BaseModelAutoConfiguration.class, })
public class FileManagerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileManagerServiceApplication.class, args);
    }
}