package com.haven.storage.domain.model.entity;

import com.haven.base.model.entity.BaseEntity;
import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.operation.storage.StorageAdapter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 文件存储数据实体 - 物理存储信息管理
 * <p>
 * ============================================================================
 * 文件描述：文件物理存储数据和配置信息的数据库映射实体
 * ============================================================================
 * <p>
 * 核心职责：
 * 1. 物理存储抽象 - 封装不同存储后端的实现细节，提供统一的存储接口
 * 2. 存储路径管理 - 管理文件在不同存储后端的路径和访问方式
 * 3. 存储配置管理 - 存储各存储后端的配置信息和连接参数
 * 4. 存储健康监控 - 跟踪存储后端的可用性和健康状态
 * 5. 数据隔离支持 - 通过家庭存储桶实现数据隔离和访问控制
 * 6. 存储策略支持 - 支持压缩、加密、备份等存储优化策略
 *
 * @author HavenButler
 * @version 1.0
 * @see FileMetadata 文件元数据实体，通过 fileId 关联
 * @see FamilyStorageStats 家庭存储统计，基于本实体数据统计
 * @see StorageType 存储类型枚举，定义支持的存储后端类型
 * @see StorageAdapter 存储适配器接口
 * @since 2024-01-01
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "file_storage_data", indexes = {
        @Index(name = "idx_storage_id", columnList = "storage_id", unique = true),    // 存储ID唯一索引
        @Index(name = "idx_file_id", columnList = "file_id"),                        // 文件ID索引 - 关联查询
        @Index(name = "idx_storage_type", columnList = "storage_type"),              // 存储类型索引 - 按类型筛选
        @Index(name = "idx_storage_status", columnList = "storage_status"),          // 存储状态索引 - 状态筛选
        @Index(name = "idx_family_bucket", columnList = "family_bucket_name"),       // 家庭桶名索引 - 按家庭筛选
        @Index(name = "idx_create_time", columnList = "create_time")                  // 创建时间索引 - 时间排序
})
@Comment("文件存储数据表 - 存储文件的物理位置、配置信息和健康状态")
public class FileStorageData extends BaseEntity {

    /**
     * 存储唯一标识符
     * <p>
     * 全局唯一的存储标识，用于：
     * - 存储后端资源的唯一标识
     * - 文件路径生成和查找
     * - 存储后端路由和负载均衡
     * - 存储缓存键和分布式锁
     * <p>
     * 命名规则：
     * - 本地存储：storage_local_{yyyyMMdd}_{序号}
     * - MinIO存储：storage_minio_{yyyyMMdd}_{序号}
     * - 云存储：storage_{provider}_{yyyyMMdd}_{序号}
     * <p>
     * 示例：storage_local_20240101_001, storage_minio_20240101_002
     */
    @Column(name = "storage_id", length = 64, nullable = false, unique = true)
    @Comment("存储唯一标识符")
    private String storageId;

    /**
     * 关联的文件元数据ID
     * <p>
     * 与 {@link FileMetadata#fileId} 关联，建立元数据与存储数据的关联关系
     * 一个文件元数据对应一条存储数据记录，实现业务与存储的解耦
     * <p>
     * 用途：
     * - 关联查询文件元数据和存储信息
     * - 数据一致性检查和修复
     * - 文件访问权限验证
     * - 存储生命周期管理
     */
    @Column(name = "file_id", length = 64, nullable = false)
    @Comment("关联文件元数据ID")
    private String fileId;

    /**
     * 存储类型
     * <p>
     * 对应 {@link StorageType} 枚举的code值：
     * - 0: LOCAL - 本地文件系统存储
     * - 1: MINIO - MinIO对象存储
     * - 2: ALIYUN_OSS - 阿里云OSS存储
     * - 3: AWS_S3 - AWS S3存储
     * - 4: TENCENT_COS - 腾讯云COS存储
     * <p>
     * 用于：
     * - 存储策略选择和适配器路由
     * - 存储成本计算和计费
     * - 存储性能监控和优化
     * - 存储后端的故障转移
     */
    @Column(name = "storage_type", nullable = false)
    @Comment("存储类型(0:本地 1:MinIO 2:OSS 3:S3)")
    private Integer storageType;

    /**
     * 家庭存储桶名称
     * <p>
     * 不同存储后端的具体含义：
     * - 本地存储：家庭专用的存储目录名称
     * - MinIO/OSS/S3：对象存储的bucket名称
     * - 用于实现家庭级别的数据隔离和访问控制
     * <p>
     * 命名规则：
     * - 建议格式：family-{familyId}-bucket
     * - 示例：family-001-bucket, family-002-data
     */
    @Column(name = "family_bucket_name", length = 100)
    @Comment("家庭存储桶名称")
    private String familyBucketName;

