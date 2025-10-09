package com.haven.admin.config;

import com.haven.admin.model.AlertRule;
import com.haven.admin.service.AlertService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.Timeout;

import java.time.Duration;
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
public class AdminConfiguration {

    @Autowired
    private AlertService alertService;

    @PostConstruct
    public void init() {
        log.info("Admin管理服务配置初始化");
        // initDefaultAlertRules();
    }

    /**
     * RestTemplate 配置 - 使用 Apache HttpClient5 连接池
     *
     * 配置策略：
     * - 最大连接数：200（避免探测风暴）
     * - 每路由最大连接数：50
     * - 连接超时：2秒
     * - Socket 读取超时：3秒
     * - Keep-Alive：30秒
     *
     * @return 配置了连接池的 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        // 1. 配置连接管理器（连接池）
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();

        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        // 最大连接数
        connectionManager.setMaxTotal(200);
        // 每个路由（目标主机）的最大连接数
        connectionManager.setDefaultMaxPerRoute(50);
        // 连接空闲多久后验证
        connectionManager.setValidateAfterInactivity(Timeout.ofSeconds(2));

        // 2. 配置 HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 禁用自动重试（健康检查失败不应该重试）
                .disableAutomaticRetries()
                // Keep-Alive 策略
                .setKeepAliveStrategy((response, context) -> Timeout.ofSeconds(30))
                .build();

        // 3. 配置请求工厂
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // 连接超时：从连接池获取连接的超时时间
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        // Socket 超时：等待数据的超时时间
        requestFactory.setConnectionRequestTimeout(Duration.ofSeconds(3));

        // 4. 创建 RestTemplate
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        log.info("RestTemplate 配置完成 - 使用 Apache HttpClient5 连接池 " +
                "(最大连接: {}, 每路由: {}, 连接超时: 2s, 读取超时: 3s)",
                connectionManager.getMaxTotal(),
                connectionManager.getDefaultMaxPerRoute());

        return restTemplate;
    }

    /**
     * 初始化默认告警规则 - 暂时禁用
     */
    /*
    private void initDefaultAlertRules() {

        // CPU使用率告警
        AlertRule cpuRule = new AlertRule();
        cpuRule.setName("CPU使用率过高");
        cpuRule.setDescription("监控系统CPU使用率");
        cpuRule.setServiceName("system");
        cpuRule.setMetricName("system.cpu.usage");
        cpuRule.setOperator(AlertRule.Operator.GREATER_THAN);
        cpuRule.setThreshold(80.0);
        cpuRule.setLevel(AlertRule.AlertLevel.WARNING);
        cpuRule.setMessageTemplate("CPU使用率达到%.1f%%");
        cpuRule.setNotifyType(AlertRule.NotifyType.EMAIL);
        alertService.createAlertRule(cpuRule);

        // 内存使用率告警
        AlertRule memoryRule = new AlertRule();
        memoryRule.setName("内存使用率过高");
        memoryRule.setDescription("监控系统内存使用率");
        memoryRule.setServiceName("system");
        memoryRule.setMetricName("system.memory.usage");
        memoryRule.setOperator(AlertRule.Operator.GREATER_THAN);
        memoryRule.setThreshold(85.0);
        memoryRule.setLevel(AlertRule.AlertLevel.WARNING);
        memoryRule.setMessageTemplate("内存使用率达到%.1f%%");
        memoryRule.setNotifyType(AlertRule.NotifyType.EMAIL);
        alertService.createAlertRule(memoryRule);

        // 磁盘使用率告警
        AlertRule diskRule = new AlertRule();
        diskRule.setName("磁盘使用率过高");
        diskRule.setDescription("监控磁盘使用率");
        diskRule.setServiceName("system");
        diskRule.setMetricName("disk.usage");
        diskRule.setOperator(AlertRule.Operator.GREATER_THAN);
        diskRule.setThreshold(90.0);
        diskRule.setLevel(AlertRule.AlertLevel.CRITICAL);
        diskRule.setMessageTemplate("磁盘使用率达到%.1f%%");
        diskRule.setNotifyType(AlertRule.NotifyType.SMS);
        alertService.createAlertRule(diskRule);

        // API错误率告警
        AlertRule apiErrorRule = new AlertRule();
        apiErrorRule.setName("API错误率过高");
        apiErrorRule.setDescription("监控API接口错误率");
        apiErrorRule.setServiceName("gateway");
        apiErrorRule.setMetricName("api.error.rate");
        apiErrorRule.setOperator(AlertRule.Operator.GREATER_THAN);
        apiErrorRule.setThreshold(5.0);
        apiErrorRule.setLevel(AlertRule.AlertLevel.WARNING);
        apiErrorRule.setMessageTemplate("API错误率达到%.1f%%");
        apiErrorRule.setNotifyType(AlertRule.NotifyType.WEBHOOK);
        alertService.createAlertRule(apiErrorRule);

        // 响应时间告警
        AlertRule responseTimeRule = new AlertRule();
        responseTimeRule.setName("响应时间过长");
        responseTimeRule.setDescription("监控HTTP响应时间");
        responseTimeRule.setServiceName("gateway");
        responseTimeRule.setMetricName("http.response.time");
        responseTimeRule.setOperator(AlertRule.Operator.GREATER_THAN);
        responseTimeRule.setThreshold(2000.0);
        responseTimeRule.setLevel(AlertRule.AlertLevel.WARNING);
        responseTimeRule.setMessageTemplate("平均响应时间达到%.0fms");
        responseTimeRule.setNotifyType(AlertRule.NotifyType.EMAIL);
        alertService.createAlertRule(responseTimeRule);

        log.info("默认告警规则初始化完成");
    }
    */


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
