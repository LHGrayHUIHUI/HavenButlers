package com.haven.storage.domain.model.file;

import com.haven.storage.model.enums.FileVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传请求
 *
 * 完整的文件上传请求类，包含字段级别的校验注解：
 * - 基础字段校验：非空、长度、格式验证
 * - 业务字段校验：文件大小、路径格式验证
 * - 安全校验：防止路径遍历攻击
 *
 * 💡 校验策略：
 * - 注解校验：基础格式和长度验证（Controller层）
 * - 业务校验：文件内容、权限验证（Service层）
 * - 存储校验：存储容量、文件类型验证（Adapter层）
 *
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件上传请求")
public class FileUploadRequest {

    @NotBlank(message = "家庭ID不能为空")
    @Size(min = 3, max = 50, message = "家庭ID长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "家庭ID只能包含字母、数字、下划线和短横线")
    @Schema(description = "家庭ID", example = "family_123", required = true)
    private String familyId;

    @Pattern(regexp = "^(/[a-zA-Z0-9_\\-\\s]*)*$", message = "文件夹路径格式不正确，必须以/开头")
    @Size(max = 255, message = "文件夹路径长度不能超过255个字符")
    @Schema(description = "文件夹路径", example = "/photos/2024/", defaultValue = "/")
    private String folderPath;

    @NotNull(message = "上传文件不能为空")
    @Schema(description = "上传的文件", required = true)
    private MultipartFile file;

    @NotBlank(message = "上传用户ID不能为空")
    @Size(min = 3, max = 50, message = "上传用户ID长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "上传用户ID只能包含字母、数字、下划线和短横线")
    @Schema(description = "上传用户ID", example = "user_456", required = true)
    private String uploaderUserId;

    @Schema(description = "文件可见性级别", example = "FAMILY")
    private FileVisibility visibility;

    @Size(max = 500, message = "文件描述长度不能超过500个字符")
    @Schema(description = "文件描述", example = "家庭聚会照片")
    private String description;

    @Size(max = 10, message = "文件标签数量不能超过10个")
    @Schema(description = "文件标签", example = "[\"家庭\", \"聚会\", \"照片\"]")
    private java.util.List<@Size(max = 50, message = "单个标签长度不能超过50个字符") String> tags;

    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "文件所有者ID只能包含字母、数字、下划线和短横线")
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

}