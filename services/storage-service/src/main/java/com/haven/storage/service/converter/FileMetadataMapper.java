package com.haven.storage.service.converter;

import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.domain.model.file.FileBasicMetadata;
import com.haven.storage.domain.model.enums.FileVisibility;
import com.haven.storage.domain.model.enums.SupportedFileType;
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
}
