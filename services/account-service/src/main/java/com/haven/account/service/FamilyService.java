package com.haven.account.service;

import com.haven.account.dto.AccountFamily;
import com.haven.account.dto.AccountFamilyMember;
import com.haven.account.entity.Family;
import com.haven.account.entity.User;
import com.haven.account.enums.FamilyRole;
import com.haven.account.repository.FamilyRepository;
import com.haven.account.repository.FamilyMemberRepository;
import com.haven.account.repository.UserRepository;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 家庭管理服务
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    /**
     * 创建新家庭
     */
    @Transactional
    public AccountFamily createFamily(Long userId, AccountFamily accountFamily) {
        log.info("开始创建家庭，用户ID: {}, 家庭名称: {}", userId, accountFamily.getName());

        // 1. 验证用户存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在"));

        // 2. 检查用户是否已有活跃家庭（可选限制）
        List<Long> userFamilies = permissionService.getUserAccessibleFamilies(userId);
        if (userFamilies.size() >= 5) { // 限制每个用户最多创建5个家庭
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "家庭数量已达上限");
        }

        // 3. 创建家庭实体
        Family family = new Family();
        family.setUuid(UUID.randomUUID());
        family.setName(accountFamily.getName());
        family.setDescription(accountFamily.getDescription());
        family.setOwnerId(userId);
        family.setStatus("ACTIVE");
        family.setCreatedAt(LocalDateTime.now());
        family.setUpdatedAt(LocalDateTime.now());

        // 4. 保存家庭
        family = familyRepository.save(family);

        // 5. 添加用户为家庭管理员
        com.haven.account.entity.FamilyMember member = new com.haven.account.entity.FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(userId);
        member.setRole(FamilyRole.ADMIN.getCode());
        member.setStatus("ACTIVE");
        member.setJoinedAt(LocalDateTime.now());

        familyMemberRepository.save(member);

        // 6. 更新用户当前家庭ID
        user.setCurrentFamilyId(family.getId());
        userRepository.save(user);

        log.info("家庭创建成功，家庭ID: {}, 家庭名称: {}", family.getId(), family.getName());

        // 7. 转换为DTO返回
        return convertToDTO(family);
    }

    /**
     * 获取家庭信息
     */
    public AccountFamily getFamily(Long userId, Long familyId) {
        // 1. 检查访问权限
        if (!permissionService.canAccessFamilyData(userId, familyId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限访问该家庭");
        }

        // 2. 获取家庭信息
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "家庭不存在"));

        return convertToDTO(family);
    }

    /**
     * 获取用户的家庭列表
     */
    public List<AccountFamily> getUserFamilies(Long userId) {
        List<Long> familyIds = permissionService.getUserAccessibleFamilies(userId);

        if (familyIds.isEmpty()) {
            return List.of();
        }

        List<Family> families = familyRepository.findAllById(familyIds);
        return families.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新家庭信息
     */
    @Transactional
    public AccountFamily updateFamily(Long userId, Long familyId, AccountFamily accountFamily) {
        // 1. 检查修改权限
        if (!permissionService.canModifyFamily(userId, familyId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限修改家庭信息");
        }

        // 2. 获取家庭
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "家庭不存在"));

        // 3. 更新信息
        if (accountFamily.getName() != null && !accountFamily.getName().trim().isEmpty()) {
            family.setName(accountFamily.getName());
        }
        if (accountFamily.getDescription() != null) {
            family.setDescription(accountFamily.getDescription());
        }
        family.setUpdatedAt(LocalDateTime.now());

        // 4. 保存
        family = familyRepository.save(family);

        log.info("家庭信息更新成功，家庭ID: {}, 用户ID: {}", familyId, userId);

        return convertToDTO(family);
    }

    /**
     * 切换用户当前家庭
     */
    @Transactional
    public void switchCurrentFamily(Long userId, Long familyId) {
        // 1. 检查是否为家庭成员
        if (!permissionService.isFamilyMember(userId, familyId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "不是该家庭的成员");
        }

        // 2. 获取用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在"));

        // 3. 更新当前家庭
        user.setCurrentFamilyId(familyId);
        user.setUpdateTime(LocalDateTime.now());  // 使用 BaseEntity 的时间字段
        userRepository.save(user);

        log.info("用户切换当前家庭成功，用户ID: {}, 家庭ID: {}", userId, familyId);
    }

    /**
     * 获取家庭成员列表
     */
    public List<AccountFamilyMember> getFamilyMembers(Long userId, Long familyId) {
        // 1. 检查访问权限
        if (!permissionService.canAccessFamilyData(userId, familyId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限访问该家庭");
        }

        // 2. 获取家庭成员
        List<com.haven.account.entity.FamilyMember> members = familyMemberRepository.findByFamilyId(familyId);

        return members.stream()
                .filter(member -> com.haven.account.entity.FamilyMember.Status.ACTIVE.getValue().equals(member.getStatus()))
                .map(this::convertMemberToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为家庭DTO
     */
    private AccountFamily convertToDTO(Family family) {
        AccountFamily dto = new AccountFamily();
        dto.setId(family.getId());
        dto.setUuid(family.getUuid());
        dto.setName(family.getName());
        dto.setDescription(family.getDescription());
        dto.setOwnerId(family.getOwnerId());
        dto.setStatus(family.getStatus());
        dto.setCreatedAt(family.getCreatedAt());
        dto.setUpdatedAt(family.getUpdatedAt());
        return dto;
    }

    /**
     * 转换为家庭成员DTO
     */
    private AccountFamilyMember convertMemberToDTO(com.haven.account.entity.FamilyMember member) {
        // 获取用户信息
        Optional<User> user = userRepository.findById(member.getUserId());

        AccountFamilyMember dto = new AccountFamilyMember();
        dto.setId(member.getId());
        dto.setFamilyId(member.getFamilyId());
        dto.setUserId(member.getUserId());
        dto.setRole(member.getRole());
        dto.setStatus(member.getStatus());
        dto.setJoinedAt(member.getJoinedAt());

        if (user.isPresent()) {
            dto.setUsername(user.get().getUsername());
            dto.setEmail(user.get().getEmail());
            dto.setAvatarUrl(user.get().getAvatarUrl());
        }

        return dto;
    }
}