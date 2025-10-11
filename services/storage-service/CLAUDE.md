# file-storage-service CLAUDE.md

## 模块概述
file-storage-service是HavenButler平台的智能文件存储核心服务，专注于文件物理存储、图片处理、分享管理和基础组件集成。作为家庭文件管理和知识库系统的存储基础，必须保证高性能、高可用和数据安全。

## 开发指导原则

### 1. 核心设计原则
- **单一职责**：专注于文件存储和基础处理，不包含复杂业务逻辑
- **适配器模式**：支持多种存储后端，提供统一的文件操作接口
- **性能优先**：流式传输、异步处理、缓存优化
- **安全第一**：严格的权限控制、数据隔离和分享安全
- **组件集成**：充分利用base-model和common的通用能力

### 2. 基础组件集成

#### 2.1 Base-Model集成
```java
/**
 * 继承Base-Model的统一日志和审计能力
 */
@Slf4j
@Component
public class FileStorageService extends BaseService {

    @LogOperation(operationType = "FILE_UPLOAD", description = "文件上传")
    @Traced(operationName = "file-upload")
    public UploadResult uploadFile(UploadRequest request) {
        // 1. 验证请求参数
        validateUploadRequest(request);

        // 2. 记录操作审计
        AuditLog auditLog = AuditLog.builder()
            .operation("FILE_UPLOAD")
            .familyId(request.getFamilyId())
            .userId(request.getUserId())
            .resource(request.getOriginalFileName())
            .build();

        // 3. 执行文件上传
        try {
            UploadResult result = storageAdapter.upload(request);

            // 4. 记录成功日志
            log.info("文件上传成功: fileId={}, familyId={}, userId={}",
                result.getFileId(), request.getFamilyId(), request.getUserId());

            // 5. 异步处理图片
            if (isImageFile(request)) {
                asyncImageProcessor.processUploadedImage(result);
            }

            return result;

        } catch (Exception e) {
            // 6. 记录错误和审计
            log.error("文件上传失败: familyId={}, userId={}, error={}",
                request.getFamilyId(), request.getUserId(), e.getMessage(), e);
            auditLogService.logFailure(auditLog, e);
            throw new FileStorageException("文件上传失败", e);
        }
    }
}
```

#### 2.2 Common组件使用
```java
/**
 * 使用Common组件的通用工具类
 */
@Component
public class FileUtils {

    @Autowired
    private IdGenerator idGenerator;  // 通用ID生成器

    @Autowired
    private JsonUtils jsonUtils;      // JSON工具类

    @Autowired
    private DateUtils dateUtils;      // 日期工具类

    @Autowired
    private EncryptionUtils encryptionUtils;  // 加密工具类

    /**
     * 生成唯一文件ID
     */
    public String generateFileId() {
        return "file_" + idGenerator.generateSnowflakeId();
    }

    /**
     * 构建家庭存储路径
     */
    public String buildFamilyPath(String familyId, String category) {
        String datePrefix = dateUtils.formatLocalDate(LocalDate.now(), "yyyy/MM/dd");
        return String.format("family/%s/%s/%s", familyId, category, datePrefix);
    }

    /**
     * 生成分享链接令牌
     */
    public String generateShareToken(String fileId, String userId, Duration expireTime) {
        ShareToken token = ShareToken.builder()
            .fileId(fileId)
            .userId(userId)
            .createTime(Instant.now())
            .expireTime(Instant.now().plus(expireTime))
            .build();

        return encryptionUtils.encrypt(jsonUtils.toJson(token));
    }
}
```

### 3. 图片画廊功能实现

