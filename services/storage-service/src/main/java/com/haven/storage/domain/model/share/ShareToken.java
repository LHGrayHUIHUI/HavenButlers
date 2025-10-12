package com.haven.storage.domain.model.share;

import java.time.Instant;
import java.util.List;

/**
 * 分享令牌
 *
 * @author Haven
 */
public class ShareToken {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 分享ID
     */
    private String shareId;

    /**
     * 创建时间
     */
    private Instant createTime;

    /**
     * 过期时间
     */
    private Instant expireTime;

    /**
     * 分享权限
     */
    private List<ShareRecord.SharePermission> permissions;

    /**
     * 是否需要密码
     */
    private Boolean requirePassword;

    /**
     * 访问次数限制
     */
    private Integer accessLimit;

    /**
     * 当前访问次数
     */
    private Integer currentAccess;

    /**
     * 令牌版本
     */
    private String version;

    // Getters and Setters
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Instant expireTime) {
        this.expireTime = expireTime;
    }

    public List<ShareRecord.SharePermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<ShareRecord.SharePermission> permissions) {
        this.permissions = permissions;
    }

    public Boolean getRequirePassword() {
        return requirePassword;
    }

    public void setRequirePassword(Boolean requirePassword) {
        this.requirePassword = requirePassword;
    }

    public Integer getAccessLimit() {
        return accessLimit;
    }

    public void setAccessLimit(Integer accessLimit) {
        this.accessLimit = accessLimit;
    }

    public Integer getCurrentAccess() {
        return currentAccess;
    }

    public void setCurrentAccess(Integer currentAccess) {
        this.currentAccess = currentAccess;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * 检查令牌是否过期
     */
    public boolean isExpired() {
        return expireTime != null && Instant.now().isAfter(expireTime);
    }

    /**
     * 检查访问次数是否超限
     */
    public boolean isAccessLimitExceeded() {
        return accessLimit != null && currentAccess != null && currentAccess >= accessLimit;
    }

    /**
     * 检查令牌是否有效
     */
    public boolean isValid() {
        return !isExpired() && !isAccessLimitExceeded();
    }
}