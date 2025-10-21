package com.haven.storage.service.cache;

import com.haven.base.utils.TraceIdUtil;
import com.haven.common.redis.RedisCache;
import com.haven.common.redis.RedisUtils;
import com.haven.storage.domain.model.file.FileMetadata;
import com.haven.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 文件元数据缓存服务
 * <p>
 * 提供文件元数据的Redis缓存管理，基于common模块Redis封装：
 * - 单个文件元数据缓存
 * - 家庭文件列表缓存
 * - 搜索结果缓存
 * - 统计数据缓存
 * <p>
 * 缓存策略：
 * - 文件元数据：7天过期，访问时刷新
 * - 家庭文件列表：1小时过期，修改时清理
 * - 搜索结果：30分钟过期
 * - 统计数据：30分钟过期
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileMetadataCacheService {

    private final RedisCache redisCache;
    private final RedisUtils redisUtils;

    // 缓存键前缀
    private static final String FILE_METADATA_PREFIX = "file:metadata:";
    private static final String FAMILY_FILES_PREFIX = "family:files:";
    private static final String SEARCH_RESULT_PREFIX = "search:result:";
    private static final String STORAGE_STATS_PREFIX = "storage:stats:";

    // 缓存过期时间
    private static final int FILE_METADATA_TTL_DAYS = 7;
    private static final int FAMILY_FILES_TTL_HOURS = 1;
    private static final int SEARCH_RESULT_TTL_MINUTES = 30;
    private static final int STORAGE_STATS_TTL_MINUTES = 30;

    /**
     * 缓存文件元数据
     */
    public void cacheFileMetadata(FileMetadata fileMetadata) {
        if (fileMetadata == null || fileMetadata.getFileId() == null) {
            return;
        }

        try {
            String cacheKey = FILE_METADATA_PREFIX + fileMetadata.getFileId();
            redisCache.set(cacheKey, fileMetadata, FILE_METADATA_TTL_DAYS, TimeUnit.DAYS);
            log.debug("文件元数据已缓存: fileId={}, fileName={}",
                    fileMetadata.getFileId(), fileMetadata.getOriginalName());
        } catch (Exception e) {
            log.warn("缓存文件元数据失败: fileId={}, error={}",
                    fileMetadata.getFileId(), e.getMessage());
        }
    }

    /**
     * 获取缓存的文件元数据
     */
    public Optional<FileMetadata> getCachedFileMetadata(String fileId) {
        if (fileId == null) {
            return Optional.empty();
        }

        try {
            String cacheKey = FILE_METADATA_PREFIX + fileId;
            FileMetadata cached = redisCache.get(cacheKey, FileMetadata.class);
            if (cached != null) {
                log.debug("从缓存获取文件元数据: fileId={}, fileName={}",
                        fileId, cached.getOriginalName());
                return Optional.of(cached);
            }
        } catch (Exception e) {
            log.warn("获取缓存文件元数据失败: fileId={}, error={}", fileId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * 删除文件元数据缓存
     */
    public void evictFileMetadata(String fileId) {
        if (fileId == null) {
            return;
        }

        try {
            String cacheKey = FILE_METADATA_PREFIX + fileId;
            redisUtils.delete(cacheKey);
            log.debug("文件元数据缓存已删除: fileId={}", fileId);
        } catch (Exception e) {
            log.warn("删除文件元数据缓存失败: fileId={}, error={}", fileId, e.getMessage());
        }
    }


    /**
     * 缓存搜索结果
     */
    public void cacheSearchResult(String familyId, String keyword, Object searchResult) {
        if (familyId == null || keyword == null || searchResult == null) {
            return;
        }

        try {
            String cacheKey = SEARCH_RESULT_PREFIX + familyId + ":" + keyword;
            redisCache.set(cacheKey, searchResult, SEARCH_RESULT_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("搜索结果已缓存: familyId={}, keyword={}", familyId, keyword);
        } catch (Exception e) {
            log.warn("缓存搜索结果失败: familyId={}, keyword={}, error={}",
                    familyId, keyword, e.getMessage());
        }
    }

    /**
     * 获取缓存的搜索结果
     */
    public Optional<Object> getCachedSearchResult(String familyId, String keyword) {
        if (familyId == null || keyword == null) {
            return Optional.empty();
        }

        try {
            String cacheKey = SEARCH_RESULT_PREFIX + familyId + ":" + keyword;
            Object cached = redisCache.get(cacheKey, Object.class);
            if (cached != null) {
                log.debug("从缓存获取搜索结果: familyId={}, keyword={}", familyId, keyword);
                return Optional.of(cached);
            }
        } catch (Exception e) {
            log.warn("获取缓存搜索结果失败: familyId={}, keyword={}, error={}",
                    familyId, keyword, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * 清理所有缓存（系统维护时使用）
     */
    public void evictAllCache() {
        try {
            // 删除各种类型的缓存
            redisCache.deletePattern(FILE_METADATA_PREFIX + "*", 200);
            redisCache.deletePattern(FAMILY_FILES_PREFIX + "*", 200);
            redisCache.deletePattern(SEARCH_RESULT_PREFIX + "*", 200);
            redisCache.deletePattern(STORAGE_STATS_PREFIX + "*", 200);

            log.info("所有缓存已清理");
        } catch (Exception e) {
            log.warn("清理所有缓存失败: error={}", e.getMessage());
        }
    }

}