package com.haven.storage.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 用户信息封装类 - 优化版本
 * <p>
 * 设计特点：
 * - 使用record确保不可变性
 * - 构造时参数验证
 * - 支持JSON序列化/反序列化
 * - 提供丰富的实用方法
 * - 优化内存使用和性能
 *
 * @author HavenButler
 */
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfo(
        @JsonProperty("userId") String userId,
        @JsonProperty("familyId") String familyId,
        @JsonProperty("userName") String userName
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 紧凑构造函数 - 只需用户ID
     */
    public UserInfo(String userId) {
        this(userId, null, null);
    }

    /**
     * 带验证的构造函数 - JSON反序列化时使用
     */
    @JsonCreator
    public UserInfo(
            @JsonProperty("userId") String userId,
            @JsonProperty("familyId") String familyId,
            @JsonProperty("userName") String userName
    ) {
        // 参数验证
        this.userId = validateAndTrim("userId", userId, true);
        this.familyId = validateAndTrim("familyId", familyId, false);
        this.userName = validateAndTrim("userName", userName, false);

        // 验证用户ID的合法性
        if (!isValidUserId(this.userId)) {
            throw new IllegalArgumentException("用户ID格式无效: " + this.userId);
        }
    }

    /**
     * 验证并修剪字符串参数
     */
    private static String validateAndTrim(String fieldName, String value, boolean required) {
        if (!StringUtils.hasText(value)) {
            if (required) {
                throw new IllegalArgumentException(fieldName + " 不能为空");
            }
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException(fieldName + " 不能为空字符串");
            }
            return null;
        }

        // 基本格式验证
        if (trimmed.length() > 255) {
            log.warn("{} 长度超过255字符，将被截断: {}", fieldName, trimmed);
            trimmed = trimmed.substring(0, 255);
        }

        return trimmed;
    }

    /**
     * 验证用户ID是否有效
     */
    private static boolean isValidUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return false;
        }

        // 用户ID格式验证：只允许字母、数字、下划线、连字符
        return userId.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * 检查用户ID是否有效
     */
    public boolean hasValidUserId() {
        return isValidUserId(userId);
    }

    /**
     * 检查是否属于指定家庭
     */
    public boolean belongsToFamily(String targetFamilyId) {
        return StringUtils.hasText(familyId) &&
                StringUtils.hasText(targetFamilyId) &&
                Objects.equals(familyId, targetFamilyId);
    }

    /**
     * 获取脱敏的用户信息（用于日志）
     */
    public String toSummaryString() {
        StringBuilder summary = new StringBuilder();
        summary.append("userId=").append(maskSensitiveInfo(userId));

        if (StringUtils.hasText(familyId)) {
            summary.append(", familyId=").append(maskSensitiveInfo(familyId));
        }

        if (StringUtils.hasText(userName)) {
            summary.append(", userName=").append(maskSensitiveInfo(userName));
        }

        return summary.toString();
    }

    /**
     * 获取完整的用户信息（非脱敏，用于调试）
     */
    public String toFullString() {
        return String.format("UserInfo{userId='%s', familyId='%s', userName='%s'}",
                userId, familyId, userName);
    }

    /**
     * 脱敏处理敏感信息
     */
    private String maskSensitiveInfo(String info) {
        if (!StringUtils.hasText(info)) {
            return null;
        }

        if (info.length() <= 3) {
            return info.charAt(0) + "**";
        }

        return info.charAt(0) + "*".repeat(info.length() - 2) + info.charAt(info.length() - 1);
    }

    /**
     * 检查用户信息是否完整
     */
    public boolean isComplete() {
        return StringUtils.hasText(userId) &&
                StringUtils.hasText(familyId) &&
                StringUtils.hasText(userName);
    }

    /**
     * 创建用户信息的副本（可选更新某些字段）
     */
    public UserInfo withFamilyId(String newFamilyId) {
        return new UserInfo(this.userId, newFamilyId, this.userName);
    }

    public UserInfo withUserName(String newUserName) {
        return new UserInfo(this.userId, this.familyId, newUserName);
    }

    /**
     * 安全地获取字段值（已修剪）
     */
    public String getValidUserId() {
        return userId;
    }

    public String getValidFamilyId() {
        return familyId;
    }

    public String getValidUserName() {
        return userName;
    }

    /**
     * 静态工厂方法
     */
    public static UserInfo of(String userId) {
        return new UserInfo(userId);
    }

    public static UserInfo of(String userId, String familyId) {
        return new UserInfo(userId, familyId, null);
    }

    public static UserInfo of(String userId, String familyId, String userName) {
        return new UserInfo(userId, familyId, userName);
    }

    /**
     * 从字符串解析（格式：userId:familyId:userName）
     */
    public static UserInfo fromString(String userInfoString) {
        if (!StringUtils.hasText(userInfoString)) {
            throw new IllegalArgumentException("用户信息字符串不能为空");
        }

        String[] parts = userInfoString.split(":", 3);
        if (parts.length == 0) {
            throw new IllegalArgumentException("用户信息字符串格式无效");
        }

        String userId = parts[0];
        String familyId = parts.length > 1 ? parts[1] : null;
        String userName = parts.length > 2 ? parts[2] : null;

        return new UserInfo(userId, familyId, userName);
    }

    /**
     * 转换为字符串格式（用于缓存键等场景）
     */
    public String toCacheKey() {
        return String.format("user:%s:%s", userId,
                StringUtils.hasText(familyId) ? familyId : "default");
    }

    @NotNull
    @Override
    public String toString() {
        return toSummaryString(); // 默认返回脱敏信息
    }
}