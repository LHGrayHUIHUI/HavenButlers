package com.haven.storage.controller;

import com.haven.base.annotation.TraceLog;
import com.haven.base.annotation.RateLimit;
import com.haven.base.annotation.Permission;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.utils.TraceIdUtil;

import com.haven.storage.domain.model.file.FileVisibility;
import com.haven.storage.service.FilePermissionService;
import com.haven.storage.service.PermissionAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件权限管理控制器
 *
 * 提供文件权限的变更、查询和管理功能
 * 支持权限级别转换、权限审计和风险分析
 *
 * @author HavenButler
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/storage/permissions")
@RequiredArgsConstructor
@Validated
@Tag(name = "文件权限管理", description = "文件权限管理相关接口")
public class PermissionController {

    private final FilePermissionService filePermissionService;
    private final PermissionAuditService permissionAuditService;

    /**
     * 变更文件权限级别
     *
     * @param request 权限变更请求
     * @return 变更结果
     */
    @PostMapping("/change-access-level")
    @Operation(summary = "变更文件权限级别", description = "变更指定文件的权限级别")
    @TraceLog(value = "变更文件权限级别", module = "permission-api", type = "CHANGE_ACCESS_LEVEL_API")
    @RateLimit(limit = 10, window = 60) // 每分钟最多10次权限变更
    @Permission(value = {"file:permission:change"})
    public ResponseWrapper<FilePermissionService.PermissionChangeResult> changeFileAccessLevel(
            @Valid @RequestBody PermissionChangeRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.info("接收文件权限变更请求: fileId={}, userId={}, newLevel={}, reason={}, traceId={}",
                request.getFileId(), request.getUserId(), request.getNewAccessLevel(), request.getReason(), traceId);

        // 参数校验
        validatePermissionChangeRequest(request);

        var result = filePermissionService.changeFileAccessLevel(
                request.getFileId(),
                request.getUserId(),
                request.getNewAccessLevel(),
                request.getReason()
        );

        if (!result.isSuccess()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, result.getErrorMessage());
        }

        log.info("文件权限变更成功: fileId={}, oldLevel={}, newLevel={}, traceId={}",
                result.getFileId(), result.getOldAccessLevel(), result.getNewAccessLevel(), traceId);

