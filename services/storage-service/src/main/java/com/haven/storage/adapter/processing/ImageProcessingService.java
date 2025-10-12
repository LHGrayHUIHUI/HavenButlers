package com.haven.storage.adapter.processing;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;

import com.haven.storage.adapter.storage.StorageAdapter;
import com.haven.storage.domain.model.file.FileMetadata;
import com.haven.storage.domain.model.file.ThumbnailSize;
import com.haven.storage.domain.model.gallery.ExifMetadata;
import com.haven.storage.domain.model.gallery.ImageMetadata;
import com.haven.storage.domain.model.gallery.ImageProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 图片处理服务
 *
 * 提供缩略图生成、EXIF元数据提取、图片分类等功能
 * 支持异步处理，提升用户体验
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private final StorageAdapter storageAdapter;
    private final ExifExtractionService exifExtractionService;
    private final ImageClassificationService classificationService;

    // 支持的图片格式
    private static final String[] SUPPORTED_FORMATS = {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff"
    };

    /**
     * 处理上传的图片
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID
     * @param fileMetadata 文件元数据
     * @return 处理结果
     */
    @TraceLog(value = "处理上传图片", module = "image-processing", type = "PROCESS_IMAGE")
    public CompletableFuture<ImageProcessResult> processUploadedImage(String fileId,
                                                                      String familyId,
                                                                      FileMetadata fileMetadata) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始处理图片: fileId={}, familyId={}, traceId={}", fileId, familyId, traceId);

                // 1. 获取原始图片流
                byte[] imageData = storageAdapter.downloadFile(fileId, familyId).getFileContent();
                InputStream imageStream = new ByteArrayInputStream(imageData);

                if (imageStream == null) {
                    throw new RuntimeException("无法获取图片文件流");
                }

                // 2. 提取EXIF元数据
                ExifMetadata exifData = exifExtractionService.extractExifMetadata(imageStream);

                // 3. 生成缩略图
                Map<ThumbnailSize, String> thumbnails = generateThumbnails(fileId, imageStream);

                // 4. 图片分类
                java.util.List<String> categories = classificationService.classifyImage(fileMetadata, exifData);

                // 5. 构建图片元数据
                ImageMetadata imageMetadata = ImageMetadata.builder()
                        .fileId(fileId)
                        .familyId(familyId)
                        .fileName(fileMetadata.getOriginalName())
                        .fileSize(fileMetadata.getFileSize())
                        .exifData(exifData)
                        .thumbnails(new HashMap<>())
                        .categories(categories)
                        .uploadTime(LocalDateTime.now())
                        .lastAccessTime(LocalDateTime.now())
                        .accessCount(0)
                        .isPublic(false)
                        .captureTime(exifData != null ? exifData.getDateTimeOriginal() : fileMetadata.getUploadTime())
                        .build();

                // 设置缩略图信息
                Map<String, String> thumbnailPaths = new HashMap<>();
                for (Map.Entry<ThumbnailSize, String> entry : thumbnails.entrySet()) {
                    thumbnailPaths.put(entry.getKey().getName(), entry.getValue());
                }
                imageMetadata.getThumbnails().putAll(thumbnailPaths);

                // 设置图片尺寸（从EXIF或从文件读取）
                if (exifData != null) {
                    imageMetadata.setWidth(exifData.getExifImageWidth());
                    imageMetadata.setHeight(exifData.getExifImageHeight());
                    imageMetadata.setDeviceInfo(exifData.getFullCameraDescription());
                    imageMetadata.setLocation(exifData.getGpsLocationDescription());
                }

                log.info("图片处理完成: fileId={}, thumbnails={}, categories={}, traceId={}",
                        fileId, thumbnails.size(), categories.size(), traceId);

                return ImageProcessResult.success(imageMetadata);

            } catch (Exception e) {
                log.error("图片处理失败: fileId={}, familyId={}, error={}, traceId={}",
                        fileId, familyId, e.getMessage(), traceId, e);
                return ImageProcessResult.failure("图片处理失败: " + e.getMessage());
            }
        });
    }

    /**
     * 生成缩略图
     *
     * @param fileId 文件ID
     * @param imageStream 图片流
     * @return 缩略图映射
     */
    @TraceLog(value = "生成缩略图", module = "image-processing", type = "GENERATE_THUMBNAILS")
    public Map<ThumbnailSize, String> generateThumbnails(String fileId, InputStream imageStream) {
        Map<ThumbnailSize, String> thumbnails = new HashMap<>();
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 读取原始图片信息
            BufferedImage originalImage = ImageIO.read(imageStream);
            if (originalImage == null) {
                log.warn("无法读取图片内容: fileId={}, traceId={}", fileId, traceId);
                return thumbnails;
            }

            // 为每种尺寸生成缩略图
            for (ThumbnailSize size : ThumbnailSize.values()) {
                try {
                    // 重置流到开始位置
                    imageStream.reset();

                    // 生成缩略图
                    byte[] thumbnailBytes = generateThumbnail(imageStream, size);

                    if (thumbnailBytes != null) {
                        // 构建缩略图文件名
                        String thumbnailId = generateThumbnailId(fileId, size);

                        // 这里应该将缩略图保存到存储适配器
                        // 暂时返回缩略图ID，实际实现中需要调用storageAdapter上传
                        String thumbnailPath = String.format("thumbnails/%s/%s", size.getName(), thumbnailId);
                        thumbnails.put(size, thumbnailPath);

                        log.debug("缩略图生成成功: fileId={}, size={}, traceId={}", fileId, size, traceId);
                    }

                } catch (Exception e) {
                    log.error("缩略图生成失败: fileId={}, size={}, error={}, traceId={}",
                            fileId, size, e.getMessage(), traceId, e);
                }
            }

            log.info("缩略图生成完成: fileId={}, count={}, traceId={}", fileId, thumbnails.size(), traceId);
            return thumbnails;

        } catch (Exception e) {
            log.error("缩略图生成失败: fileId={}, error={}, traceId={}", fileId, e.getMessage(), traceId, e);
            return thumbnails;
        }
    }

    /**
     * 生成单个缩略图
     *
     * @param imageStream 图片流
     * @param size 目标尺寸
     * @return 缩略图字节数组
     */
    private byte[] generateThumbnail(InputStream imageStream, ThumbnailSize size) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(imageStream)
                .size(size.getWidth(), size.getHeight())
                .outputQuality(size.getQuality())
                .keepAspectRatio(true)
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }

    /**
     * 生成缩略图ID
     *
     * @param fileId 原文件ID
     * @param size 缩略图尺寸
     * @return 缩略图ID
     */
    private String generateThumbnailId(String fileId, ThumbnailSize size) {
        return String.format("%s_%s", fileId, size.getName());
    }

    /**
     * 检查文件是否为支持的图片格式
     *
     * @param fileName 文件名
     * @return 是否为支持的图片格式
     */
    public boolean isSupportedImageFormat(String fileName) {
        if (fileName == null) {
            return false;
        }

        String extension = getFileExtension(fileName);
        if (extension == null) {
            return false;
        }

        for (String supportedFormat : SUPPORTED_FORMATS) {
            if (supportedFormat.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 文件扩展名（不包含点号）
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * 获取图片信息
     *
     * @param fileId 文件ID
     * @param familyId 家庭ID
     * @return 图片信息
     */
    @TraceLog(value = "获取图片信息", module = "image-processing", type = "GET_IMAGE_INFO")
    public ImageMetadata getImageInfo(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 这里应该从缓存或数据库中获取图片元数据
            // 暂时返回基本信息，实际实现中需要完善
            ImageMetadata metadata = ImageMetadata.builder()
                    .fileId(fileId)
                    .familyId(familyId)
                    .build();

            log.info("获取图片信息: fileId={}, familyId={}, traceId={}", fileId, familyId, traceId);
            return metadata;

        } catch (Exception e) {
            log.error("获取图片信息失败: fileId={}, familyId={}, error={}, traceId={}",
                    fileId, familyId, e.getMessage(), traceId, e);
            throw new RuntimeException("获取图片信息失败", e);
        }
    }
}