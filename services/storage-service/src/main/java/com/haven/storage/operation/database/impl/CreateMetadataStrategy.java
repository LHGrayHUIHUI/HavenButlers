package com.haven.storage.operation.database.impl;

import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.domain.model.entity.FileStorageData;
import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.enums.FileStatus;
import com.haven.storage.domain.model.enums.SupportedFileType;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.operation.database.DatabaseOperationStrategy;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.domain.model.entity.FamilyStorageStats;
import com.haven.storage.repository.FamilyStorageStatsRepository;
import com.haven.storage.repository.FileMetadataRepository;
import com.haven.storage.repository.FileStorageDataRepository;
import com.haven.storage.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 创建元数据策略
 * 处理文件上传时的数据库元数据创建操作
 * 负责任链中的元数据持久化环节，将上下文中的文件信息保存到数据库
 *
 * <p>该策略是文件上传处理流程中的关键组件，负责：
 * <ul>
 *   <li>创建文件存储数据记录（FileStorageData）</li>
 *   <li>创建文件元数据记录（FileMetadata）</li>
 *   <li>更新家庭存储统计信息（FamilyStorageStats）</li>
 *   <li>维护数据一致性和完整性</li>
 * </ul>
 *
 * <p>处理流程：
 * <pre>
 * 1. 验证上下文完整性（必要参数检查）
 * 2. 创建文件存储数据记录（存储路径、大小、状态等）
 * 3. 创建文件元数据记录（文件信息、权限、分类等）
 * 4. 更新家庭存储统计（文件数量、大小、分类统计）
 * 5. 更新上下文状态为METADATA_PERSISTED
 * </pre>
 *
 * @author HavenButler
 * @since 1.0.0
 * @see FileStorageData 文件存储数据实体
 * @see FileMetadata 文件元数据实体
 * @see FamilyStorageStats 家庭存储统计实体
 * @see FileProcessContext 文件处理上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateMetadataStrategy implements DatabaseOperationStrategy {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageDataRepository fileStorageDataRepository;
    private final FamilyStorageStatsRepository familyStorageStatsRepository;

    /**
     * 执行文件元数据创建操作
     *
     * <p>该方法是策略的主要入口点，负责协调整个元数据创建流程。
     * 使用事务确保数据一致性，任何步骤失败都会回滚所有操作。
     *
     * <p>事务边界说明：
     * <ul>
     *   <li>整个方法在事务中执行，确保原子性</li>
     *   <li>任何异常都会触发回滚，保持数据一致性</li>
     *   <li>统计更新失败不会影响主流程，但会记录错误日志</li>
     * </ul>
     *
     * @param context 文件处理上下文，包含文件基础元数据和存储信息
     * @return 处理结果，成功时包含成功消息，失败时包含错误信息
     * @throws IllegalArgumentException 当上下文参数不完整时
     * @throws Exception 当数据库操作失败时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessResult execute(FileProcessContext context) {
        log.info("开始执行文件元数据创建操作 - familyId: {}, fileId: {}",
                context.getFileBasicMetadata().getFamilyId(), context.getFileBasicMetadata().getFileId());

        try {
            // 1. 验证上下文完整性（必要参数检查）
            validateContext(context);

            // 2. 创建文件存储数据记录（存储路径、大小、状态等）
            FileStorageData storageData = createFileStorageData(context);
            fileStorageDataRepository.save(storageData);
            log.debug("文件存储数据创建成功 - storageId: {}", storageData.getStorageId());

            // 3. 创建文件元数据记录（文件信息、权限、分类等）
            FileMetadata fileMetadata = createFileMetadata(context, storageData.getStorageId());
            fileMetadataRepository.save(fileMetadata);
            log.debug("文件元数据创建成功 - fileId: {}", fileMetadata.getFileId());

            // 4. 更新家庭存储统计（文件数量、大小、分类统计）
            updateFamilyStorageStats(context, fileMetadata);

            // 5. 更新上下文状态为METADATA_PERSISTED，标记元数据持久化完成
            context.setStage(FileProcessContext.ProcessingStage.METADATA_PERSISTED);

            log.info("文件元数据创建完成 - fileId: {}, storageId: {}",
                    fileMetadata.getFileId(), storageData.getStorageId());

            return ProcessResult.success("文件元数据创建成功");

        } catch (Exception e) {
            log.error("文件元数据创建失败 - fileId: {}, error: {}",
                    context.getFileBasicMetadata().getFileId(), e.getMessage(), e);
            return ProcessResult.failure("文件元数据创建失败: " + e.getMessage());
        }
    }

    @Override
    public FileOperation getSupportOperation() {
        return FileOperation.UPLOAD;
    }

    /**
     * 验证上下文完整性
     *
     * <p>确保执行元数据创建所需的所有必要参数都存在且有效。
     * 这是数据完整性的第一道防线，防止后续操作因参数缺失而失败。
     *
     * <p>验证规则：
     * <ul>
     *   <li>文件基础元数据不能为空</li>
     *   <li>文件存储信息不能为空</li>
     *   <li>家庭ID不能为空（用于数据隔离）</li>
     *   <li>文件ID不能为空（用于唯一标识）</li>
     * </ul>
     *
     * @param context 待验证的文件处理上下文
     * @throws IllegalArgumentException 当任何必要参数缺失时
     */
    private void validateContext(FileProcessContext context) {
        if (context.getFileBasicMetadata() == null) {
            throw new IllegalArgumentException("文件基础元数据不能为空");
        }
        if (context.getFileStorageInfo() == null) {
            throw new IllegalArgumentException("文件存储信息不能为空");
        }
        if (!StringUtils.hasText(context.getFileBasicMetadata().getFamilyId())) {
            throw new IllegalArgumentException("家庭ID不能为空");
        }
        if (!StringUtils.hasText(context.getFileBasicMetadata().getFileId())) {
            throw new IllegalArgumentException("文件ID不能为空");
        }
    }

    /**
     * 创建文件存储数据记录
     *
     * <p>根据上下文信息构建FileStorageData实体，该实体记录文件的物理存储信息。
     * 存储数据与元数据分离设计，便于存储管理和迁移。
     *
     * <p>存储数据包含：
     * <ul>
     *   <li>存储ID：唯一标识存储记录</li>
     *   <li>存储类型：LOCAL、MINIO、S3等</li>
     *   <li>存储路径：文件在存储系统中的位置</li>
     *   <li>文件大小：用于存储容量统计</li>
     *   <li>存储状态：启用、禁用、归档等</li>
     * </ul>
     *
     * @param context 文件处理上下文
     * @return 创建的文件存储数据实体
     */
    private FileStorageData createFileStorageData(FileProcessContext context) {
        var fileBasicMetadata = context.getFileBasicMetadata();
        var fileStorageInfo = context.getFileStorageInfo();
        FileStorageData storageData = new FileStorageData();

        // 生成唯一的存储ID，格式：存储类型前缀 + 唯一标识符
        storageData.setStorageId(FileUtils.generateStorageId(context.getStorageType()));

        // 关联文件元数据ID，建立存储数据与元数据的关联
        storageData.setFileId(fileBasicMetadata.getFileId());

        // 设置存储类型（LOCAL=0, MINIO=1, S3=2等）
        storageData.setStorageType(context.getStorageType().getCode());

        // 设置家庭存储桶名称，用于数据隔离和家庭级别管理
        storageData.setFamilyBucketName(fileStorageInfo.getFamilyBucketName());

        // 设置文件存储路径（相对路径）
        storageData.setFilePath(fileStorageInfo.getFilePath());

        // 设置完整访问路径（临时使用文件路径，后续可生成CDN链接）
        storageData.setFullAccessPath(fileStorageInfo.getFilePath());

        // 设置存储状态（1-正常启用）
        storageData.setStorageStatus(FileStatus.NORMAL.getCode());

        // 设置文件大小（用于存储容量统计和计费）
        storageData.setFileSize(fileBasicMetadata.getFileSize());

        // 创建时间和更新时间由JPA自动设置（@CreationTimestamp, @UpdateTimestamp）
        return storageData;
    }

    /**
     * 创建文件元数据记录
     *
     * <p>根据上下文信息构建FileMetadata实体，该实体记录文件的业务元数据。
     * 元数据包含文件的描述性信息，用于搜索、分类和权限管理。
     *
     * <p>元数据结构：
     * <ul>
     *   <li>基础信息：文件ID、名称、大小、类型</li>
     *   <li>权限信息：所有者、上传者、可见性</li>
     *   <li>分类信息：文件类型、MIME类型、标签</li>
     *   <li>统计信息：上传时间、访问次数</li>
     *   <li>业务信息：描述、权限设置</li>
     * </ul>
     *
     * @param context 文件处理上下文
     * @param storageId 关联的存储数据ID
     * @return 创建的文件元数据实体
     */
    private FileMetadata createFileMetadata(FileProcessContext context, String storageId) {
        var fileBasicMetadata = context.getFileBasicMetadata();
        FileMetadata fileMetadata = new FileMetadata();

        // === 基础标识信息 ===
        fileMetadata.setFileId(fileBasicMetadata.getFileId());
        fileMetadata.setStorageId(storageId);  // 关联存储数据

        // === 状态和删除标记 ===
        fileMetadata.setDeleted(0);  // 未删除状态
        fileMetadata.setStatus(1);   // 启用状态

        // === 归属和权限字段 ===
        fileMetadata.setFamilyId(fileBasicMetadata.getFamilyId());  // 家庭ID，用于数据隔离

        // 设置所有者相关信息（优先使用上下文中的用户，否则使用文件所有者）
        fileMetadata.setOwnerId(fileBasicMetadata.getOwnerId());
        fileMetadata.setUploaderUserId(fileBasicMetadata.getOwnerId());
        fileMetadata.setUploadedBy(fileBasicMetadata.getOwnerId());

        // === 文件基本信息字段 ===
        fileMetadata.setOriginalName(fileBasicMetadata.getFileName());  // 原始文件名
        fileMetadata.setFileSize(fileBasicMetadata.getFileSize());      // 文件大小

        // === 文件类型和MIME类型设置 ===
        SupportedFileType fileType = context.getFileBasicMetadata().getFileType();
        fileMetadata.setFileType(fileType.getCategory().getCategoryName());  // 文件分类（图片、文档、视频等）
        fileMetadata.setMimeType(fileType.getMimeType());                   // MIME类型
        fileMetadata.setContentType(fileType.getMimeType());                 // 内容类型（用于HTTP响应头）

        // === 上传和访问信息字段 ===
        fileMetadata.setUploadTime(LocalDateTime.now());  // 上传时间
        fileMetadata.setAccessCount(0);                  // 初始访问次数为0

        // === 标签和分类信息 ===
        fileMetadata.setTags(fileType.getExtensions());   // 初始标签（根据文件扩展名自动生成）
        fileMetadata.setDescription(fileBasicMetadata.getDescription());  // 文件描述（可选）

        // === 权限设置 ===
        fileMetadata.setFileVisibility(context.getFileBasicMetadata().getFileVisibility());  // 文件可见性（默认私有）

        return fileMetadata;
    }

    /**
     * 更新家庭存储统计信息
     *
     * <p>维护家庭级别的存储统计数据，用于存储配额管理、使用分析和计费。
     * 统计数据包括文件数量、总大小、分类统计、操作计数等。
     *
     * <p>统计策略：
     * <ul>
     *   <li>增量更新：如果统计记录存在，进行增量更新</li>
     *   <li>创建新记录：如果统计记录不存在，创建初始记录</li>
     *   <li>异步容错：统计更新失败不影响主流程</li>
     *   <li>分类统计：按文件类型维护详细统计</li>
     * </ul>
     *
     * @param context 文件处理上下文
     * @param fileMetadata 已创建的文件元数据
     */
    private void updateFamilyStorageStats(FileProcessContext context, FileMetadata fileMetadata) {
        String familyId = fileMetadata.getFamilyId();
        if (!StringUtils.hasText(familyId)) {
            log.warn("家庭ID为空，跳过存储统计更新 - fileId: {}", fileMetadata.getFileId());
            return;
        }

        try {
            LocalDateTime updateTime = LocalDateTime.now();
            long fileSize = fileMetadata.getFileSize();
            String fileType = fileMetadata.getFileType();

            // 检查是否存在统计记录
            boolean statsExists = familyStorageStatsRepository.existsByFamilyId(familyId);
            if (statsExists) {
                // 存在统计记录，进行增量更新
                log.debug("更新家庭存储统计 - familyId: {}, fileSize: {}, fileType: {}",
                         familyId, fileSize, fileType);

                // 更新总文件数量和总大小
                familyStorageStatsRepository.incrementFilesByFamily(familyId, fileSize, updateTime);

                // 更新特定文件类型的分类统计
                familyStorageStatsRepository.incrementCategoryCountByFamily(familyId, fileType, 1, updateTime);

                // 更新操作计数（上传计数+1，删除计数+0）
                familyStorageStatsRepository.updateOperationCountsByFamily(familyId, 1, 0, updateTime);
            } else {
                // 不存在统计记录，创建新的统计记录
                log.info("创建家庭存储统计记录 - familyId: {}", familyId);
                updateOrCreateFamilyStats(context, familyId, fileSize, fileType, updateTime);
            }

            log.debug("家庭存储统计更新成功 - familyId: {}", familyId);

        } catch (Exception e) {
            log.error("更新家庭存储统计失败 - familyId: {}, fileId: {}, error: {}",
                     familyId, fileMetadata.getFileId(), e.getMessage(), e);
            // 统计更新失败不应该影响文件上传的主要流程
            // 可以在这里添加异步重试机制或告警通知
        }
    }

    /**
     * 创建或更新家庭存储统计记录
     *
     * <p>当家庭的第一个文件上传时，需要创建初始的统计记录。
     * 该方法构建完整的FamilyStorageStats实体，包含所有必要的统计字段。
     *
     * <p>初始统计数据：
     * <ul>
     *   <li>基础统计：1个文件，文件大小为当前文件大小</li>
     *   <li>分类统计：按文件类型分类，当前类型计数为1</li>
     *   <li>文件特征：最大文件大小、最大文件名、最近文件时间</li>
     *   <li>操作统计：上传次数1次，删除次数0次</li>
     *   <li>存储健康：默认健康状态</li>
     *   <li>配额限制：默认100GB（可配置）</li>
     * </ul>
     *
     * @param context 文件处理上下文
     * @param familyId 家庭ID
     * @param fileSize 文件大小
     * @param fileType 文件类型
     * @param updateTime 更新时间
     */
    private void updateOrCreateFamilyStats(FileProcessContext context, String familyId, long fileSize, String fileType, LocalDateTime updateTime) {
        try {
            log.info("为家庭 {} 创建初始存储统计记录 - 文件大小: {}, 类型: {}",
                    familyId, fileSize, fileType);

            // 创建新的FamilyStorageStats实体
            FamilyStorageStats familyStats = new FamilyStorageStats();

            // === 设置基本信息 ===
            familyStats.setFamilyId(familyId);

            // 设置存储类型（从上下文获取，转换为小写字符串存储）
            if (context != null && context.getStorageType() != null) {
                familyStats.setStorageType(context.getStorageType().name().toLowerCase());
            }

            // === 设置初始统计数据 ===
            familyStats.setTotalFiles(1);        // 初始文件数量为1
            familyStats.setTotalSize(fileSize);  // 初始总大小为当前文件大小

            // === 根据文件类型设置分类统计 ===
            familyStats.incrementCategoryCount(fileType, 1);

            // === 设置文件特征信息 ===
            familyStats.setLargestFileSize(fileSize);                    // 最大文件大小（当前文件）
            familyStats.setLargestFileName(fileType + " file");          // 最大文件名（临时标识）
            familyStats.setMostRecentFileTime(updateTime);               // 最近文件时间

            // === 设置操作统计 ===
            familyStats.setTotalUploads(1);     // 总上传次数
            familyStats.setTotalDeletes(0);     // 总删除次数

            // === 设置存储健康状态（默认健康） ===
            familyStats.setStorageHealthyStatus(true);

            // === 设置默认配额限制（100GB，可根据需要调整） ===
            familyStats.setQuotaLimit(100L * 1024 * 1024 * 1024); // 100GB

            // 保存到数据库
            FamilyStorageStats savedStats = familyStorageStatsRepository.save(familyStats);

            log.info("成功创建家庭存储统计记录 - familyId: {}, statsId: {}, totalFiles: {}, totalSize: {}",
                    familyId, savedStats.getFamilyId(), savedStats.getTotalFiles(),
                    savedStats.getTotalSize());

        } catch (Exception e) {
            log.error("创建家庭存储统计记录失败 - familyId: {}, error: {}", familyId, e.getMessage(), e);
            // 统计记录创建失败不应该影响文件上传的主要流程
            // 可以在这里添加异步重试机制或告警通知
        }
    }


}