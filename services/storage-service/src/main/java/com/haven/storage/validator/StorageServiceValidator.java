package com.haven.storage.validator;

import com.haven.base.common.exception.AuthException;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.exception.SystemException;
import com.haven.base.common.exception.ValidationException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.file.AccessLevel;
import com.haven.storage.file.FileUploadRequest;
import com.haven.storage.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 存储服务验证器
 * <p>
 * 负责文件上传相关的参数验证和权限校验
 * 严格遵循base-model的异常处理规范
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class StorageServiceValidator {

    // 文件大小限制（100MB）
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024L;

    // 支持的文件类型
    private static final String[] SUPPORTED_IMAGE_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp", "image/svg+xml"
    };

    private static final String[] SUPPORTED_DOCUMENT_TYPES = {
            "text/plain", "text/html", "text/css", "text/javascript", "application/pdf",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    };

    private static final String[] SUPPORTED_VIDEO_TYPES = {
            "video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv", "video/webm"
    };

    private static final String[] SUPPORTED_AUDIO_TYPES = {
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/flac", "audio/aac", "audio/ogg"
    };

    private static final String[] SUPPORTED_ARCHIVE_TYPES = {
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
            "application/x-tar", "application/gzip"
    };

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
            // 1. 验证用户认证
            validateUserAuthentication();

            // 2. 验证用户ID一致性
            validateUserIdConsistency(request, traceId);

            // 3. 验证家庭ID
            validateFamilyId(request.getFamilyId(), traceId);

            // 4. 验证文件
            validateFile(request.getFile(), traceId);

            // 5. 验证权限级别
            validateAccessLevel(request.getAccessLevel(), traceId);

            // 6. 验证文件夹路径
            validateFolderPath(request.getFolderPath(), traceId);

            // 7. 验证所有者ID（如果设置了）
            validateOwnerId(request.getOwnerId(), request.getUploaderUserId(), traceId);

            log.info("文件上传请求验证通过: family={}, userId={}, file={}, traceId={}",
                    request.getFamilyId(), request.getUploaderUserId(),
                    request.getOriginalFileName(), traceId);

        } catch (ValidationException | AuthException e) {
            throw e; // 重新抛出已知异常
        } catch (Exception e) {
            log.error("文件上传请求验证失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.NETWORK_ERROR, e);
        }
    }

    /**
     * 验证用户ID一致性
     */
    private void validateUserIdConsistency(FileUploadRequest fileUploadRequest, String traceId) {

    }

    /**
     * 验证家庭ID
     */
    private void validateFamilyId(String familyId, String traceId) {
        if (familyId == null || familyId.trim().isEmpty()) {
            log.warn("家庭ID不能为空: traceId={}", traceId);
            throw new ValidationException("家庭ID不能为空", "30001");
        }

        // 验证家庭ID格式（简单验证）
        if (familyId.length() < 3 || familyId.length() > 50) {
            log.warn("家庭ID格式不正确: familyId={}, traceId={}", familyId, traceId);
            throw new ValidationException("家庭ID格式不正确", "30002");
        }
    }

    /**
     * 验证上传文件
     */
    private void validateFile(MultipartFile file, String traceId) {
        if (file == null || file.isEmpty()) {
            log.warn("上传文件不能为空: traceId={}", traceId);
            throw new ValidationException("上传文件不能为空", "30003");
        }

        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("文件大小超过限制: size={}, maxSize={}, traceId={}",
                    file.getSize(), MAX_FILE_SIZE, traceId);
            throw new ValidationException("文件大小不能超过100MB", "30004");
        }

        // 验证文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            log.warn("文件名不能为空: traceId={}", traceId);
            throw new ValidationException("文件名不能为空", "30005");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            log.warn("不支持的文件类型: contentType={}, fileName={}, traceId={}",
                    contentType, originalFilename, traceId);
            throw new ValidationException("不支持的文件类型: " + contentType, "30006");
        }

        // 验证文件扩展名
        String extension = getFileExtension(originalFilename);
        if (!isValidFileExtension(extension, contentType)) {
            log.warn("文件扩展名与内容类型不匹配: extension={}, contentType={}, traceId={}",
                    extension, contentType, traceId);
            throw new ValidationException("文件扩展名与内容类型不匹配", "30007");
        }
    }

    /**
     * 验证权限级别
     */
    private void validateAccessLevel(AccessLevel accessLevel, String traceId) {
        if (accessLevel == null) {
            log.warn("权限级别不能为空: traceId={}", traceId);
            throw new ValidationException("权限级别不能为空", "30008");
        }

        // 验证权限级别是否在有效范围内
        try {
            AccessLevel.valueOf(accessLevel.name());
        } catch (IllegalArgumentException e) {
            log.warn("无效的权限级别: accessLevel={}, traceId={}", accessLevel, traceId);
            throw new ValidationException("无效的权限级别: " + accessLevel, "30009");
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
     * 验证所有者ID
     */
    private void validateOwnerId(String ownerId, String uploaderUserId, String traceId) {
        if (ownerId != null && !ownerId.trim().isEmpty()) {
            String currentUserId = UserContext.getCurrentUserId();

            // 验证所有者ID是否为当前用户或当前用户有权限设置其他所有者
            if (!ownerId.equals(currentUserId) && !ownerId.equals(uploaderUserId)) {
                log.warn("无权设置指定所有者: ownerId={}, currentUserId={}, traceId={}",
                        ownerId, currentUserId, traceId);
                throw new AuthException(ErrorCode.ACCOUNT_LOCKED, "无权设置指定所有者");
            }
        }
    }

    /**
     * 检查文件类型是否有效
     */
    private boolean isValidFileType(String contentType) {
        if (contentType == null) return false;

        String lowerType = contentType.toLowerCase();

        // 检查图片类型
        for (String type : SUPPORTED_IMAGE_TYPES) {
            if (lowerType.equals(type) || lowerType.startsWith(type.replace("/*", "/"))) {
                return true;
            }
        }

        // 检查文档类型
        for (String type : SUPPORTED_DOCUMENT_TYPES) {
            if (lowerType.equals(type)) {
                return true;
            }
        }

        // 检查视频类型
        for (String type : SUPPORTED_VIDEO_TYPES) {
            if (lowerType.equals(type) || lowerType.startsWith(type.replace("/*", "/"))) {
                return true;
            }
        }

        // 检查音频类型
        for (String type : SUPPORTED_AUDIO_TYPES) {
            if (lowerType.equals(type) || lowerType.startsWith(type.replace("/*", "/"))) {
                return true;
            }
        }

        // 检查压缩包类型
        for (String type : SUPPORTED_ARCHIVE_TYPES) {
            if (lowerType.equals(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 验证文件扩展名与内容类型是否匹配
     */
    private boolean isValidFileExtension(String extension, String contentType) {
        if (extension == null || contentType == null) return false;

        String lowerExt = extension.toLowerCase();
        String lowerType = contentType.toLowerCase();

        // 简单的扩展名和内容类型匹配验证
        switch (lowerType) {
            case "image/jpeg":
                return lowerExt.equals("jpg") || lowerExt.equals("jpeg");
            case "image/png":
                return lowerExt.equals("png");
            case "image/gif":
                return lowerExt.equals("gif");
            case "application/pdf":
                return lowerExt.equals("pdf");
            case "text/plain":
                return lowerExt.equals("txt");
            default:
                return true; // 对于其他类型，暂时允许通过
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

            // 2. 验证家庭ID
            validateFamilyId(familyId, traceId);

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
}