package com.haven.base.aspect;

import com.haven.base.annotation.Permission;
import com.haven.base.common.exception.AuthException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 权限校验切面实现
 * 基于@Permission注解进行方法级权限控制
 *
 * 注意：这是一个预留实现，实际项目中需要集成具体的权限管理系统
 * 如Spring Security、Apache Shiro或自定义权限框架
 *
 * @author HavenButler
 */
@Slf4j
@Aspect
// 移除@Component注解，改由BaseModelAutoConfiguration中@Bean方式注册
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PermissionAspect {

    /**
     * 权限校验切面方法
     * 在方法执行前进行权限检查
     */
    @Around("@annotation(permission)")
    public Object checkPermission(ProceedingJoinPoint point, Permission permission) throws Throwable {
        String traceId = TraceIdUtil.getCurrentOrGenerate();
        String methodName = point.getSignature().getName();
        String className = point.getTarget().getClass().getSimpleName();

        try {
            log.debug("开始权限校验: class={}, method={}, permission={}, traceId={}",
                    className, methodName, permission.value(), traceId);

            // 检查权限
            boolean hasPermission = hasPermission(permission);

            if (!hasPermission) {
                String message = permission.message().isEmpty() ?
                    "权限不足，无法执行此操作" : permission.message();

                log.warn("权限校验失败: class={}, method={}, permission={}, traceId={}",
                        className, methodName, permission.value(), traceId);

                throw new AuthException(ErrorCode.PERMISSION_DENIED, message);
            }

            log.debug("权限校验通过: class={}, method={}, permission={}, traceId={}",
                    className, methodName, permission.value(), traceId);

            // 执行原方法
            return point.proceed();

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("权限校验异常: class={}, method={}, traceId={}, error={}",
                    className, methodName, traceId, e.getMessage(), e);

            // 权限系统异常时的策略：根据strictMode决定
            if (permission.strictMode()) {
                // 严格模式：权限系统异常时拒绝访问
                throw new AuthException(ErrorCode.SYSTEM_ERROR, "权限系统暂时不可用");
            } else {
                // 宽松模式：权限系统异常时放行，但记录警告
                log.warn("权限系统异常，宽松模式下放行访问: class={}, method={}, traceId={}",
                        className, methodName, traceId);
                return point.proceed();
            }
        }
    }

    /**
     * 权限检查逻辑
     * 这里提供默认实现，实际项目中应该对接具体的权限管理系统
     */
    private boolean hasPermission(Permission permission) {
        // TODO: 集成实际的权限管理系统
        // 示例实现逻辑：

        try {
            // 1. 获取当前用户信息
            String currentUser = getCurrentUser();
            if (currentUser == null) {
                log.debug("用户未登录，权限检查失败");
                return false;
            }

            // 2. 检查是否为超级管理员
            if (isSuperAdmin(currentUser)) {
                log.debug("超级管理员，权限检查通过");
                return true;
            }

            // 3. 检查具体权限
            boolean hasPermission = checkUserPermissions(currentUser, permission.value(), permission.logic());

            log.debug("权限检查结果: user={}, permission={}, result={}",
                    currentUser, java.util.Arrays.toString(permission.value()), hasPermission);

            return hasPermission;

        } catch (Exception e) {
            log.error("权限检查过程异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取当前用户
     * 实际实现中应该从Security Context或JWT Token中获取
     */
    private String getCurrentUser() {
        // TODO: 从Spring Security SecurityContext或JWT中获取当前用户
        // 这里提供示例实现

        try {
            // 示例：从ThreadLocal或请求头中获取用户信息
            // SecurityContext context = SecurityContextHolder.getContext();
            // Authentication auth = context.getAuthentication();
            // return auth != null ? auth.getName() : null;

            // 临时实现：返回模拟用户
            return "test_user"; // 开发环境返回测试用户

        } catch (Exception e) {
            log.debug("获取当前用户失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查是否为超级管理员
     */
    private boolean isSuperAdmin(String user) {
        // TODO: 实际实现中应该查询数据库或缓存
        return "admin".equals(user) || "superadmin".equals(user);
    }

    /**
     * 检查用户权限数组
     * 根据逻辑（AND/OR）判断权限
     */
    private boolean checkUserPermissions(String user, String[] permissions, com.haven.base.annotation.Permission.Logic logic) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }

        boolean result;
        if (logic == com.haven.base.annotation.Permission.Logic.AND) {
            // AND逻辑：所有权限都必须具备
            result = true;
            for (String permission : permissions) {
                if (!checkUserPermission(user, permission)) {
                    result = false;
                    break;
                }
            }
        } else {
            // OR逻辑：至少具备一个权限
            result = false;
            for (String permission : permissions) {
                if (checkUserPermission(user, permission)) {
                    result = true;
                    break;
                }
            }
        }

        log.debug("权限数组检查: user={}, permissions={}, logic={}, result={}",
                user, java.util.Arrays.toString(permissions), logic, result);
        return result;
    }

    /**
     * 检查用户是否具有指定权限
     * 实际实现中应该查询权限管理系统
     */
    private boolean checkUserPermission(String user, String permission) {
        // TODO: 实际实现逻辑
        // 1. 查询用户角色
        // 2. 查询角色权限
        // 3. 检查权限匹配

        // 临时实现：简单的权限模拟
        if ("test_user".equals(user)) {
            // 测试用户拥有基础权限
            return permission.startsWith("user:") || permission.equals("read");
        }

        // 默认拒绝
        return false;
    }

    /**
     * 权限评估器接口（预留扩展点）
     * 实际项目中可以实现此接口来集成具体的权限系统
     */
    public interface PermissionEvaluator {
        /**
         * 评估用户是否具有指定权限
         *
         * @param user       用户标识
         * @param permission 权限标识
         * @param target     目标对象（可选）
         * @return 是否具有权限
         */
        boolean hasPermission(String user, String permission, Object target);
    }
}