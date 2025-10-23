package com.haven.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户数据传输对象
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
public class UserDTO {

    private Long id;
    private UUID uuid;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在6-128个字符之间")
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String avatarUrl;

    private String status;

    private Long currentFamilyId;

    private String roles;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 默认构造函数
     */
    public UserDTO() {
    }

    /**
     * 构造函数（用于注册）
     */
    public UserDTO(String username, String email, String password, String phone) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.status = "ACTIVE";
        this.roles = "USER";
    }

    /**
     * 转换为注册请求DTO
     */
    public RegisterRequest toRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setPhone(phone);
        return request;
    }

    /**
     * 转换为登录请求DTO
     */
    public LoginRequest toLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}