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
 * 更新元数据策略
 * 处理文件修改时的数据库元数据更新操作
 *
 * <p>该策略负责文件更新时的数据库操作，包括：
 * <ul>
 *   <li>更新文件元数据记录（文件名、描述、大小等）</li>
 *   <li>更新文件存储数据记录（存储路径、大小等）</li>
 *   <li>更新家庭存储统计信息（大小变化）</li>
 *   <li>维护数据一致性和完整性</li>
 * </ul>
 *
 * <p>更新策略：
 * <ul>
 *   <li>存在性验证：确保文件存在且未被删除</li>
 *   <li>增量更新：只更新变化的字段</li>
 *   <li>统计同步：更新家庭存储统计</li>
 *   <li>事务保护：确保更新操作的原子性</li>
 * </ul>
 *
 * @author HavenButler
 * @since 1.0.0
 * @see FileMetadata 文件元数据实体
 * @see FileStorageData 文件存储数据实体
 * @see FamilyStorageStatsService 家庭存储统计服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateMetadataStrategy implements DatabaseOperationStrategy {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageDataRepository fileStorageDataRepository;
    private final FamilyStorageStatsService familyStorageStatsService;

    /**
     * 执行文件元数据更新操作
     *
     * <p>该方法是更新策略的主要入口点，负责协调整个文件更新流程。
     * 使用事务确保数据一致性，任何步骤失败都会回滚所有操作。
     *
     * <p>更新流程：
     * <pre>
     * 1. 验证上下文完整性（必要参数检查）
     * 2. 查询并验证文件存在性
     * 3. 更新文件元数据记录（名称、描述、大小等）
     * 4. 更新文件存储数据记录（存储路径、大小等）
     * 5. 更新家庭存储统计（大小变化）
     * 6. 更新上下文状态为COMPLETED
     * </pre>
     *
     * @param context 文件处理上下文，包含文件基础元数据和存储信息
     * @return 处理结果，成功时包含成功消息，失败时包含错误信息
     * @throws IllegalArgumentException 当上下文参数不完整时
     * @throws Exception 当数据库操作失败时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResult execute(FileProcessContext context) {
        String fileId = extractFileId(context);
        String familyId = extractFamilyId(context);

        log.info("开始执行文件元数据更新操作 - familyId: {}, fileId: {}", familyId, fileId);

        try {
            // 1. 验证上下文完整性
            validateContext(context);

            // 2. 查询文件元数据记录
            FileMetadata existingFileMetadata = findFileMetadata(fileId, familyId);
            if (existingFileMetadata == null) {
                return ProcessResult.failure("文件不存在或已被删除 - fileId: " + fileId);
            }

            // 3. 查询文件存储数据记录
            FileStorageData existingStorageData = findFileStorageData(fileId);
            if (existingStorageData == null) {
                return ProcessResult.failure("文件存储数据不存在 - fileId: " + fileId);
            }

            // 4. 更新文件元数据记录
            FileMetadata updatedFileMetadata = updateFileMetadata(context, existingFileMetadata);
            fileMetadataRepository.save(updatedFileMetadata);

            // 5. 更新文件存储数据记录（如果存储信息有变化）
            FileStorageData updatedStorageData = updateFileStorageData(context, existingStorageData);
            fileStorageDataRepository.save(updatedStorageData);

            // 6. 更新家庭存储统计（处理文件大小变化）
            updateFamilyStorageStats(familyId, existingFileMetadata, updatedFileMetadata);

            // 7. 更新上下文状态
            context.setStage(FileProcessContext.ProcessingStage.COMPLETED);

            log.info("文件元数据更新完成 - fileId: {}, familyId: {}", fileId, familyId);
            return ProcessResult.success("文件元数据更新成功");

        } catch (Exception e) {
            log.error("文件元数据更新失败 - fileId: {}, familyId: {}, error: {}",
                    fileId, familyId, e.getMessage(), e);
            return ProcessResult.failure("文件元数据更新失败: " + e.getMessage());
        }
    }

    @Override
    public FileOperation getSupportOperation() {
        return FileOperation.MODIFY;
    }

    /**
     * 验证上下文完整性
     *
     * <p>确保执行更新操作所需的所有必要参数都存在且有效。
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
     * 如果文件不存在或已被删除，返回null表示无法进行更新操作。
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID
     * @return 文件元数据记录，如果不存在或已删除则返回null
     */
    private FileMetadata findFileMetadata(String fileId, String familyId) {
        log.debug("查询文件元数据 - fileId: {}, familyId: {}", fileId, familyId);

        return fileMetadataRepository.findActiveFileByFileIdAndFamily(fileId, familyId)
                .orElseGet(() -> {
                    log.debug("未找到活跃的文件元数据，查询所有记录 - fileId: {}", fileId);
                    // 如果没有活跃记录，查询所有记录（可能已经被删除）
                    return fileMetadataRepository.findByFileId(fileId).orElse(null);
                });
    }

    /**
     * 查询文件存储数据记录
     *
     * <p>根据文件ID查询文件的存储数据记录。
     * 如果存储数据不存在，表示文件无法正常访问。
     *
     * @param fileId 文件ID
     * @return 文件存储数据记录，如果不存在则返回null
     */
    private FileStorageData findFileStorageData(String fileId) {
        log.debug("查询文件存储数据 - fileId: {}", fileId);

        return fileStorageDataRepository.findByFileId(fileId).orElse(null);
    }

    /**
     * 更新文件元数据记录
     *
     * <p>根据上下文信息更新文件元数据，只更新变化的字段。
     * 支持文件名、描述、可见性等信息的更新。
     *
     * @param context 文件处理上下文
     * @param existingFileMetadata 现有的文件元数据
     * @return 更新后的文件元数据
     */
    private FileMetadata updateFileMetadata(FileProcessContext context, FileMetadata existingFileMetadata) {
        log.debug("开始更新文件元数据 - fileId: {}", existingFileMetadata.getFileId());

        var fileBasicMetadata = context.getFileBasicMetadata();

        // 更新文件名（如果发生变化）
        if (StringUtils.hasText(fileBasicMetadata.getFileName()) &&
            !fileBasicMetadata.getFileName().equals(existingFileMetadata.getOriginalName())) {
            existingFileMetadata.setOriginalName(fileBasicMetadata.getFileName());
            log.debug("更新文件名 - fileId: {}, newName: {}",
                     existingFileMetadata.getFileId(), fileBasicMetadata.getFileName());
        }

        // 更新文件描述（如果发生变化）
        if (fileBasicMetadata.getDescription() != null &&
            !fileBasicMetadata.getDescription().equals(existingFileMetadata.getDescription())) {
            existingFileMetadata.setDescription(fileBasicMetadata.getDescription());
            log.debug("更新文件描述 - fileId: {}", existingFileMetadata.getFileId());
        }

        // 更新文件大小（如果发生变化）
        if (fileBasicMetadata.getFileSize() != null &&
            !fileBasicMetadata.getFileSize().equals(existingFileMetadata.getFileSize())) {
            existingFileMetadata.setFileSize(fileBasicMetadata.getFileSize());
            log.debug("更新文件大小 - fileId: {}, newSize: {}",
                     existingFileMetadata.getFileId(), fileBasicMetadata.getFileSize());
        }

        // 更新文件可见性（如果发生变化）
        if (fileBasicMetadata.getFileVisibility() != null &&
            !fileBasicMetadata.getFileVisibility().equals(existingFileMetadata.getFileVisibility())) {
            existingFileMetadata.setFileVisibility(fileBasicMetadata.getFileVisibility());
            log.debug("更新文件可见性 - fileId: {}, newVisibility: {}",
                     existingFileMetadata.getFileId(), fileBasicMetadata.getFileVisibility());
        }

        // 更新修改时间
        existingFileMetadata.setUpdateTime(LocalDateTime.now());

        log.debug("文件元数据更新完成 - fileId: {}", existingFileMetadata.getFileId());
        return existingFileMetadata;
    }

    /**
     * 更新文件存储数据记录
     *
     * <p>根据上下文信息更新文件存储数据，主要处理存储路径和大小的变化。
     *
     *
     * @param context 文件处理上下文
     * @param existingStorageData 现有的文件存储数据
     * @return 更新后的文件存储数据
     */
    private FileStorageData updateFileStorageData(FileProcessContext context, FileStorageData existingStorageData) {
        log.debug("开始更新文件存储数据 - storageId: {}", existingStorageData.getStorageId());

        var fileStorageInfo = context.getFileStorageInfo();
        var fileBasicMetadata = context.getFileBasicMetadata();

        // 更新存储路径（如果发生变化）
        if (fileStorageInfo != null && StringUtils.hasText(fileStorageInfo.getFilePath()) &&
            !fileStorageInfo.getFilePath().equals(existingStorageData.getFilePath())) {
            existingStorageData.setFilePath(fileStorageInfo.getFilePath());
            existingStorageData.setFullAccessPath(fileStorageInfo.getFilePath());
            log.debug("更新存储路径 - storageId: {}, newPath: {}",
                     existingStorageData.getStorageId(), fileStorageInfo.getFilePath());
        }

        // 更新家庭存储桶名称（如果发生变化）
        if (fileStorageInfo != null && StringUtils.hasText(fileStorageInfo.getFamilyBucketName()) &&
            !fileStorageInfo.getFamilyBucketName().equals(existingStorageData.getFamilyBucketName())) {
            existingStorageData.setFamilyBucketName(fileStorageInfo.getFamilyBucketName());
            log.debug("更新家庭存储桶 - storageId: {}, newBucket: {}",
                     existingStorageData.getStorageId(), fileStorageInfo.getFamilyBucketName());
        }

        // 更新文件大小（同步元数据的大小）
        if (fileBasicMetadata.getFileSize() != null &&
            !fileBasicMetadata.getFileSize().equals(existingStorageData.getFileSize())) {
            existingStorageData.setFileSize(fileBasicMetadata.getFileSize());
            log.debug("同步更新存储文件大小 - storageId: {}, newSize: {}",
                     existingStorageData.getStorageId(), fileBasicMetadata.getFileSize());
        }

        // 更新修改时间
        existingStorageData.setUpdateTime(LocalDateTime.now());

        log.debug("文件存储数据更新完成 - storageId: {}", existingStorageData.getStorageId());
        return existingStorageData;
    }

    /**
     * 更新家庭存储统计信息
     *
     * <p>处理文件大小变化对家庭存储统计的影响。
     * 只有当文件大小发生变化时才需要更新统计。
     *
     * @param familyId 家庭ID
     * @param originalMetadata 原始文件元数据
     * @param updatedMetadata 更新后的文件元数据
     */
    private void updateFamilyStorageStats(String familyId, FileMetadata originalMetadata, FileMetadata updatedMetadata) {
        if (originalMetadata == null || updatedMetadata == null) {
            log.warn("文件元数据为空，跳过统计更新 - familyId: {}", familyId);
            return;
        }

        // 检查文件大小是否发生变化
        long originalFileSize = originalMetadata.getFileSize();

        long updatedSize = updatedMetadata.getFileSize();
        long sizeDifference = updatedSize - originalFileSize;

        if (sizeDifference == 0) {
            log.debug("文件大小未发生变化，跳过统计更新 - familyId: {}, fileId: {}",
                     familyId, updatedMetadata.getFileId());
            return;
        }

        try {
            log.debug("更新家庭存储统计（文件大小变化） - familyId: {}, sizeDiff: {}",
                     familyId, sizeDifference);
            // 委托给统计服务处理统计更新
            familyStorageStatsService.updateStorageStats(getSupportOperation(),updatedMetadata, sizeDifference);

            log.debug("家庭存储统计更新完成 - familyId: {}", familyId);

        } catch (Exception e) {
            log.error("更新家庭存储统计失败 - familyId: {}, fileId: {}, error: {}",
                     familyId, updatedMetadata.getFileId(), e.getMessage(), e);
            // 统计更新失败不应该影响文件更新的主要流程
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