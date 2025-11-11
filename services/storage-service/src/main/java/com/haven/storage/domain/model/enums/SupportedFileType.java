package com.haven.storage.domain.model.enums;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持的文件类型定义枚举 (包含魔数参考)
 * * 职责：定义应用支持哪些类型，并提供从MIME和扩展名查找的能力。
 * 魔数检测逻辑由 FileTypeDetector 委托给 Tika 完成。
 */
@Getter
public enum SupportedFileType {

    // --- 图片 ---
    JPG(List.of("jpg", "jpeg"), "image/jpeg", FileCategory.IMAGE),
    PNG(List.of("png"), "image/png", FileCategory.IMAGE),
    GIF(List.of("gif"), "image/gif", FileCategory.IMAGE),
    BMP(List.of("bmp"), "image/bmp", FileCategory.IMAGE),

    // --- 视频 ---
    MP4(List.of("mp4", "m4v"), "video/mp4", FileCategory.VIDEO),
    MOV(List.of("mov"), "video/quicktime", FileCategory.VIDEO),

    // --- 文档 (注意：DOCX/XLSX/PPTX 的 MIME 类型是特定的，不是 application/zip) ---
    PDF(List.of("pdf"), "application/pdf", FileCategory.DOCUMENT),
    DOC(List.of("doc"), "application/msword", FileCategory.DOCUMENT),
    DOCX(List.of("docx"), "application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileCategory.DOCUMENT),
    XLS(List.of("xls"), "application/vnd.ms-excel", FileCategory.DOCUMENT),
    XLSX(List.of("xlsx"), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FileCategory.DOCUMENT),
    PPT(List.of("ppt"), "application/vnd.ms-powerpoint", FileCategory.DOCUMENT),
    PPTX(List.of("pptx"), "application/vnd.openxmlformats-officedocument.presentationml.presentation", FileCategory.DOCUMENT),
    TXT(List.of("txt"), "text/plain", FileCategory.DOCUMENT),

    // --- 压缩包 ---
    ZIP(List.of("zip"), "application/zip", FileCategory.ARCHIVE),
    RAR(List.of("rar"), "application/x-rar-compressed", FileCategory.ARCHIVE),

    // --- 未知/通用 ---
    UNKNOWN(List.of(), "application/octet-stream", FileCategory.UNKNOWN);


    // Getters
    private final List<String> extensions;
    private final String mimeType;
    private final FileCategory category;

    SupportedFileType(List<String> extensions, String mimeType, FileCategory category) {
        this.extensions = extensions;
        this.mimeType = mimeType;
        this.category = category;
    }

    // ===== 查找逻辑 (自动构建映射表) =====

    private static final Map<String, SupportedFileType> EXTENSION_MAP;
    private static final Map<String, SupportedFileType> MIME_MAP;

    static {
        Map<String, SupportedFileType> extMap = new HashMap<>();
        Map<String, SupportedFileType> mimeMap = new HashMap<>();

        for (SupportedFileType type : values()) {
            // 填充 MIME 映射
            if (!type.getMimeType().equals("application/octet-stream")) {
                mimeMap.put(type.getMimeType().toLowerCase(), type);
            }

            // 填充 扩展名 映射
            for (String ext : type.getExtensions()) {
                extMap.put(ext.toLowerCase(), type);
            }
        }

        EXTENSION_MAP = Collections.unmodifiableMap(extMap);
        MIME_MAP = Collections.unmodifiableMap(mimeMap);
    }

    /**
     * 根据文件扩展名查找 (用于文件名检测)
     */
    public static SupportedFileType findByExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return UNKNOWN;
        }
        return EXTENSION_MAP.getOrDefault(extension.toLowerCase(), UNKNOWN);
    }

    /**
     * 根据MIME类型查找 (用于MIME和魔数检测)
     */
    public static SupportedFileType findByMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return UNKNOWN;
        }
        // 清理 ;charset=UTF-8 等
        String cleanMime = mimeType.split(";")[0].trim().toLowerCase();

        // 精确匹配
        SupportedFileType exactMatch = MIME_MAP.get(cleanMime);
        if (exactMatch != null) {
            return exactMatch;
        }

        // Tika 可能会返回 'text/plain; charset=ISO-8859-1'，但我们只定义了 'text/plain'
        // 对于未在枚举中明确定义的 MIME 类型（如 'image/tiff'），我们进行分类匹配
        if (cleanMime.startsWith("image/")) return JPG; // 归类为一种已知的图片
        if (cleanMime.startsWith("video/")) return MP4; // 归类为一种已知的视频
        if (cleanMime.startsWith("audio/")) return UNKNOWN; // 假设不支持
        if (cleanMime.startsWith("text/")) return TXT;   // 归类为 TXT

        return UNKNOWN;
    }
}
