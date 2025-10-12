package com.haven.storage.service;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.knowledge.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 个人知识库服务
 * <p>
 * 🎯 核心功能：
 * - 文档向量化和存储
 * - 知识库构建和管理
 * - 语义搜索和检索
 * - 知识图谱构建
 * <p>
 * 💡 使用场景：
 * - 个人学习笔记管理
 * - 工作文档知识库
 * - 家庭资料整理
 * - AI助手知识源
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class PersonalKnowledgeBaseService {

    @Value("${knowledge.embedding.model:text-embedding-ada-002}")
    private String embeddingModel;

    @Value("${knowledge.chunk.size:512}")
    private Integer chunkSize;

    @Value("${knowledge.vector.dimension:1536}")
    private Integer vectorDimension;

    // 知识库存储
    private final Map<String, KnowledgeBase> knowledgeBases = new ConcurrentHashMap<>();

    // 文档向量存储
    private final Map<String, List<DocumentVector>> documentVectors = new ConcurrentHashMap<>();

    // 知识图谱存储
    private final Map<String, KnowledgeGraph> knowledgeGraphs = new ConcurrentHashMap<>();

    /**
     * 创建个人知识库
     */
    @TraceLog(value = "创建知识库", module = "knowledge", type = "CREATE")
    public KnowledgeBase createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            String knowledgeBaseId = UUID.randomUUID().toString();

            KnowledgeBase knowledgeBase = new KnowledgeBase();
            knowledgeBase.setKnowledgeBaseId(knowledgeBaseId);
            knowledgeBase.setFamilyId(request.getFamilyId());
            knowledgeBase.setName(request.getName());
            knowledgeBase.setDescription(request.getDescription());
            knowledgeBase.setCategory(request.getCategory());
            knowledgeBase.setCreatedBy(request.getCreatorUserId());
            // setCreatedAt会通过BaseEntity自动设置
            knowledgeBase.setLastUpdated(LocalDateTime.now());
            knowledgeBase.setDocumentCount(0);
            knowledgeBase.setVectorCount(0);

            // 初始化配置
            KnowledgeBaseConfig config = new KnowledgeBaseConfig();
            config.setEmbeddingModel(embeddingModel);
            config.setChunkSize(chunkSize);
            config.setVectorDimension(vectorDimension);
            config.setLanguage("zh-CN");
            config.setEnableSemanticSearch(true);
            config.setEnableKnowledgeGraph(false); // 默认关闭知识图谱
            knowledgeBase.setConfig(config);

            knowledgeBases.put(knowledgeBaseId, knowledgeBase);

            log.info("知识库创建成功: id={}, name={}, family={}, TraceID={}",
                    knowledgeBaseId, request.getName(), request.getFamilyId(), traceId);

            return knowledgeBase;

        } catch (Exception e) {
            log.error("创建知识库失败: name={}, error={}, TraceID={}",
                    request.getName(), e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 向知识库添加文档
     */
    @TraceLog(value = "添加知识库文档", module = "knowledge", type = "ADD_DOCUMENT")
    public AddDocumentResult addDocument(String knowledgeBaseId, AddDocumentRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            KnowledgeBase knowledgeBase = knowledgeBases.get(knowledgeBaseId);
            if (knowledgeBase == null) {
                return AddDocumentResult.error("知识库不存在", traceId);
            }

            String documentId = UUID.randomUUID().toString();

            // 创建文档记录
            KnowledgeDocument document = new KnowledgeDocument();
            document.setDocumentId(documentId);
            document.setKnowledgeBaseId(knowledgeBaseId);
            document.setFamilyId(knowledgeBase.getFamilyId());
            document.setTitle(request.getTitle());
            document.setContent(request.getContent());
            document.setSourceUrl(request.getSourceUrl());
            document.setFileId(request.getFileId()); // 关联文件系统中的文件
            document.setAddedBy(request.getUserId());
            document.setAddedAt(LocalDateTime.now());
            document.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());

            // 文档分块
            List<DocumentChunk> chunks = splitDocumentIntoChunks(document, knowledgeBase.getConfig());

            // 向量化处理（这里使用模拟实现）
            List<DocumentVector> vectors = vectorizeDocumentChunks(chunks, knowledgeBase.getConfig());

            // 存储向量
            documentVectors.computeIfAbsent(knowledgeBaseId, k -> new ArrayList<>()).addAll(vectors);

            // 更新知识库统计
            knowledgeBase.setDocumentCount(knowledgeBase.getDocumentCount() + 1);
            knowledgeBase.setVectorCount(knowledgeBase.getVectorCount() + vectors.size());
            knowledgeBase.setLastUpdated(LocalDateTime.now());

            log.info("文档添加成功: kbId={}, docId={}, chunks={}, vectors={}, TraceID={}",
                    knowledgeBaseId, documentId, chunks.size(), vectors.size(), traceId);

            return AddDocumentResult.success(document, vectors.size(), traceId);

        } catch (Exception e) {
            log.error("添加文档失败: kbId={}, title={}, error={}, TraceID={}",
                    knowledgeBaseId, request.getTitle(), e.getMessage(), traceId);
            return AddDocumentResult.error(e.getMessage(), traceId);
        }
    }

    /**
     * 知识库语义搜索
     */
    @TraceLog(value = "知识库搜索", module = "knowledge", type = "SEARCH")
    public KnowledgeSearchResult searchKnowledge(String knowledgeBaseId, KnowledgeSearchRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            KnowledgeBase knowledgeBase = knowledgeBases.get(knowledgeBaseId);
            if (knowledgeBase == null) {
                throw new IllegalArgumentException("知识库不存在");
            }

            // 获取查询向量（模拟实现）
            List<Double> queryVector = generateQueryVector(request.getQuery(), knowledgeBase.getConfig());

            // 向量相似性搜索
            List<DocumentVector> allVectors = documentVectors.getOrDefault(knowledgeBaseId, new ArrayList<>());
            List<SearchResultItem> searchResults = performVectorSearch(queryVector, allVectors, request.getTopK());

            KnowledgeSearchResult result = new KnowledgeSearchResult();
            result.setKnowledgeBaseId(knowledgeBaseId);
            result.setQuery(request.getQuery());
            result.setResults(searchResults);
            result.setTotalResults(searchResults.size());
            result.setSearchTime(System.currentTimeMillis());
            result.setTraceId(traceId);

            log.info("知识库搜索完成: kbId={}, query={}, results={}, TraceID={}",
                    knowledgeBaseId, request.getQuery(), searchResults.size(), traceId);

            return result;

        } catch (Exception e) {
            log.error("知识库搜索失败: kbId={}, query={}, error={}, TraceID={}",
                    knowledgeBaseId, request.getQuery(), e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 获取知识库列表
     */
    @TraceLog(value = "获取知识库列表", module = "knowledge", type = "LIST")
    public List<KnowledgeBase> getKnowledgeBases(String familyId, String userId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<KnowledgeBase> familyKnowledgeBases = knowledgeBases.values().stream()
                    .filter(kb -> kb.getFamilyId().equals(familyId))
                    .sorted(Comparator.comparing(KnowledgeBase::getLastUpdated).reversed())
                    .collect(Collectors.toList());

            log.info("知识库列表获取成功: family={}, count={}, TraceID={}",
                    familyId, familyKnowledgeBases.size(), traceId);

            return familyKnowledgeBases;

        } catch (Exception e) {
            log.error("获取知识库列表失败: family={}, error={}, TraceID={}",
                    familyId, e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 删除知识库
     */
    @TraceLog(value = "删除知识库", module = "knowledge", type = "DELETE")
    public boolean deleteKnowledgeBase(String knowledgeBaseId, String userId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            KnowledgeBase knowledgeBase = knowledgeBases.remove(knowledgeBaseId);
            if (knowledgeBase == null) {
                return false;
            }

            // 删除相关向量数据
            documentVectors.remove(knowledgeBaseId);

            // 删除知识图谱数据
            knowledgeGraphs.remove(knowledgeBaseId);

            log.info("知识库删除成功: id={}, name={}, TraceID={}",
                    knowledgeBaseId, knowledgeBase.getName(), traceId);

            return true;

        } catch (Exception e) {
            log.error("删除知识库失败: id={}, error={}, TraceID={}",
                    knowledgeBaseId, e.getMessage(), traceId);
            return false;
        }
    }

    /**
     * 获取知识库统计信息
     */
    @TraceLog(value = "获取知识库统计", module = "knowledge", type = "STATS")
    public KnowledgeBaseStats getKnowledgeBaseStats(String knowledgeBaseId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            KnowledgeBase knowledgeBase = knowledgeBases.get(knowledgeBaseId);
            if (knowledgeBase == null) {
                throw new IllegalArgumentException("知识库不存在");
            }

            List<DocumentVector> vectors = documentVectors.getOrDefault(knowledgeBaseId, new ArrayList<>());

            KnowledgeBaseStats stats = new KnowledgeBaseStats();
            stats.setKnowledgeBaseId(knowledgeBaseId);
            stats.setDocumentCount(knowledgeBase.getDocumentCount());
            stats.setVectorCount(vectors.size());
            stats.setLastUpdated(knowledgeBase.getLastUpdated());

            // 计算标签统计
            Map<String, Integer> tagStats = vectors.stream()
                    .flatMap(v -> ((List<String>) v.getMetadata().getOrDefault("tags", new ArrayList<String>())).stream())
                    .collect(Collectors.groupingBy(
                            tag -> (String) tag,
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                    ));
            stats.setTagStats(tagStats);

            return stats;

        } catch (Exception e) {
            log.error("获取知识库统计失败: id={}, error={}, TraceID={}",
                    knowledgeBaseId, e.getMessage(), traceId);
            throw e;
        }
    }

    // 私有方法实现

    private List<DocumentChunk> splitDocumentIntoChunks(KnowledgeDocument document, KnowledgeBaseConfig config) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String content = document.getContent();
        int chunkSize = config.getChunkSize();

        // 简化实现：按字符数分块
        for (int i = 0; i < content.length(); i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, content.length());
            String chunkContent = content.substring(i, endIndex);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setChunkId(UUID.randomUUID().toString());
            chunk.setDocumentId(document.getDocumentId());
            chunk.setChunkIndex(chunks.size());
            chunk.setContent(chunkContent);
            chunk.setStartOffset(i);
            chunk.setEndOffset(endIndex);

            chunks.add(chunk);
        }

        return chunks;
    }

    private List<DocumentVector> vectorizeDocumentChunks(List<DocumentChunk> chunks, KnowledgeBaseConfig config) {
        List<DocumentVector> vectors = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            // 模拟向量化过程
            List<Double> vector = generateMockEmbedding(chunk.getContent(), config.getVectorDimension());

            DocumentVector documentVector = new DocumentVector();
            documentVector.setVectorId(UUID.randomUUID().toString());
            documentVector.setDocumentId(chunk.getDocumentId());
            documentVector.setChunkId(chunk.getChunkId());
            documentVector.setVector(vector);
            documentVector.setContent(chunk.getContent());

            // 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkIndex", chunk.getChunkIndex());
            metadata.put("startOffset", chunk.getStartOffset());
            metadata.put("endOffset", chunk.getEndOffset());
            documentVector.setMetadata(metadata);

            vectors.add(documentVector);
        }

        return vectors;
    }

    private List<Double> generateQueryVector(String query, KnowledgeBaseConfig config) {
        // 模拟查询向量生成
        return generateMockEmbedding(query, config.getVectorDimension());
    }

    private List<Double> generateMockEmbedding(String text, int dimension) {
        // 模拟向量生成：基于文本内容生成伪随机向量
        Random random = new Random(text.hashCode());
        List<Double> vector = new ArrayList<>();
        for (int i = 0; i < dimension; i++) {
            vector.add(random.nextGaussian());
        }
        return vector;
    }

    private List<SearchResultItem> performVectorSearch(List<Double> queryVector,
                                                       List<DocumentVector> allVectors, int topK) {
        return allVectors.stream()
                .map(docVector -> {
                    double similarity = calculateCosineSimilarity(queryVector, docVector.getVector());

                    SearchResultItem item = new SearchResultItem();
                    item.setDocumentId(docVector.getDocumentId());
                    item.setChunkId(docVector.getChunkId());
                    item.setContent(docVector.getContent());
                    item.setSimilarity(similarity);
                    item.setMetadata(docVector.getMetadata());

                    return item;
                })
                .sorted(Comparator.comparing(SearchResultItem::getSimilarity).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

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