package com.haven.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户登录请求DTO
 * 修改为支持手机号登录的模式
 * 登录方式：手机号 + 密码 或 用户名 + 密码
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
public class LoginRequest {

    /**
     * 登录标识 - 支持手机号或用户名
     * 优先使用手机号登录，如果为空则使用用户名
     */
    private String username;

    /**
     * 手机号 - 推荐的登录方式
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 记住登录状态
     */
    private Boolean rememberMe = false;

    /**
     * 验证码 - 二步验证使用
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