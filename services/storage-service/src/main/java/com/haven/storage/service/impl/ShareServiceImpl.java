package com.haven.storage.service.impl;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.share.ShareRecord;
import com.haven.storage.domain.model.share.ShareRequest;
import com.haven.storage.domain.model.share.ShareResult;
import com.haven.storage.domain.model.share.ShareStatistics;
import com.haven.storage.exception.ShareException;
import com.haven.storage.service.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 文件分享服务实现
 *
 * 提供文件分享、权限验证、统计等核心功能的实现
 * 目前使用内存存储，生产环境应替换为数据库
 *
 * @author Haven
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    // 内存存储分享记录（实际应用中应替换为数据库）
    private final Map<String, ShareRecord> shareStore = new ConcurrentHashMap<>();

    // 文件分享索引：fileId -> Set<shareId>
    private final Map<String, Set<String>> fileShareIndex = new ConcurrentHashMap<>();

    // 用户分享索引：userId -> Set<shareId>
    private final Map<String, Set<String>> userShareIndex = new ConcurrentHashMap<>();

    // 家庭分享索引：familyId -> Set<shareId>
    private final Map<String, Set<String>> familyShareIndex = new ConcurrentHashMap<>();

    @Override
    @TraceLog(value = "创建文件分享", module = "share-service", type = "CREATE_SHARE")
    public ShareResult createShare(ShareRequest request) throws ShareException {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            String shareId = generateShareId();
            String shareToken = generateShareToken();
            String shareUrl = buildShareUrl(shareId, shareToken);

            // 计算过期时间
            Instant expireTime = calculateExpireTime(request.getExpireHours());

            // 创建分享记录
            ShareRecord shareRecord = new ShareRecord();
            shareRecord.setShareId(shareId);
            shareRecord.setFileId(request.getFileId());
            shareRecord.setOwnerId(request.getUserId());
            shareRecord.setFamilyId(request.getFamilyId());
            shareRecord.setShareToken(shareToken);
            shareRecord.setShareUrl(shareUrl);
            shareRecord.setShareType(request.getShareType());
            shareRecord.setPermissions(request.getPermissions());
            shareRecord.setPassword(request.getPassword());
            shareRecord.setExpireTime(expireTime);
            shareRecord.setCreateTime(Instant.now());
            shareRecord.setEnabled(true);
            shareRecord.setAccessCount(0L);

            // 存储分享记录
            shareStore.put(shareId, shareRecord);

            // 更新索引
            fileShareIndex.computeIfAbsent(request.getFileId(), k -> ConcurrentHashMap.newKeySet()).add(shareId);
            userShareIndex.computeIfAbsent(request.getUserId(), k -> ConcurrentHashMap.newKeySet()).add(shareId);
            familyShareIndex.computeIfAbsent(request.getFamilyId(), k -> ConcurrentHashMap.newKeySet()).add(shareId);

            log.info("文件分享创建成功: shareId={}, fileId={}, userId={}, familyId={}, traceId={}",
                    shareId, request.getFileId(), request.getUserId(), request.getFamilyId(), traceId);

            ShareResult shareResult = new ShareResult();
            shareResult.setShareId(shareId);
            shareResult.setShareUrl(shareUrl);
            shareResult.setExpireTime(expireTime);
            return shareResult;

        } catch (Exception e) {
            log.error("创建文件分享失败: fileId={}, error={}, traceId={}",
                    request.getFileId(), e.getMessage(), traceId, e);
            throw new ShareException("创建文件分享失败", e);
        }
    }

    @Override
    @TraceLog(value = "获取分享信息", module = "share-service", type = "GET_SHARE")
    public ShareRecord getShare(String shareId) throws ShareException {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ShareRecord share = shareStore.get(shareId);
            if (share == null) {
                log.warn("分享不存在: shareId={}, traceId={}", shareId, traceId);
                throw new ShareException("分享不存在");
            }

            log.debug("分享信息获取成功: shareId={}, traceId={}", shareId, traceId);
            return share;

        } catch (ShareException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取分享信息失败: shareId={}, error={}, traceId={}", shareId, e.getMessage(), traceId, e);
            throw new ShareException("获取分享信息失败", e);
        }
    }

    @Override
    @TraceLog(value = "验证分享访问权限", module = "share-service", type = "VALIDATE_ACCESS")
    public ShareRecord validateShareAccess(String shareId, String shareToken, String password) throws ShareException {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ShareRecord share = getShare(shareId); // 复用获取逻辑

            // 检查分享是否启用
            if (!share.getEnabled()) {
                log.warn("分享已禁用: shareId={}, traceId={}", shareId, traceId);
                throw new ShareException("分享已禁用");
            }

            // 检查分享是否过期
            if (share.getExpireTime() != null && share.getExpireTime().isBefore(Instant.now())) {
                log.warn("分享已过期: shareId={}, expireTime={}, traceId={}",
                        shareId, share.getExpireTime(), traceId);
                throw new ShareException("分享已过期");
            }

            // 验证分享令牌
            if (!share.getShareToken().equals(shareToken)) {
                log.warn("分享令牌无效: shareId={}, traceId={}", shareId, traceId);
                throw new ShareException("分享令牌无效");
            }

            // 验证密码（如果有）
            if (share.getPassword() != null && !share.getPassword().isEmpty()) {
                if (password == null || !password.equals(share.getPassword())) {
                    log.warn("分享密码错误: shareId={}, traceId={}", shareId, traceId);
                    throw new ShareException("分享密码错误");
                }
            }

            // 增加访问次数
            share.setAccessCount(share.getAccessCount() + 1);

            log.debug("分享权限验证通过: shareId={}, traceId={}", shareId, traceId);
            return share;

        } catch (ShareException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证分享访问权限失败: shareId={}, error={}, traceId={}", shareId, e.getMessage(), traceId, e);
            throw new ShareException("验证分享访问权限失败", e);
        }
    }

    @Override
    @TraceLog(value = "更新分享信息", module = "share-service", type = "UPDATE_SHARE")
    public ShareRecord updateShare(String shareId, ShareRequest request) throws ShareException {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ShareRecord share = getShare(shareId); // 复用获取逻辑

            // 验证权限（只有所有者可以更新）
            if (!share.getOwnerId().equals(request.getUserId())) {
                log.warn("无权限更新分享: shareId={}, ownerId={}, userId={}, traceId={}",
                        shareId, share.getOwnerId(), request.getUserId(), traceId);
                throw new ShareException("无权限更新分享");
            }

            // 更新信息
            if (request.getExpireHours() != null) {
                share.setExpireTime(calculateExpireTime(request.getExpireHours()));
            }
            if (request.getPassword() != null) {
                share.setPassword(request.getPassword());
            }

            log.info("分享信息更新成功: shareId={}, traceId={}", shareId, traceId);
            return share;

        } catch (ShareException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新分享信息失败: shareId={}, error={}, traceId={}", shareId, e.getMessage(), traceId, e);
            throw new ShareException("更新分享信息失败", e);
        }
    }

    @Override
    @TraceLog(value = "删除分享", module = "share-service", type = "DELETE_SHARE")
    public void deleteShare(String shareId, String userId) throws ShareException {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ShareRecord share = getShare(shareId); // 复用获取逻辑

            // 验证权限（只有所有者可以删除）
            if (!share.getOwnerId().equals(userId)) {
                log.warn("无权限删除分享: shareId={}, ownerId={}, userId={}, traceId={}",
                        shareId, share.getOwnerId(), userId, traceId);
                throw new ShareException("无权限删除分享");
            }

            // 从存储中移除
            shareStore.remove(shareId);

            // 从索引中移除
            if (share.getFileId() != null) {
                fileShareIndex.computeIfPresent(share.getFileId(), (k, v) -> {
                    v.remove(shareId);
                    return v.isEmpty() ? null : v;
                });
            }
            userShareIndex.computeIfPresent(share.getOwnerId(), (k, v) -> {
                v.remove(shareId);
                return v.isEmpty() ? null : v;
            });
            if (share.getFamilyId() != null) {
                familyShareIndex.computeIfPresent(share.getFamilyId(), (k, v) -> {
                    v.remove(shareId);
                    return v.isEmpty() ? null : v;
                });
            }

            log.info("分享删除成功: shareId={}, userId={}, traceId={}", shareId, userId, traceId);

        } catch (ShareException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除分享失败: shareId={}, userId={}, error={}, traceId={}", shareId, userId, e.getMessage(), traceId, e);
            throw new ShareException("删除分享失败", e);
        }
    }

    @Override
    @TraceLog(value = "切换分享状态", module = "share-service", type = "TOGGLE_SHARE")
    public void toggleShare(String shareId, String userId, boolean enabled) throws ShareException {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ShareRecord share = getShare(shareId); // 复用获取逻辑

            // 验证权限（只有所有者可以切换状态）
            if (!share.getOwnerId().equals(userId)) {
                log.warn("无权限切换分享状态: shareId={}, ownerId={}, userId={}, traceId={}",
                        shareId, share.getOwnerId(), userId, traceId);
                throw new ShareException("无权限切换分享状态");
            }

            share.setEnabled(enabled);

            log.info("分享状态切换成功: shareId={}, enabled={}, traceId={}", shareId, enabled, traceId);

        } catch (ShareException e) {
            throw e;
        } catch (Exception e) {
            log.error("切换分享状态失败: shareId={}, enabled={}, error={}, traceId={}",
                    shareId, enabled, e.getMessage(), traceId, e);
            throw new ShareException("切换分享状态失败", e);
        }
    }

    @Override
    public List<ShareRecord> getUserShares(String userId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            Set<String> userShareIds = userShareIndex.getOrDefault(userId, Collections.emptySet());
            return userShareIds.stream()
                    .map(shareStore::get)
                    .filter(Objects::nonNull)
                    .filter(share -> familyId.equals(share.getFamilyId()))
                    .filter(share -> share.getEnabled())
                    .sorted((s1, s2) -> s2.getCreateTime().compareTo(s1.getCreateTime()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户分享列表失败: userId={}, familyId={}, error={}, traceId={}",
                    userId, familyId, e.getMessage(), traceId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<ShareRecord> getFileShares(String fileId, String userId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            Set<String> fileShareIds = fileShareIndex.getOrDefault(fileId, Collections.emptySet());
            return fileShareIds.stream()
                    .map(shareStore::get)
                    .filter(Objects::nonNull)
                    .filter(share -> userId.equals(share.getOwnerId()))
                    .filter(share -> share.getEnabled())
                    .sorted((s1, s2) -> s2.getCreateTime().compareTo(s1.getCreateTime()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取文件分享列表失败: fileId={}, userId={}, error={}, traceId={}",
                    fileId, userId, e.getMessage(), traceId, e);
            return Collections.emptyList();
        }
    }

    @Override
    @TraceLog(value = "获取分享统计", module = "share-service", type = "GET_STATISTICS")
    public ShareStatistics getShareStatistics(String shareId, String userId) throws ShareException {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            ShareRecord share = getShare(shareId); // 复用获取逻辑

            // 验证权限（只有所有者可以查看统计）
            if (!share.getOwnerId().equals(userId)) {
                log.warn("无权限查看分享统计: shareId={}, ownerId={}, userId={}, traceId={}",
                        shareId, share.getOwnerId(), userId, traceId);
                throw new ShareException("无权限查看分享统计");
            }

            // 构建统计信息
            ShareStatistics statistics = new ShareStatistics();
            statistics.setShareId(shareId);
            statistics.setTotalAccess(share.getAccessCount());

            log.debug("分享统计获取成功: shareId={}, totalAccess={}, traceId={}",
                    shareId, share.getAccessCount(), traceId);
            return statistics;

        } catch (ShareException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取分享统计失败: shareId={}, error={}, traceId={}", shareId, e.getMessage(), traceId, e);
            throw new ShareException("获取分享统计失败", e);
        }
    }

    @Override
    @TraceLog(value = "记录分享访问", module = "share-service", type = "RECORD_ACCESS")
    public void recordShareAccess(String shareId, String visitorId, String visitorIp, String userAgent) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 这里可以记录详细的访问日志
            // 实际应用中可以记录到数据库或日志系统
            log.info("记录分享访问: shareId={}, visitorId={}, visitorIp={}, userAgent={}, traceId={}",
                    shareId, visitorId, visitorIp, userAgent, traceId);

        } catch (Exception e) {
            log.error("记录分享访问失败: shareId={}, error={}, traceId={}", shareId, e.getMessage(), traceId, e);
        }
    }

    @Override
    @TraceLog(value = "清理过期分享", module = "share-service", type = "CLEANUP_EXPIRED")
    public void cleanupExpiredShares() {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            Instant now = Instant.now();
            List<String> expiredShareIds = new ArrayList<>();

            // 找出过期的分享
            shareStore.entrySet().forEach(entry -> {
                ShareRecord share = entry.getValue();
                if (share.getExpireTime() != null && share.getExpireTime().isBefore(now)) {
                    expiredShareIds.add(entry.getKey());
                }
            });

            // 删除过期分享
            for (String expiredShareId : expiredShareIds) {
                ShareRecord share = shareStore.get(expiredShareId);
                if (share != null) {
                    try {
                    deleteShare(expiredShareId, share.getOwnerId());
                } catch (ShareException e) {
                    log.warn("清理过期分享失败: shareId={}, error={}", expiredShareId, e.getMessage());
                }
                }
            }

            if (!expiredShareIds.isEmpty()) {
                log.info("清理过期分享完成: count={}, traceId={}", expiredShareIds.size(), traceId);
            }

        } catch (Exception e) {
            log.error("清理过期分享失败: error={}, traceId={}", e.getMessage(), traceId, e);
        }
    }

    @Override
    @TraceLog(value = "批量删除分享", module = "share-service", type = "BATCH_DELETE")
    public void batchDeleteShares(List<String> shareIds, String userId) throws ShareException {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            int successCount = 0;
            int failureCount = 0;

            for (String shareId : shareIds) {
                try {
                    deleteShare(shareId, userId);
                    successCount++;
                } catch (ShareException e) {
                    failureCount++;
                    log.warn("批量删除分享失败: shareId={}, error={}", shareId, e.getMessage());
                }
            }

            log.info("批量删除分享完成: total={}, success={}, failure={}, traceId={}",
                    shareIds.size(), successCount, failureCount, traceId);

        } catch (Exception e) {
            log.error("批量删除分享失败: userId={}, error={}, traceId={}", userId, e.getMessage(), traceId, e);
            throw new ShareException("批量删除分享失败", e);
        }
    }

    // ===== 私有方法 =====

    /**
     * 生成分享ID
     */
    private String generateShareId() {
        return "share_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成分享令牌
     */
    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 构建分享URL
     */
    private String buildShareUrl(String shareId, String shareToken) {
        return String.format("http://localhost:8081/api/v1/share/%s?token=%s", shareId, shareToken);
    }

    /**
     * 计算过期时间
     */
    private Instant calculateExpireTime(Integer expireHours) {
        if (expireHours == null || expireHours <= 0) {
            return null; // 永久分享
        }
        return Instant.now().plus(expireHours, ChronoUnit.HOURS);
    }
}