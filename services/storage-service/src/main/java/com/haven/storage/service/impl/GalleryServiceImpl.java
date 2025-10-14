package com.haven.storage.service.impl;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.gallery.ExifMetadata;
import com.haven.storage.domain.model.gallery.ImageMetadata;
import com.haven.storage.domain.model.gallery.ThumbnailSize;
import com.haven.storage.service.GalleryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 图片画廊服务实现
 *
 * 提供图片管理、搜索、分类、统计等核心功能的实现
 * 目前使用内存存储，生产环境应替换为数据库存储
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryServiceImpl implements GalleryService {

    // 内存存储图片元数据（实际应用中应替换为数据库）
    private final Map<String, ImageMetadata> imageMetadataStore = new ConcurrentHashMap<>();

    // 家庭图片索引：familyId -> Set<fileId>
    private final Map<String, Set<String>> familyImageIndex = new ConcurrentHashMap<>();

    // 分类索引：familyId -> category -> Set<fileId>
    private final Map<String, Map<String, Set<String>>> categoryIndex = new ConcurrentHashMap<>();

    @Override
    @TraceLog(value = "获取缩略图", module = "gallery-service", type = "GET_THUMBNAIL")
    public byte[] getThumbnail(String fileId, String familyId, ThumbnailSize size) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ImageMetadata metadata = imageMetadataStore.get(fileId);
            if (metadata == null) {
                log.warn("图片不存在: fileId={}, familyId={}, traceId={}", fileId, familyId, traceId);
                return null;
            }

            // 检查权限
            if (!familyId.equals(metadata.getFamilyId())) {
                log.warn("无权限访问图片: fileId={}, familyId={}, imageFamilyId={}, traceId={}",
                        fileId, familyId, metadata.getFamilyId(), traceId);
                return null;
            }

            // 从缩略图缓存中获取
            String thumbnailKey = fileId + "_" + size.getName();
            String thumbnailPath = metadata.getThumbnails().get(thumbnailKey);

            // 如果找不到缩略图，返回null（实际应用中可以动态生成）
            if (thumbnailPath == null) {
                log.debug("缩略图不存在: fileId={}, size={}, traceId={}", fileId, size, traceId);
                return null;
            }

            // 这里应该返回缩略图的二进制数据，暂时返回空数组
            log.debug("缩略图获取成功: fileId={}, size={}, traceId={}", fileId, size, traceId);
            return new byte[0];

        } catch (Exception e) {
            log.error("获取缩略图失败: fileId={}, familyId={}, size={}, error={}, traceId={}",
                    fileId, familyId, size, e.getMessage(), traceId);
            return null;
        }
    }

    @Override
    @TraceLog(value = "获取EXIF数据", module = "gallery-service", type = "GET_EXIF")
    public ExifMetadata getImageExifData(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ImageMetadata metadata = imageMetadataStore.get(fileId);
            if (metadata == null) {
                log.warn("图片不存在: fileId={}, familyId={}, traceId={}", fileId, familyId, traceId);
                return null;
            }

            // 检查权限
            if (!familyId.equals(metadata.getFamilyId())) {
                log.warn("无权限访问图片: fileId={}, familyId={}, imageFamilyId={}, traceId={}",
                        fileId, familyId, metadata.getFamilyId(), traceId);
                return null;
            }

            ExifMetadata exifData = metadata.getExifData();
            log.debug("EXIF数据获取成功: fileId={}, hasExif={}, traceId={}",
                    fileId, exifData != null, traceId);
            return exifData;

        } catch (Exception e) {
            log.error("获取EXIF数据失败: fileId={}, familyId={}, error={}, traceId={}",
                    fileId, familyId, e.getMessage(), traceId);
            return null;
        }
    }

    @Override
    @TraceLog(value = "获取图片元数据", module = "gallery-service", type = "GET_METADATA")
    public ImageMetadata getImageMetadata(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ImageMetadata metadata = imageMetadataStore.get(fileId);
            if (metadata == null) {
                log.warn("图片不存在: fileId={}, familyId={}, traceId={}", fileId, familyId, traceId);
                return null;
            }

            // 检查权限
            if (!familyId.equals(metadata.getFamilyId())) {
                log.warn("无权限访问图片: fileId={}, familyId={}, imageFamilyId={}, traceId={}",
                        fileId, familyId, metadata.getFamilyId(), traceId);
                return null;
            }

            // 更新访问记录
            metadata.setLastAccessTime(LocalDateTime.now());
            metadata.setAccessCount(metadata.getAccessCount() + 1);

            log.debug("图片元数据获取成功: fileId={}, accessCount={}, traceId={}",
                    fileId, metadata.getAccessCount(), traceId);
            return metadata;

        } catch (Exception e) {
            log.error("获取图片元数据失败: fileId={}, familyId={}, error={}, traceId={}",
                    fileId, familyId, e.getMessage(), traceId);
            return null;
        }
    }

    @Override
    @TraceLog(value = "获取家庭图片画廊", module = "gallery-service", type = "GET_FAMILY_GALLERY")
    public FamilyGalleryResult getFamilyGallery(FamilyGalleryRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            String familyId = request.getFamilyId();
            int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 0;
            int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

            // 获取家庭所有图片
            Set<String> familyImageIds = familyImageIndex.getOrDefault(familyId, Collections.emptySet());
            List<ImageMetadata> allImages = familyImageIds.stream()
                    .map(imageMetadataStore::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 应用过滤条件
            List<ImageMetadata> filteredImages = allImages;

            if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                String category = request.getCategory().trim();
                filteredImages = allImages.stream()
                        .filter(img -> img.getCategories().contains(category))
                        .collect(Collectors.toList());
            }

            // 排序
            String sortBy = request.getSortBy() != null ? request.getSortBy() : "uploadTime";
            String sortOrder = request.getSortOrder() != null ? request.getSortOrder() : "desc";

            filteredImages = sortImages(filteredImages, sortBy, sortOrder);

            // 分页
            int total = filteredImages.size();
            int fromIndex = pageNumber * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            List<ImageMetadata> pageImages = fromIndex < total ?
                    filteredImages.subList(fromIndex, toIndex) : Collections.emptyList();

            // 构建结果
            FamilyGalleryResult result = FamilyGalleryResult.builder()
                    .images(pageImages)
                    .totalCount(total)
                    .pageNumber(pageNumber)
                    .pageSize(pageSize)
                    .categories(getAllCategories(familyId))
                    .stats(calculateImageStats(familyId))
                    .build();

            log.info("家庭图片画廊获取成功: familyId={}, total={}, page={}, traceId={}",
                    familyId, total, pageNumber, traceId);
            return result;

        } catch (Exception e) {
            log.error("获取家庭图片画廊失败: familyId={}, error={}, traceId={}",
                    request.getFamilyId(), e.getMessage(), traceId);
            return FamilyGalleryResult.builder()
                    .images(Collections.emptyList())
                    .totalCount(0)
                    .pageNumber(request.getPageNumber())
                    .pageSize(request.getPageSize())
                    .categories(Collections.emptyList())
                    .stats(new ImageStats())
                    .build();
        }
    }

    @Override
    @TraceLog(value = "获取图片分类", module = "gallery-service", type = "GET_CATEGORIES")
    public List<String> getImageCategories(String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<String> categories = getAllCategories(familyId);
            log.debug("图片分类获取成功: familyId={}, categories={}, traceId={}",
                    familyId, categories.size(), traceId);
            return categories;

        } catch (Exception e) {
            log.error("获取图片分类失败: familyId={}, error={}, traceId={}", familyId, e.getMessage(), traceId);
            return Collections.emptyList();
        }
    }

    @Override
    @TraceLog(value = "获取时间线视图", module = "gallery-service", type = "GET_TIMELINE")
    public ImageTimelineResult getTimelineView(ImageTimelineRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            String familyId = request.getFamilyId();
            String groupBy = request.getGroupBy() != null ? request.getGroupBy() : "month";

            // 获取家庭所有图片
            Set<String> familyImageIds = familyImageIndex.getOrDefault(familyId, Collections.emptySet());
            List<ImageMetadata> allImages = familyImageIds.stream()
                    .map(imageMetadataStore::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 时间过滤
            if (request.getStartDate() != null || request.getEndDate() != null) {
                allImages = filterImagesByDateRange(allImages, request.getStartDate(), request.getEndDate());
            }

            // 按时间分组
            List<TimelineGroup> groups = groupImagesByTime(allImages, groupBy);

            ImageTimelineResult result = ImageTimelineResult.builder()
                    .groups(groups)
                    .totalCount(allImages.size())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .build();

            log.info("时间线视图获取成功: familyId={}, groupBy={}, groups={}, traceId={}",
                    familyId, groupBy, groups.size(), traceId);
            return result;

        } catch (Exception e) {
            log.error("获取时间线视图失败: familyId={}, error={}, traceId={}",
                    request.getFamilyId(), e.getMessage(), traceId);
            return ImageTimelineResult.builder()
                    .groups(Collections.emptyList())
                    .totalCount(0)
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .build();
        }
    }

    @Override
    @TraceLog(value = "搜索图片", module = "gallery-service", type = "SEARCH_IMAGES")
    public ImageSearchResult searchImages(ImageSearchRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            String familyId = request.getFamilyId();
            String keyword = request.getKeyword().toLowerCase().trim();

            // 获取家庭所有图片
            Set<String> familyImageIds = familyImageIndex.getOrDefault(familyId, Collections.emptySet());
            List<ImageMetadata> allImages = familyImageIds.stream()
                    .map(imageMetadataStore::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 搜索过滤
            List<ImageMetadata> matchedImages = allImages.stream()
                    .filter(img -> matchesKeyword(img, keyword))
                    .collect(Collectors.toList());

            // 分类过滤
            if (request.getCategories() != null && !request.getCategories().isEmpty()) {
                matchedImages = matchedImages.stream()
                        .filter(img -> img.getCategories().stream()
                                .anyMatch(cat -> request.getCategories().contains(cat)))
                        .collect(Collectors.toList());
            }

            // 时间过滤
            if (request.getStartDate() != null || request.getEndDate() != null) {
                matchedImages = filterImagesByDateRange(matchedImages, request.getStartDate(), request.getEndDate());
            }

            // 排序（默认按上传时间倒序）
            String sortBy = request.getSortBy() != null ? request.getSortBy() : "uploadTime";
            matchedImages = sortImages(matchedImages, sortBy, "desc");

            // 分页
            int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 0;
            int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
            int total = matchedImages.size();
            int fromIndex = pageNumber * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            List<ImageMetadata> pageImages = fromIndex < total ?
                    matchedImages.subList(fromIndex, toIndex) : Collections.emptyList();

            ImageSearchResult result = ImageSearchResult.builder()
                    .images(pageImages)
                    .totalCount(total)
                    .pageNumber(pageNumber)
                    .pageSize(pageSize)
                    .suggestions(generateSearchSuggestions(keyword))
                    .build();

            log.info("图片搜索完成: familyId={}, keyword={}, total={}, traceId={}",
                    familyId, keyword, total, traceId);
            return result;

        } catch (Exception e) {
            log.error("图片搜索失败: familyId={}, keyword={}, error={}, traceId={}",
                    request.getFamilyId(), request.getKeyword(), e.getMessage(), traceId);
            return ImageSearchResult.builder()
                    .images(Collections.emptyList())
                    .totalCount(0)
                    .pageNumber(request.getPageNumber())
                    .pageSize(request.getPageSize())
                    .suggestions(Collections.emptyList())
                    .build();
        }
    }

    @Override
    @TraceLog(value = "更新图片信息", module = "gallery-service", type = "UPDATE_IMAGE")
    public boolean updateImageInfo(String fileId, String familyId, ImageUpdateRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ImageMetadata metadata = imageMetadataStore.get(fileId);
            if (metadata == null) {
                log.warn("图片不存在: fileId={}, familyId={}, traceId={}", fileId, familyId, traceId);
                return false;
            }

            // 检查权限
            if (!familyId.equals(metadata.getFamilyId())) {
                log.warn("无权限修改图片: fileId={}, familyId={}, imageFamilyId={}, traceId={}",
                        fileId, familyId, metadata.getFamilyId(), traceId);
                return false;
            }

            // 更新信息
            if (request.getTitle() != null) {
                // 暂时跳过title字段更新，因为ImageMetadata中没有title字段
            }
            if (request.getDescription() != null) {
                metadata.setDescription(request.getDescription());
            }
            if (request.getCategories() != null) {
                metadata.setCategories(new ArrayList<>(request.getCategories()));
                updateCategoryIndex(familyId, fileId,
                        request.getCategories(), metadata.getCategories());
            }
            if (request.getTags() != null) {
                metadata.setTags(new ArrayList<>(request.getTags()));
            }
            if (request.getIsFavorite() != null) {
                metadata.setIsFavorite(request.getIsFavorite());
            }

            log.info("图片信息更新成功: fileId={}, familyId={}, traceId={}", fileId, familyId, traceId);
            return true;

        } catch (Exception e) {
            log.error("更新图片信息失败: fileId={}, familyId={}, error={}, traceId={}",
                    fileId, familyId, e.getMessage(), traceId);
            return false;
        }
    }

    @Override
    @TraceLog(value = "删除图片", module = "gallery-service", type = "DELETE_IMAGE")
    public boolean deleteImage(String fileId, String familyId, String userId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ImageMetadata metadata = imageMetadataStore.get(fileId);
            if (metadata == null) {
                log.warn("图片不存在: fileId={}, familyId={}, traceId={}", fileId, familyId, traceId);
                return false;
            }

            // 检查权限（暂时简化，只检查familyId）
            if (!familyId.equals(metadata.getFamilyId())) {
                log.warn("无权限删除图片: fileId={}, familyId={}, imageFamilyId={}, traceId={}",
                        fileId, familyId, metadata.getFamilyId(), traceId);
                return false;
            }

            // 从索引中移除
            familyImageIndex.computeIfPresent(familyId, (k, v) -> {
                v.remove(fileId);
                return v;
            });

            // 从分类索引中移除
            if (metadata.getCategories() != null) {
                for (String category : metadata.getCategories()) {
                    categoryIndex.computeIfPresent(familyId, (k, v) -> {
                        Map<String, Set<String>> categoryMap = v;
                        categoryMap.computeIfPresent(category, (catKey, catSet) -> {
                            catSet.remove(fileId);
                            return catSet.isEmpty() ? categoryMap.remove(catKey) : catSet;
                        });
                        return categoryMap.isEmpty() ? v : categoryMap;
                    });
                }
            }

            // 软删除
            imageMetadataStore.remove(fileId);

            log.info("图片删除成功: fileId={}, familyId={}, userId={}, traceId={}", fileId, familyId, userId, traceId);
            return true;

        } catch (Exception e) {
            log.error("删除图片失败: fileId={}, familyId={}, userId={}, error={}, traceId={}",
                    fileId, familyId, userId, e.getMessage(), traceId);
            return false;
        }
    }

    @Override
    @TraceLog(value = "批量处理图片", module = "gallery-service", type = "BATCH_PROCESS")
    public BatchProcessResult batchProcessImages(String familyId, BatchProcessRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            int totalCount = request.getFileIds().size();
            int successCount = 0;
            int failureCount = 0;
            List<String> successFileIds = new ArrayList<>();
            List<String> failureFileIds = new ArrayList<>();
            Map<String, String> errors = new HashMap<>();

            for (String fileId : request.getFileIds()) {
                try {
                    boolean success = false;

                    switch (request.getOperation()) {
                        case "delete":
                            success = deleteImage(fileId, familyId, "system"); // 系统操作
                            break;
                        case "categorize":
                            if (request.getCategories() != null) {
                                ImageMetadata metadata = imageMetadataStore.get(fileId);
                                if (metadata != null && familyId.equals(metadata.getFamilyId())) {
                                    List<String> oldCategories = metadata.getCategories();
                                    metadata.setCategories(new ArrayList<>(request.getCategories()));
                                    updateCategoryIndex(familyId, fileId,
                                            request.getCategories(), oldCategories);
                                    success = true;
                                }
                            }
                            break;
                        case "tag":
                            if (request.getTags() != null) {
                                ImageMetadata metadata = imageMetadataStore.get(fileId);
                                if (metadata != null && familyId.equals(metadata.getFamilyId())) {
                                    metadata.setTags(new ArrayList<>(request.getTags()));
                                    success = true;
                                }
                            }
                            break;
                        default:
                            log.warn("不支持的批量操作: operation={}, fileId={}, traceId={}",
                                    request.getOperation(), fileId, traceId);
                            break;
                    }

                    if (success) {
                        successCount++;
                        successFileIds.add(fileId);
                    } else {
                        failureCount++;
                        failureFileIds.add(fileId);
                        errors.put(fileId, "操作失败");
                    }

                } catch (Exception e) {
                    failureCount++;
                    failureFileIds.add(fileId);
                    errors.put(fileId, e.getMessage());
                }
            }

            BatchProcessResult result = BatchProcessResult.builder()
                    .totalCount(totalCount)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .successFileIds(successFileIds)
                    .failureFileIds(failureFileIds)
                    .errors(errors)
                    .build();

            log.info("批量处理完成: familyId={}, operation={}, total={}, success={}, failure={}, traceId={}",
                    familyId, request.getOperation(), totalCount, successCount, failureCount, traceId);
            return result;

        } catch (Exception e) {
            log.error("批量处理失败: familyId={}, operation={}, error={}, traceId={}",
                    familyId, request.getOperation(), e.getMessage(), traceId);
            return BatchProcessResult.builder()
                    .totalCount(0)
                    .successCount(0)
                    .failureCount(0)
                    .successFileIds(Collections.emptyList())
                    .failureFileIds(Collections.emptyList())
                    .errors(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @Override
    public ImageStats getImageStats(String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            Set<String> familyImageIds = familyImageIndex.getOrDefault(familyId, Collections.emptySet());
            List<ImageMetadata> images = familyImageIds.stream()
                    .map(imageMetadataStore::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return calculateImageStats(familyId);

        } catch (Exception e) {
            log.error("获取图片统计失败: familyId={}, error={}, traceId={}", familyId, e.getMessage(), traceId);
            return new ImageStats();
        }
    }

    // ===== 私有方法 =====

    /**
     * 获取所有分类
     */
    private List<String> getAllCategories(String familyId) {
        Map<String, Set<String>> categoryMap = categoryIndex.getOrDefault(familyId, Collections.emptyMap());
        return new ArrayList<>(categoryMap.keySet());
    }

    /**
     * 更新分类索引
     */
    private void updateCategoryIndex(String familyId, String fileId,
                                    List<String> newCategories, List<String> oldCategories) {
        // 移除旧分类
        if (oldCategories != null) {
            for (String category : oldCategories) {
                categoryIndex.computeIfPresent(familyId, (k, v) -> {
                    Map<String, Set<String>> categoryMap = v;
                    categoryMap.computeIfPresent(category, (catKey, catSet) -> {
                        catSet.remove(fileId);
                        return catSet.isEmpty() ? categoryMap.remove(catKey) : catSet;
                    });
                    return categoryMap.isEmpty() ? v : categoryMap;
                });
            }
        }

        // 添加新分类
        if (newCategories != null) {
            for (String category : newCategories) {
                categoryIndex.computeIfAbsent(familyId, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet()).add(fileId);
            }
        }
    }

    /**
     * 按日期范围过滤图片
     */
    private List<ImageMetadata> filterImagesByDateRange(List<ImageMetadata> images, String startDate, String endDate) {
        return images.stream()
                .filter(img -> {
                    LocalDateTime uploadTime = img.getUploadTime();
                    if (uploadTime == null) return false;

                    LocalDate uploadDate = uploadTime.toLocalDate();
                    LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.MIN;
                    LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.MAX;

                    return !uploadDate.isBefore(start) && !uploadDate.isAfter(end);
                })
                .collect(Collectors.toList());
    }

    /**
     * 按时间分组图片
     */
    private List<TimelineGroup> groupImagesByTime(List<ImageMetadata> images, String groupBy) {
        Map<String, List<ImageMetadata>> grouped = images.stream()
                .collect(Collectors.groupingBy(img -> {
                    LocalDateTime uploadTime = img.getUploadTime();
                    if (uploadTime == null) return "未知";

                    switch (groupBy) {
                        case "day":
                            return uploadTime.toLocalDate().toString();
                        case "week":
                            return uploadTime.toLocalDate().with(
                                java.time.temporal.TemporalAdjusters.previousOrSame(
                                    java.time.DayOfWeek.SUNDAY
                                )
                            ).toString();
                        case "month":
                            return YearMonth.from(uploadTime).toString();
                        default:
                            return uploadTime.toLocalDate().toString();
                    }
                }));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> TimelineGroup.builder()
                        .date(entry.getKey())
                        .images(entry.getValue())
                        .count(entry.getValue().size())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 检查图片是否匹配关键词
     */
    private boolean matchesKeyword(ImageMetadata image, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return true;

        String keywordLower = keyword.toLowerCase().trim();

        // 检查文件名
        if (image.getFileName() != null && image.getFileName().toLowerCase().contains(keywordLower)) {
            return true;
        }

        // 检查描述
        if (image.getDescription() != null && image.getDescription().toLowerCase().contains(keywordLower)) {
            return true;
        }

        // 检查分类
        if (image.getCategories() != null) {
            boolean categoryMatch = image.getCategories().stream()
                    .anyMatch(cat -> cat.toLowerCase().contains(keywordLower));
            if (categoryMatch) return true;
        }

        // 检查标签
        if (image.getTags() != null) {
            boolean tagMatch = image.getTags().stream()
                    .anyMatch(tag -> tag.toLowerCase().contains(keywordLower));
            if (tagMatch) return true;
        }

        return false;
    }

    /**
     * 排序图片
     */
    private List<ImageMetadata> sortImages(List<ImageMetadata> images, String sortBy, String sortOrder) {
        Comparator<ImageMetadata> comparator = getComparator(sortBy, sortOrder);
        return images.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * 获取比较器
     */
    private Comparator<ImageMetadata> getComparator(String sortBy, String sortOrder) {
        boolean ascending = "asc".equals(sortOrder);

        Comparator<ImageMetadata> comparator;
        switch (sortBy) {
            case "uploadTime":
                comparator = Comparator.comparing(ImageMetadata::getUploadTime);
                break;
            case "fileName":
                comparator = Comparator.comparing(img -> img.getFileName() != null ? img.getFileName() : "");
                break;
            case "accessCount":
                comparator = Comparator.comparingInt(ImageMetadata::getAccessCount);
                break;
            default:
                comparator = Comparator.comparing(ImageMetadata::getUploadTime);
                break;
        }

        return ascending ? comparator : comparator.reversed();
    }

    /**
     * 生成搜索建议
     */
    private List<String> generateSearchSuggestions(String keyword) {
        // 这里可以实现更智能的建议算法
        return Collections.singletonList(keyword);
    }

    /**
     * 计算图片统计信息
     */
    private ImageStats calculateImageStats(String familyId) {
        Set<String> familyImageIds = familyImageIndex.getOrDefault(familyId, Collections.emptySet());
        List<ImageMetadata> images = familyImageIds.stream()
                .map(imageMetadataStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        ImageStats stats = new ImageStats();
        stats.setTotalImages(images.size());
        stats.setTotalSize(images.stream().mapToLong(ImageMetadata::getFileSize).sum());
        stats.setLastUploadDate(images.stream()
                .filter(img -> img.getUploadTime() != null)
                .map(img -> img.getUploadTime().toString())
                .max(Comparator.naturalOrder())
                .orElse(null));

        // 分类统计
        Map<String, Integer> categoryStats = new HashMap<>();
        Map<String, Integer> monthlyStats = new HashMap<>();

        images.forEach(img -> {
            // 分类统计
            if (img.getCategories() != null) {
                for (String category : img.getCategories()) {
                    categoryStats.merge(category, 1, Integer::sum);
                }
            }

            // 月度统计
            if (img.getUploadTime() != null) {
                String month = YearMonth.from(img.getUploadTime()).toString();
                monthlyStats.merge(month, 1, Integer::sum);
            }
        });

        stats.setCategoryStats(categoryStats);
        stats.setMonthlyStats(monthlyStats);

        return stats;
    }

    /**
     * 缓存图片元数据
     */
    public void cacheImageMetadata(ImageMetadata metadata) {
        imageMetadataStore.put(metadata.getFileId(), metadata);
        familyImageIndex.computeIfAbsent(metadata.getFamilyId(), k -> ConcurrentHashMap.newKeySet()).add(metadata.getFileId());

        // 更新分类索引
        if (metadata.getCategories() != null) {
            updateCategoryIndex(metadata.getFamilyId(), metadata.getFileId(),
                    metadata.getCategories(), Collections.emptyList());
        }
    }
}