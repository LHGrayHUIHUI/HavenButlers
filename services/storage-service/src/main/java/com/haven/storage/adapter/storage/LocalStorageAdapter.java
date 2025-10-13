package com.haven.storage.adapter.storage;


import com.haven.storage.domain.model.file.FileDownloadResult;
import com.haven.storage.domain.model.file.FileMetadata;
import com.haven.storage.domain.model.file.FileUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 本地文件存储适配器
 *
 * 功能特性：
 * - 基于家庭ID的目录隔离
 * - 自动创建目录结构
 * - 文件大小和类型验证
 * - 文件名重复处理
 * - 安全路径验证
 *
 * @author HavenButler
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.file.storage-type", havingValue = "local")
public class LocalStorageAdapter implements StorageAdapter {

    @Value("${storage.file.local.base-path:/data/haven-storage}")
    private String basePath;

    @Value("${storage.file.local.auto-create-dirs:true}")
    private boolean autoCreateDirs;

    @Value("${storage.file.local.max-file-size:104857600}") // 100MB
    private long maxFileSize;

    @Value("${storage.file.local.allowed-extensions:pdf,doc,docx,txt,jpg,jpeg,png,gif,mp4,avi,mp3,wav,zip,rar}")
    private String allowedExtensions;

    private static final String STORAGE_TYPE = "local";

