package com.haven.admin.controller;

import com.haven.admin.model.ServiceInfo;
import com.haven.admin.model.ServiceMetrics;
import com.haven.admin.service.ServiceManageService;
import com.haven.admin.service.AdminNacosServiceManager;
import com.haven.admin.common.AdminResponse;
import com.haven.admin.model.PageRequest;
import com.haven.admin.model.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 服务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
public class ServiceManageController {

    private final ServiceManageService serviceManageService;
    private final AdminNacosServiceManager adminNacosServiceManager;

    /**
     * 获取所有服务列表
     */
    @GetMapping("/list")
    public AdminResponse<List<ServiceInfo>> getServiceList() {
        List<ServiceInfo> services = serviceManageService.getAllServices();
        return AdminResponse.success(services);
    }

    /**
     * 获取服务详情
     */
    @GetMapping("/{serviceName}")
    public AdminResponse<ServiceInfo> getServiceDetail(@PathVariable String serviceName) {
        ServiceInfo serviceInfo = serviceManageService.getServiceDetail(serviceName);
        return AdminResponse.success(serviceInfo);
    }

    /**
     * 获取服务健康状态
     */
    @GetMapping("/{serviceName}/health")
    public AdminResponse<Map<String, Object>> getServiceHealth(@PathVariable String serviceName) {
        Map<String, Object> health = serviceManageService.getServiceHealth(serviceName);
        return AdminResponse.success(health);
    }

    /**
     * 获取服务指标
     */
    @GetMapping("/{serviceName}/metrics")
    public AdminResponse<ServiceMetrics> getServiceMetrics(
            @PathVariable String serviceName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        ServiceMetrics metrics = serviceManageService.getServiceMetrics(serviceName, startTime, endTime);
        return AdminResponse.success(metrics);
    }

    /**
     * 重启服务
     */
    @PostMapping("/{serviceName}/restart")
    public AdminResponse<Void> restartService(@PathVariable String serviceName) {
        serviceManageService.restartService(serviceName);
        log.info("重启服务: {}", serviceName);
        return AdminResponse.success();
    }

    /**
     * 停止服务
     */
    @PostMapping("/{serviceName}/stop")
    public AdminResponse<Void> stopService(@PathVariable String serviceName) {
        serviceManageService.stopService(serviceName);
        log.info("停止服务: {}", serviceName);
        return AdminResponse.success();
    }

    /**
     * 启动服务
     */
    @PostMapping("/{serviceName}/start")
    public AdminResponse<Void> startService(@PathVariable String serviceName) {
        serviceManageService.startService(serviceName);
        log.info("启动服务: {}", serviceName);
        return AdminResponse.success();
    }

    /**
     * 获取服务日志
     */
    @GetMapping("/{serviceName}/logs")
    public AdminResponse<PageResponse<String>> getServiceLogs(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "INFO") String level,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        PageResponse<String> logs = serviceManageService.getServiceLogs(serviceName, level, pageRequest);
        return AdminResponse.success(logs);
    }

    /**
     * 更新服务配置
     */
    @PutMapping("/{serviceName}/config")
    public AdminResponse<Void> updateServiceConfig(
            @PathVariable String serviceName,
            @RequestBody Map<String, Object> config) {
        serviceManageService.updateServiceConfig(serviceName, config);
        log.info("更新服务配置: {}, 配置: {}", serviceName, config);
        return AdminResponse.success();
    }

    /**
     * 获取服务依赖关系
     */
    @GetMapping("/dependencies")
    public AdminResponse<Map<String, List<String>>> getServiceDependencies() {
        Map<String, List<String>> dependencies = serviceManageService.getServiceDependencies();
        return AdminResponse.success(dependencies);
    }

    /**
     * 服务健康检查
     */
    @PostMapping("/health-check")
    public AdminResponse<Map<String, String>> healthCheck() {
        Map<String, String> result = serviceManageService.performHealthCheck();
        return AdminResponse.success(result);
    }

    // =============== Nacos专用API ===============

    /**
     * 获取Nacos中的所有服务名称
     */
    @GetMapping("/nacos/services")
    public AdminResponse<List<String>> getNacosServices() {
        List<String> services = adminNacosServiceManager.getAllServiceNames();
        return AdminResponse.success(services);
    }

    /**
     * 获取指定服务的所有实例（Nacos）
     */
    @GetMapping("/nacos/{serviceName}/instances")
    public AdminResponse<List<ServiceInstance>> getServiceInstances(@PathVariable String serviceName) {
        List<ServiceInstance> instances = adminNacosServiceManager.getServiceInstances(serviceName);
        return AdminResponse.success(instances);
    }

    /**
     * 获取服务详细信息（包含Nacos特有信息）
     */
    @GetMapping("/nacos/{serviceName}/details")
    public AdminResponse<Map<String, Object>> getNacosServiceDetails(@PathVariable String serviceName) {
        Map<String, Object> details = adminNacosServiceManager.getServiceDetails(serviceName);
        return AdminResponse.success(details);
    }

    /**
     * 获取Nacos服务健康状态
     */
    @GetMapping("/nacos/{serviceName}/health")
    public AdminResponse<Map<String, Object>> getNacosServiceHealth(@PathVariable String serviceName) {
        Map<String, Object> health = adminNacosServiceManager.getServiceHealth(serviceName);
        return AdminResponse.success(health);
    }

    /**
     * 获取系统整体健康状态
     */
    @GetMapping("/nacos/system/health")
    public AdminResponse<Map<String, Object>> getSystemHealth() {
        Map<String, Object> systemHealth = adminNacosServiceManager.getSystemHealth();
        return AdminResponse.success(systemHealth);
    }

    /**
     * 临时下线服务实例（维护模式）
     */
    @PostMapping("/nacos/{serviceName}/deregister")
    public AdminResponse<Boolean> deregisterInstance(
            @PathVariable String serviceName,
            @RequestParam String ip,
            @RequestParam int port) {
        boolean result = adminNacosServiceManager.deregisterInstance(serviceName, ip, port);
        if (result) {
            log.info("临时下线服务实例成功: {}:{}:{}", serviceName, ip, port);
        } else {
            log.error("临时下线服务实例失败: {}:{}:{}", serviceName, ip, port);
        }
        return AdminResponse.success(result);
    }

    /**
     * 重新上线服务实例
     */
    @PostMapping("/nacos/{serviceName}/register")
    public AdminResponse<Boolean> registerInstance(
            @PathVariable String serviceName,
            @RequestParam String ip,
            @RequestParam int port) {
        boolean result = adminNacosServiceManager.registerInstance(serviceName, ip, port);
        if (result) {
            log.info("重新上线服务实例成功: {}:{}:{}", serviceName, ip, port);
        } else {
            log.error("重新上线服务实例失败: {}:{}:{}", serviceName, ip, port);
        }
        return AdminResponse.success(result);
    }
}