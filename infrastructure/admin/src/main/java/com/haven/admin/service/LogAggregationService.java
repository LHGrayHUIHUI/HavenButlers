package com.haven.admin.service;

import com.haven.admin.model.LogEntry;
import com.haven.admin.model.PageRequest;
import com.haven.admin.model.PageResponse;
import com.haven.admin.web.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 日志聚合服务
 * 从各个微服务实例收集日志并提供统一查询接口
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class LogAggregationService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${admin.log.aggregation.enabled:true}")
    private boolean aggregationEnabled;

    @Value("${admin.log.aggregation.retention.hours:168}") // 默认7天
    private int logRetentionHours;

    @Value("${admin.log.aggregation.batch.size:100}")
    private int batchSize;

    // 内存存储日志（生产环境应使用Elasticsearch）
    private final Map<String, List<LogEntry>> serviceLogs = new ConcurrentHashMap<>();
    private final AtomicLong logIdGenerator = new AtomicLong(1);

    // 存储服务实例信息
    private final Map<String, ServiceInstance> serviceInstances = new ConcurrentHashMap<>();

    /**
     * 定期从各服务实例收集日志
     */
    @Scheduled(fixedDelay = 30000) // 30秒执行一次
    public void collectLogsFromInstances() {
        if (!aggregationEnabled) {
            return;
        }

        try {
            log.debug("开始收集各服务日志...");

            // 获取所有注册的服务实例
            List<ServiceInstance> instances = getRegisteredInstances();

            for (ServiceInstance instance : instances) {
                try {
                    collectLogsFromInstance(instance);
                } catch (Exception e) {
                    log.error("从服务实例收集日志失败: service={}, instance={}",
                             instance.getServiceName(), instance.getInstanceId(), e);
                }
            }

            // 清理过期日志
            cleanupExpiredLogs();

            log.debug("日志收集完成，当前存储服务数: {}", serviceLogs.size());

        } catch (Exception e) {
            log.error("日志聚合任务执行失败", e);
        }
    }

    /**
     * 查询日志（分页）
     */
    public PageResponse<LogEntry> searchLogs(String serviceName,
                                            String level,
                                            String traceId,
                                            String keyword,
                                            LocalDateTime startTime,
                                            LocalDateTime endTime,
                                            PageRequest pageRequest) {

        List<LogEntry> allLogs = new ArrayList<>();

        // 从所有服务的日志中搜索
        if (serviceName != null && !serviceName.trim().isEmpty()) {
            // 查询指定服务的日志
            List<LogEntry> serviceLogList = serviceLogs.get(serviceName);
            if (serviceLogList != null) {
                allLogs.addAll(serviceLogList);
            }
        } else {
            // 查询所有服务的日志
            serviceLogs.values().forEach(allLogs::addAll);
        }

        // 应用过滤条件
        List<LogEntry> filteredLogs = allLogs.stream()
                .filter(log -> level == null || level.equals(log.getLevel()))
                .filter(log -> traceId == null || traceId.equals(log.getTraceId()))
                .filter(log -> keyword == null ||
                         log.getMessage().toLowerCase().contains(keyword.toLowerCase()))
                .filter(log -> startTime == null ||
                         log.getTimestamp().isAfter(startTime.atZone(ZoneId.systemDefault()).toInstant()))
                .filter(log -> endTime == null ||
                         log.getTimestamp().isBefore(endTime.atZone(ZoneId.systemDefault()).toInstant()))
                .sorted((l1, l2) -> l2.getTimestamp().compareTo(l1.getTimestamp()))
                .collect(Collectors.toList());

        // 分页处理
        int start = (pageRequest.getPage() - 1) * pageRequest.getSize();
        int end = Math.min(start + pageRequest.getSize(), filteredLogs.size());
        List<LogEntry> pageData = start < filteredLogs.size() ?
                                  filteredLogs.subList(start, end) : new ArrayList<>();

        return PageResponse.of(pageData, (long) filteredLogs.size(), pageRequest);
    }

    /**
     * 获取日志统计信息
     */
    public Map<String, Object> getLogStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = new HashMap<>();

        List<LogEntry> allLogs = new ArrayList<>();
        serviceLogs.values().forEach(allLogs::addAll);

        // 时间过滤
        List<LogEntry> filteredLogs = allLogs.stream()
                .filter(log -> startTime == null ||
                         log.getTimestamp().isAfter(startTime.atZone(ZoneId.systemDefault()).toInstant()))
                .filter(log -> endTime == null ||
                         log.getTimestamp().isBefore(endTime.atZone(ZoneId.systemDefault()).toInstant()))
                .collect(Collectors.toList());

        // 按级别统计
        Map<String, Long> levelCounts = filteredLogs.stream()
                .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));

        // 按服务统计
        Map<String, Long> serviceCounts = filteredLogs.stream()
                .collect(Collectors.groupingBy(LogEntry::getServiceName, Collectors.counting()));

        // 按时间统计（小时）
        Map<String, Long> timeCounts = filteredLogs.stream()
                .collect(Collectors.groupingBy(
                    log -> log.getTimestamp().atZone(ZoneId.systemDefault())
                              .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00")),
                    Collectors.counting()
                ));

        statistics.put("total", filteredLogs.size());
        statistics.put("levelCounts", levelCounts);
        statistics.put("serviceCounts", serviceCounts);
        statistics.put("timeCounts", timeCounts);
        statistics.put("serviceLogSizes", serviceLogs.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().size()
                )));

        return statistics;
    }

    /**
     * 根据traceId查找相关日志
     */
    public List<LogEntry> getLogsByTraceId(String traceId) {
        List<LogEntry> traceLogs = new ArrayList<>();

        for (List<LogEntry> serviceLogList : serviceLogs.values()) {
            serviceLogList.stream()
                    .filter(log -> traceId.equals(log.getTraceId()))
                    .forEach(traceLogs::add);
        }

        // 按时间排序
        traceLogs.sort(Comparator.comparing(LogEntry::getTimestamp));
        return traceLogs;
    }

    /**
     * 手动添加日志条目
     */
    public void addLogEntry(LogEntry logEntry) {
        if (!aggregationEnabled) {
            return;
        }

        if (logEntry.getId() == null) {
            logEntry.setId(logIdGenerator.getAndIncrement());
        }

        String serviceName = logEntry.getServiceName();
        serviceLogs.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(logEntry);

        // 限制单个服务的日志数量（防止内存溢出）
        List<LogEntry> logs = serviceLogs.get(serviceName);
        if (logs.size() > 10000) {
            // 删除最旧的日志
            logs.subList(0, 1000).clear();
        }
    }

    /**
     * 获取服务实例列表
     */
    private List<ServiceInstance> getRegisteredInstances() {
        // 这里应该从Spring Boot Admin获取注册的实例
        // 暂时返回模拟数据，实际应该注入InstanceRepository
        List<ServiceInstance> instances = new ArrayList<>();

        // 模拟一些服务实例
        instances.add(new ServiceInstance("gateway", "gateway-1", "http://localhost:8080"));
        instances.add(new ServiceInstance("storage-service", "storage-service-1", "http://localhost:8081"));
        instances.add(new ServiceInstance("account-service", "account-service-1", "http://localhost:8082"));
        instances.add(new ServiceInstance("message-service", "message-service-1", "http://localhost:8083"));
        instances.add(new ServiceInstance("ai-service", "ai-service-1", "http://localhost:8084"));
        instances.add(new ServiceInstance("nlp-service", "nlp-service-1", "http://localhost:8085"));
        instances.add(new ServiceInstance("file-manager-service", "file-manager-service-1", "http://localhost:8086"));

        return instances;
    }

    /**
     * 从单个服务实例收集日志
     */
    private void collectLogsFromInstance(ServiceInstance instance) {
        try {
            // 构建请求URL
            String logUrl = instance.getBaseUrl() + "/actuator/loggers";

            // 发送请求获取日志配置
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                logUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // 解析日志数据（这里需要根据实际API调整）
                parseLogData(instance, response.getBody());
            }

        } catch (Exception e) {
            log.warn("无法从实例 {} 收集日志: {}", instance.getInstanceId(), e.getMessage());
        }
    }

    /**
     * 解析日志数据
     */
    private void parseLogData(ServiceInstance instance, Map<String, Object> logData) {
        // 这里应该根据实际的日志API解析数据
        // 暂时创建一些模拟日志数据
        List<LogEntry> logs = new ArrayList<>();

        Instant now = Instant.now();
        String[] levels = {"INFO", "WARN", "ERROR", "DEBUG"};
        String[] messages = {
            "Service started successfully",
            "Processing request",
            "Database connection established",
            "Cache hit for key",
            "User authentication successful",
            "API response sent",
            "Background task completed",
            "Health check passed"
        };

        // 生成一些示例日志
        for (int i = 0; i < 5; i++) {
            LogEntry logEntry = LogEntry.builder()
                    .id(logIdGenerator.getAndIncrement())
                    .serviceName(instance.getServiceName())
                    .instanceId(instance.getInstanceId())
                    .level(levels[new Random().nextInt(levels.length)])
                    .message(messages[new Random().nextInt(messages.length)])
                    .timestamp(now.minusSeconds(new Random().nextInt(3600)))
                    .threadName("thread-" + new Random().nextInt(10))
                    .loggerName(instance.getServiceName() + ".controller")
                    .traceId("tr-" + System.currentTimeMillis() + "-" + new Random().nextInt(100000))
                    .build();

            logs.add(logEntry);
        }

        // 存储日志
        serviceLogs.computeIfAbsent(instance.getServiceName(), k -> new ArrayList<>()).addAll(logs);

        log.debug("从实例 {} 收集到 {} 条日志", instance.getInstanceId(), logs.size());
    }

    /**
     * 清理过期日志
     */
    private void cleanupExpiredLogs() {
        Instant cutoffTime = Instant.now().minusSeconds(logRetentionHours * 3600L);

        serviceLogs.forEach((serviceName, logs) -> {
            int originalSize = logs.size();
            logs.removeIf(log -> log.getTimestamp().isBefore(cutoffTime));
            int removed = originalSize - logs.size();

            if (removed > 0) {
                log.debug("清理服务 {} 的过期日志 {} 条", serviceName, removed);
            }
        });
    }

    /**
     * 服务实例信息
     */
    public static class ServiceInstance {
        private String serviceName;
        private String instanceId;
        private String baseUrl;

        public ServiceInstance(String serviceName, String instanceId, String baseUrl) {
            this.serviceName = serviceName;
            this.instanceId = instanceId;
            this.baseUrl = baseUrl;
        }

        public String getServiceName() { return serviceName; }
        public String getInstanceId() { return instanceId; }
        public String getBaseUrl() { return baseUrl; }
    }
}