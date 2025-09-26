package com.haven.admin.controller;

import com.haven.admin.service.AdminHealthService;
import com.haven.base.common.response.ResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 简化的管理控制器
 * 提供基础的管理功能
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminHealthService adminHealthService;

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public ResponseWrapper<Map<String, Object>> getHealth() {
        try {
            log.info("获取系统健康状态");
            Map<String, Object> healthData = adminHealthService.getSystemHealth();
            return ResponseWrapper.success(healthData);
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            return ResponseWrapper.error(500, "获取系统健康状态失败: " + e.getMessage(), (Map<String, Object>) null);
        }
    }

    /**
     * 获取系统指标
     */
    @GetMapping("/metrics")
    public ResponseWrapper<Map<String, Object>> getMetrics() {
        try {
            log.info("获取系统指标");
            Map<String, Object> metricsData = adminHealthService.getSystemMetrics();
            return ResponseWrapper.success(metricsData);
        } catch (Exception e) {
            log.error("获取系统指标失败", e);
            return ResponseWrapper.error(500, "获取系统指标失败: " + e.getMessage(), (Map<String, Object>) null);
        }
    }

    /**
     * 获取服务状态
     */
    @GetMapping("/services")
    public ResponseWrapper<Map<String, Object>> getServices() {
        try {
            log.info("获取服务状态");
            Map<String, Object> servicesData = adminHealthService.getServicesStatus();
            return ResponseWrapper.success(servicesData);
        } catch (Exception e) {
            log.error("获取服务状态失败", e);
            return ResponseWrapper.error(500, "获取服务状态失败: " + e.getMessage(), (Map<String, Object>) null);
        }
    }
}