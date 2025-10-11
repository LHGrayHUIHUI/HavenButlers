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

    // 权限相关字段
    private String ownerId;               // 文件所有者ID
    private AccessLevel accessLevel;      // 访问权限级别

    // 权限变更相关字段
    private String accessChangeReason;     // 权限变更原因
    private String accessChangeOperator;   // 权限变更操作者
    private LocalDateTime accessChangeTime; // 权限变更时间

    // 便捷方法
    public String getOriginalFileName() {
        return originalName != null ? originalName : fileName;
    }

    /**
     * 检查用户是否可以访问文件
     *
     * @param userId 用户ID
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

        // 3. 根据权限级别检查
        if (accessLevel == null) {
            accessLevel = AccessLevel.PRIVATE; // 默认私有
        }

        switch (accessLevel) {
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
     * @param userId 用户ID
     * @param userFamilyId 用户所属家庭ID
     * @param operation 操作类型
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

        // 根据权限级别和操作类型判断
        switch (accessLevel) {
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
     * 检查是否可以变更权限级别
     *
     * @param userId 操作用户ID
     * @param newLevel 新的权限级别
     * @return 是否可以变更
     */
    public boolean canChangeAccessLevel(String userId, AccessLevel newLevel) {
        // 只有所有者可以变更权限
        if (ownerId == null || !ownerId.equals(userId)) {
            return false;
        }

        // 权限不能变更为相同级别
        if (newLevel == this.accessLevel) {
            return false;
        }

        // 检查权限变更的合理性
        return validateAccessLevelChange(newLevel);
    }

    /**
     * 变更权限级别
     *
     * @param userId 操作用户ID
     * @param newLevel 新的权限级别
     * @param reason 变更原因
     */
    public void changeAccessLevel(String userId, AccessLevel newLevel, String reason) {
        if (!canChangeAccessLevel(userId, newLevel)) {
            throw new SecurityException("无权限变更文件权限级别");
        }

        this.accessLevel = newLevel;
        this.accessChangeOperator = userId;
        this.accessChangeReason = reason;
        this.accessChangeTime = LocalDateTime.now();
        setUpdateTime(LocalDateTime.now());
    }

    
    /**
     * 获取权限级别描述
     *
     * @return 权限级别描述
     */
    public String getAccessLevelDescription() {
        if (accessLevel == null) {
            return "未知";
        }
        return accessLevel.getDescription();
    }

    /**
     * 验证权限级别变更的合理性
     *
     * @param newLevel 新的权限级别
     * @return 是否合理
     */
    private boolean validateAccessLevelChange(AccessLevel newLevel) {
        // 权限变更的基本验证
        return true;
    }

    /**
     * 文件操作类型枚举
     */
    public enum FileOperation {
        VIEW("查看"),
        DOWNLOAD("下载"),
        MODIFY("修改"),
        DELETE("删除");

        private final String description;

        FileOperation(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

}