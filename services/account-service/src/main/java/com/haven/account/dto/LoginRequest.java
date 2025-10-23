package com.haven.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求DTO
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名或邮箱不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 记住登录状态
     */
    private Boolean rememberMe = false;

    /**
     * 验证码
     */
    private String verifyCode;

    /**
     * 设备信息
     */
    private String deviceId;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 登录来源
     */
    private String source = "WEB";
}