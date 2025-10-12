package com.haven.storage.controller;

import com.haven.storage.domain.model.share.ShareRecord;
import com.haven.storage.domain.model.share.ShareRequest;
import com.haven.storage.domain.model.share.ShareResult;
import com.haven.storage.domain.model.share.ShareStatistics;
import com.haven.storage.service.ShareService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

/**
 * 文件分享控制器
 *
 * @author Haven
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/share")
public class ShareController {

    @Autowired
    private ShareService shareService;

    /**
     * 创建文件分享
     */
    @PostMapping
    public ResponseEntity<ShareResult> createShare(@Valid @RequestBody ShareRequest request) {
        log.info("创建文件分享: fileId={}, userId={}, familyId={}",
                request.getFileId(), request.getUserId(), request.getFamilyId());

        try {
            ShareResult result = shareService.createShare(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建文件分享失败: fileId={}, error={}", request.getFileId(), e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取分享信息
     */
    @GetMapping("/{shareId}")
    public ResponseEntity<ShareRecord> getShare(@PathVariable String shareId,
                                               @RequestParam String userId) {
        log.info("获取分享信息: shareId={}, userId={}", shareId, userId);

        try {
            ShareRecord share = shareService.getShare(shareId);
            return ResponseEntity.ok(share);
        } catch (Exception e) {
            log.error("获取分享信息失败: shareId={}, error={}", shareId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 验证分享访问权限
     */
    @PostMapping("/{shareId}/access")
    public ResponseEntity<ShareRecord> validateShareAccess(@PathVariable String shareId,
                                                          @RequestParam String shareToken,
                                                          @RequestParam(required = false) String password,
                                                          HttpServletRequest request) {
        String visitorIp = getClientIpAddress(request);
        log.info("验证分享访问权限: shareId={}, visitorIp={}", shareId, visitorIp);

        try {
            ShareRecord share = shareService.validateShareAccess(shareId, shareToken, password);

            // 记录访问
            String visitorId = generateVisitorId(request);
            String userAgent = request.getHeader("User-Agent");
            shareService.recordShareAccess(shareId, visitorId, visitorIp, userAgent);

            return ResponseEntity.ok(share);
        } catch (Exception e) {
            log.error("验证分享访问权限失败: shareId={}, error={}", shareId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新分享信息
     */
    @PutMapping("/{shareId}")
    public ResponseEntity<ShareRecord> updateShare(@PathVariable String shareId,
                                                  @Valid @RequestBody ShareRequest request) {
        log.info("更新分享信息: shareId={}, userId={}", shareId, request.getUserId());

        try {
            ShareRecord share = shareService.updateShare(shareId, request);
            return ResponseEntity.ok(share);
        } catch (Exception e) {
            log.error("更新分享信息失败: shareId={}, error={}", shareId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除分享
     */
    @DeleteMapping("/{shareId}")
    public ResponseEntity<Void> deleteShare(@PathVariable String shareId,
                                           @RequestParam String userId) {
        log.info("删除分享: shareId={}, userId={}", shareId, userId);

        try {
            shareService.deleteShare(shareId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("删除分享失败: shareId={}, error={}", shareId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 禁用/启用分享
     */
    @PatchMapping("/{shareId}/toggle")
    public ResponseEntity<Void> toggleShare(@PathVariable String shareId,
                                           @RequestParam String userId,
                                           @RequestParam boolean enabled) {
        log.info("切换分享状态: shareId={}, userId={}, enabled={}", shareId, userId, enabled);

        try {
            shareService.toggleShare(shareId, userId, enabled);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("切换分享状态失败: shareId={}, error={}", shareId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取用户的分享列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ShareRecord>> getUserShares(@PathVariable String userId,
                                                          @RequestParam String familyId) {
        log.info("获取用户分享列表: userId={}, familyId={}", userId, familyId);

        try {
            List<ShareRecord> shares = shareService.getUserShares(userId, familyId);
            return ResponseEntity.ok(shares);
        } catch (Exception e) {
            log.error("获取用户分享列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取文件的分享列表
     */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<List<ShareRecord>> getFileShares(@PathVariable String fileId,
                                                          @RequestParam String userId) {
        log.info("获取文件分享列表: fileId={}, userId={}", fileId, userId);

        try {
            List<ShareRecord> shares = shareService.getFileShares(fileId, userId);
            return ResponseEntity.ok(shares);
        } catch (Exception e) {
            log.error("获取文件分享列表失败: fileId={}, error={}", fileId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取分享统计信息
     */
    @GetMapping("/{shareId}/statistics")
    public ResponseEntity<ShareStatistics> getShareStatistics(@PathVariable String shareId,
                                                             @RequestParam String userId) {
        log.info("获取分享统计信息: shareId={}, userId={}", shareId, userId);

        try {
            ShareStatistics statistics = shareService.getShareStatistics(shareId, userId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取分享统计信息失败: shareId={}, error={}", shareId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 批量删除分享
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDeleteShares(@RequestBody List<String> shareIds,
                                                 @RequestParam String userId) {
        log.info("批量删除分享: shareIds={}, userId={}", shareIds, userId);

        try {
            shareService.batchDeleteShares(shareIds, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("批量删除分享失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 生成访客ID
     */
    private String generateVisitorId(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String clientIp = getClientIpAddress(request);

        return "visitor_" + Math.abs((userAgent + clientIp).hashCode()) + "_" +
               System.currentTimeMillis();
    }
}