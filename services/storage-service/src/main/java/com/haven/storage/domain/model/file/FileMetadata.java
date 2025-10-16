package com.haven.storage.domain.model.file;

import com.haven.base.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 文件元数据实体
 */
@Data
@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_family_id", columnList = "family_id"),
    @Index(name = "idx_owner_id", columnList = "owner_id"),
    @Index(name = "idx_file_type", columnList = "file_type"),
    @Index(name = "idx_upload_time", columnList = "upload_time"),
    @Index(name = "idx_family_owner", columnList = "family_id, owner_id"),
    @Index(name = "idx_family_type", columnList = "family_id, file_type")
})
public class FileMetadata extends BaseEntity {

    @Id
    @Column(name = "file_id", length = 64, nullable = false)
    private String fileId;

    // 基础字段
    @Column(name = "id")
    private Long id;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "deleted")
    private Integer deleted;

    @Column(name = "status")
    private Integer status;

    @Column(name = "family_id", length = 50, nullable = false)
    private String familyId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "original_name", length = 255)        // 原始文件名
    private String originalName;

    @Column(name = "relative_path", length = 500)
    private String relativePath;

    @Column(name = "folder_path", length = 255)
    private String folderPath;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "mime_type", length = 200)
    private String mimeType;

    @Column(name = "content_type", length = 200)         // 内容类型
    private String contentType;

    @Column(name = "uploaded_by", length = 50)
    private String uploadedBy;

    @Column(name = "uploader_user_id", length = 50)      // 上传用户ID
    private String uploaderUserId;

    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime;

    @Column(name = "upload_time")    // 上传时间
    private LocalDateTime uploadTime;

    @Column(name = "access_count")
    private int accessCount;

    @ElementCollection
    @CollectionTable(name = "file_tags", joinColumns = @JoinColumn(name = "file_id"))
    @Column(name = "tag", length = 100)
    private List<String> tags;

    @Column(name = "description", length = 1000)    // 可见性的描述
    private String description;

    // 存储相关字段
    @Column(name = "storage_path", length = 500)         // 存储路径
    private String storagePath;

    @Column(name = "storage_type", length = 50)         // 存储类型
    private String storageType;

    // 文件预览信息（可选）
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Column(name = "has_preview")
    private boolean hasPreview;

    // 权限相关字段
    @Column(name = "owner_id", length = 50)               // 文件所有者ID
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_visibility", length = 20)
    private FileVisibility fileVisibility = FileVisibility.PRIVATE;     // 文件可见性级别

    // 权限变更相关字段
    @Column(name = "access_change_reason", length = 500)     // 权限变更原因
    private String accessChangeReason;

    @Column(name = "access_change_operator", length = 50)   // 权限变更操作者
    private String accessChangeOperator;

    @Column(name = "access_change_time") // 权限变更时间
    private LocalDateTime accessChangeTime;

    /**
     * 获取文件ID
     * @return
     */
    public String getFileId() {
        if (fileId == null) {
            fileId = generateFileId();
        }
        return fileId;
    }

    // 便捷方法
    public String getOriginalFileName() {
        return originalName != null ? originalName : fileName;
    }

    /**
     * 检查用户是否可以访问文件
     *
     * @param userId       用户ID
     * @param userFamilyId 用户所属家庭ID
     * @return 是否可访问
     */
    public boolean isAccessibleByUser(String userId, String userFamilyId) {
        // 1. 文件状态检查
        if (getDeleted() != null && getDeleted() == 1) {
            return false;
        }
        // 暂时简化状态检查
        // if (getStatus() != null && getStatus() != 1) {
        //     return false;
        // }

        // 2. 所有者权限（始终可访问）
        if (ownerId != null && ownerId.equals(userId)) {
            return true;
        }

        // 3. 根据可见性级别检查
        if (fileVisibility == null) {
            fileVisibility = FileVisibility.PRIVATE; // 默认私有
        }

        switch (fileVisibility) {
            case PUBLIC:
                return true;  // 所有人可访问
            case FAMILY:
                return userFamilyId != null && userFamilyId.equals(this.familyId);  // 家庭成员可访问
            case PRIVATE:
                return false;  // 仅所有者可访问
            default:
                return false;
        }
    }

    /**
     * 检查用户是否可以对文件执行指定操作
     *
     * @param userId       用户ID
     * @param userFamilyId 用户所属家庭ID
     * @param operation    操作类型
     * @return 是否可执行操作
     */
    public boolean canPerformOperation(String userId, String userFamilyId, FileOperation operation) {
        if (!isAccessibleByUser(userId, userFamilyId)) {
            return false;
        }

        // 所有者拥有所有权限
        if (ownerId != null && ownerId.equals(userId)) {
            return true;
        }

        // 根据可见性级别和操作类型判断
        switch (fileVisibility) {
            case PUBLIC:
                // 公共文件：所有用户都可以查看，但不能修改或删除
                return operation == FileOperation.VIEW;
            case FAMILY:
                // 家庭文件：家庭成员可以查看，但不能修改或删除
                return operation == FileOperation.VIEW;
            case PRIVATE:
                // 私有文件：只有所有者可以访问
                return false;
            default:
                return false;
        }
    }

    /**
     * 检查文件是否适合分享
     *
     * @return 是否适合分享
     */
    public boolean isValidForOperation() {
        return (getDeleted() == null || getDeleted() != 1) &&
                // 暂时简化状态检查
                // (getStatus() == null || getStatus() == 1) &&
                ownerId != null && !ownerId.trim().isEmpty() &&
                familyId != null && !familyId.trim().isEmpty();
    }

    /**
     * 检查是否可以变更可见性级别
     *
     * @param userId   操作用户ID
     * @param newLevel 新的可见性级别
     * @return 是否可以变更
     */
    public boolean canChangeVisibility(String userId, FileVisibility newLevel) {
        // 只有所有者可以变更可见性
        if (ownerId == null || !ownerId.equals(userId)) {
            return false;
        }

        // 可见性不能变更为相同级别
        if (newLevel == this.fileVisibility) {
            return false;
        }

        // 检查可见性变更的合理性
        return validateVisibilityChange(newLevel);
    }

    /**
     * 变更可见性级别
     *
     * @param userId   操作用户ID
     * @param newLevel 新的可见性级别
     * @param reason   变更原因
     */
    public void changeVisibility(String userId, FileVisibility newLevel, String reason) {
        if (!canChangeVisibility(userId, newLevel)) {
            throw new SecurityException("无权限变更文件可见性级别");
        }

        this.fileVisibility = newLevel;
        this.accessChangeOperator = userId;
        this.accessChangeReason = reason;
        this.accessChangeTime = LocalDateTime.now();
        setUpdateTime(LocalDateTime.now());
    }


    /**
     * 获取可见性级别描述
     *
     * @return 可见性级别描述
     */
    public String getVisibilityDescription() {
        if (fileVisibility == null) {
            return "未知";
        }
        return fileVisibility.getDescription();
    }

    /**
     * 验证可见性级别变更的合理性
     *
     * @param newLevel 新的可见性级别
     * @return 是否合理
     */
    private boolean validateVisibilityChange(FileVisibility newLevel) {
        // 可见性变更的基本验证
        return true;
    }

    /**
     * 文件操作类型枚举
     */
    @Getter
    public enum FileOperation {
        VIEW("查看"),
        DOWNLOAD("下载"),
        MODIFY("修改"),
        DELETE("删除");

        private final String description;

        FileOperation(String description) {
            this.description = description;
        }

    }

    /**
     * 变更权限级别
     *
     * @param userId   操作用户ID
     * @param newLevel 新的权限级别
     * @param reason   变更原因
     */
    public void changeAccessLevel(String userId, FileVisibility newLevel, String reason) {
        if (!canChangeAccessLevel(userId, newLevel)) {
            throw new SecurityException("无权限变更文件权限级别");
        }

        this.fileVisibility = newLevel;
        this.accessChangeOperator = userId;
        this.accessChangeReason = reason;
        this.accessChangeTime = LocalDateTime.now();
        setUpdateTime(LocalDateTime.now());
    }

    /**
     * 检查是否可以变更权限级别
     *
     * @param userId   操作用户ID
     * @param newLevel 新的权限级别
     * @return 是否可以变更
     */
    public boolean canChangeAccessLevel(String userId, FileVisibility newLevel) {
        // 只有所有者可以变更权限
        if (ownerId == null || !ownerId.equals(userId)) {
            return false;
        }

        // 权限不能变更为相同级别
        if (newLevel == this.fileVisibility) {
            return false;
        }

        // 检查权限变更的合理性
        return validateAccessLevelChange(newLevel);
    }

    /**
     * 验证权限级别变更的合理性
     *
     * @param newLevel 新的权限级别
     * @return 是否合理
     */
    private boolean validateAccessLevelChange(FileVisibility newLevel) {
        // 权限变更的基本验证
        return true;
    }

    // 基础字段的 getter/setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    // JPA 生命周期回调
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        this.deleted = 0;  // 默认未删除
        this.status = 1;   // 默认正常状态
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    // 生成文件ID的方法
    public String generateFileId() {
        if (fileId == null) {
            fileId = "file_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        }
        return fileId;
    }


}