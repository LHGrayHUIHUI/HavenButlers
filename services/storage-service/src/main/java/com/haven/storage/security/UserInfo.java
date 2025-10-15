package com.haven.storage.security;

import org.springframework.util.StringUtils;

/**
 * 用户信息封装类
 *
 * 独立的用户信息类，用于在系统各层之间传递用户身份信息
 * 包含用户ID、家庭ID和用户名等核心信息
 *
 * @author HavenButler
 */
public record UserInfo(String userId, String familyId, String userName) {

    /**
     * 构建用户信息摘要（用于日志）
     *
     * @return 格式化的用户信息摘要字符串
     */
    public String toSummaryString() {
        StringBuilder summary = new StringBuilder();
        summary.append("userId=").append(userId);

        if (StringUtils.hasText(familyId)) {
            summary.append(", familyId=").append(familyId);
        }

        if (StringUtils.hasText(userName)) {
            summary.append(", userName=").append(userName);
        }
        return summary.toString();
    }

    /**
     * 检查用户ID是否有效
     *
     * @return 如果用户ID不为空且有效则返回true
     */
    public boolean hasValidUserId() {
        return StringUtils.hasText(userId);
    }

    /**
     * 检查是否属于指定家庭
     *
     * @param targetFamilyId 目标家庭ID
     * @return 如果用户属于指定家庭则返回true
     */
    public boolean belongsToFamily(String targetFamilyId) {
        return StringUtils.hasText(familyId) && familyId.equals(targetFamilyId);
    }

    /**
     * 获取有效的用户ID
     *
     * @return 有效的用户ID，如果为空则返回null
     */
    public String getValidUserId() {
        return StringUtils.hasText(userId) ? userId.trim() : null;
    }

    /**
     * 获取有效的家庭ID
     *
     * @return 有效的家庭ID，如果为空则返回null
     */
    public String getValidFamilyId() {
        return StringUtils.hasText(familyId) ? familyId.trim() : null;
    }

    /**
     * 获取有效的用户名
     *
     * @return 有效的用户名，如果为空则返回null
     */
    public String getValidUserName() {
        return StringUtils.hasText(userName) ? userName.trim() : null;
    }
}