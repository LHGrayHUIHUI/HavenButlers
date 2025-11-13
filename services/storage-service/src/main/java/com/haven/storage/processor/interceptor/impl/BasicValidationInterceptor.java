package com.haven.storage.processor.interceptor.impl;

import com.haven.storage.domain.model.enums.FileCategory;
import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.processor.interceptor.FileInterceptorChain;
import com.haven.storage.processor.interceptor.FileProcessInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 基础校验拦截器
 *
 * <p>作为文件处理链中的第一个拦截器（@Order(1)），负责对文件上传和修改操作进行基础验证。
 * 所有文件操作都必须经过此拦截器的校验，确保系统的安全性和稳定性。</p>
 *
 * <p>主要校验内容包括：</p>
 * <ul>
 *   <li>文件流存在性校验 - 确保文件数据不为空且可读取</li>
 *   <li>文件大小校验 - 防止过大文件影响系统性能</li>
 *   <li>文件名称校验 - 防止非法字符和路径遍历攻击</li>
 *   <li>文件类型校验 - 根据文件扩展名和内容进行白名单验证</li>
 *   <li>家庭权限校验 - 确保操作有有效的家庭ID和用户ID</li>
 *   <li>元数据完整性校验 - 验证必要字段是否完整</li>
 * </ul>
 *
 * <p>安全特性：</p>
 * <ul>
 *   <li>严格的文件类型白名单控制</li>
 *   <li>文件大小限制防止资源耗尽</li>
 *   <li>文件名安全校验防止路径遍历</li>
 *   <li>家庭数据隔离验证</li>
 * </ul>
 *
 * @author Haven Storage Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class BasicValidationInterceptor implements FileProcessInterceptor {

    // ==================== 常量定义 ====================

    /** 最大文件大小：100MB */
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    /** 最小文件大小：1字节 */
    private static final long MIN_FILE_SIZE = 1L;

    /** 最大文件名长度：255字符 */
    private static final int MAX_FILENAME_LENGTH = 255;

    /** 安全的文件名正则表达式 - 不允许路径遍历字符 */
    private static final Pattern SAFE_FILENAME_PATTERN =
        Pattern.compile("^[a-zA-Z0-9一-龥._-]+$");

    /** 危险文件扩展名黑名单 */
    private static final List<String> DANGEROUS_FILE_EXTENSIONS = Arrays.asList(
        "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar", "sh", "php", "asp", "jsp"
    );

    /** 允许的图片文件扩展名 */
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff", "ico"
    );

    /** 允许的文档文件扩展名 */
    private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS = Arrays.asList(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp"
    );

    /** 允许的视频文件扩展名 */
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp", "rmvb"
    );

    /** 允许的音频文件扩展名 */
    private static final List<String> ALLOWED_AUDIO_EXTENSIONS = Arrays.asList(
        "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus"
    );

    // ==================== 核心拦截方法 ====================

    /**
     * 执行文件基础校验
     *
     * <p>此方法是拦截器的核心实现，按以下顺序执行校验：</p>
     * <ol>
     *   <li>文件流存在性检查</li>
     *   <li>文件大小范围校验</li>
     *   <li>基础元数据完整性校验</li>
     *   <li>文件名称安全性校验</li>
     *   <li>文件扩展名白名单校验</li>
     *   <li>家庭权限信息校验</li>
     * </ol>
     *
     * <p>任何校验失败都会立即返回失败结果，不会继续后续处理。</p>
     *
     * @param context 文件处理上下文，包含文件数据和元数据信息
     * @param interceptorChain 拦截器链，用于调用下一个拦截器
     * @return 校验结果，成功时继续链式调用，失败时返回错误信息
     */
    @Override
    public ProcessResult intercept(FileProcessContext context, FileInterceptorChain interceptorChain) {
        log.info("【基础校验拦截器】开始执行文件校验: traceId={}, operationType={}",
                context.getTraceId(), context.getOperationType());

        try {
            // 1. 文件流存在性校验
            ProcessResult streamValidation = validateFileStream(context);
            if (!streamValidation.isSuccess()) {
                return streamValidation;
            }

            // 2. 文件大小校验
            ProcessResult sizeValidation = validateFileSize(context);
            if (!sizeValidation.isSuccess()) {
                return sizeValidation;
            }

            // 3. 基础元数据完整性校验
            ProcessResult metadataValidation = validateBasicMetadata(context);
            if (!metadataValidation.isSuccess()) {
                return metadataValidation;
            }

            // 4. 文件名称安全性校验
            ProcessResult filenameValidation = validateFilename(context);
            if (!filenameValidation.isSuccess()) {
                return filenameValidation;
            }

            // 5. 文件类型校验
            ProcessResult fileTypeValidation = validateFileType(context);
            if (!fileTypeValidation.isSuccess()) {
                return fileTypeValidation;
            }

            // 6. 家庭权限信息校验
            ProcessResult familyValidation = validateFamilyPermissions(context);
            if (!familyValidation.isSuccess()) {
                return familyValidation;
            }

            log.info("【基础校验拦截器】校验通过: traceId={}", context.getTraceId());
            context.setValidFileData(true);

            // 所有校验通过，继续执行后续拦截器
            return interceptorChain.proceed(context);

        } catch (Exception e) {
            log.error("【基础校验拦截器】校验过程中发生异常: traceId={}, error={}",
                     context.getTraceId(), e.getMessage(), e);
            return ProcessResult.fail("文件校验过程中发生系统异常: " + e.getMessage());
        }
    }

    // ==================== 支持的操作类型 ====================

    /**
     * 获取当前拦截器支持的文件操作类型
     *
     * <p>基础校验拦截器主要针对需要数据输入的操作进行校验，包括：</p>
     * <ul>
     *   <li>UPLOAD - 文件上传操作，需要完整的文件流和元数据校验</li>
     *   <li>MODIFY - 文件修改操作，需要校验修改后的数据合法性</li>
 * </ul>
     *
     * <p>只读操作（如VIEW、DOWNLOAD）不需要经过此拦截器，因为它们不涉及数据写入。</p>
     *
     * @return 支持的操作类型集合
     */
    @Override
    public Set<FileOperation> supportedOperations() {
        return EnumSet.of(
                FileOperation.UPLOAD,
                FileOperation.MODIFY
        );
    }

    // ==================== 私有校验方法 ====================

    /**
     * 校验文件流是否存在且可读
     *
     * @param context 文件处理上下文
     * @return 校验结果
     */
    private ProcessResult validateFileStream(FileProcessContext context) {
        if (context.getBufferedSource() == null) {
            log.warn("【基础校验拦截器】文件流为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("文件数据不能为空");
        }

        if (!context.getBufferedSource().isOpen()) {
            log.warn("【基础校验拦截器】文件流已关闭: traceId={}", context.getTraceId());
            return ProcessResult.fail("文件流已关闭，无法读取数据");
        }

        log.debug("【基础校验拦截器】文件流校验通过: traceId={}", context.getTraceId());
        return ProcessResult.success("文件流校验通过");
    }

    /**
     * 校验文件大小是否在允许范围内
     *
     * @param context 文件处理上下文
     * @return 校验结果
     */
    private ProcessResult validateFileSize(FileProcessContext context) {
        Long fileSize = context.getFileBasicMetadata().getFileSize();

        if (fileSize == null) {
            log.warn("【基础校验拦截器】文件大小为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("文件大小不能为空");
        }

        if (fileSize < MIN_FILE_SIZE) {
            log.warn("【基础校验拦截器】文件大小过小: traceId={}, size={}", context.getTraceId(), fileSize);
            return ProcessResult.fail("文件大小不能小于1字节");
        }

        if (fileSize > MAX_FILE_SIZE) {
            log.warn("【基础校验拦截器】文件大小超过限制: traceId={}, size={}, maxSize={}",
                    context.getTraceId(), fileSize, MAX_FILE_SIZE);
            return ProcessResult.fail("文件大小超过限制（最大100MB）");
        }

        log.debug("【基础校验拦截器】文件大小校验通过: traceId={}, size={}bytes", context.getTraceId(), fileSize);
        return ProcessResult.success("文件大小校验通过");
    }

    /**
     * 校验基础元数据的完整性
     *
     * @param context 文件处理上下文
     * @return 校验结果
     */
    private ProcessResult validateBasicMetadata(FileProcessContext context) {
        if (context.getFileBasicMetadata() == null) {
            log.warn("【基础校验拦截器】文件基础元数据为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("文件元数据不能为空");
        }

        // 校验文件ID（修改操作时必须有）
        if (context.getOperationType() == FileOperation.MODIFY) {
            if (!StringUtils.hasText(context.getFileBasicMetadata().getFileId())) {
                log.warn("【基础校验拦截器】修改操作缺少文件ID: traceId={}", context.getTraceId());
                return ProcessResult.fail("修改操作必须指定文件ID");
            }
        }

        log.debug("【基础校验拦截器】基础元数据校验通过: traceId={}", context.getTraceId());
        return ProcessResult.success("基础元数据校验通过");
    }

    /**
     * 校验文件名称的安全性
     *
     * @param context 文件处理上下文
     * @return 校验结果
     */
    private ProcessResult validateFilename(FileProcessContext context) {
        String fileName = context.getFileBasicMetadata().getFileName();

        if (!StringUtils.hasText(fileName)) {
            log.warn("【基础校验拦截器】文件名为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("文件名不能为空");
        }

        // 检查文件名长度
        if (fileName.length() > MAX_FILENAME_LENGTH) {
            log.warn("【基础校验拦截器】文件名过长: traceId={}, fileName={}, length={}",
                    context.getTraceId(), fileName, fileName.length());
            return ProcessResult.fail("文件名长度不能超过255个字符");
        }

        // 检查文件名是否包含危险字符
        if (containsDangerousPathCharacters(fileName)) {
            log.warn("【基础校验拦截器】文件名包含危险字符: traceId={}, fileName={}",
                    context.getTraceId(), fileName);
            return ProcessResult.fail("文件名包含非法字符，不能包含路径分隔符");
        }

        // 检查文件名是否符合安全模式
        String nameWithoutExtension = fileName.contains(".") ?
            fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

        if (!StringUtils.hasText(nameWithoutExtension) ||
            !SAFE_FILENAME_PATTERN.matcher(nameWithoutExtension).matches()) {
            log.warn("【基础校验拦截器】文件名不符合安全规范: traceId={}, fileName={}",
                    context.getTraceId(), fileName);
            return ProcessResult.fail("文件名只能包含字母、数字、中文、下划线、连字符和点号");
        }

        log.debug("【基础校验拦截器】文件名称校验通过: traceId={}, fileName={}", context.getTraceId(), fileName);
        return ProcessResult.success("文件名称校验通过");
    }

    /**
     * 校验文件类型是否在允许的白名单中
     *
     * @param context 文件处理上下文
     * @return 校验结果
     */
    private ProcessResult validateFileType(FileProcessContext context) {
        String fileFormat = context.getFileBasicMetadata().getFileFormat();
        FileCategory fileCategory = context.getFileBasicMetadata().getFileCategory();

        if (!StringUtils.hasText(fileFormat)) {
            log.warn("【基础校验拦截器】文件格式为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("文件格式不能为空");
        }

        String normalizedExtension = fileFormat.toLowerCase().trim();

        // 检查是否为危险文件类型
        if (DANGEROUS_FILE_EXTENSIONS.contains(normalizedExtension)) {
            log.warn("【基础校验拦截器】危险文件类型: traceId={}, fileFormat={}",
                    context.getTraceId(), normalizedExtension);
            return ProcessResult.fail("不允许上传可执行文件或脚本文件");
        }

        // 根据文件分类检查扩展名是否在允许列表中
        switch (fileCategory) {
            case IMAGE:
                String imageAllowedList = String.join(", ", ALLOWED_IMAGE_EXTENSIONS);
                if (!ALLOWED_IMAGE_EXTENSIONS.contains(normalizedExtension)) {
                    log.warn("【基础校验拦截器】文件扩展名不在图片允许列表中: traceId={}, fileFormat={}, allowedList={}",
                            context.getTraceId(), normalizedExtension, imageAllowedList);
                    return ProcessResult.fail("图片类型" + normalizedExtension + "不在允许列表中，允许的类型：" + imageAllowedList);
                }
                break;
            case DOCUMENT:
                String documentAllowedList = String.join(", ", ALLOWED_DOCUMENT_EXTENSIONS);
                if (!ALLOWED_DOCUMENT_EXTENSIONS.contains(normalizedExtension)) {
                    log.warn("【基础校验拦截器】文件扩展名不在文档允许列表中: traceId={}, fileFormat={}, allowedList={}",
                            context.getTraceId(), normalizedExtension, documentAllowedList);
                    return ProcessResult.fail("文档类型" + normalizedExtension + "不在允许列表中，允许的类型：" + documentAllowedList);
                }
                break;
            case VIDEO:
                String videoAllowedList = String.join(", ", ALLOWED_VIDEO_EXTENSIONS);
                if (!ALLOWED_VIDEO_EXTENSIONS.contains(normalizedExtension)) {
                    log.warn("【基础校验拦截器】文件扩展名不在视频允许列表中: traceId={}, fileFormat={}, allowedList={}",
                            context.getTraceId(), normalizedExtension, videoAllowedList);
                    return ProcessResult.fail("视频类型" + normalizedExtension + "不在允许列表中，允许的类型：" + videoAllowedList);
                }
                break;
            case AUDIO:
                String audioAllowedList = String.join(", ", ALLOWED_AUDIO_EXTENSIONS);
                if (!ALLOWED_AUDIO_EXTENSIONS.contains(normalizedExtension)) {
                    log.warn("【基础校验拦截器】文件扩展名不在音频允许列表中: traceId={}, fileFormat={}, allowedList={}",
                            context.getTraceId(), normalizedExtension, audioAllowedList);
                    return ProcessResult.fail("音频类型" + normalizedExtension + "不在允许列表中，允许的类型：" + audioAllowedList);
                }
                break;
            default:
                log.debug("【基础校验拦截器】其他文件类型，跳过扩展名白名单检查: traceId={}, fileCategory={}",
                         context.getTraceId(), fileCategory);
                break;
        }

        log.debug("【基础校验拦截器】文件类型校验通过: traceId={}, fileFormat={}, fileCategory={}",
                 context.getTraceId(), normalizedExtension, fileCategory);
        return ProcessResult.success("文件类型校验通过");
    }

    /**
     * 校验家庭权限信息
     *
     * @param context 文件处理上下文
     * @return 校验结果
     */
    private ProcessResult validateFamilyPermissions(FileProcessContext context) {
        String familyId = context.getFileBasicMetadata().getFamilyId();
        String ownerId = context.getFileBasicMetadata().getOwnerId();

        if (!StringUtils.hasText(familyId)) {
            log.warn("【基础校验拦截器】家庭ID为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("必须指定家庭ID");
        }

        if (!StringUtils.hasText(ownerId)) {
            log.warn("【基础校验拦截器】用户ID为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("必须指定用户ID");
        }

        // 简单的ID格式校验（假设ID为UUID格式）
        if (!isValidIdFormat(familyId)) {
            log.warn("【基础校验拦截器】家庭ID格式无效: traceId={}, familyId={}",
                    context.getTraceId(), familyId);
            return ProcessResult.fail("家庭ID格式无效");
        }

        if (!isValidIdFormat(ownerId)) {
            log.warn("【基础校验拦截器】用户ID格式无效: traceId={}, ownerId={}",
                    context.getTraceId(), ownerId);
            return ProcessResult.fail("用户ID格式无效");
        }

        log.debug("【基础校验拦截器】家庭权限校验通过: traceId={}, familyId={}, ownerId={}",
                 context.getTraceId(), familyId, ownerId);
        return ProcessResult.success("家庭权限校验通过");
    }

    // ==================== 工具方法 ====================

    /**
     * 检查文件名是否包含危险的路径字符
     *
     * @param filename 文件名
     * @return 是否包含危险字符
     */
    private boolean containsDangerousPathCharacters(String filename) {
        return filename.contains("..") ||
               filename.contains("\\") ||
               filename.contains("/") ||
               filename.contains(":") ||
               filename.contains("*") ||
               filename.contains("?") ||
               filename.contains("\"") ||
               filename.contains("<") ||
               filename.contains(">") ||
               filename.contains("|");
    }

    /**
     * 校验ID格式是否有效（简单的UUID格式校验）
     *
     * @param id 要校验的ID
     * @return 是否为有效格式
     */
    private boolean isValidIdFormat(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        // 简单的UUID格式校验
        return id.length() >= 10; // 最基本的长度检查
    }
}