    @Override
    public FileUploadResult uploadFile(String familyId, String folderPath,
                                       MultipartFile file, String uploaderUserId) {
        try {
            // 参数验证
            if (!StringUtils.hasText(familyId) || file == null || file.isEmpty()) {
                return FileUploadResult.failure("参数错误：familyId和file不能为空");
            }

            // 文件大小验证
            if (file.getSize() > maxFileSize) {
                return FileUploadResult.failure("文件大小超过限制：" + (maxFileSize / 1024 / 1024) + "MB");
            }

            // 文件类型验证
            String fileName = file.getOriginalFilename();
            if (!isAllowedFileType(fileName)) {
                return FileUploadResult.failure("不支持的文件类型：" + getFileExtension(fileName));
            }

            // 构建存储路径
            String familyDir = buildFamilyDirectory(familyId);
            String targetDir = buildTargetDirectory(familyDir, folderPath);

            // 创建目录
            Path targetPath = Paths.get(targetDir);
            if (autoCreateDirs && !Files.exists(targetPath)) {
                Files.createDirectories(targetPath);
                log.info("创建存储目录：{}", targetPath);
            }

            // 生成唯一文件名
            String fileId = UUID.randomUUID().toString();
            String fileExtension = getFileExtension(fileName);
            String savedFileName = fileId + "." + fileExtension;
            Path filePath = targetPath.resolve(savedFileName);

            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 创建文件元数据
            FileMetadata metadata = new FileMetadata();
            metadata.setFileId(fileId);
            metadata.setOriginalName(fileName);
            metadata.setFileSize(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setFamilyId(familyId);
            metadata.setFolderPath(folderPath);
            metadata.setStoragePath(filePath.toString());
            metadata.setStorageType(STORAGE_TYPE);
            metadata.setUploaderUserId(uploaderUserId);
            metadata.setUploadTime(LocalDateTime.now());

            log.info("文件上传成功：familyId={}, fileId={}, fileName={}, size={}",
                    familyId, fileId, fileName, file.getSize());

            return FileUploadResult.success(metadata, "tr-" + System.currentTimeMillis());

        } catch (IOException e) {
            log.error("文件上传失败：familyId={}, fileName={}, error={}",
                    familyId, file.getOriginalFilename(), e.getMessage());
            return FileUploadResult.failure("文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public FileDownloadResult downloadFile(String fileId, String familyId) {
        try {
            // 参数验证
            if (!StringUtils.hasText(fileId) || !StringUtils.hasText(familyId)) {
                return FileDownloadResult.failure("参数错误：fileId和familyId不能为空");
            }

            // 查找文件路径
            Path filePath = findFileByIdAndFamily(fileId, familyId);
            if (filePath == null || !Files.exists(filePath)) {
                return FileDownloadResult.failure("文件不存在或无权限访问");
            }

            // 读取文件内容
            byte[] fileContent = Files.readAllBytes(filePath);
            String fileName = filePath.getFileName().toString();

            log.info("文件下载成功：familyId={}, fileId={}, size={}",
                    familyId, fileId, fileContent.length);

            // 创建临时的FileMetadata用于返回
            FileMetadata tempMetadata = new FileMetadata();
            tempMetadata.setOriginalName(fileName);
            tempMetadata.setContentType(getContentType(fileName));

            return FileDownloadResult.success(fileContent, tempMetadata, "tr-" + System.currentTimeMillis());

        } catch (IOException e) {
            log.error("文件下载失败：familyId={}, fileId={}, error={}",
                    familyId, fileId, e.getMessage());
            return FileDownloadResult.failure("文件下载失败：" + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileId, String familyId) {
        try {
            // 查找文件路径
            Path filePath = findFileByIdAndFamily(fileId, familyId);
            if (filePath == null || !Files.exists(filePath)) {
                log.warn("删除文件失败：文件不存在，familyId={}, fileId={}", familyId, fileId);
                return false;
            }

            // 删除文件
            Files.delete(filePath);

            log.info("文件删除成功：familyId={}, fileId={}", familyId, fileId);
            return true;

        } catch (IOException e) {
            log.error("文件删除失败：familyId={}, fileId={}, error={}",
                    familyId, fileId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> listFiles(String familyId, String folderPath) {
        try {
            String familyDir = buildFamilyDirectory(familyId);
            String targetDir = buildTargetDirectory(familyDir, folderPath);
            Path targetPath = Paths.get(targetDir);

            if (!Files.exists(targetPath)) {
                return new ArrayList<>();
            }

            return Files.list(targetPath)
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("获取文件列表失败：familyId={}, folderPath={}, error={}",
                    familyId, folderPath, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            Path basePathObj = Paths.get(basePath);

            // 检查基础路径是否存在且可写
            if (!Files.exists(basePathObj)) {
                if (autoCreateDirs) {
                    Files.createDirectories(basePathObj);
                } else {
                    return false;
                }
            }

            return Files.isDirectory(basePathObj) && Files.isWritable(basePathObj);

        } catch (Exception e) {
            log.error("本地存储健康检查失败：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    @Override
    public String getFileAccessUrl(String fileId, String familyId, int expireMinutes) {
        // 本地存储不支持直接URL访问，返回文件下载路径标识
        return "/api/v1/storage/files/download/" + fileId + "?familyId=" + familyId;
    }

    /**
     * 构建家庭存储目录
     */
    private String buildFamilyDirectory(String familyId) {
        return Paths.get(basePath, "families", familyId).toString();
    }

    /**
     * 构建目标存储目录
     */
    private String buildTargetDirectory(String familyDir, String folderPath) {
        if (!StringUtils.hasText(folderPath) || "/".equals(folderPath)) {
            return familyDir;
        }

        // 清理路径，防止目录穿越攻击
        String cleanPath = folderPath.replaceAll("[.]{2,}", "")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");

        return Paths.get(familyDir, cleanPath).toString();
    }

    /**
     * 根据fileId和familyId查找文件路径
     */
    private Path findFileByIdAndFamily(String fileId, String familyId) {
        try {
            String familyDir = buildFamilyDirectory(familyId);
            Path familyPath = Paths.get(familyDir);

            if (!Files.exists(familyPath)) {
                return null;
            }

            // 递归搜索包含fileId的文件
            return Files.walk(familyPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(fileId + "."))
                    .findFirst()
                    .orElse(null);

        } catch (IOException e) {
            log.error("查找文件失败：familyId={}, fileId={}, error={}", familyId, fileId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }

    /**
     * 检查文件类型是否允许
     */
    private boolean isAllowedFileType(String fileName) {
        if (!StringUtils.hasText(allowedExtensions)) {
            return true; // 如果没有配置限制，则允许所有类型
        }

        String extension = getFileExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            return false;
        }

        List<String> allowed = Arrays.asList(allowedExtensions.split(","));
        return allowed.stream().anyMatch(ext -> ext.trim().equalsIgnoreCase(extension));
    }

    /**
     * 根据文件名获取Content-Type
     */
    private String getContentType(String fileName) {
        String extension = getFileExtension(fileName);
        switch (extension) {
            case "pdf": return "application/pdf";
            case "doc": case "docx": return "application/msword";
            case "txt": return "text/plain";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp4": return "video/mp4";
            case "mp3": return "audio/mpeg";
            case "zip": return "application/zip";
            case "rar": return "application/x-rar-compressed";
            default: return "application/octet-stream";
        }
    }
}