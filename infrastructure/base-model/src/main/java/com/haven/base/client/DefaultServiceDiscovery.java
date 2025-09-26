package com.haven.base.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认服务发现实现
 * 基于配置文件的简单服务发现，适合开发和测试环境
 * 生产环境建议使用Nacos、Eureka等专业服务发现组件
 *
 * @author HavenButler
 */
@Slf4j
@Component
@ConditionalOnMissingBean(ServiceDiscovery.class)
@ConfigurationProperties(prefix = "base-model.service-discovery")
public class DefaultServiceDiscovery implements ServiceDiscovery {

    /**
     * 服务配置映射
     * key: serviceName, value: serviceUrl列表
     */
    private Map<String, List<String>> services = new ConcurrentHashMap<>();

    /**
     * 服务健康状态缓存
     */
    private Map<String, Boolean> healthStatus = new ConcurrentHashMap<>();

    /**
     * 负载均衡计数器
     */
    private Map<String, Integer> loadBalanceCounters = new ConcurrentHashMap<>();

    @Override
    public String getServiceUrl(String serviceName) {
        List<String> instances = services.get(serviceName);

        if (instances == null || instances.isEmpty()) {
            // 如果没有配置，使用默认规则：http://serviceName:8080
            String defaultUrl = "http://" + serviceName + ":8080";
            log.warn("服务[{}]未配置实例，使用默认地址: {}", serviceName, defaultUrl);
            return defaultUrl;
        }

        // 简单轮询负载均衡
        int counter = loadBalanceCounters.getOrDefault(serviceName, 0);
        String selectedUrl = instances.get(counter % instances.size());
        loadBalanceCounters.put(serviceName, counter + 1);

        log.debug("选择服务实例: {} -> {}", serviceName, selectedUrl);
        return selectedUrl;
    }

    @Override
    public List<String> getServiceInstances(String serviceName) {
        return services.getOrDefault(serviceName, Collections.emptyList());
    }

    @Override
    public boolean isServiceHealthy(String serviceName) {
        return healthStatus.getOrDefault(serviceName, true);
    }

    @Override
    public void registerService(String serviceName, String serviceUrl) {
        services.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(serviceUrl);
        healthStatus.put(serviceName, true);
        log.info("注册服务: {} -> {}", serviceName, serviceUrl);
    }

    @Override
    public void deregisterService(String serviceName, String serviceUrl) {
        List<String> instances = services.get(serviceName);
        if (instances != null) {
            instances.remove(serviceUrl);
            if (instances.isEmpty()) {
                services.remove(serviceName);
                healthStatus.remove(serviceName);
                loadBalanceCounters.remove(serviceName);
            }
        }
        log.info("注销服务: {} -> {}", serviceName, serviceUrl);
    }

    /**
     * 设置服务健康状态
     */
    public void setServiceHealth(String serviceName, boolean healthy) {
        healthStatus.put(serviceName, healthy);
        log.info("更新服务健康状态: {} -> {}", serviceName, healthy);
    }

    // Getters and Setters for ConfigurationProperties

    public Map<String, List<String>> getServices() {
        return services;
    }

    public void setServices(Map<String, List<String>> services) {
        this.services = services;
        log.info("加载服务配置: {}", services);
    }
}