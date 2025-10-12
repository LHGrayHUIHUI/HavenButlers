package com.haven.storage.adapter.storage;

import com.haven.storage.domain.model.file.FileDownloadResult;
import com.haven.storage.domain.model.file.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 存储适配器接口
 *
 * 支持多种存储方式的统一接口：
 * - 本地文件存储
 * - MinIO对象存储
 * - 云存储（阿里云OSS、腾讯云COS、AWS S3）
 *
 * @author HavenButler
 */
public interface StorageAdapter {

    /**
     * 上传文件
     *
     * @param familyId 家庭ID（数据隔离）
     * @param folderPath 文件夹路径
     * @param file 上传的文件
     * @param uploaderUserId 上传者用户ID
     * @return 上传结果
     */
    FileUploadResult uploadFile(String familyId, String folderPath,
                                MultipartFile file, String uploaderUserId);

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
}