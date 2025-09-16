package com.haven.ai;

import com.haven.base.config.BaseModelAutoConfiguration;
import com.haven.common.config.CommonAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * AI服务启动类
 *
 * @author HavenButler
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import({BaseModelAutoConfiguration.class, CommonAutoConfiguration.class})
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}