        return ResponseWrapper.success("权限变更成功", result);
    }

    /**
     * 批量变更文件权限
     *
     * @param request 批量权限变更请求
     * @return 批量变更结果
     */
    @PostMapping("/batch-change")
    @Operation(summary = "批量变更文件权限", description = "批量变更多个文件的权限级别")
    @TraceLog(value = "批量变更文件权限API", module = "permission-api", type = "BATCH_CHANGE_PERMISSION_API")
    @RateLimit(limit = 5, window = 300) // 5分钟最多5次批量操作
    @Permission(value = {"file:permission:batch"})
    public ResponseWrapper<FilePermissionService.BatchPermissionChangeResult> batchChangeAccessLevel(
            @Valid @RequestBody BatchPermissionChangeRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.info("接收批量文件权限变更请求: fileCount={}, userId={}, newLevel={}, traceId={}",
                request.getFileIds().size(), request.getUserId(), request.getNewAccessLevel(), traceId);

        // 参数校验
        validateBatchPermissionChangeRequest(request);

        var result = filePermissionService.batchChangeAccessLevel(
                request.getFileIds(),
                request.getUserId(),
                request.getNewAccessLevel(),
                request.getReason()
        );

        log.info("批量文件权限变更完成: total={}, success={}, failure={}, traceId={}",
                result.getTotalFiles(), result.getSuccessCount(), result.getFailureCount(), traceId);

        return ResponseWrapper.success("批量权限变更完成", result);
    }

    /**
     * 检查文件访问权限
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param familyId 家庭ID
     * @return 访问权限检查结果
     */
    @GetMapping("/check-access/{fileId}")
    @Operation(summary = "检查文件访问权限", description = "检查用户对指定文件的访问权限")
    @TraceLog(value = "检查文件访问权限API", module = "permission-api", type = "CHECK_ACCESS_PERMISSION_API")
    @RateLimit(limit = 100, window = 60) // 每分钟最多100次权限检查
    public ResponseWrapper<FilePermissionService.FileAccessCheckResult> checkFileAccessPermission(
            @Parameter(description = "文件ID", required = true)
            @PathVariable @NotBlank String fileId,

            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,

            @Parameter(description = "家庭ID", required = true)
            @RequestParam @NotBlank String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.debug("检查文件访问权限: fileId={}, userId={}, familyId={}, traceId={}",
                fileId, userId, familyId, traceId);

        // 参数校验
        validateAccessCheckParams(fileId, userId, familyId);

        var result = filePermissionService.checkFileAccessPermission(fileId, userId, familyId);

        // 记录访问尝试审计
        permissionAuditService.recordAccessAttempt(
                fileId, userId, "CHECK_ACCESS", result.isAccessible(),
                result.isAccessible() ? null : "权限不足");

        return ResponseWrapper.success(result);
    }

    /**
     * 获取文件权限变更历史
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param limit 记录数量限制
     * @return 权限变更历史
     */
    @GetMapping("/history/{fileId}")
    @Operation(summary = "获取权限变更历史", description = "获取指定文件的权限变更历史记录")
    @TraceLog(value = "获取权限变更历史API", module = "permission-api", type = "GET_PERMISSION_HISTORY_API")
    @RateLimit(limit = 50, window = 60) // 每分钟最多50次历史查询
    @Permission(value = {"file:permission:history"})
    public ResponseWrapper<List<FilePermissionService.PermissionChangeRecord>> getPermissionChangeHistory(
            @Parameter(description = "文件ID", required = true)
            @PathVariable @NotBlank String fileId,

            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,

            @Parameter(description = "记录数量限制")
            @RequestParam(defaultValue = "20") int limit) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.debug("获取权限变更历史: fileId={}, userId={}, limit={}, traceId={}",
                fileId, userId, limit, traceId);

        // 参数校验
        validateHistoryQueryParams(fileId, userId, limit);

        List<FilePermissionService.PermissionChangeRecord> history =
                filePermissionService.getPermissionChangeHistory(fileId, userId);

        return ResponseWrapper.success(history);
    }

    /**
     * 获取用户权限操作统计
     *
     * @param userId 用户ID
     * @param days 统计天数
     * @return 操作统计信息
     */
    @GetMapping("/statistics/{userId}")
    @Operation(summary = "获取用户权限操作统计", description = "获取指定用户的权限操作统计信息")
    @TraceLog(value = "获取用户权限统计API", module = "permission-api", type = "GET_PERMISSION_STATISTICS_API")
    @RateLimit(limit = 30, window = 60) // 每分钟最多30次统计查询
    @Permission(value = {"file:permission:statistics"})
    public ResponseWrapper<PermissionAuditService.PermissionOperationStatistics> getUserPermissionStatistics(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotBlank String userId,

            @Parameter(description = "统计天数")
            @RequestParam(defaultValue = "7") int days) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.debug("获取用户权限统计: userId={}, days={}, traceId={}", userId, days, traceId);

        // 参数校验
        validateStatisticsParams(userId, days);

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);

        var statistics = permissionAuditService.getUserPermissionStatistics(userId, startTime, endTime);

        return ResponseWrapper.success(statistics);
    }

    /**
     * 获取文件权限风险分析
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 风险分析结果
     */
    @GetMapping("/risk-analysis/{fileId}")
    @Operation(summary = "获取文件权限风险分析", description = "获取指定文件的权限风险分析")
    @TraceLog(value = "获取权限风险分析API", module = "permission-api", type = "GET_RISK_ANALYSIS_API")
    @Permission(value = {"file:permission:risk"})
    public ResponseWrapper<PermissionAuditService.PermissionRiskAnalysis> getFilePermissionRiskAnalysis(
            @Parameter(description = "文件ID", required = true)
            @PathVariable @NotBlank String fileId,

            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.debug("获取文件权限风险分析: fileId={}, userId={}, traceId={}", fileId, userId, traceId);

        // 参数校验
        validateRiskAnalysisParams(fileId, userId);

        var riskAnalysis = permissionAuditService.analyzePermissionRisk(fileId, userId);

        log.info("文件权限风险分析完成: fileId={}, riskLevel={}, riskScore={}, traceId={}",
                fileId, riskAnalysis.getRiskLevel(), riskAnalysis.getRiskScore(), traceId);

        return ResponseWrapper.success(riskAnalysis);
    }

    /**
     * 获取权限级别说明
     *
     * @return 权限级别说明
     */
    @GetMapping("/access-levels")
    @Operation(summary = "获取权限级别说明", description = "获取所有权限级别的说明信息")
    @RateLimit(limit = 20, window = 60) // 每分钟最多20次查询
    public ResponseWrapper<List<AccessLevelDescription>> getAccessLevelDescriptions() {
        List<AccessLevelDescription> descriptions = List.of(
                AccessLevelDescription.builder()
                        .level(FileVisibility.PRIVATE)
                        .name("私有文件")
                        .description("归属当前用户，绑定用户ID和家庭ID，支持通过分享功能向其他用户开放不同权限")
                        .permissions(List.of("查看", "修改", "删除", "分享"))
                        .canShare(true)
                        .build(),

                AccessLevelDescription.builder()
                        .level(FileVisibility.FAMILY)
                        .name("家庭文件")
                        .description("属于家庭分组，该家庭所有成员可访问，支持通过分享转为其他家庭成员的私有文件")
                        .permissions(List.of("查看"))
                        .canShare(true)
                        .build(),

                AccessLevelDescription.builder()
                        .level(FileVisibility.PUBLIC)
                        .name("公共文件")
                        .description("对所有登录用户开放访问权限，无需分享即可访问")
                        .permissions(List.of("查看"))
                        .canShare(false)
                        .build()
        );

        return ResponseWrapper.success(descriptions);
    }

    // ===== 私有校验方法 =====

    /**
     * 校验权限变更请求参数
     */
    private void validatePermissionChangeRequest(PermissionChangeRequest request) {
        if (request.getFileId() == null || request.getFileId().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "文件ID不能为空");
        }
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "用户ID不能为空");
        }
        if (request.getNewAccessLevel() == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "新权限级别不能为空");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "变更原因不能为空");
        }
    }

    /**
     * 校验批量权限变更请求参数
     */
    private void validateBatchPermissionChangeRequest(BatchPermissionChangeRequest request) {
        if (request.getFileIds() == null || request.getFileIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "文件ID列表不能为空");
        }
        if (request.getFileIds().size() > 100) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "批量操作文件数量不能超过100个");
        }
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "用户ID不能为空");
        }
        if (request.getNewAccessLevel() == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "新权限级别不能为空");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "批量变更原因不能为空");
        }
    }

    /**
     * 校验访问检查参数
     */
    private void validateAccessCheckParams(String fileId, String userId, String familyId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "文件ID不能为空");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "用户ID不能为空");
        }
        if (familyId == null || familyId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "家庭ID不能为空");
        }
    }

    /**
     * 校验历史查询参数
     */
    private void validateHistoryQueryParams(String fileId, String userId, int limit) {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "文件ID不能为空");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "用户ID不能为空");
        }
        if (limit <= 0 || limit > 100) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "记录数量限制必须在1-100之间");
        }
    }

    /**
     * 校验统计查询参数
     */
    private void validateStatisticsParams(String userId, int days) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "用户ID不能为空");
        }
        if (days <= 0 || days > 90) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "统计天数必须在1-90之间");
        }
    }

    /**
     * 校验风险分析参数
     */
    private void validateRiskAnalysisParams(String fileId, String userId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "文件ID不能为空");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "用户ID不能为空");
        }
    }

    // ===== 请求模型 =====

    /**
     * 权限变更请求
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PermissionChangeRequest {
        @NotBlank(message = "文件ID不能为空")
        private String fileId;

        @NotBlank(message = "用户ID不能为空")
        private String userId;

        @NotNull(message = "新权限级别不能为空")
        private FileVisibility newAccessLevel;

        @NotBlank(message = "变更原因不能为空")
        private String reason;
    }

    /**
     * 批量权限变更请求
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchPermissionChangeRequest {
        private List<String> fileIds;

        @NotBlank(message = "用户ID不能为空")
        private String userId;

        @NotNull(message = "新权限级别不能为空")
        private FileVisibility newAccessLevel;

        @NotBlank(message = "变更原因不能为空")
        private String reason;
    }

    /**
     * 权限级别说明
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AccessLevelDescription {
        private FileVisibility level;
        private String name;
        private String description;
        private List<String> permissions;
        private boolean canShare;
    }
}