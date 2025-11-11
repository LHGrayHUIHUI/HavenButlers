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
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 文件元数据实体 - 文件业务信息管理（充血模型）
 * <p>
 * ============================================================================
 * 文件描述：文件业务元数据和权限控制的数据库映射实体
 * ============================================================================
 * <p>
 * 核心职责：
 * 1. 文件业务元数据管理 - 管理文件的名称、类型、大小、描述等业务信息
 * 2. 权限控制和访问管理 - 实现基于家庭和用户的细粒度权限控制
 * 3. 文件分类和标签管理 - 支持文件类型分类和自定义标签系统
 * 4. 审计和访问统计 - 记录文件访问历史和操作审计日志
 * 5. 文件生命周期管理 - 管理文件从创建到删除的完整生命周期
 * 6. 用户上下文集成 - 与全局用户上下文系统集成，实现透明权限验证
 * <p>
 * 与其他实体的关系：
 * - 与 FileStorageData：通过 storageId 建立一对一关联，管理物理存储信息
 * - 与 FamilyStorageStats：提供统计数据来源，支持家庭存储使用情况分析
 * - 与 UserContext：集成权限验证，实现基于当前用户的访问控制
 * - 与 PermissionMatrix：实现权限矩阵查询和操作权限判断
 * @author HavenButler
 * @version 2.0
 * @since 2024-01-01
 * @see FileStorageData 文件存储数据实体，通过 storageId 关联物理存储信息
 * @see FamilyStorageStats 家庭存储统计实体，基于本实体数据生成统计信息
 * @see UserContext 全局用户上下文，提供透明的权限验证
 * @see PermissionMatrix 权限矩阵，定义角色、可见性和操作的权限关系
 * @see FileOperation 文件操作枚举，定义支持的各种文件操作类型
 * @see FileVisibility 文件可见性枚举，定义文件的访问级别
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
    @Index(name = "idx_family_type", columnList = "family_id, file_type"),          // 复合索引 - 家庭内类型筛选
    @Index(name = "idx_storage_id", columnList = "storage_id")                     // 存储ID索引 - 关联查询
})
@Comment("文件元数据表 - 管理文件业务信息、权限控制和访问统计")
public class FileMetadata extends BaseEntity {

    // ==================== 基础标识字段 ====================

    /**
     * 文件唯一标识符
     * <p>
     * 格式：{type}_{timestamp}_{random}，如：file_1704067200_abc123
     * 用于文件的唯一标识和业务逻辑处理，与FileStorageData.fileId关联
     * <p>
     * 命名规则：
     * - 前缀：固定为 "file"
     * - 时间戳：文件创建时间的Unix时间戳
     * - 随机数：6位随机字符串，确保唯一性
     * <p>
     * 用途：
     * - 文件的全局唯一标识
     * - 业务逻辑中的文件引用
     * - 缓存键和分布式锁
     * - 文件访问URL的组成元素
     */
    @Column(name = "file_id", length = 64, nullable = false, unique = true)
    @Comment("文件唯一标识符")
    private String fileId;

    /**
     * 关联的存储数据ID
     * <p>
     * 关联到 {@link FileStorageData#storageId}，建立业务元数据与物理存储的关联
     * 用于查询文件的物理存储信息，实现业务逻辑与存储的解耦
     * <p>
     * 查询关联的存储信息包括：
     * - 文件物理存储位置和访问路径
     * - 存储后端类型和配置信息
     * - 存储健康状态和可用性
     * - 文件完整性校验信息
     * <p>
     * 注意：
     * - 这个字段是可选的，某些场景下文件可能没有物理存储
     * - 例如：虚拟文件、外部链接文件、URL引用等
     * - 通过 hasPhysicalStorage() 方法检查是否有物理存储
     */
    @Column(name = "storage_id", length = 64)
    @Comment("关联存储数据ID")
    private String storageId;

