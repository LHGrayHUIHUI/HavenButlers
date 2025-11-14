package com.haven.storage.service.converter;

import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.domain.model.file.FileBasicMetadata;
import com.haven.storage.domain.model.enums.FileVisibility;
import com.haven.storage.domain.model.enums.SupportedFileType;
import com.haven.storage.processor.context.FileProcessContext;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 文件元数据实体和基础元数据模型之间的转换器
 *
 * <p>该转换器负责在 FileMetadata 实体和 FileBasicMetadata 模型之间进行转换，
 * 支持对象级别的映射和列表级别的批量转换。
 *
 * <p>转换功能：
 * <ul>
 *   <li>FileMetadata → FileBasicMetadata：实体转基础模型</li>
 *   <li>FileBasicMetadata → FileMetadata：基础模型转实体（预留）</li>
 *   <li>批量转换：支持集合级别的转换操作</li>
 *   <li>安全转换：处理null值和异常情况</li>
 * </ul>
 *
 * @author HavenButler
 * @since 1.0.0
 * @see FileMetadata 文件元数据实体
 * @see FileBasicMetadata 文件基础元数据模型
 */
@Slf4j
@Component
public class FileMetadataMapper {

    /**
     * 将 FileMetadata 实体转换为 FileBasicMetadata 模型
     *
     * <p>转换规则：
     * <ul>
     *   <li>基础属性映射：fileId, familyId, ownerId等</li>
     *   <li>文件属性映射：fileName, fileSize, fileType等</li>
     *   <li>权限属性映射：fileVisibility等</li>
     *   <li>时间属性映射：uploadTime, lastModifiedTime等</li>
     *   <li>安全处理：处理null值和空值情况</li>
     * </ul>
     *
     * @param fileMetadata 文件元数据实体，可能为null
     * @return 转换后的文件基础元数据模型，如果输入为null则返回null
     */
    public FileBasicMetadata toFileBasicMetadata(FileMetadata fileMetadata) {
        if (fileMetadata == null) {
            log.debug("FileMetadata为null，返回null");
            return null;
        }

        try {
            log.debug("开始转换FileMetadata到FileBasicMetadata - fileId: {}", fileMetadata.getFileId());

            FileBasicMetadata basicMetadata = new FileBasicMetadata();

            // 映射基础属性
            basicMetadata.setFileId(fileMetadata.getFileId());
            basicMetadata.setFamilyId(fileMetadata.getFamilyId());
            basicMetadata.setFileName(fileMetadata.getOriginalName());
            basicMetadata.setOwnerId(fileMetadata.getOwnerId());
            basicMetadata.setFileSize(fileMetadata.getFileSize());
            basicMetadata.setDescription(fileMetadata.getDescription());
            basicMetadata.setUploadTime(fileMetadata.getUploadTime());
            basicMetadata.setLastModifiedTime(fileMetadata.getUpdateTime());
            basicMetadata.setFileVisibility(fileMetadata.getFileVisibility());

            // 从文件名中提取文件格式
            String originalName = fileMetadata.getOriginalName();
            if (StringUtils.hasText(originalName) && originalName.contains(".")) {
                String fileFormat = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
                basicMetadata.setFileFormat(fileFormat);
            }

            // 转换文件类型枚举
            basicMetadata.setFileType(convertToSupportedFileType(fileMetadata.getFileType()));

            // 设置可选属性（目前为null，可根据需要从其他信息中提取）
            basicMetadata.setFolderPath(null); // 可从文件路径或其他信息中提取
            basicMetadata.setStorageKey(null); // 可从存储信息中提取

            log.debug("FileMetadata转换完成 - fileId: {}, fileName: {}",
                    basicMetadata.getFileId(), basicMetadata.getFileName());
            return basicMetadata;

        } catch (Exception e) {
            log.error("转换FileMetadata到FileBasicMetadata失败 - fileId: {}, error: {}",
                    fileMetadata.getFileId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 批量转换 FileMetadata 列表为 FileBasicMetadata 列表
     *
     * <p>对列表中的每个元素进行转换，过滤掉转换失败的元素。
     * 使用并行流处理提高批量转换性能。
     *
     * @param fileMetadataList 文件元数据实体列表
     * @return 转换后的文件基础元数据模型列表，过滤掉null元素
     */
    public List<FileBasicMetadata> toFileBasicMetadataList(List<FileMetadata> fileMetadataList) {
        if (fileMetadataList == null || fileMetadataList.isEmpty()) {
            log.debug("FileMetadata列表为空，返回空列表");
            return Collections.emptyList();
        }

        try {
            log.debug("开始批量转换FileMetadata列表 - 数量: {}", fileMetadataList.size());

            List<FileBasicMetadata> result = fileMetadataList.stream()
                    .map(this::toFileBasicMetadata)
                    .filter(java.util.Objects::nonNull)
                    .toList();

            log.debug("批量转换完成 - 输入数量: {}, 输出数量: {}",
                    fileMetadataList.size(), result.size());
            return result;

        } catch (Exception e) {
            log.error("批量转换FileMetadata列表失败 - 列表大小: {}, error: {}",
                    fileMetadataList.size(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 将字符串文件类型转换为 SupportedFileType 枚举
     *
     * <p>根据文件类型字符串匹配对应的枚举值。
     * 支持模糊匹配和大小写不敏感的转换。
     *
     * @param fileTypeStr 文件类型字符串
     * @return 匹配的SupportedFileType枚举，如果匹配失败则返回null
     */
    private SupportedFileType convertToSupportedFileType(String fileTypeStr) {
        if (!StringUtils.hasText(fileTypeStr)) {
            return null;
        }

        try {
            // 尝试通过枚举值查找
            for (SupportedFileType type : SupportedFileType.values()) {
                if (type.getCategory().getCategoryName().equalsIgnoreCase(fileTypeStr)) {
                    return type;
                }
            }

            log.debug("未找到匹配的文件类型枚举 - fileType: {}", fileTypeStr);
            return null;

        } catch (Exception e) {
            log.error("转换文件类型枚举失败 - fileType: {}, error: {}", fileTypeStr, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 验证转换结果的完整性
     *
     * <p>检查转换后的FileBasicMetadata是否包含必要的核心字段。
     * 用于调试和质量控制。
     *
     * @param basicMetadata 转换后的基础元数据
     * @return true 如果转换结果完整，否则返回false
     */
    public boolean validateConversionResult(FileBasicMetadata basicMetadata) {
        if (basicMetadata == null) {
            return false;
        }

        // 检查必要字段
        return StringUtils.hasText(basicMetadata.getFileId()) &&
               StringUtils.hasText(basicMetadata.getFamilyId()) &&
               basicMetadata.getFileSize() != null &&
               basicMetadata.getFileSize() >= 0;
    }

    /**
     * 估算转换性能指标
     *
     * <p>用于性能监控和优化建议。
     *
     * @param inputSize 输入对象数量
     * @param outputSize 输出对象数量
     * @return 转换成功率（0.0到1.0之间）
     */
    public double calculateConversionRate(int inputSize, int outputSize) {
        if (inputSize == 0) {
            return 0.0;
        }
        return (double) outputSize / inputSize;
    }

    /**
     * 从文件处理上下文构建文件元数据实体
     *
     * <p>将文件处理上下文中的信息转换为FileMetadata实体，包含完整的字段映射。
     * 该方法专注于数据转换，不包含业务逻辑验证。</p>
     *
     * <p>转换内容：
     * <ul>
     *   <li>基础标识信息：fileId, storageId</li>
     *   <li>状态信息：删除标记、启用状态</li>
     *   <li>归属信息：家庭ID、所有者信息</li>
     *   <li>文件信息：名称、大小、类型、MIME类型</li>
     *   <li>时间信息：上传时间、访问次数</li>
     *   <li>权限信息：可见性设置</li>
     *   <li>标签和描述信息</li>
     * </ul>
     *
     * @param context 文件处理上下文，包含文件基础信息
     * @param storageId 关联的存储数据ID
     * @return 构建完成的文件元数据实体
     * @throws IllegalArgumentException 当context或必要参数为空时
     */
    public FileMetadata fromContextToFileMetadata(FileProcessContext context, String storageId) {
        if (context == null) {
            throw new IllegalArgumentException("文件处理上下文不能为空");
        }
        if (!StringUtils.hasText(storageId)) {
            throw new IllegalArgumentException("存储ID不能为空");
        }

        var fileBasicMetadata = context.getFileBasicMetadata();
        if (fileBasicMetadata == null) {
            throw new IllegalArgumentException("文件基础元数据不能为空");
        }

        try {
            log.debug("开始构建文件元数据实体 - fileId: {}, storageId: {}",
                     fileBasicMetadata.getFileId(), storageId);

            FileMetadata fileMetadata = new FileMetadata();

            // === 基础标识信息 ===
            fileMetadata.setFileId(fileBasicMetadata.getFileId());
            fileMetadata.setStorageId(storageId);

            // === 状态和删除标记 ===
            fileMetadata.setDeleted(0);  // 未删除状态
            fileMetadata.setStatus(1);   // 启用状态

            // === 归属和权限字段 ===
            fileMetadata.setFamilyId(fileBasicMetadata.getFamilyId());  // 家庭ID，用于数据隔离

            // 设置所有者相关信息
            fileMetadata.setOwnerId(fileBasicMetadata.getOwnerId());
            fileMetadata.setUploaderUserId(fileBasicMetadata.getOwnerId());
            fileMetadata.setUploadedBy(fileBasicMetadata.getOwnerId());

            // === 文件基本信息字段 ===
            fileMetadata.setOriginalName(fileBasicMetadata.getFileName());  // 原始文件名
            fileMetadata.setFileSize(fileBasicMetadata.getFileSize());      // 文件大小

            // === 文件类型和MIME类型设置 ===
            SupportedFileType fileType = context.getFileBasicMetadata().getFileType();
            if (fileType != null) {
                fileMetadata.setFileType(fileType.getCategory().getCategoryName());  // 文件分类（图片、文档、视频等）
                fileMetadata.setMimeType(fileType.getMimeType());                   // MIME类型
                fileMetadata.setContentType(fileType.getMimeType());                 // 内容类型（用于HTTP响应头）
                fileMetadata.setTags(fileType.getExtensions());   // 初始标签（根据文件扩展名自动生成）
            }

            // === 上传和访问信息字段 ===
            fileMetadata.setUploadTime(LocalDateTime.now());  // 上传时间
            fileMetadata.setAccessCount(0);                  // 初始访问次数为0

            // === 标签和分类信息 ===
            fileMetadata.setDescription(fileBasicMetadata.getDescription());  // 文件描述（可选）

            // === 权限设置 ===
            fileMetadata.setFileVisibility(context.getFileBasicMetadata().getFileVisibility());  // 文件可见性（默认私有）

            log.debug("文件元数据实体构建完成 - fileId: {}", fileMetadata.getFileId());
            return fileMetadata;

        } catch (Exception e) {
            log.error("构建文件元数据实体失败 - fileId: {}, error: {}",
                     fileBasicMetadata.getFileId(), e.getMessage(), e);
            throw new RuntimeException("构建文件元数据实体失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从文件处理上下文创建并验证文件元数据实体
     *
     * <p>这是一个高级工厂方法，包含了完整的创建和验证流程。
     * 适用于需要完整FileMetadata实体的业务场景。</p>
     *
     * <p>处理流程：
     * <ul>
     *   <li>参数验证：确保context和storageId有效</li>
     *   <li>实体构建：调用fromContextToFileMetadata进行构建</li>
     *   <li>完整性验证：调用validateFileMetadata确保数据质量</li>
     *   <li>异常处理：提供详细的错误信息和日志</li>
     * </ul>
     *
     * @param context 文件处理上下文，包含文件基础信息
     * @param storageId 关联的存储数据ID
     * @return 创建并验证通过的文件元数据实体
     * @throws IllegalArgumentException 当参数无效或验证失败时
     * @throws RuntimeException 当创建过程中发生异常时
     */
    public FileMetadata createAndValidateFileMetadata(FileProcessContext context, String storageId) {
        if (context == null) {
            throw new IllegalArgumentException("文件处理上下文不能为空");
        }
        if (!StringUtils.hasText(storageId)) {
            throw new IllegalArgumentException("存储ID不能为空");
        }

        var fileBasicMetadata = context.getFileBasicMetadata();
        if (fileBasicMetadata == null) {
            throw new IllegalArgumentException("文件基础元数据不能为空");
        }

        log.debug("开始创建并验证文件元数据实体 - fileId: {}, storageId: {}",
                 fileBasicMetadata.getFileId(), storageId);

        try {
            // 1. 构建文件元数据实体
            FileMetadata fileMetadata = fromContextToFileMetadata(context, storageId);
            log.debug("文件元数据实体构建成功 - fileId: {}", fileMetadata.getFileId());

            // 2. 验证实体完整性
            if (!validateFileMetadata(fileMetadata)) {
                throw new IllegalArgumentException("构建的文件元数据不完整或无效");
            }

            log.debug("文件元数据实体验证通过 - fileId: {}", fileMetadata.getFileId());
            return fileMetadata;

        } catch (Exception e) {
            log.error("创建并验证文件元数据实体失败 - fileId: {}, storageId: {}, error: {}",
                     fileBasicMetadata.getFileId(), storageId, e.getMessage(), e);

            if (e instanceof IllegalArgumentException) {
                throw e; // 重新抛出参数验证异常
            }
            throw new RuntimeException("创建并验证文件元数据实体失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证文件元数据实体的完整性
     *
     * <p>检查文件元数据实体是否包含必要的核心字段。
     * 用于数据质量控制和业务规则验证。</p>
     *
     * <p>验证字段：
     * <ul>
     *   <li>fileId：文件ID不能为空</li>
     *   <li>familyId：家庭ID不能为空（数据隔离要求）</li>
     *   <li>fileSize：文件大小必须为非负数</li>
     *   <li>storageId：存储ID不能为空</li>
     * </ul>
     *
     * @param fileMetadata 待验证的文件元数据实体
     * @return true 如果验证通过，否则返回false
     */
    public boolean validateFileMetadata(FileMetadata fileMetadata) {
        if (fileMetadata == null) {
            log.debug("FileMetadata为null，验证失败");
            return false;
        }

        // 检查必要字段
        boolean isValid = StringUtils.hasText(fileMetadata.getFileId()) &&
                         StringUtils.hasText(fileMetadata.getFamilyId()) &&
                         StringUtils.hasText(fileMetadata.getStorageId()) &&
                         fileMetadata.getFileSize() != null &&
                         fileMetadata.getFileSize() >= 0;

        if (!isValid) {
            log.debug("FileMetadata验证失败 - fileId: {}, familyId: {}, storageId: {}, fileSize: {}",
                     fileMetadata.getFileId(), fileMetadata.getFamilyId(),
                     fileMetadata.getStorageId(), fileMetadata.getFileSize());
        } else {
            log.debug("FileMetadata验证通过 - fileId: {}", fileMetadata.getFileId());
        }

        return isValid;
    }
}
