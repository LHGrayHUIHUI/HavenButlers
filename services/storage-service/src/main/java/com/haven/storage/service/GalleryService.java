package com.haven.storage.service;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 图片画廊服务接口
 *
 * 定义图片管理、搜索、分类、统计等核心功能
 *
 * @author HavenButler
 */
public interface GalleryService {

    /**
     * 获取缩略图
     */
    byte[] getThumbnail(String fileId, String familyId, ThumbnailSize size);

    /**
     * 获取图片EXIF数据
     */
    ExifMetadata getImageExifData(String fileId, String familyId);

    /**
     * 获取图片元数据
     */
    ImageMetadata getImageMetadata(String fileId, String familyId);

    /**
     * 获取家庭图片画廊
     */
    FamilyGalleryResult getFamilyGallery(FamilyGalleryRequest request);

    /**
     * 获取图片分类
     */
    List<String> getImageCategories(String familyId);

    /**
     * 获取时间线视图
     */
    ImageTimelineResult getTimelineView(ImageTimelineRequest request);

    /**
     * 搜索图片
     */
    ImageSearchResult searchImages(ImageSearchRequest request);

    /**
     * 更新图片信息
     */
    boolean updateImageInfo(String fileId, String familyId, ImageUpdateRequest request);

    /**
     * 删除图片
     */
    boolean deleteImage(String fileId, String familyId, String userId);

    /**
     * 批量处理图片
     */
    BatchProcessResult batchProcessImages(String familyId, BatchProcessRequest request);

    /**
     * 获取图片统计信息
     */
    ImageStats getImageStats(String familyId);

    // ===== 内部类定义 =====

    /**
     * 家庭画廊请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class FamilyGalleryRequest {
        private String familyId;
        private String userId;
        private String category;
        private String sortBy;
        private String sortOrder;
        private Integer pageSize;
        private Integer pageNumber;
    }

    /**
     * 家庭画廊结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class FamilyGalleryResult {
        private List<ImageMetadata> images;
        private Integer totalCount;
        private Integer pageNumber;
        private Integer pageSize;
        private List<String> categories;
        private ImageStats stats;
    }

    /**
     * 时间线请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ImageTimelineRequest {
        private String familyId;
        private String userId;
        private String startDate;
        private String endDate;
        private String groupBy; // day, week, month
    }

    /**
     * 时间线结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ImageTimelineResult {
        private List<TimelineGroup> groups;
        private Integer totalCount;
        private String startDate;
        private String endDate;
    }

    /**
     * 时间线分组
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class TimelineGroup {
        private String date;
        private List<ImageMetadata> images;
        private Integer count;
    }

    /**
     * 图片搜索请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ImageSearchRequest {
        private String familyId;
        private String userId;
        private String keyword;
        private List<String> categories;
        private String startDate;
        private String endDate;
        private String sortBy;
        private Integer pageSize;
        private Integer pageNumber;
    }

    /**
     * 图片搜索结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ImageSearchResult {
        private List<ImageMetadata> images;
        private Integer totalCount;
        private Integer pageNumber;
        private Integer pageSize;
        private List<String> suggestions;
    }

    /**
     * 图片更新请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ImageUpdateRequest {
        private String title;
        private String description;
        private List<String> categories;
        private List<String> tags;
        private Boolean isFavorite;
    }

    /**
     * 批量处理请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class BatchProcessRequest {
        private List<String> fileIds;
        private String operation; // delete, categorize, tag
        private String parameter;
        private List<String> categories;
        private List<String> tags;
    }

    /**
     * 批量处理结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class BatchProcessResult {
        private Integer totalCount;
        private Integer successCount;
        private Integer failureCount;
        private List<String> successFileIds;
        private List<String> failureFileIds;
        private Map<String, String> errors;
    }

    /**
     * 图片统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ImageStats {
        private Integer totalImages;
        private Long totalSize;
        private Map<String, Integer> categoryStats;
        private Map<String, Integer> monthlyStats;
        private String lastUploadDate;
    }
}