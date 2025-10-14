package com.haven.storage.utils;

import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件类型检测器
 * <p>
 * 统一的文件类型检测工具类，提供以下功能：
 * 1. 基于MIME类型的文件类型检测
 * 2. 基于文件扩展名的文件类型检测
 * 3. 双重验证机制（MIME类型 + 扩展名）
 * 4. 支持的文件类型配置管理
 * 5. 文件类型分类（图片、视频、音频、文档、压缩包等）
 * <p>
 * 设计原则：
 * - 线程安全：使用不可变集合和ConcurrentHashMap
 * - 高性能：缓存机制减少重复计算
 * - 可扩展：支持动态添加新的文件类型
 * - 易维护：集中的文件类型配置管理
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class FileTypeDetector {

    // ===== 文件扩展名到MIME类型的映射 =====

    /**
     * 图片文件扩展名到MIME类型的映射
     */
    private static final Map<String, String> IMAGE_EXTENSIONS_TO_MIME = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "bmp", "image/bmp",
            "webp", "image/webp",
            "svg", "image/svg+xml",
            "ico", "image/x-icon",
            "tiff", "image/tiff",
            "psd", "image/vnd.adobe.photoshop"
    );

    /**
     * 视频文件扩展名到MIME类型的映射
     */
    private static final Map<String, String> VIDEO_EXTENSIONS_TO_MIME = Map.of(
            "mp4", "video/mp4",
            "avi", "video/x-msvideo",
            "mov", "video/quicktime",
            "wmv", "video/x-ms-wmv",
            "flv", "video/x-flv",
            "webm", "video/webm",
            "mkv", "video/x-matroska",
            "m4v", "video/x-m4v",
            "3gp", "video/3gpp"
    );

    /**
     * 音频文件扩展名到MIME类型的映射
     */
    private static final Map<String, String> AUDIO_EXTENSIONS_TO_MIME = Map.of(
            "mp3", "audio/mpeg",
            "wav", "audio/wav",
            "flac", "audio/flac",
            "aac", "audio/aac",
            "ogg", "audio/ogg",
            "m4a", "audio/mp4",
            "wma", "audio/x-ms-wma",
            "opus", "audio/opus"
    );

    /**
     * 文档文件扩展名到MIME类型的映射
     */
    private static final Map<String, String> DOCUMENT_EXTENSIONS_TO_MIME;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("pdf", "application/pdf");
        map.put("doc", "application/msword");
        map.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        map.put("xls", "application/vnd.ms-excel");
        map.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        map.put("ppt", "application/vnd.ms-powerpoint");
        map.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        map.put("txt", "text/plain");
        map.put("md", "text/markdown");
        map.put("markdown", "text/markdown");
        map.put("rtf", "application/rtf");
        map.put("odt", "application/vnd.oasis.opendocument.text");
        map.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
        map.put("odp", "application/vnd.oasis.opendocument.presentation");
        map.put("csv", "text/csv");
        map.put("json", "application/json");
        map.put("xml", "text/xml");
        map.put("html", "text/html");
        map.put("htm", "text/html");
        map.put("css", "text/css");
        map.put("js", "text/javascript");
        // 新增编程语言文件
        map.put("java", "text/x-java-source");
        map.put("py", "text/x-python");
        map.put("sh", "application/x-sh");
        // 新增应用程序文件
        map.put("apk", "application/vnd.android.package-archive");
        map.put("exe", "application/x-msdownload");
        map.put("ipa", "application/x-itunes-ipa");
        DOCUMENT_EXTENSIONS_TO_MIME = Collections.unmodifiableMap(map);
    }

    /**
     * 压缩文件扩展名到MIME类型的映射
     */
    private static final Map<String, String> ARCHIVE_EXTENSIONS_TO_MIME = Map.of(
            "zip", "application/zip",
            "rar", "application/x-rar-compressed",
            "7z", "application/x-7z-compressed",
            "tar", "application/x-tar",
            "gz", "application/gzip",
            "bz2", "application/x-bzip2"
    );

    // ===== 统一的扩展名到MIME类型映射 =====

    /**
     * 所有支持的文件扩展名到MIME类型的映射
     */
    private static final Map<String, String> EXTENSION_TO_MIME;

    static {
        Map<String, String> map = new HashMap<>();
        map.putAll(IMAGE_EXTENSIONS_TO_MIME);
        map.putAll(VIDEO_EXTENSIONS_TO_MIME);
        map.putAll(AUDIO_EXTENSIONS_TO_MIME);
        map.putAll(DOCUMENT_EXTENSIONS_TO_MIME);
        map.putAll(ARCHIVE_EXTENSIONS_TO_MIME);
        EXTENSION_TO_MIME = Collections.unmodifiableMap(map);
    }

    // ===== MIME类型到文件分类的映射 =====

    /**
     * MIME类型到文件分类的映射
     */
    private static final Map<String, String> MIME_TO_CATEGORY;

    static {
        Map<String, String> map = new HashMap<>();

        // 图片分类
        IMAGE_EXTENSIONS_TO_MIME.values().forEach(mime -> map.put(mime, "image"));

        // 视频分类
        VIDEO_EXTENSIONS_TO_MIME.values().forEach(mime -> map.put(mime, "video"));

        // 音频分类
        AUDIO_EXTENSIONS_TO_MIME.values().forEach(mime -> map.put(mime, "audio"));

        // 文档分类
        DOCUMENT_EXTENSIONS_TO_MIME.values().forEach(mime -> map.put(mime, "document"));

        // 压缩包分类
        ARCHIVE_EXTENSIONS_TO_MIME.values().forEach(mime -> map.put(mime, "archive"));

        MIME_TO_CATEGORY = Collections.unmodifiableMap(map);
    }

    // ===== 支持的MIME类型集合 =====

    /**
     * 所有支持的MIME类型
     */
    private static final Set<String> SUPPORTED_MIME_TYPES = Collections.unmodifiableSet(EXTENSION_TO_MIME.keySet());

    /**
     * 支持的文档MIME类型（用于验证器）
     */
    private static final Set<String> SUPPORTED_DOCUMENT_MIME_TYPES = Set.of(
            "text/plain", "text/html", "text/css", "text/javascript", "text/markdown", "text/x-markdown",
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/json", "application/xml", "text/xml", "text/csv"
    );

    /**
     * 支持的图片MIME类型（用于验证器）
     */
    private static final Set<String> SUPPORTED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp", "image/svg+xml"
    );

    /**
     * 支持的视频MIME类型（用于验证器）
     */
    private static final Set<String> SUPPORTED_VIDEO_MIME_TYPES = Set.of(
            "video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv", "video/webm"
    );

    /**
     * 支持的音频MIME类型（用于验证器）
     */
    private static final Set<String> SUPPORTED_AUDIO_MIME_TYPES = Set.of(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/flac", "audio/aac", "audio/ogg"
    );

    /**
     * 支持的压缩包MIME类型（用于验证器）
     */
    private static final Set<String> SUPPORTED_ARCHIVE_MIME_TYPES = Set.of(
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
            "application/x-tar", "application/gzip"
    );

    // ===== 缓存机制 =====

    /**
     * 文件类型检测结果缓存
     */
    private final Map<String, FileTypeDetectionResult> detectionCache = new ConcurrentHashMap<>();

    /**
     * 缓存大小限制
     */
    private static final int MAX_CACHE_SIZE = 1000;

    // ===== 公共API方法 =====

    /**
     * 检测文件类型（双重验证：MIME类型 + 扩展名）
     * <p>
     * 优先使用MIME类型进行检测，当MIME类型为通用类型（application/octet-stream）时，
     * 使用文件扩展名作为备用检测机制。
     *
     * @param fileName     文件名
     * @param mimeType     MIME类型
     * @param traceId      链路追踪ID
     * @return 文件类型检测结果
     */
    public FileTypeDetectionResult detectFileType(String fileName, String mimeType, String traceId) {
        if (traceId == null) {
            traceId = TraceIdUtil.getCurrentOrGenerate();
        }

        String cacheKey = buildCacheKey(fileName, mimeType);

        // 检查缓存
        FileTypeDetectionResult cachedResult = detectionCache.get(cacheKey);
        if (cachedResult != null) {
            log.debug("从缓存获取文件类型检测结果: fileName={}, mimeType={}, result={}, traceId={}",
                    fileName, mimeType, cachedResult, traceId);
            return cachedResult;
        }

        try {
            FileTypeDetectionResult result = performDetection(fileName, mimeType, traceId);

            // 缓存结果
            cacheResult(cacheKey, result);

            log.debug("文件类型检测完成: fileName={}, mimeType={}, result={}, traceId={}",
                    fileName, mimeType, result, traceId);

            return result;

        } catch (Exception e) {
            log.error("文件类型检测异常: fileName={}, mimeType={}, traceId={}, error={}",
                    fileName, mimeType, traceId, e.getMessage(), e);

            FileTypeDetectionResult errorResult = FileTypeDetectionResult.unknown("检测异常: " + e.getMessage());
            cacheResult(cacheKey, errorResult);
            return errorResult;
        }
    }

    /**
     * 检测文件类型（仅使用文件名）
     *
     * @param fileName 文件名
     * @param traceId  链路追踪ID
     * @return 文件类型检测结果
     */
    public FileTypeDetectionResult detectFileTypeByName(String fileName, String traceId) {
        return detectFileType(fileName, null, traceId);
    }

    /**
     * 检测文件类型（仅使用MIME类型）
     *
     * @param mimeType MIME类型
     * @param traceId  链路追踪ID
     * @return 文件类型检测结果
     */
    public FileTypeDetectionResult detectFileTypeByMimeType(String mimeType, String traceId) {
        return detectFileType(null, mimeType, traceId);
    }

    /**
     * 验证文件类型是否被支持（基于MIME类型）
     * <p>
     * 用于验证器的快速验证方法
     *
     * @param mimeType MIME类型
     * @return 是否支持该文件类型
     */
    public boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return false;
        }

        String lowerMimeType = mimeType.toLowerCase();

        // 检查是否在支持的MIME类型列表中
        if (SUPPORTED_MIME_TYPES.contains(lowerMimeType)) {
            return true;
        }

        // 检查通配符匹配
        return lowerMimeType.startsWith("image/") ||
               lowerMimeType.startsWith("video/") ||
               lowerMimeType.startsWith("audio/") ||
               lowerMimeType.startsWith("text/");
    }

    /**
     * 验证文件扩展名是否被支持
     * <p>
     * 用于存储适配器的快速验证方法
     *
     * @param fileName 文件名
     * @return 是否支持该文件扩展名
     */
    public boolean isSupportedExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        String extension = extractExtension(fileName);
        return extension != null && EXTENSION_TO_MIME.containsKey(extension.toLowerCase());
    }

    /**
     * 根据文件扩展名获取MIME类型
     *
     * @param fileName 文件名
     * @return MIME类型，如果不支持则返回"application/octet-stream"
     */
    public String getMimeTypeByExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "application/octet-stream";
        }

        String extension = extractExtension(fileName);
        if (extension == null) {
            return "application/octet-stream";
        }

        String mimeType = EXTENSION_TO_MIME.get(extension.toLowerCase());
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    /**
     * 根据MIME类型获取文件分类
     *
     * @param mimeType MIME类型
     * @return 文件分类（image、video、audio、document、archive、unknown）
     */
    public String getCategoryByMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return "unknown";
        }

        String lowerMimeType = mimeType.toLowerCase();

        // 直接映射查找
        String category = MIME_TO_CATEGORY.get(lowerMimeType);
        if (category != null) {
            return category;
        }

        // 通用模式匹配
        if (lowerMimeType.startsWith("image/")) {
            return "image";
        } else if (lowerMimeType.startsWith("video/")) {
            return "video";
        } else if (lowerMimeType.startsWith("audio/")) {
            return "audio";
        } else if (lowerMimeType.startsWith("text/")) {
            return "document";
        } else if (lowerMimeType.contains("document") ||
                   lowerMimeType.contains("pdf") ||
                   lowerMimeType.contains("spreadsheet") ||
                   lowerMimeType.contains("presentation")) {
            return "document";
        } else if (lowerMimeType.contains("zip") ||
                   lowerMimeType.contains("rar") ||
                   lowerMimeType.contains("tar") ||
                   lowerMimeType.contains("7z")) {
            return "archive";
        }

        return "unknown";
    }

    /**
     * 获取所有支持的文件扩展名
     *
     * @return 支持的文件扩展名集合
     */
    public Set<String> getSupportedExtensions() {
        return EXTENSION_TO_MIME.keySet();
    }

    /**
     * 获取所有支持的文件扩展名（以逗号分隔的字符串形式）
     * <p>
     * 用于配置allowedExtensions等场景
     *
     * @return 逗号分隔的文件扩展名字符串
     */
    public String getSupportedExtensionsAsString() {
        return String.join(",", EXTENSION_TO_MIME.keySet());
    }

    /**
     * 获取所有支持的MIME类型
     *
     * @return 支持的MIME类型集合
     */
    public Set<String> getSupportedMimeTypes() {
        return SUPPORTED_MIME_TYPES;
    }

    /**
     * 获取特定分类支持的扩展名
     *
     * @param category 文件分类（image、video、audio、document、archive）
     * @return 该分类支持的扩展名集合
     */
    public Set<String> getExtensionsByCategory(String category) {
        if (category == null) {
            return Collections.emptySet();
        }

        switch (category.toLowerCase()) {
            case "image":
                return IMAGE_EXTENSIONS_TO_MIME.keySet();
            case "video":
                return VIDEO_EXTENSIONS_TO_MIME.keySet();
            case "audio":
                return AUDIO_EXTENSIONS_TO_MIME.keySet();
            case "document":
                return DOCUMENT_EXTENSIONS_TO_MIME.keySet();
            case "archive":
                return ARCHIVE_EXTENSIONS_TO_MIME.keySet();
            default:
                return Collections.emptySet();
        }
    }

    // ===== 私有辅助方法 =====

    /**
     * 执行文件类型检测
     */
    private FileTypeDetectionResult performDetection(String fileName, String mimeType, String traceId) {
        String extension = extractExtension(fileName);
        String detectedMimeType = determineMimeType(fileName, mimeType, extension, traceId);
        String category = getCategoryByMimeType(detectedMimeType);
        String detectionMethod = determineDetectionMethod(mimeType, extension);

        boolean isSupported = isSupportedMimeType(detectedMimeType) ||
                             (extension != null && EXTENSION_TO_MIME.containsKey(extension.toLowerCase()));

        return FileTypeDetectionResult.builder()
                .fileName(fileName)
                .extension(extension)
                .detectedMimeType(detectedMimeType)
                .originalMimeType(mimeType)
                .category(category)
                .isSupported(isSupported)
                .detectionMethod(detectionMethod)
                .traceId(traceId)
                .build();
    }

    /**
     * 确定MIME类型
     */
    private String determineMimeType(String fileName, String mimeType, String extension, String traceId) {
        // 1. 如果提供了有效的MIME类型且不是通用类型，优先使用
        if (mimeType != null && !mimeType.trim().isEmpty() &&
            !"application/octet-stream".equals(mimeType)) {

            log.debug("使用提供的MIME类型: mimeType={}, traceId={}", mimeType, traceId);
            return mimeType.toLowerCase();
        }

        // 2. 如果有文件扩展名，尝试通过扩展名确定MIME类型
        if (extension != null) {
            String mimeTypeByExtension = EXTENSION_TO_MIME.get(extension.toLowerCase());
            if (mimeTypeByExtension != null) {
                log.info("通过扩展名确定MIME类型: extension={}, mimeType={}, traceId={}",
                        extension, mimeTypeByExtension, traceId);
                return mimeTypeByExtension;
            }
        }

        // 3. 如果都无法确定，返回原始MIME类型或通用类型
        String resultMimeType = (mimeType != null && !mimeType.trim().isEmpty()) ?
                                mimeType : "application/octet-stream";

        log.warn("无法确定具体的MIME类型: fileName={}, extension={}, originalMimeType={}, result={}, traceId={}",
                fileName, extension, mimeType, resultMimeType, traceId);

        return resultMimeType.toLowerCase();
    }

    /**
     * 确定检测方法
     */
    private String determineDetectionMethod(String mimeType, String extension) {
        if (mimeType != null && !"application/octet-stream".equals(mimeType)) {
            return extension != null ? "MIME_TYPE_PLUS_EXTENSION" : "MIME_TYPE_ONLY";
        } else if (extension != null) {
            return "EXTENSION_ONLY";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * 提取文件扩展名
     */
    private String extractExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String fileName, String mimeType) {
        return (fileName != null ? fileName : "") + "|" + (mimeType != null ? mimeType : "");
    }

    /**
     * 缓存结果
     */
    private void cacheResult(String cacheKey, FileTypeDetectionResult result) {
        // 简单的缓存大小控制
        if (detectionCache.size() >= MAX_CACHE_SIZE) {
            // 清理一半的缓存
            detectionCache.clear();
        }

        detectionCache.put(cacheKey, result);
    }

    // ===== 内部类：文件类型检测结果 =====

    /**
     * 文件类型检测结果
     */
    public static class FileTypeDetectionResult {
        private final String fileName;
        private final String extension;
        private final String detectedMimeType;
        private final String originalMimeType;
        private final String category;
        private final boolean isSupported;
        private final String detectionMethod;
        private final String traceId;

        private FileTypeDetectionResult(Builder builder) {
            this.fileName = builder.fileName;
            this.extension = builder.extension;
            this.detectedMimeType = builder.detectedMimeType;
            this.originalMimeType = builder.originalMimeType;
            this.category = builder.category;
            this.isSupported = builder.isSupported;
            this.detectionMethod = builder.detectionMethod;
            this.traceId = builder.traceId;
        }

        public static FileTypeDetectionResult unknown(String reason) {
            return new Builder()
                    .detectedMimeType("application/octet-stream")
                    .category("unknown")
                    .isSupported(false)
                    .detectionMethod("UNKNOWN")
                    .build();
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getFileName() { return fileName; }
        public String getExtension() { return extension; }
        public String getDetectedMimeType() { return detectedMimeType; }
        public String getOriginalMimeType() { return originalMimeType; }
        public String getCategory() { return category; }
        public boolean isSupported() { return isSupported; }
        public String getDetectionMethod() { return detectionMethod; }
        public String getTraceId() { return traceId; }

        @Override
        public String toString() {
            return "FileTypeDetectionResult{" +
                    "fileName='" + fileName + '\'' +
                    ", extension='" + extension + '\'' +
                    ", detectedMimeType='" + detectedMimeType + '\'' +
                    ", originalMimeType='" + originalMimeType + '\'' +
                    ", category='" + category + '\'' +
                    ", isSupported=" + isSupported +
                    ", detectionMethod='" + detectionMethod + '\'' +
                    ", traceId='" + traceId + '\'' +
                    '}';
        }

        public static class Builder {
            private String fileName;
            private String extension;
            private String detectedMimeType;
            private String originalMimeType;
            private String category;
            private boolean isSupported;
            private String detectionMethod;
            private String traceId;

            public Builder fileName(String fileName) { this.fileName = fileName; return this; }
            public Builder extension(String extension) { this.extension = extension; return this; }
            public Builder detectedMimeType(String detectedMimeType) { this.detectedMimeType = detectedMimeType; return this; }
            public Builder originalMimeType(String originalMimeType) { this.originalMimeType = originalMimeType; return this; }
            public Builder category(String category) { this.category = category; return this; }
            public Builder isSupported(boolean isSupported) { this.isSupported = isSupported; return this; }
            public Builder detectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; return this; }
            public Builder traceId(String traceId) { this.traceId = traceId; return this; }

            public FileTypeDetectionResult build() {
                return new FileTypeDetectionResult(this);
            }
        }
    }
}