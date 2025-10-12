package com.haven.storage.domain.model.share;

import java.util.List;

/**
 * 文件分享请求
 *
 * @author Haven
 */
public class ShareRequest {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 家庭ID
     */
    private String familyId;

    /**
     * 分享类型
     */
    private ShareRecord.ShareType shareType;

    /**
     * 分享权限
     */
    private List<ShareRecord.SharePermission> permissions;

    /**
     * 分享密码（可选）
     */
    private String password;

    /**
     * 过期小时数
     */
    private Integer expireHours;

    /**
     * 分享标题
     */
    private String title;

    /**
     * 分享描述
     */
    private String description;

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

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
    }

    public ShareRecord.ShareType getShareType() {
        return shareType;
    }

    public void setShareType(ShareRecord.ShareType shareType) {
        this.shareType = shareType;
    }

    public List<ShareRecord.SharePermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<ShareRecord.SharePermission> permissions) {
        this.permissions = permissions;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getExpireHours() {
        return expireHours;
    }

    public void setExpireHours(Integer expireHours) {
        this.expireHours = expireHours;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}