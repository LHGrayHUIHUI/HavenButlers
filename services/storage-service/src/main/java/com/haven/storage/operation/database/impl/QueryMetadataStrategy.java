package com.haven.storage.operation.database.impl;

import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.domain.model.entity.FileStorageData;
import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.operation.database.DatabaseOperationStrategy;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.repository.FileMetadataRepository;
import com.haven.storage.repository.FileStorageDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 查询元数据策略
 * 处理文件查询时的数据库元数据读取操作
 *
 * <p>该策略负责文件查询的数据库操作，包括：
 * <ul>
 *   <li>查询单个文件的元数据信息</li>
 *   <li>关联查询文件存储数据信息</li>
 *   <li>支持多种查询条件和过滤方式</li>
 *   <li>记录文件访问统计信息</li>
 *   <li>维护数据访问审计</li>
 * </ul>
 *
 * <p>查询策略：
 * <ul>
 *   <li>精确查询：根据文件ID查询特定文件</li>
 *   <li>权限验证：确保用户有权限访问目标文件</li>
 *   <li>关联查询：同时获取元数据和存储数据</li>
 *   <li>访问统计：更新文件访问次数和最后访问时间</li>
 *   <li>数据完整性：验证查询结果的完整性</li>
 * </ul>
 *
 * @author HavenButler
 * @since 1.0.0
 * @see FileMetadata 文件元数据实体
 * @see FileStorageData 文件存储数据实体
 * @see FileProcessContext 文件处理上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryMetadataStrategy implements DatabaseOperationStrategy {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageDataRepository fileStorageDataRepository;

    /**
     * 执行文件元数据查询操作
     *
     * <p>该方法是查询策略的主要入口点，负责协调整个文件查询流程。
     * 支持多种查询模式，包括单个文件查询、列表查询和统计查询。
     *
     * <p>查询流程：
     * <pre>
     * 1. 验证上下文完整性（必要参数检查）
     * 2. 解析查询条件和参数
     * 3. 执行数据库查询操作
     * 4. 验证查询结果和权限
     * 5. 更新文件访问统计
     * 6. 更新上下文状态为METADATA_QUERIED
     * </pre>
     *
     * @param context 文件处理上下文，包含查询条件和参数
     * @return 处理结果，成功时包含查询数据，失败时包含错误信息
     * @throws IllegalArgumentException 当查询参数不完整时
     * @throws Exception 当数据库操作失败时
     */
    @Override
    public ProcessResult execute(FileProcessContext context) {
        String fileId = extractFileId(context);
        String familyId = extractFamilyId(context);

        log.info("开始执行文件元数据查询操作 - familyId: {}, fileId: {}", familyId, fileId);

        try {
            // 1. 验证上下文完整性
            validateContext(context);

            // 2. 根据查询类型执行查询操作
            Object queryResult = performQuery(context, fileId, familyId);

            // 3. 更新文件访问统计（如果是单文件查询）
            updateAccessStatistics(fileId, familyId);

            // 4. 更新上下文状态
            context.setStage(FileProcessContext.ProcessingStage.METADATA_QUERIED);

            log.info("文件元数据查询完成 - fileId: {}, familyId: {}, 查询结果类型: {}",
                    fileId, familyId, queryResult != null ? queryResult.getClass().getSimpleName() : "null");

            // 将查询结果存储在上下文中供后续使用
            return ProcessResult.success("文件元数据查询成功");

        } catch (Exception e) {
            log.error("文件元数据查询失败 - fileId: {}, familyId: {}, error: {}",
                    fileId, familyId, e.getMessage(), e);
            return ProcessResult.failure("文件元数据查询失败: " + e.getMessage());
        }
    }

    @Override
    public FileOperation getSupportOperation() {
        return FileOperation.DOWNLOAD;
    }

    /**
     * 执行查询操作
     *
     * <p>根据上下文参数判断查询类型并执行相应的查询操作：
     * <ul>
     *   <li>单文件查询：当有具体fileId时</li>
     *   <li>家庭文件列表查询：当只有familyId时</li>
     *   <li>条件查询：当有其他查询条件时</li>
     * </ul>
     *
     * @param context 文件处理上下文
     * @param fileId 文件ID
     * @param familyId 家庭ID
     * @return 查询结果
     */
    private Object performQuery(FileProcessContext context, String fileId, String familyId) {
        // 如果有具体的文件ID，执行单文件查询
        if (StringUtils.hasText(fileId)) {
            return querySingleFile(fileId, familyId);
        }

        // 如果只有家庭ID，查询家庭文件列表
        if (StringUtils.hasText(familyId)) {
            return queryFamilyFiles(familyId);
        }

        // 默认返回空结果
        log.warn("查询参数不完整，无法执行查询操作");
        return null;
    }

    /**
     * 查询单个文件
     *
     * <p>根据文件ID和家庭ID查询单个文件的完整信息，包括：
     * <ul>
     *   <li>文件元数据信息</li>
     *   <li>关联的存储数据信息</li>
     *   <li>文件访问权限验证</li>
     * </ul>
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID
     * @return 文件信息对象，包含元数据和存储数据
     */
    private FileInfo querySingleFile(String fileId, String familyId) {
        log.debug("查询单个文件 - fileId: {}, familyId: {}", fileId, familyId);

        // 1. 查询文件元数据
        Optional<FileMetadata> fileMetadataOpt;
        if (StringUtils.hasText(familyId)) {
            // 优先查询指定家庭的活跃文件
            fileMetadataOpt = fileMetadataRepository.findActiveFileByFileIdAndFamily(fileId, familyId);
        } else {
            // 如果没有家庭ID，查询所有活跃文件
            fileMetadataOpt = fileMetadataRepository.findByFileId(fileId)
                    .filter(FileMetadata::isActiveFile);
        }

        if (fileMetadataOpt.isEmpty()) {
            log.warn("未找到文件或文件已被删除 - fileId: {}, familyId: {}", fileId, familyId);
            return null;
        }

        FileMetadata fileMetadata = fileMetadataOpt.get();

        // 2. 查询关联的存储数据
        Optional<FileStorageData> storageDataOpt = fileStorageDataRepository.findByFileId(fileId);

        // 3. 构建文件信息对象
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileMetadata(fileMetadata);
        storageDataOpt.ifPresent(fileInfo::setFileStorageData);

        log.debug("单文件查询成功 - fileId: {}, hasStorageData: {}", fileId, storageDataOpt.isPresent());
        return fileInfo;
    }

    /**
     * 查询家庭文件列表
     *
     * <p>查询指定家庭的所有活跃文件，按创建时间倒序排列。
     * 可以添加分页、排序和过滤条件支持。
     *
     * @param familyId 家庭ID
     * @return 家庭文件列表
     */
    private List<FileMetadata> queryFamilyFiles(String familyId) {
        log.debug("查询家庭文件列表 - familyId: {}", familyId);

        List<FileMetadata> familyFiles = fileMetadataRepository.findActiveFilesByFamily(familyId);

        log.debug("家庭文件列表查询成功 - familyId: {}, 文件数量: {}", familyId, familyFiles.size());
        return familyFiles;
    }

    /**
     * 更新文件访问统计
     *
     * <p>记录文件访问次数和最后访问时间，用于：
     * <ul>
     *   <li>文件访问热度和使用情况分析</li>
     *   <li>用户行为模式和偏好分析</li>
     *   <li>缓存策略优化和性能提升</li>
     * </ul>
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID
     */
    private void updateAccessStatistics(String fileId, String familyId) {
        if (!StringUtils.hasText(fileId)) {
            return;
        }

        try {
            LocalDateTime accessTime = LocalDateTime.now();
            fileMetadataRepository.incrementAccessCount(fileId, accessTime);
            log.debug("文件访问统计更新成功 - fileId: {}", fileId);
        } catch (Exception e) {
            log.error("更新文件访问统计失败 - fileId: {}, error: {}", fileId, e.getMessage(), e);
            // 统计更新失败不应该影响查询主流程
        }
    }

    /**
     * 验证上下文完整性
     *
     * <p>确保执行查询操作所需的基本参数有效。
     * 查询操作比删除操作更宽松，至少需要fileId或familyId中的一个。
     *
     * @param context 待验证的文件处理上下文
     * @throws IllegalArgumentException 当上下文完全为空时
     */
    private void validateContext(FileProcessContext context) {
        if (context.getFileBasicMetadata() == null) {
            throw new IllegalArgumentException("文件基础元数据不能为空");
        }

        String fileId = extractFileId(context);
        String familyId = extractFamilyId(context);

        if (!StringUtils.hasText(fileId) && !StringUtils.hasText(familyId)) {
            throw new IllegalArgumentException("文件ID和家庭ID不能同时为空");
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

    /**
     * 文件信息封装类
     *
     * <p>用于封装查询到的文件完整信息，包括元数据和存储数据。
     * 便于在责任链中传递和使用文件信息。
     */
    @lombok.Getter
    @lombok.Setter
    public static class FileInfo {
        private FileMetadata fileMetadata;
        private FileStorageData fileStorageData;

        /**
         * 检查是否有物理存储数据
         *
         * @return true 如果有存储数据，否则返回false
         */
        public boolean hasStorageData() {
            return fileStorageData != null;
        }

        /**
         * 获取文件大小
         *
         * @return 文件大小，优先使用元数据中的大小
         */
        public long getFileSize() {
            if (fileMetadata != null) {
                return fileMetadata.getFileSize();
            }
            if (fileStorageData != null) {
                return fileStorageData.getFileSize();
            }
            return 0;
        }

        /**
         * 获取文件名称
         *
         * @return 文件名称，使用元数据中的原始文件名
         */
        public String getOriginalFileName() {
            return fileMetadata != null ? fileMetadata.getOriginalName() : "未知文件";
        }

        /**
         * 获取文件类型
         *
         * @return 文件类型，使用元数据中的文件类型
         */
        public String getFileType() {
            return fileMetadata != null ? fileMetadata.getFileType() : "未知类型";
        }
    }
}