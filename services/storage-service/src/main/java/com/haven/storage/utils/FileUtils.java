package com.haven.storage.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件工具类
 *
 * @author Haven
 */
public class FileUtils {

    /**
     * 生成唯一文件ID
     */
    public static String generateFileId() {
        return "file_" + System.currentTimeMillis() + "_" +
                Integer.toHexString((int) (Math.random() * 0xFFFF));
    }

    /**
     * 构建家庭存储路径
     */
    public static String buildFamilyPath(String familyId, String category) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("family/%s/%s/%s", familyId, category, datePrefix);
    }

    /**
     * 构建完整文件路径
     */
    public static String buildFilePath(String familyId, String category, String fileName) {
        String basePath = buildFamilyPath(familyId, category);
        String fileId = generateFileId();
        String extension = getFileExtension(fileName);

        if (extension != null && !extension.isEmpty()) {
            return String.format("%s/%s.%s", basePath, fileId, extension);
        } else {
            return String.format("%s/%s", basePath, fileId);
        }
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 获取文件名（不含扩展名）
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName;
        }

        return fileName.substring(0, lastDotIndex);
    }

    /**
     * 判断是否为图片文件
     */
    public static boolean isImageFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        String extension = getFileExtension(fileName);
        if (extension == null) {
            return false;
        }

        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico"};
        for (String imgExt : imageExtensions) {
            if (imgExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否为视频文件
     */
    public static boolean isVideoFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        String extension = getFileExtension(fileName);
        if (extension == null) {
            return false;
        }

        String[] videoExtensions = {"mp4", "avi", "mov", "wmv", "flv", "webm", "mkv", "m4v"};
        for (String videoExt : videoExtensions) {
            if (videoExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否为文档文件
     */
    public static boolean isDocumentFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        String extension = getFileExtension(fileName);
        if (extension == null) {
            return false;
        }

        String[] documentExtensions = {"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "txt", "rtf", "odt", "ods", "odp"};
        for (String docExt : documentExtensions) {
            if (docExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 验证文件名是否合法
     */
    public static boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isEmpty() || fileName.length() > 255) {
            return false;
        }

        // 检查非法字符
        String[] invalidChars = {"\\", "/", ":", "*", "?", "\"", "<", ">", "|"};
        for (String invalidChar : invalidChars) {
            if (fileName.contains(invalidChar)) {
                return false;
            }
        }

        // 检查是否为保留名称
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3",
                "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
                "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6",
                "LPT7", "LPT8", "LPT9"};

        String nameWithoutExt = getFileNameWithoutExtension(fileName).toUpperCase();
        for (String reservedName : reservedNames) {
            if (reservedName.equals(nameWithoutExt)) {
                return false;
            }
        }

        return true;
    }
}