package com.haven.storage.logging;

import com.haven.storage.logging.entity.OperationLog;
import com.haven.storage.logging.entity.PerformanceLog;
import com.haven.storage.logging.entity.SecurityLog;
import com.haven.storage.logging.repository.OperationLogRepository;
import com.haven.storage.logging.repository.PerformanceLogRepository;
import com.haven.storage.logging.repository.SecurityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 统一日志服务
 * 负责记录、查询和管理系统中所有的日志信息
 *
 * 功能范围：
 * 1. 操作日志：数据库操作、API调用记录
 * 2. 性能日志：响应时间、连接池状态监控
 * 3. 安全日志：认证失败、权限拒绝、异常行为
 * 4. 日志查询：支持多条件查询和分页
 * 5. 日志清理：自动清理过期日志
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final PerformanceLogRepository performanceLogRepository;
    private final SecurityLogRepository securityLogRepository;

    /**
     * 记录操作日志
     * 异步执行，避免影响主业务性能
     *
     * @param familyId 家庭ID
     * @param serviceType 服务类型(POSTGRESQL/MONGODB/REDIS/HTTP_API)
     * @param clientIP 客户端IP
     * @param userId 用户ID
     * @param operationType 操作类型(SELECT/INSERT/UPDATE/DELETE等)
     * @param databaseName 数据库名
     * @param tableOrCollection 表名或集合名
     * @param operationContent 操作内容(脱敏后的SQL/命令)
     * @param executionTimeMs 执行时间(毫秒)
     * @param resultStatus 结果状态(SUCCESS/FAILED/BLOCKED)
     * @param errorMessage 错误信息
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logOperation(String familyId,
                                               String serviceType,
                                               String clientIP,
                                               String userId,
                                               String operationType,
                                               String databaseName,
                                               String tableOrCollection,
                                               String operationContent,
                                               Integer executionTimeMs,
                                               String resultStatus,
                                               String errorMessage) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setFamilyId(familyId);
            operationLog.setServiceType(serviceType);
            operationLog.setClientIP(clientIP);
            operationLog.setUserId(userId);
            operationLog.setOperationType(operationType);
            operationLog.setDatabaseName(databaseName);
            operationLog.setTableOrCollection(tableOrCollection);
            operationLog.setOperationContent(sanitizeContent(operationContent));
            operationLog.setExecutionTimeMs(executionTimeMs);
            operationLog.setResultStatus(resultStatus);
            operationLog.setErrorMessage(errorMessage);
            operationLog.setCreatedAt(LocalDateTime.now());

            operationLogRepository.save(operationLog);

            log.debug("操作日志记录成功: {} - {} - {}", serviceType, operationType, clientIP);

        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 记录性能日志
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logPerformance(String serviceType,
                                                 String metricName,
                                                 Double metricValue,
                                                 String metricUnit,
                                                 String instanceId) {
        try {
            PerformanceLog performanceLog = new PerformanceLog();
            performanceLog.setServiceType(serviceType);
            performanceLog.setMetricName(metricName);
            performanceLog.setMetricValue(metricValue);
            performanceLog.setMetricUnit(metricUnit);
            performanceLog.setInstanceId(instanceId);
            performanceLog.setCreatedAt(LocalDateTime.now());

            performanceLogRepository.save(performanceLog);

        } catch (Exception e) {
            log.error("记录性能日志失败: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 记录安全事件日志
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logSecurityEvent(String familyId,
                                                   String clientIP,
                                                   String eventType,
                                                   String serviceType,
                                                   String eventDetails,
                                                   String riskLevel) {
        try {
            SecurityLog securityLog = new SecurityLog();
            securityLog.setFamilyId(familyId);
            securityLog.setClientIP(clientIP);
            securityLog.setEventType(eventType);
            securityLog.setServiceType(serviceType);
            securityLog.setEventDetails(eventDetails);
            securityLog.setRiskLevel(riskLevel);
            securityLog.setCreatedAt(LocalDateTime.now());

            securityLogRepository.save(securityLog);

            // 高风险事件立即输出警告日志
            if ("HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel)) {
                log.warn("🚨 高风险安全事件: {} - {} - {}", eventType, clientIP, eventDetails);
            }

        } catch (Exception e) {
            log.error("记录安全日志失败: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 分页查询操作日志
     */
    public Page<OperationLog> queryOperationLogs(String familyId,
                                                String serviceType,
                                                String clientIP,
                                                LocalDateTime startTime,
                                                LocalDateTime endTime,
                                                int page,
                                                int size) {
        PageRequest pageRequest = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "createdAt"));

        return operationLogRepository.findByConditions(
            familyId, serviceType, clientIP, startTime, endTime, pageRequest);
    }

    /**
     * 查询性能指标统计
     */
    public List<Map<String, Object>> getPerformanceStats(String serviceType,
                                                        String metricName,
                                                        LocalDateTime startTime,
                                                        LocalDateTime endTime) {
        return performanceLogRepository.findPerformanceStats(
            serviceType, metricName, startTime, endTime);
    }

    /**
     * 查询安全事件统计
     */
    public List<Map<String, Object>> getSecurityStats(String familyId,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime) {
        return securityLogRepository.findSecurityStats(familyId, startTime, endTime);
    }

    /**
     * 获取实时监控数据
     */
    public Map<String, Object> getRealTimeMetrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        // 获取最近1小时的统计数据
        Map<String, Object> metrics = Map.of(
            "totalOperations", operationLogRepository.countByTimeRange(oneHourAgo, now),
            "failedOperations", operationLogRepository.countByStatusAndTimeRange("FAILED", oneHourAgo, now),
            "blockedOperations", operationLogRepository.countByStatusAndTimeRange("BLOCKED", oneHourAgo, now),
            "securityEvents", securityLogRepository.countByTimeRange(oneHourAgo, now),
            "avgResponseTime", performanceLogRepository.findAvgMetricByTimeRange("response_time", oneHourAgo, now)
        );

        return metrics;
    }

    /**
     * 清理过期日志
     * 定时任务调用，清理超过指定天数的日志
     */
    @Transactional
    public void cleanupExpiredLogs(int retentionDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);

        try {
            // 清理操作日志
            int operationLogsDeleted = operationLogRepository.deleteByCreatedAtBefore(cutoffTime);
            log.info("清理操作日志 {} 条", operationLogsDeleted);

            // 清理性能日志
            int performanceLogsDeleted = performanceLogRepository.deleteByCreatedAtBefore(cutoffTime);
            log.info("清理性能日志 {} 条", performanceLogsDeleted);

            // 安全日志保留时间更长(保留6个月)
            LocalDateTime securityCutoffTime = LocalDateTime.now().minusDays(retentionDays * 6);
            int securityLogsDeleted = securityLogRepository.deleteByCreatedAtBefore(securityCutoffTime);
            log.info("清理安全日志 {} 条", securityLogsDeleted);

        } catch (Exception e) {
            log.error("清理过期日志失败: {}", e.getMessage(), e);
        }
    }


    /**
     * 敏感信息脱敏处理
     */
    private String sanitizeContent(String content) {
        if (content == null) {
            return null;
        }

        // 移除密码等敏感信息
        String sanitized = content
            .replaceAll("(?i)password\\s*=\\s*'[^']*'", "password='***'")
            .replaceAll("(?i)password\\s*=\\s*\"[^\"]*\"", "password=\"***\"")
            .replaceAll("(?i)passwd\\s*=\\s*'[^']*'", "passwd='***'")
            .replaceAll("(?i)secret\\s*=\\s*'[^']*'", "secret='***'")
            .replaceAll("(?i)token\\s*=\\s*'[^']*'", "token='***'");

        // 限制内容长度，避免存储过大的数据
        if (sanitized.length() > 2000) {
            sanitized = sanitized.substring(0, 2000) + "...[截断]";
        }

        return sanitized;
    }

    /**
     * 批量记录操作日志
     * 用于高并发场景的批量日志记录
     */
    @Async("logExecutor")
    public CompletableFuture<Void> batchLogOperations(List<OperationLog> operationLogs) {
        try {
            operationLogRepository.saveAll(operationLogs);
            log.debug("批量记录操作日志 {} 条", operationLogs.size());
        } catch (Exception e) {
            log.error("批量记录操作日志失败: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 获取日志统计概览
     */
    public Map<String, Object> getLogOverview(String familyId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        LocalDateTime thisWeek = now.minusDays(7);
        LocalDateTime thisMonth = now.minusDays(30);

        return Map.of(
            "todayOperations", operationLogRepository.countByFamilyIdAndTimeRange(familyId, today, now),
            "weekOperations", operationLogRepository.countByFamilyIdAndTimeRange(familyId, thisWeek, now),
            "monthOperations", operationLogRepository.countByFamilyIdAndTimeRange(familyId, thisMonth, now),
            "todaySecurityEvents", securityLogRepository.countByFamilyIdAndTimeRange(familyId, today, now),
            "weekSecurityEvents", securityLogRepository.countByFamilyIdAndTimeRange(familyId, thisWeek, now)
        );
    }
}