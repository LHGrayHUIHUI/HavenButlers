package com.haven.storage.validator;

import com.haven.base.common.exception.AuthException;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.exception.SystemException;
import com.haven.base.common.exception.ValidationException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.file.FileVisibility;
import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.security.UserContext;
import com.haven.storage.utils.FileTypeDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 统一文件验证器
 * <p>
 * 存储服务的统一文件验证解决方案，整合了所有文件相关的验证逻辑
 * 负责文件上传相关的参数验证和权限校验
 * 严格遵循base-model的异常处理规范
 * <p>
 * 功能特性：
 * - 统一的文件验证入口，消除重复代码
 * - 支持异常式和结果式两种验证模式
 * - 集成FileTypeDetector进行文件类型检测
 * - 完整的用户认证和权限校验
 * - 支持多种存储适配器的验证需求
 *
 * @author HavenButler
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class UnifiedFileValidator {


    private final FileTypeDetector fileTypeDetector;

    // 文件大小限制（100MB）
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024L;

    /**
     * 验证用户认证状态
     *
     * @throws AuthException 当用户未认证时抛出
     */
    public void validateUserAuthentication() {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            if (!UserContext.isAuthenticated()) {
                log.warn("用户未认证，拒绝访问: traceId={}", traceId);
                throw new AuthException(ErrorCode.ACCOUNT_NOT_FOUND, "用户未认证，请重新登录");
            }

            String currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null || currentUserId.trim().isEmpty()) {
                log.warn("无法获取用户身份信息: traceId={}", traceId);
                throw new AuthException(ErrorCode.ACCOUNT_NOT_FOUND, "无法获取用户身份信息");
            }

            log.debug("用户认证验证通过: userId={}, traceId={}", currentUserId, traceId);

        } catch (AuthException e) {
            throw e; // 重新抛出认证异常
        } catch (Exception e) {
            log.error("用户认证验证失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * 验证文件上传请求
     *
     * @param request 上传请求
     * @throws AuthException 当权限验证失败时抛出
     */
    public void validateUploadRequest(FileUploadRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 1. 验证用户认证状态
            validateUserAuthentication();

            // 2. 验证用户ID是否为空
            validateUserId(request.getUploaderUserId(), traceId);

            // 3. 验证家庭ID（可以为空）
            validateFamilyIdOptional(request.getFamilyId(), traceId);

            // 4. 验证文件分组（没有的话默认为用户ID私有）
            validateFileVisibility(request.getVisibility(), request.getUploaderUserId(), traceId);

            // 验证文件夹路径（可选）
            validateFolderPath(request.getFolderPath(), traceId);

            // 5. 文件相关验证（大小、类型等）
            validateUploadedFile(request.getFile(), traceId);

            log.info("文件上传请求验证通过: family={}, userId={}, file={}, visibility={}, traceId={}",
                    request.getFamilyId(), request.getUploaderUserId(),
                    request.getOriginalFileName(), request.getVisibility(), traceId);

        } catch (ValidationException | AuthException e) {
            throw e; // 重新抛出已知异常
        } catch (Exception e) {
            log.error("文件上传请求验证失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.NETWORK_ERROR, e);
        }
    }

    /**
     * 验证用户ID
     */
    private void validateUserId(String userId, String traceId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("用户ID不能为空: traceId={}", traceId);
            throw new ValidationException("用户ID不能为空", "30001");
        }

        // 验证用户ID格式（简单验证）
        if (userId.length() < 3 || userId.length() > 50) {
            log.warn("用户ID格式不正确: userId={}, traceId={}", userId, traceId);
            throw new ValidationException("用户ID格式不正确", "30002");
        }
    }

    /**
     * 验证家庭ID（可以为空）
     */
    private void validateFamilyIdOptional(String familyId, String traceId) {
        if (familyId != null && !familyId.trim().isEmpty()) {
            // 如果提供了家庭ID，则验证格式
            if (familyId.length() < 3 || familyId.length() > 50) {
                log.warn("家庭ID格式不正确: familyId={}, traceId={}", familyId, traceId);
                throw new ValidationException("家庭ID格式不正确", "30003");
            }
        }
        // 如果为空，则允许通过（用户私有文件）
    }

    /**
     * 验证上传的文件
     *
     * @param file 上传的文件
     * @param traceId 链路追踪ID
     * @throws ValidationException 当文件验证失败时抛出
     */
    private void validateUploadedFile(MultipartFile file, String traceId) {
        validateFileBasics(file, traceId);
        validateFileSize(file.getSize(), traceId);
        validateFileName(file.getOriginalFilename(), traceId);
        validateFileType(file.getOriginalFilename(), file.getContentType(), traceId);
        log.debug("文件验证通过: fileName={}, size={}, contentType={}, traceId={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType(), traceId);
    }

    /**
     * 验证文件基础信息
     */
    private void validateFileBasics(MultipartFile file, String traceId) {
        if (file == null || file.isEmpty()) {
            log.warn("文件不能为空: traceId={}", traceId);
            throw new ValidationException("文件不能为空", "30001");
        }
    }

    /**
     * 验证文件大小
     */
    private void validateFileSize(long fileSize, String traceId) {
        if (fileSize > MAX_FILE_SIZE) {
            log.warn("文件大小超过限制: size={}, maxSize={}, traceId={}",
                    fileSize, MAX_FILE_SIZE, traceId);
            throw new ValidationException("文件大小超过限制：" + (MAX_FILE_SIZE / 1024 / 1024) + "MB", "30002");
        }
    }

    /**
     * 验证文件名
     */
    private void validateFileName(String fileName, String traceId) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("文件名不能为空: traceId={}", traceId);
            throw new ValidationException("文件名不能为空", "30003");
        }
    }

    /**
     * 验证文件类型
     */
    private void validateFileType(String fileName, String contentType, String traceId) {
        // 基于文件扩展名验证
        if (!fileTypeDetector.isSupportedExtension(fileName)) {
            String extension = getFileExtension(fileName);
            log.warn("不支持的文件类型: extension={}, fileName={}, traceId={}",
                    extension, fileName, traceId);
            throw new ValidationException("不支持的文件类型：" + extension, "30004");
        }

        // 基于Content-Type验证（如果存在）
        if (contentType != null && !contentType.trim().isEmpty() &&
            !fileTypeDetector.isSupportedMimeType(contentType)) {
            log.warn("不支持的MIME类型: contentType={}, fileName={}, traceId={}",
                    contentType, fileName, traceId);
            throw new ValidationException("不支持的文件类型：" + contentType, "30005");
        }
    }

    /**
     * 验证文件可见性级别
     */
    private void validateFileVisibility(FileVisibility visibility, String userId, String traceId) {
        // 如果没有指定可见性级别，默认设为私有
        if (visibility == null) {
            log.info("未指定文件可见性级别，默认设置为私有: userId={}, traceId={}", userId, traceId);
            // 不抛出异常，在service层会设置为PRIVATE
            return;
        }

        // 验证可见性级别是否在有效范围内
        try {
            FileVisibility.valueOf(visibility.name());
        } catch (IllegalArgumentException e) {
            log.warn("无效的文件可见性级别: visibility={}, traceId={}", visibility, traceId);
            throw new ValidationException("无效的文件可见性级别: " + visibility, "30009");
        }
    }

    /**
     * 验证文件夹路径
     */
    private void validateFolderPath(String folderPath, String traceId) {
        if (folderPath != null && !folderPath.trim().isEmpty()) {
            String path = folderPath.trim();

            // 路径必须以 / 开头
            if (!path.startsWith("/")) {
                log.warn("文件夹路径必须以 / 开头: path={}, traceId={}", path, traceId);
                throw new ValidationException("文件夹路径必须以 / 开头", "30010");
            }

            // 检查非法字符
            String[] invalidChars = {"..", "\\", ":", "*", "?", "\"", "<", ">", "|"};
            for (String invalidChar : invalidChars) {
                if (path.contains(invalidChar)) {
                    log.warn("文件夹路径包含非法字符: path={}, invalidChar={}, traceId={}",
                            path, invalidChar, traceId);
                    throw new ValidationException("文件夹路径包含非法字符: " + invalidChar, "30011");
                }
            }

            // 检查路径长度
            if (path.length() > 255) {
                log.warn("文件夹路径过长: path={}, length={}, traceId={}", path, path.length(), traceId);
                throw new ValidationException("文件夹路径过长，最大255个字符", "30012");
            }
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) return null;
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    /**
     * 验证文件下载权限
     *
     * @param fileId   文件ID
     * @param familyId 家庭ID
     * @throws AuthException     当权限不足时抛出
     * @throws BusinessException 当文件不存在或无权限时抛出
     */
    public void validateDownloadPermission(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 1. 验证用户认证
            validateUserAuthentication();

            // 2. 验证家庭ID（可以为空）
            validateFamilyIdOptional(familyId, traceId);

            // 3. 验证文件ID
            if (fileId == null || fileId.trim().isEmpty()) {
                log.warn("文件ID不能为空: traceId={}", traceId);
                throw new ValidationException("文件ID不能为空", "30013");
            }

            // TODO: 这里可以添加更复杂的权限验证逻辑
            // 例如：查询数据库验证文件是否存在、用户是否有权限下载等

            log.debug("文件下载权限验证通过: fileId={}, familyId={}, traceId={}",
                    fileId, familyId, traceId);

        } catch (ValidationException | AuthException | BusinessException e) {
            throw e; // 重新抛出已知异常
        } catch (Exception e) {
            log.error("文件下载权限验证失败: fileId={}, familyId={}, traceId={}, error={}",
                    fileId, familyId, traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.DATABASE_ERROR, e);
        }
    }

    // ==================== 适配器层专用方法（返回ValidationResult） ====================

    /**
     * 验证文件上传（适配器层专用，返回ValidationResult）
     * 供LocalStorageAdapter和MinIOStorageAdapter使用
     *
     * @param familyId 家庭ID
     * @param file 上传的文件
     * @param maxFileSize 最大文件大小限制
     * @return 验证结果
     */
    public ValidationResult validateFileUpload(String familyId, MultipartFile file, long maxFileSize) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 1. 验证familyId
            if (familyId == null || familyId.trim().isEmpty()) {
                log.warn("适配器层文件验证失败：familyId为空，traceId={}", traceId);
                return ValidationResult.failure("参数错误：familyId不能为空");
            }

            // 2. 验证文件
            if (file == null || file.isEmpty()) {
                log.warn("适配器层文件验证失败：文件为空，familyId={}, traceId={}", familyId, traceId);
                return ValidationResult.failure("参数错误：文件不能为空");
            }

            // 3. 检查文件大小
            if (file.getSize() > maxFileSize) {
                log.warn("适配器层文件验证失败：文件大小超过限制，size={}, maxSize={}, familyId={}, traceId={}",
                        file.getSize(), maxFileSize, familyId, traceId);
                return ValidationResult.failure("文件大小超过限制：" + (maxFileSize / 1024 / 1024) + "MB");
            }

            // 4. 检查文件名
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.trim().isEmpty()) {
                log.warn("适配器层文件验证失败：文件名为空，familyId={}, traceId={}", familyId, traceId);
                return ValidationResult.failure("文件名不能为空");
            }

            // 5. 使用FileTypeDetector验证文件类型
            if (!fileTypeDetector.isSupportedExtension(fileName)) {
                String extension = getFileExtension(fileName);
                log.warn("适配器层文件验证失败：不支持的文件类型，extension={}, fileName={}, familyId={}, traceId={}",
                        extension, fileName, familyId, traceId);
                return ValidationResult.failure("不支持的文件类型：" + extension);
            }

            // 6. 如果有Content-Type，也进行验证
            String contentType = file.getContentType();
            if (contentType != null && !contentType.trim().isEmpty() &&
                !fileTypeDetector.isSupportedMimeType(contentType)) {
                log.warn("适配器层文件验证失败：不支持的MIME类型，contentType={}, fileName={}, familyId={}, traceId={}",
                        contentType, fileName, familyId, traceId);
                return ValidationResult.failure("不支持的文件类型：" + contentType);
            }

            log.debug("适配器层文件验证通过：fileName={}, size={}, contentType={}, familyId={}, traceId={}",
                    fileName, file.getSize(), contentType, familyId, traceId);

            return ValidationResult.success();

        } catch (Exception e) {
            log.error("适配器层文件验证异常：familyId={}, traceId={}, error={}", familyId, traceId, e.getMessage(), e);
            return ValidationResult.failure("文件验证过程中发生异常：" + e.getMessage());
        }
    }

    /**
         * 验证结果封装类
         * 用于适配器层，提供成功/失败结果而不抛出异常
         */
        public record ValidationResult(boolean valid, String errorMessage) {

        public static ValidationResult success() {
                return new ValidationResult(true, null);
            }

            public static ValidationResult failure(String errorMessage) {
                return new ValidationResult(false, errorMessage);
            }

            @NotNull
            @Override
            public String toString() {
                return valid ? "ValidationResult{valid=true}" :
                        "ValidationResult{valid=false, errorMessage='" + errorMessage + "'}";
            }
        }
}