package com.haven.storage.file;

import com.haven.base.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
    private String relativePath;
    private String folderPath;
    private long fileSize;
    private String fileType;
    private String mimeType;
    private String uploadedBy;
    private LocalDateTime lastAccessTime;
    private int accessCount;
    private List<String> tags;

    // 文件预览信息（可选）
    private String thumbnailPath;
    private boolean hasPreview;
}