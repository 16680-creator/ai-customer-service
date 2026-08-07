package com.aics.search.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索服务实现 —— 基于 Chroma 向量库
 *
 * <p>全文检索已从 Elasticsearch 切换到 Chroma：
 * <ul>
 *   <li>检索：similaritySearch 语义相似度 + metadata.knowledgeBase 过滤（index 参数即知识库标识）</li>
 *   <li>入库：vectorStore.add，打上 knowledgeBase 标签</li>
 *   <li>删除：按 knowledgeBase 过滤删除</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final VectorStore vectorStore;

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
            log.info("索引删除成功: knowledgeBase={}", index);
            return Result.success();
        } catch (Exception e) {
            log.error("索引删除失败: knowledgeBase={}", index, e);
            throw new BusinessException(ResultCode.SEARCH_INDEX_NOT_FOUND, "索引删除失败: " + e.getMessage());
        }
    }
}