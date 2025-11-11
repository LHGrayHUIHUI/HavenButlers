package com.haven.storage.domain.model.entity;

import com.haven.base.model.entity.BaseEntity;
import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.enums.FileVisibility;
import com.haven.storage.domain.model.enums.UserRole;
import com.haven.storage.permission.PermissionMatrix;
import com.haven.storage.security.UserContext;
import com.haven.storage.security.UserInfo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 文件元数据实体 - 充血模型
 * <p>
 * 数据库表结构：
 * 设计特性：
 * - 充血模型设计：封装文件业务逻辑和权限控制
 * - 用户上下文集成：直接使用全局UserContext进行权限验证
 * - 细粒度权限控制：支持家庭、用户级别的权限管理
 * - 完整的审计功能：记录文件访问和权限变更历史
 * - 多存储后端支持：本地存储、MinIO、云存储等
 * - 标签和分类管理：支持文件标签和自定义分类
 * <p>
 * 权限模型：
 * - 所有者(OWNER)：完全控制权限
 * - 家庭成员(FAMILY_MEMBER)：根据可见性级别访问
 * - 公开用户(PUBLIC_USER)：仅能访问公开文件
 * <p>
 * 使用示例：
 * <pre>
 * // 创建文件元数据
 * FileMetadata file = new FileMetadata();
 * file.setFileId("file_123456");
 * file.setFamilyId("family_001");
 * file.setOwnerId("user_001");
 * file.setOriginalName("photo.jpg");
 * file.setFileSize(1024 * 1024L); // 1MB
 * file.setFileType("image");
 * file.setContentType("image/jpeg");
 * file.save();
 *
 * // 权限检查
 * if (file.isAccessibleByCurrentUser()) {
 *     // 用户有权限访问文件
 *     Set&lt;FileOperation&gt; operations = file.getAvailableOperationsForCurrentUser();
 *     if (operations.contains(FileOperation.DOWNLOAD)) {
 *         // 用户可以下载文件
 *     }
 * }
 *
 * // 变更文件可见性
 * file.changeVisibility(FileVisibility.PUBLIC, "分享到家庭相册");
 *
 * // 记录访问
 * file.recordAccess();
 * </pre>
 *
 * @author HavenButler
 * @version 2.0
 * @since 2024-01-01
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "file_metadata", indexes = {
        @Index(name = "idx_family_id", columnList = "family_id"),                    // 家庭ID索引 - 按家庭查询文件
        @Index(name = "idx_owner_id", columnList = "owner_id"),                      // 所有者索引 - 按用户查询文件
        @Index(name = "idx_file_type", columnList = "file_type"),                    // 文件类型索引 - 按类型筛选
        @Index(name = "idx_upload_time", columnList = "upload_time"),                  // 上传时间索引 - 时间排序
        @Index(name = "idx_family_owner", columnList = "family_id, owner_id"),        // 复合索引 - 家庭内用户文件
        @Index(name = "idx_family_type", columnList = "family_id, file_type")          // 复合索引 - 家庭内类型筛选
})
public class FileMetadata extends BaseEntity {

    // ==================== 基础标识字段 ====================

    /**
     * 文件唯一标识符
     * 格式：{type}_{timestamp}_{random}，如：file_1704067200_abc123
     * 用于文件的唯一标识和路径生成
     */
    @Column(name = "file_id", length = 64, nullable = false, unique = true)
    private String fileId;

    /**
     * 软删除标记
     * 0 = 未删除（正常状态）
     * 1 = 已删除（逻辑删除，不参与查询）
     */
    @Column(name = "deleted")
    private Integer deleted;

    /**
     * 文件状态
     * 0 = 禁用（不可访问）
     * 1 = 启用（正常访问）
     * 2 = 处理中（正在生成缩略图、OCR等）
     * 3 = 处理失败
     */
    @Column(name = "status")
    private Integer status;

    // ==================== 归属和权限字段 ====================

    /**
     * 所属家庭ID
     * 用于数据隔离和权限控制
     * 所有文件必须归属于某个家庭
     */
    @Column(name = "family_id", length = 50, nullable = false)
    private String familyId;

    /**
     * 文件所有者ID
     * 拥有文件的完全控制权限
     * 可以为空，默认使用上传者ID
     */
    @Column(name = "owner_id", length = 50)
    private String ownerId;

