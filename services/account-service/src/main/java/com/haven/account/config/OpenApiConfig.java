package com.haven.account.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Account Service OpenAPI 配置
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * 全局 OpenAPI 元信息配置
     */
    @Bean
    public OpenAPI accountServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HavenButler Account Service API")
                        .description("HavenButler 智能家庭平台账户管理服务")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("HavenButler Team")
                                .email("support@havenbutler.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    /**
     * 家庭管理接口分组
     */
    @Bean
    public GroupedOpenApi familyApiGroup() {
        return GroupedOpenApi.builder()
                .group("family")
                .displayName("家庭管理")
                .pathsToMatch("/api/v1/family/**")
                .build();
    }
    /**
     * 认证接口分组
     */
    @Bean
    public GroupedOpenApi authApiGroup() {
        return GroupedOpenApi.builder()
                .group("auth")
                .displayName("认证接口")
                .packagesToScan("com.haven.account.controller")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    /**
     * 健康检查接口分组
     */
    @Bean
    public GroupedOpenApi healthApiGroup() {
        return GroupedOpenApi.builder()
                .group("health")
                .displayName("健康检查")
                .pathsToMatch("/actuator/**")
                .build();
    }
}