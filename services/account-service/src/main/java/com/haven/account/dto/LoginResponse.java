package com.haven.account.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录响应DTO
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
public class LoginResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";

    /**
     * 访问令牌过期时间（毫秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserDTO userInfo;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 当前家庭ID
     */
    private Long currentFamilyId;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 设备信息
     */
    private String deviceId;

    /**
     * 构造函数
     */
    public LoginResponse(String accessToken, String refreshToken, UserDTO userInfo) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userInfo = userInfo;
        this.loginTime = LocalDateTime.now();
    }

    /**
     * 设置过期时间（小时）
     */
    public void setExpiresInHours(int hours) {
        this.expiresIn = hours * 60 * 60 * 1000L; // 转换为毫秒
    }
}