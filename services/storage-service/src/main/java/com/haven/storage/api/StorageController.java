package com.haven.storage.api;

import com.haven.base.annotation.TraceLog;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.storage.database.*;
import com.haven.storage.file.*;
import com.haven.storage.knowledge.*;
import com.haven.storage.vectortag.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储服务统一API控制器
 *
 * 🎯 核心功能：
 * - 数据库连接管理
 * - 家庭文件存储
 * - 个人知识库构建
 * - 向量标签服务
 *
 * 💡 设计原则：
 * - RESTful API设计
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
public class StorageController {

    private final DatabaseConnectionService databaseConnectionService;
    private final FamilyFileStorageService fileStorageService;
    private final PersonalKnowledgeBaseService knowledgeBaseService;
    private final VectorTagService vectorTagService;

    // ===== 数据库连接管理 API =====

    /**
     * 获取项目数据库连接信息
     */
    @GetMapping("/database/connection/{projectId}")
    @TraceLog(value = "获取数据库连接", module = "storage-api", type = "DB_CONNECTION")
    public ResponseWrapper<DatabaseConnectionInfo> getDatabaseConnection(
            @PathVariable String projectId,
            @RequestParam String familyId) {

        DatabaseConnectionInfo connectionInfo = databaseConnectionService
                .getDatabaseConnection(projectId, familyId);

        return ResponseWrapper.success(connectionInfo);
    }

    /**
     * 创建新项目数据库
     */
    @PostMapping("/database/project")
    @TraceLog(value = "创建项目数据库", module = "storage-api", type = "CREATE_DB")
    public ResponseWrapper<DatabaseConnectionInfo> createProjectDatabase(
            @RequestBody CreateProjectDatabaseRequest request) {

        DatabaseConnectionInfo connectionInfo = databaseConnectionService
                .createProjectDatabase(request);

        return ResponseWrapper.success(connectionInfo);
    }

    /**
     * 获取家庭所有项目数据库
     */
    @GetMapping("/database/projects")
    @TraceLog(value = "获取项目列表", module = "storage-api", type = "LIST_PROJECTS")
    public ResponseEntity<List<DatabaseConnectionInfo>> getFamilyProjects(
            @RequestParam String familyId) {

        List<DatabaseConnectionInfo> projects = databaseConnectionService
                .getFamilyProjects(familyId);

        return ResponseEntity.ok(projects);
    }

    // ===== 家庭文件存储 API =====

    /**
     * 上传文件
     */
    @PostMapping("/files/upload")
    @TraceLog(value = "文件上传", module = "storage-api", type = "FILE_UPLOAD")
    public ResponseEntity<FileUploadResult> uploadFile(
            @RequestParam String familyId,
            @RequestParam(required = false, defaultValue = "/") String folderPath,
            @RequestParam MultipartFile file,
            @RequestParam String uploaderUserId) {

        FileUploadResult result = fileStorageService.uploadFile(
                familyId, folderPath, file, uploaderUserId);

        return ResponseEntity.ok(result);
    }

