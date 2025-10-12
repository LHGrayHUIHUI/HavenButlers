package com.haven.storage.domain.model.gallery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 图片元数据实体类
 *
 * 用于存储图片的详细信息，包括EXIF数据、缩略图信息、分类标签等
 *
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMetadata {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 家庭ID
     */
    private String familyId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 图片宽度（像素）
     */
    private Integer width;

    /**
     * 图片高度（像素）
     */
    private Integer height;

    /**
     * 图片格式（jpg, png, gif等）
     */
    private String format;

    /**
     * EXIF元数据
     */
    private ExifMetadata exifData;

    /**
     * 缩略图路径映射
     * Key: 缩略图尺寸 (small, medium, large)
     * Value: 缩略图文件路径或URL
     */
    private Map<String, String> thumbnails;

    /**
     * 图片分类标签列表
     */
    private List<String> categories;

    /**
     * 图片标签列表
     */
    private List<String> tags;

    /**
     * 拍摄时间（从EXIF提取或文件创建时间）
     */
    private LocalDateTime captureTime;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 访问次数
     */
    private Integer accessCount;

    /**
     * 是否为公共图片（家庭成员可见）
     */
    private Boolean isPublic;

    /**
     * 图片描述
     */
    private String description;

    /**
     * 拍摄设备信息
     */
    private String deviceInfo;

    /**
     * GPS位置信息
     */
    private String location;

    /**
     * 图片评分（1-5星）
     */
    private Integer rating;

    /**
     * 是否为收藏图片
     */
    private Boolean isFavorite;

    /**
     * 验证图片元数据是否有效
     */
    public boolean isValid() {
        return fileId != null && !fileId.trim().isEmpty() &&
               familyId != null && !familyId.trim().isEmpty() &&
               fileName != null && !fileName.trim().isEmpty() &&
               fileSize != null && fileSize > 0 &&
               width != null && width > 0 &&
               height != null && height > 0;
    }

    /**
     * 检查是否为横屏图片
     */
    public boolean isLandscape() {
        return width != null && height != null && width > height;
    }

    /**
     * 检查是否为竖屏图片
     */
    public boolean isPortrait() {
        return width != null && height != null && height > width;
    }

    /**
     * 获取图片宽高比
     */
    public double getAspectRatio() {
        if (width != null && height != null && height > 0) {
            return (double) width / height;
        }
        return 0.0;
    }

    /**
     * 增加访问次数
     */
    public void incrementAccessCount() {
        this.accessCount = (this.accessCount == null ? 0 : this.accessCount) + 1;
        this.lastAccessTime = LocalDateTime.now();
    }
}