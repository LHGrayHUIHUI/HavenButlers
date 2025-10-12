package com.haven.storage.domain.model.share;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分享统计信息
 *
 * @author Haven
 */
public class ShareStatistics {

    /**
     * 分享ID
     */
    private String shareId;

    /**
     * 总访问次数
     */
    private Long totalAccess;

    /**
     * 独立访客数
     */
    private Long uniqueVisitors;

    /**
     * 总下载次数
     */
    private Long totalDownloads;

    /**
     * 最近访问记录
     */
    private List<ShareAccessRecord> recentAccess;

    /**
     * 每日访问趋势
     */
    private Map<LocalDate, Long> dailyAccess;

    /**
     * 每小时访问分布
     */
    private Map<Integer, Long> hourlyDistribution;

    /**
     * 访问来源统计
     */
    private Map<String, Long> sourceStats;

    /**
     * 设备类型统计
     */
    private Map<String, Long> deviceStats;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 分享状态
     */
    private ShareStatus status;

    /**
     * 分享状态枚举
     */
    public enum ShareStatus {
        ACTIVE("活跃"),
        EXPIRED("已过期"),
        DISABLED("已禁用"),
        LIMIT_EXCEEDED("访问超限");

        private final String description;

        ShareStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 分享访问记录
     */
    public static class ShareAccessRecord {
        private String visitorId;
        private String visitorIp;
        private String userAgent;
        private String referer;
        private LocalDateTime accessTime;
        private String action; // ACCESS, DOWNLOAD, etc.

        // Getters and Setters
        public String getVisitorId() {
            return visitorId;
        }

        public void setVisitorId(String visitorId) {
            this.visitorId = visitorId;
        }

        public String getVisitorIp() {
            return visitorIp;
        }

        public void setVisitorIp(String visitorIp) {
            this.visitorIp = visitorIp;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getReferer() {
            return referer;
        }

        public void setReferer(String referer) {
            this.referer = referer;
        }

        public LocalDateTime getAccessTime() {
            return accessTime;
        }

        public void setAccessTime(LocalDateTime accessTime) {
            this.accessTime = accessTime;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }

    // Getters and Setters
    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public Long getTotalAccess() {
        return totalAccess;
    }

    public void setTotalAccess(Long totalAccess) {
        this.totalAccess = totalAccess;
    }

    public Long getUniqueVisitors() {
        return uniqueVisitors;
    }

    public void setUniqueVisitors(Long uniqueVisitors) {
        this.uniqueVisitors = uniqueVisitors;
    }

    public Long getTotalDownloads() {
        return totalDownloads;
    }

    public void setTotalDownloads(Long totalDownloads) {
        this.totalDownloads = totalDownloads;
    }

    public List<ShareAccessRecord> getRecentAccess() {
        return recentAccess;
    }

    public void setRecentAccess(List<ShareAccessRecord> recentAccess) {
        this.recentAccess = recentAccess;
    }

    public Map<LocalDate, Long> getDailyAccess() {
        return dailyAccess;
    }

    public void setDailyAccess(Map<LocalDate, Long> dailyAccess) {
        this.dailyAccess = dailyAccess;
    }

    public Map<Integer, Long> getHourlyDistribution() {
        return hourlyDistribution;
    }

    public void setHourlyDistribution(Map<Integer, Long> hourlyDistribution) {
        this.hourlyDistribution = hourlyDistribution;
    }

    public Map<String, Long> getSourceStats() {
        return sourceStats;
    }

    public void setSourceStats(Map<String, Long> sourceStats) {
        this.sourceStats = sourceStats;
    }

    public Map<String, Long> getDeviceStats() {
        return deviceStats;
    }

    public void setDeviceStats(Map<String, Long> deviceStats) {
        this.deviceStats = deviceStats;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public ShareStatus getStatus() {
        return status;
    }

    public void setStatus(ShareStatus status) {
        this.status = status;
    }
}