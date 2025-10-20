package com.haven.storage.controller;

import com.haven.base.annotation.TraceLog;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.api.StorageHealthInfo;
import com.haven.storage.async.AsyncProcessingTrigger;
import com.haven.storage.domain.model.file.*;
import com.haven.storage.domain.model.knowledge.*;
import com.haven.storage.domain.model.vectortag.*;
import com.haven.storage.service.FileStorageService;
import com.haven.storage.service.PersonalKnowledgeBaseService;
import com.haven.storage.service.VectorTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储服务统一API控制器
 * <p>
 * 🎯 核心功能：
 * - 家庭文件存储
 * - 个人知识库构建
 * - 向量标签服务
 * <p>
 * 💡 设计原则：
 * - Refile API设计
 * - 统一错误处理
 * - 请求参数验证
 * - 链路追踪支持
 *
 * @author HavenButler
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Validated
@Tag(name = "存储服务", description = "文件存储、知识库和向量标签服务")
public class StorageController {

    private final FileStorageService fileStorageService;
    private final PersonalKnowledgeBaseService knowledgeBaseService;
    private final VectorTagService vectorTagService;
    private final AsyncProcessingTrigger asyncProcessingTrigger;

    // ===== 家庭文件存储 API =====

    /**
     * 增强文件上传
     * <p>
     * 支持完整权限设置、元数据配置和异步处理
     */
    @PostMapping("/files/upload")
    @Operation(summary = "文件上传", description = "上传文件并设置权限和元数据")
    @TraceLog(value = "文件上传", module = "storage-api", type = "FILE_UPLOAD")
    public ResponseWrapper<FileMetadata> uploadFile(@Valid @ModelAttribute FileUploadRequest request) {
        // 1. 执行统一文件上传处理（包含验证、元数据构建、物理存储）
        FileMetadata fileMetadata = fileStorageService.completeFileUpload(request);

        // 2. 上传成功，触发异步后处理任务（缩略图生成、OCR识别等）
        asyncProcessingTrigger.triggerAsyncProcessing(request, fileMetadata);

        // 3. 返回成功响应（异常处理由GlobalExceptionHandler统一处理）
        return ResponseWrapper.success("文件上传成功", fileMetadata);
    }

