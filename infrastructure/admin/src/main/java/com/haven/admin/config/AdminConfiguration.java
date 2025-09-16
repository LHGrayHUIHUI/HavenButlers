package com.haven.admin.config;

import com.haven.admin.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Admin服务配置
 *
 * @author HavenButler
 */
@Slf4j
@Configuration
@EnableScheduling
public class AdminConfiguration {

    @PostConstruct
    public void init() {
        log.info("Admin管理服务配置初始化");
        initDefaultAlertRules();
    }

    /**
     * RestTemplate配置
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 初始化默认告警规则
     */
    private void initDefaultAlertRules() {
        AlertService alertService = alertService();

        // CPU使用率告警
        alertService.addAlertRule(AlertService.AlertRule.builder()
                .ruleId("cpu-high")
                .ruleName("CPU使用率过高")
                .metricName("system.cpu.usage")
                .operator(AlertService.ComparisonOperator.GREATER_THAN)
                .threshold(0.8)
                .level(AlertService.AlertLevel.WARNING)
                .messageTemplate("CPU使用率达到%.1f%%")
                .build());

        // 内存使用率告警
        alertService.addAlertRule(AlertService.AlertRule.builder()
                .ruleId("memory-high")
                .ruleName("内存使用率过高")
                .metricName("system.memory.usage")
                .operator(AlertService.ComparisonOperator.GREATER_THAN)
                .threshold(0.85)
                .level(AlertService.AlertLevel.WARNING)
                .messageTemplate("内存使用率达到%.1f%%")
                .build());

        // 磁盘使用率告警
        alertService.addAlertRule(AlertService.AlertRule.builder()
                .ruleId("disk-high")
                .ruleName("磁盘使用率过高")
                .metricName("disk.usage")
                .operator(AlertService.ComparisonOperator.GREATER_THAN)
                .threshold(0.9)
                .level(AlertService.AlertLevel.CRITICAL)
                .messageTemplate("磁盘使用率达到%.1f%%")
                .build());

        // API错误率告警
        alertService.addAlertRule(AlertService.AlertRule.builder()
                .ruleId("api-error-rate")
                .ruleName("API错误率过高")
                .metricName("api.error.rate")
                .operator(AlertService.ComparisonOperator.GREATER_THAN)
                .threshold(0.05)
                .level(AlertService.AlertLevel.WARNING)
                .messageTemplate("API错误率达到%.1f%%")
                .build());

        // 响应时间告警
        alertService.addAlertRule(AlertService.AlertRule.builder()
                .ruleId("response-slow")
                .ruleName("响应时间过长")
                .metricName("http.response.time")
                .operator(AlertService.ComparisonOperator.GREATER_THAN)
                .threshold(2000)
                .level(AlertService.AlertLevel.WARNING)
                .messageTemplate("平均响应时间达到%.0fms")
                .build());

        log.info("默认告警规则初始化完成");
    }

    @Bean
    public AlertService alertService() {
        return new AlertService();
    }

    /**
     * 解决Spring Boot Admin与Actuator端点映射问题
     */
    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(
            WebEndpointsSupplier webEndpointsSupplier,
            ServletEndpointsSupplier servletEndpointsSupplier,
            ControllerEndpointsSupplier controllerEndpointsSupplier,
            EndpointMediaTypes endpointMediaTypes,
            CorsEndpointProperties corsProperties,
            WebEndpointProperties webEndpointProperties,
            Environment environment) {

        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());

        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);

        boolean shouldRegisterLinksMapping = shouldRegisterLinksMapping(
                webEndpointProperties, environment, basePath);

        return new WebMvcEndpointHandlerMapping(
                endpointMapping,
                webEndpoints,
                endpointMediaTypes,
                corsProperties.toCorsConfiguration(),
                new EndpointLinksResolver(allEndpoints, basePath),
                shouldRegisterLinksMapping);
    }

    private boolean shouldRegisterLinksMapping(WebEndpointProperties webEndpointProperties,
                                               Environment environment, String basePath) {
        return webEndpointProperties.getDiscovery().isEnabled() &&
                (StringUtils.hasText(basePath) ||
                        ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
    }
}