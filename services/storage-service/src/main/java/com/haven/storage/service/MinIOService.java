package com.haven.storage.service;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinIO 文件存储服务
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOService {

    @Autowired(required = false)
    private MinioClient minioClient;

    @Value("${minio.bucket:smarthome}")
    private String bucketName;

    /**
     * 检查MinIO是否可用
     */
    public boolean isMinIOAvailable() {
        return minioClient != null;
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("创建MinIO存储桶: {}", bucketName);
        }
    }

    /**
     * 上传文件到MinIO
     *
     * @param file 文件
     * @param objectName 对象名称
     * @param familyId 家庭ID
     * @return 上传是否成功
     */
    public boolean uploadFile(MultipartFile file, String objectName, String familyId) {
        if (!isMinIOAvailable()) {
            log.warn("MinIO客户端不可用");
            return false;
        }

        try {
            ensureBucketExists();

            // 构建对象路径：familyId/objectName
            String fullObjectName = familyId + "/" + objectName;

            // 上传文件
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullObjectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );

            log.info("文件上传成功: bucket={}, object={}, size={}",
                bucketName, fullObjectName, file.getSize());
            return true;

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从MinIO下载文件
     *
     * @param objectName 对象名称
     * @param familyId 家庭ID
     * @return 文件字节数组，如果失败返回null
     */
    public byte[] downloadFile(String objectName, String familyId) {
        if (!isMinIOAvailable()) {
            log.warn("MinIO客户端不可用");
            return null;
        }

        try {
            String fullObjectName = familyId + "/" + objectName;

            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullObjectName)
                    .build()
            );

            byte[] fileBytes = stream.readAllBytes();
            stream.close();

            log.info("文件下载成功: bucket={}, object={}, size={}",
                bucketName, fullObjectName, fileBytes.length);
            return fileBytes;

        } catch (Exception e) {
            log.error("文件下载失败: bucket={}, object={}/{}, error={}",
                bucketName, familyId, objectName, e.getMessage());
            return null;
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param objectName 对象名称
     * @param familyId 家庭ID
     * @return 文件是否存在
     */
    public boolean fileExists(String objectName, String familyId) {
        if (!isMinIOAvailable()) {
            return false;
        }

        try {
            String fullObjectName = familyId + "/" + objectName;

            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullObjectName)
                    .build()
            );
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     * @param familyId 家庭ID
     * @return 删除是否成功
     */
    public boolean deleteFile(String objectName, String familyId) {
        if (!isMinIOAvailable()) {
            return false;
        }

        try {
            String fullObjectName = familyId + "/" + objectName;

            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullObjectName)
                    .build()
            );

            log.info("文件删除成功: bucket={}, object={}", bucketName, fullObjectName);
            return true;

        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage());
            return false;
        }
    }
}