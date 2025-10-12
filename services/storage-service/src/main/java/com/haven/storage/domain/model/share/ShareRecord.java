package com.haven.storage.domain.model.share;

import java.time.Instant;
import java.util.List;

/**
 * 文件分享记录
 *
 * @author Haven
 */
public class ShareRecord {

    /**
     * 分享ID
     */
    private String shareId;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 分享者ID
     */
    private String ownerId;

    /**
     * 家庭ID
     */
    private String familyId;

    /**
     * 分享令牌
     */
    private String shareToken;

    /**
     * 分享链接
     */
    private String shareUrl;

    /**
     * 分享类型
     */
    private ShareType shareType;

    /**
     * 分享权限
     */
    private List<SharePermission> permissions;

    /**
     * 分享密码（加密存储）
     */
    private String password;

    /**
     * 过期时间
     */
    private Instant expireTime;

    /**
     * 创建时间
     */
    private Instant createTime;

    /**
     * 访问次数
     */
    private Long accessCount;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 分享类型枚举
     */
    public enum ShareType {
        PUBLIC("公开分享"),
        PASSWORD("密码分享"),
        PRIVATE("私密分享");

        private final String description;

        ShareType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 分享权限枚举
     */
    public enum SharePermission {
        VIEW("查看"),
        DOWNLOAD("下载"),
        PRINT("打印"),
        COMMENT("评论");

        private final String description;

        SharePermission(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters and Setters
    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public ShareType getShareType() {
        return shareType;
    }

    public void setShareType(ShareType shareType) {
        this.shareType = shareType;
    }

    public List<SharePermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<SharePermission> permissions) {
        this.permissions = permissions;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Instant getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Instant expireTime) {
        this.expireTime = expireTime;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Long accessCount) {
        this.accessCount = accessCount;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}