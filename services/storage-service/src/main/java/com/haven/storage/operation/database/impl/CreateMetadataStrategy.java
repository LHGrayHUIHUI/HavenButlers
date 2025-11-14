package com.haven.storage.operation.database.impl;

import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.domain.model.entity.FileStorageData;
import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.enums.FileStatus;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.operation.database.DatabaseOperationStrategy;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.repository.FileMetadataRepository;
import com.haven.storage.repository.FileStorageDataRepository;
import com.haven.storage.service.FamilyStorageStatsService;
import com.haven.storage.service.converter.FileMetadataMapper;
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
 *   <li>调用存储统计服务更新统计信息</li>
 *   <li>维护数据一致性和完整性</li>
 * </ul>
 *
 * <p>处理流程：
 * <pre>
 * 1. 验证上下文完整性（必要参数检查）
 * 2. 创建文件存储数据记录（存储路径、大小、状态等）
 * 3. 创建文件元数据记录（文件信息、权限、分类等）
 * 4. 调用存储统计服务更新家庭存储统计
 * 5. 更新上下文状态为METADATA_PERSISTED
 * </pre>
 *
 * @author HavenButler
 * @since 1.0.0
 * @see FileStorageData 文件存储数据实体
 * @see FileMetadata 文件元数据实体
 * @see FileProcessContext 文件处理上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateMetadataStrategy implements DatabaseOperationStrategy {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageDataRepository fileStorageDataRepository;
    private final FamilyStorageStatsService familyStorageStatsService;
    private final FileMetadataMapper fileMetadataMapper;

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
            // 4. 更新家庭存储统计（文件数量、大小、分类统计）
            familyStorageStatsService.updateStorageStats(getSupportOperation(), fileMetadata,fileMetadata.getFileSize());

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
     * <p>委托给FileMetadataMapper进行完整的实体构建和验证。
     * 策略类专注于业务流程编排，将具体的数据转换逻辑交由专门的转换器处理。</p>
     *
     * <p>设计说明：
     * <ul>
     *   <li>单一职责：Strategy负责流程编排，Mapper负责数据转换</li>
     *   <li>代码复用：Mapper中的创建逻辑可被其他模块复用</li>
     *   <li>易于测试：转换逻辑可独立测试，业务逻辑也可单独测试</li>
     * </ul>
     *
     * @param context 文件处理上下文，包含文件基础信息
     * @param storageId 关联的存储数据ID
     * @return 创建并验证通过的文件元数据实体
     * @throws IllegalArgumentException 当参数无效或验证失败时
     * @throws RuntimeException 当创建过程中发生异常时
     */
    private FileMetadata createFileMetadata(FileProcessContext context, String storageId) {
        log.debug("开始创建文件元数据 - fileId: {}, storageId: {}",
                 context.getFileBasicMetadata().getFileId(), storageId);

        try {
            // 委托给转换器进行完整的创建和验证流程
            // 包含：参数验证 → 实体构建 → 完整性验证 → 异常处理
            return fileMetadataMapper.createAndValidateFileMetadata(context, storageId);

        } catch (Exception e) {
            log.error("文件元数据创建失败 - fileId: {}, error: {}",
                     context.getFileBasicMetadata().getFileId(), e.getMessage(), e);

            // 重新抛出异常，保持原有的异常类型和消息
            if (e instanceof IllegalArgumentException) {
                throw e; // 参数验证异常直接抛出
            }
            throw new RuntimeException("文件元数据创建失败: " + e.getMessage(), e);
        }
    }

}