    /**
     * 软删除标记
     * <p>
     * 用于逻辑删除，保证数据可恢复性和审计完整性：
     * - 0 = 未删除（正常状态，参与所有查询）
     * - 1 = 已删除（逻辑删除，不参与普通查询，管理员可见）
     * <p>
     * 软删除的优势：
     * - 数据可恢复，避免误删造成的损失
     * - 保留审计轨迹，满足合规要求
     * - 支持数据分析和统计
     * - 便于数据迁移和清理策略制定
     */
    @Column(name = "deleted")
    @Comment("软删除标记(0:未删除 1:已删除)")
    private Integer deleted;

    /**
     * 文件状态
     * <p>
     * 文件的当前处理状态，用于异步处理和状态跟踪：
     * - 0 = 禁用（不可访问，临时或永久禁用）
     * - 1 = 启用（正常访问，文件可正常使用）
     * - 2 = 处理中（正在生成缩略图、OCR识别、转码等）
     * - 3 = 处理失败（需要人工干预或重新处理）
     * <p>
     * 状态转换流程：
     * 创建 → 处理中 → 启用（成功）或 处理失败（失败）
     * 启用 ↔ 禁用（管理员操作）
     * 处理失败 → 处理中 → 启用（重试成功）
     */
    @Column(name = "status")
    @Comment("文件状态(0:禁用 1:启用 2:处理中 3:处理失败)")
    private Integer status;

    // ==================== 归属和权限字段 ====================

    /**
     * 所属家庭ID
     * <p>
     * 用于数据隔离和权限控制的基础，所有文件必须归属于某个家庭
     * 与家庭管理系统的家庭ID保持一致，确保多租户数据隔离
     * <p>
     * 数据隔离策略：
     * - 所有数据查询必须包含 family_id 条件
     * - 跨家庭数据访问被严格禁止
     * - 家庭数据可以通过管理员工具进行迁移
     * <p>
     * 权限控制基础：
     * - 家庭成员权限基于此字段判断
     * - 文件可见性权限也依赖此字段
     * - 家庭管理员可以管理所有家庭文件
     */
    @Column(name = "family_id", length = 50, nullable = false)
    @Comment("所属家庭ID")
    private String familyId;

    /**
     * 文件所有者ID
     * <p>
     * 拥有文件的完全控制权限，包括：
     * - 修改文件元数据（名称、描述、标签等）
     * - 变更文件可见性级别
     * - 删除文件（软删除）
     * - 转移文件所有权
     * - 设置文件访问权限
     * <p>
     * 所有者特性：
     * - 可以为空，默认使用上传者ID
     * - 支持所有权转移功能
     * - 所有者权限不可被其他用户撤销
     * - 所有者变更需要完整的审计记录
     */
    @Column(name = "owner_id", length = 50)
    @Comment("文件所有者ID")
    private String ownerId;

    // ==================== 文件基本信息字段 ====================

    /**
     * 原始文件名
     * <p>
     * 用户上传时的原始文件名，用于：
     * - 用户界面展示和识别
     * - 文件下载时的默认文件名
     * - 搜索和文件查找
     * - 用户友好的文件管理
     * <p>
     * 处理规则：
     * - 保持用户原始命名，支持中文和特殊字符
     * - 文件名长度限制为255个字符
     * - 防止文件名注入攻击
     * - 支持文件名搜索和模糊匹配
     */
    @Column(name = "original_name", length = 255)
    @Comment("原始文件名")
    private String originalName;

    /**
     * 文件大小（字节）
     * <p>
     * 用于存储容量统计、限制检查和计费管理：
     * - 家庭存储配额控制和统计
     * - 上传文件大小限制验证
     * - 存储成本计算和预算管理
     * - 文件传输时间估算
     * <p>
     * 与 FileStorageData.fileSize 保持一致：
     * - 上传时同时设置两个字段
     * - 存储变更时同步更新
     * - 定期检查数据一致性
     * - 异常情况的修复和恢复
     */
    @Column(name = "file_size", nullable = false)
    @Comment("文件大小(字节)")
    private long fileSize;

