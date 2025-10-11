package com.haven.storage.gallery;

/**
 * 缩略图尺寸枚举
 *
 * 定义不同用途的缩略图尺寸规格
 *
 * @author HavenButler
 */
public enum ThumbnailSize {

    /**
     * 小尺寸缩略图 - 用于列表预览
     */
    SMALL("small", 200, 200, 0.8f, "列表预览图"),

    /**
     * 中等尺寸缩略图 - 用于详情页展示
     */
    MEDIUM("medium", 400, 400, 0.85f, "详情展示图"),

    /**
     * 大尺寸缩略图 - 用于画廊浏览
     */
    LARGE("large", 800, 800, 0.9f, "画廊浏览图");

    private final String name;
    private final int width;
    private final int height;
    private final float quality;
    private final String description;

    ThumbnailSize(String name, int width, int height, float quality, String description) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.quality = quality;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getQuality() {
        return quality;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据名称查找缩略图尺寸
     *
     * @param name 尺寸名称
     * @return 对应的枚举值，如果未找到则返回SMALL
     */
    public static ThumbnailSize fromName(String name) {
        if (name == null) {
            return SMALL;
        }

        for (ThumbnailSize size : values()) {
            if (size.name.equalsIgnoreCase(name)) {
                return size;
            }
        }

        return SMALL;
    }

    /**
     * 检查是否为有效的尺寸名称
     *
     * @param name 尺寸名称
     * @return 是否有效
     */
    public static boolean isValidSize(String name) {
        if (name == null) {
            return false;
        }

        for (ThumbnailSize size : values()) {
            if (size.name.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取所有可用的尺寸名称
     *
     * @return 尺寸名称数组
     */
    public static String[] getAllSizeNames() {
        ThumbnailSize[] sizes = values();
        String[] names = new String[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            names[i] = sizes[i].name;
        }
        return names;
    }

    @Override
    public String toString() {
        return String.format("%s (%dx%d, %.0f%%)", name, width, height, quality * 100);
    }
}