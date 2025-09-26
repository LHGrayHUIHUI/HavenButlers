package com.haven.admin.monitor;

import com.haven.admin.model.ServiceOverview;
import com.haven.admin.service.HealthSnapshotService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Autowired
    private HealthSnapshotService healthSnapshotService;

    @Autowired
    private MeterRegistry meterRegistry;

    // 使用可变对象承载gauge值，避免频繁注册Gauge导致的度量污染
    private final Map<String, AtomicInteger> instanceHealthGauges = new ConcurrentHashMap<>();

    /**
     * 定时更新服务健康状态指标（复用 HealthSnapshotService 的结果）
     * 频率降低到 30 秒，避免与 HealthSnapshotService（10秒）的重复探测
     */
    @Scheduled(fixedDelay = 30000)
    public void updateHealthMetrics() {
        try {
            List<ServiceOverview> overviews = healthSnapshotService.getAllServiceOverview();
            log.debug("开始更新{}个服务的健康状态指标", overviews.size());

            for (ServiceOverview overview : overviews) {
                updateServiceHealthGauge(overview);
            }
        } catch (Exception e) {
            log.error("更新健康状态指标失败", e);
        }
    }

    /**
     * 更新服务级别的健康状态指标
     */
    private void updateServiceHealthGauge(ServiceOverview overview) {
        String serviceName = overview.getServiceName();
        String key = "service#" + serviceName;

        // 计算服务级别的健康比率
        double healthRatio = overview.getInstanceCount() > 0 ?
            (double) overview.getHealthyCount() / overview.getInstanceCount() : 0.0;

        int healthValue = determineServiceHealthValue(overview.getStatus());

        AtomicInteger holder = instanceHealthGauges.computeIfAbsent(key, k -> {
            AtomicInteger init = new AtomicInteger(healthValue);
            meterRegistry.gauge(
                "service.health",
                Tags.of("service", serviceName),
                init
            );
            return init;
        });
        holder.set(healthValue);

        // 同时记录健康比率指标
        meterRegistry.gauge(
            "service.health.ratio",
            Tags.of("service", serviceName),
            healthRatio
        );

        // 记录异常状态
        if (!"UP".equals(overview.getStatus())) {
            log.warn("服务 {} 状态异常: {}, 健康实例 {}/{}",
                serviceName, overview.getStatus(),
                overview.getHealthyCount(), overview.getInstanceCount());
        }
    }

    /**
     * 将服务状态转换为数值
     */
    private int determineServiceHealthValue(String status) {
        switch (status) {
            case "UP":
                return 2;
            case "DEGRADED":
                return 1;
            case "DOWN":
            default:
                return 0;
        }
    }

    @Override
    public Health health() {
        try {
            List<ServiceOverview> overviews = healthSnapshotService.getAllServiceOverview();
            long healthyServices = overviews.stream()
                .mapToLong(overview -> "UP".equals(overview.getStatus()) ? 1 : 0)
                .sum();

            return Health.up()
                .withDetail("status", "监控服务正常运行")
                .withDetail("totalServices", overviews.size())
                .withDetail("healthyServices", healthyServices)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
