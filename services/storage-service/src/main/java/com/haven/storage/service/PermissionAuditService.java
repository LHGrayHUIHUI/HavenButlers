package com.haven.storage.service;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.file.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 权限审计服务
 *
 * 负责记录和管理文件权限变更的审计日志
 * 提供权限变更的追踪和分析功能
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionAuditService {

    /**
     * 内存存储审计记录（实际应用中应替换为数据库）
     */
    private final Map<String, List<PermissionAuditRecord>> auditRecords = new ConcurrentHashMap<>();

    /**
     * 记录权限变更
     *
     * @param changeRecord 权限变更记录
     */
    @TraceLog(value = "记录权限变更审计", module = "permission-audit", type = "RECORD_PERMISSION_CHANGE")
    public void recordPermissionChange(FilePermissionService.PermissionChangeRecord changeRecord) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            PermissionAuditRecord auditRecord = PermissionAuditRecord.builder()
                    .auditId(generateAuditId())
                    .fileId(changeRecord.getFileId())
                    .operatorId(changeRecord.getOperatorId())
                    .operationType(PermissionOperationType.CHANGE_ACCESS_LEVEL)
                    .oldAccessLevel(changeRecord.getOldAccessLevel())
                    .newAccessLevel(changeRecord.getNewAccessLevel())
                    .operationReason(changeRecord.getChangeReason())
                    .operationTime(changeRecord.getChangeTime())
                    .traceId(changeRecord.getTraceId())
                    .auditTime(LocalDateTime.now())
                    .build();

            // 保存审计记录
            auditRecords.computeIfAbsent(changeRecord.getFileId(), k -> new CopyOnWriteArrayList<>())
                    .add(auditRecord);

            log.debug("权限变更审计记录成功: fileId={}, operatorId={}, oldLevel={}, newLevel={}, auditId={}",
                    changeRecord.getFileId(), changeRecord.getOperatorId(),
                    changeRecord.getOldAccessLevel(), changeRecord.getNewAccessLevel(), auditRecord.getAuditId());

        } catch (Exception e) {
            log.error("记录权限变更审计失败: fileId={}, error={}, traceId={}",
                    changeRecord.getFileId(), e.getMessage(), traceId, e);
        }
    }

    /**
     * 记录权限访问尝试
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param operation 操作类型
     * @param result 访问结果
     * @param failureReason 失败原因
     */
    @TraceLog(value = "记录权限访问审计", module = "permission-audit", type = "RECORD_ACCESS_ATTEMPT")
    public void recordAccessAttempt(String fileId, String userId, String operation,
                                   boolean result, String failureReason) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            PermissionAuditRecord auditRecord = PermissionAuditRecord.builder()
                    .auditId(generateAuditId())
                    .fileId(fileId)
                    .operatorId(userId)
                    .operationType(result ? PermissionOperationType.ACCESS_GRANTED : PermissionOperationType.ACCESS_DENIED)
                    .accessOperation(operation)
                    .accessResult(result)
                    .failureReason(failureReason)
                    .operationTime(LocalDateTime.now())
                    .traceId(traceId)
                    .auditTime(LocalDateTime.now())
                    .build();

            // 保存审计记录
            auditRecords.computeIfAbsent(fileId, k -> new CopyOnWriteArrayList<>())
                    .add(auditRecord);

            log.debug("权限访问审计记录成功: fileId={}, userId={}, operation={}, result={}, auditId={}",
                    fileId, userId, operation, result, auditRecord.getAuditId());

        } catch (Exception e) {
            log.error("记录权限访问审计失败: fileId={}, userId={}, error={}, traceId={}",
                    fileId, userId, e.getMessage(), traceId, e);
        }
    }

    /**
     * 获取文件权限审计记录
     *
     * @param fileId 文件ID
     * @param userId 用户ID（用于验证权限）
     * @param limit 记录数量限制
     * @return 审计记录列表
     */
    public List<PermissionAuditRecord> getFilePermissionAuditRecords(String fileId, String userId, int limit) {
        // 验证用户权限（这里应该验证用户是否有权限查看审计记录）
        List<PermissionAuditRecord> records = auditRecords.get(fileId);
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        // 按时间倒序排列，并限制数量
        return records.stream()
                .sorted((r1, r2) -> r2.getAuditTime().compareTo(r1.getAuditTime()))
                .limit(limit)
                .toList();
    }

    /**
     * 获取用户的权限操作统计
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作统计信息
     */
    public PermissionOperationStatistics getUserPermissionStatistics(String userId,
                                                                 LocalDateTime startTime,
                                                                 LocalDateTime endTime) {
        int totalOperations = 0;
        int accessGranted = 0;
        int accessDenied = 0;
        int permissionChanges = 0;

        for (List<PermissionAuditRecord> records : auditRecords.values()) {
            for (PermissionAuditRecord record : records) {
                if (!userId.equals(record.getOperatorId())) {
                    continue;
                }

                if (record.getOperationTime().isBefore(startTime) ||
                    record.getOperationTime().isAfter(endTime)) {
                    continue;
                }

                totalOperations++;

                switch (record.getOperationType()) {
                    case ACCESS_GRANTED:
                        accessGranted++;
                        break;
                    case ACCESS_DENIED:
                        accessDenied++;
                        break;
                    case CHANGE_ACCESS_LEVEL:
                        permissionChanges++;
                        break;
                }
            }
        }

        return PermissionOperationStatistics.builder()
                .userId(userId)
                .startTime(startTime)
                .endTime(endTime)
                .totalOperations(totalOperations)
                .accessGranted(accessGranted)
                .accessDenied(accessDenied)
                .permissionChanges(permissionChanges)
                .accessSuccessRate(totalOperations > 0 ? (double) accessGranted / totalOperations : 0.0)
                .build();
    }

    /**
     * 获取权限风险分析
     *
     * @param fileId 文件ID
     * @param userId 用户ID（用于验证权限）
     * @return 风险分析结果
     */
    public PermissionRiskAnalysis analyzePermissionRisk(String fileId, String userId) {
        List<PermissionAuditRecord> records = auditRecords.get(fileId);
        if (records == null || records.isEmpty()) {
            return PermissionRiskAnalysis.builder()
                    .fileId(fileId)
                    .riskLevel(PermissionRiskAnalysis.RiskLevel.LOW)
                    .riskScore(0)
                    .riskFactors(List.of())
                    .build();
        }

        List<String> riskFactors = new ArrayList<>();
        int riskScore = 0;

        // 检查权限变更频率
        long recentChanges = records.stream()
                .filter(r -> r.getOperationType() == PermissionOperationType.CHANGE_ACCESS_LEVEL)
                .filter(r -> r.getOperationTime().isAfter(LocalDateTime.now().minusDays(7)))
                .count();
        if (recentChanges > 5) {
            riskFactors.add("权限变更频繁 - 最近7天变更" + recentChanges + "次");
            riskScore += 20;
        }

        // 检查访问拒绝率
        long recentAccessAttempts = records.stream()
                .filter(r -> r.getOperationType() == PermissionOperationType.ACCESS_GRANTED ||
                              r.getOperationType() == PermissionOperationType.ACCESS_DENIED)
                .filter(r -> r.getOperationTime().isAfter(LocalDateTime.now().minusDays(1)))
                .count();
        long recentAccessDenied = records.stream()
                .filter(r -> r.getOperationType() == PermissionOperationType.ACCESS_DENIED)
                .filter(r -> r.getOperationTime().isAfter(LocalDateTime.now().minusDays(1)))
                .count();

        if (recentAccessAttempts > 0) {
            double denialRate = (double) recentAccessDenied / recentAccessAttempts;
            if (denialRate > 0.3) {
                riskFactors.add("访问拒绝率过高 - 最近24小时拒绝率" +
                        String.format("%.1f%%", denialRate * 100));
                riskScore += 25;
            }
        }

        // 检查异常操作模式
        Set<String> uniqueOperators = records.stream()
                .filter(r -> r.getOperationTime().isAfter(LocalDateTime.now().minusDays(7)))
                .map(PermissionAuditRecord::getOperatorId)
                .collect(Collectors.toSet());
        if (uniqueOperators.size() > 3) {
            riskFactors.add("操作者分散 - 最近7天有" + uniqueOperators.size() + "个不同用户操作");
            riskScore += 15;
        }

        // 确定风险等级
        PermissionRiskAnalysis.RiskLevel riskLevel;
        if (riskScore >= 60) {
            riskLevel = PermissionRiskAnalysis.RiskLevel.HIGH;
        } else if (riskScore >= 30) {
            riskLevel = PermissionRiskAnalysis.RiskLevel.MEDIUM;
        } else {
            riskLevel = PermissionRiskAnalysis.RiskLevel.LOW;
        }

        return PermissionRiskAnalysis.builder()
                .fileId(fileId)
                .riskLevel(riskLevel)
                .riskScore(riskScore)
                .riskFactors(riskFactors)
                .analysisTime(LocalDateTime.now())
                .build();
    }

    /**
     * 清理过期审计记录
     *
     * @param days 保留天数
     * @return 清理的记录数量
     */
    public int cleanupExpiredAuditRecords(int days) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        int cleanedCount = 0;

        for (Map.Entry<String, List<PermissionAuditRecord>> entry : auditRecords.entrySet()) {
            List<PermissionAuditRecord> records = entry.getValue();
            int originalSize = records.size();

            // 移除过期记录
            records.removeIf(record -> record.getAuditTime().isBefore(cutoffTime));

            cleanedCount += originalSize - records.size();
        }

        log.info("清理过期权限审计记录完成: cleanedCount={}, days={}", cleanedCount, days);
        return cleanedCount;
    }

    // ===== 私有方法 =====

    /**
     * 生成审计ID
     */
    private String generateAuditId() {
        return "audit_" + System.currentTimeMillis() + "_" +
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }

    // ===== 实体类 =====

    /**
     * 权限审计记录
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PermissionAuditRecord {
        private String auditId;
        private String fileId;
        private String operatorId;
        private PermissionOperationType operationType;
        private String accessOperation;
        private AccessLevel oldAccessLevel;
        private AccessLevel newAccessLevel;
        private String operationReason;
        private boolean accessResult;
        private String failureReason;
        private LocalDateTime operationTime;
        private String traceId;
        private LocalDateTime auditTime;
    }

    /**
     * 权限操作统计
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PermissionOperationStatistics {
        private String userId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalOperations;
        private int accessGranted;
        private int accessDenied;
        private int permissionChanges;
        private double accessSuccessRate;
    }

    /**
     * 权限风险分析
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PermissionRiskAnalysis {
        private String fileId;
        private RiskLevel riskLevel;
        private Integer riskScore;
        private List<String> riskFactors;
        private LocalDateTime analysisTime;

        /**
         * 风险等级枚举
         */
        public enum RiskLevel {
            LOW("低风险"),
            MEDIUM("中风险"),
            HIGH("高风险");

            private final String description;

            RiskLevel(String description) {
                this.description = description;
            }

            public String getDescription() {
                return description;
            }
        }
    }

    /**
     * 权限操作类型枚举
     */
    public enum PermissionOperationType {
        CHANGE_ACCESS_LEVEL("变更权限级别"),
        ACCESS_GRANTED("访问允许"),
        ACCESS_DENIED("访问拒绝");

        private final String description;

        PermissionOperationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}