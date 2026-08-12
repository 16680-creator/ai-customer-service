package com.aics.search.hybrid;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 混合检索服务实现 —— "关键词 + 语义"双路召回再融合。
 *
 * <h3>学习要点（技术：Hybrid Search / BM25 / 降级）</h3>
 * <ul>
 *   <li><b>为什么双路</b>：ES 关键词检索(BM25 multiMatch)对精确串（型号/编号/专有名词）强，
 *       对同义改写弱；向量检索（bge-m3）反之。双路召回可互补。</li>
 *   <li><b>ES 路</b>：{@code multiMatch(title^2, content)} 按 knowledgeBase 过滤取 Top-20，
 *       title 权重翻倍因为标题更关键。</li>
 *   <li><b>向量路</b>：Chroma similaritySearch，同样按 knowledgeBase 过滤取 Top-20。</li>
 *   <li><b>RRF 融合</b>：两路各自排名后由 {@link RrfMerger} 倒数排名融合，
 *       不依赖分数可比性（BM25 分与向量距离量纲不同，不能直接相加）。</li>
 *   <li><b>降级</b>：任一路异常自动降级——ES 挂返回向量结果、向量挂返回 ES 结果，
 *       保证搜索功能不整体失效。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchServiceImpl implements HybridSearchService {

    /** 单路召回条数 */
    private static final int RETRIEVE_TOP_K = 20;

    /** RRF 平滑常数（越大排名靠后的结果权重差异越小） */
    private static final int RRF_K = 60;

    private final ElasticsearchClient esClient;
    private final VectorStore vectorStore;

    @Override
    public List<HybridSearchResult> hybridSearch(String knowledgeBase, String query, int topK) {
        log.info("混合检索开始: knowledgeBase={}, query={}, topK={}", knowledgeBase, query, topK);
        List<RankedItem> esItems = esSearch(knowledgeBase, query);
        List<RankedItem> vectorItems = vectorSearch(knowledgeBase, query);

        if (esItems.isEmpty() && vectorItems.isEmpty()) {
            log.warn("混合检索两路均无结果: knowledgeBase={}, query={}", knowledgeBase, query);
            return Collections.emptyList();
        }
        if (esItems.isEmpty()) {
            log.info("ES 路无结果/异常，降级为仅向量结果: knowledgeBase={}, 命中={}", knowledgeBase, vectorItems.size());
            return toResults(vectorItems);
        }
        if (vectorItems.isEmpty()) {
            log.info("向量路无结果/异常，降级为仅 ES 结果: knowledgeBase={}, 命中={}", knowledgeBase, esItems.size());
            return toResults(esItems);
        }

        List<RankedItem> merged = RrfMerger.merge(esItems, vectorItems, topK, RRF_K);
        log.info("混合检索完成: knowledgeBase={}, 融合结果={} 条", knowledgeBase, merged.size());
        return toResults(merged);
    }

    /**
     * ES 关键词检索：multiMatch(title^2, content) + knowledgeBase 过滤，取 Top-20。
     * 异常时返回空列表（调用方降级为仅向量结果）。
     */
    private List<RankedItem> esSearch(String knowledgeBase, String query) {
        try {
            String indexName = esIndexName(knowledgeBase);
            @SuppressWarnings({"unchecked", "rawtypes"})
            SearchResponse<Map<String, Object>> response = esClient.search(s -> s
                            .index(indexName)
                            .size(RETRIEVE_TOP_K)
                            .query(q -> q.bool(b -> b
                                    .filter(f -> f.term(t -> t.field("knowledgeBase").value(knowledgeBase)))
                                    .must(m -> m.multiMatch(mm -> mm
                                            .fields("title^2", "content")
                                            .query(query))))),
                    (Class) Map.class);
            List<RankedItem> items = new ArrayList<>();
            int rank = 1;
            for (Hit<Map<String, Object>> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) {
                    continue;
                }
                RankedItem item = new RankedItem();
                item.setId(hit.id());
                item.setRank1(rank++);
                item.setScore(hit.score() != null ? hit.score() : 0.0);
                item.setTitle(strValue(source.get("title")));
                item.setContent(strValue(source.get("content")));
                item.setKnowledgeBase(knowledgeBase);
                item.setPage(intValue(source.get("page")));
                item.setDocType(strValue(source.get("docType")));
                items.add(item);
            }
            log.info("ES 检索完成: knowledgeBase={}, 命中={}", knowledgeBase, items.size());
            return items;
        } catch (Exception e) {
            log.warn("ES 检索失败（降级为仅向量检索）: knowledgeBase={}, err={}", knowledgeBase, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 向量检索：Chroma similaritySearch（bge-m3 语义相似度）+ knowledgeBase 过滤，取 Top-20。
     * 异常时返回空列表（调用方降级为仅 ES 结果）。
     */
    private List<RankedItem> vectorSearch(String knowledgeBase, String query) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(RETRIEVE_TOP_K)
                    .filterExpression("knowledgeBase == '" + knowledgeBase + "'")
                    .build();
            List<Document> docs = vectorStore.similaritySearch(searchRequest);
            List<RankedItem> items = new ArrayList<>();
            int rank = 1;
            for (Document doc : docs) {
                Map<String, Object> metadata = doc.getMetadata();
                RankedItem item = new RankedItem();
                Object documentId = metadata.get("documentId");
                item.setId(documentId != null ? String.valueOf(documentId) : doc.getId());
                item.setRank2(rank++);
                item.setScore(doc.getScore() != null ? doc.getScore() : 0.0);
                item.setTitle(strValue(metadata.get("title")));
                item.setContent(doc.getText());
                item.setKnowledgeBase(knowledgeBase);
                item.setPage(intValue(metadata.get("page")));
                item.setDocType(strValue(metadata.get("docType")));
                items.add(item);
            }
            log.info("向量检索完成: knowledgeBase={}, 命中={}", knowledgeBase, items.size());
            return items;
        } catch (Exception e) {
            log.warn("向量检索失败（降级为仅 ES 检索）: knowledgeBase={}, err={}", knowledgeBase, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** RankedItem 列表 → HybridSearchResult VO 列表（保留各路排名） */
    private List<HybridSearchResult> toResults(List<RankedItem> items) {
        List<HybridSearchResult> results = new ArrayList<>(items.size());
        for (RankedItem item : items) {
            HybridSearchResult result = new HybridSearchResult();
            result.setDocumentId(item.getId());
            result.setTitle(item.getTitle());
            result.setContent(item.getContent());
            result.setScore(item.getScore());
            result.setKnowledgeBase(item.getKnowledgeBase());
            result.setPage(item.getPage());
            result.setDocType(item.getDocType());
            result.setEsRank(item.getRank1());
            result.setVectorRank(item.getRank2());
            results.add(result);
        }
        return results;
    }

    /** ES 索引名 = 知识库标识小写（ES 索引名不允许大写） */
    private String esIndexName(String knowledgeBase) {
        return knowledgeBase == null ? "default" : knowledgeBase.toLowerCase(Locale.ROOT);
    }

    private String strValue(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                // 非数字，忽略
            }
        }
        return null;
    }
}
