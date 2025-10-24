package com.haven.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求DTO
 * 修改为以手机号为主要注册实体的模式
 * 登录方式：手机号 + 密码
 * 用户名：作为用户可选信息
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
public class RegisterRequest {

    /**
     * 手机号 - 主要注册标识，登录凭证
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 密码 - 登录凭证
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在6-128个字符之间")
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 用户名 - 可选的用户信息，不用于登录
     */
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\u4e00-\\u9fa5_\\u4e00-\\u9fa5a-zA-Z0-9_\\u4e00-\\u9fa5_]+$", message = "用户名只能包含中文、字母、数字和下划线")
    private String username;

    /**
     * 邮箱 - 可选的用户信息
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /**
     * 验证码 - 手机验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 8, message = "验证码长度必须在4-8个字符之间")
    private String verifyCode;

    /**
     * 注册来源
     */
    private String source = "WEB";

    /**
     * 设备信息
     */
    private String deviceId;

    /**
     * IP地址
     */
    private String ipAddress;
}