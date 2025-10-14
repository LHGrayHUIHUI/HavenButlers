package com.haven.storage.domain.model.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传请求
 *
 * 简化的文件上传请求类，只包含必要的请求参数
 * 验证逻辑由UnifiedFileValidator处理
 *
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件上传请求")
public class FileUploadRequest {

    @Schema(description = "家庭ID", example = "family_123")
    private String familyId;

    @Schema(description = "文件夹路径", example = "/photos/2024/", defaultValue = "/")
    private String folderPath;

    @NotNull(message = "上传文件不能为空")
    @Schema(description = "上传的文件", required = true)
    private MultipartFile file;

    @Schema(description = "上传用户ID", example = "user_456")
    private String uploaderUserId;

    @Schema(description = "文件可见性级别", example = "FAMILY")
    private FileVisibility visibility;

    @Schema(description = "文件描述", example = "家庭聚会照片")
    private String description;

    @Schema(description = "文件标签", example = "[\"家庭\", \"聚会\", \"照片\"]")
    private java.util.List<String> tags;

    @Schema(description = "文件所有者ID（如果不设置则使用上传者ID）", example = "user_456")
    private String ownerId;

    @Schema(description = "是否生成缩略图", example = "true", defaultValue = "false")
    private Boolean generateThumbnail;

    @Schema(description = "是否启用OCR识别", example = "false", defaultValue = "false")
    private Boolean enableOCR;

    // ===== 便捷方法 =====

    /**
     * 获取文件所有者ID（优先使用ownerId，否则使用uploaderUserId）
     */
    public String getEffectiveOwnerId() {
        return ownerId != null && !ownerId.trim().isEmpty() ? ownerId : uploaderUserId;
    }

    /**
     * 获取文件大小
     */
    public long getFileSize() {
        return file != null ? file.getSize() : 0;
    }

    /**
     * 获取原始文件名
     */
    public String getOriginalFileName() {
        return file != null ? file.getOriginalFilename() : null;
    }

    /**
     * 获取文件类型
     */
    public String getContentType() {
        return file != null ? file.getContentType() : null;
    }

    /**
     * 获取文件扩展名
     */
    public String getFileExtension() {
        if (file == null || file.getOriginalFilename() == null) {
            return null;
        }
        String fileName = file.getOriginalFilename();
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }

    /**
     * 检查是否为图片文件
     */
    public boolean isImageFile() {
        if (file == null || file.getContentType() == null) {
            return false;
        }
        String contentType = file.getContentType().toLowerCase();
        return contentType.startsWith("image/");
    }

    /**
     * 检查是否为文档文件
     */
    public boolean isDocumentFile() {
        if (file == null || file.getContentType() == null) {
            return false;
        }
        String contentType = file.getContentType().toLowerCase();
        return contentType.startsWith("text/") ||
               contentType.contains("document") ||
               contentType.equals("application/pdf") ||
               contentType.contains("office") ||
               contentType.contains("word") ||
               contentType.contains("excel") ||
               contentType.contains("powerpoint");
    }
}