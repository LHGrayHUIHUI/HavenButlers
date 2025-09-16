package com.haven.storage;

import com.haven.base.config.BaseModelAutoConfiguration;
import com.haven.common.config.CommonAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 存储服务启动类
 *
 * @author HavenButler
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import({BaseModelAutoConfiguration.class, CommonAutoConfiguration.class})
public class StorageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageServiceApplication.class, args);
    }
}