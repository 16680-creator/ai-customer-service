package com.aics.search.hybrid;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 混合检索服务（ES 关键词 + Chroma 向量，RRF 融合）单元测试
 *
 * <p>Mock ElasticsearchClient 与 VectorStore，验证：
 * <ul>
 *   <li>双路召回 + RRF 融合流程（含 topK 透传）</li>
 *   <li>ES 异常降级为仅向量结果、向量异常降级为仅 ES 结果</li>
 *   <li>两路均异常返回空列表</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class HybridSearchServiceImplTest {

    private static final String KB = "kb1";

    @Mock
    private ElasticsearchClient esClient;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private HybridSearchServiceImpl hybridSearchService;

    @Test
    @DisplayName("双路召回 + RRF 融合：结果按融合分数降序，保留各路排名与字段")
    void hybridSearch_shouldMergeEsAndVectorResults() throws IOException {
        stubEsSearch(esResponse(List.of(
                hit("1", Map.of("title", "ES-1", "content", "内容1", "page", 1, "docType", "pdf"), 2.5),
                hit("2", Map.of("title", "ES-2", "content", "内容2", "page", 2, "docType", "pdf"), 1.8))));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc("2", "向量-2", "向量内容2", 0.95), doc("3", "向量-3", "向量内容3", 0.88)));

        List<HybridSearchResult> results = hybridSearchService.hybridSearch(KB, "蓝牙耳机", 10);

        // id2 双路命中（1/61 + 1/62）最高，其次 id1（1/61），再次 id3（1/62）
        assertEquals(3, results.size());
        assertEquals("2", results.get(0).getDocumentId());
        assertEquals(2, results.get(0).getEsRank());
        assertEquals(1, results.get(0).getVectorRank());
        assertEquals(1.0 / 61 + 1.0 / 62, results.get(0).getScore(), 1e-9);
        assertEquals("ES-2", results.get(0).getTitle(), "ES 路先到者优先保留字段");
        assertEquals("内容2", results.get(0).getContent());
        assertEquals(2, results.get(0).getPage());
        assertEquals("pdf", results.get(0).getDocType());
        assertEquals(KB, results.get(0).getKnowledgeBase());

        assertEquals("1", results.get(1).getDocumentId());
        assertEquals(1, results.get(1).getEsRank());
        assertEquals(0, results.get(1).getVectorRank(), "仅 ES 命中时 vectorRank 应为 0");

        assertEquals("3", results.get(2).getDocumentId());
        assertEquals(0, results.get(2).getEsRank(), "仅向量命中时 esRank 应为 0");
        assertEquals(2, results.get(2).getVectorRank());

        verify(esClient).search(any(Function.class), eq((Class) Map.class));
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("topK 透传：融合结果截断为 topK 条")
    void hybridSearch_shouldRespectTopK() throws IOException {
        stubEsSearch(esResponse(List.of(
                hit("1", Map.of("title", "ES-1"), 2.5),
                hit("2", Map.of("title", "ES-2"), 1.8))));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc("2", "向量-2", "内容2", 0.95), doc("3", "向量-3", "内容3", 0.88)));

        List<HybridSearchResult> results = hybridSearchService.hybridSearch(KB, "蓝牙耳机", 2);

        // id2(1/61+1/62) > id1(1/61) > id3(1/62)，topK=2 截断后返回 [id2, id1]
        assertEquals(2, results.size());
        assertEquals("2", results.get(0).getDocumentId());
        assertEquals("1", results.get(1).getDocumentId());
    }

    @Test
    @DisplayName("ES 异常降级：仅返回向量结果，保留向量排名与分数")
    void hybridSearch_shouldDegradeToVectorWhenEsFails() throws IOException {
        when(esClient.search(any(Function.class), eq((Class) Map.class)))
                .thenThrow(new RuntimeException("ES 连接失败"));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc("2", "向量-2", "向量内容2", 0.95), doc("3", "向量-3", "向量内容3", 0.88)));

        List<HybridSearchResult> results = hybridSearchService.hybridSearch(KB, "蓝牙耳机", 10);

        assertEquals(2, results.size());
        assertEquals("2", results.get(0).getDocumentId());
        assertEquals("3", results.get(1).getDocumentId());
        assertEquals(1, results.get(0).getVectorRank());
        assertEquals(0, results.get(0).getEsRank());
        assertEquals(0.95, results.get(0).getScore(), 1e-6);
    }

    @Test
    @DisplayName("向量异常降级：仅返回 ES 结果，保留 ES 排名与分数")
    void hybridSearch_shouldDegradeToEsWhenVectorFails() throws IOException {
        stubEsSearch(esResponse(List.of(
                hit("1", Map.of("title", "ES-1", "content", "内容1", "page", 1, "docType", "pdf"), 2.5),
                hit("2", Map.of("title", "ES-2", "content", "内容2", "page", 2, "docType", "pdf"), 1.8))));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Chroma 连接失败"));

        List<HybridSearchResult> results = hybridSearchService.hybridSearch(KB, "蓝牙耳机", 10);

        assertEquals(2, results.size());
        assertEquals("1", results.get(0).getDocumentId());
        assertEquals("2", results.get(1).getDocumentId());
        assertEquals(1, results.get(0).getEsRank());
        assertEquals(0, results.get(0).getVectorRank());
        assertEquals(2.5, results.get(0).getScore(), 1e-6);
        assertEquals("ES-1", results.get(0).getTitle());
    }

    @Test
    @DisplayName("两路均异常：返回空列表")
    void hybridSearch_shouldReturnEmptyWhenBothFail() throws IOException {
        when(esClient.search(any(Function.class), eq((Class) Map.class)))
                .thenThrow(new RuntimeException("ES 连接失败"));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Chroma 连接失败"));

        List<HybridSearchResult> results = hybridSearchService.hybridSearch(KB, "蓝牙耳机", 10);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ---------- mock 辅助方法 ----------

    /** 让 esClient.search 返回指定的 ES 响应（匹配 lambda 构建器 + Map.class 双参重载） */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubEsSearch(SearchResponse<Map<String, Object>> response) throws IOException {
        when(esClient.<Map<String, Object>>search(any(Function.class), eq((Class) Map.class))).thenReturn(response);
    }

    /** 构造 ES 响应：hits() → hits() 返回指定命中列表 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private SearchResponse<Map<String, Object>> esResponse(List<Hit<Map<String, Object>>> hits) {
        SearchResponse<Map<String, Object>> response = mock(SearchResponse.class);
        HitsMetadata<Map<String, Object>> hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.hits()).thenReturn(hits);
        when(response.hits()).thenReturn(hitsMetadata);
        return response;
    }

    /** 构造 ES 命中：带 _id、source 与相关性分数 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Hit<Map<String, Object>> hit(String id, Map<String, Object> source, double score) {
        Hit<Map<String, Object>> hit = mock(Hit.class);
        when(hit.id()).thenReturn(id);
        when(hit.source()).thenReturn(source);
        when(hit.score()).thenReturn(score);
        return hit;
    }

    /** 构造向量检索文档：documentId 元数据与向量路分数 */
    private Document doc(String id, String title, String content, Double score) {
        return Document.builder()
                .id(id)
                .text(content)
                .metadata(Map.of("documentId", id, "title", title, "docType", "pdf", "page", 1))
                .score(score)
                .build();
    }
}
