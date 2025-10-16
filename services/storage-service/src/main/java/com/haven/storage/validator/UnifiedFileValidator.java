package com.haven.storage.validator;

import com.haven.base.common.exception.AuthException;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.exception.SystemException;
import com.haven.base.common.exception.ValidationException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.file.FileVisibility;
import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.security.UserInfo;
import com.haven.storage.security.UserContext;
import com.haven.storage.utils.FileTypeDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
     * 统一验证用户身份和权限
     * 整合用户认证、身份一致性验证、家庭ID验证，提高验证效率和代码可维护性
     *
     * @param uploaderUserId 请求中的上传者用户ID
     * @param familyId 请求中的家庭ID（可选）
     * @param traceId 链路追踪ID
     * @return 已验证的用户信息
     * @throws AuthException 当认证失败或身份不一致时抛出
     * @throws ValidationException 当参数格式不正确时抛出
     */
    public UserInfo validateUserIdentityAndPermissions(String uploaderUserId, String familyId, String traceId) {
        try {
            // 1. 验证用户认证状态和获取当前用户信息
            if (!UserContext.isAuthenticated()) {
                throw new AuthException(ErrorCode.ACCOUNT_NOT_FOUND, "用户未认证，请重新登录");
            }

            String currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null || currentUserId.trim().isEmpty()) {
                throw new AuthException(ErrorCode.ACCOUNT_NOT_FOUND, "无法获取用户身份信息");
            }

            // 2. 验证请求中的上传者用户ID与当前认证用户是否一致
            if (uploaderUserId == null || !uploaderUserId.trim().equals(currentUserId.trim())) {
                log.warn("用户身份不一致 - 当前用户: {}, 请求上传者: {}, traceId={}",
                        currentUserId, uploaderUserId, traceId);
                throw new AuthException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "用户身份验证失败：请求中的上传者与当前认证用户不一致");
            }

            // 3. 验证家庭ID格式（如果提供）
            if (familyId != null && !familyId.trim().isEmpty()) {
                if (familyId.length() < 3 || familyId.length() > 50) {
                    throw new ValidationException("家庭ID格式不正确", "30003");
                }
            }

            return UserContext.getCurrentUserInfo().orElse(null);

        } catch (AuthException | ValidationException e) {
            throw e; // 重新抛出已知异常
        } catch (Exception e) {
            log.error("用户身份和权限验证失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * 验证用户认证状态（保留供其他场景使用）
     *
     * @throws AuthException 当用户未认证时抛出
     */
    public void validateUserAuthentication() {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

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
            // 1. 统一验证用户身份和权限（整合了原来的3个验证方法）
            UserInfo userInfo = validateUserIdentityAndPermissions(
                request.getUploaderUserId(), request.getFamilyId(), traceId);

            // 2. 验证文件分组（没有的话默认为用户ID私有）
            validateFileVisibility(request.getVisibility(), userInfo.userId(), traceId);

            // 3. 验证文件夹路径（可选）
            validateFolderPath(request.getFolderPath(), traceId);

            // 4. 文件相关验证（大小、类型等）
            validateUploadedFile(request.getFile(), traceId);

            log.info("文件上传请求验证通过: family={}, userId={}, file={}, visibility={}, traceId={}",
                    userInfo.familyId(), userInfo.userId(),
                    request.getOriginalFileName(), request.getVisibility(), traceId);

        } catch (ValidationException | AuthException e) {
            throw e; // 重新抛出已知异常
        } catch (Exception e) {
            log.error("文件上传请求验证失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.NETWORK_ERROR, e);
        }
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

            // 2. 验证家庭ID格式（如果提供）
            if (familyId != null && !familyId.trim().isEmpty()) {
                if (familyId.length() < 3 || familyId.length() > 50) {
                    log.warn("家庭ID格式不正确: familyId={}, traceId={}", familyId, traceId);
                    throw new ValidationException("家庭ID格式不正确", "30003");
                }
            }

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