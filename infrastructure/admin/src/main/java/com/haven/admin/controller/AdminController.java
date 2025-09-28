package com.haven.admin.controller;

import com.haven.admin.service.AdminHealthService;
import com.haven.admin.common.AdminResponse;
import com.haven.admin.common.AdminException;
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
    public AdminResponse<Map<String, Object>> getHealth() {
        try {
            log.info("获取系统健康状态");
            Map<String, Object> healthData = adminHealthService.getSystemHealth();
            return AdminResponse.success(healthData);
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            throw new AdminException("获取系统健康状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统指标
     */
    @GetMapping("/metrics")
    public AdminResponse<Map<String, Object>> getMetrics() {
        try {
            log.info("获取系统指标");
            Map<String, Object> metricsData = adminHealthService.getSystemMetrics();
            return AdminResponse.success(metricsData);
        } catch (Exception e) {
            log.error("获取系统指标失败", e);
            throw new AdminException("获取系统指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取服务状态
     */
    @GetMapping("/services")
    public AdminResponse<Map<String, Object>> getServices() {
        try {
            log.info("获取服务状态");
            Map<String, Object> servicesData = adminHealthService.getServicesStatus();
            return AdminResponse.success(servicesData);
        } catch (Exception e) {
            log.error("获取服务状态失败", e);
            throw new AdminException( "获取服务状态失败: " + e.getMessage());
        }
    }
}