    /**
     * 文件存储路径
     * <p>
     * 相对于存储根目录的文件路径：
     * - 本地存储：相对路径，如 "family/001/images/2024/01/01/abc123.jpg"
     * - 对象存储：对象key，如 "photos/2024/01/01/def456.jpg"
     * <p>
     * 用途：
     * - 文件定位和路径生成
     * - 存储目录结构管理
     * - 文件访问权限控制
     * - 存储空间统计和清理
     */
    @Column(name = "file_path", length = 500, nullable = false)
    @Comment("文件存储路径")
    private String filePath;

    /**
     * 存储配置信息
     * <p>
     * JSON格式的存储后端配置，包括但不限于：
     * - 访问密钥、端点地址、区域信息
     * - 连接参数、性能配置、超时设置
     * - 备份策略、加密设置、压缩参数
     * - 缓存策略、CDN配置、访问限制
     * <p>
     * 本地存储配置示例：
     * <pre>
     * {
     *   "basePath": "/data/storage",
     *   "createMissingDirs": true,
     *   "maxFileSize": "100MB",
     *   "allowedExtensions": ["jpg", "png", "pdf"]
     * }
     * </pre>
     * <p>
     * MinIO配置示例：
     * <pre>
     * {
     *   "endpoint": "http://minio:9000",
     *   "accessKey": "minioadmin",
     *   "secretKey": "minioadmin",
     *   "region": "us-east-1",
     *   "secure": false,
     *   "bucketRegion": "us-east-1",
     *   "connectionTimeout": 30,
     *   "socketTimeout": 60
     * }
     * </pre>
     */
    @Column(name = "storage_config", columnDefinition = "JSON")
    @Comment("存储配置信息(JSON格式)")
    private String storageConfig;

    /**
     * 完整访问路径
     * <p>
     * 文件的完整访问URL或文件系统路径：
     * - 本地存储：绝对路径，如 "/data/storage/family/001/images/abc123.jpg"
     * - 对象存储：访问URL，如 "http://minio:9000/bucket/photos/abc123.jpg"
     * - 云存储：完整的访问地址，可能包含CDN加速URL
     * <p>
     * 用途：
     * - 文件下载和预览
     * - 外部系统访问集成
     * - CDN缓存和加速
     * - 文件分享和公开访问
     */
    @Column(name = "full_access_path", length = 500)
    @Comment("完整访问路径")
    private String fullAccessPath;

    /**
     * 存储状态
     * <p>
     * 存储资源的当前状态，用于：
     * - 存储可用性监控和管理
     * - 存储故障检测和恢复
     * - 文件访问权限控制
     * - 存储生命周期管理
     * <p>
     * 状态定义：
     * - 0: AVAILABLE - 存储可用，文件可以正常访问
     * - 1: UNAVAILABLE - 存储不可用，文件暂时无法访问
     * - 2: MIGRATING - 存储迁移中，文件位置正在变更
     * - 3: DELETED - 存储已删除，需要清理数据
     * - 4: CORRUPTED - 存储损坏，需要修复或重新上传
     */
    @Column(name = "storage_status", nullable = false)
    @Comment("存储状态(0:可用 1:不可用)")
    private Integer storageStatus;

    /**
     * 文件大小（字节）
     * <p>
     * 实际存储的文件大小，用于：
     * - 存储容量统计和计费管理
     * - 传输带宽估算和优化
     * - 存储空间规划和预警
     * - 文件完整性校验
     * - 与FileMetadata.fileSize保持一致
     */
    @Column(name = "file_size")
    @Comment("文件大小(字节)")
    private Long fileSize;

    /**
     * 文件校验和
     * <p>
     * 用于文件完整性验证：
     * - MD5、SHA-1、SHA-256等哈希值
     * - 防止文件传输和存储过程中的损坏
     * - 支持文件去重和重复检测
     * - 数据一致性检查和修复
     * <p>
     * 推荐算法：
     * - 小文件（<10MB）：MD5
     * - 中等文件（10MB-100MB）：SHA-1
     * - 大文件（>100MB）：SHA-256
     */
    @Column(name = "file_checksum", length = 128)
    @Comment("文件校验和(MD5/SHA)")
    private String fileChecksum;

    /**
     * 压缩标记
     * <p>
     * 标记文件是否进行了压缩存储：
     * - true: 文件已压缩，需要解压后使用
     * - false: 文件原始存储，直接可用
     * <p>
     * 压缩策略：
     * - 图片：JPEG压缩，质量85%
     * - 文档：ZIP压缩，级别6
     * - 视频：保持原格式，不压缩
     * - 音频：MP3压缩，比特率128kbps
     */
    @Column(name = "is_compressed", nullable = false)
    @Comment("是否压缩存储")
    private Boolean isCompressed = false;