    // ==================== 文件基本信息字段 ====================

    /**
     * 存储文件名
     * 实际存储在文件系统中的文件名
     * 可能包含UUID、时间戳等，用于避免文件名冲突
     */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * 原始文件名
     * 用户上传时的原始文件名
     * 用于展示和下载时使用
     */
    @Column(name = "original_name", length = 255)
    private String originalName;

    /**
     * 相对存储路径
     * 相对于存储根目录的路径
     * 格式：family/{familyId}/{category}/{yyyy/MM/dd}/
     */
    @Column(name = "relative_path", length = 500)
    private String relativePath;

    /**
     * 文件夹路径
     * 用户指定的文件夹路径
     * 格式：/photos/2024/01/
     */
    @Column(name = "folder_path", length = 255)
    private String folderPath;

    /**
     * 文件大小（字节）
     * 用于存储容量统计和限制检查
     */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /**
     * 文件类型分类
     * 基于内容的简略分类：image, video, document, audio, other
     * 用于文件分类展示和统计
     */
    @Column(name = "file_type", length = 100)
    private String fileType;

    /**
     * MIME类型
     * 标准的MIME类型，如：image/jpeg, video/mp4
     * 用于浏览器正确识别和处理文件
     */
    @Column(name = "mime_type", length = 200)
    private String mimeType;

    /**
     * 内容类型
     * 可与mimeType相同，或更详细的类型描述
     * 用于业务逻辑处理
     */
    @Column(name = "content_type", length = 200)
    private String contentType;

    // ==================== 上传和访问信息字段 ====================

    /**
     * 上传者姓名
     * 可选的用户友好名称
     */
    @Column(name = "uploaded_by", length = 50)
    private String uploadedBy;

    /**
     * 上传者用户ID
     * 实际执行上传操作的用户ID
     * 用于权限追溯和审计
     */
    @Column(name = "uploader_user_id", length = 50)
    private String uploaderUserId;

    /**
     * 最后访问时间
     * 记录文件的最后一次访问时间
     * 用于活跃度分析和清理策略
     */
    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime;

    /**
     * 上传时间
     * 文件首次上传到系统的时间
     * 用于时间排序和统计分析
     */
    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    /**
     * 访问次数
     * 文件被访问的总次数
     * 用于热门文件分析和使用统计
     */
    @Column(name = "access_count")
    private int accessCount;

      // ==================== 标签和描述字段 ====================

