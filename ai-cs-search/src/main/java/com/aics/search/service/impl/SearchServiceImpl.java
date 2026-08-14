package com.aics.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.search.service.SearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 搜索服务实现 —— 基于 Chroma 向量库 + Elasticsearch 混合检索
 *
 * <p>双写双删：
 * <ul>
 *   <li>检索：similaritySearch 语义相似度 + metadata.knowledgeBase 过滤（index 参数即知识库标识）；
 *       混合检索见 HybridSearchServiceImpl（ES 关键词 + 向量语义，RRF 融合）</li>
 *   <li>入库：vectorStore.add 后同步写 ES（先按 documentId 删除旧文档再索引）</li>
 *   <li>删除：按 knowledgeBase 删除 Chroma 后同步删除 ES（deleteByQuery）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final VectorStore vectorStore;
    private final ElasticsearchClient esClient;

    /** 启动时预创建默认知识库的 ES 索引（幂等，失败仅告警不影响启动） */
    @PostConstruct
    public void initDefaultEsIndex() {
        createEsIndexIfNeeded("knowledge");
    }

    @Override
    public Result<List<Map<String, Object>>> search(String index, String query, int page, int size) {
        log.info("全文搜索(Chroma): knowledgeBase={}, query={}, page={}, size={}", index, query, page, size);
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(Math.max(1, size))
                    .filterExpression("knowledgeBase == '" + index + "'")
                    .build();
            List<Document> docs = vectorStore.similaritySearch(searchRequest);
            List<Map<String, Object>> results = new ArrayList<>();
            for (Document doc : docs) {
                Map<String, Object> item = new LinkedHashMap<>();
                Object documentId = doc.getMetadata().get("documentId");
                item.put("id", documentId != null ? documentId : doc.getId());
                Object title = doc.getMetadata().get("title");
                item.put("title", title != null ? title : "");
                item.put("content", doc.getText());
                item.put("summary", doc.getMetadata().getOrDefault("summary", ""));
                item.put("tags", doc.getMetadata().getOrDefault("tags", ""));
                item.put("docType", doc.getMetadata().getOrDefault("docType", ""));
                item.put("categoryId", doc.getMetadata().get("categoryId"));
                item.put("status", doc.getMetadata().get("status"));
                item.put("_score", doc.getScore() != null ? doc.getScore() : 0.0);
                results.add(item);
            }
            log.info("搜索完成: knowledgeBase={}, 结果数={}", index, results.size());
            return Result.success(results);
        } catch (Exception e) {
            log.error("搜索失败: knowledgeBase={}, query={}", index, query, e);
            throw new BusinessException(ResultCode.SEARCH_QUERY_FAIL, "搜索查询失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> createIndex(String index, Map<String, Object> mappings) {
        // Chroma 集合由服务统一管理（aics-knowledge），无需按 index 创建索引
        log.info("Chroma 模式：无需创建索引, knowledgeBase={}", index);
        return Result.success();
    }

    @Override
    public Result<Void> indexDocument(String index, Map<String, Object> document) {
        log.info("索引文档(Chroma): knowledgeBase={}", index);
        try {
            Object content = document.get("content");
            if (content == null) {
                content = document.get("text");
            }
            if (content == null || content.toString().isBlank()) {
                throw new BusinessException(ResultCode.SEARCH_INDEX_CREATE_FAIL, "文档内容不能为空");
            }
            Document doc = new Document(content.toString());
            doc.getMetadata().put("knowledgeBase", index);
            for (Map.Entry<String, Object> entry : document.entrySet()) {
                if ("content".equals(entry.getKey()) || "text".equals(entry.getKey())) {
                    continue;
                }
                if (entry.getValue() != null) {
                    doc.getMetadata().put(entry.getKey(), entry.getValue());
                }
            }
            vectorStore.add(List.of(doc));
            // 同步写入 Elasticsearch（先按 documentId 删除旧文档，再索引新文档）
            indexToEs(index, document);
            log.info("文档索引成功: knowledgeBase={}, title={}", index, document.get("title"));
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档索引失败: knowledgeBase={}", index, e);
            throw new BusinessException(ResultCode.SEARCH_INDEX_CREATE_FAIL, "文档索引失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteIndex(String index) {
        log.info("删除索引(Chroma): knowledgeBase={}", index);
        try {
            vectorStore.delete("knowledgeBase == '" + index + "'");
            // 同步删除 ES 中该 knowledgeBase 的所有文档
            deleteEsDocuments(index);
            log.info("索引删除成功: knowledgeBase={}", index);
            return Result.success();
        } catch (Exception e) {
            log.error("索引删除失败: knowledgeBase={}", index, e);
            throw new BusinessException(ResultCode.SEARCH_INDEX_NOT_FOUND, "索引删除失败: " + e.getMessage());
        }
    }

    // ==================== Elasticsearch 同步 ====================

    /**
     * 同步索引文档到 ES：幂等创建索引后，先按 documentId 删除旧文档，再索引新文档。
     * ES 异常不影响 Chroma 结果，仅记录告警日志。
     */
    private void indexToEs(String knowledgeBase, Map<String, Object> document) {
        try {
            createEsIndexIfNeeded(knowledgeBase);
            String indexName = esIndexName(knowledgeBase);
            Object documentId = document.get("documentId");
            String docId = documentId != null ? String.valueOf(documentId) : null;
            // 先按 documentId 删除旧文档（幂等，文档不存在时忽略）
            if (docId != null) {
                try {
                    esClient.delete(d -> d.index(indexName).id(docId));
                } catch (Exception e) {
                    log.debug("ES 旧文档不存在或删除失败（忽略）: docId={}, err={}", docId, e.getMessage());
                }
            }
            // 构建 ES 文档：保留全部字段，补充 knowledgeBase 与默认空 title（multiMatch 需要）
            Map<String, Object> esDoc = new HashMap<>(document);
            esDoc.putIfAbsent("knowledgeBase", knowledgeBase);
            esDoc.putIfAbsent("title", "");
            esClient.index(i -> i
                    .index(indexName)
                    .id(docId != null ? docId : UUID.randomUUID().toString())
                    .document(esDoc));
            log.info("ES 文档索引成功: index={}, docId={}", indexName, docId);
        } catch (Exception e) {
            log.warn("ES 索引同步失败（不影响 Chroma 结果）: knowledgeBase={}, err={}", knowledgeBase, e.getMessage());
        }
    }

    /** 同步删除 ES 中该 knowledgeBase 的所有文档（deleteByQuery） */
    private void deleteEsDocuments(String knowledgeBase) {
        try {
            String indexName = esIndexName(knowledgeBase);
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (!exists) {
                log.info("ES 索引不存在，跳过删除: index={}", indexName);
                return;
            }
            esClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.term(t -> t.field("knowledgeBase").value(knowledgeBase))));
            log.info("ES 文档删除成功: index={}, knowledgeBase={}", indexName, knowledgeBase);
        } catch (Exception e) {
            log.warn("ES 文档删除失败（不影响 Chroma 结果）: knowledgeBase={}, err={}", knowledgeBase, e.getMessage());
        }
    }

    /**
     * 幂等创建 ES 索引（带标题/内容/知识库等字段映射）。
     * 索引已存在（resource_already_exists）时静默跳过，其他异常仅告警。
     */
    private void createEsIndexIfNeeded(String knowledgeBase) {
        try {
            String indexName = esIndexName(knowledgeBase);
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return;
            }
            esClient.indices().create(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties("documentId", p -> p.keyword(k -> k))
                            .properties("title", p -> p.text(t -> t))
                            .properties("content", p -> p.text(t -> t))
                            .properties("summary", p -> p.text(t -> t))
                            .properties("tags", p -> p.keyword(k -> k))
                            .properties("knowledgeBase", p -> p.keyword(k -> k))
                            .properties("docType", p -> p.keyword(k -> k))
                            .properties("page", p -> p.integer(i -> i))));
            log.info("ES 索引创建成功: index={}", indexName);
        } catch (ElasticsearchException e) {
            if (!e.getMessage().contains("resource_already_exists")) {
                log.warn("ES 索引创建失败: err={}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("ES 索引创建失败: err={}", e.getMessage());
        }
    }

    /** ES 索引名 = 知识库标识小写（ES 索引名不允许大写） */
    private String esIndexName(String knowledgeBase) {
        return knowledgeBase == null ? "default" : knowledgeBase.toLowerCase(Locale.ROOT);
    }
}