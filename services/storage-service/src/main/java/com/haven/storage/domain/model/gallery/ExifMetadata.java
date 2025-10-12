package com.haven.storage.domain.model.gallery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * EXIF元数据实体类
 *
 * 存储从图片中提取的EXIF信息，包括拍摄时间、相机信息、GPS位置等
 *
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExifMetadata {

    /**
     * 拍摄时间
     */
    private LocalDateTime dateTimeOriginal;

    /**
     * 数字化时间
     */
    private LocalDateTime dateTimeDigitized;

    /**
     * 修改时间
     */
    private LocalDateTime dateTimeModified;

    /**
     * 相机制造商
     */
    private String make;

    /**
     * 相机型号
     */
    private String model;

    /**
     * 镜头型号
     */
    private String lensModel;

    /**
     * 镜头规格
     */
    private String lensSpecification;

    /**
     * 焦距（mm）
     */
    private BigDecimal focalLength;

    /**
     * 35mm等效焦距
     */
    private BigDecimal focalLength35mm;

    /**
     * 光圈值（f/number）
     */
    private BigDecimal fNumber;

    /**
     * 曝光时间（秒）
     */
    private BigDecimal exposureTime;

    /**
     * ISO感光度
     */
    private Integer isoSpeedRatings;

    /**
     * 闪光灯模式
     */
    private String flash;

    /**
     * 曝光模式
     */
    private String exposureMode;

    /**
     * 测光模式
     */
    private String meteringMode;

    /**
     * 白平衡模式
     */
    private String whiteBalance;

    /**
     * 色彩空间
     */
    private String colorSpace;

    /**
     * 压缩模式
     */
    private String compression;

    /**
     * 图像方向
     */
    private Integer orientation;

    /**
     * X分辨率
     */
    private BigDecimal xResolution;

    /**
     * Y分辨率
     */
    private BigDecimal yResolution;

    /**
     * 分辨率单位
     */
    private String resolutionUnit;

    /**
     * 软件信息
     */
    private String software;

    /**
     * 拍摄者姓名
     */
    private String artist;

    /**
     * 版权信息
     */
    private String copyright;

    /**
     * 用户注释
     */
    private String userComment;

    /**
     * GPS纬度
     */
    private BigDecimal gpsLatitude;

    /**
     * GPS经度
     */
    private BigDecimal gpsLongitude;

    /**
     * GPS海拔（米）
     */
    private BigDecimal gpsAltitude;

    /**
     * GPS方位角
     */
    private BigDecimal gpsImgDirection;

    /**
     * GPS日期时间
     */
    private LocalDateTime gpsDateTime;

    /**
     * GPS位置描述
     */
    private String gpsLocationDescription;

    /**
     * 图像宽度（像素）
     */
    private Integer exifImageWidth;

    /**
     * 图像高度（像素）
     */
    private Integer exifImageHeight;

    /**
     * 图像唯一ID
     */
    private String imageUniqueId;

    /**
     * 子区块数量
     */
    private Integer subSecTime;

    /**
     * 子区块原始时间
     */
    private String subSecTimeOriginal;

    /**
     * 子区块数字化时间
     */
    private String subSecTimeDigitized;

    /**
     * 检查是否包含GPS信息
     */
    public boolean hasGpsInfo() {
        return gpsLatitude != null && gpsLongitude != null;
    }

    /**
     * 检查是否包含相机信息
     */
    public boolean hasCameraInfo() {
        return make != null || model != null;
    }

    /**
     * 检查是否包含拍摄时间信息
     */
    public boolean hasDateTimeInfo() {
        return dateTimeOriginal != null;
    }

    /**
     * 获取完整的相机描述
     */
    public String getFullCameraDescription() {
        StringBuilder sb = new StringBuilder();

        if (make != null) {
            sb.append(make);
        }

        if (model != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(model);
        }

        if (lensModel != null) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(lensModel);
        }

        return sb.length() > 0 ? sb.toString() : "未知设备";
    }

    /**
     * 获取拍摄参数描述
     */
    public String getShootingParametersDescription() {
        StringBuilder sb = new StringBuilder();

        if (focalLength != null) {
            sb.append(focalLength).append("mm");
        }

        if (fNumber != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("f/").append(fNumber);
        }

        if (exposureTime != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            if (exposureTime.compareTo(BigDecimal.ONE) < 0) {
                // 小于1秒的曝光时间显示为分数
                sb.append("1/").append(BigDecimal.ONE.divide(exposureTime, 0, RoundingMode.HALF_UP)).append("s");
            } else {
                sb.append(exposureTime).append("s");
            }
        }

        if (isoSpeedRatings != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("ISO").append(isoSpeedRatings);
        }

        return sb.length() > 0 ? sb.toString() : "未知参数";
    }
}