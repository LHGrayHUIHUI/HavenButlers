package com.haven.storage.service;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.vectortag.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 向量标签服务
 *
 * 🎯 核心功能：
 * - 为文件生成向量化标签
 * - 基于内容的智能分类
 * - 语义相似度计算
 * - 标签推荐和聚合
 *
 * 💡 使用场景：
 * - 文件智能分类
 * - 内容语义搜索
 * - 标签自动推荐
 * - 大模型知识库构建
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class VectorTagService {

    @Value("${vectortag.embedding.model:text-embedding-ada-002}")
    private String embeddingModel;

    @Value("${vectortag.similarity.threshold:0.7}")
    private Double similarityThreshold;

    @Value("${vectortag.max.tags:10}")
    private Integer maxTagsPerFile;

    @Value("${vectortag.vector.dimension:1536}")
    private Integer vectorDimension;

    // 文件向量标签存储 <fileId, List<VectorTag>>
    private final Map<String, List<VectorTag>> fileVectorTags = new ConcurrentHashMap<>();

    // 标签向量存储 <tagName, tagVector>
    private final Map<String, List<Double>> tagVectors = new ConcurrentHashMap<>();

    // 家庭标签统计 <familyId, Map<tagName, count>>
    private final Map<String, Map<String, Integer>> familyTagStats = new ConcurrentHashMap<>();

    /**
     * 为文件生成向量标签
     */
    @TraceLog(value = "生成文件向量标签", module = "vectortag", type = "GENERATE")
    public VectorTagResult generateVectorTags(GenerateVectorTagRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 参数验证
            if (!StringUtils.hasText(request.getFileId()) ||
                !StringUtils.hasText(request.getFamilyId()) ||
                !StringUtils.hasText(request.getContent())) {
                return VectorTagResult.error("参数不能为空", traceId);
            }

            String fileId = request.getFileId();
            String familyId = request.getFamilyId();

            // 内容预处理
            String processedContent = preprocessContent(request.getContent());

            // 生成内容向量
            List<Double> contentVector = generateContentVector(processedContent);

            // 基于内容特征提取关键词
            List<String> extractedKeywords = extractKeywords(processedContent);

            // 生成向量标签
            List<VectorTag> vectorTags = new ArrayList<>();
            for (String keyword : extractedKeywords) {
                if (vectorTags.size() >= maxTagsPerFile) {
                    break;
                }

                // 为关键词生成向量
                List<Double> keywordVector = generateKeywordVector(keyword);

                // 计算内容与关键词的相似度
                double similarity = calculateCosineSimilarity(contentVector, keywordVector);

                if (similarity >= similarityThreshold) {
                    VectorTag vectorTag = new VectorTag();
                    vectorTag.setTagId(UUID.randomUUID().toString());
                    vectorTag.setFileId(fileId);
                    vectorTag.setFamilyId(familyId);
                    vectorTag.setTagName(keyword);
                    vectorTag.setTagVector(keywordVector);
                    vectorTag.setSimilarityScore(similarity);
                    vectorTag.setTagType(determineTagType(keyword));
                    // setCreatedAt会通过BaseEntity自动设置
                    vectorTag.setUserId(request.getUserId());

                    vectorTags.add(vectorTag);
                }
            }

            // 推荐相关标签
            List<String> recommendedTags = recommendSimilarTags(contentVector, familyId);
            for (String tagName : recommendedTags) {
                if (vectorTags.size() >= maxTagsPerFile) {
                    break;
                }

                // 避免重复标签
                boolean exists = vectorTags.stream()
                        .anyMatch(tag -> tag.getTagName().equals(tagName));
                if (!exists) {
                    List<Double> tagVector = tagVectors.getOrDefault(tagName, generateKeywordVector(tagName));
                    double similarity = calculateCosineSimilarity(contentVector, tagVector);

                    if (similarity >= similarityThreshold * 0.8) { // 推荐标签阈值稍低
                        VectorTag vectorTag = new VectorTag();
                        vectorTag.setTagId(UUID.randomUUID().toString());
                        vectorTag.setFileId(fileId);
                        vectorTag.setFamilyId(familyId);
                        vectorTag.setTagName(tagName);
                        vectorTag.setTagVector(tagVector);
                        vectorTag.setSimilarityScore(similarity);
                        vectorTag.setTagType(TagType.RECOMMENDED);
                        // setCreatedAt会通过BaseEntity自动设置
                        vectorTag.setUserId(request.getUserId());

                        vectorTags.add(vectorTag);
                    }
                }
            }

            // 存储向量标签
            fileVectorTags.put(fileId, vectorTags);

            // 更新标签向量库
            for (VectorTag tag : vectorTags) {
                tagVectors.put(tag.getTagName(), tag.getTagVector());
            }

            // 更新家庭标签统计
            updateFamilyTagStats(familyId, vectorTags);

            log.info("文件向量标签生成成功: fileId={}, tags={}, TraceID={}",
                    fileId, vectorTags.size(), traceId);

            return VectorTagResult.success(vectorTags, traceId);

        } catch (Exception e) {
            log.error("生成文件向量标签失败: fileId={}, error={}, TraceID={}",
                    request.getFileId(), e.getMessage(), traceId);
            return VectorTagResult.error(e.getMessage(), traceId);
        }
    }

    /**
     * 基于向量相似度搜索文件
     */
    @TraceLog(value = "向量标签搜索", module = "vectortag", type = "SEARCH")
    public VectorSearchResult searchByVectorTags(VectorSearchRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            String familyId = request.getFamilyId();
            String query = request.getQuery();
            int topK = request.getTopK() != null ? request.getTopK() : 10;

            // 生成查询向量
            List<Double> queryVector = generateQueryVector(query);

            // 搜索所有家庭文件的向量标签
            List<FileVectorMatch> matches = new ArrayList<>();

            for (Map.Entry<String, List<VectorTag>> entry : fileVectorTags.entrySet()) {
                String fileId = entry.getKey();
                List<VectorTag> tags = entry.getValue();

                // 过滤家庭数据
                List<VectorTag> familyTags = tags.stream()
                        .filter(tag -> tag.getFamilyId().equals(familyId))
                        .collect(Collectors.toList());

                if (familyTags.isEmpty()) {
                    continue;
                }

                // 计算文件的最高相似度
                double maxSimilarity = familyTags.stream()
                        .mapToDouble(tag -> calculateCosineSimilarity(queryVector, tag.getTagVector()))
                        .max()
                        .orElse(0.0);

                // 获取最相似的标签
                VectorTag bestMatchTag = familyTags.stream()
                        .max(Comparator.comparing(tag ->
                                calculateCosineSimilarity(queryVector, tag.getTagVector())))
                        .orElse(null);

                if (maxSimilarity >= similarityThreshold * 0.6 && bestMatchTag != null) { // 搜索阈值更宽松
                    FileVectorMatch match = new FileVectorMatch();
                    match.setFileId(fileId);
                    match.setSimilarity(maxSimilarity);
                    match.setMatchedTag(bestMatchTag);
                    match.setAllTags(familyTags);

                    matches.add(match);
                }
            }

            // 按相似度排序并限制结果数量
            List<FileVectorMatch> sortedMatches = matches.stream()
                    .sorted(Comparator.comparing(FileVectorMatch::getSimilarity).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());

            VectorSearchResult result = new VectorSearchResult();
            result.setFamilyId(familyId);
            result.setQuery(query);
            result.setMatches(sortedMatches);
            result.setTotalMatches(sortedMatches.size());
            result.setTraceId(traceId);

            log.info("向量标签搜索完成: family={}, query={}, matches={}, TraceID={}",
                    familyId, query, sortedMatches.size(), traceId);

            return result;

        } catch (Exception e) {
            log.error("向量标签搜索失败: family={}, query={}, error={}, TraceID={}",
                    request.getFamilyId(), request.getQuery(), e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 获取文件的向量标签
     */
    @TraceLog(value = "获取文件标签", module = "vectortag", type = "GET")
    public List<VectorTag> getFileVectorTags(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<VectorTag> tags = fileVectorTags.getOrDefault(fileId, new ArrayList<>());

            // 过滤家庭数据
            List<VectorTag> familyTags = tags.stream()
                    .filter(tag -> tag.getFamilyId().equals(familyId))
                    .sorted(Comparator.comparing(VectorTag::getSimilarityScore).reversed())
                    .collect(Collectors.toList());

            log.info("获取文件标签成功: fileId={}, tags={}, TraceID={}",
                    fileId, familyTags.size(), traceId);

            return familyTags;

        } catch (Exception e) {
            log.error("获取文件标签失败: fileId={}, error={}, TraceID={}",
                    fileId, e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 获取家庭标签统计
     */
    @TraceLog(value = "获取标签统计", module = "vectortag", type = "STATS")
    public FamilyTagStats getFamilyTagStats(String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            Map<String, Integer> tagCounts = familyTagStats.getOrDefault(familyId, new HashMap<>());

            // 计算总标签数和文件数
            int totalTags = tagCounts.values().stream().mapToInt(Integer::intValue).sum();
            long totalFiles = fileVectorTags.values().stream()
                    .mapToLong(tags -> tags.stream().filter(tag -> tag.getFamilyId().equals(familyId)).count())
                    .sum();

            // 获取热门标签（使用频率最高的前10个）
            List<String> popularTags = tagCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            FamilyTagStats stats = new FamilyTagStats();
            stats.setFamilyId(familyId);
            stats.setTotalTags(totalTags);
            stats.setTotalFiles((int) totalFiles);
            stats.setUniqueTagCount(tagCounts.size());
            stats.setTagDistribution(tagCounts);
            stats.setPopularTags(popularTags);
            stats.setLastUpdated(LocalDateTime.now());

            return stats;

        } catch (Exception e) {
            log.error("获取标签统计失败: familyId={}, error={}, TraceID={}",
                    familyId, e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 删除文件的向量标签
     */
    @TraceLog(value = "删除文件标签", module = "vectortag", type = "DELETE")
    public boolean deleteFileVectorTags(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<VectorTag> removedTags = fileVectorTags.remove(fileId);

            if (removedTags != null) {
                // 更新家庭标签统计
                Map<String, Integer> tagCounts = familyTagStats.get(familyId);
                if (tagCounts != null) {
                    for (VectorTag tag : removedTags) {
                        if (tag.getFamilyId().equals(familyId)) {
                            tagCounts.compute(tag.getTagName(), (k, v) ->
                                v != null && v > 1 ? v - 1 : null);
                        }
                    }
                }

                log.info("删除文件标签成功: fileId={}, tags={}, TraceID={}",
                        fileId, removedTags.size(), traceId);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("删除文件标签失败: fileId={}, error={}, TraceID={}",
                    fileId, e.getMessage(), traceId);
            return false;
        }
    }

    // 私有方法实现

    /**
     * 内容预处理
     */
    private String preprocessContent(String content) {
        return content.replaceAll("\\s+", " ")
                .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "")
                .trim();
    }

    /**
     * 生成内容向量（模拟实现）
     */
    private List<Double> generateContentVector(String content) {
        return generateMockEmbedding(content);
    }

    /**
     * 生成关键词向量（模拟实现）
     */
    private List<Double> generateKeywordVector(String keyword) {
        return generateMockEmbedding(keyword);
    }

    /**
     * 生成查询向量（模拟实现）
     */
    private List<Double> generateQueryVector(String query) {
        return generateMockEmbedding(query);
    }

    /**
     * 模拟向量生成
     */
    private List<Double> generateMockEmbedding(String text) {
        Random random = new Random(text.hashCode());
        List<Double> vector = new ArrayList<>();
        for (int i = 0; i < vectorDimension; i++) {
            vector.add(random.nextGaussian());
        }
        return vector;
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String content) {
        // 简化实现：分词并过滤停用词
        String[] words = content.split("\\s+");
        return Arrays.stream(words)
                .filter(word -> word.length() >= 2)
                .filter(word -> !isStopWord(word))
                .distinct()
                .limit(20) // 最多20个关键词
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为停用词
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
            "的", "是", "在", "了", "和", "有", "为", "个", "与", "或",
            "a", "an", "the", "is", "are", "was", "were", "and", "or"
        );
        return stopWords.contains(word.toLowerCase());
    }

    /**
     * 确定标签类型
     */
    private TagType determineTagType(String keyword) {
        // 简化实现：根据关键词特征判断类型
        if (keyword.matches("\\d{4}")) {
            return TagType.DATE;
        } else if (keyword.matches("[a-zA-Z]+\\.(pdf|doc|txt|jpg|png)")) {
            return TagType.FILE_FORMAT;
        } else if (keyword.length() <= 4) {
            return TagType.KEYWORD;
        } else {
            return TagType.CONTENT;
        }
    }

    /**
     * 推荐相似标签
     */
    private List<String> recommendSimilarTags(List<Double> contentVector, String familyId) {
        Map<String, Integer> familyTagCounts = familyTagStats.getOrDefault(familyId, new HashMap<>());

        return familyTagCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2) // 至少被使用过2次
                .map(Map.Entry::getKey)
                .filter(tagName -> tagVectors.containsKey(tagName))
                .sorted((tag1, tag2) -> {
                    double sim1 = calculateCosineSimilarity(contentVector, tagVectors.get(tag1));
                    double sim2 = calculateCosineSimilarity(contentVector, tagVectors.get(tag2));
                    return Double.compare(sim2, sim1);
                })
                .limit(5) // 最多推荐5个
                .collect(Collectors.toList());
    }

    /**
     * 更新家庭标签统计
     */
    private void updateFamilyTagStats(String familyId, List<VectorTag> vectorTags) {
        Map<String, Integer> tagCounts = familyTagStats.computeIfAbsent(familyId, k -> new HashMap<>());

        for (VectorTag tag : vectorTags) {
            tagCounts.put(tag.getTagName(), tagCounts.getOrDefault(tag.getTagName(), 0) + 1);
        }
    }

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += Math.pow(vector1.get(i), 2);
            norm2 += Math.pow(vector2.get(i), 2);
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}