    /**
     * 下载文件 - 支持流式传输
     * <p>
     * 采用流式传输，边读边写，不一次性加载全文件到内存
     * 通过HTTP响应头告知浏览器文件的名称、类型、大小等信息
     * 对于无法流式传输的文件，自动降级到字节数组传输
     * <p>
     * 注意：此方法不使用@TraceLog注解，因为InputStreamResource无法被序列化用于日志记录
     */
    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable @NotBlank(message = "fileId不能为空") String fileId,
                                                            @RequestParam(required = false) String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();
        MDC.put("traceId", traceId); // 放入MDC
        log.info("开始文件下载: fileId={}, familyId={}, traceId={}", fileId, familyId, traceId);
        try {
            // 1. 获取文件下载结果
            FileDownloadResult result = fileStorageService.downloadFile(fileId, familyId);

            if (!result.isSuccess()) {
                log.error("文件下载失败: fileId={}, familyId={}, error={}, traceId={}",
                        fileId, familyId, result.getErrorMessage(), traceId);
                return ResponseEntity.notFound().build();
            }
            // 2. 构建下载响应头
            HttpHeaders headers = buildDownloadHeaders(result.getFileMetadata());
            // 3. 返回流式响应
            String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            log.info("文件下载成功: fileId={}, fileName={}, traceId={}",
                    result.getFileMetadata().getFileId(), result.getFileMetadata().getOriginalFileName(), traceId);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(mediaType)
                    .body(new InputStreamResource(new BufferedInputStream(result.getInputStream())));

        } catch (Exception e) {
            log.error("文件下载异常: fileId={}, familyId={}, error={}, traceId={}",
                    fileId, familyId, e.getMessage(), traceId, e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取家庭文件列表
     */
    @GetMapping("/files/list")
    @TraceLog(value = "获取文件列表", module = "storage-api", type = "FILE_LIST")
    public ResponseWrapper<FamilyFileList> getFamilyFiles(@RequestParam String familyId, @RequestParam(required = false, defaultValue = "/") String folderPath) {
        FamilyFileList fileList = fileStorageService.getFamilyFiles(familyId, folderPath);
        return ResponseWrapper.success("获取文件列表", fileList);
    }

    /**
     * 搜索文件
     */
    @GetMapping("/files/search")
    @TraceLog(value = "文件搜索", module = "storage-api", type = "FILE_SEARCH")
    public ResponseEntity<FileSearchResult> searchFiles(@RequestParam String familyId, @RequestParam String keyword) {
        FileSearchResult result = fileStorageService.searchFiles(familyId, keyword);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/files/{fileId}")
    @TraceLog(value = "文件删除", module = "storage-api", type = "FILE_DELETE")
    public ResponseEntity<FileDeleteResult> deleteFile(@PathVariable String fileId, @RequestParam String familyId, @RequestParam String userId) {

        FileDeleteResult result = fileStorageService.deleteFile(fileId, familyId, userId);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取家庭存储统计
     */
    @GetMapping("/files/stats")
    @TraceLog(value = "获取存储统计", module = "storage-api", type = "STORAGE_STATS")
    public ResponseWrapper<FamilyStorageStats> getStorageStats(@RequestParam String familyId) {
        FamilyStorageStats stats = fileStorageService.getFamilyStorageStats(familyId);
        return ResponseWrapper.success(stats);
    }

    // ===== 个人知识库 API =====

    /**
     * 创建知识库
     */
    @PostMapping("/knowledge/bases")
    @TraceLog(value = "创建知识库", module = "storage-api", type = "CREATE_KB")
    public ResponseEntity<KnowledgeBase> createKnowledgeBase(@RequestBody CreateKnowledgeBaseRequest request) {

        KnowledgeBase knowledgeBase = knowledgeBaseService.createKnowledgeBase(request);

        return ResponseEntity.ok(knowledgeBase);
    }

    /**
     * 向知识库添加文档
     */
    @PostMapping("/knowledge/bases/{knowledgeBaseId}/documents")
    @TraceLog(value = "添加知识库文档", module = "storage-api", type = "ADD_DOCUMENT")
    public ResponseEntity<AddDocumentResult> addDocument(@PathVariable String knowledgeBaseId, @RequestBody AddDocumentRequest request) {

        AddDocumentResult result = knowledgeBaseService.addDocument(knowledgeBaseId, request);

        return ResponseEntity.ok(result);
    }

    /**
     * 知识库搜索
     */
    @PostMapping("/knowledge/bases/{knowledgeBaseId}/search")
    @TraceLog(value = "知识库搜索", module = "storage-api", type = "SEARCH_KB")
    public ResponseEntity<KnowledgeSearchResult> searchKnowledge(@PathVariable String knowledgeBaseId, @RequestBody KnowledgeSearchRequest request) {

        KnowledgeSearchResult result = knowledgeBaseService.searchKnowledge(knowledgeBaseId, request);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取知识库列表
     */
    @GetMapping("/knowledge/bases")
    @TraceLog(value = "获取知识库列表", module = "storage-api", type = "LIST_KB")
    public ResponseEntity<List<KnowledgeBase>> getKnowledgeBases(@RequestParam String familyId, @RequestParam String userId) {

        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getKnowledgeBases(familyId, userId);

        return ResponseEntity.ok(knowledgeBases);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/knowledge/bases/{knowledgeBaseId}")
    @TraceLog(value = "删除知识库", module = "storage-api", type = "DELETE_KB")
    public ResponseEntity<Boolean> deleteKnowledgeBase(@PathVariable String knowledgeBaseId, @RequestParam String userId) {

        boolean deleted = knowledgeBaseService.deleteKnowledgeBase(knowledgeBaseId, userId);

        return ResponseEntity.ok(deleted);
    }

    /**
     * 获取知识库统计
     */
    @GetMapping("/knowledge/bases/{knowledgeBaseId}/stats")
    @TraceLog(value = "获取知识库统计", module = "storage-api", type = "KB_STATS")
    public ResponseEntity<KnowledgeBaseStats> getKnowledgeBaseStats(@PathVariable String knowledgeBaseId) {

        KnowledgeBaseStats stats = knowledgeBaseService.getKnowledgeBaseStats(knowledgeBaseId);

        return ResponseEntity.ok(stats);
    }

    // ===== 向量标签服务 API =====

    /**
     * 为文件生成向量标签
     */
    @PostMapping("/vector-tags/generate")
    @TraceLog(value = "生成向量标签", module = "storage-api", type = "GENERATE_TAGS")
    public ResponseEntity<VectorTagResult> generateVectorTags(@RequestBody GenerateVectorTagRequest request) {

        VectorTagResult result = vectorTagService.generateVectorTags(request);

        return ResponseEntity.ok(result);
    }

    /**
     * 基于向量相似度搜索文件
     */
    @PostMapping("/vector-tags/search")
    @TraceLog(value = "向量标签搜索", module = "storage-api", type = "VECTOR_SEARCH")
    public ResponseEntity<VectorSearchResult> searchByVectorTags(@RequestBody VectorSearchRequest request) {

        VectorSearchResult result = vectorTagService.searchByVectorTags(request);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取文件的向量标签
     */
    @GetMapping("/vector-tags/files/{fileId}")
    @TraceLog(value = "获取文件标签", module = "storage-api", type = "GET_FILE_TAGS")
    public ResponseEntity<List<VectorTag>> getFileVectorTags(@PathVariable String fileId, @RequestParam String familyId) {

        List<VectorTag> vectorTags = vectorTagService.getFileVectorTags(fileId, familyId);

        return ResponseEntity.ok(vectorTags);
    }

    /**
     * 获取家庭标签统计
     */
    @GetMapping("/vector-tags/stats")
    @TraceLog(value = "获取标签统计", module = "storage-api", type = "TAG_STATS")
    public ResponseEntity<FamilyTagStats> getFamilyTagStats(@RequestParam String familyId) {

        FamilyTagStats stats = vectorTagService.getFamilyTagStats(familyId);

        return ResponseEntity.ok(stats);
    }

    /**
     * 删除文件的向量标签
     */
    @DeleteMapping("/vector-tags/files/{fileId}")
    @TraceLog(value = "删除文件标签", module = "storage-api", type = "DELETE_FILE_TAGS")
    public ResponseEntity<Boolean> deleteFileVectorTags(@PathVariable String fileId, @RequestParam String familyId) {

        boolean deleted = vectorTagService.deleteFileVectorTags(fileId, familyId);

        return ResponseEntity.ok(deleted);
    }

    // ===== 健康检查和系统信息 API =====


    /**
     * 获取存储适配器状态
     */
    @GetMapping("/files/storage-status")
    @TraceLog(value = "获取存储状态", module = "storage-api", type = "STORAGE_STATUS")
    public ResponseWrapper<Map<String, Object>> getStorageStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("currentStorageType", fileStorageService.getCurrentStorageType());
        status.put("isHealthy", fileStorageService.isStorageHealthy());
        status.put("supportedTypes", Arrays.asList("local", "minio", "cloud"));

        return ResponseWrapper.success(status);
    }

    /**
     * 获取文件访问URL
     */
    @GetMapping("/files/access-url/{fileId}")
    @TraceLog(value = "获取文件访问URL", module = "storage-api", type = "FILE_ACCESS_URL")
    public ResponseWrapper<String> getFileAccessUrl(@PathVariable String fileId, @RequestParam String familyId, @RequestParam(defaultValue = "60") int expireMinutes) {

        String accessUrl = fileStorageService.getFileAccessUrl(fileId, familyId, expireMinutes);

        if (accessUrl != null) {
            return ResponseWrapper.success(accessUrl);
        } else {
            return ResponseWrapper.error(40002, "无法生成文件访问URL", null);
        }
    }

    /**
     * 构建下载响应头
     */
    private HttpHeaders buildDownloadHeaders(FileMetadata metadata) {
        HttpHeaders headers = new HttpHeaders();

        // 1. 设置文件名（使用RFC 5987编码）
        String originalFileName = metadata.getOriginalFileName();
        if (originalFileName != null) {
            // URL编码文件名以支持中文和特殊字符
            String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8);
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + originalFileName + "\"; filename*=UTF-8''" + encodedFileName);
        }

        // 2. 设置内容类型
        String contentType = metadata.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = metadata.getMimeType();
        }
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);

        // 3. 设置文件大小
        if (metadata.getFileSize() > 0) {
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(metadata.getFileSize()));
        }

        // 4. 添加缓存控制
        headers.add(HttpHeaders.CACHE_CONTROL, "private, max-age=3600");

        return headers;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<StorageHealthInfo> healthCheck() {
        StorageHealthInfo health = new StorageHealthInfo();
        health.setStatus("UP");
        health.setServiceName("storage-service");
        health.setVersion("v1.0.0");
        health.setTimestamp(System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }


}