#### 3.1 图片处理引擎
```java
/**
 * 图片处理引擎 - 缩略图生成、EXIF提取、元数据管理
 */
@Component
public class ImageProcessingEngine {

    @Autowired
    private ThumbnailatorProcessor thumbnailatorProcessor;

    @Autowired
    private ExifExtractor exifExtractor;

    @Autowired
    private ImageClassifier imageClassifier;

    /**
     * 处理上传的图片
     */
    @Async("imageProcessingExecutor")
    public CompletableFuture<ImageProcessResult> processImage(String fileId, String familyId) {
        try {
            // 1. 获取原始图片
            InputStream imageStream = storageAdapter.download(fileId);

            // 2. 生成多尺寸缩略图
            Map<ThumbnailSize, String> thumbnails = generateThumbnails(fileId, imageStream);

            // 3. 提取EXIF元数据
            ExifMetadata exifData = exifExtractor.extract(imageStream);

            // 4. 自动分类
            List<String> categories = imageClassifier.classify(imageStream, exifData);

            // 5. 构建处理结果
            ImageProcessResult result = ImageProcessResult.builder()
                .fileId(fileId)
                .familyId(familyId)
                .thumbnails(thumbnails)
                .exifMetadata(exifData)
                .categories(categories)
                .processTime(Instant.now())
                .build();

            // 6. 缓存处理结果
            cacheImageMetadata(fileId, result);

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("图片处理失败: fileId={}, familyId={}", fileId, familyId, e);
            throw new ImageProcessingException("图片处理失败", e);
        }
    }

    /**
     * 生成多尺寸缩略图
     */
    private Map<ThumbnailSize, String> generateThumbnails(String fileId, InputStream imageStream) {
        Map<ThumbnailSize, String> thumbnails = new HashMap<>();

        for (ThumbnailSize size : ThumbnailSize.values()) {
            try {
                // 重置流到开始位置
                imageStream.reset();

                // 生成缩略图
                byte[] thumbnailBytes = thumbnailatorProcessor.generateThumbnail(
                    imageStream, size.getWidth(), size.getHeight(), size.getQuality());

                // 上传缩略图
                String thumbnailId = uploadThumbnail(fileId, size, thumbnailBytes);
                thumbnails.put(size, thumbnailId);

            } catch (Exception e) {
                log.warn("缩略图生成失败: fileId={}, size={}", fileId, size, e);
            }
        }

        return thumbnails;
    }
}
```

#### 3.2 图片分类管理
```java
/**
 * 图片自动分类和标签管理
 */
@Component
public class ImageClassifier {

    /**
     * 基于EXIF数据和时间自动分类
     */
    public List<String> classify(InputStream imageStream, ExifMetadata exifData) {
        List<String> categories = new ArrayList<>();

        // 1. 按时间分类
        if (exifData.getDateTimeOriginal() != null) {
            categories.add(getTimeBasedCategory(exifData.getDateTimeOriginal()));
        }

        // 2. 按地点分类
        if (exifData.getGpsLocation() != null) {
            categories.add(getLocationBasedCategory(exifData.getGpsLocation()));
        }

        // 3. 按设备分类
        if (exifData.getCameraMake() != null) {
            categories.add("设备/" + exifData.getCameraMake());
        }

        // 4. 按内容分类（可集成AI识别）
        categories.addAll(getContentBasedCategories(imageStream));

        return categories.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 获取基于时间的分类
     */
    private String getTimeBasedCategory(LocalDateTime dateTime) {
        int hour = dateTime.getHour();

        if (hour >= 6 && hour < 12) {
            return "时光/上午";
        } else if (hour >= 12 && hour < 18) {
            return "时光/下午";
        } else if (hour >= 18 && hour < 22) {
            return "时光/傍晚";
        } else {
            return "时光/夜晚";
        }
    }
}
```

### 4. 文件分享系统实现

#### 4.1 分享权限管理
```java
/**
 * 文件分享权限管理器
 */
@Component
public class SharePermissionManager {

    @Autowired
    private ShareTokenService shareTokenService;

    @Autowired
    private ShareAuditService shareAuditService;

    /**
     * 创建分享链接
     */
    @LogOperation(operationType = "FILE_SHARE_CREATE", description = "创建文件分享")
    public ShareResult createShare(ShareRequest request) {
        try {
            // 1. 验证用户权限
            validateSharePermission(request.getUserId(), request.getFileId());

            // 2. 生成分享令牌
            String shareToken = generateShareToken(request);

            // 3. 构建分享链接
            String shareUrl = buildShareUrl(shareToken);

            // 4. 保存分享记录
            ShareRecord shareRecord = ShareRecord.builder()
                .shareId(generateShareId())
                .fileId(request.getFileId())
                .ownerId(request.getUserId())
                .familyId(request.getFamilyId())
                .shareToken(shareToken)
                .shareUrl(shareUrl)
                .shareType(request.getShareType())
                .permissions(request.getPermissions())
                .password(request.getPassword())
                .expireTime(Instant.now().plusHours(request.getExpireHours()))
                .createTime(Instant.now())
                .build();

            shareRecord = shareRepository.save(shareRecord);

            // 5. 记录审计日志
            shareAuditService.logShareCreation(shareRecord);

            return ShareResult.builder()
                .shareId(shareRecord.getShareId())
                .shareUrl(shareUrl)
                .expireTime(shareRecord.getExpireTime())
                .build();

        } catch (Exception e) {
            log.error("创建分享失败: fileId={}, userId={}", request.getFileId(), request.getUserId(), e);
            throw new ShareException("创建分享失败", e);
        }
    }

    /**
     * 验证分享访问权限
     */
    public ShareAccessResult validateShareAccess(String shareId, String shareToken, String password) {
        // 1. 获取分享记录
        ShareRecord shareRecord = shareRepository.findByShareId(shareId)
            .orElseThrow(() -> new ShareNotFoundException("分享不存在"));

        // 2. 验证分享是否过期
        if (shareRecord.getExpireTime().isBefore(Instant.now())) {
            throw new ShareExpiredException("分享已过期");
        }

        // 3. 验证分享令牌
        if (!validateShareToken(shareRecord, shareToken)) {
            throw new ShareInvalidException("无效的分享令牌");
        }

        // 4. 验证密码（如果有）
        if (StringUtils.hasText(shareRecord.getPassword())) {
            if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, shareRecord.getPassword())) {
                throw new SharePasswordException("分享密码错误");
            }
        }

        // 5. 记录访问日志
        shareAuditService.logShareAccess(shareRecord);

        return ShareAccessResult.builder()
            .shareId(shareId)
            .fileId(shareRecord.getFileId())
            .permissions(shareRecord.getPermissions())
            .accessTime(Instant.now())
            .build();
    }
}
```

