package com.haven.account.controller;

import com.haven.account.model.dto.LoginRequest;
import com.haven.account.model.dto.LoginResponse;
import com.haven.account.model.dto.RegisterRequest;
import com.haven.account.model.dto.UserInfoDTO;
import com.haven.account.service.UserService;
import com.haven.account.security.JwtTokenService;
import com.haven.base.annotation.TraceLog;
import com.haven.base.annotation.RateLimit;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @TraceLog(value = "用户注册", module = "认证")
    @RateLimit(limit = 5, window = 300) // 5分钟内最多5次注册请求
    public ResponseWrapper<UserInfoDTO> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserInfoDTO userDTO = userService.register(request);
            return ResponseWrapper.success("注册成功", userDTO);
        } catch (BusinessException e) {
            log.warn("用户注册失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("用户注册异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @TraceLog(value = "用户登录", module = "认证")
    @RateLimit(limit = 10, window = 60) // 1分钟内最多10次登录请求
    public ResponseWrapper<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return ResponseWrapper.success("登录成功", response);
        } catch (BusinessException e) {
            log.warn("用户登录失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("用户登录异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，请稍后重试");
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @TraceLog(value = "用户登出", module = "认证")
    public ResponseWrapper<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // TODO: 实现Token黑名单机制
            log.info("用户登出成功");
            return ResponseWrapper.success("登出成功", null);
        } catch (Exception e) {
            log.error("用户登出异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登出失败");
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/profile")
    @TraceLog(value = "获取用户信息", module = "认证")
    public ResponseWrapper<UserInfoDTO> getProfile(@RequestHeader("Authorization") String token) {
        try {
            // 移除Bearer前缀
            String actualToken = token.replace("Bearer ", "");

            // 验证Token有效性
            if (!jwtTokenService.validateToken(actualToken)) {
                log.warn("Token验证失败");
                throw new BusinessException(ErrorCode.TOKEN_INVALID, "无效的访问令牌");
            }

            // 从Token中解析用户ID
            Long userId = jwtTokenService.getUserIdFromToken(actualToken);
            if (userId == null) {
                log.warn("无法从Token中解析用户ID");
                throw new BusinessException(ErrorCode.TOKEN_INVALID, "无法从令牌中解析用户ID");
            }

            UserInfoDTO userDTO = userService.getUserById(userId);

            return ResponseWrapper.success("获取用户信息成功", userDTO);
        } catch (BusinessException e) {
            log.warn("获取用户信息失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取用户信息异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取用户信息失败");
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @TraceLog(value = "刷新Token", module = "认证")
    public ResponseWrapper<LoginResponse> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        try {
            // 移除Bearer前缀
            String actualToken = refreshToken.replace("Bearer ", "");

            // 验证Token有效性
            if (!jwtTokenService.validateToken(actualToken)) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID, "无效的刷新令牌");
            }

            // 生成新的访问令牌
            String newAccessToken = jwtTokenService.refreshAccessToken(actualToken);

            // 获取用户信息
            Long userId = jwtTokenService.getUserIdFromToken(actualToken);
            UserInfoDTO userDTO = userService.getUserById(userId);

            // 创建响应
            LoginResponse response = new LoginResponse(newAccessToken, actualToken, userDTO);
            response.setExpiresInHours(24);

            log.info("Token刷新成功，用户ID: {}", userId);
            return ResponseWrapper.success("Token刷新成功", response);
        } catch (Exception e) {
            log.error("Token刷新异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Token刷新失败");
        }
    }

    /**
     * 验证Token
     */
    @GetMapping("/validate")
    @TraceLog(value = "验证Token", module = "认证")
    public ResponseWrapper<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        try {
            // 移除Bearer前缀
            String actualToken = token.replace("Bearer ", "");

            // 验证Token
            boolean isValid = jwtTokenService.validateToken(actualToken);

            if (isValid) {
                log.info("Token验证成功");
                return ResponseWrapper.success("Token验证成功", true);
            } else {
                return ResponseWrapper.success("Token验证失败", false);
            }
        } catch (Exception e) {
            log.error("Token验证异常", e);
            return ResponseWrapper.success("Token验证失败", false);
        }
    }
}