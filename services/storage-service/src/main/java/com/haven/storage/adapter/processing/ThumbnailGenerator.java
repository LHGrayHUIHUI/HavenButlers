package com.haven.storage.adapter.processing;

import com.haven.storage.domain.model.file.FileMetadata;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 缩略图生成器
 *
 * 负责为上传的图片文件生成多尺寸缩略图
 * 支持多种图片格式和缩略图规格
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class ThumbnailGenerator {

    /**
     * 缩略图尺寸规格
     */
    @Getter
    public enum ThumbnailSize {
        SMALL("small", 150, 150, 0.8f),
        MEDIUM("medium", 300, 300, 0.85f),
        LARGE("large", 800, 600, 0.9f);

        private final String name;
        private final int width;
        private final int height;
        private final float quality;

        ThumbnailSize(String name, int width, int height, float quality) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.quality = quality;
        }

    }

    /**
     * 异步生成缩略图
     *
     * @param fileMetadata 文件元数据
     * @param traceId 链路追踪ID
     */
    @Async
    public void generateThumbnailAsync(FileMetadata fileMetadata, String traceId) {
        try {
            log.info("开始生成缩略图: fileId={}, traceId={}", fileMetadata.getFileId(), traceId);

            // TODO: 从存储服务获取原始文件数据
            // byte[] fileData = fileStorageService.downloadFileData(fileMetadata.getFileId(), fileMetadata.getFamilyId());
            byte[] fileData = new byte[0]; // 临时实现

            if (fileData == null || fileData.length == 0) {
                log.warn("无法获取文件数据，跳过缩略图生成: fileId={}, traceId={}", fileMetadata.getFileId(), traceId);
                return;
            }

            // 生成多尺寸缩略图
            Map<ThumbnailSize, String> thumbnailPaths = generateThumbnails(fileData, fileMetadata, traceId);

            // 更新文件元数据
            updateFileMetadataWithThumbnails(fileMetadata, thumbnailPaths, traceId);

            log.info("缩略图生成完成: fileId={}, thumbnailCount={}, traceId={}",
                    fileMetadata.getFileId(), thumbnailPaths.size(), traceId);

        } catch (Exception e) {
            log.error("缩略图生成失败: fileId={}, traceId={}, error={}",
                    fileMetadata.getFileId(), traceId, e.getMessage(), e);
        }
    }

    /**
     * 生成多尺寸缩略图
     *
     * @param fileData 原始文件数据
     * @param fileMetadata 文件元数据
     * @param traceId 链路追踪ID
     * @return 缩略图路径映射
     */
    private Map<ThumbnailSize, String> generateThumbnails(byte[] fileData, FileMetadata fileMetadata, String traceId) {
        Map<ThumbnailSize, String> thumbnailPaths = new HashMap<>();

        try {
            // 将文件数据转换为BufferedImage
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(fileData));
            if (originalImage == null) {
                log.warn("无法解析图片文件: fileId={}, traceId={}", fileMetadata.getFileId(), traceId);
                return thumbnailPaths;
            }

            // 为每种尺寸生成缩略图
            for (ThumbnailSize size : ThumbnailSize.values()) {
                try {
                    String thumbnailPath = generateSingleThumbnail(originalImage, size, fileMetadata, traceId);
                    if (thumbnailPath != null) {
                        thumbnailPaths.put(size, thumbnailPath);
                    }
                } catch (Exception e) {
                    log.warn("生成缩略图失败: fileId={}, size={}, traceId={}, error={}",
                            fileMetadata.getFileId(), size.name(), traceId, e.getMessage());
                }
            }

        } catch (IOException e) {
            log.error("读取原始图片失败: fileId={}, traceId={}, error={}",
                    fileMetadata.getFileId(), traceId, e.getMessage(), e);
        }

        return thumbnailPaths;
    }

    /**
     * 生成单个缩略图
     *
     * @param originalImage 原始图片
     * @param size 缩略图尺寸
     * @param fileMetadata 文件元数据
     * @param traceId 链路追踪ID
     * @return 缩略图存储路径
     */
    private String generateSingleThumbnail(BufferedImage originalImage, ThumbnailSize size, FileMetadata fileMetadata, String traceId) {
        try {
            // 计算缩略图尺寸（保持宽高比）
            int targetWidth = size.getWidth();
            int targetHeight = size.getHeight();
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 保持宽高比计算实际尺寸
            double aspectRatio = (double) originalWidth / originalHeight;
            if (aspectRatio > (double) targetWidth / targetHeight) {
                targetHeight = (int) (targetWidth / aspectRatio);
            } else {
                targetWidth = (int) (targetHeight * aspectRatio);
            }

            // 创建缩略图
            BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();

            // 设置渲染质量
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制缩略图
            g2d.drawImage(originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);
            g2d.dispose();

            // 构建缩略图存储路径
            String thumbnailPath = buildThumbnailPath(fileMetadata, size, traceId);

            // 将缩略图转换为字节数组
            byte[] thumbnailData = imageToByteArray(thumbnail, size.getQuality());

            // TODO: 上传缩略图到存储服务
            // boolean success = fileStorageService.uploadThumbnail(thumbnailData, thumbnailPath, fileMetadata.getFamilyId());

            log.debug("缩略图生成成功: size={}, path={}, traceId={}", size.name(), thumbnailPath, traceId);
            return thumbnailPath;

        } catch (Exception e) {
            log.error("生成单个缩略图失败: fileId={}, size={}, traceId={}, error={}",
                    fileMetadata.getFileId(), size.name(), traceId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建缩略图存储路径
     *
     * @param fileMetadata 文件元数据
     * @param size 缩略图尺寸
     * @param traceId 链路追踪ID
     * @return 缩略图存储路径
     */
    private String buildThumbnailPath(FileMetadata fileMetadata, ThumbnailSize size, String traceId) {
        String familyId = fileMetadata.getFamilyId();
        String fileId = fileMetadata.getFileId();
        String fileExtension = getFileExtension(fileMetadata.getOriginalName());

        return String.format("thumbnails/%s/%s/%s_%s.%s",
                familyId, size.getName(), fileId, traceId, fileExtension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "jpg";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : "jpg";
    }

    /**
     * 将BufferedImage转换为字节数组
     *
     * @param image 图片
     * @param quality 质量 (0.0-1.0)
     * @return 字节数组
     */
    private byte[] imageToByteArray(BufferedImage image, float quality) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 使用PNG格式保持质量
            javax.imageio.ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("图片转换为字节数组失败", e);
            return new byte[0];
        }
    }

    /**
     * 更新文件元数据中的缩略图信息
     *
     * @param fileMetadata 文件元数据
     * @param thumbnailPaths 缩略图路径映射
     * @param traceId 链路追踪ID
     */
    private void updateFileMetadataWithThumbnails(FileMetadata fileMetadata, Map<ThumbnailSize, String> thumbnailPaths, String traceId) {
        try {
            // TODO: 更新数据库中的缩略图信息
            // fileMetadataService.updateThumbnailPaths(fileMetadata.getFileId(), thumbnailPaths);

            log.info("缩略图信息已更新: fileId={}, thumbnailCount={}, traceId={}",
                    fileMetadata.getFileId(), thumbnailPaths.size(), traceId);

        } catch (Exception e) {
            log.error("更新缩略图信息失败: fileId={}, traceId={}, error={}",
                    fileMetadata.getFileId(), traceId, e.getMessage(), e);
        }
    }

    /**
     * 批量生成缩略图
     *
     * @param fileMetadataList 文件元数据列表
     * @param traceId 链路追踪ID
     */
    @Async
    public void generateThumbnailsBatch(java.util.List<FileMetadata> fileMetadataList, String traceId) {
        if (fileMetadataList == null || fileMetadataList.isEmpty()) {
            log.warn("文件列表为空，跳过批量缩略图生成");
            return;
        }

        log.info("开始批量生成缩略图: fileCount={}, traceId={}", fileMetadataList.size(), traceId);

        for (FileMetadata fileMetadata : fileMetadataList) {
            if (fileMetadata != null && isImageFile(fileMetadata)) {
                generateThumbnailAsync(fileMetadata, traceId);
            }
        }

        log.info("批量缩略图生成任务已提交: fileCount={}, traceId={}", fileMetadataList.size(), traceId);
    }

    /**
     * 检查是否为图片文件
     */
    private boolean isImageFile(FileMetadata fileMetadata) {
        String fileType = fileMetadata.getFileType();
        String mimeType = fileMetadata.getMimeType();

        return "image".equals(fileType) || (mimeType != null && mimeType.startsWith("image/"));
    }

    /**
     * 删除缩略图
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID
     */
    public void deleteThumbnails(String fileId, String familyId) {
        log.info("删除文件缩略图: fileId={}, familyId={}", fileId, familyId);

        try {
            // TODO: 实现缩略图删除逻辑
            // 1. 查找所有相关的缩略图文件
            // 2. 从存储服务删除缩略图
            // 3. 更新数据库记录

        } catch (Exception e) {
            log.error("删除缩略图失败: fileId={}, familyId={}, error={}", fileId, familyId, e.getMessage(), e);
        }
    }

    /**
     * 获取缩略图URL
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID
     * @param size 缩略图尺寸
     * @param expireMinutes 过期时间（分钟）
     * @return 缩略图URL
     */
    public String getThumbnailUrl(String fileId, String familyId, ThumbnailSize size, int expireMinutes) {
        try {
            // TODO: 实现缩略图URL生成逻辑
            String thumbnailPath = String.format("thumbnails/%s/%s/%s", familyId, size.getName(), fileId);

            // return fileStorageService.generateAccessUrl(thumbnailPath, expireMinutes);
            return ""; // 临时实现

        } catch (Exception e) {
            log.error("获取缩略图URL失败: fileId={}, familyId={}, size={}, error={}",
                    fileId, familyId, size.name(), e.getMessage(), e);
            return null;
        }
    }
}