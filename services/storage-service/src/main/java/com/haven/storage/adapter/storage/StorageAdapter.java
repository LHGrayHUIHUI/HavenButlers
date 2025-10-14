package com.haven.storage.adapter.storage;

import com.haven.storage.domain.model.file.FileDownloadResult;
import com.haven.storage.domain.model.file.FileMetadata;
import com.haven.storage.domain.model.file.FileUploadResult;
import com.haven.storage.exception.FileStorageException;
import com.haven.storage.utils.FileTypeDetector;
import com.haven.storage.validator.UnifiedFileValidator;
import org.springframework.web.multipart.MultipartFile;

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
     * 上传文件
     * <p>
     * 统一的文件上传接口，支持各种存储后端。
     * 实现类必须进行完整的参数验证和错误处理。
     *
     * @param fileMetadata 包含所有必要文件信息的元数据对象
     *                      必须包含：familyId, folderPath, fileId, uploaderUserId
     * @param file 待上传的文件对象，不能为空
     * @return 文件上传结果，包含上传后的文件元数据和操作状态
     * @throws IllegalArgumentException 当参数验证失败时抛出
     * @throws StorageException 当存储操作失败时抛出
     */
    FileUploadResult uploadFile(FileMetadata fileMetadata, MultipartFile file);

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID（权限验证）
     * @return 下载结果
     */
    FileDownloadResult downloadFile(String fileId, String familyId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID（权限验证）
     * @return 是否删除成功
     */
    boolean deleteFile(String fileId, String familyId);

    /**
     * 获取文件列表
     *
     * @param familyId 家庭ID
     * @param folderPath 文件夹路径
     * @return 文件列表
     */
    List<String> listFiles(String familyId, String folderPath);

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
    String getStorageType();

    /**
     * 获取文件访问URL
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID
     * @param expireMinutes 过期时间（分钟）
     * @return 访问URL
     */
    String getFileAccessUrl(String fileId, String familyId, int expireMinutes);

    /**
     * 数据校验方法
     * <p>
     * 提供统一的数据校验接口，所有实现类必须使用UnifiedFileValidator进行参数验证。
     * 这样可以确保所有存储适配器的验证逻辑一致，避免重复代码。
     * <p>
     * 🎯 核心要求：
     * - 必须使用UnifiedFileValidator进行统一的参数验证
     * - 必须支持文件大小、文件类型、基础参数的验证
     * 必须返回UnifiedFileValidator.ValidationResult结果
     * - 验证失败时必须记录详细的日志信息
     *
     * @param fileMetadata 包含所有必要文件信息的元数据对象
     * @param file 待上传的文件对象，不能为空
     * @param maxFileSize 最大文件大小限制（字节）
     * @return 校验结果，成功或失败信息
     */
    default UnifiedFileValidator.ValidationResult validateUploadData(FileMetadata fileMetadata, MultipartFile file, long maxFileSize) {
        // 默认实现：使用UnifiedFileValidator进行标准验证
        // 各个实现类可以根据需要重写此方法添加特定的验证逻辑
        // 注意：实际使用时应该通过依赖注入
        UnifiedFileValidator validator = new UnifiedFileValidator(new FileTypeDetector());
        return validator.validateFileUpload(fileMetadata.getFamilyId(), file, maxFileSize);
    }
}