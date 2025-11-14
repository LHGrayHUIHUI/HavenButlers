package com.haven.storage.service.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haven.storage.domain.model.entity.FileStorageData;
import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.domain.model.file.FileStorageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件存储数据实体和存储信息模型之间的转换器
 *
 * <p>该转换器负责在 FileStorageData 实体和 FileStorageInfo 模型之间进行转换，
 * 支持对象级别的映射和列表级别的批量转换。
 *
 * <p>转换功能：
 * <ul>
 *   <li>FileStorageData → FileStorageInfo：实体转存储信息模型</li>
 *   <li>FileStorageInfo → FileStorageData：存储信息模型转实体（预留）</li>
 *   <li>批量转换：支持集合级别的转换操作</li>
 *   <li>JSON解析：处理存储配置的JSON转换</li>
 * </ul>
 *
 * @author HavenButler
 * @since 1.0.0
 * @see FileStorageData 文件存储数据实体
 * @see FileStorageInfo 文件存储信息模型
 */
@Slf4j
@Component
public class FileStorageDataMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将 FileStorageData 实体转换为 FileStorageInfo 模型
     *
     * <p>转换规则：
     * <ul>
     *   <li>基础属性映射：storageId, fileId, familyBucketName等</li>
     *   <li>存储类型映射：Integer枚举值转换为StorageType枚举</li>
     *   <li>存储配置解析：JSON字符串转换为Map结构</li>
     *   <li>文件属性映射：filePath, fileSize, fileChecksum等</li>
     *   <li>状态属性映射：storageStatus转换为fileStatus</li>
     *   <li>时间属性映射：创建时间、更新时间等</li>
     *   <li>安全处理：处理null值和异常情况</li>
     * </ul>
     *
     * @param fileStorageData 文件存储数据实体，可能为null
     * @return 转换后的文件存储信息模型，如果输入为null则返回null
     */
    public FileStorageInfo toFileStorageInfo(FileStorageData fileStorageData) {
        if (fileStorageData == null) {
            log.debug("FileStorageData为null，返回null");
            return null;
        }

        try {
            log.debug("开始转换FileStorageData到FileStorageInfo - storageId: {}", fileStorageData.getStorageId());

            FileStorageInfo storageInfo = new FileStorageInfo();

            // 映射基础属性
            storageInfo.setFileId(fileStorageData.getFileId());
            storageInfo.setFamilyBucketName(fileStorageData.getFamilyBucketName());
            storageInfo.setFilePath(fileStorageData.getFilePath());
            storageInfo.setFileSize(fileStorageData.getFileSize());
            storageInfo.setCreateTime(fileStorageData.getCreateTime());
            storageInfo.setUpdateTime(fileStorageData.getUpdateTime());

            // 转换存储类型枚举
            storageInfo.setStorageType(convertToStorageType(fileStorageData.getStorageType()));

            // 解析存储配置JSON
            storageInfo.setStorageConfig(parseStorageConfig(fileStorageData.getStorageConfig()));

            // 设置文件MD5（使用fileChecksum字段）
            storageInfo.setFileMd5(fileStorageData.getFileChecksum());

            // 转换文件状态（存储状态转换为文件状态）
            storageInfo.setFileStatus(convertToFileStatus(fileStorageData.getStorageStatus()));

            log.debug("FileStorageData转换完成 - storageId: {}, fileId: {}",
                    fileStorageData.getStorageId(), storageInfo.getFileId());
            return storageInfo;

        } catch (Exception e) {
            log.error("转换FileStorageData到FileStorageInfo失败 - storageId: {}, error: {}",
                    fileStorageData.getStorageId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 批量转换 FileStorageData 列表为 FileStorageInfo 列表
     *
     * <p>对列表中的每个元素进行转换，过滤掉转换失败的元素。
     * 使用并行流处理提高批量转换性能。
     *
     * @param fileStorageDataList 文件存储数据实体列表
     * @return 转换后的文件存储信息模型列表，过滤掉null元素
     */
    public List<FileStorageInfo> toFileStorageInfoList(List<FileStorageData> fileStorageDataList) {
        if (fileStorageDataList == null || fileStorageDataList.isEmpty()) {
            log.debug("FileStorageData列表为空，返回空列表");
            return Collections.emptyList();
        }

        try {
            log.debug("开始批量转换FileStorageData列表 - 数量: {}", fileStorageDataList.size());

            List<FileStorageInfo> result = fileStorageDataList.stream()
                    .map(this::toFileStorageInfo)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            log.debug("批量转换完成 - 输入数量: {}, 输出数量: {}",
                    fileStorageDataList.size(), result.size());
            return result;

        } catch (Exception e) {
            log.error("批量转换FileStorageData列表失败 - 列表大小: {}, error: {}",
                    fileStorageDataList.size(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 将整数存储类型转换为 StorageType 枚举
     *
     * <p>根据整数类型码匹配对应的StorageType枚举值。
     * 支持类型安全的转换和错误处理。
     *
     * @param storageTypeInt 存储类型整数码
     * @return 匹配的StorageType枚举，如果匹配失败则返回null
     */
    private StorageType convertToStorageType(Integer storageTypeInt) {
        if (storageTypeInt == null) {
            return null;
        }

        try {
            for (StorageType type : StorageType.values()) {
                if (type.getCode().equals(storageTypeInt)) {
                    return type;
                }
            }

            log.debug("未找到匹配的存储类型枚举 - storageTypeInt: {}", storageTypeInt);
            return null;

        } catch (Exception e) {
            log.error("转换存储类型枚举失败 - storageTypeInt: {}, error: {}", storageTypeInt, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析存储配置JSON字符串
     *
     * <p>将JSON格式的存储配置字符串解析为Map结构。
     * 支持各种存储后端的配置格式。
     *
     * @param storageConfigJson 存储配置JSON字符串
     * @return 解析后的配置Map，如果解析失败则返回空Map
     */
    private Map<String, String> parseStorageConfig(String storageConfigJson) {
        if (!StringUtils.hasText(storageConfigJson)) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(storageConfigJson, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析存储配置JSON失败 - json: {}, error: {}", storageConfigJson, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 转换存储状态为文件状态
     *
     * <p>将存储状态的枚举值转换为文件状态的表示：
     * <ul>
     *   <li>0 (可用) → 0 (正常)</li>
     *   <li>1 (不可用) → 1 (删除)</li>
     *   <li>其他 → 2 (过期)</li>
     * </ul>
     *
     * @param storageStatus 存储状态值
     * @return 转换后的文件状态值
     */
    private Integer convertToFileStatus(Integer storageStatus) {
        if (storageStatus == null) {
            return 2; // 默认为过期状态
        }

        return switch (storageStatus) {
            case 0 -> 0; // 可用 → 正常
            case 1 -> 1; // 不可用 → 删除
            default -> 2; // 其他 → 过期
        };
    }

    /**
     * 验证转换结果的完整性
     *
     * <p>检查转换后的FileStorageInfo是否包含必要的核心字段。
     * 用于调试和质量控制。
     *
     * @param storageInfo 转换后的存储信息
     * @return true 如果转换结果完整，否则返回false
     */
    public boolean validateConversionResult(FileStorageInfo storageInfo) {
        if (storageInfo == null) {
            return false;
        }

        // 检查必要字段
        return StringUtils.hasText(storageInfo.getFileId()) &&
               StringUtils.hasText(storageInfo.getFilePath()) &&
               storageInfo.getStorageType() != null;
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
     * 根据存储类型过滤转换结果
     *
     * <p>从转换结果列表中过滤出指定存储类型的记录。
     * 支持按存储类型进行数据分析和统计。
     *
     * @param storageInfoList 存储信息列表
     * @param storageType 指定的存储类型
     * @return 过滤后的存储信息列表
     */
    public List<FileStorageInfo> filterByStorageType(List<FileStorageInfo> storageInfoList, StorageType storageType) {
        if (storageInfoList == null || storageInfoList.isEmpty()) {
            return Collections.emptyList();
        }

        return storageInfoList.stream()
                .filter(info -> storageType.equals(info.getStorageType()))
                .collect(Collectors.toList());
    }

    /**
     * 根据家庭桶名过滤转换结果
     *
     * <p>从转换结果列表中过滤出指定家庭桶名的记录。
     * 支持按家庭进行数据统计和分析。
     *
     * @param storageInfoList 存储信息列表
     * @param familyBucketName 指定的家庭桶名
     * @return 过滤后的存储信息列表
     */
    public List<FileStorageInfo> filterByFamilyBucket(List<FileStorageInfo> storageInfoList, String familyBucketName) {
        if (!StringUtils.hasText(familyBucketName)) {
            return Collections.emptyList();
        }

        if (storageInfoList == null || storageInfoList.isEmpty()) {
            return Collections.emptyList();
        }

        return storageInfoList.stream()
                .filter(info -> familyBucketName.equals(info.getFamilyBucketName()))
                .collect(Collectors.toList());
    }
}