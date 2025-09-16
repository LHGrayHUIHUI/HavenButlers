package com.haven.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据传输对象
 * 在服务间传递用户信息的通用模型
 *
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号（脱敏）
     */
    private String phone;

    /**
     * 邮箱（脱敏）
     */
    private String email;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 用户状态（ACTIVE/INACTIVE/LOCKED）
     */
    private String status;

    /**
     * 用户类型（NORMAL/VIP/ADMIN）
     */
    private String userType;

    /**
     * 性别（M/F/U）
     */
    private String gender;

    /**
     * 出生日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime birthDate;

    /**
     * 所属家庭ID列表
     */
    private List<String> familyIds;

    /**
     * 当前家庭ID
     */
    private String currentFamilyId;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 用户偏好设置（JSON）
     */
    private String preferences;

    /**
     * 注册时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 登录次数
     */
    private Integer loginCount;

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return "ADMIN".equals(userType) ||
               (roles != null && roles.contains("ADMIN"));
    }

    /**
     * 是否为VIP用户
     */
    public boolean isVip() {
        return "VIP".equals(userType);
    }

    /**
     * 是否激活
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * 是否被锁定
     */
    public boolean isLocked() {
        return "LOCKED".equals(status);
    }

    /**
     * 是否属于某个家庭
     */
    public boolean belongsToFamily(String familyId) {
        return familyIds != null && familyIds.contains(familyId);
    }

    /**
     * 是否拥有某个权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * 是否拥有某个角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}