#### 4.2 分享统计和监控
```java
/**
 * 分享统计和监控服务
 */
@Component
public class ShareAnalyticsService {

    /**
     * 记录分享访问
     */
    @Async("shareAnalyticsExecutor")
    public void recordShareAccess(String shareId, String visitorId, String visitorIp, String userAgent) {
        ShareAccessRecord accessRecord = ShareAccessRecord.builder()
            .shareId(shareId)
            .visitorId(visitorId)
            .visitorIp(visitorIp)
            .userAgent(userAgent)
            .accessTime(Instant.now())
            .build();

        shareAccessRepository.save(accessRecord);

        // 更新访问统计
        updateShareStatistics(shareId);
    }

    /**
     * 获取分享统计信息
     */
    public ShareStatistics getShareStatistics(String shareId, String userId) {
        // 验证权限
        validateShareOwner(shareId, userId);

        ShareRecord shareRecord = shareRepository.findByShareId(shareId)
            .orElseThrow(() -> new ShareNotFoundException("分享不存在"));

        // 获取访问统计
        long totalAccess = shareAccessRepository.countByShareId(shareId);
        long uniqueVisitors = shareAccessRepository.countDistinctVisitorByShareId(shareId);

        // 获取最近访问记录
        List<ShareAccessRecord> recentAccess = shareAccessRepository
            .findTop10ByShareIdOrderByAccessTimeDesc(shareId);

        // 获取访问趋势
        Map<LocalDate, Long> dailyAccess = getDailyAccessTrend(shareId, 30);

        return ShareStatistics.builder()
            .shareId(shareId)
            .totalAccess(totalAccess)
            .uniqueVisitors(uniqueVisitors)
            .recentAccess(recentAccess)
            .dailyAccess(dailyAccess)
            .createTime(shareRecord.getCreateTime())
            .expireTime(shareRecord.getExpireTime())
            .build();
    }

    /**
     * 获取访问趋势数据
     */
    private Map<LocalDate, Long> getDailyAccessTrend(String shareId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<DailyAccessCount> dailyCounts = shareAccessRepository
            .countByShareIdAndDateRange(shareId, startDate, endDate);

        return dailyCounts.stream()
            .collect(Collectors.toMap(
                DailyAccessCount::getDate,
                DailyAccessCount::getCount,
                (existing, replacement) -> existing
            ));
    }
}
```

### 5. 按家庭组织的存储结构

#### 5.1 家庭存储管理
```java
/**
 * 按家庭组织的存储管理器
 */
@Component
public class FamilyStorageManager {

    /**
     * 创建家庭存储目录
     */
    public String createFamilyStoragePath(String familyId, String category, String fileName) {
        // 存储路径结构：family/{familyId}/{category}/yyyy/MM/dd/{fileId}
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileId = FileUtils.generateFileId();

        String storagePath = String.format("family/%s/%s/%s/%s",
            familyId, category, datePrefix, fileId);

        return storagePath;
    }

    /**
     * 获取家庭存储统计
     */
    public FamilyStorageStats getFamilyStorageStats(String familyId) {
        // 统计各类文件数量和大小
        Map<String, FileStats> categoryStats = new HashMap<>();

        // 图片统计
        FileStats imageStats = getFileStatsByCategory(familyId, "images");
        categoryStats.put("images", imageStats);

        // 文档统计
        FileStats documentStats = getFileStatsByCategory(familyId, "documents");
        categoryStats.put("documents", documentStats);

        // 视频统计
        FileStats videoStats = getFileStatsByCategory(familyId, "videos");
        categoryStats.put("videos", videoStats);

        // 其他统计
        FileStats otherStats = getFileStatsByCategory(familyId, "others");
        categoryStats.put("others", otherStats);

        // 总计
        long totalFiles = categoryStats.values().stream()
            .mapToLong(FileStats::getCount)
            .sum();
        long totalSize = categoryStats.values().stream()
            .mapToLong(FileStats::getSize)
            .sum();

        return FamilyStorageStats.builder()
            .familyId(familyId)
            .totalFiles(totalFiles)
            .totalSize(totalSize)
            .categoryStats(categoryStats)
            .build();
    }

    /**
     * 清理家庭过期数据
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupExpiredFamilyData() {
        List<String> familyIds = familyService.getAllActiveFamilyIds();

        for (String familyId : familyIds) {
            try {
                // 清理过期分享
                cleanupExpiredShares(familyId);

                // 清理临时缩略图
                cleanupTempThumbnails(familyId);

                // 清理孤儿文件
                cleanupOrphanFiles(familyId);

            } catch (Exception e) {
                log.error("清理家庭数据失败: familyId={}", familyId, e);
            }
        }
    }
}
```

