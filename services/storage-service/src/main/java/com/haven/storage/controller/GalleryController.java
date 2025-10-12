package com.haven.storage.controller;

import com.haven.base.annotation.TraceLog;
import com.haven.base.common.response.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.haven.storage.gallery.ImageProcessingService;

/**
 * 图片画廊控制器
 *
 * 提供图片处理、缩略图、EXIF信息、分类展示等API接口
 * 支持家庭图片的智能管理和浏览
 *
 * @author HavenButler
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/storage/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final  ImageProcessingService imageProcessingService;
    private final ExifExtractionService exifExtractionService;
    private final ImageClassificationService classificationService;
    private final GalleryService galleryService;

    // ===== 图片处理相关接口 =====

    /**
     * 获取图片缩略图
     */
    @GetMapping("/images/{fileId}/thumbnail/{size}")
    @TraceLog(value = "获取图片缩略图", module = "gallery-api", type = "GET_THUMBNAIL")
    public ResponseEntity<byte[]> getImageThumbnail(
            @PathVariable String fileId,
            @PathVariable String size,
            @RequestParam String familyId) {

        try {
            // 验证尺寸参数
            if (!ThumbnailSize.isValidSize(size)) {
                return ResponseEntity.badRequest().build();
            }

            ThumbnailSize thumbnailSize = ThumbnailSize.fromName(size);

            // 获取缩略图
            byte[] thumbnailBytes = galleryService.getThumbnail(fileId, familyId, thumbnailSize);

            if (thumbnailBytes != null) {
                // 设置响应头
                return ResponseEntity.ok()
                        .header("Content-Type", "image/jpeg")
                        .header("Cache-Control", "public, max-age=86400") // 缓存1天
                        .body(thumbnailBytes);
            } else {
                // 如果缩略图不存在，返回404
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("获取缩略图失败: fileId={}, familyId={}, size={}, error={}",
                    fileId, familyId, size, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取图片EXIF信息
     */
    @GetMapping("/images/{fileId}/exif")
    @TraceLog(value = "获取EXIF信息", module = "gallery-api", type = "GET_EXIF")
    public ResponseWrapper<ExifMetadata> getImageExif(
            @PathVariable String fileId,
            @RequestParam String familyId) {

        try {
            ExifMetadata exifData = galleryService.getImageExifData(fileId, familyId);

            if (exifData != null) {
                return ResponseWrapper.success(exifData);
            } else {
                return ResponseWrapper.error(40401, "图片EXIF信息不存在", null);
            }

        } catch (Exception e) {
            log.error("获取EXIF信息失败: fileId={}, familyId={}, error={}",
                    fileId, familyId, e.getMessage(), e);
            return ResponseWrapper.error(50001, "获取EXIF信息失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取图片元数据
     */
    @GetMapping("/images/{fileId}/metadata")
    @TraceLog(value = "获取图片元数据", module = "gallery-api", type = "GET_METADATA")
    public ResponseWrapper<ImageMetadata> getImageMetadata(
            @PathVariable String fileId,
            @RequestParam String familyId) {

        try {
            ImageMetadata metadata = galleryService.getImageMetadata(fileId, familyId);

            if (metadata != null) {
                // 增加访问次数
                metadata.incrementAccessCount();
                return ResponseWrapper.success(metadata);
            } else {
                return ResponseWrapper.error(40402, "图片元数据不存在", null);
            }

        } catch (Exception e) {
            log.error("获取图片元数据失败: fileId={}, familyId={}, error={}",
                    fileId, familyId, e.getMessage(), e);
            return ResponseWrapper.error(50002, "获取图片元数据失败: " + e.getMessage(), null);
        }
    }

    // ===== 画廊展示相关接口 =====

    /**
     * 获取家庭图片画廊
     */
    @GetMapping("/family/{familyId}")
    @TraceLog(value = "获取家庭图片画廊", module = "gallery-api", type = "FAMILY_GALLERY")
    public ResponseWrapper<GalleryService.FamilyGalleryResult> getFamilyGallery(
            @PathVariable String familyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        try {
            GalleryService.FamilyGalleryRequest request = GalleryService.FamilyGalleryRequest.builder()
                    .familyId(familyId)
                    .pageNumber(page)
                    .pageSize(size)
                    .category(category)
                    .sortBy(sortBy)
                    .sortOrder(sortOrder)
                    .build();

            GalleryService.FamilyGalleryResult result = galleryService.getFamilyGallery(request);
            return ResponseWrapper.success(result);

        } catch (Exception e) {
            log.error("获取家庭图片画廊失败: familyId={}, error={}", familyId, e.getMessage(), e);
            return ResponseWrapper.error(50003, "获取家庭图片画廊失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取图片分类
     */
    @GetMapping("/family/{familyId}/categories")
    @TraceLog(value = "获取图片分类", module = "gallery-api", type = "GET_CATEGORIES")
    public ResponseWrapper<List<String>> getImageCategories(@PathVariable String familyId) {
        try {
            List<String> categories = galleryService.getImageCategories(familyId);
            return ResponseWrapper.success(categories);

        } catch (Exception e) {
            log.error("获取图片分类失败: familyId={}, error={}", familyId, e.getMessage(), e);
            return ResponseWrapper.error(50004, "获取图片分类失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取时间线视图
     */
    @GetMapping("/family/{familyId}/timeline")
    @TraceLog(value = "获取时间线视图", module = "gallery-api", type = "GET_TIMELINE")
    public ResponseWrapper<GalleryService.ImageTimelineResult> getTimelineView(
            @PathVariable String familyId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "month") String groupBy) {

        try {
            GalleryService.ImageTimelineRequest request = GalleryService.ImageTimelineRequest.builder()
                    .familyId(familyId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .groupBy(groupBy)
                    .build();

            GalleryService.ImageTimelineResult result = galleryService.getTimelineView(request);
            return ResponseWrapper.success(result);

        } catch (Exception e) {
            log.error("获取时间线视图失败: familyId={}, error={}", familyId, e.getMessage(), e);
            return ResponseWrapper.error(50005, "获取时间线视图失败: " + e.getMessage(), null);
        }
    }

    // ===== 图片搜索接口 =====

    /**
     * 搜索图片
     */
    @GetMapping("/family/{familyId}/search")
    @TraceLog(value = "搜索图片", module = "gallery-api", type = "SEARCH_IMAGES")
    public ResponseWrapper<GalleryService.ImageSearchResult> searchImages(
            @PathVariable String familyId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            GalleryService.ImageSearchRequest request = GalleryService.ImageSearchRequest.builder()
                    .familyId(familyId)
                    .keyword(keyword)
                    .pageNumber(page)
                    .pageSize(size)
                    .categories(category != null ? List.of(category) : null)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            GalleryService.ImageSearchResult result = galleryService.searchImages(request);
            return ResponseWrapper.success(result);

        } catch (Exception e) {
            log.error("搜索图片失败: familyId={}, keyword={}, error={}", familyId, keyword, e.getMessage(), e);
            return ResponseWrapper.error(50006, "搜索图片失败: " + e.getMessage(), null);
        }
    }

    // ===== 图片管理接口 =====

    /**
     * 更新图片信息
     */
    @PutMapping("/images/{fileId}")
    @TraceLog(value = "更新图片信息", module = "gallery-api", type = "UPDATE_IMAGE")
    public ResponseWrapper<Boolean> updateImageInfo(
            @PathVariable String fileId,
            @RequestParam String familyId,
            @RequestBody GalleryService.ImageUpdateRequest request) {

        try {
            boolean success = galleryService.updateImageInfo(fileId, familyId, request);

            if (success) {
                return ResponseWrapper.success(true);
            } else {
                return ResponseWrapper.error(40003, "更新图片信息失败", null);
            }

        } catch (Exception e) {
            log.error("更新图片信息失败: fileId={}, familyId={}, error={}",
                    fileId, familyId, e.getMessage(), e);
            return ResponseWrapper.error(50007, "更新图片信息失败: " + e.getMessage(), null);
        }
    }

    /**
     * 删除图片
     */
    @DeleteMapping("/images/{fileId}")
    @TraceLog(value = "删除图片", module = "gallery-api", type = "DELETE_IMAGE")
    public ResponseWrapper<Boolean> deleteImage(
            @PathVariable String fileId,
            @RequestParam String familyId,
            @RequestParam String userId) {

        try {
            boolean success = galleryService.deleteImage(fileId, familyId, userId);

            if (success) {
                return ResponseWrapper.success(true);
            } else {
                return ResponseWrapper.error(40004, "删除图片失败", null);
            }

        } catch (Exception e) {
            log.error("删除图片失败: fileId={}, familyId={}, userId={}, error={}",
                    fileId, familyId, userId, e.getMessage(), e);
            return ResponseWrapper.error(50008, "删除图片失败: " + e.getMessage(), null);
        }
    }

    /**
     * 批量处理图片
     */
    @PostMapping("/batch-process")
    @TraceLog(value = "批量处理图片", module = "gallery-api", type = "BATCH_PROCESS")
    public ResponseWrapper<GalleryService.BatchProcessResult> batchProcessImages(
            @RequestParam String familyId,
            @RequestBody GalleryService.BatchProcessRequest request) {

        try {
            GalleryService.BatchProcessResult result = galleryService.batchProcessImages(familyId, request);
            return ResponseWrapper.success(result);

        } catch (Exception e) {
            log.error("批量处理图片失败: familyId={}, error={}", familyId, e.getMessage(), e);
            return ResponseWrapper.error(50009, "批量处理图片失败: " + e.getMessage(), null);
        }
    }

    // ===== 统计接口 =====

    /**
     * 获取图片统计信息
     */
    @GetMapping("/family/{familyId}/stats")
    @TraceLog(value = "获取图片统计", module = "gallery-api", type = "GET_STATS")
    public ResponseWrapper<GalleryService.ImageStats> getGalleryStats(@PathVariable String familyId) {
        try {
            GalleryService.ImageStats stats = galleryService.getImageStats(familyId);
            return ResponseWrapper.success(stats);

        } catch (Exception e) {
            log.error("获取图片统计失败: familyId={}, error={}", familyId, e.getMessage(), e);
            return ResponseWrapper.error(50010, "获取图片统计失败: " + e.getMessage(), null);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "gallery-service",
                "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(health);
    }
}