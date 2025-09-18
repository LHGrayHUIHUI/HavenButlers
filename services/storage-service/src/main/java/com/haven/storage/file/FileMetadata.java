package com.haven.storage.file;

import com.haven.base.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件元数据实体
 * 继承BaseEntity获得通用字段和方法
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileMetadata extends BaseEntity {
    private String fileId;
    private String familyId;
    private String fileName;
    private String originalName;        // 原始文件名
    private String relativePath;
    private String folderPath;
    private long fileSize;
    private String fileType;
    private String mimeType;
    private String contentType;         // 内容类型
    private String uploadedBy;
    private String uploaderUserId;      // 上传用户ID
    private LocalDateTime lastAccessTime;
    private LocalDateTime uploadTime;    // 上传时间
    private int accessCount;
    private List<String> tags;

    // 存储相关字段
    private String storagePath;         // 存储路径
    private String storageType;         // 存储类型

    // 文件预览信息（可选）
    private String thumbnailPath;
    private boolean hasPreview;


}