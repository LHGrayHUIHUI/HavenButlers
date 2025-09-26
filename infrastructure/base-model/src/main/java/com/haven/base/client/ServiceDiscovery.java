package com.haven.base.client;

import java.util.List;

/**
 * 服务发现接口
 * 抽象服务发现逻辑，支持多种实现（Nacos、Eureka、Consul等）
 *
 * @author HavenButler
 */
public interface ServiceDiscovery {

    /**
     * 获取服务URL
     * 支持负载均衡，返回可用的服务实例地址
     *
     * @param serviceName 服务名称
     * @return 服务URL
     */
    String getServiceUrl(String serviceName);

    /**
     * 获取服务所有实例
     *
     * @param serviceName 服务名称
     * @return 服务实例列表
     */
    List<String> getServiceInstances(String serviceName);

    /**
     * 检查服务是否健康
     *
     * @param serviceName 服务名称
     * @return 是否健康
     */
    boolean isServiceHealthy(String serviceName);

    /**
     * 注册服务实例
     *
     * @param serviceName 服务名称
     * @param serviceUrl 服务地址
     */
    void registerService(String serviceName, String serviceUrl);

    /**
     * 注销服务实例
     *
     * @param serviceName 服务名称
     * @param serviceUrl 服务地址
     */
    void deregisterService(String serviceName, String serviceUrl);
}