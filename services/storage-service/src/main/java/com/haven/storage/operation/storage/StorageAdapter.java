package com.haven.storage.operation.storage;

import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.domain.model.file.FileDownloadResult;
import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.domain.model.file.FileStorageInfo;
import com.haven.storage.domain.model.file.FileUploadResult;
import okio.BufferedSource;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

/**
 * 存储适配器接口
 * <p>
 * 🎯 核心功能：
 * - 支持多种存储方式的统一接口设计
 * - 本地文件存储、MinIO对象存储、云存储（阿里云OSS、腾讯云COS、AWS S3）
 * - 统一的文件上传、下载、删除、列表管理
 * - 基于familyId的数据隔离和安全控制
 * <p>
 * 💡 设计原则：
 * - 适配器模式：支持不同存储后端的无缝切换
 * - 统一接口：为上层服务提供一致的存储操作接口
 * - 数据隔离：基于家庭ID实现租户级别的数据隔离
 * - 可扩展性：便于添加新的存储类型支持
 * <p>
 * 🔧 实现要求：
 * - 所有实现类必须使用FileUploadValidator进行统一的参数验证
 * - 必须支持并发安全的文件操作
 * - 必须实现适当的错误处理和重试机制
 * - 必须支持大文件的流式处理
 *
 * @author HavenButler
 */
public interface StorageAdapter {


    /**
     * 构建家庭专用桶名
     */
    String buildFamilyBucketName(String familyId);

    /**
     * 构建文件存储路径（不含文件名）
     *
     * @param familyId 家庭ID
     * @param fileType 文件类型（如"image"、"video"，可选）
     * @return 路径（如"family/123/image/202405/"）
     */
    String buildFilePath(String familyId, String fileType);



    /**
     * 上传文件
     * <p>
     * 统一的文件上传接口，支持各种存储后端。
     * 实现类必须进行完整的参数验证和错误处理。
     *
     * @param fileStorageInfo 包含所有必要文件信息的元数据对象
     *                        必须包含：familyId, folderPath, fileId, uploaderUserId
     * @param bufferedSource  待上传的文件对象，不能为空
     * @return 文件上传结果，包含上传后的文件元数据和操作状态
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    boolean uploadFile(FileStorageInfo fileStorageInfo, BufferedSource bufferedSource);

    /**
     * 下载文件
     *
     * @param fileId   文件ID
     * @param familyId 家庭ID（权限验证）
     * @return 下载结果
     */
    BufferedSource downloadFile(String fileId, String familyId);

    /**
     * 删除文件
     *
     * @param fileId   文件ID
     * @param familyId 家庭ID（权限验证）
     * @return 是否删除成功
     */
    boolean deleteFile(String fileId, String familyId);

    /**
     * 检查存储健康状态
     *
     * @return 是否健康
     */
    boolean isHealthy();

    /**
     * 获取存储类型标识
     *
     * @return 存储类型（local, minio, cloud）
     */
    StorageType getStorageType();

    /**
     * 获取文件访问URL
     *
     * @param fileStorageInfo 文件的存储对象
     * @return 访问URL
     */
    URI getFileAccessUrl(FileStorageInfo fileStorageInfo);
}