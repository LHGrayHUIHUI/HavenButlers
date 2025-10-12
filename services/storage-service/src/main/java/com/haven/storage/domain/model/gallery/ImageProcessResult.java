package com.haven.storage.gallery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片处理结果类
 *
 * 封装图片处理的结果信息，包括处理状态、图片元数据、错误信息等
 *
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageProcessResult {

    /**
     * 处理是否成功
     */
    private boolean success;

    /**
     * 图片元数据
     */
    private ImageMetadata imageMetadata;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 处理耗时（毫秒）
     */
    private Long processingTimeMs;

    /**
     * 生成缩略图数量
     */
    private Integer thumbnailCount;

    /**
     * 提取的EXIF标签数量
     */
    private Integer exifTagCount;

    /**
     * 生成的分类数量
     */
    private Integer categoryCount;

    /**
     * 创建成功结果
     */
    public static ImageProcessResult success(ImageMetadata imageMetadata) {
        return ImageProcessResult.builder()
                .success(true)
                .imageMetadata(imageMetadata)
                .thumbnailCount(imageMetadata != null && imageMetadata.getThumbnails() != null ?
                        imageMetadata.getThumbnails().size() : 0)
                .exifTagCount(imageMetadata != null && imageMetadata.getExifData() != null ? 1 : 0)
                .categoryCount(imageMetadata != null && imageMetadata.getCategories() != null ?
                        imageMetadata.getCategories().size() : 0)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ImageProcessResult failure(String errorMessage) {
        return ImageProcessResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 检查结果是否成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取文件ID
     */
    public String getFileId() {
        return imageMetadata != null ? imageMetadata.getFileId() : null;
    }

    /**
     * 获取家庭ID
     */
    public String getFamilyId() {
        return imageMetadata != null ? imageMetadata.getFamilyId() : null;
    }

    /**
     * 获取处理摘要
     */
    public String getProcessingSummary() {
        if (!success) {
            return "处理失败: " + errorMessage;
        }

        StringBuilder summary = new StringBuilder("处理成功");
        if (thumbnailCount != null && thumbnailCount > 0) {
            summary.append(", 生成缩略图").append(thumbnailCount).append("张");
        }
        if (exifTagCount != null && exifTagCount > 0) {
            summary.append(", 提取EXIF信息");
        }
        if (categoryCount != null && categoryCount > 0) {
            summary.append(", 分类标签").append(categoryCount).append("个");
        }
        if (processingTimeMs != null) {
            summary.append(", 耗时").append(processingTimeMs).append("ms");
        }

        return summary.toString();
    }
}