### 6. 性能优化策略

#### 6.1 缩略图缓存
```java
/**
 * 缩略图缓存管理
 */
@Component
public class ThumbnailCacheManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String THUMBNAIL_CACHE_PREFIX = "thumbnail:";
    private static final Duration CACHE_EXPIRE = Duration.ofDays(7);

    /**
     * 缓存缩略图URL
     */
    public void cacheThumbnailUrl(String fileId, ThumbnailSize size, String url) {
        String cacheKey = buildCacheKey(fileId, size);
        ThumbnailCache cache = ThumbnailCache.builder()
            .fileId(fileId)
            .size(size)
            .url(url)
            .createTime(Instant.now())
            .build();

        redisTemplate.opsForValue().set(cacheKey, cache, CACHE_EXPIRE);
    }

    /**
     * 获取缓存的缩略图URL
     */
    public String getCachedThumbnailUrl(String fileId, ThumbnailSize size) {
        String cacheKey = buildCacheKey(fileId, size);
        ThumbnailCache cache = (ThumbnailCache) redisTemplate.opsForValue().get(cacheKey);

        return cache != null ? cache.getUrl() : null;
    }

    /**
     * 预热家庭缩略图缓存
     */
    @Async("thumbnailWarmupExecutor")
    public void warmupFamilyThumbnailCache(String familyId) {
        // 获取家庭所有图片
        List<FileMetadata> familyImages = fileMetadataService.getFamilyImages(familyId);

        for (FileMetadata image : familyImages) {
            for (ThumbnailSize size : ThumbnailSize.values()) {
                try {
                    // 检查缓存是否存在
                    if (getCachedThumbnailUrl(image.getFileId(), size) == null) {
                        // 异步生成缩略图
                        thumbnailService.generateThumbnail(image.getFileId(), size);
                    }
                } catch (Exception e) {
                    log.warn("预热缩略图缓存失败: fileId={}, size={}", image.getFileId(), size, e);
                }
            }
        }
    }
}
```

### 7. 开发注意事项

#### 必须做的事
- 所有文件操作必须进行权限验证和familyId隔离
- 必须记录文件操作和分享操作的审计日志
- 必须使用base-model提供的统一日志和异常处理
- 必须使用common组件提供的通用工具类
- 必须对图片进行自动处理（缩略图、EXIF、分类）
- 必须实现分享权限控制和访问频率限制
- 必须使用流式处理大文件，避免内存溢出
- 必须实现存储容量监控和告警

#### 不能做的事
- 不能绕过familyId进行跨家庭数据访问
- 不能在日志中记录文件内容和敏感信息
- 不能硬编码存储路径和访问密钥
- 不能允许未授权的文件分享
- 不能忽略图片处理的错误，影响主流程
- 不能在没有权限验证的情况下提供文件访问
- 不能将缩略图和原图混在一起存储
- 不能忽略分享链接的过期管理

### 8. 性能调优建议
- 使用多线程池处理图片和分享任务
- 实现多级缓存策略（Redis + 本地缓存）
- 使用CDN加速缩略图和分享文件访问
- 实现智能预加载，提前处理常用缩略图
- 使用压缩算法减少存储空间和传输带宽
- 实现分享链接的批量操作和统计
- 定期分析性能瓶颈，优化热点代码

### 9. 监控和告警
- 文件上传下载成功率监控
- 图片处理性能和成功率监控
- 分享链接访问量和异常监控
- 存储容量使用率监控
- 权限验证失败次数监控
- 系统资源使用情况监控

### 10. 知识库集成支持
作为知识库系统的存储基础，需要支持：
- 文本文件内容提取（集成Apache Tika）
- 支持知识索引服务的文件访问接口
- 图片OCR识别（为图片中的文字建立索引）
- 文件版本管理（为知识库更新提供基础）
- 支持文件内容分享到知识库
- 文件标签和元数据的智能推荐