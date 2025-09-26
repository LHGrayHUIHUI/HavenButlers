package com.haven.admin.service;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Nacos服务管理器
 *
 * 功能：
 * - 获取所有注册的服务列表
 * - 监控服务实例健康状态
 * - 管理服务的上线下线
 * - 提供服务详细信息查询
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Service("adminNacosServiceManager")
public class AdminNacosServiceManager {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Autowired
    private NamingService namingService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SimpleCacheService cacheService;

    /**
     * 获取所有注册的服务列表
     *
     * @return 服务名称列表
     */
    public List<String> getAllServiceNames() {
        try {
            return discoveryClient.getServices();
        } catch (Exception e) {
            log.error("获取服务列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取指定服务的所有实例
     *
     * @param serviceName 服务名称
     * @return 服务实例列表
     */
    public List<ServiceInstance> getServiceInstances(String serviceName) {
        try {
            return discoveryClient.getInstances(serviceName);
        } catch (Exception e) {
            log.error("获取服务实例失败，服务: {}", serviceName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取服务详细信息（包含Nacos特有的信息）
     *
     * @param serviceName 服务名称
     * @return 服务详细信息
     */
    public Map<String, Object> getServiceDetails(String serviceName) {
        Map<String, Object> details = new HashMap<>();

        try {
            // 获取基础信息
            List<ServiceInstance> instances = getServiceInstances(serviceName);
            details.put("serviceName", serviceName);
            details.put("instanceCount", instances.size());
            details.put("instances", instances);

            // 获取Nacos特有信息
            String groupName = nacosDiscoveryProperties.getGroup();
            // 注意：不同版本的Nacos API可能不同，这里简化处理
            // ServiceInfo serviceInfo = namingService.getServiceInfo(serviceName, groupName);

            // 如果需要获取更详细信息，可以通过其他方式
            details.put("groupName", groupName);

            // 通过HTTP探测统计健康状态
            long healthyCount = instances.stream()
                .mapToLong(instance -> checkInstanceHealthByHttp(instance) ? 1 : 0)
                .sum();
            details.put("healthyInstanceCount", healthyCount);
            details.put("unhealthyInstanceCount", instances.size() - healthyCount);

        } catch (Exception e) {
            log.error("获取服务详细信息失败，服务: {}", serviceName, e);
            details.put("error", e.getMessage());
        }

        return details;
    }

    /**
     * 获取服务健康状态
     *
     * @param serviceName 服务名称
     * @return 健康状态信息
     */
    public Map<String, Object> getServiceHealth(String serviceName) {
        Map<String, Object> health = new HashMap<>();

        try {
            List<ServiceInstance> instances = getServiceInstances(serviceName);
            int totalInstances = instances.size();

            if (totalInstances == 0) {
                health.put("status", "DOWN");
                health.put("message", "没有可用的服务实例");
                return health;
            }

            // 检查每个实例的健康状态
            long healthyCount = instances.stream()
                .mapToLong(instance -> {
                    return checkInstanceHealthByHttp(instance) ? 1 : 0;
                })
                .sum();

            double healthRatio = (double) healthyCount / totalInstances;

            health.put("totalInstances", totalInstances);
            health.put("healthyInstances", healthyCount);
            health.put("unhealthyInstances", totalInstances - healthyCount);
            health.put("healthRatio", healthRatio);

            if (healthRatio >= 0.8) {
                health.put("status", "UP");
            } else if (healthRatio >= 0.5) {
                health.put("status", "DEGRADED");
            } else {
                health.put("status", "DOWN");
            }

        } catch (Exception e) {
            log.error("获取服务健康状态失败，服务: {}", serviceName, e);
            health.put("status", "UNKNOWN");
            health.put("error", e.getMessage());
        }

        return health;
    }

    /**
     * 获取系统整体健康状态
     *
     * @return 系统健康状态
     */
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> systemHealth = new HashMap<>();

        try {
            List<String> serviceNames = getAllServiceNames();
            int totalServices = serviceNames.size();

            if (totalServices == 0) {
                systemHealth.put("status", "DOWN");
                systemHealth.put("message", "没有注册的服务");
                return systemHealth;
            }

            Map<String, String> serviceStatuses = new HashMap<>();
            long upServices = 0;

            for (String serviceName : serviceNames) {
                Map<String, Object> health = getServiceHealth(serviceName);
                String status = (String) health.get("status");
                serviceStatuses.put(serviceName, status);

                if ("UP".equals(status)) {
                    upServices++;
                }
            }

            double upRatio = (double) upServices / totalServices;
            systemHealth.put("totalServices", totalServices);
            systemHealth.put("upServices", upServices);
            systemHealth.put("downServices", totalServices - upServices);
            systemHealth.put("upRatio", upRatio);
            systemHealth.put("serviceStatuses", serviceStatuses);

            if (upRatio >= 0.8) {
                systemHealth.put("status", "UP");
            } else if (upRatio >= 0.5) {
                systemHealth.put("status", "DEGRADED");
            } else {
                systemHealth.put("status", "DOWN");
            }

        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            systemHealth.put("status", "UNKNOWN");
            systemHealth.put("error", e.getMessage());
        }

        return systemHealth;
    }

    /**
     * 通过HTTP调用检查实例健康状态（使用缓存优化）
     * 实际探测actuator/health端点，失败时回落为DOWN
     *
     * @param instance 服务实例
     * @return 是否健康
     */
    private boolean checkInstanceHealthByHttp(ServiceInstance instance) {
        String cacheKey = String.format("nacos_health_%s_%s_%d",
                instance.getServiceId(), instance.getHost(), instance.getPort());

        // 缓存10秒，与AdminNacosServiceManager的探测频率匹配
        Boolean cachedResult = cacheService.computeIfAbsent(
                cacheKey,
                10,
                () -> performHttpHealthCheck(instance)
        );

        return cachedResult != null ? cachedResult : false;
    }

    /**
     * 执行实际的HTTP健康检查
     */
    private Boolean performHttpHealthCheck(ServiceInstance instance) {
        try {
            String healthUrl = String.format("http://%s:%d/actuator/health",
                    instance.getHost(), instance.getPort());

            // 使用Map解析，避免直接反序列化Health对象
            @SuppressWarnings("unchecked")
            Map<String, Object> healthResponse = restTemplate.getForObject(healthUrl, Map.class);

            if (healthResponse == null) {
                log.debug("实例 {}:{} 健康检查返回空响应", instance.getHost(), instance.getPort());
                return false;
            }

            String status = (String) healthResponse.get("status");
            boolean isHealthy = "UP".equals(status);

            if (!isHealthy) {
                log.debug("实例 {}:{} 健康状态为: {}", instance.getHost(), instance.getPort(), status);
            }

            return isHealthy;
        } catch (Exception e) {
            // HTTP调用失败，回落为DOWN
            log.debug("实例 {}:{} 健康检查HTTP调用失败: {}",
                    instance.getHost(), instance.getPort(), e.getMessage());
            return false;
        }
    }

    /**
     * 临时下线服务实例（用于维护）
     *
     * @param serviceName 服务名称
     * @param ip IP地址
     * @param port 端口
     * @return 操作结果
     */
    public boolean deregisterInstance(String serviceName, String ip, int port) {
        try {
            namingService.deregisterInstance(serviceName, ip, port);
            log.info("临时下线服务实例成功: {}:{}:{}", serviceName, ip, port);
            return true;
        } catch (NacosException e) {
            log.error("临时下线服务实例失败: {}:{}:{}", serviceName, ip, port, e);
            return false;
        }
    }

    /**
     * 重新上线服务实例
     *
     * @param serviceName 服务名称
     * @param ip IP地址
     * @param port 端口
     * @return 操作结果
     */
    public boolean registerInstance(String serviceName, String ip, int port) {
        try {
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setHealthy(true);
            instance.setEnabled(true);

            namingService.registerInstance(serviceName, instance);
            log.info("重新上线服务实例成功: {}:{}:{}", serviceName, ip, port);
            return true;
        } catch (NacosException e) {
            log.error("重新上线服务实例失败: {}:{}:{}", serviceName, ip, port, e);
            return false;
        }
    }
}