    /**
     * 加密标记
     * <p>
     * 标记文件是否进行了加密存储：
     * - true: 文件已加密，需要解密后使用
     * - false: 文件明文存储，直接可用
     * <p>
     * 加密策略：
     * - 算法：AES-256-GCM
     * - 密钥管理：基于家庭的密钥派生
     * - 敏感文件：自动加密存储
     * - 性能考虑：大文件分块加密
     */
    @Column(name = "is_encrypted", nullable = false)
    @Comment("是否加密存储")
    private Boolean isEncrypted = false;

    /**
     * 备份状态
     * <p>
     * 文件备份情况，用于灾难恢复：
     * - 0: NOT_BACKED_UP - 未备份
     * - 1: BACKED_UP - 已备份
     * - 2: BACKUP_IN_PROGRESS - 备份中
     * - 3: BACKUP_FAILED - 备份失败
     * <p>
     * 备份策略：
     * - 重要文件：实时备份
     * - 普通文件：每日备份
     * - 临时文件：不备份
     * - 大文件：异步备份
     */
    @Column(name = "backup_status")
    @Comment("备份状态")
    private Integer backupStatus;

    /**
     * 最后访问时间
     * <p>
     * 文件最后一次被访问的时间戳：
     * - 用于热点文件分析和缓存策略
     * - 支持文件生命周期管理
     * - 冷热数据分层存储决策
     * - 存储优化和清理策略
     * <p>
     * 访问包括：
     * - 文件下载和预览
     * - 文件元数据查询
     * - 文件分享和公开访问
     * - 系统管理和维护操作
     */
    @Column(name = "last_access_time")
    @Comment("最后访问时间")
    private LocalDateTime lastAccessTime;

    /**
     * 最后备份时间
     * <p>
     * 文件最后一次成功备份的时间：
     * - 备份策略执行依据
     * - 数据恢复点选择
     * - 备份合规性检查
     * - 备份SLA监控
     * <p>
     * 备份监控：
     * - 跟踪备份延迟和失败率
     * - 监控备份存储空间使用
     * - 定期验证备份完整性
     * - 测试备份恢复流程
     */
    @Column(name = "last_backup_time")
    @Comment("最后备份时间")
    private LocalDateTime lastBackupTime;

    /**
     * 存储元数据
     * <p>
     * 存储后端返回的元数据信息，包括：
     * - 对象存储的ETag、版本号、Content-Type
     * - 文件系统的inode信息、权限位
     * - 云存储的CDN地址、缓存策略、访问统计
     * - 存储优化建议、性能指标、健康状态
     * <p>
     * MinIO元数据示例：
     * <pre>
     * {
     *   "etag": "d41d8cd98f00b204e9800998ecf8427e",
     *   "versionId": "abc123def456",
     *   "contentType": "image/jpeg",
     *   "lastModified": "2024-01-01T12:00:00Z",
     *   "contentLength": 2097152,
     *   "storageClass": "STANDARD"
     * }
     * </pre>
     */
    @Column(name = "storage_metadata", columnDefinition = "JSON")
    @Comment("存储元数据(JSON格式)")
    private String storageMetadata;

    // ==================== 业务方法 ====================


    /**
     * 设置存储状态为可用
     */
    public void setAvailable() {
        this.storageStatus = 0;
        setUpdateTime(LocalDateTime.now());
    }

    /**
     * 设置存储状态为不可用
     */
    public void setUnavailable() {
        this.storageStatus = 1;
        setUpdateTime(LocalDateTime.now());
    }

    /**
     * 记录文件访问
     */
    public void recordAccess() {
        this.lastAccessTime = LocalDateTime.now();
        setUpdateTime(LocalDateTime.now());
    }

    /**
     * 记录备份操作
     */
    public void recordBackup() {
        this.lastBackupTime = LocalDateTime.now();
        this.backupStatus = 1; // 已备份
        setUpdateTime(LocalDateTime.now());
    }


    // ==================== JPA 生命周期回调 ====================

    @PrePersist
    protected void onCreate() {
        if (this.storageStatus == null) {
            this.storageStatus = 0; // 默认可用状态
        }
        if (this.isCompressed == null) {
            this.isCompressed = false;
        }
        if (this.isEncrypted == null) {
            this.isEncrypted = false;
        }
        if (this.backupStatus == null) {
            this.backupStatus = 0; // 默认未备份
        }
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdateTime(LocalDateTime.now());
    }
}
