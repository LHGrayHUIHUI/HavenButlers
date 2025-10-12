package com.haven.storage.processing;

import com.haven.base.utils.TraceIdUtil;

import com.haven.storage.file.FileMetadata;
import com.haven.storage.file.FileUploadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 异步处理触发器
 *
 * 负责触发文件上传后的异步处理任务
 * 包括缩略图生成、OCR识别、向量标签生成等
 *
 * @author HavenButler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncProcessingTrigger {

    private final ThumbnailGenerator thumbnailGenerator;
    private final OCREngine ocrEngine;
    private final VectorTagGenerator vectorTagGenerator;

    /**
     * 触发文件上传后的异步处理任务
     *
     * @param request 上传请求
     * @param metadata 文件元数据
     */
    public void triggerAsyncProcessing(FileUploadRequest request, FileMetadata metadata) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 1. 生成缩略图（如果是图片）
            if (shouldGenerateThumbnail(request, metadata)) {
                log.info("触发缩略图生成任务: fileId={}, traceId={}", metadata.getFileId(), traceId);
                thumbnailGenerator.generateThumbnailAsync(metadata, traceId);
            }

            // 2. OCR识别（如果是文档）
            if (shouldPerformOCR(request, metadata)) {
                log.info("触发OCR识别任务: fileId={}, traceId={}", metadata.getFileId(), traceId);
                ocrEngine.performOCRAsync(metadata, traceId);
            }

            // 3. 生成向量标签
            if (shouldGenerateVectorTags(request, metadata)) {
                log.info("触发向量标签生成任务: fileId={}, traceId={}", metadata.getFileId(), traceId);
                vectorTagGenerator.generateVectorTagsAsync(metadata, traceId);
            }

            log.info("异步处理任务触发完成: fileId={}, traceId={}", metadata.getFileId(), traceId);

        } catch (Exception e) {
            log.error("异步处理任务触发失败: fileId={}, traceId={}, error={}",
                    metadata.getFileId(), traceId, e.getMessage(), e);
        }
    }

    /**
     * 判断是否应该生成缩略图
     */
    private boolean shouldGenerateThumbnail(FileUploadRequest request, FileMetadata metadata) {
        // 检查是否为图片文件且用户要求生成缩略图
        return request.isImageFile() && Boolean.TRUE.equals(request.getGenerateThumbnail());
    }

    /**
     * 判断是否应该执行OCR识别
     */
    private boolean shouldPerformOCR(FileUploadRequest request, FileMetadata metadata) {
        // 检查是否为文档文件且用户要求OCR识别
        return request.isDocumentFile() && Boolean.TRUE.equals(request.getEnableOCR());
    }

    /**
     * 判断是否应该生成向量标签
     */
    private boolean shouldGenerateVectorTags(FileUploadRequest request, FileMetadata metadata) {
        // 对于所有支持的文件类型都生成向量标签
        return true; // 可以根据业务需求调整策略
    }

    /**
     * 触发单个处理任务
     *
     * @param fileId 文件ID
     * @param processingType 处理类型
     * @param traceId 链路追踪ID
     */
    public void triggerSingleProcessing(String fileId, String processingType, String traceId) {
        try {
            log.info("触发单个处理任务: fileId={}, processingType={}, traceId={}",
                    fileId, processingType, traceId);

            // TODO: 从数据库获取文件元数据
            // FileMetadata metadata = fileMetadataService.getFileMetadata(fileId);
            // 根据processingType触发相应的处理

        } catch (Exception e) {
            log.error("单个处理任务触发失败: fileId={}, processingType={}, traceId={}, error={}",
                    fileId, processingType, traceId, e.getMessage(), e);
        }
    }

    /**
     * 批量触发处理任务
     *
     * @param fileIds 文件ID列表
     * @param processingType 处理类型
     */
    public void triggerBatchProcessing(java.util.List<String> fileIds, String processingType) {
        if (fileIds == null || fileIds.isEmpty()) {
            log.warn("文件ID列表为空，跳过批量处理");
            return;
        }

        String traceId = TraceIdUtil.getCurrentOrGenerate();
        log.info("触发批量处理任务: fileCount={}, processingType={}, traceId={}",
                fileIds.size(), processingType, traceId);

        for (String fileId : fileIds) {
            triggerSingleProcessing(fileId, processingType, traceId);
        }
    }

    /**
     * 检查处理任务状态
     *
     * @param taskId 任务ID
     * @return 处理状态
     */
    public String checkProcessingStatus(String taskId) {
        // TODO: 实现任务状态检查逻辑
        log.debug("检查处理任务状态: taskId={}", taskId);
        return "processing"; // 简单实现，实际应该从任务管理器获取状态
    }

    /**
     * 取消处理任务
     *
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    public boolean cancelProcessingTask(String taskId) {
        log.info("取消处理任务: taskId={}", taskId);
        // TODO: 实现任务取消逻辑
        return true; // 简单实现
    }

    /**
     * 获取处理任务列表
     *
     * @param fileId 文件ID
     * @return 任务列表
     */
    public java.util.List<ProcessingTask> getProcessingTasks(String fileId) {
        // TODO: 实现任务列表获取逻辑
        log.debug("获取处理任务列表: fileId={}", fileId);
        return java.util.Collections.emptyList(); // 简单实现
    }

    /**
     * 处理任务信息类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ProcessingTask {
        private String taskId;
        private String fileId;
        private String taskType;
        private String status; // pending, processing, completed, failed
        private String errorMessage;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
    }
}