package com.haven.storage.domain.model.file;

import com.haven.base.model.entity.BaseModel;
import com.haven.storage.domain.model.enums.StorageType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 文件存储信息模型类
 * 兼容本地存储、MinIO、云端存储（S3/OSS等）多种场景
 */
@Data
@Accessors(chain = true) // 支持链式调用
public class FileStorageInfo implements BaseModel {


    // 核心必选属性
    /**
     * 存储类型（不可为空）
     */
    private StorageType storageType;

    /**
     * 家庭专用桶名（本地存储可留空，MinIO/云端存储必填）
     */
    private String familyBucketName;

    /**
     * 文件名称（包含扩展名，不可为空）
     */
    private String fileName;

    /**
     * 文件路径（不同存储类型含义不同）：
     * - 本地存储：绝对路径（如 D:/family/files/photo.jpg）
     * - MinIO/云端：对象存储路径（如 family/photos/2025/photo.jpg）
     */
    private String filePath;

    // 辅助属性（增强实用性）
    /**
     * 文件唯一标识（可选，用于快速查询）
     */
    private String fileId;

    /**
     * 文件大小（单位：字节）
     */
    private Long fileSize;

    /**
     * 文件创建时间
     */
    private LocalDateTime createTime;

    /**
     * 文件更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 存储配置参数（灵活扩展不同存储的额外配置）：
     * - MinIO：{endpoint: "http://minio:9000", accessKey: "xxx", secretKey: "xxx"}
     * - 云端存储：{region: "cn-hangzhou", endpoint: "xxx"}
     * - 本地存储：{baseDir: "D:/family/storage"}
     */
    private Map<String, String> storageConfig;

    /**
     * 文件MD5值（用于校验文件完整性）
     */
    private String fileMd5;

    /**
     * 文件状态（0-正常，1-删除，2-过期）
     */
    private Integer fileStatus = 0;

    // 构造方法
    public FileStorageInfo() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 核心属性构造方法（快速创建必要信息）
     *
     * @param storageType      存储类型
     * @param familyBucketName 家庭桶名
     * @param fileName         文件名
     * @param filePath         文件路径
     */
    public FileStorageInfo(StorageType storageType, String familyBucketName, String fileName, String filePath) {
        this();
        this.storageType = storageType;
        this.familyBucketName = familyBucketName;
        this.fileName = fileName;
        this.filePath = filePath;
        // 校验核心属性
        validateCoreParams();
    }

    /**
     * 核心参数校验（确保必要信息不为空）
     */
    private void validateCoreParams() {
        if (Objects.isNull(storageType)) {
            throw new IllegalArgumentException("存储类型不能为空");
        }
        if (Objects.isNull(fileName) || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名称不能为空");
        }
        if (Objects.isNull(filePath) || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        // MinIO/云端存储必须指定桶名
        if (storageType != StorageType.LOCAL && (Objects.isNull(familyBucketName) || familyBucketName.trim().isEmpty())) {
            throw new IllegalArgumentException(storageType.getDesc() + "必须指定家庭专用桶名");
        }
    }

    /**
     * 获取完整的文件访问路径（根据存储类型拼接）
     *
     * @return 完整访问路径
     */
    public String getFullAccessPath() {
        return switch (storageType) {
            case LOCAL ->
                // 本地存储：直接返回文件绝对路径
                    filePath;
            case MINIO ->
                // MinIO：桶名 + 文件路径（如 bucketName/filePath）
                    familyBucketName + "/" + filePath;
            case CLOUD_S3, CLOUD_OSS, CLOUD_COS -> {
                // 云端存储：根据配置拼接完整URL（示例，可根据实际需求调整）
                String endpoint = storageConfig.getOrDefault("endpoint", "");
                yield endpoint + "/" + familyBucketName + "/" + filePath;
            }
            default -> familyBucketName + "/" + filePath;
        };
    }

    /**
     * 重写toString方法，美化输出
     */
    @Override
    public String toString() {
        return "FileStorageInfo{" +
                "storageType=" + storageType.getDesc() +
                ", familyBucketName='" + familyBucketName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fullAccessPath='" + getFullAccessPath() + '\'' +
                ", fileSize=" + fileSize +
                ", createTime=" + createTime +
                ", fileStatus=" + (fileStatus == 0 ? "正常" : fileStatus == 1 ? "删除" : "过期") +
                '}';
    }
}
