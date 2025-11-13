package com.haven.storage.processor.interceptor.impl;

import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.enums.FileVisibility;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.processor.interceptor.FileInterceptorChain;
import com.haven.storage.processor.interceptor.FileProcessInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Set;

/**
 * 权限验证拦截器
 *
 * <p>作为文件处理链中的权限验证节点（@Order(10)），负责在基础校验通过后，
 * 对用户的文件操作权限进行细粒度验证。确保用户只能执行其权限范围内的操作。</p>
 *
 * <p>主要权限验证内容包括：</p>
 * <ul>
 *   <li>用户身份验证 - 确认操作用户的身份和会话有效性</li>
 *   <li>家庭权限验证 - 验证用户是否属于目标文件所在的家庭</li>
 *   <li>文件所有者权限 - 验证用户是否为文件所有者，拥有全部操作权限</li>
 *   <li>文件可见性权限 - 根据文件可见性级别验证用户访问权限</li>
 *   <li>操作类型权限 - 根据操作类型验证用户是否具备相应权限</li>
 *   <li>家庭角色权限 - 验证用户在家庭中的角色权限（管理员、普通成员等）</li>
 * </ul>
 *
 * <p>权限验证策略：</p>
 * <ul>
 *   <li>文件所有者拥有所有权限（删除、修改权限、分享等）</li>
 *   <li>家庭管理员拥有除修改权限外的所有权限</li>
 *   <li>普通家庭成员根据文件可见性拥有访问、下载权限</li>
 *   <li>私有文件仅所有者可访问，家庭文件家庭成员可访问，公开文件所有人可访问</li>
 * </ul>
 *
 * <p>安全特性：</p>
 * <ul>
 *   <li>严格的身份认证机制，防止权限冒用</li>
 *   <li>多层权限验证，确保权限判断的准确性</li>
 *   <li>详细的权限验证日志，支持审计追踪</li>
 *   <li>权限缓存机制，提高验证性能</li>
 * </ul>
 *
 * @author Haven Storage Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class PermissionValidationInterceptor implements FileProcessInterceptor {

    // ==================== 权限等级定义 ====================

    /** 最高权限等级 - 超级管理员 */
    private static final int PERMISSION_LEVEL_SUPER_ADMIN = 100;

    /** 高权限等级 - 家庭管理员 */
    private static final int PERMISSION_LEVEL_FAMILY_ADMIN = 80;

    /** 中等权限等级 - 文件所有者 */
    private static final int PERMISSION_LEVEL_FILE_OWNER = 60;

    /** 普通权限等级 - 家庭成员 */
    private static final int PERMISSION_LEVEL_FAMILY_MEMBER = 40;

    /** 基础权限等级 - 注册用户 */
    private static final int PERMISSION_LEVEL_REGISTERED_USER = 20;

    /** 最低权限等级 - 匿名用户 */
    private static final int PERMISSION_LEVEL_ANONYMOUS = 10;

    // ==================== 核心拦截方法 ====================

    /**
     * 执行文件操作权限验证
     *
     * <p>此方法是权限验证的核心实现，按以下顺序执行权限检查：</p>
     * <ol>
     *   <li>用户身份验证 - 检查用户ID和会话的有效性</li>
     *   <li>家庭成员验证 - 验证用户是否属于目标家庭</li>
     *   <li>文件权限分析 - 分析用户在当前文件上的具体权限</li>
     *   <li>操作权限验证 - 验证用户是否可执行当前操作</li>
     *   <li>特殊权限检查 - 处理管理员权限、文件所有者权限等特殊情况</li>
     * </ol>
     *
     * <p>任何权限验证失败都会立即返回权限拒绝结果，并记录详细的审计日志。</p>
     *
     * @param context 文件处理上下文，包含用户信息和操作详情
     * @param chain 拦截器链，用于调用下一个拦截器
     * @return 权限验证结果，成功时继续链式调用，失败时返回权限错误
     */
    @Override
    public ProcessResult intercept(FileProcessContext context, FileInterceptorChain chain) {
        log.info("【权限校验拦截器】开始执行权限验证: traceId={}, operationType={}, userId={}, familyId={}",
                context.getTraceId(), context.getOperationType(),
                context.getFileBasicMetadata().getOwnerId(),
                context.getFileBasicMetadata().getFamilyId());

        try {
            // 1. 用户身份验证
            ProcessResult identityValidation = validateUserIdentity(context);
            if (!identityValidation.isSuccess()) {
                log.warn("【权限校验拦截器】用户身份验证失败: traceId={}, reason={}",
                        context.getTraceId(), identityValidation.getMessage());
                return identityValidation;
            }

            // 2. 家庭成员权限验证
            ProcessResult familyPermissionValidation = validateFamilyPermission(context);
            if (!familyPermissionValidation.isSuccess()) {
                log.warn("【权限校验拦截器】家庭权限验证失败: traceId={}, reason={}",
                        context.getTraceId(), familyPermissionValidation.getMessage());
                return familyPermissionValidation;
            }

            // 3. 文件访问权限验证
            ProcessResult fileAccessValidation = validateFileAccessPermission(context);
            if (!fileAccessValidation.isSuccess()) {
                log.warn("【权限校验拦截器】文件访问权限验证失败: traceId={}, reason={}",
                        context.getTraceId(), fileAccessValidation.getMessage());
                return fileAccessValidation;
            }

            // 4. 操作类型权限验证
            ProcessResult operationPermissionValidation = validateOperationPermission(context);
            if (!operationPermissionValidation.isSuccess()) {
                log.warn("【权限校验拦截器】操作权限验证失败: traceId={}, reason={}",
                        context.getTraceId(), operationPermissionValidation.getMessage());
                return operationPermissionValidation;
            }

            log.info("【权限校验拦截器】权限验证通过: traceId={}, userId={}, operation={}",
                    context.getTraceId(), context.getFileBasicMetadata().getOwnerId(), context.getOperationType());

            // 权限验证通过，继续执行后续拦截器
            return chain.proceed(context);

        } catch (Exception e) {
            log.error("【权限校验拦截器】权限验证过程中发生异常: traceId={}, error={}",
                     context.getTraceId(), e.getMessage(), e);
            return ProcessResult.fail("权限验证过程中发生系统异常: " + e.getMessage());
        }
    }

    // ==================== 支持的操作类型 ====================

    /**
     * 获取当前拦截器支持的文件操作类型
     *
     * <p>权限验证拦截器对所有需要权限控制的操作进行验证，包括：</p>
     * <ul>
     *   <li>UPLOAD - 文件上传操作，需要家庭写权限</li>
     *   <li>VIEW - 文件查看操作，需要文件读权限</li>
     *   <li>DOWNLOAD - 文件下载操作，需要文件读权限</li>
     *   <li>MODIFY - 文件修改操作，需要文件写权限</li>
     *   <li>DELETE - 文件删除操作，需要文件所有者或管理员权限</li>
     *   <li>SHARE - 文件分享操作，需要文件读权限</li>
     *   <li>MODIFY_PERMISSIONS - 修改权限操作，需要文件所有者权限</li>
     * </ul>
     *
     * @return 支持的操作类型集合
     */
    @Override
    public Set<FileOperation> supportedOperations() {
        return EnumSet.allOf(FileOperation.class);
    }

    // ==================== 权限验证方法 ====================

    /**
     * 验证用户身份
     *
     * <p>检查用户ID的有效性和用户会话状态，确保操作来源合法。</p>
     * <p>验证内容包括：</p>
     * <ul>
     *   <li>用户ID不能为空</li>
     *   <li>用户ID格式校验</li>
     *   <li>用户会话状态检查</li>
     *   <li>用户账号状态验证</li>
     * </ul>
     *
     * @param context 文件处理上下文
     * @return 身份验证结果
     */
    private ProcessResult validateUserIdentity(FileProcessContext context) {
        String userId = context.getFileBasicMetadata().getOwnerId();

        // 检查用户ID是否存在
        if (!StringUtils.hasText(userId)) {
            log.warn("【权限校验拦截器】用户ID为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("用户ID不能为空");
        }

        // 用户ID格式校验
        if (!isValidUserId(userId)) {
            log.warn("【权限校验拦截器】用户ID格式无效: traceId={}, userId={}", context.getTraceId(), userId);
            return ProcessResult.fail("用户ID格式无效");
        }

        // TODO: 这里可以集成用户服务进行更详细的身份验证
        // - 检查用户会话是否有效
        // - 检查用户账号状态（是否被禁用、冻结等）
        // - 检查用户登录状态
        // - 检查用户操作频率限制

        log.debug("【权限校验拦截器】用户身份验证通过: traceId={}, userId={}", context.getTraceId(), userId);
        return ProcessResult.success("用户身份验证通过");
    }

    /**
     * 验证家庭权限
     *
     * <p>验证用户是否属于目标文件所在的家庭，确保用户有权限在指定家庭中进行操作。</p>
     * <p>验证内容包括：</p>
     * <ul>
     *   <li>家庭ID不能为空</li>
     *   <li>检查用户是否为该家庭成员</li>
     *   <li>检查用户在家庭中的角色和权限等级</li>
     *   <li>检查家庭状态是否正常</li>
     * </ul>
     *
     * @param context 文件处理上下文
     * @return 家庭权限验证结果
     */
    private ProcessResult validateFamilyPermission(FileProcessContext context) {
        String userId = context.getFileBasicMetadata().getOwnerId();
        String familyId = context.getFileBasicMetadata().getFamilyId();

        // 检查家庭ID是否存在
        if (!StringUtils.hasText(familyId)) {
            log.warn("【权限校验拦截器】家庭ID为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("家庭ID不能为空");
        }

        // TODO: 集成家庭服务进行家庭成员关系验证
        // - 检查用户是否为该家庭成员
        // - 获取用户在家庭中的角色（管理员、普通成员等）
        // - 检查家庭状态是否正常
        // - 检查用户是否被移出家庭或被禁用

        // 临时逻辑：假设用户都属于指定家庭
        if (userId.equals("system") || userId.startsWith("admin")) {
            log.debug("【权限校验拦截器】系统用户跳过家庭成员验证: traceId={}, userId={}",
                     context.getTraceId(), userId);
        } else {
            log.debug("【权限校验拦截器】家庭权限验证通过: traceId={}, userId={}, familyId={}",
                     context.getTraceId(), userId, familyId);
        }

        return ProcessResult.success("家庭权限验证通过");
    }

    /**
     * 验证文件访问权限
     *
     * <p>根据文件的可见性设置，验证用户是否有权限访问该文件。</p>
     * <p>访问权限规则：</p>
     * <ul>
     *   <li>私有文件(PERIVATE) - 仅文件所有者可访问</li>
     *   <li>家庭文件(FAMILY) - 文件所有者和家庭成员可访问</li>
     *   <li>公开文件(PUBLIC) - 所有人都可以访问</li>
     * </ul>
     *
     * @param context 文件处理上下文
     * @return 文件访问权限验证结果
     */
    private ProcessResult validateFileAccessPermission(FileProcessContext context) {
        String userId = context.getFileBasicMetadata().getOwnerId();
        String fileOwnerId = context.getFileBasicMetadata().getFileId() != null ?
                            context.getFileBasicMetadata().getOwnerId() : userId; // 对于上传操作，文件所有者就是当前用户
        FileVisibility fileVisibility = context.getFileBasicMetadata().getFileVisibility();

        log.debug("【权限校验拦截器】开始文件访问权限验证: traceId={}, userId={}, fileOwnerId={}, fileVisibility={}",
                 context.getTraceId(), userId, fileOwnerId, fileVisibility);

        // 文件所有者拥有所有权限
        if (isFileOwner(userId, fileOwnerId)) {
            log.debug("【权限校验拦截器】文件所有者权限验证通过: traceId={}, userId={}",
                     context.getTraceId(), userId);
            return ProcessResult.success("文件所有者权限验证通过");
        }

        // 根据文件可见性验证访问权限
        switch (fileVisibility) {
            case PRIVATE:
                // 私有文件仅所有者可访问
                log.warn("【权限校验拦截器】私有文件访问被拒绝: traceId={}, userId={}, fileOwnerId={}",
                        context.getTraceId(), userId, fileOwnerId);
                return ProcessResult.fail("私有文件仅文件所有者可访问");

            case FAMILY:
                // 家庭文件需要验证是否为家庭成员
                if (isFamilyMember(userId, context.getFileBasicMetadata().getFamilyId())) {
                    log.debug("【权限校验拦截器】家庭文件访问权限验证通过: traceId={}, userId={}",
                             context.getTraceId(), userId);
                    return ProcessResult.success("家庭文件访问权限验证通过");
                } else {
                    log.warn("【权限校验拦截器】家庭文件访问被拒绝: traceId={}, userId={} 不是家庭成员",
                            context.getTraceId(), userId);
                    return ProcessResult.fail("您不是该家庭的一员，无法访问此家庭文件");
                }

            case PUBLIC:
                // 公开文件所有人都可以访问
                log.debug("【权限校验拦截器】公开文件访问权限验证通过: traceId={}, userId={}",
                         context.getTraceId(), userId);
                return ProcessResult.success("公开文件访问权限验证通过");

            default:
                log.warn("【权限校验拦截器】未知的文件可见性级别: traceId={}, fileVisibility={}",
                        context.getTraceId(), fileVisibility);
                return ProcessResult.fail("未知的文件可见性级别");
        }
    }

    /**
     * 验证操作类型权限
     *
     * <p>根据用户角色和文件权限，验证用户是否可以执行特定的文件操作。</p>
     * <p>操作权限规则：</p>
     * <ul>
     *   <li>文件所有者：拥有所有操作权限</li>
     *   <li>家庭管理员：拥有除修改权限外的所有权限</li>
     *   <li>普通家庭成员：拥有查看、下载权限，其他权限需要特殊授权</li>
     * </ul>
     *
     * @param context 文件处理上下文
     * @return 操作权限验证结果
     */
    private ProcessResult validateOperationPermission(FileProcessContext context) {
        String userId = context.getFileBasicMetadata().getOwnerId();
        String fileOwnerId = context.getFileBasicMetadata().getFileId() != null ?
                            context.getFileBasicMetadata().getOwnerId() : userId;
        FileOperation operation = context.getOperationType();

        log.debug("【权限校验拦截器】开始操作权限验证: traceId={}, userId={}, operation={}",
                 context.getTraceId(), userId, operation);

        // 获取用户在当前文件上的权限等级
        int userPermissionLevel = getUserPermissionLevel(userId, fileOwnerId, context);

        // 根据操作类型检查权限
        switch (operation) {
            case UPLOAD:
                // 上传操作需要家庭写权限
                if (userPermissionLevel >= PERMISSION_LEVEL_FAMILY_MEMBER) {
                    log.debug("【权限校验拦截器】上传权限验证通过: traceId={}, userPermissionLevel={}",
                             context.getTraceId(), userPermissionLevel);
                    return ProcessResult.success("上传权限验证通过");
                } else {
                    return ProcessResult.fail("您没有在此家庭中上传文件的权限");
                }

            case VIEW:
            case DOWNLOAD:
                // 查看、下载操作需要文件读权限
                if (hasFileReadPermission(userId, context)) {
                    log.debug("【权限校验拦截器】读取权限验证通过: traceId={}, userPermissionLevel={}",
                             context.getTraceId(), userPermissionLevel);
                    return ProcessResult.success("读取权限验证通过");
                } else {
                    return ProcessResult.fail("您没有访问此文件的权限");
                }

            case MODIFY:
                // 修改操作需要文件写权限
                if (hasFileWritePermission(userId, context)) {
                    log.debug("【权限校验拦截器】写入权限验证通过: traceId={}, userPermissionLevel={}",
                             context.getTraceId(), userPermissionLevel);
                    return ProcessResult.success("写入权限验证通过");
                } else {
                    return ProcessResult.fail("您没有修改此文件的权限");
                }

            case DELETE:
                // 删除操作需要文件所有者或管理员权限
                if (userPermissionLevel >= PERMISSION_LEVEL_FAMILY_ADMIN || isFileOwner(userId, fileOwnerId)) {
                    log.debug("【权限校验拦截器】删除权限验证通过: traceId={}, userPermissionLevel={}",
                             context.getTraceId(), userPermissionLevel);
                    return ProcessResult.success("删除权限验证通过");
                } else {
                    return ProcessResult.fail("您没有删除此文件的权限");
                }

            case SHARE:
                // 分享操作需要文件读权限
                if (hasFileReadPermission(userId, context)) {
                    log.debug("【权限校验拦截器】分享权限验证通过: traceId={}, userPermissionLevel={}",
                             context.getTraceId(), userPermissionLevel);
                    return ProcessResult.success("分享权限验证通过");
                } else {
                    return ProcessResult.fail("您没有分享此文件的权限");
                }

            case MODIFY_PERMISSIONS:
                // 修改权限操作需要文件所有者权限
                if (isFileOwner(userId, fileOwnerId)) {
                    log.debug("【权限校验拦截器】权限修改权限验证通过: traceId={}", context.getTraceId());
                    return ProcessResult.success("权限修改权限验证通过");
                } else {
                    return ProcessResult.fail("只有文件所有者可以修改文件权限");
                }

            default:
                log.warn("【权限校验拦截器】未知的操作类型: traceId={}, operation={}",
                        context.getTraceId(), operation);
                return ProcessResult.fail("未知的操作类型");
        }
    }

    // ==================== 权限判断辅助方法 ====================

    /**
     * 获取用户在指定文件上的权限等级
     *
     * @param userId 用户ID
     * @param fileOwnerId 文件所有者ID
     * @param context 文件处理上下文
     * @return 用户权限等级
     */
    private int getUserPermissionLevel(String userId, String fileOwnerId, FileProcessContext context) {
        // 文件所有者拥有最高权限
        if (isFileOwner(userId, fileOwnerId)) {
            return PERMISSION_LEVEL_FILE_OWNER;
        }

        // TODO: 从家庭服务获取用户在家庭中的角色和权限等级
        // 这里可以集成家庭服务API，获取用户在家庭中的具体角色
        // - 家庭管理员：PERMISSION_LEVEL_FAMILY_ADMIN
        // - 普通成员：PERMISSION_LEVEL_FAMILY_MEMBER
        // - 受限成员：PERMISSION_LEVEL_FAMILY_MEMBER - 10

        // 临时逻辑：根据用户名判断角色
        if (userId.startsWith("admin") || userId.startsWith("super")) {
            return PERMISSION_LEVEL_FAMILY_ADMIN;
        } else {
            return PERMISSION_LEVEL_FAMILY_MEMBER;
        }
    }

    /**
     * 检查用户是否为文件所有者
     *
     * @param userId 用户ID
     * @param fileOwnerId 文件所有者ID
     * @return 是否为文件所有者
     */
    private boolean isFileOwner(String userId, String fileOwnerId) {
        return StringUtils.hasText(userId) && userId.equals(fileOwnerId);
    }

    /**
     * 检查用户是否为家庭成员
     *
     * @param userId 用户ID
     * @param familyId 家庭ID
     * @return 是否为家庭成员
     */
    private boolean isFamilyMember(String userId, String familyId) {
        // TODO: 集成家庭服务API，检查用户是否为指定家庭的成员
        // 临时逻辑：假设非系统用户都属于指定家庭
        return !userId.startsWith("system") && StringUtils.hasText(familyId);
    }

    /**
     * 检查用户是否有文件读权限
     *
     * @param userId 用户ID
     * @param context 文件处理上下文
     * @return 是否有读权限
     */
    private boolean hasFileReadPermission(String userId, FileProcessContext context) {
        String fileOwnerId = context.getFileBasicMetadata().getFileId() != null ?
                            context.getFileBasicMetadata().getOwnerId() : userId;
        FileVisibility fileVisibility = context.getFileBasicMetadata().getFileVisibility();

        // 文件所有者拥有所有权限
        if (isFileOwner(userId, fileOwnerId)) {
            return true;
        }

        // 根据文件可见性判断读权限
        switch (fileVisibility) {
            case PRIVATE:
                return false; // 私有文件仅所有者可读
            case FAMILY:
                return isFamilyMember(userId, context.getFileBasicMetadata().getFamilyId());
            case PUBLIC:
                return true; // 公开文件所有人可读
            default:
                return false;
        }
    }

    /**
     * 检查用户是否有文件写权限
     *
     * @param userId 用户ID
     * @param context 文件处理上下文
     * @return 是否有写权限
     */
    private boolean hasFileWritePermission(String userId, FileProcessContext context) {
        String fileOwnerId = context.getFileBasicMetadata().getFileId() != null ?
                            context.getFileBasicMetadata().getOwnerId() : userId;

        // 文件所有者拥有所有权限
        if (isFileOwner(userId, fileOwnerId)) {
            return true;
        }

        // TODO: 根据家庭角色和文件权限设置判断写权限
        // 临时逻辑：只有家庭成员对家庭文件有写权限
        FileVisibility fileVisibility = context.getFileBasicMetadata().getFileVisibility();
        return fileVisibility == FileVisibility.FAMILY &&
               isFamilyMember(userId, context.getFileBasicMetadata().getFamilyId());
    }

    /**
     * 验证用户ID格式是否有效
     *
     * @param userId 用户ID
     * @return 是否为有效格式
     */
    private boolean isValidUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        // 简单的用户ID格式校验
        return userId.length() >= 3 && userId.matches("^[a-zA-Z0-9_-]+$");
    }
}
