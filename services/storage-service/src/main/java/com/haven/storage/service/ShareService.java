package com.haven.storage.service;

import com.haven.storage.domain.model.share.ShareRecord;
import com.haven.storage.domain.model.share.ShareRequest;
import com.haven.storage.domain.model.share.ShareResult;
import com.haven.storage.domain.model.share.ShareStatistics;
import com.haven.storage.exception.ShareException;

import java.util.List;

/**
 * 文件分享服务
 *
 * @author Haven
 */
public interface ShareService {

    /**
     * 创建文件分享
     *
     * @param request 分享请求
     * @return 分享结果
     * @throws ShareException 分享异常
     */
    ShareResult createShare(ShareRequest request) throws ShareException;

    /**
     * 获取分享信息
     *
     * @param shareId 分享ID
     * @return 分享记录
     * @throws ShareException 分享异常
     */
    ShareRecord getShare(String shareId) throws ShareException;

    /**
     * 验证分享访问权限
     *
     * @param shareId 分享ID
     * @param shareToken 分享令牌
     * @param password 密码（可选）
     * @return 分享记录
     * @throws ShareException 分享异常
     */
    ShareRecord validateShareAccess(String shareId, String shareToken, String password) throws ShareException;

    /**
     * 更新分享信息
     *
     * @param shareId 分享ID
     * @param request 分享请求
     * @return 更新后的分享记录
     * @throws ShareException 分享异常
     */
    ShareRecord updateShare(String shareId, ShareRequest request) throws ShareException;

    /**
     * 删除分享
     *
     * @param shareId 分享ID
     * @param userId 用户ID
     * @throws ShareException 分享异常
     */
    void deleteShare(String shareId, String userId) throws ShareException;

    /**
     * 禁用/启用分享
     *
     * @param shareId 分享ID
     * @param userId 用户ID
     * @param enabled 是否启用
     * @throws ShareException 分享异常
     */
    void toggleShare(String shareId, String userId, boolean enabled) throws ShareException;

    /**
     * 获取用户的分享列表
     *
     * @param userId 用户ID
     * @param familyId 家庭ID
     * @return 分享列表
     */
    List<ShareRecord> getUserShares(String userId, String familyId);

    /**
     * 获取文件的分享列表
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 分享列表
     */
    List<ShareRecord> getFileShares(String fileId, String userId);

    /**
     * 获取分享统计信息
     *
     * @param shareId 分享ID
     * @param userId 用户ID
     * @return 分享统计
     * @throws ShareException 分享异常
     */
    ShareStatistics getShareStatistics(String shareId, String userId) throws ShareException;

    /**
     * 记录分享访问
     *
     * @param shareId 分享ID
     * @param visitorId 访客ID
     * @param visitorIp 访客IP
     * @param userAgent 用户代理
     */
    void recordShareAccess(String shareId, String visitorId, String visitorIp, String userAgent);

    /**
     * 清理过期分享
     */
    void cleanupExpiredShares();

    /**
     * 批量删除分享
     *
     * @param shareIds 分享ID列表
     * @param userId 用户ID
     * @throws ShareException 分享异常
     */
    void batchDeleteShares(List<String> shareIds, String userId) throws ShareException;
}