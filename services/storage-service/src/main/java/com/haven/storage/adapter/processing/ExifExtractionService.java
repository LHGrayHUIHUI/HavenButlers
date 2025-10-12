package com.haven.storage.adapter.processing;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.gallery.ExifMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * EXIF元数据提取服务
 *
 * 从图片文件中提取EXIF信息，包括拍摄时间、相机信息、GPS位置等
 * 使用metadata-extractor库进行元数据提取
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class ExifExtractionService {

    /**
     * 提取图片的EXIF元数据
     *
     * @param imageStream 图片输入流
     * @return EXIF元数据对象
     */
    @TraceLog(value = "提取EXIF元数据", module = "exif-extraction", type = "EXTRACT_EXIF")
    public ExifMetadata extractExifMetadata(InputStream imageStream) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageStream);
            ExifMetadata exifData = new ExifMetadata();

            // 提取各个目录的元数据
            extractBasicExifData(metadata, exifData);
            extractCameraInfo(metadata, exifData);
            extractShootingParameters(metadata, exifData);
            extractDateTimeInfo(metadata, exifData);
            extractGpsInfo(metadata, exifData);
            extractImageDimensions(metadata, exifData);

            log.debug("EXIF元数据提取完成: traceId={}", traceId);
            return exifData;

        } catch (ImageProcessingException | IOException e) {
            log.warn("EXIF元数据提取失败: error={}, traceId={}", e.getMessage(), traceId);
            return null;
        }
    }

    /**
     * 提取基础EXIF数据
     */
    private void extractBasicExifData(Metadata metadata, ExifMetadata exifData) {
        ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd0Directory != null) {
            exifData.setMake(ifd0Directory.getString(ExifDirectoryBase.TAG_MAKE));
            exifData.setModel(ifd0Directory.getString(ExifDirectoryBase.TAG_MODEL));
            exifData.setSoftware(ifd0Directory.getString(ExifDirectoryBase.TAG_SOFTWARE));
            exifData.setArtist(ifd0Directory.getString(ExifDirectoryBase.TAG_ARTIST));
            exifData.setCopyright(ifd0Directory.getString(ExifDirectoryBase.TAG_COPYRIGHT));
            exifData.setOrientation(ifd0Directory.getInteger(ExifDirectoryBase.TAG_ORIENTATION));
            exifData.setXResolution(getRational(ifd0Directory, ExifDirectoryBase.TAG_X_RESOLUTION));
            exifData.setYResolution(getRational(ifd0Directory, ExifDirectoryBase.TAG_Y_RESOLUTION));
            exifData.setResolutionUnit(ifd0Directory.getString(ExifDirectoryBase.TAG_RESOLUTION_UNIT));
        }
    }

    /**
     * 提取相机信息
     */
    private void extractCameraInfo(Metadata metadata, ExifMetadata exifData) {
        ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        ExifSubIFDDirectory subIfdDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (ifd0Directory != null) {
            exifData.setMake(ifd0Directory.getString(ExifDirectoryBase.TAG_MAKE));
            exifData.setModel(ifd0Directory.getString(ExifDirectoryBase.TAG_MODEL));
        }

        if (subIfdDirectory != null) {
            exifData.setLensModel(subIfdDirectory.getString(ExifDirectoryBase.TAG_LENS_MODEL));
            exifData.setLensSpecification(subIfdDirectory.getString(ExifDirectoryBase.TAG_LENS_SPECIFICATION));
        }
    }

    /**
     * 提取拍摄参数
     */
    private void extractShootingParameters(Metadata metadata, ExifMetadata exifData) {
        ExifSubIFDDirectory subIfdDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (subIfdDirectory != null) {
            exifData.setFocalLength(getRational(subIfdDirectory, ExifDirectoryBase.TAG_FOCAL_LENGTH));
            // TODO: 这些标签在新版本中可能不存在，需要查找正确的标签常量
            // exifData.setFocalLength35mm(subIfdDirectory.getInteger(ExifDirectoryBase.TAG_FOCAL_LENGTH_IN_35MM_FORMAT));
            // exifData.setFNumber(getRational(subIfdDirectory, ExifDirectoryBase.TAG_F_NUMBER));
            exifData.setExposureTime(getRational(subIfdDirectory, ExifDirectoryBase.TAG_EXPOSURE_TIME));
            exifData.setIsoSpeedRatings(subIfdDirectory.getInteger(ExifDirectoryBase.TAG_ISO_EQUIVALENT));
            exifData.setFlash(getFlashDescription(subIfdDirectory));
            exifData.setExposureMode(getExposureModeDescription(subIfdDirectory));
            exifData.setMeteringMode(getMeteringModeDescription(subIfdDirectory));
            exifData.setWhiteBalance(getWhiteBalanceDescription(subIfdDirectory));
            exifData.setColorSpace(subIfdDirectory.getString(ExifDirectoryBase.TAG_COLOR_SPACE));
            exifData.setCompression(subIfdDirectory.getString(ExifDirectoryBase.TAG_COMPRESSION));
        }
    }

    /**
     * 提取日期时间信息
     */
    private void extractDateTimeInfo(Metadata metadata, ExifMetadata exifData) {
        ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        ExifSubIFDDirectory subIfdDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (ifd0Directory != null) {
            exifData.setDateTimeModified(parseDateTime(ifd0Directory.getString(ExifDirectoryBase.TAG_DATETIME)));
        }

        if (subIfdDirectory != null) {
            exifData.setDateTimeOriginal(parseDateTime(subIfdDirectory.getString(ExifDirectoryBase.TAG_DATETIME_ORIGINAL)));
            exifData.setDateTimeDigitized(parseDateTime(subIfdDirectory.getString(ExifDirectoryBase.TAG_DATETIME_DIGITIZED)));
            // TODO: 这些标签在新版本中可能不存在，需要查找正确的标签常量
            // exifData.setSubSecTime(subIfdDirectory.getInteger(ExifDirectoryBase.TAG_SUB_SEC_TIME));
            // exifData.setSubSecTimeOriginal(subIfdDirectory.getString(ExifDirectoryBase.TAG_SUB_SEC_TIME_ORIGINAL));
            // exifData.setSubSecTimeDigitized(subIfdDirectory.getString(ExifDirectoryBase.TAG_SUB_SEC_TIME_DIGITIZED));
        }
    }

    /**
     * 提取GPS信息
     */
    private void extractGpsInfo(Metadata metadata, ExifMetadata exifData) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory != null && gpsDirectory.containsTag(GpsDirectory.TAG_LATITUDE) &&
            gpsDirectory.containsTag(GpsDirectory.TAG_LONGITUDE)) {

            try {
                exifData.setGpsLatitude(convertGpsCoordinate(gpsDirectory, GpsDirectory.TAG_LATITUDE));
                exifData.setGpsLongitude(convertGpsCoordinate(gpsDirectory, GpsDirectory.TAG_LONGITUDE));
                exifData.setGpsAltitude(getRational(gpsDirectory, GpsDirectory.TAG_ALTITUDE));
                exifData.setGpsImgDirection(getRational(gpsDirectory, GpsDirectory.TAG_IMG_DIRECTION));

                // 解析GPS日期时间
                String gpsDateStr = gpsDirectory.getString(GpsDirectory.TAG_DATE_STAMP);
                String gpsTimeStr = gpsDirectory.getString(GpsDirectory.TAG_TIME_STAMP);
                if (gpsDateStr != null && gpsTimeStr != null) {
                    exifData.setGpsDateTime(parseGpsDateTime(gpsDateStr, gpsTimeStr));
                }

                // 生成位置描述
                exifData.setGpsLocationDescription(generateLocationDescription(exifData));

            } catch (Exception e) {
                log.debug("GPS信息解析失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 提取图像尺寸信息
     */
    private void extractImageDimensions(Metadata metadata, ExifMetadata exifData) {
        ExifSubIFDDirectory subIfdDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (subIfdDirectory != null) {
            exifData.setExifImageWidth(subIfdDirectory.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH));
            exifData.setExifImageHeight(subIfdDirectory.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT));
        }
    }

    /**
     * 获取有理数值
     */
    private BigDecimal getRational(Directory directory, int tagType) {
        try {
            if (directory.containsTag(tagType)) {
                return BigDecimal.valueOf(directory.getRational(tagType).doubleValue());
            }
        } catch (Exception e) {
            log.debug("获取有理数值失败: tag={}, error={}", tagType, e.getMessage());
        }
        return null;
    }

    /**
     * 解析日期时间
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            log.debug("日期时间解析失败: {}", dateTimeStr);
            return null;
        }
    }

    /**
     * 解析GPS日期时间
     */
    private LocalDateTime parseGpsDateTime(String dateStr, String timeStr) {
        if (dateStr == null || timeStr == null) {
            return null;
        }

        try {
            String dateTimeStr = dateStr + " " + timeStr;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            log.debug("GPS日期时间解析失败: date={}, time={}", dateStr, timeStr);
            return null;
        }
    }

    /**
     * 转换GPS坐标
     */
    private BigDecimal convertGpsCoordinate(GpsDirectory gpsDirectory, int tagType) {
        try {
            return BigDecimal.valueOf(gpsDirectory.getGeoLocation().getLatitude());
        } catch (Exception e) {
            log.debug("GPS坐标转换失败: tag={}, error={}", tagType, e.getMessage());
            return null;
        }
    }

    /**
     * 生成位置描述
     */
    private String generateLocationDescription(ExifMetadata exifData) {
        if (exifData.getGpsLatitude() != null && exifData.getGpsLongitude() != null) {
            return String.format("%.6f, %.6f",
                    exifData.getGpsLatitude(), exifData.getGpsLongitude());
        }
        return null;
    }

    /**
     * 获取闪光灯描述
     */
    private String getFlashDescription(ExifSubIFDDirectory directory) {
        try {
            if (directory.containsTag(ExifDirectoryBase.TAG_FLASH)) {
                int flashValue = directory.getInteger(ExifDirectoryBase.TAG_FLASH);
                return (flashValue & 0x01) == 0 ? "未闪光" : "闪光";
            }
        } catch (Exception e) {
            log.debug("闪光灯信息解析失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取曝光模式描述
     */
    private String getExposureModeDescription(ExifSubIFDDirectory directory) {
        try {
            if (directory.containsTag(ExifDirectoryBase.TAG_EXPOSURE_MODE)) {
                int mode = directory.getInteger(ExifDirectoryBase.TAG_EXPOSURE_MODE);
                switch (mode) {
                    case 0: return "自动";
                    case 1: return "手动";
                    case 2: return "自动包围";
                    default: return "未知";
                }
            }
        } catch (Exception e) {
            log.debug("曝光模式解析失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取测光模式描述
     */
    private String getMeteringModeDescription(ExifSubIFDDirectory directory) {
        try {
            if (directory.containsTag(ExifDirectoryBase.TAG_METERING_MODE)) {
                int mode = directory.getInteger(ExifDirectoryBase.TAG_METERING_MODE);
                switch (mode) {
                    case 0: return "未知";
                    case 1: return "平均";
                    case 2: return "中央重点";
                    case 3: return "点测";
                    case 4: return "多点";
                    case 5: return "评估";
                    case 6: return "局部";
                    default: return "未知";
                }
            }
        } catch (Exception e) {
            log.debug("测光模式解析失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取白平衡描述
     */
    private String getWhiteBalanceDescription(ExifSubIFDDirectory directory) {
        try {
            if (directory.containsTag(ExifDirectoryBase.TAG_WHITE_BALANCE)) {
                int balance = directory.getInteger(ExifDirectoryBase.TAG_WHITE_BALANCE);
                switch (balance) {
                    case 0: return "自动";
                    case 1: return "手动";
                    default: return "未知";
                }
            }
        } catch (Exception e) {
            log.debug("白平衡解析失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取所有EXIF标签（用于调试）
     */
    public List<String> getAllExifTags(InputStream imageStream) {
        List<String> tags = new ArrayList<>();
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageStream);

            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String tagInfo = String.format("[%s] %s: %s",
                            directory.getName(), tag.getTagName(), tag.getDescription());
                    tags.add(tagInfo);
                }
            }

            log.debug("EXIF标签解析完成: count={}, traceId={}", tags.size(), traceId);

        } catch (ImageProcessingException | IOException e) {
            log.warn("EXIF标签解析失败: error={}, traceId={}", e.getMessage(), traceId);
        }

        return tags;
    }
}