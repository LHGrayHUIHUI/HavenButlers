package com.haven.storage.service;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.file.AccessLevel;
import com.haven.storage.domain.model.file.FileMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件元数据服务
 *
 * 提供文件元数据的管理和查询功能
 * 支持文件的CRUD操作和权限验证
 * 目前使用内存存储，实际应用中应替换为数据库
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileMetadataService {

    /**
     * 内存存储文件元数据（实际应用中应替换为数据库）
     */
    private final Map<String, FileMetadata> fileMetadataStore = new ConcurrentHashMap<>();

    /**
     * 保存文件元数据
     *
     * @param fileMetadata 文件元数据
     * @return 保存的文件元数据
     */
    @TraceLog(value = "保存文件元数据", module = "file-metadata", type = "SAVE_METADATA")
    public FileMetadata saveFileMetadata(FileMetadata fileMetadata) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            if (fileMetadata.getFileId() == null || fileMetadata.getFileId().trim().isEmpty()) {
                fileMetadata.setFileId(generateFileId());
            }

            // 设置默认值
            if (fileMetadata.getCreateTime() == null) {
                fileMetadata.setCreateTime(LocalDateTime.now());
            }
            fileMetadata.setUpdateTime(LocalDateTime.now());

            if (fileMetadata.getAccessLevel() == null) {
                fileMetadata.setAccessLevel(AccessLevel.PRIVATE);
            }

            // 暂时简化状态设置
            // if (fileMetadata.getStatus() == null) {
            //     fileMetadata.setStatus(1); // 1=启用
            // }

            if (fileMetadata.getDeleted() == null) {
                fileMetadata.setDeleted(0); // 0=未删除
            }

            // 设置所有者ID
            if (fileMetadata.getOwnerId() == null && fileMetadata.getUploaderUserId() != null) {
                fileMetadata.setOwnerId(fileMetadata.getUploaderUserId());
            }

            fileMetadataStore.put(fileMetadata.getFileId(), fileMetadata);

            log.info("文件元数据保存成功: fileId={}, fileName={}, familyId={}, traceId={}",
                    fileMetadata.getFileId(), fileMetadata.getOriginalFileName(), fileMetadata.getFamilyId(), traceId);

            return fileMetadata;

        } catch (Exception e) {
            log.error("保存文件元数据失败: fileName={}, error={}, traceId={}",
                    fileMetadata.getOriginalFileName(), e.getMessage(), traceId, e);
            throw new RuntimeException("保存文件元数据失败", e);
        }
    }

    /**
     * 根据文件ID获取文件元数据
     *
     * @param fileId 文件ID
     * @return 文件元数据，如果不存在返回null
     */
    @TraceLog(value = "获取文件元数据", module = "file-metadata", type = "GET_METADATA")
    public FileMetadata getFileMetadata(String fileId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            FileMetadata fileMetadata = fileMetadataStore.get(fileId);

            if (fileMetadata != null) {
                log.debug("文件元数据获取成功: fileId={}, fileName={}, traceId={}",
                        fileId, fileMetadata.getOriginalFileName(), traceId);
            } else {
                log.debug("文件元数据不存在: fileId={}, traceId={}", fileId, traceId);
            }

            return fileMetadata;

        } catch (Exception e) {
            log.error("获取文件元数据失败: fileId={}, error={}, traceId={}",
                    fileId, e.getMessage(), traceId, e);
            return null;
        }
    }

    /**
     * 更新文件元数据
     *
     * @param fileMetadata 文件元数据
     * @return 更新后的文件元数据
     */
    @TraceLog(value = "更新文件元数据", module = "file-metadata", type = "UPDATE_METADATA")
    public FileMetadata updateFileMetadata(FileMetadata fileMetadata) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            if (fileMetadata.getFileId() == null || !fileMetadataStore.containsKey(fileMetadata.getFileId())) {
                throw new IllegalArgumentException("文件不存在: " + fileMetadata.getFileId());
            }

            fileMetadata.setUpdateTime(LocalDateTime.now());
            fileMetadataStore.put(fileMetadata.getFileId(), fileMetadata);

            log.info("文件元数据更新成功: fileId={}, fileName={}, traceId={}",
                    fileMetadata.getFileId(), fileMetadata.getOriginalFileName(), traceId);

            return fileMetadata;

        } catch (Exception e) {
            log.error("更新文件元数据失败: fileId={}, error={}, traceId={}",
                    fileMetadata.getFileId(), e.getMessage(), traceId, e);
            throw new RuntimeException("更新文件元数据失败", e);
        }
    }

    /**
     * 删除文件元数据（软删除）
     *
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    @TraceLog(value = "删除文件元数据", module = "file-metadata", type = "DELETE_METADATA")
    public boolean deleteFileMetadata(String fileId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            FileMetadata fileMetadata = fileMetadataStore.get(fileId);
            if (fileMetadata == null) {
                log.warn("删除文件失败 - 文件不存在: fileId={}, traceId={}", fileId, traceId);
                return false;
            }

            // 软删除
            fileMetadata.setDeleted(1);
            fileMetadata.setUpdateTime(LocalDateTime.now());
            fileMetadataStore.put(fileId, fileMetadata);

            log.info("文件元数据删除成功: fileId={}, fileName={}, traceId={}",
                    fileId, fileMetadata.getOriginalFileName(), traceId);

            return true;

        } catch (Exception e) {
            log.error("删除文件元数据失败: fileId={}, error={}, traceId={}",
                    fileId, e.getMessage(), traceId, e);
            return false;
        }
    }

    /**
     * 获取用户的文件列表
     *
     * @param userId 用户ID
     * @param familyId 家庭ID
     * @param includeDeleted 是否包含已删除文件
     * @return 文件列表
     */
    public List<FileMetadata> getUserFiles(String userId, String familyId, boolean includeDeleted) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            return fileMetadataStore.values().stream()
                    .filter(file -> familyId.equals(file.getFamilyId()))
                    .filter(file -> userId.equals(file.getOwnerId()))
                    .filter(file -> includeDeleted || file.getDeleted() != 1)
                    .sorted((f1, f2) -> f2.getCreateTime().compareTo(f1.getCreateTime()))
                    .toList();

        } catch (Exception e) {
            log.error("获取用户文件列表失败: userId={}, familyId={}, error={}, traceId={}",
                    userId, familyId, e.getMessage(), traceId, e);
            return List.of();
        }
    }

    /**
     * 获取家庭文件列表
     *
     * @param familyId 家庭ID
     * @param accessLevel 权限级别过滤
     * @return 文件列表
     */
    public List<FileMetadata> getFamilyFiles(String familyId, AccessLevel accessLevel) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            return fileMetadataStore.values().stream()
                    .filter(file -> familyId.equals(file.getFamilyId()))
                    .filter(file -> file.getDeleted() != 1)
                    // 暂时简化状态检查
                    // .filter(file -> file.getStatus() == 1)
                    .filter(file -> accessLevel == null ||
                            file.getAccessLevel().ordinal() >= accessLevel.ordinal())
                    .sorted((f1, f2) -> f2.getCreateTime().compareTo(f1.getCreateTime()))
                    .toList();

        } catch (Exception e) {
            log.error("获取家庭文件列表失败: familyId={}, accessLevel={}, error={}, traceId={}",
                    familyId, accessLevel, e.getMessage(), traceId, e);
            return List.of();
        }
    }

    /**
     * 根据文件类型获取文件列表
     *
     * @param familyId 家庭ID
     * @param fileType 文件类型（如 "image/", "video/" 等）
     * @return 文件列表
     */
    public List<FileMetadata> getFilesByType(String familyId, String fileType) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            return fileMetadataStore.values().stream()
                    .filter(file -> familyId.equals(file.getFamilyId()))
                    .filter(file -> file.getDeleted() != 1)
                    // 暂时简化状态检查
                    // .filter(file -> file.getStatus() == 1)
                    .filter(file -> file.getFileType() != null && file.getFileType().startsWith(fileType))
                    .sorted((f1, f2) -> f2.getCreateTime().compareTo(f1.getCreateTime()))
                    .toList();

        } catch (Exception e) {
            log.error("根据类型获取文件列表失败: familyId={}, fileType={}, error={}, traceId={}",
                    familyId, fileType, e.getMessage(), traceId, e);
            return List.of();
        }
    }

    /**
     * 搜索文件
     *
     * @param familyId 家庭ID
     * @param keyword 搜索关键词
     * @return 匹配的文件列表
     */
    public List<FileMetadata> searchFiles(String familyId, String keyword) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return List.of();
            }

            String lowercaseKeyword = keyword.toLowerCase().trim();

            return fileMetadataStore.values().stream()
                    .filter(file -> familyId.equals(file.getFamilyId()))
                    .filter(file -> file.getDeleted() != 1)
                    // 暂时简化状态检查
                    // .filter(file -> file.getStatus() == 1)
                    .filter(file -> matchesKeyword(file, lowercaseKeyword))
                    .sorted((f1, f2) -> f2.getCreateTime().compareTo(f1.getCreateTime()))
                    .toList();

        } catch (Exception e) {
            log.error("搜索文件失败: familyId={}, keyword={}, error={}, traceId={}",
                    familyId, keyword, e.getMessage(), traceId, e);
            return List.of();
        }
    }

    /**
     * 获取存储统计信息
     *
     * @param familyId 家庭ID
     * @return 存储统计信息
     */
    public FamilyStorageStats getFamilyStorageStats(String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<FileMetadata> familyFiles = getFamilyFiles(familyId, AccessLevel.PRIVATE);

            long totalFiles = familyFiles.size();
            long totalSize = familyFiles.stream()
                    .mapToLong(FileMetadata::getFileSize)
                    .sum();

            // 按类型统计
            Map<String, Long> typeStats = familyFiles.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            file -> getFileTypeCategory(file.getFileType()),
                            java.util.stream.Collectors.summingLong(FileMetadata::getFileSize)
                    ));

            return FamilyStorageStats.builder()
                    .familyId(familyId)
                    .totalFiles(totalFiles)
                    .totalSize(totalSize)
                    .typeStats(typeStats)
                    .build();

        } catch (Exception e) {
            log.error("获取家庭存储统计失败: familyId={}, error={}, traceId={}",
                    familyId, e.getMessage(), traceId, e);
            return FamilyStorageStats.builder()
                    .familyId(familyId)
                    .totalFiles(0L)
                    .totalSize(0L)
                    .typeStats(Map.of())
                    .build();
        }
    }

    // ===== 私有方法 =====

    /**
     * 生成文件ID
     */
    private String generateFileId() {
        return "file_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 检查文件是否匹配关键词
     */
    private boolean matchesKeyword(FileMetadata file, String keyword) {
        String fileName = file.getOriginalFileName();
        String description = null; // 暂时去掉description字段，因为BaseEntity中已有
        List<String> tags = file.getTags();

        // 检查文件名
        if (fileName != null && fileName.toLowerCase().contains(keyword)) {
            return true;
        }

        // 检查描述 - 暂时跳过，因为BaseEntity中有description字段但访问权限问题
        // if (description != null && description.toLowerCase().contains(keyword)) {
        //     return true;
        // }

        // 检查标签
        if (tags != null) {
            return tags.stream()
                    .anyMatch(tag -> tag != null && tag.toLowerCase().contains(keyword));
        }

        return false;
    }

    /**
     * 获取文件类型分类
     */
    private String getFileTypeCategory(String fileType) {
        if (fileType == null) {
            return "其他";
        }

        String lowerFileType = fileType.toLowerCase();
        if (lowerFileType.startsWith("image/")) {
            return "图片";
        } else if (lowerFileType.startsWith("video/")) {
            return "视频";
        } else if (lowerFileType.startsWith("audio/")) {
            return "音频";
        } else if (lowerFileType.startsWith("text/") || lowerFileType.contains("document")) {
            return "文档";
        } else if (lowerFileType.contains("zip") || lowerFileType.contains("rar") || lowerFileType.contains("tar")) {
            return "压缩包";
        } else {
            return "其他";
        }
    }

    /**
     * 家庭存储统计信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FamilyStorageStats {
        private String familyId;
        private Long totalFiles;
        private Long totalSize;
        private Map<String, Long> typeStats;
    }
}