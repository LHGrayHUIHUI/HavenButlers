package com.haven.storage.gallery;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.file.FileMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 图片分类服务
 *
 * 基于EXIF数据和文件信息对图片进行自动分类
 * 支持按时间、地点、设备、事件等多维度分类
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class ImageClassificationService {

    // 常见的相机品牌
    private static final List<String> CAMERA_BRANDS = Arrays.asList(
            "Canon", "Nikon", "Sony", "Fujifilm", "Olympus", "Panasonic",
            "Samsung", "LG", "Xiaomi", "Huawei", "OPPO", "Vivo", "OnePlus"
    );

    // 支持的图片格式
    private static final List<String> IMAGE_FORMATS = Arrays.asList(
            "JPG", "JPEG", "PNG", "GIF", "BMP", "WEBP", "TIFF", "RAW"
    );

    /**
     * 对图片进行分类
     *
     * @param fileMetadata 文件元数据
     * @param exifData EXIF元数据
     * @return 分类标签列表
     */
    @TraceLog(value = "图片分类", module = "image-classification", type = "CLASSIFY_IMAGE")
    public List<String> classifyImage(FileMetadata fileMetadata, ExifMetadata exifData) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();
        List<String> categories = new ArrayList<>();

        try {
            // 1. 按时间分类
            List<String> timeCategories = classifyByTime(exifData, fileMetadata);
            categories.addAll(timeCategories);

            // 2. 按地点分类
            List<String> locationCategories = classifyByLocation(exifData);
            categories.addAll(locationCategories);

            // 3. 按设备分类
            List<String> deviceCategories = classifyByDevice(exifData);
            categories.addAll(deviceCategories);

            // 4. 按文件格式分类
            List<String> formatCategories = classifyByFormat(fileMetadata);
            categories.addAll(formatCategories);

            // 5. 按文件大小分类
            List<String> sizeCategories = classifyBySize(fileMetadata);
            categories.addAll(sizeCategories);

            // 6. 按特殊特征分类
            List<String> featureCategories = classifyByFeatures(exifData);
            categories.addAll(featureCategories);

            // 去重并返回
            List<String> uniqueCategories = categories.stream()
                    .distinct()
                    .filter(category -> category != null && !category.trim().isEmpty())
                    .sorted()
                    .toList();

            log.info("图片分类完成: categories={}, traceId={}", uniqueCategories, traceId);
            return uniqueCategories;

        } catch (Exception e) {
            log.error("图片分类失败: error={}, traceId={}", e.getMessage(), traceId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 按时间分类
     */
    private List<String> classifyByTime(ExifMetadata exifData, FileMetadata fileMetadata) {
        List<String> categories = new ArrayList<>();
        LocalDateTime dateTime = null;

        // 优先使用EXIF中的拍摄时间
        if (exifData != null && exifData.getDateTimeOriginal() != null) {
            dateTime = exifData.getDateTimeOriginal();
        } else if (fileMetadata != null && fileMetadata.getUploadTime() != null) {
            dateTime = fileMetadata.getUploadTime();
        }

        if (dateTime == null) {
            return categories;
        }

        // 按时间段分类
        int hour = dateTime.getHour();
        if (hour >= 5 && hour < 9) {
            categories.add("时光/清晨");
        } else if (hour >= 9 && hour < 12) {
            categories.add("时光/上午");
        } else if (hour >= 12 && hour < 14) {
            categories.add("时光/中午");
        } else if (hour >= 14 && hour < 17) {
            categories.add("时光/下午");
        } else if (hour >= 17 && hour < 19) {
            categories.add("时光/傍晚");
        } else if (hour >= 19 && hour < 22) {
            categories.add("时光/晚上");
        } else {
            categories.add("时光/深夜");
        }

        // 按季节分类
        int month = dateTime.getMonthValue();
        if (month >= 3 && month <= 5) {
            categories.add("季节/春季");
        } else if (month >= 6 && month <= 8) {
            categories.add("季节/夏季");
        } else if (month >= 9 && month <= 11) {
            categories.add("季节/秋季");
        } else {
            categories.add("季节/冬季");
        }

        // 按年份分类
        categories.add("年份/" + dateTime.getYear());

        // 按月份分类
        String monthName = dateTime.getMonth().name();
        categories.add("月份/" + monthName);

        // 按节假日分类（简单实现）
        categories.addAll(classifyByHoliday(dateTime));

        return categories;
    }

    /**
     * 按节假日分类
     */
    private List<String> classifyByHoliday(LocalDateTime dateTime) {
        List<String> categories = new ArrayList<>();
        int month = dateTime.getMonthValue();
        int day = dateTime.getDayOfMonth();

        // 元旦
        if (month == 1 && day == 1) {
            categories.add("节日/元旦");
        }

        // 春节（简单判断，实际应该根据农历）
        if (month >= 1 && month <= 2) {
            categories.add("节日/春节");
        }

        // 情人节
        if (month == 2 && day == 14) {
            categories.add("节日/情人节");
        }

        // 劳动节
        if (month == 5 && day >= 1 && day <= 3) {
            categories.add("节日/劳动节");
        }

        // 儿童节
        if (month == 6 && day == 1) {
            categories.add("节日/儿童节");
        }

        // 国庆节
        if (month == 10 && day >= 1 && day <= 7) {
            categories.add("节日/国庆节");
        }

        // 圣诞节
        if (month == 12 && day == 25) {
            categories.add("节日/圣诞节");
        }

        // 周末
        if (dateTime.getDayOfWeek().getValue() >= 6) {
            categories.add("时光/周末");
        }

        return categories;
    }

    /**
     * 按地点分类
     */
    private List<String> classifyByLocation(ExifMetadata exifData) {
        List<String> categories = new ArrayList<>();

        if (exifData == null) {
            return categories;
        }

        // 如果有GPS信息
        if (exifData.hasGpsInfo()) {
            categories.add("地点/有GPS信息");

            // 根据GPS信息判断大致位置（这里只是简单示例）
            double latitude = exifData.getGpsLatitude().doubleValue();
            double longitude = exifData.getGpsLongitude().doubleValue();

            // 中国境内大致判断
            if (latitude >= 3.86 && latitude <= 53.55 &&
                longitude >= 73.66 && longitude <= 135.05) {
                categories.add("地点/中国");
            }

            // 根据经纬度判断南北半球
            if (latitude > 0) {
                categories.add("地点/北半球");
            } else {
                categories.add("地点/南半球");
            }

            // 根据经纬度判断东西半球
            if (longitude > 0) {
                categories.add("地点/东半球");
            } else {
                categories.add("地点/西半球");
            }
        } else {
            categories.add("地点/无GPS信息");
        }

        return categories;
    }

    /**
     * 按设备分类
     */
    private List<String> classifyByDevice(ExifMetadata exifData) {
        List<String> categories = new ArrayList<>();

        if (exifData == null) {
            return categories;
        }

        String make = exifData.getMake();
        String model = exifData.getModel();

        if (make != null) {
            String makeUpper = make.toUpperCase();

            // 按品牌分类
            for (String brand : CAMERA_BRANDS) {
                if (makeUpper.contains(brand.toUpperCase())) {
                    categories.add("设备/" + brand);
                    break;
                }
            }

            // 按设备类型分类
            if (makeUpper.contains("IPHONE") || makeUpper.contains("ANDROID")) {
                categories.add("设备/手机");
            } else if (makeUpper.contains("CANON") || makeUpper.contains("NIKON") ||
                       makeUpper.contains("SONY")) {
                categories.add("设备/相机");
            } else {
                categories.add("设备/其他");
            }
        }

        // 根据拍摄参数判断设备类型
        if (exifData.getFocalLength() != null && exifData.getFocalLength().doubleValue() > 50) {
            categories.add("设备/长焦");
        } else if (exifData.getFocalLength() != null && exifData.getFocalLength().doubleValue() < 35) {
            categories.add("设备/广角");
        }

        return categories;
    }

    /**
     * 按文件格式分类
     */
    private List<String> classifyByFormat(FileMetadata fileMetadata) {
        List<String> categories = new ArrayList<>();

        if (fileMetadata == null || fileMetadata.getOriginalName() == null) {
            return categories;
        }

        String fileName = fileMetadata.getOriginalName().toUpperCase();

        // 提取文件扩展名
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
            String extension = fileName.substring(lastDotIndex + 1);

            if (IMAGE_FORMATS.contains(extension)) {
                categories.add("格式/" + extension);

                // 按格式类型分类
                if (extension.equals("JPG") || extension.equals("JPEG")) {
                    categories.add("格式/有损压缩");
                } else if (extension.equals("PNG")) {
                    categories.add("格式/无损压缩");
                } else if (extension.equals("RAW")) {
                    categories.add("格式/原始格式");
                } else if (extension.equals("GIF")) {
                    categories.add("格式/动图");
                }
            }
        }

        return categories;
    }

    /**
     * 按文件大小分类
     */
    private List<String> classifyBySize(FileMetadata fileMetadata) {
        List<String> categories = new ArrayList<>();

        if (fileMetadata == null) {
            return categories;
        }

        // 检查fileSize是否有效（大于0）
        if (fileMetadata.getFileSize() <= 0) {
            return categories;
        }

        long fileSize = fileMetadata.getFileSize();

        // 按大小分类（字节）
        if (fileSize < 100 * 1024) { // 小于100KB
            categories.add("大小/小图片");
        } else if (fileSize < 500 * 1024) { // 小于500KB
            categories.add("大小/中等图片");
        } else if (fileSize < 2 * 1024 * 1024) { // 小于2MB
            categories.add("大小/大图片");
        } else {
            categories.add("大小/超大图片");
        }

        // 按大小单位分类
        if (fileSize < 1024) {
            categories.add("单位/字节");
        } else if (fileSize < 1024 * 1024) {
            categories.add("单位/KB");
        } else if (fileSize < 1024 * 1024 * 1024) {
            categories.add("单位/MB");
        } else {
            categories.add("单位/GB");
        }

        return categories;
    }

    /**
     * 按特殊特征分类
     */
    private List<String> classifyByFeatures(ExifMetadata exifData) {
        List<String> categories = new ArrayList<>();

        if (exifData == null) {
            return categories;
        }

        // 根据拍摄参数分类
        if (exifData.getFlash() != null && exifData.getFlash().contains("闪光")) {
            categories.add("特征/闪光灯");
        }

        if (exifData.getIsoSpeedRatings() != null) {
            int iso = exifData.getIsoSpeedRatings();
            if (iso >= 1600) {
                categories.add("特征/高感光");
            }
        }

        if (exifData.getExposureTime() != null) {
            double exposureTime = exifData.getExposureTime().doubleValue();
            if (exposureTime >= 1) {
                categories.add("特征/长曝光");
            }
        }

        if (exifData.getFNumber() != null) {
            double fNumber = exifData.getFNumber().doubleValue();
            if (fNumber <= 2.8) {
                categories.add("特征/大光圈");
            }
        }

        // 根据图像方向分类
        if (exifData.getOrientation() != null) {
            int orientation = exifData.getOrientation();
            if (orientation != 1) {
                categories.add("特征/旋转");
            }
        }

        return categories;
    }

    /**
     * 获取所有可用分类
     *
     * @return 分类列表
     */
    public List<String> getAllAvailableCategories() {
        return Arrays.asList(
                "时光/清晨", "时光/上午", "时光/中午", "时光/下午",
                "时光/傍晚", "时光/晚上", "时光/深夜", "时光/周末",
                "季节/春季", "季节/夏季", "季节/秋季", "季节/冬季",
                "节日/元旦", "节日/春节", "节日/情人节", "节日/劳动节",
                "节日/儿童节", "节日/国庆节", "节日/圣诞节",
                "地点/有GPS信息", "地点/无GPS信息", "地点/中国",
                "地点/北半球", "地点/南半球", "地点/东半球", "地点/西半球",
                "设备/手机", "设备/相机", "设备/长焦", "设备/广角",
                "格式/JPG", "格式/PNG", "格式/RAW", "格式/GIF",
                "格式/有损压缩", "格式/无损压缩", "格式/原始格式", "格式/动图",
                "大小/小图片", "大小/中等图片", "大小/大图片", "大小/超大图片",
                "单位/字节", "单位/KB", "单位/MB", "单位/GB",
                "特征/闪光灯", "特征/高感光", "特征/长曝光", "特征/大光圈", "特征/旋转"
        );
    }
}