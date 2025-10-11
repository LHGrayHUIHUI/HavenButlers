package com.haven.storage.service;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.file.AccessLevel;
import com.haven.storage.file.FileMetadata;
import com.haven.storage.file.FileMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件权限管理服务
 *
 * 提供文件权限的动态转换和管理功能
 * 支持权限变更的访问控制规则和审计
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilePermissionService {

    private final FileMetadataService fileMetadataService;
    private final PermissionAuditService permissionAuditService;

    /**
     * 内存存储权限变更记录（实际应用中应替换为数据库）
     */
    private final Map<String, List<PermissionChangeRecord>> permissionChangeHistory = new ConcurrentHashMap<>();

    /**
     * 变更文件权限级别
     *
     * @param fileId 文件ID
     * @param userId 操作用户ID
     * @param newAccessLevel 新的权限级别
     * @param reason 变更原因
     * @return 变更结果
     */
    @TraceLog(value = "变更文件权限级别", module = "file-permission", type = "CHANGE_ACCESS_LEVEL")
    @Transactional
    public PermissionChangeResult changeFileAccessLevel(String fileId, String userId,
                                                      AccessLevel newAccessLevel, String reason) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            log.info("开始变更文件权限: fileId={}, userId={}, newLevel={}, reason={}, traceId={}",
                    fileId, userId, newAccessLevel, reason, traceId);

            // 1. 获取文件元数据
            FileMetadata fileMetadata = fileMetadataService.getFileMetadata(fileId);
            if (fileMetadata == null) {
                return PermissionChangeResult.builder()
                        .success(false)
                        .errorCode(40401)
                        .errorMessage("文件不存在")
                        .build();
            }

            // 2. 验证权限变更条件
            PermissionValidationResult validation = validatePermissionChange(fileMetadata, userId, newAccessLevel);
            if (!validation.isValid()) {
                return PermissionChangeResult.builder()
                        .success(false)
                        .errorCode(validation.getErrorCode())
                        .errorMessage(validation.getErrorMessage())
                        .build();
            }

            
            // 4. 记录变更前状态
            AccessLevel oldAccessLevel = fileMetadata.getAccessLevel();

            // 5. 执行权限变更
            fileMetadata.changeAccessLevel(userId, newAccessLevel, reason);
            fileMetadataService.updateFileMetadata(fileMetadata);

            // 6. 记录权限变更审计
            PermissionChangeRecord changeRecord = createPermissionChangeRecord(
                    fileId, userId, oldAccessLevel, newAccessLevel, reason, traceId);
            savePermissionChangeRecord(changeRecord);

            
            log.info("文件权限变更成功: fileId={}, userId={}, oldLevel={}, newLevel={}, traceId={}",
                    fileId, userId, oldAccessLevel, newAccessLevel, traceId);

            return PermissionChangeResult.builder()
                    .success(true)
                    .fileId(fileId)
                    .oldAccessLevel(oldAccessLevel)
                    .newAccessLevel(newAccessLevel)
                    .changeTime(changeRecord.getChangeTime())
                    .build();

        } catch (Exception e) {
            log.error("变更文件权限失败: fileId={}, userId={}, newLevel={}, error={}, traceId={}",
                    fileId, userId, newAccessLevel, e.getMessage(), traceId, e);
            return PermissionChangeResult.builder()
                    .success(false)
                    .errorCode(50001)
                    .errorMessage("权限变更失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 批量变更文件权限
     *
     * @param fileIds 文件ID列表
     * @param userId 操作用户ID
     * @param newAccessLevel 新的权限级别
     * @param reason 变更原因
     * @return 批量变更结果
     */
    @TraceLog(value = "批量变更文件权限", module = "file-permission", type = "BATCH_CHANGE_ACCESS_LEVEL")
    @Transactional
    public BatchPermissionChangeResult batchChangeAccessLevel(List<String> fileIds, String userId,
                                                             AccessLevel newAccessLevel, String reason) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            log.info("开始批量变更文件权限: fileCount={}, userId={}, newLevel={}, traceId={}",
                    fileIds.size(), userId, newAccessLevel, traceId);

            BatchPermissionChangeResult.Builder resultBuilder = new BatchPermissionChangeResult.Builder()
                    .totalFiles(fileIds.size());

            for (String fileId : fileIds) {
                try {
                    PermissionChangeResult changeResult = changeFileAccessLevel(fileId, userId, newAccessLevel, reason);
                    if (changeResult.isSuccess()) {
                        resultBuilder.successCount(resultBuilder.successCount + 1);
                        resultBuilder.addSuccessFile(fileId, changeResult);
                    } else {
                        resultBuilder.failureCount(resultBuilder.failureCount + 1);
                        resultBuilder.addFailureFile(fileId, changeResult.getErrorMessage());
                    }
                } catch (Exception e) {
                    resultBuilder.failureCount(resultBuilder.failureCount + 1);
                    resultBuilder.addFailureFile(fileId, "处理异常: " + e.getMessage());
                }
            }

            log.info("批量变更文件权限完成: total={}, success={}, failure={}, traceId={}",
                    fileIds.size(), resultBuilder.successCount, resultBuilder.failureCount, traceId);

            return resultBuilder.build();

        } catch (Exception e) {
            log.error("批量变更文件权限失败: userId={}, newLevel={}, error={}, traceId={}",
                    userId, newAccessLevel, e.getMessage(), traceId, e);
            throw new RuntimeException("批量权限变更失败", e);
        }
    }

    /**
     * 获取文件权限变更历史
     *
     * @param fileId 文件ID
     * @param userId 用户ID（用于验证权限）
     * @return 权限变更历史
     */
    @TraceLog(value = "获取权限变更历史", module = "file-permission", type = "GET_PERMISSION_HISTORY")
    public List<PermissionChangeRecord> getPermissionChangeHistory(String fileId, String userId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 验证用户权限（只有文件所有者可以查看权限变更历史）
            FileMetadata fileMetadata = fileMetadataService.getFileMetadata(fileId);
            if (fileMetadata == null || !userId.equals(fileMetadata.getOwnerId())) {
                log.warn("获取权限变更历史权限不足: fileId={}, userId={}, traceId={}", fileId, userId, traceId);
                return List.of();
            }

            List<PermissionChangeRecord> history = permissionChangeHistory.get(fileId);
            if (history == null || history.isEmpty()) {
                return List.of();
            }

            // 按时间倒序排列
            return history.stream()
                    .sorted((r1, r2) -> r2.getChangeTime().compareTo(r1.getChangeTime()))
                    .toList();

        } catch (Exception e) {
            log.error("获取权限变更历史失败: fileId={}, userId={}, error={}, traceId={}",
                    fileId, userId, e.getMessage(), traceId, e);
            return List.of();
        }
    }

    /**
     * 检查用户是否可以访问文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param userFamilyId 用户所属家庭ID
     * @return 访问检查结果
     */
    @TraceLog(value = "检查文件访问权限", module = "file-permission", type = "CHECK_ACCESS_PERMISSION")
    public FileAccessCheckResult checkFileAccessPermission(String fileId, String userId, String userFamilyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            FileMetadata fileMetadata = fileMetadataService.getFileMetadata(fileId);
            if (fileMetadata == null) {
                return FileAccessCheckResult.builder()
                        .accessible(false)
                        .errorCode(40401)
                        .errorMessage("文件不存在")
                        .build();
            }

            boolean accessible = fileMetadata.isAccessibleByUser(userId, userFamilyId);

            return FileAccessCheckResult.builder()
                    .accessible(accessible)
                    .fileId(fileId)
                    .fileName(fileMetadata.getOriginalFileName())
                    .accessLevel(fileMetadata.getAccessLevel())
                    .ownerId(fileMetadata.getOwnerId())
                    .familyId(fileMetadata.getFamilyId())
                    .build();

        } catch (Exception e) {
            log.error("检查文件访问权限失败: fileId={}, userId={}, error={}, traceId={}",
                    fileId, userId, e.getMessage(), traceId, e);
            return FileAccessCheckResult.builder()
                    .accessible(false)
                    .errorCode(50001)
                    .errorMessage("权限检查失败")
                    .build();
        }
    }

    // ===== 私有方法 =====

    /**
     * 验证权限变更条件
     */
    private PermissionValidationResult validatePermissionChange(FileMetadata fileMetadata, String userId,
                                                               AccessLevel newAccessLevel) {
        // 检查用户是否为文件所有者
        if (!userId.equals(fileMetadata.getOwnerId())) {
            return PermissionValidationResult.builder()
                    .valid(false)
                    .errorCode(40301)
                    .errorMessage("只有文件所有者可以变更权限级别")
                    .build();
        }

        // 检查权限级别是否相同
        if (newAccessLevel == fileMetadata.getAccessLevel()) {
            return PermissionValidationResult.builder()
                    .valid(false)
                    .errorCode(40001)
                    .errorMessage("权限级别没有变化")
                    .build();
        }

        // 检查文件状态
        if (!fileMetadata.isValidForOperation()) {
            return PermissionValidationResult.builder()
                    .valid(false)
                    .errorCode(40002)
                    .errorMessage("文件状态不允许权限变更")
                    .build();
        }

        return PermissionValidationResult.builder().valid(true).build();
    }

    
    /**
     * 检查权限是否被收窄
     */
    private boolean isAccessLevelNarrowed(AccessLevel oldLevel, AccessLevel newLevel) {
        // 权限级别：PUBLIC > FAMILY > PRIVATE
        return newLevel.ordinal() < oldLevel.ordinal();
    }

    
    
    /**
     * 创建权限变更记录
     */
    private PermissionChangeRecord createPermissionChangeRecord(String fileId, String userId,
                                                                AccessLevel oldLevel, AccessLevel newLevel,
                                                                String reason, String traceId) {
        return PermissionChangeRecord.builder()
                .recordId(generateRecordId())
                .fileId(fileId)
                .operatorId(userId)
                .oldAccessLevel(oldLevel)
                .newAccessLevel(newLevel)
                .changeReason(reason)
                .changeTime(LocalDateTime.now())
                .traceId(traceId)
                .build();
    }

    /**
     * 保存权限变更记录
     */
    private void savePermissionChangeRecord(PermissionChangeRecord record) {
        permissionChangeHistory.computeIfAbsent(record.getFileId(), k -> new ArrayList<>())
                .add(record);

        // 同时记录到审计服务
        permissionAuditService.recordPermissionChange(record);
    }

    /**
     * 生成记录ID
     */
    private String generateRecordId() {
        return "perm_change_" + System.currentTimeMillis() + "_" +
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }

    // ===== 结果类 =====

    /**
     * 权限变更结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PermissionChangeResult {
        private boolean success;
        private Integer errorCode;
        private String errorMessage;
        private String fileId;
        private AccessLevel oldAccessLevel;
        private AccessLevel newAccessLevel;
        private LocalDateTime changeTime;
    }

    /**
     * 批量权限变更结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchPermissionChangeResult {
        private Integer totalFiles;
        private Integer successCount;
        private Integer failureCount;
        private Map<String, PermissionChangeResult> successFiles;
        private Map<String, String> failureFiles;

        public static class Builder {
            private Integer totalFiles;
            private Integer successCount = 0;
            private Integer failureCount = 0;
            private Map<String, PermissionChangeResult> successFiles = new HashMap<>();
            private Map<String, String> failureFiles = new HashMap<>();

            public Builder totalFiles(Integer totalFiles) {
                this.totalFiles = totalFiles;
                return this;
            }

            public Builder successCount(Integer successCount) {
                this.successCount = successCount;
                return this;
            }

            public Builder failureCount(Integer failureCount) {
                this.failureCount = failureCount;
                return this;
            }

            public Builder addSuccessFile(String fileId, PermissionChangeResult result) {
                this.successFiles.put(fileId, result);
                return this;
            }

            public Builder addFailureFile(String fileId, String errorMessage) {
                this.failureFiles.put(fileId, errorMessage);
                return this;
            }

            public BatchPermissionChangeResult build() {
                return new BatchPermissionChangeResult(totalFiles, successCount, failureCount, successFiles, failureFiles);
            }
        }
    }

    /**
     * 文件访问检查结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FileAccessCheckResult {
        private boolean accessible;
        private Integer errorCode;
        private String errorMessage;
        private String fileId;
        private String fileName;
        private AccessLevel accessLevel;
        private String ownerId;
        private String familyId;
    }

    /**
     * 权限验证结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class PermissionValidationResult {
        private boolean valid;
        private Integer errorCode;
        private String errorMessage;
    }

    
    /**
     * 权限变更记录
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PermissionChangeRecord {
        private String recordId;
        private String fileId;
        private String operatorId;
        private AccessLevel oldAccessLevel;
        private AccessLevel newAccessLevel;
        private String changeReason;
        private LocalDateTime changeTime;
        private String traceId;
    }
}