package com.haven.base.utils;

/**
 * 文件大小工具类
 * <p>
 * 🎯 功能特性：
 * - 文件大小格式化显示
 * - 文件大小单位转换
 * - 支持多种格式输出
 * - 国际化友好的显示格式
 * <p>
 * 💡 使用场景：
 * - 文件上传大小限制显示
 * - 文件列表大小展示
 * - 存储容量统计显示
 * - 系统监控指标展示
 * <p>
 * 🔧 支持的单位：
 * - B (Bytes) - 字节
 * - KB (Kilobytes) - 千字节
 * - MB (Megabytes) - 兆字节
 * - GB (Gigabytes) - 吉字节
 * - TB (Terabytes) - 太字节
 * - PB (Petabytes) - 拍字节
 *
 * @author HavenButler
 */
public final class FileSizeUtil {

    /**
     * 私有构造函数，防止实例化
     */
    private FileSizeUtil() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }

    // 常量定义
    private static final long BYTES_PER_KB = 1024L;
    private static final long BYTES_PER_MB = 1024L * 1024L;
    private static final long BYTES_PER_GB = 1024L * 1024L * 1024L;
    private static final long BYTES_PER_TB = 1024L * 1024L * 1024L * 1024L;
    private static final long BYTES_PER_PB = 1024L * 1024L * 1024L * 1024L * 1024L;

    /**
     * 格式化文件大小显示（自动选择合适的单位）
     * <p>
     * 根据文件大小自动选择最合适的单位进行显示
     * 特殊处理无效大小值（0或负数），常用于异常情况处理
     *
     * @param bytes 文件大小（字节）
     * @return 格式化后的字符串，如 "1.5 MB"、"256 KB"
     *         对于无效值返回默认提示信息
     */
    public static String formatFileSize(long bytes) {
        // 处理异常情况：当配置错误或获取失败时，常用于文件上传限制异常处理
        if (bytes <= 0) {
            return "请检查文件大小是否超过100MB";
        }

        if (bytes < BYTES_PER_KB) {
            return bytes + " B";
        } else if (bytes < BYTES_PER_MB) {
            return formatSize(bytes, BYTES_PER_KB, "KB");
        } else if (bytes < BYTES_PER_GB) {
            return formatSize(bytes, BYTES_PER_MB, "MB");
        } else if (bytes < BYTES_PER_TB) {
            return formatSize(bytes, BYTES_PER_GB, "GB");
        } else if (bytes < BYTES_PER_PB) {
            return formatSize(bytes, BYTES_PER_TB, "TB");
        } else {
            return formatSize(bytes, BYTES_PER_PB, "PB");
        }
    }

    /**
     * 严格格式化文件大小显示（自动选择合适的单位）
     * <p>
     * 与formatFileSize不同，此方法对无效值会抛出异常，不提供默认提示
     * 适用于需要严格参数校验的场景
     *
     * @param bytes 文件大小（字节）
     * @return 格式化后的字符串，如 "1.5 MB"、"256 KB"
     * @throws IllegalArgumentException 当字节数为负数或0时抛出
     */
    public static String formatFileSizeStrict(long bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("文件大小必须为正数: " + bytes);
        }

        if (bytes < BYTES_PER_KB) {
            return bytes + " B";
        } else if (bytes < BYTES_PER_MB) {
            return formatSize(bytes, BYTES_PER_KB, "KB");
        } else if (bytes < BYTES_PER_GB) {
            return formatSize(bytes, BYTES_PER_MB, "MB");
        } else if (bytes < BYTES_PER_TB) {
            return formatSize(bytes, BYTES_PER_GB, "GB");
        } else if (bytes < BYTES_PER_PB) {
            return formatSize(bytes, BYTES_PER_TB, "TB");
        } else {
            return formatSize(bytes, BYTES_PER_PB, "PB");
        }
    }

    /**
     * 格式化文件大小显示（指定单位）
     * <p>
     * 使用指定单位进行格式化显示
     *
     * @param bytes 文件大小（字节）
     * @param unit  目标单位（KB, MB, GB, TB, PB）
     * @return 格式化后的字符串
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public static String formatFileSize(long bytes, FileSizeUnit unit) {
        if (bytes < 0) {
            throw new IllegalArgumentException("文件大小不能为负数: " + bytes);
        }

        if (unit == null) {
            throw new IllegalArgumentException("文件大小单位不能为空");
        }

        switch (unit) {
            case B:
                return bytes + " B";
            case KB:
                return formatSize(bytes, BYTES_PER_KB, "KB");
            case MB:
                return formatSize(bytes, BYTES_PER_MB, "MB");
            case GB:
                return formatSize(bytes, BYTES_PER_GB, "GB");
            case TB:
                return formatSize(bytes, BYTES_PER_TB, "TB");
            case PB:
                return formatSize(bytes, BYTES_PER_PB, "PB");
            default:
                throw new IllegalArgumentException("不支持的文件大小单位: " + unit);
        }
    }

    /**
     * 格式化文件大小显示（自定义精度）
     * <p>
     * 支持自定义小数位数进行格式化
     *
     * @param bytes      文件大小（字节）
     * @param decimalPlaces 小数位数
     * @return 格式化后的字符串
     */
    public static String formatFileSize(long bytes, int decimalPlaces) {
        if (bytes < 0) {
            throw new IllegalArgumentException("文件大小不能为负数: " + bytes);
        }

        if (decimalPlaces < 0 || decimalPlaces > 10) {
            throw new IllegalArgumentException("小数位数必须在0-10之间: " + decimalPlaces);
        }

        String format = "%." + decimalPlaces + "f %s";

        if (bytes < BYTES_PER_KB) {
            return bytes + " B";
        } else if (bytes < BYTES_PER_MB) {
            return String.format(format, (double) bytes / BYTES_PER_KB, "KB");
        } else if (bytes < BYTES_PER_GB) {
            return String.format(format, (double) bytes / BYTES_PER_MB, "MB");
        } else if (bytes < BYTES_PER_TB) {
            return String.format(format, (double) bytes / BYTES_PER_GB, "GB");
        } else if (bytes < BYTES_PER_PB) {
            return String.format(format, (double) bytes / BYTES_PER_TB, "TB");
        } else {
            return String.format(format, (double) bytes / BYTES_PER_PB, "PB");
        }
    }

    /**
     * 解析文件大小字符串为字节数
     * <p>
     * 将格式化的文件大小字符串解析为字节数
     *
     * @param sizeStr 文件大小字符串，如 "1.5 MB"、"256 KB"
     * @return 字节数
     * @throws IllegalArgumentException 当格式无效时抛出
     */
    public static long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("文件大小字符串不能为空");
        }

        String trimmed = sizeStr.trim().toUpperCase();

        // 匹配数字部分和单位部分
        if (!trimmed.matches("^\\d+(\\.\\d+)?\\s*[BKMGTP]B?$")) {
            throw new IllegalArgumentException("无效的文件大小格式: " + sizeStr);
        }

        // 分离数字和单位
        String[] parts = trimmed.split("\\s+");
        String numberPart = parts[0];
        String unitPart = parts.length > 1 ? parts[1] : "B";

        // 确保单位格式正确
        if (!unitPart.endsWith("B")) {
            unitPart += "B";
        }

        try {
            double number = Double.parseDouble(numberPart);
            FileSizeUnit unit = FileSizeUnit.valueOf(unitPart);

            return (long) (number * unit.getBytes());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的数字格式: " + numberPart, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的文件大小单位: " + unitPart, e);
        }
    }

    /**
     * 获取文件大小的单位信息
     *
     * @param bytes 文件大小（字节）
     * @return 包含数值和单位的FileSizeInfo对象
     */
    public static FileSizeInfo getFileSizeInfo(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("文件大小不能为负数: " + bytes);
        }

        FileSizeUnit unit;
        double value;

        if (bytes < BYTES_PER_KB) {
            unit = FileSizeUnit.B;
            value = bytes;
        } else if (bytes < BYTES_PER_MB) {
            unit = FileSizeUnit.KB;
            value = (double) bytes / BYTES_PER_KB;
        } else if (bytes < BYTES_PER_GB) {
            unit = FileSizeUnit.MB;
            value = (double) bytes / BYTES_PER_MB;
        } else if (bytes < BYTES_PER_TB) {
            unit = FileSizeUnit.GB;
            value = (double) bytes / BYTES_PER_GB;
        } else if (bytes < BYTES_PER_PB) {
            unit = FileSizeUnit.TB;
            value = (double) bytes / BYTES_PER_TB;
        } else {
            unit = FileSizeUnit.PB;
            value = (double) bytes / BYTES_PER_PB;
        }

        return new FileSizeInfo(value, unit, bytes);
    }

    /**
     * 比较两个文件大小
     *
     * @param bytes1 第一个文件大小（字节）
     * @param bytes2 第二个文件大小（字节）
     * @return 比较结果：-1表示bytes1小于bytes2，0表示相等，1表示bytes1大于bytes2
     */
    public static int compareFileSize(long bytes1, long bytes2) {
        return Long.compare(bytes1, bytes2);
    }

    /**
     * 判断文件大小是否在指定范围内
     *
     * @param bytes  文件大小（字节）
     * @param minBytes 最小值（字节）
     * @param maxBytes 最大值（字节）
     * @return 是否在范围内
     */
    public static boolean isFileSizeInRange(long bytes, long minBytes, long maxBytes) {
        return bytes >= minBytes && bytes <= maxBytes;
    }

    /**
     * 格式化文件大小核心方法
     */
    private static String formatSize(long bytes, long divisor, String unit) {
        double size = (double) bytes / divisor;

        // 对于整数结果，不显示小数部分
        if (size == (long) size) {
            return String.format("%.0f %s", size, unit);
        } else {
            return String.format("%.1f %s", size, unit);
        }
    }

    /**
     * 文件大小单位枚举
     */
    public enum FileSizeUnit {
        B(1L, "B"),
        KB(1024L, "KB"),
        MB(1024L * 1024L, "MB"),
        GB(1024L * 1024L * 1024L, "GB"),
        TB(1024L * 1024L * 1024L * 1024L, "TB"),
        PB(1024L * 1024L * 1024L * 1024L * 1024L, "PB");

        private final long bytes;
        private final String symbol;

        FileSizeUnit(long bytes, String symbol) {
            this.bytes = bytes;
            this.symbol = symbol;
        }

        public long getBytes() {
            return bytes;
        }

        public String getSymbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    /**
     * 文件大小信息类
     */
    public static class FileSizeInfo {
        private final double value;
        private final FileSizeUnit unit;
        private final long bytes;

        public FileSizeInfo(double value, FileSizeUnit unit, long bytes) {
            this.value = value;
            this.unit = unit;
            this.bytes = bytes;
        }

        public double getValue() {
            return value;
        }

        public FileSizeUnit getUnit() {
            return unit;
        }

        public long getBytes() {
            return bytes;
        }

        @Override
        public String toString() {
            return String.format("%.1f %s", value, unit.getSymbol());
        }
    }
}