    /**
     * 文件标签列表
     * 用于文件分类、搜索和筛选
     * 存储在独立的file_tags表中，支持多标签
     */
    @ElementCollection
    @CollectionTable(name = "file_tags",
            joinColumns = @JoinColumn(name = "file_id",
                    referencedColumnName = "file_id",
                    foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT)))
    @Column(name = "tag", length = 100)
    private List<String> tags;

    /**
     * 文件描述
     * 用户对文件的详细描述
     * 用于搜索和文件说明，支持富文本内容
     */
    @Column(name = "description", length = 1000)
    private String description;

    // ==================== 存储相关字段 ====================

    /**
     * 存储路径
     * 完整的文件存储路径
     * 格式：{storageType}://{relativePath}{fileName}
     * 用于文件访问和迁移
     */
    @Column(name = "storage_path", length = 500)
    private String storagePath;

    /**
     * 存储类型
     * 存储后端类型：local, minio, oss, s3等
     * 用于多存储后端支持和存储策略选择
     */
    @Column(name = "storage_type", length = 50)
    private String storageType;

    /**
     * 缩略图路径
     * 缩略图的访问路径
     * 用于图片预览和快速展示，提升用户体验
     */
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    /**
     * 是否有预览
     * 标记文件是否生成了预览版本
     * 如：PDF预览图、视频封面图、文档预览等
     */
    @Column(name = "has_preview")
    private boolean hasPreview;

    // ==================== 权限和可见性字段 ====================

    /**
     * 文件所有者ID
     * 拥有文件的完全控制权限
     * 可以为空，默认使用上传者ID
     */
    @Column(name = "owner_id", length = 50)
    private String ownerId;

    /**
     * 文件可见性级别
     * 控制不同用户的访问权限：
     * - PRIVATE: 仅所有者可见
     * - FAMILY: 家庭成员可见
     * - PUBLIC: 公开可见
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_visibility", length = 20)
    private FileVisibility fileVisibility = FileVisibility.PRIVATE;

    /**
     * 访问权限变更原因
     * 记录权限变更的原因说明
     * 用于审计和追溯，如：分享给家庭成员、设为公开等
     */
    @Column(name = "access_change_reason", length = 500)
    private String accessChangeReason;

    /**
     * 访问权限变更操作人
     * 执行权限变更的用户ID
     * 用于权限变更审计和责任追踪
     */
    @Column(name = "access_change_operator", length = 50)
    private String accessChangeOperator;

    /**
     * 访问权限变更时间
     * 记录最近一次权限变更的时间
     * 用于权限变更历史追踪和统计分析
     */
    @Column(name = "access_change_time")
    private LocalDateTime accessChangeTime;

    // ==================== 核心业务方法 ====================

    /**
     * 检查当前用户是否有权限访问文件
     * 使用全局 UserContext 自动获取当前用户信息
     *
     * 权限检查逻辑：
     * 1. 文件必须是活跃状态（未删除且启用）
     * 2. 检查用户与文件的关系（所有者、家庭成员、公开用户）
     * 3. 根据文件可见性级别判断访问权限
     *
     * @return true - 当前用户可以访问文件，false - 无权限访问
     */
    public boolean isAccessibleByCurrentUser() {
        return UserContext.getCurrentUserInfo()
                .map(this::isAccessibleBy)
                .orElse(false);
    }

    /**
     * 检查指定用户是否有权限访问文件
     *
     * 权限判断规则：
     * - 所有者：始终可以访问自己的文件
     * - 家庭成员：可以访问家庭内可见的文件
     * - 公开用户：只能访问公开可见的文件
     *
     * @param userInfo 用户信息（包含userId、familyId等）
     * @return true - 用户可以访问文件，false - 无权限访问
     */
    public boolean isAccessibleBy(UserInfo userInfo) {
        if (!isActiveFile()) {
            return false;
        }

        if (isOwner(userInfo.userId())) {
            return true;
        }

        UserRole userRole = determineUserRole(userInfo);
        return userRole.canAccess(getEffectiveVisibility());
    }

    /**
     * 检查当前用户是否可以执行指定操作
     * 使用全局 UserContext 自动获取当前用户信息
     *
     * 支持的操作类型：
     * - READ: 读取文件内容
     * - DOWNLOAD: 下载文件
     * - UPDATE: 更新文件元数据
     * - DELETE: 删除文件
     * - SHARE: 分享文件
     * - MODIFY_PERMISSIONS: 修改权限
     *
     * @param operation 要执行的操作类型
     * @return true - 用户可以执行该操作，false - 无权限执行
     */
    public boolean canCurrentUserPerform(FileOperation operation) {
        return UserContext.getCurrentUserInfo()
                .map(userInfo -> canPerformOperation(userInfo, operation))
                .orElse(false);
    }

    /**
     * 检查指定用户是否可以执行指定操作
     *
     * 权限判断逻辑：
     * 1. 文件必须是活跃状态
     * 2. 确定用户角色（所有者、家庭成员、公开用户）
     * 3. 根据文件可见性和操作类型查询权限矩阵
     *
     * @param userInfo 用户信息
     * @param operation 要执行的操作类型
     * @return true - 用户可以执行该操作，false - 无权限执行
     */
    public boolean canPerformOperation(UserContext.UserInfo userInfo, FileOperation operation) {
        if (!isActiveFile()) {
            return false;
        }

        UserRole userRole = determineUserRole(userInfo);
        FileVisibility visibility = getEffectiveVisibility();

        return PermissionMatrix.hasPermission(userRole, visibility, operation);
    }

    /**
     * 获取当前用户对该文件的所有可用操作
     * 使用全局 UserContext
     *
     * @return 可执行的操作集合
     */
    public Set<FileOperation> getAvailableOperationsForCurrentUser() {
        return UserContext.getCurrentUserInfo()
                .map(this::getAvailableOperations)
                .orElse(EnumSet.noneOf(FileOperation.class));
    }

    /**
     * 获取指定用户对该文件的所有可用操作
     *
     * @param userInfo 用户信息
     * @return 可执行的操作集合
     */
    public Set<FileOperation> getAvailableOperations(UserInfo userInfo) {
        if (!isActiveFile()) {
            return EnumSet.noneOf(FileOperation.class);
        }

        UserRole userRole = determineUserRole(userInfo);
        FileVisibility visibility = getEffectiveVisibility();

        return PermissionMatrix.getPermissions(userRole, visibility);
    }

    /**
     * 变更文件可见性级别（使用当前用户）
     *
     * 此方法会自动从UserContext获取当前用户ID，并验证权限后执行变更
     * 变更会记录完整的审计信息：操作人、时间、原因
     *
     * 支持的可见性级别：
     * - PRIVATE: 仅文件所有者可见
     * - FAMILY: 家庭成员可见
     * - PUBLIC: 所有用户可见
     *
     * 使用示例：
     * file.changeVisibility(FileVisibility.FAMILY, "分享给家庭成员查看");
     *
     * @param newVisibility 新的可见性级别，不能为空
     * @param reason 变更原因，用于审计追踪，不能为空且不超过500字符
     * @throws SecurityException 当用户不是文件所有者时抛出
     * @throws IllegalArgumentException 当参数无效时抛出（newVisibility为空、reason为空或过长）
     * @throws IllegalStateException 当用户未认证时抛出
     */
    public void changeVisibility(FileVisibility newVisibility, String reason) {
        String currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("用户未认证，无法变更文件可见性");
        }

        changeVisibility(currentUserId, newVisibility, reason);
    }

    /**
     * 变更文件可见性级别（指定用户）
     *
     * 提供指定用户ID的版本，用于系统管理、批量操作等场景
     * 包含完整的权限验证和审计记录
     *
     * @param userId 执行变更操作的用户ID，必须是文件所有者
     * @param newVisibility 新的可见性级别，不能为空且不能与当前级别相同
     * @param reason 变更原因，用于审计追踪，不能为空且不超过500字符
     * @throws SecurityException 当指定用户不是文件所有者时抛出
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public void changeVisibility(String userId, FileVisibility newVisibility, String reason) {
        validateVisibilityChange(userId, newVisibility, reason);

        this.fileVisibility = newVisibility;
        this.accessChangeOperator = userId;
        this.accessChangeReason = reason;
        this.accessChangeTime = LocalDateTime.now();
        setUpdateTime(LocalDateTime.now());
    }

    /**
     * 记录文件访问（自动使用当前用户）
     */
    public void recordAccess() {
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount++;
    }

    /**
     * 标记文件为已删除（使用当前用户）
     *
     * @throws SecurityException 当用户无权限时
     * @throws IllegalStateException 当用户未认证时
     */
    public void markAsDeleted() {
        String currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("用户未认证，无法删除文件");
        }

        markAsDeleted(currentUserId);
    }

    /**
     * 标记文件为已删除（指定用户）
     *
     * @param userId 操作用户ID
     * @throws SecurityException 当用户无权限时
     */
    public void markAsDeleted(String userId) {
        if (!isOwner(userId)) {
            throw new SecurityException("只有所有者可以删除文件");
        }
        this.deleted = 1;
        setUpdateTime(LocalDateTime.now());
    }

    // ==================== 查询方法 ====================

    /**
     * 获取原始文件名
     */
    public String getOriginalFileName() {
        return originalName != null ? originalName : fileName;
    }

    /**
     * 获取可见性描述
     */
    public String getVisibilityDescription() {
        return fileVisibility != null ? fileVisibility.getDescription() : "未知";
    }

    /**
     * 检查文件是否为有效的可操作状态
     *
     * 有效文件必须满足以下条件：
     * - 文件未被删除（活跃状态）
     * - 有明确的所有者
     * - 属于某个家庭
     *
     * @return true - 文件可进行业务操作，false - 文件状态异常
     */
    public boolean isValidForOperation() {
        return isActiveFile()
                && ownerId != null && !ownerId.trim().isEmpty()
                && familyId != null && !familyId.trim().isEmpty();
    }

    /**
     * 检查文件是否为活跃状态（未被删除）
     *
     * 活跃文件指未被软删除的文件，可以正常访问和操作
     *
     * @return true - 文件处于活跃状态，false - 文件已被删除
     */
    public boolean isActiveFile() {
        return deleted == null || deleted != 1;
    }

    /**
     * 检查指定用户是否为文件所有者
     *
     * 文件所有者拥有对文件的完全控制权限，包括：
     * - 修改文件元数据
     * - 变更文件可见性
     * - 删除文件
     * - 转移文件所有权
     *
     * @param userId 要检查的用户ID
     * @return true - 用户是文件所有者，false - 用户不是所有者
     */
    public boolean isOwner(String userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    /**
     * 检查当前登录用户是否为文件所有者
     *
     * 使用全局UserContext自动获取当前用户信息
     * 便捷方法，常用于权限检查前的预判断
     *
     * @return true - 当前用户是文件所有者，false - 当前用户不是所有者或未登录
     */
    public boolean isCurrentUserOwner() {
        String currentUserId = UserContext.getCurrentUserId();
        return currentUserId != null && isOwner(currentUserId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 确定用户角色
     */
    private UserRole determineUserRole(UserInfo userInfo) {
        if (isOwner(userInfo.userId())) {
            return UserRole.OWNER;
        }

        if (isSameFamily(userInfo.familyId())) {
            return UserRole.FAMILY_MEMBER;
        }

        return UserRole.PUBLIC_USER;
    }

    /**
     * 获取有效的可见性级别（带默认值）
     */
    private FileVisibility getEffectiveVisibility() {
        return fileVisibility != null ? fileVisibility : FileVisibility.PRIVATE;
    }

    /**
     * 检查是否为同一家庭
     */
    private boolean isSameFamily(String userFamilyId) {
        return userFamilyId != null && userFamilyId.equals(this.familyId);
    }

    /**
     * 验证可见性变更的合法性
     */
    private void validateVisibilityChange(String userId, FileVisibility newVisibility, String reason) {
        if (!isOwner(userId)) {
            throw new SecurityException("只有文件所有者可以变更可见性级别");
        }

        if (newVisibility == null) {
            throw new IllegalArgumentException("新的可见性级别不能为空");
        }

        if (newVisibility == this.fileVisibility) {
            throw new IllegalArgumentException("新的可见性级别与当前级别相同");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("变更原因不能为空");
        }

        if (reason.length() > 500) {
            throw new IllegalArgumentException("变更原因不能超过500个字符");
        }
    }

    // ==================== JPA 生命周期回调 ====================

    @PrePersist
    protected void onCreate() {
        if (this.deleted == null) {
            this.deleted = 0;
        }
        if (this.status == null) {
            this.status = 1;
        }
        if (this.fileVisibility == null) {
            this.fileVisibility = FileVisibility.PRIVATE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdateTime(LocalDateTime.now());
    }

    // ==================== 使用示例和最佳实践 ====================

    /**
     * FileMetadata 使用示例和最佳实践指南
     *
     * 1. 文件创建和基本操作：
     * <pre>
     * // 创建新文件元数据
     * FileMetadata file = new FileMetadata();
     * file.setFileId("file_1704067200_abc123");
     * file.setFamilyId("family_001");
     * file.setOwnerId("user_001");
     * file.setOriginalName("family_photo.jpg");
     * file.setFileSize(2 * 1024 * 1024L); // 2MB
     * file.setFileType("image");
     * file.setContentType("image/jpeg");
     * file.setTags(Arrays.asList("家庭", "聚会", "2024"));
     * file.setDescription("2024年春节家庭聚会照片");
     *
     * // 设置存储信息
     * file.setStoragePath("local:/family/family_001/images/2024/01/01/file_1704067200_abc123.jpg");
     * file.setStorageType("local");
     * file.setThumbnailPath("/thumbnails/family_001/images/file_1704067200_abc123_thumb.jpg");
     * file.setHasPreview(true);
     *
     * // 保存到数据库
     * fileRepository.save(file);
     * </pre>
     *
     * 2. 权限检查和访问控制：
     * <pre>
     * // 检查当前用户是否可以访问文件
     * if (file.isAccessibleByCurrentUser()) {
     *     // 用户有权限访问文件
     *     log.info("用户 {} 可以访问文件 {}", UserContext.getCurrentUserId(), file.getFileId());
     * }
     *
     * // 检查用户是否可以执行特定操作
     * if (file.canCurrentUserPerform(FileOperation.DOWNLOAD)) {
     *     // 用户可以下载文件
     *     byte[] fileContent = storageService.downloadFile(file.getStoragePath());
     * }
     *
     * // 获取用户可执行的所有操作
     * Set&lt;FileOperation&gt; availableOps = file.getAvailableOperationsForCurrentUser();
     * if (availableOps.contains(FileOperation.SHARE)) {
     *     // 用户可以分享文件
     *     createShareLink(file);
     * }
     * </pre>
     *
     * 3. 文件可见性管理：
     * <pre>
     * // 将文件分享给家庭成员
     * file.changeVisibility(FileVisibility.FAMILY, "分享给家庭成员查看春节照片");
     *
     * // 将文件设为公开（需要谨慎操作）
     * file.changeVisibility(FileVisibility.PUBLIC, "设为公开分享给朋友圈");
     *
     * // 检查权限变更历史
     * log.info("文件权限变更：操作人={}, 原因={}, 时间={}",
     *     file.getAccessChangeOperator(),
     *     file.getAccessChangeReason(),
     *     file.getAccessChangeTime());
     * </pre>
     *
     * 4. 文件访问统计：
     * <pre>
     * // 记录文件访问（在每次文件访问时调用）
     * file.recordAccess();
     * log.info("文件 {} 已被访问 {} 次", file.getFileId(), file.getAccessCount());
     *
     * // 获取文件统计信息
     * String summary = String.format(
     *     "文件：%s, 大小：%.2fMB, 访问次数：%d, 上传时间：%s",
     *     file.getOriginalFileName(),
     *     file.getFileSize() / (1024.0 * 1024.0),
     *     file.getAccessCount(),
     *     file.getUploadTime()
     * );
     * </pre>
     *
     * 5. 文件状态管理：
     * <pre>
     * // 检查文件状态
     * if (file.isValidForOperation()) {
     *     // 文件状态正常，可以进行操作
     * } else {
     *     log.warn("文件状态异常：fileId={}, deleted={}, owner={}, family={}",
     *         file.getFileId(), file.getDeleted(), file.getOwnerId(), file.getFamilyId());
     * }
     *
     * // 软删除文件（推荐使用）
     * file.markAsDeleted(); // 使用当前用户
     * // 或
     * file.markAsDeleted("admin_001"); // 指定用户
     * </pre>
     *
     * 6. 批量操作和查询：
     * <pre>
     * // 查询家庭内的图片文件
     * List&lt;FileMetadata&gt; familyImages = fileRepository.findByFamilyIdAndFileType("family_001", "image");
     *
     * // 查询用户拥有的文件
     * List&lt;FileMetadata&gt; userFiles = fileRepository.findByOwnerId("user_001");
     *
     * // 查询公开文件
     * List&lt;FileMetadata&gt; publicFiles = fileRepository.findByFileVisibility(FileVisibility.PUBLIC);
     * </pre>
     *
     * 7. 错误处理和异常情况：
     * <pre>
     * try {
     *     file.changeVisibility(FileVisibility.PUBLIC, "分享给朋友");
     * } catch (SecurityException e) {
     *     log.error("权限不足：{}", e.getMessage());
     *     // 处理权限不足的情况
     * } catch (IllegalArgumentException e) {
     *     log.error("参数错误：{}", e.getMessage());
     *     // 处理参数错误的情况
     * } catch (IllegalStateException e) {
     *     log.error("状态错误：{}", e.getMessage());
     *     // 处理用户未认证的情况
     * }
     * </pre>
     *
     * 8. 性能优化建议：
     * - 使用索引优化查询：family_id, owner_id, file_type, upload_time
     * - 批量操作时使用事务
     * - 大文件操作考虑异步处理
     * - 定期清理软删除的文件
     * - 合理使用缓存存储热点文件元数据
     *
     * 9. 安全注意事项：
     * - 始终进行权限检查后再执行文件操作
     * - 软删除而非物理删除，保证数据可恢复
     * - 记录所有权限变更和敏感操作
     * - 文件路径和存储信息需要加密存储
     * - 定期审计文件访问日志
     *
     * 10. 监控和统计：
     * - 监控文件上传下载量
     * - 统计各类文件的使用频率
     * - 跟踪存储空间使用情况
     * - 分析用户行为模式
     * - 预警异常访问行为
     */
}