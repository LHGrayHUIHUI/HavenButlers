package com.haven.storage.domain.model.share;

import java.time.Instant;

/**
 * 文件分享结果
 *
 * @author Haven
 */
public class ShareResult {

    /**
     * 分享ID
     */
    private String shareId;

    /**
     * 分享链接
     */
    private String shareUrl;

    /**
     * 分享令牌
     */
    private String shareToken;

    /**
     * 过期时间
     */
    private Instant expireTime;

    /**
     * 二维码图片URL（可选）
     */
    private String qrCodeUrl;

    /**
     * 短链接（可选）
     */
    private String shortUrl;

    // Getters and Setters
    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public Instant getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Instant expireTime) {
        this.expireTime = expireTime;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }
}