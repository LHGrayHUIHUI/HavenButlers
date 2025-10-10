package com.haven.admin.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置
 *
 * 作用：
 * - 提供 OpenAPI 元信息（标题/版本/描述）
 * - 配置分组与包扫描，便于按模块/前缀查看
 * - 与 Spring Security 配合：默认需要登录访问文档
 *
 * 注意：
 * - 生产环境默认通过 springdoc 开关禁用（见 application-docker.yml）
 * - 如需在生产内网开放，请结合网关/ACL/白名单控制
 * - 文档访问地址：
 *   - Swagger UI: /swagger-ui/index.html
 *   - OpenAPI JSON: /v3/api-docs
 *
 * 认证说明：
 * - Admin 服务使用 Spring Security 基础认证（HTTP Basic Auth）
 * - 访问文档需要先登录（账户名：admin，密码：配置中的密码）
 * - 如需在开发环境免登录，可在 SecurityConfig 中白名单放行 /swagger-ui/**, /v3/api-docs/**
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * 全局 OpenAPI 元信息
     *
     * 返回 /v3/api-docs 的基础信息（标题/版本/描述/License/外链）
     *
     * 包含内容：
     * - API 标题和描述
     * - 版本信息
     * - 许可证信息
     * - 外部文档链接
     *
     * @return OpenAPI 元信息对象
     */
    @Bean
    public OpenAPI adminServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HavenButler Admin API")
                        .description("管理端服务接口文档（默认需登录访问）\n\n" +
                                "## 认证方式\n" +
                                "Admin 服务使用 Spring Security 基础认证（HTTP Basic Auth）\n\n" +
                                "## 功能模块\n" +
                                "- 服务管理：服务注册、健康检查、指标采集\n" +
                                "- 告警管理：告警规则配置、告警记录查询\n" +
                                "- 环境管理：配置管理、环境切换\n" +
                                "- 健康监控：实时健康状态、SSE 推送")
                        .version("v1.0.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("HavenButler 项目文档")
                        .url("https://github.com/yourusername/HavenButler"));
    }

    /**
     * 管理端 API 分组
     *
     * 仅扫描 com.haven.admin.controller 下的接口
     * 访问 UI 时可在分组下拉中选择 "admin"
     *
     * 扫描规则：
     * - 包路径：com.haven.admin.controller
     * - URL 路径：/api/**
     *
     * @return 管理端 API 分组配置
     */
    @Bean
    public GroupedOpenApi adminApiGroup() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("管理端 API")
                .packagesToScan("com.haven.admin.controller")
                .pathsToMatch("/api/**")
                .build();
    }

    /**
     * 服务管理 API 分组
     *
     * 专门针对服务管理相关的接口
     * 包括：服务注册、健康检查、指标采集等
     *
     * @return 服务管理 API 分组配置
     */
    @Bean
    public GroupedOpenApi serviceManageApiGroup() {
        return GroupedOpenApi.builder()
                .group("service-manage")
                .displayName("服务管理")
                .packagesToScan("com.haven.admin.controller")
                .pathsToMatch("/api/service/**", "/api/services/**")
                .build();
    }

    /**
     * 告警管理 API 分组
     *
     * 专门针对告警管理相关的接口
     * 包括：告警规则、告警记录、告警通知等
     *
     * @return 告警管理 API 分组配置
     */
    @Bean
    public GroupedOpenApi alertApiGroup() {
        return GroupedOpenApi.builder()
                .group("alert")
                .displayName("告警管理")
                .packagesToScan("com.haven.admin.controller")
                .pathsToMatch("/api/alert/**", "/api/alerts/**")
                .build();
    }

    /**
     * 健康监控 API 分组
     *
     * 专门针对健康监控相关的接口
     * 包括：健康状态查询、SSE 推送等
     *
     * @return 健康监控 API 分组配置
     */
    @Bean
    public GroupedOpenApi healthApiGroup() {
        return GroupedOpenApi.builder()
                .group("health")
                .displayName("健康监控")
                .packagesToScan("com.haven.admin.controller")
                .pathsToMatch("/api/service/overview/**", "/api/service/stream/**")
                .build();
    }
}