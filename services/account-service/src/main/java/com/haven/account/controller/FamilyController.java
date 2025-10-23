package com.haven.account.controller;

import com.haven.account.dto.FamilyDTO;
import com.haven.account.dto.FamilyMemberDTO;
import com.haven.account.security.JwtTokenService;
import com.haven.account.service.FamilyService;
import com.haven.base.annotation.TraceLog;
import com.haven.base.annotation.RateLimit;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 家庭管理控制器
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final JwtTokenService jwtTokenService;

    /**
     * 创建家庭
     */
    @PostMapping
    @TraceLog(value = "创建家庭", module = "家庭管理")
    @RateLimit(limit = 3, window = 300) // 5分钟内最多创建3个家庭
    public ResponseWrapper<FamilyDTO> createFamily(@RequestHeader("Authorization") String token,
                                                   @Valid @RequestBody FamilyDTO familyDTO) {
        try {
            Long userId = getUserIdFromToken(token);
            FamilyDTO createdFamily = familyService.createFamily(userId, familyDTO);
            return ResponseWrapper.success("家庭创建成功", createdFamily);
        } catch (BusinessException e) {
            log.warn("创建家庭失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建家庭异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建家庭失败，请稍后重试");
        }
    }

    /**
     * 获取家庭信息
     */
    @GetMapping("/{familyId}")
    @TraceLog(value = "获取家庭信息", module = "家庭管理")
    public ResponseWrapper<FamilyDTO> getFamily(@RequestHeader("Authorization") String token,
                                               @PathVariable Long familyId) {
        try {
            Long userId = getUserIdFromToken(token);
            FamilyDTO family = familyService.getFamily(userId, familyId);
            return ResponseWrapper.success(family);
        } catch (BusinessException e) {
            log.warn("获取家庭信息失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取家庭信息异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取家庭信息失败");
        }
    }

    /**
     * 更新家庭信息
     */
    @PutMapping("/{familyId}")
    @TraceLog(value = "更新家庭信息", module = "家庭管理")
    @RateLimit(limit = 10, window = 60) // 1分钟内最多10次更新
    public ResponseWrapper<FamilyDTO> updateFamily(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long familyId,
                                                   @Valid @RequestBody FamilyDTO familyDTO) {
        try {
            Long userId = getUserIdFromToken(token);
            FamilyDTO updatedFamily = familyService.updateFamily(userId, familyId, familyDTO);
            return ResponseWrapper.success("家庭信息更新成功", updatedFamily);
        } catch (BusinessException e) {
            log.warn("更新家庭信息失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("更新家庭信息异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新家庭信息失败");
        }
    }

    /**
     * 获取用户家庭列表
     */
    @GetMapping
    @TraceLog(value = "获取用户家庭列表", module = "家庭管理")
    public ResponseWrapper<List<FamilyDTO>> getUserFamilies(@RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            List<FamilyDTO> families = familyService.getUserFamilies(userId);
            return ResponseWrapper.success(families);
        } catch (Exception e) {
            log.error("获取用户家庭列表异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取家庭列表失败");
        }
    }

    /**
     * 切换当前家庭
     */
    @PostMapping("/{familyId}/switch")
    @TraceLog(value = "切换当前家庭", module = "家庭管理")
    @RateLimit(limit = 5, window = 60) // 1分钟内最多切换5次
    public ResponseWrapper<Void> switchCurrentFamily(@RequestHeader("Authorization") String token,
                                                     @PathVariable Long familyId) {
        try {
            Long userId = getUserIdFromToken(token);
            familyService.switchCurrentFamily(userId, familyId);
            return ResponseWrapper.success("切换家庭成功", null);
        } catch (BusinessException e) {
            log.warn("切换家庭失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("切换家庭异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "切换家庭失败");
        }
    }

    /**
     * 获取家庭成员列表
     */
    @GetMapping("/{familyId}/members")
    @TraceLog(value = "获取家庭成员列表", module = "家庭管理")
    public ResponseWrapper<List<FamilyMemberDTO>> getFamilyMembers(@RequestHeader("Authorization") String token,
                                                                    @PathVariable Long familyId) {
        try {
            Long userId = getUserIdFromToken(token);
            List<FamilyMemberDTO> members = familyService.getFamilyMembers(userId, familyId);
            return ResponseWrapper.success(members);
        } catch (BusinessException e) {
            log.warn("获取家庭成员列表失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取家庭成员列表异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取家庭成员列表失败");
        }
    }

    /**
     * 从Token中提取用户ID
     */
    private Long getUserIdFromToken(String token) {
        try {
            String actualToken = token.replace("Bearer ", "");
            return jwtTokenService.getUserIdFromToken(actualToken);
        } catch (Exception e) {
            log.error("从Token中提取用户ID失败", e);
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "无效的访问令牌");
        }
    }
}