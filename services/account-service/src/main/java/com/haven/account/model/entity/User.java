package com.haven.account.model.entity;

import com.haven.account.model.enums.UserStatus;
import com.haven.base.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户实体类
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>用户认证：支持用户名/邮箱/手机号登录</li>
 *   <li>权限管理：基于角色的访问控制(RBAC)</li>
 *   <li>多家庭支持：用户可加入多个家庭，切换当前操作家庭</li>
 *   <li>审计追踪：记录创建者、更新者、版本等信息</li>
 * </ul>
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseEntity {



    /**
     * 用户唯一标识符
     * 系统生成的UUID，用于外部接口和分布式系统中的用户标识
     * 与数据库自增ID配合使用，提供业务层面的唯一性保证
     */
    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private UUID uuid;

    /**
     * 用户名
     * 用户登录的唯一标识符，系统内不允许重复
     * 长度限制50字符，支持字母、数字、下划线组合
     */
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /**
     * 邮箱地址
     * 用户注册邮箱，用于登录验证和接收通知
     * 系统内唯一，长度限制100字符
     */
    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    /**
     * 密码哈希值
     * 使用BCrypt算法加密后的用户密码，永不存储明文
     * 长度自适应，通常为60字符
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * 手机号码
     * 用户注册手机号，可选字段，用于短信验证和双因素认证
     * 支持国际号码格式，最大20字符
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 用户头像URL
     * 用户个人头像图片的访问地址
     * 支持外部CDN链接，最大长度500字符
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 用户背景图片URL
     * 用户个人主页背景图片的访问地址
     * 支持外部CDN链接，最大长度500字符
     */
    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

    /**
     * 用户状态
     * 表示账户的当前状态：ACTIVE(正常)、INACTIVE(未激活)、LOCKED(锁定)
     * 默认值：ACTIVE，影响用户登录权限和系统访问
     */
    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    /**
     * 当前激活家庭ID
     * 用户当前选择操作的家庭标识，关联Family表主键
     * 用于多家庭环境下的上下文切换，null表示未选择家庭
     */
    @Column(name = "current_family_id")
    private Long currentFamilyId;

    /**
     * 用户角色权限
     * 字符串形式存储的用户角色集合，支持多角色
     * 格式：ROLE_ADMIN,ROLE_USER，用于Spring Security权限控制
     */
    @Column(name = "roles")
    private String roles;

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        // 使用 BaseEntity 的时间字段
        if (getCreateTime() == null) {
            setCreateTime(LocalDateTime.now());
        }
        setUpdateTime(LocalDateTime.now());

        // 初始化用户状态和角色
        if (status == null) {
            status = UserStatus.ACTIVE.getCode();
        }
        if (roles == null) {
            roles = "ROLE_USER";
        }

        // 初始化审计字段
        if (getCreateBy() == null) {
            setCreateBy("SYSTEM");
        }
        if (getUpdateBy() == null) {
            setUpdateBy("SYSTEM");
        }
        if (getVersion() == null) {
            setVersion(1);
        }

        // currentFamilyId 保持为 null，用户首次登录后选择家庭时设置
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdateTime(LocalDateTime.now());

        // 自动递增版本号
        if (getVersion() != null) {
            setVersion(getVersion() + 1);
        }

        // 更新修改者信息 - 这里可以通过Spring Security获取当前用户
        // 暂时设置为SYSTEM，实际使用时应该获取当前登录用户ID
        setUpdateBy("SYSTEM");
    }

    /**
     * 用户状态枚举
     */
    public enum Status {
        ACTIVE("ACTIVE"),
        INACTIVE("INACTIVE"),
        SUSPENDED("SUSPENDED"),
        DELETED("DELETED");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 获取用户状态
     */
    public Status getUserStatus() {
        return Status.valueOf(status);
    }

    /**
     * 设置用户状态
     */
    public void setUserStatus(Status status) {
        this.status = status.getValue();
    }
}