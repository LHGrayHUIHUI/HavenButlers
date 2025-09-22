package com.haven.admin.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务健康监控器
 * 定期检查各微服务的健康状态
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class ServiceHealthMonitor implements HealthIndicator {

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private RestTemplate restTemplate;

    // 使用可变对象承载gauge值，避免频繁注册Gauge导致的度量污染
    private final Map<String, AtomicInteger> instanceHealthGauges = new ConcurrentHashMap<>();

    /**
     * 定时检查服务健康状态
     */
    @Scheduled(fixedDelay = 30000)
    public void checkServiceHealth() {
        if (discoveryClient == null) {
            log.debug("DiscoveryClient未配置，跳过服务健康检查");
            return;
        }

        List<String> services = discoveryClient.getServices();
        log.info("开始检查{}个服务的健康状态", services.size());

        for (String service : services) {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);

            for (ServiceInstance instance : instances) {
                checkInstanceHealth(instance);
            }
        }
    }

    /**
     * 检查单个实例的健康状态
     */
    private void checkInstanceHealth(ServiceInstance instance) {
        String healthUrl = instance.getUri() + "/actuator/health";

        try {
            Map response = restTemplate.getForObject(healthUrl, Map.class);

            if (response != null) {
                String status = (String) response.get("status");

                // 记录指标（稳定 gauge 对象，重复使用）
                int value = "UP".equals(status) ? 1 : 0;
                updateHealthGauge(instance, value);

                // 记录日志
                if (!"UP".equals(status)) {
                    log.warn("服务实例 {} ({}) 状态异常: {}",
                            instance.getServiceId(),
                            instance.getInstanceId(),
                            status);
                }
            }
        } catch (Exception e) {
            log.error("无法检查服务实例 {} ({}) 的健康状态",
                    instance.getServiceId(),
                    instance.getInstanceId(), e);

            // 记录不可达指标
            updateHealthGauge(instance, 0);
        }
    }

    private void updateHealthGauge(ServiceInstance instance, int value) {
        String key = instance.getServiceId() + "#" + instance.getInstanceId();
        AtomicInteger holder = instanceHealthGauges.computeIfAbsent(key, k -> {
            AtomicInteger init = new AtomicInteger(value);
            meterRegistry.gauge(
                "service.health",
                Tags.of("service", instance.getServiceId(), "instance", instance.getInstanceId()),
                init
            );
            return init;
        });
        holder.set(value);
    }

    @Override
    public Health health() {
        try {
            if (discoveryClient != null && !discoveryClient.getServices().isEmpty()) {
                return Health.up()
                    .withDetail("status", "监控服务正常运行")
                    .withDetail("services", discoveryClient.getServices().size())
                    .build();
            }
            return Health.up()
                .withDetail("status", "监控服务正常运行（独立模式）")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
