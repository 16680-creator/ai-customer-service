package com.aics.chat.rag.retrieve;

import com.aics.chat.dto.ChatHybridPageVO;
import com.aics.chat.dto.ChatHybridSearchResult;
import com.aics.chat.feign.SearchFeignClient;
import com.aics.chat.rag.graph.GraphRagService;
import com.aics.chat.rag.graph.GraphTriple;
import com.aics.chat.rag.rewrite.QueryRewriteService;
import com.aics.chat.rag.rewrite.RewriteResult;
import com.aics.chat.service.KnowledgeBaseService;
import com.aics.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * HybridRetriever 单元测试：四种检索模式与降级行为。
 */
class HybridRetrieverTest {

    private KnowledgeBaseService knowledgeBaseService;
    private VectorStore vectorStore;
    private SearchFeignClient searchFeignClient;
    private QueryRewriteService queryRewriteService;
    private GraphRagService graphRagService;
    private RagRetrieveProperties properties;
    private HybridRetriever retriever;

    @BeforeEach
    void setUp() {
        knowledgeBaseService = mock(KnowledgeBaseService.class);
        vectorStore = mock(VectorStore.class);
        searchFeignClient = mock(SearchFeignClient.class);
        queryRewriteService = mock(QueryRewriteService.class);
        graphRagService = mock(GraphRagService.class);
        properties = new RagRetrieveProperties();
        retriever = new HybridRetriever(knowledgeBaseService, vectorStore, searchFeignClient,
                queryRewriteService, graphRagService, properties);
    }

    @Test
    @DisplayName("VECTOR 模式: 走本地向量检索")
    void retrieve_vector() {
        when(knowledgeBaseService.search(anyString(), anyString(), anyInt(), any(Double.class)))
                .thenReturn(List.of(doc("v1")));
        RetrieveResult result = retriever.retrieve("kb", "q", RetrievalMode.VECTOR, 5);
        assertThat(result.getMode()).isEqualTo("VECTOR");
        assertThat(result.getDocuments()).hasSize(1);
        assertThat(result.isDegraded()).isFalse();
    }

    @Test
    @DisplayName("HYBRID 但全局未启用: 降级纯向量")
    void retrieve_hybridDisabled_degrade() {
        properties.setHybridEnabled(false);
        when(knowledgeBaseService.search(anyString(), anyString(), anyInt(), any(Double.class)))
                .thenReturn(List.of(doc("v1")));
        RetrieveResult result = retriever.retrieve("kb", "q", RetrievalMode.HYBRID, 5);
        assertThat(result.getMode()).isEqualTo("VECTOR");
        assertThat(result.isDegraded()).isTrue();
    }

    @Test
    @DisplayName("HYBRID 启用: 调搜索服务并转换结果")
    void retrieve_hybrid() {
        properties.setHybridEnabled(true);
        ChatHybridSearchResult r = new ChatHybridSearchResult();
        r.setDocumentId("h1");
        r.setTitle("混合命中");
        r.setContent("内容");
        r.setScore(0.9);
        ChatHybridPageVO page = new ChatHybridPageVO();
        page.setRecords(List.of(r));
        when(searchFeignClient.hybridSearch(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Result.success(page));

        RetrieveResult result = retriever.retrieve("kb", "型号 ABC-123", RetrievalMode.HYBRID, 5);
        assertThat(result.getMode()).isEqualTo("HYBRID");
        assertThat(result.getDocuments()).hasSize(1);
        assertThat(result.getDocuments().get(0).getMetadata()).containsEntry("documentId", "h1");
        assertThat(result.isDegraded()).isFalse();
    }

    @Test
    @DisplayName("HYBRID 搜索服务异常: 降级纯向量")
    void retrieve_hybridFailure_degrade() {
        properties.setHybridEnabled(true);
        when(searchFeignClient.hybridSearch(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("es down"));
        when(knowledgeBaseService.search(anyString(), anyString(), anyInt(), any(Double.class)))
                .thenReturn(List.of(doc("v1")));
        RetrieveResult result = retriever.retrieve("kb", "q", RetrievalMode.HYBRID, 5);
        assertThat(result.getMode()).isEqualTo("VECTOR");
        assertThat(result.isDegraded()).isTrue();
        assertThat(result.getDocuments()).hasSize(1);
    }

    @Test
    @DisplayName("HYBRID_QUERY_REWRITE: 多查询 + HyDE 融合去重")
    void retrieve_rewrite() {
        properties.setRewriteEnabled(true);
        RewriteResult rewrite = new RewriteResult();
        rewrite.setOriginalQuery("那个功能怎么用");
        rewrite.setSubQueries(List.of("退款功能怎么用", "申请退款入口在哪"));
        rewrite.setHydeDocument("假设性文档：用户想了解退款功能的使用方法。");
        when(queryRewriteService.rewrite(anyString())).thenReturn(rewrite);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenAnswer(inv -> List.of(doc("s1"), doc("s2")));

        RetrieveResult result = retriever.retrieve("kb", "那个功能怎么用", RetrievalMode.HYBRID_QUERY_REWRITE, 5);
        assertThat(result.getMode()).isEqualTo("HYBRID_QUERY_REWRITE");
        assertThat(result.getDocuments()).isNotEmpty();
    }

    @Test
    @DisplayName("GRAPH_RAG 命中: 图谱上下文置于最前")
    void retrieve_graphHit() {
        properties.setGraphEnabled(true);
        GraphTriple t = new GraphTriple();
        t.setId(1L);
        t.setSubject("退款政策");
        t.setPredicate("指向");
        t.setObject("申请入口");
        t.setKnowledgeBase("kb");
        when(graphRagService.retrieveWithGraph(anyString(), anyString())).thenReturn(List.of(t));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc("v1")));

        RetrieveResult result = retriever.retrieve("kb", "退款要多久", RetrievalMode.GRAPH_RAG, 5);
        assertThat(result.getDocuments()).isNotEmpty();
        assertThat(result.getDocuments().get(0).getMetadata()).containsEntry("source", "graph");
    }

    @Test
    @DisplayName("GRAPH_RAG 未命中: 降级普通检索")
    void retrieve_graphNoHit() {
        properties.setGraphEnabled(true);
        when(graphRagService.retrieveWithGraph(anyString(), anyString())).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc("v1")));
        RetrieveResult result = retriever.retrieve("kb", "q", RetrievalMode.GRAPH_RAG, 5);
        assertThat(result.isDegraded()).isTrue();
        assertThat(result.getDocuments()).hasSize(1);
    }

    private Document doc(String id) {
        return new Document(id, "text-" + id, Map.of("documentId", id));
    }
}