    /**
     * 文件类型分类
     * <p>
     * 基于文件内容和用途的简略分类，用于：
     * - 文件分类展示和筛选
     * - 家庭存储统计分析
     * - 不同类型文件的专门处理
     * - 存储策略和权限控制
     * <p>
     * 支持的分类：
     * - image：图片文件（照片、截图、设计图等）
     * - video：视频文件（电影、录像、动画等）
     * - document：文档文件（PDF、Word、Excel等）
     * - audio：音频文件（音乐、录音、播客等）
     * - other：其他文件（压缩包、可执行文件等）
     */
    @Column(name = "file_type", length = 100)
    @Comment("文件类型分类")
    private String fileType;

    /**
     * MIME类型
     * <p>
     * 标准的MIME类型，用于：
     * - 浏览器正确识别和处理文件
     * - HTTP响应头的Content-Type设置
     * - 文件下载和预览功能
     * - 安全策略和访问控制
     * <p>
     * 常见MIME类型：
     * - image/jpeg, image/png, image/gif
     * - video/mp4, video/avi, video/mkv
     * - application/pdf, text/plain
     * - audio/mpeg, audio/wav
     */
    @Column(name = "mime_type", length = 200)
    @Comment("MIME类型")
    private String mimeType;

    /**
     * 内容类型
     * <p>
     * 可以与mimeType相同，或提供更详细的类型描述，用于：
     * - 业务逻辑处理和路由
     * - 文件处理策略选择
     * - 特殊文件类型的识别
     * - 自定义处理流程的触发
     * <p>
     * 扩展用途：
     * - 标识特殊格式（如RAW照片、CAD图纸等）
     * - 指定处理优先级和策略
     * - 触发专门的处理流程
     * - 支持自定义业务逻辑
     */
    @Column(name = "content_type", length = 200)
    @Comment("内容类型")
    private String contentType;

    // ==================== 上传和访问信息字段 ====================

    /**
     * 上传者姓名
     * <p>
     * 可选的用户友好名称，用于：
     * - 用户界面显示和展示
     * - 文件上传历史的用户识别
     * - 统计分析和报表生成
     * - 审计日志的可读性
     * <p>
     * 特点：
     * - 可选字段，支持为空
     * - 支持中文和特殊字符
     * - 与uploader_user_id配合使用
     * - 可以通过用户服务获取最新信息
     */
    @Column(name = "uploaded_by", length = 50)
    @Comment("上传者姓名")
    private String uploadedBy;

    /**
     * 上传者用户ID
     * <p>
     * 实际执行上传操作的用户ID，用于：
     * - 权限追溯和审计
     * - 上传者权限验证
     * - 用户操作统计
     * - 责任追踪和问题定位
     * <p>
     * 权限关系：
     * - 上传者不一定是文件所有者
     * - 所有者可以在创建后转移所有权
     * - 上传者保留上传操作的审计记录
     * - 支持代理上传和批量操作场景
     */
    @Column(name = "uploader_user_id", length = 50)
    @Comment("上传者用户ID")
    private String uploaderUserId;

    /**
     * 最后访问时间
     * <p>
     * 记录文件的最后一次访问时间，用于：
     * - 文件活跃度分析和统计
     * - 冷数据识别和清理策略
     * - 用户行为分析和推荐
     * - 存储优化和分层管理
     * <p>
     * 访问类型包括：
     * - 文件下载和预览
     * - 元数据查询和查看
     * - 文件分享和公开访问
     * - 管理员操作和维护
     */
    @Column(name = "last_access_time")
    @Comment("最后访问时间")
    private LocalDateTime lastAccessTime;

    /**
     * 上传时间
     * <p>
     * 文件首次上传到系统的时间，用于：
     * - 时间排序和最新文件展示
     * - 存储使用量统计分析
     * - 文件生命周期管理
     * - 合规性和数据保留策略
     * <p>
     * 时间用途：
     * - 按时间范围筛选和查询
     * - 文件版本和历史追踪
     * - 数据备份和恢复策略
     * - 存储成本计算和计费
     */
    @Column(name = "upload_time")
    @Comment("上传时间")
    private LocalDateTime uploadTime;