    /**
     * 下载文件
     */
    @GetMapping("/files/download/{fileId}")
    @TraceLog(value = "文件下载", module = "storage-api", type = "FILE_DOWNLOAD")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String fileId,
            @RequestParam String familyId) {

        FileDownloadResult result = fileStorageService.downloadFile(fileId, familyId);

        if (result.isSuccess()) {
            return ResponseEntity.ok()
                    .header("Content-Disposition",
                           "attachment; filename=\"" + result.getFileName() + "\"")
                    .header("Content-Type", result.getContentType())
                    .body(result.getFileContent());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取家庭文件列表
     */
    @GetMapping("/files/list")
    @TraceLog(value = "获取文件列表", module = "storage-api", type = "FILE_LIST")
    public ResponseEntity<FamilyFileList> getFamilyFiles(
            @RequestParam String familyId,
            @RequestParam(required = false, defaultValue = "/") String folderPath) {

        FamilyFileList fileList = fileStorageService.getFamilyFiles(familyId, folderPath);

        return ResponseEntity.ok(fileList);
    }

    /**
     * 搜索文件
     */
    @GetMapping("/files/search")
    @TraceLog(value = "文件搜索", module = "storage-api", type = "FILE_SEARCH")
    public ResponseEntity<FileSearchResult> searchFiles(
            @RequestParam String familyId,
            @RequestParam String keyword) {

        FileSearchResult result = fileStorageService.searchFiles(familyId, keyword);

        return ResponseEntity.ok(result);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/files/{fileId}")
    @TraceLog(value = "文件删除", module = "storage-api", type = "FILE_DELETE")
    public ResponseEntity<FileDeleteResult> deleteFile(
            @PathVariable String fileId,
            @RequestParam String familyId,
            @RequestParam String userId) {

        FileDeleteResult result = fileStorageService.deleteFile(fileId, familyId, userId);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取家庭存储统计
     */
    @GetMapping("/files/stats")
    @TraceLog(value = "获取存储统计", module = "storage-api", type = "STORAGE_STATS")
    public ResponseEntity<FamilyStorageStats> getStorageStats(
            @RequestParam String familyId) {

        FamilyStorageStats stats = fileStorageService.getFamilyStorageStats(familyId);

        return ResponseEntity.ok(stats);
    }

    // ===== 个人知识库 API =====

    /**
     * 创建知识库
     */
    @PostMapping("/knowledge/bases")
    @TraceLog(value = "创建知识库", module = "storage-api", type = "CREATE_KB")
    public ResponseEntity<KnowledgeBase> createKnowledgeBase(
            @RequestBody CreateKnowledgeBaseRequest request) {

        KnowledgeBase knowledgeBase = knowledgeBaseService.createKnowledgeBase(request);

        return ResponseEntity.ok(knowledgeBase);
    }

    /**
     * 向知识库添加文档
     */
    @PostMapping("/knowledge/bases/{knowledgeBaseId}/documents")
    @TraceLog(value = "添加知识库文档", module = "storage-api", type = "ADD_DOCUMENT")
    public ResponseEntity<AddDocumentResult> addDocument(
            @PathVariable String knowledgeBaseId,
            @RequestBody AddDocumentRequest request) {

        AddDocumentResult result = knowledgeBaseService.addDocument(knowledgeBaseId, request);

        return ResponseEntity.ok(result);
    }

    /**
     * 知识库搜索
     */
    @PostMapping("/knowledge/bases/{knowledgeBaseId}/search")
    @TraceLog(value = "知识库搜索", module = "storage-api", type = "SEARCH_KB")
    public ResponseEntity<KnowledgeSearchResult> searchKnowledge(
            @PathVariable String knowledgeBaseId,
            @RequestBody KnowledgeSearchRequest request) {

        KnowledgeSearchResult result = knowledgeBaseService
                .searchKnowledge(knowledgeBaseId, request);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取知识库列表
     */
    @GetMapping("/knowledge/bases")
    @TraceLog(value = "获取知识库列表", module = "storage-api", type = "LIST_KB")
    public ResponseEntity<List<KnowledgeBase>> getKnowledgeBases(
            @RequestParam String familyId,
            @RequestParam String userId) {

        List<KnowledgeBase> knowledgeBases = knowledgeBaseService
                .getKnowledgeBases(familyId, userId);

        return ResponseEntity.ok(knowledgeBases);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/knowledge/bases/{knowledgeBaseId}")
    @TraceLog(value = "删除知识库", module = "storage-api", type = "DELETE_KB")
    public ResponseEntity<Boolean> deleteKnowledgeBase(
            @PathVariable String knowledgeBaseId,
            @RequestParam String userId) {

        boolean deleted = knowledgeBaseService.deleteKnowledgeBase(knowledgeBaseId, userId);

        return ResponseEntity.ok(deleted);
    }

    /**
     * 获取知识库统计
     */
    @GetMapping("/knowledge/bases/{knowledgeBaseId}/stats")
    @TraceLog(value = "获取知识库统计", module = "storage-api", type = "KB_STATS")
    public ResponseEntity<KnowledgeBaseStats> getKnowledgeBaseStats(
            @PathVariable String knowledgeBaseId) {

        KnowledgeBaseStats stats = knowledgeBaseService
                .getKnowledgeBaseStats(knowledgeBaseId);

        return ResponseEntity.ok(stats);
    }

    // ===== 向量标签服务 API =====

    /**
     * 为文件生成向量标签
     */
    @PostMapping("/vector-tags/generate")
    @TraceLog(value = "生成向量标签", module = "storage-api", type = "GENERATE_TAGS")
    public ResponseEntity<VectorTagResult> generateVectorTags(
            @RequestBody GenerateVectorTagRequest request) {

        VectorTagResult result = vectorTagService.generateVectorTags(request);

        return ResponseEntity.ok(result);
    }

    /**
     * 基于向量相似度搜索文件
     */
    @PostMapping("/vector-tags/search")
    @TraceLog(value = "向量标签搜索", module = "storage-api", type = "VECTOR_SEARCH")
    public ResponseEntity<VectorSearchResult> searchByVectorTags(
            @RequestBody VectorSearchRequest request) {

        VectorSearchResult result = vectorTagService.searchByVectorTags(request);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取文件的向量标签
     */
    @GetMapping("/vector-tags/files/{fileId}")
    @TraceLog(value = "获取文件标签", module = "storage-api", type = "GET_FILE_TAGS")
    public ResponseEntity<List<VectorTag>> getFileVectorTags(
            @PathVariable String fileId,
            @RequestParam String familyId) {

        List<VectorTag> vectorTags = vectorTagService.getFileVectorTags(fileId, familyId);

        return ResponseEntity.ok(vectorTags);
    }

    /**
     * 获取家庭标签统计
     */
    @GetMapping("/vector-tags/stats")
    @TraceLog(value = "获取标签统计", module = "storage-api", type = "TAG_STATS")
    public ResponseEntity<FamilyTagStats> getFamilyTagStats(
            @RequestParam String familyId) {

        FamilyTagStats stats = vectorTagService.getFamilyTagStats(familyId);

        return ResponseEntity.ok(stats);
    }

    /**
     * 删除文件的向量标签
     */
    @DeleteMapping("/vector-tags/files/{fileId}")
    @TraceLog(value = "删除文件标签", module = "storage-api", type = "DELETE_FILE_TAGS")
    public ResponseEntity<Boolean> deleteFileVectorTags(
            @PathVariable String fileId,
            @RequestParam String familyId) {

        boolean deleted = vectorTagService.deleteFileVectorTags(fileId, familyId);

        return ResponseEntity.ok(deleted);
    }

    // ===== 健康检查和系统信息 API =====

    /**
     * 动态切换存储方式
     */
    @PostMapping("/files/switch-storage")
    @TraceLog(value = "切换存储方式", module = "storage-api", type = "SWITCH_STORAGE")
    public ResponseWrapper<String> switchStorage(@RequestBody Map<String, String> request) {
        String storageType = request.get("storageType");

        boolean success = fileStorageService.switchStorageAdapter(storageType);

        if (success) {
            return ResponseWrapper.success("存储方式切换成功：" + storageType);
        } else {
            return ResponseWrapper.error("存储方式切换失败");
        }
    }

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
    public ResponseWrapper<String> getFileAccessUrl(
            @PathVariable String fileId,
            @RequestParam String familyId,
            @RequestParam(defaultValue = "60") int expireMinutes) {

        String accessUrl = fileStorageService.getFileAccessUrl(fileId, familyId, expireMinutes);

        if (accessUrl != null) {
            return ResponseWrapper.success(accessUrl);
        } else {
            return ResponseWrapper.error("无法生成文件访问URL");
        }
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