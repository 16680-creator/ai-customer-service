package com.aics.chat.rag.rerank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * SiliconFlowRerankService 单元测试
 *
 * <p>使用 MockRestServiceServer 拦截 RestClient 调用，模拟硅基流动 Rerank API 响应，
 * 覆盖正常解析、API Key 为空降级、超时降级、5xx 降级、空文档降级与 minScore 过滤。</p>
 */
class SiliconFlowRerankServiceTest {

    private static final String BASE_URL = "https://api.siliconflow.cn";
    private static final String API_KEY = "test-api-key";

    private RerankProperties properties;
    private MockRestServiceServer server;
    private SiliconFlowRerankService service;

    @BeforeEach
    void setUp() {
        properties = new RerankProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setApiKey(API_KEY);
        properties.setModel("BAAI/bge-reranker-v2-m3");
        properties.setMinScore(0.7);
        properties.setTimeoutMs(5000);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new SiliconFlowRerankService(properties, builder,
                io.micrometer.observation.ObservationRegistry.create());
    }

    private List<Document> sampleDocuments() {
        return List.of(
                new Document("doc0: 退货政策说明"),
                new Document("doc1: 物流时效说明"),
                new Document("doc2: 售后保障说明"));
    }

    @Test
    @DisplayName("正常返回：解析 results 中的 index 与 relevanceScore")
    void rerank_withValidResponse_parsesIndexAndRelevanceScore() {
        server.expect(requestTo(BASE_URL + "/v1/rerank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andRespond(withSuccess("""
                        {
                          "id": "rerank-123",
                          "model": "BAAI/bge-reranker-v2-m3",
                          "results": [
                            {"index": 2, "relevance_score": 0.95, "document": {"text": "doc2: 售后保障说明"}},
                            {"index": 0, "relevance_score": 0.88, "document": {"text": "doc0: 退货政策说明"}},
                            {"index": 1, "relevance_score": 0.72, "document": {"text": "doc1: 物流时效说明"}}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<RerankResultItem> result = service.rerank("退货政策", sampleDocuments(), 5).block();

        assertThat(result).isNotNull().hasSize(3);
        // 按相关度分数降序
        assertThat(result.get(0).getIndex()).isEqualTo(2);
        assertThat(result.get(0).getRelevanceScore()).isEqualTo(0.95);
        assertThat(result.get(0).getText()).contains("售后保障");
        assertThat(result.get(1).getIndex()).isEqualTo(0);
        assertThat(result.get(1).getRelevanceScore()).isEqualTo(0.88);
        assertThat(result.get(2).getIndex()).isEqualTo(1);
        assertThat(result.get(2).getRelevanceScore()).isEqualTo(0.72);
        server.verify();
    }

    @Test
    @DisplayName("API Key 为空：直接降级返回空（不发起 HTTP 请求）")
    void rerank_withEmptyApiKey_degradesToEmpty() {
        properties.setApiKey("");

        List<RerankResultItem> result = service.rerank("退货政策", sampleDocuments(), 5).block();

        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("API Key 为空白字符串：同样降级返回空")
    void rerank_withBlankApiKey_degradesToEmpty() {
        properties.setApiKey("   ");

        List<RerankResultItem> result = service.rerank("退货政策", sampleDocuments(), 5).block();

        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("超时：响应超过 timeoutMs 时降级返回空")
    void rerank_withTimeout_degradesToEmpty() {
        properties.setTimeoutMs(100);
        server.expect(requestTo(BASE_URL + "/v1/rerank"))
                .andRespond(request -> {
                    // 响应延迟 300ms 超过 timeoutMs=100ms，且返回合法 JSON + content-type，
                    // 确保只有真正的超时降级才能通过断言（响应转换失败也会降级，会掩盖超时问题）
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    MockClientHttpResponse response = new MockClientHttpResponse(
                            "{\"results\": [{\"index\": 0, \"relevance_score\": 0.95}]}".getBytes(StandardCharsets.UTF_8),
                            HttpStatus.OK);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                });

        List<RerankResultItem> result = service.rerank("退货政策", sampleDocuments(), 5).block();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("5xx 服务端异常：降级返回空")
    void rerank_withServerError_degradesToEmpty() {
        server.expect(requestTo(BASE_URL + "/v1/rerank"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        List<RerankResultItem> result = service.rerank("退货政策", sampleDocuments(), 5).block();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("空文档列表：直接返回空（不发起 HTTP 请求）")
    void rerank_withEmptyDocuments_returnsEmpty() {
        List<RerankResultItem> result = service.rerank("退货政策", List.of(), 5).block();

        assertThat(result).isNull();
        server.verify();
    }

    @Test
    @DisplayName("minScore 过滤：低于阈值(0.7)的结果被过滤，并按分数降序")
    void rerank_withLowScoreItems_filtersBelowMinScore() {
        server.expect(requestTo(BASE_URL + "/v1/rerank"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            {"index": 0, "relevance_score": 0.95, "document": {"text": "doc0"}},
                            {"index": 1, "relevance_score": 0.65, "document": {"text": "doc1"}},
                            {"index": 2, "relevance_score": 0.80, "document": {"text": "doc2"}}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<RerankResultItem> result = service.rerank("退货政策", sampleDocuments(), 5).block();

        assertThat(result).isNotNull().hasSize(2);
        // 0.65 被过滤，剩余按降序：0.95 -> 0.80
        assertThat(result.get(0).getIndex()).isEqualTo(0);
        assertThat(result.get(0).getRelevanceScore()).isEqualTo(0.95);
        assertThat(result.get(1).getIndex()).isEqualTo(2);
        assertThat(result.get(1).getRelevanceScore()).isEqualTo(0.80);
        server.verify();
    }
}
