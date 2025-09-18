package com.haven.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 简化的管理服务 - 健康检查服务
 * 替代复杂的AlertService和ServiceManageService
 */
@Service
public class AdminHealthService {

    private static final Logger log = LoggerFactory.getLogger(AdminHealthService.class);

    /**
     * 获取系统健康状态
     */
    public Map<String, Object> getSystemHealth() {
        log.info("检查系统健康状态");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("services", getServicesStatus());

        return health;
    }

    /**
     * 获取服务状态
     */
    public Map<String, Object> getServicesStatus() {
        Map<String, Object> services = new HashMap<>();

        // 模拟服务状态
        services.put("admin-service", "UP");
        services.put("database", "UP");
        services.put("redis", "DOWN");

        return services;
    }

    /**
     * 获取系统指标
     */
    public Map<String, Object> getSystemMetrics() {
        log.debug("获取系统指标");

        Map<String, Object> metrics = new HashMap<>();

        // 内存指标
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        metrics.put("memory.total", totalMemory);
        metrics.put("memory.free", freeMemory);
        metrics.put("memory.used", usedMemory);
        metrics.put("memory.usage", (double) usedMemory / totalMemory * 100);

        // CPU指标 (模拟)
        metrics.put("cpu.usage", Math.random() * 100);

        return metrics;
    }
}