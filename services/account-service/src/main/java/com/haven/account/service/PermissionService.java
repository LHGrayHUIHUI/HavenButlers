package com.haven.account.service;

import com.haven.account.entity.FamilyMember;
import com.haven.account.entity.User;
import com.haven.account.enums.FamilyRole;
import com.haven.account.repository.FamilyMemberRepository;
import com.haven.account.repository.UserRepository;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 权限检查服务
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRepository userRepository;
    private final FamilyMemberRepository familyMemberRepository;

    /**
     * 检查用户是否为家庭管理员
     */
    public boolean isFamilyAdmin(Long userId, Long familyId) {
        try {
            return familyMemberRepository.isFamilyAdmin(familyId, userId);
        } catch (Exception e) {
            log.error("检查家庭管理员权限失败，用户ID: {}, 家庭ID: {}", userId, familyId, e);
            return false;
        }
    }

    /**
     * 检查用户是否为家庭成员
     */
    public boolean isFamilyMember(Long userId, Long familyId) {
        try {
            Optional<FamilyMember> member = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId);
            return member.isPresent() && FamilyMember.Status.ACTIVE.equals(member.get().getStatus());
        } catch (Exception e) {
            log.error("检查家庭成员权限失败，用户ID: {}, 家庭ID: {}", userId, familyId, e);
            return false;
        }
    }

    /**
     * 获取用户在家庭中的角色
     */
    public FamilyRole getUserFamilyRole(Long userId, Long familyId) {
        try {
            Optional<FamilyMember> member = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId);
            if (member.isPresent() && FamilyMember.Status.ACTIVE.equals(member.get().getStatus())) {
                return FamilyRole.fromCode(member.get().getRole());
            }
            return FamilyRole.GUEST;
        } catch (Exception e) {
            log.error("获取用户家庭角色失败，用户ID: {}, 家庭ID: {}", userId, familyId, e);
            return FamilyRole.GUEST;
        }
    }

    /**
     * 检查用户是否有足够权限级别执行操作
     */
    public boolean hasPermissionLevel(Long userId, Long familyId, int requiredLevel) {
        FamilyRole userRole = getUserFamilyRole(userId, familyId);
        return userRole.hasPermissionLevel(requiredLevel);
    }

    /**
     * 验证权限并抛出异常（如果没有权限）
     */
    public void checkPermission(Long userId, Long familyId, int requiredLevel, String operation) {
        if (!hasPermissionLevel(userId, familyId, requiredLevel)) {
            FamilyRole userRole = getUserFamilyRole(userId, familyId);
            log.warn("用户权限不足，用户ID: {}, 家庭ID: {}, 当前角色: {}, 操作: {}",
                    userId, familyId, userRole.getDescription(), operation);
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    String.format("权限不足，需要角色级别: %d，当前角色: %s", requiredLevel, userRole.getDescription()));
        }
    }

    /**
     * 检查用户是否可以邀请家庭成员（需要管理员权限）
     */
    public boolean canInviteMember(Long userId, Long familyId) {
        return isFamilyAdmin(userId, familyId);
    }

    /**
     * 检查用户是否可以移除家庭成员（需要管理员权限，且不能移除自己）
     */
    public boolean canRemoveMember(Long userId, Long familyId, Long targetUserId) {
        // 不能移除自己
        if (userId.equals(targetUserId)) {
            return false;
        }

        // 必须是管理员
        if (!isFamilyAdmin(userId, familyId)) {
            return false;
        }

        // 获取目标用户的角色，不能移除其他管理员（除非自己是创建者）
        FamilyRole targetRole = getUserFamilyRole(targetUserId, familyId);
        if (targetRole == FamilyRole.ADMIN) {
            // TODO: 检查是否为家庭创建者
            return false;
        }

        return true;
    }

    /**
     * 检查用户是否可以修改家庭信息（需要管理员权限）
     */
    public boolean canModifyFamily(Long userId, Long familyId) {
        return isFamilyAdmin(userId, familyId);
    }

    /**
     * 获取用户可访问的家庭列表
     */
    public java.util.List<Long> getUserAccessibleFamilies(Long userId) {
        try {
            java.util.List<FamilyMember> memberships = familyMemberRepository.findActiveMembershipsByUserId(userId);
            return memberships.stream()
                    .map(FamilyMember::getFamilyId)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户可访问家庭列表失败，用户ID: {}", userId, e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 检查用户是否可以访问指定家庭的数据
     */
    public boolean canAccessFamilyData(Long userId, Long familyId) {
        return isFamilyMember(userId, familyId);
    }
}