    /**
     * 访问次数
     * <p>
     * 文件被访问的总次数，用于：
     * - 热门文件分析和推荐
     * - 用户行为模式分析
     * - 文件价值和重要性评估
     * - 缓存策略和性能优化
     * <p>
     * 统计策略：
     * - 每次文件访问都递增计数
     * - 区分不同类型的访问（下载、预览、分享等）
     * - 支持按时间周期统计分析
     * - 防止恶意刷量的保护机制
     */
    @Column(name = "access_count")
    @Comment("访问次数")
    private int accessCount;

    // ==================== 标签和描述字段 ====================

    /**
     * 文件标签列表
     * <p>
     * 用于文件分类、搜索和筛选，支持：
     * - 自定义标签和系统标签
     * - 标签的层级分类管理
     * - 标签的权重和优先级
     * - 标签的自动推荐和生成
     * <p>
     * 标签用途：
     * - 文件分类和浏览
     * - 智能搜索和推荐
     * - 相册和集合管理
     * - 内容分析和理解
     * <p>
     * 存储方式：
     * - 存储在独立的file_tags表中
     * - 支持多对多关系
     * - 支持标签的增删改查
     * - 支持标签的统计分析
     */
    @ElementCollection
    @CollectionTable(name = "file_tags",
            joinColumns = @JoinColumn(name = "file_id",
                    referencedColumnName = "file_id",
                    foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT)))
    @Column(name = "tag", length = 100)
    @Comment("文件标签")
    private List<String> tags;

    /**
     * 文件描述
     * <p>
     * 用户对文件的详细描述，支持：
     * - 富文本内容和格式化
     * - 多语言描述支持
     * - 描述内容的搜索和索引
     * - 描述的版本历史管理
     * <p>
     * 描述用途：
     * - 文件内容说明和背景介绍
     * - 搜索和文件查找
     * - 文件上下文信息记录
     * - 协作和知识管理
     * <p>
     - 支持Markdown格式
     * - 支持图片和链接嵌入
     * - 支持描述模板和快捷方式
     * - 支持描述的自动生成和AI辅助
     */
    @Column(name = "description", length = 1000)
    @Comment("文件描述")
    private String description;

    // ==================== 权限和可见性字段 ====================

    /**
     * 文件可见性级别
     * <p>
     * 控制不同用户的访问权限，实现细粒度的访问控制：
     * - PRIVATE：仅所有者可见，最高隐私保护级别
     * - FAMILY：家庭成员可见，适合家庭内部共享
     * - PUBLIC：所有用户可见，最大公开访问级别
     * <p>
     * 权限控制矩阵：
     * - 所有者：所有权限，不受可见性限制
     * - 家庭成员：根据可见性有不同权限
     * - 公开用户：只能访问公开文件
     * <p>
     * 安全考虑：
     * - 可见性变更需要完整的审计记录
     * - 敏感文件默认设置为PRIVATE
     * - 支持临时可见性和时效控制
     * - 防止权限提升和越权访问
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_visibility", length = 20)
    @Comment("文件可见性级别")
    private FileVisibility fileVisibility = FileVisibility.PRIVATE;

    /**
     * 访问权限变更原因
     * <p>
     * 记录权限变更的原因说明，用于：
     * - 权限变更的审计和追溯
     * - 变更原因的分类和统计
     * - 异常变更的检测和告警
     * - 合规性检查和报告生成
     * <p>
     * 常见变更原因：
     * - "分享给家庭成员查看"
     * - "设置为公开分享"
     * - "保护隐私设置为私有"
     * - "临时公开展示"
     */
    @Column(name = "access_change_reason", length = 500)
    @Comment("访问权限变更原因")
    private String accessChangeReason;

    /**
     * 访问权限变更操作人
     * <p>
     * 执行权限变更的用户ID，用于：
     * - 权限变更的责任追踪
     * - 操作权限的验证
     * - 异常行为的检测
     * - 用户行为模式分析
     * <p>
     * 记录要求：
     * - 必须记录实际操作的用户ID
     * - 支持系统管理员操作记录
     * - 防止操作人信息的伪造
     * - 与操作时间形成完整的审计记录
     */
    @Column(name = "access_change_operator", length = 50)
    @Comment("访问权限变更操作人")
    private String accessChangeOperator;

    /**
     * 访问权限变更时间
     * <p>
     * 记录最近一次权限变更的时间，用于：
     * - 权限变更历史追踪
     * - 权限时效性检查
     * - 审计报告的时间线
     * - 权限变更趋势分析
     * <p>
     * 时间精度：
     * - 记录到秒级别的时间戳
     * - 支持时区转换和显示
     * - 用于权限变更的序列分析
     * - 支持按时间范围查询变更记录
     */
    @Column(name = "access_change_time")
    @Comment("访问权限变更时间")
    private LocalDateTime accessChangeTime;

    // ==================== 核心业务方法 ====================

    /**
     * 检查当前用户是否有权限访问文件
     * <p>
     * 使用全局 UserContext 自动获取当前用户信息，实现透明的权限验证
     * 这是充血模型设计的核心方法，将权限逻辑封装在实体内部
     *
     * 权限检查逻辑：
     * 1. 文件必须是活跃状态（未删除且启用）
     * 2. 检查用户与文件的关系（所有者、家庭成员、公开用户）
     * 3. 根据文件可见性级别判断访问权限
     * 4. 考虑特殊权限和临时授权情况
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
     * - 所有者：始终可以访问自己的文件，不受可见性限制
     * - 家庭成员：可以访问家庭内可见的文件（FAMILY级别）
     * - 公开用户：只能访问公开可见的文件（PUBLIC级别）
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
     * <p>
     * 使用全局 UserContext 自动获取当前用户信息，实现透明的操作权限检查
     * 支持细粒度的操作权限控制，每种操作都有独立的权限判断逻辑
     *
     * 支持的操作类型：
     * - READ: 读取文件内容和元数据
     * - DOWNLOAD: 下载文件到本地
     * - UPDATE: 更新文件元数据
     * - DELETE: 删除文件（软删除）
     * - SHARE: 分享文件给其他用户
     * - MODIFY_PERMISSIONS: 修改文件权限
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
     * 1. 文件必须是活跃状态（未删除且启用）
     * 2. 确定用户角色（所有者、家庭成员、公开用户）
     * 3. 根据文件可见性和操作类型查询权限矩阵
     * 4. 考虑特殊权限和临时授权情况
     *
     * @param userInfo 用户信息
     * @param operation 要执行的操作类型
     * @return true - 用户可以执行该操作，false - 无权限执行
     */
    public boolean canPerformOperation(UserInfo userInfo, FileOperation operation) {
        if (!isActiveFile()) {
            return false;
        }

        UserRole userRole = determineUserRole(userInfo);
        FileVisibility visibility = getEffectiveVisibility();

        return PermissionMatrix.hasPermission(userRole, visibility, operation);
    }

    /**
     * 获取当前用户对该文件的所有可用操作
     * <p>
     * 使用全局 UserContext 自动获取当前用户信息，返回用户可执行的所有操作
     * 便于用户界面的功能展示和权限控制
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
     * file.changeVisibility(FileVisibility.FAMILY, "分享给家庭成员查看春节照片");
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
     * <p>
     * 每次文件被访问时都应该调用此方法，用于访问统计和分析
     * 会自动更新最后访问时间和访问次数
     */
    public void recordAccess() {
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount++;
    }

    /**
     * 标记文件为已删除（使用当前用户）
     *
     * 执行软删除操作，保留数据用于审计和恢复
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
     * <p>
     * 返回用户友好的文件名，如果originalName为空则返回空字符串
     */
    public String getOriginalFileName() {
        return originalName != null ? originalName : "";
    }

    /**
     * 获取可见性描述
     * <p>
     * 返回可见性级别的中文描述，用于用户界面展示
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
     * 检查文件是否有物理存储
     *
     * @return true - 有storageId关联物理存储，false - 无物理存储
     */
    public boolean hasPhysicalStorage() {
        return storageId != null && !storageId.trim().isEmpty();
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
        if (this.accessCount == 0) {
            this.accessCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdateTime(LocalDateTime.now());
    }
}