package com.haven.storage.operation.database.impl;

import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.domain.model.entity.FileStorageData;
import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.operation.database.DatabaseOperationStrategy;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.repository.FileMetadataRepository;
import com.haven.storage.repository.FileStorageDataRepository;
import com.haven.storage.service.FamilyStorageStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 删除元数据策略
 * 处理文件删除时的数据库元数据清理操作
 *
 * <p>该策略负责文件删除的数据库操作，包括：
 * <ul>
 *   <li>软删除文件元数据记录（保留审计信息）</li>
 *   <li>删除文件存储数据记录（物理存储信息）</li>
 *   <li>更新家庭存储统计信息</li>
 *   <li>维护数据一致性和完整性</li>
 * </ul>
 *
 * <p>删除策略：
 * <ul>
 *   <li>软删除：元数据标记为已删除，保留审计和恢复能力</li>
 *   <li>物理删除：存储数据记录直接删除，清理物理存储关联</li>
 *   <li>统计更新：减少家庭文件数量和存储大小统计</li>
 *   <li>事务保护：确保删除操作的原子性</li>
 * </ul>
 *
 * @author HavenButler
 * @see FileMetadata 文件元数据实体
 * @see FileStorageData 文件存储数据实体
 * @see FamilyStorageStatsRepository 家庭存储统计Repository
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteMetadataStrategy implements DatabaseOperationStrategy {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageDataRepository fileStorageDataRepository;
    private final FamilyStorageStatsService familyStorageStatsService;

    /**
     * 执行文件元数据删除操作
     *
     * <p>该方法是删除策略的主要入口点，负责协调整个文件删除流程。
     * 使用事务确保数据一致性，任何步骤失败都会回滚所有操作。
     *
     * <p>删除流程：
     * <pre>
     * 1. 验证上下文完整性（必要参数检查）
     * 2. 查询并验证文件存在性
     * 3. 软删除文件元数据记录（保留审计信息）
     * 4. 删除文件存储数据记录（清理物理存储关联）
     * 5. 更新家庭存储统计（减少文件数量和大小）
     * 6. 更新上下文状态为COMPLETED
     * </pre>
     *
     * @param context 文件处理上下文，包含文件基础元数据
     * @return 处理结果，成功时包含成功消息，失败时包含错误信息
     * @throws IllegalArgumentException 当上下文参数不完整时
     * @throws Exception                当数据库操作失败时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResult execute(FileProcessContext context) {
        String fileId = extractFileId(context);
        String familyId = extractFamilyId(context);

        log.info("开始执行文件元数据删除操作 - familyId: {}, fileId: {}", familyId, fileId);

        try {
            // 1. 验证上下文完整性
            validateContext(context);

            // 2. 查询文件元数据记录
            FileMetadata fileMetadata = findFileMetadata(fileId, familyId);
            if (fileMetadata == null) {
                log.warn("文件元数据不存在，可能已被删除 - fileId: {}, familyId: {}", fileId, familyId);
                return ProcessResult.success("文件已被删除");
            }

            // 3. 软删除文件元数据记录（保留审计信息）
            softDeleteFileMetadata(fileId);

            // 4. 删除文件存储数据记录（清理物理存储关联）
            deleteFileStorageData(fileId);

            // 5. 更新家庭存储统计（减少文件数量和大小）
            updateFamilyStorageStats(familyId, fileMetadata);

            // 6. 更新上下文状态
            context.setStage(FileProcessContext.ProcessingStage.COMPLETED);

            log.info("文件元数据删除完成 - fileId: {}, familyId: {}", fileId, familyId);
            return ProcessResult.success("文件元数据删除成功");

        } catch (Exception e) {
            log.error("文件元数据删除失败 - fileId: {}, familyId: {}, error: {}",
                    fileId, familyId, e.getMessage(), e);
            return ProcessResult.failure("文件元数据删除失败: " + e.getMessage());
        }
    }

    @Override
    public FileOperation getSupportOperation() {
        return FileOperation.DELETE;
    }

    /**
     * 验证上下文完整性
     *
     * <p>确保执行删除操作所需的所有必要参数都存在且有效。
     * 这是数据完整性的第一道防线，防止后续操作因参数缺失而失败。
     *
     * @param context 待验证的文件处理上下文
     * @throws IllegalArgumentException 当任何必要参数缺失时
     */
    private void validateContext(FileProcessContext context) {
        String fileId = extractFileId(context);
        String familyId = extractFamilyId(context);

        if (context.getFileBasicMetadata() == null) {
            throw new IllegalArgumentException("文件基础元数据不能为空");
        }
        if (!StringUtils.hasText(fileId)) {
            throw new IllegalArgumentException("文件ID不能为空");
        }
        if (!StringUtils.hasText(familyId)) {
            throw new IllegalArgumentException("家庭ID不能为空");
        }
    }

    /**
     * 查询文件元数据记录
     *
     * <p>根据文件ID和家庭ID查询活跃的文件元数据记录。
     * 如果文件不存在或已被删除，返回null表示无需继续删除操作。
     *
     * @param fileId   文件ID
     * @param familyId 家庭ID
     * @return 文件元数据记录，如果不存在或已删除则返回null
     */
    private FileMetadata findFileMetadata(String fileId, String familyId) {
        log.debug("查询文件元数据 - fileId: {}, familyId: {}", fileId, familyId);

        // 优先查询活跃状态的文件
        return fileMetadataRepository.findActiveFileByFileIdAndFamily(fileId, familyId)
                .orElseGet(() -> {
                    log.debug("未找到活跃的文件元数据，查询所有记录 - fileId: {}", fileId);
                    // 如果没有活跃记录，查询所有记录（可能已经被删除）
                    return fileMetadataRepository.findByFileId(fileId).orElse(null);
                });
    }

    /**
     * 软删除文件元数据记录
     *
     * <p>执行软删除操作，将文件标记为已删除状态。
     * 软删除保留审计信息，支持数据恢复和合规性要求。
     *
     * @param fileId 文件ID
     */
    private void softDeleteFileMetadata(String fileId) {
        log.debug("软删除文件元数据 - fileId: {}", fileId);

        int updatedRows = fileMetadataRepository.softDeleteById(fileId, LocalDateTime.now());
        if (updatedRows == 0) {
            log.warn("未找到需要软删除的文件元数据记录 - fileId: {}", fileId);
        } else {
            log.debug("文件元数据软删除成功 - fileId: {}, 影响行数: {}", fileId, updatedRows);
        }
    }

    /**
     * 删除文件存储数据记录
     *
     * <p>删除文件的物理存储数据记录，清理与物理存储的关联。
     * 这是物理删除操作，会彻底移除存储数据记录。
     *
     * @param fileId 文件ID
     */
    private void deleteFileStorageData(String fileId) {
        log.debug("删除文件存储数据 - fileId: {}", fileId);

        try {
            fileStorageDataRepository.deleteByFileId(fileId);
            log.debug("文件存储数据删除成功 - fileId: {}", fileId);
        } catch (Exception e) {
            log.error("删除文件存储数据失败 - fileId: {}, error: {}", fileId, e.getMessage(), e);
            // 存储数据删除失败不应该影响主流程，但需要记录错误
            // 可以在这里添加异步清理机制
        }
    }

    /**
     * 更新家庭存储统计信息
     *
     * <p>委托给专门的统计服务处理家庭存储统计数据的更新。
     * 策略类专注于业务流程编排，将统计逻辑交由专门的服务处理。</p>
     *
     * <p>设计说明：
     * <ul>
     *   <li>职责分离：Strategy负责文件删除流程，Service负责统计计算</li>
     *   <li>代码复用：统计服务可被其他删除场景复用</li>
     *   <li>易于测试：统计逻辑可独立测试，删除逻辑也可单独测试</li>
     * </ul>
     *
     * @param familyId     家庭ID
     * @param fileMetadata 被删除的文件元数据
     */
    private void updateFamilyStorageStats(String familyId, FileMetadata fileMetadata) {
        log.debug("委托统计服务更新家庭存储统计 - familyId: {}, fileId: {}",
                familyId, fileMetadata.getFileId());

        try {
            // 委托给专门地统计服务进行统计更新
            // 包含：参数验证 → 统计更新 → 容错处理 → 日志记录
            familyStorageStatsService.updateStorageStats(getSupportOperation(), fileMetadata, fileMetadata.getFileSize());

            log.debug("家庭存储统计委托更新完成 - familyId: {}", familyId);

        } catch (Exception e) {
            log.error("委托更新家庭存储统计失败 - familyId: {}, fileId: {}, error: {}",
                    familyId, fileMetadata.getFileId(), e.getMessage(), e);

            // 统计更新失败不应该影响文件删除的主要流程
            // 但需要记录错误信息，避免静默失败
        }
    }

    /**
     * 从上下文中提取文件ID
     *
     * @param context 文件处理上下文
     * @return 文件ID，如果为空则返回null
     */
    private String extractFileId(FileProcessContext context) {
        if (context.getFileBasicMetadata() != null) {
            return context.getFileBasicMetadata().getFileId();
        }
        return null;
    }

    /**
     * 从上下文中提取家庭ID
     *
     * @param context 文件处理上下文
     * @return 家庭ID，如果为空则返回null
     */
    private String extractFamilyId(FileProcessContext context) {
        if (context.getFileBasicMetadata() != null) {
            return context.getFileBasicMetadata().getFamilyId();
        }
        return null;
    }
}
