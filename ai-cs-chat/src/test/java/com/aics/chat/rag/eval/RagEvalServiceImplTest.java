package com.aics.chat.rag.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RagEvalServiceImpl 单元测试：桩数据源（命中期望文档）+ Mock Judge，验证汇总指标与阈值门禁。
 */
class RagEvalServiceImplTest {

    private GoldenCaseLoader loader;
    private RagAnswerJudge judge;
    private EvalGateConfig gateConfig;
    private RagEvalServiceImpl service;
    private List<GoldenCase> cases;

    @BeforeEach
    void setUp() throws Exception {
        loader = new GoldenCaseLoader(new ObjectMapper());
        cases = loader.load("classpath:eval/golden-set.json");
        judge = mock(RagAnswerJudge.class);
        // 门禁扩展配置：默认不配置 P95/Token 阈值，不参与判定（向后兼容）
        gateConfig = new EvalGateConfig();
    }

    private RagEvalDataSource stubDataSource(boolean hit) {
        return (kb, query, topK) -> {
            if (!hit) {
                return List.of();
            }
            List<Document> docs = new ArrayList<>();
            for (GoldenCase c : cases) {
                if (c.getQuestion().equals(query) && c.getExpectedDocumentIds() != null) {
                    for (String id : c.getExpectedDocumentIds()) {
                        docs.add(new Document("doc-" + id, "text", Map.of("documentId", id)));
                    }
                }
            }
            return docs;
        };
    }

    @Test
    @DisplayName("命中全部期望文档: hitRate=1.0, LLM 均分 4.0, passed=true")
    void evaluate_allHit() {
        when(judge.score(anyString(), anyString(), anyString())).thenReturn(4);
        service = new RagEvalServiceImpl(loader, stubDataSource(true), judge, gateConfig);

        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setKnowledgeBase("product-manual");
        req.setHitRateThreshold(0.6);
        req.setLlmScoreThreshold(3.5);

        RagEvalReport report = service.evaluate(req);

        assertThat(report.getMetrics().getHitRate()).isEqualTo(1.0);
        assertThat(report.getAvgLlmScore()).isEqualTo(4.0);
        assertThat(report.isPassed()).isTrue();
        assertThat(report.getCaseResults()).hasSize(20);
    }

    @Test
    @DisplayName("全部未命中: hitRate=0, passed=false")
    void evaluate_noHit() {
        when(judge.score(anyString(), anyString(), anyString())).thenReturn(2);
        service = new RagEvalServiceImpl(loader, stubDataSource(false), judge, gateConfig);

        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setHitRateThreshold(0.6);

        RagEvalReport report = service.evaluate(req);

        assertThat(report.getMetrics().getHitRate()).isZero();
        assertThat(report.isPassed()).isFalse();
    }

    @Test
    @DisplayName("Judge 返回 null: avgLlmScore 为空且不校验 LLM 阈值")
    void evaluate_judgeNull() {
        when(judge.score(anyString(), anyString(), anyString())).thenReturn(null);
        service = new RagEvalServiceImpl(loader, stubDataSource(true), judge, gateConfig);

        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setHitRateThreshold(0.6);
        req.setLlmScoreThreshold(3.5);

        RagEvalReport report = service.evaluate(req);

        assertThat(report.getAvgLlmScore()).isNull();
        assertThat(report.isPassed()).isTrue();
    }

    // ==================== 门禁扩展（spec：P95 延迟与单请求平均 Token 上限） ====================

    @Test
    @DisplayName("门禁：未配置阈值时 P95/Token 只记录不判定（既有门禁行为不变）")
    void evaluate_gateThresholdsNotConfigured() {
        when(judge.score(anyString(), anyString(), anyString())).thenReturn(4);
        service = new RagEvalServiceImpl(loader, stubDataSource(true), judge, gateConfig);

        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setKnowledgeBase("product-manual");
        req.setHitRateThreshold(0.6);
        req.setLlmScoreThreshold(3.5);

        RagEvalReport report = service.evaluate(req);

        // 阈值未配置：passed 仍由既有门禁决定（true），指标值被记录
        assertThat(report.isPassed()).isTrue();
        assertThat(report.getP95LatencyMs()).isNotNull();
        assertThat(report.getCaseResults()).allMatch(r -> r.getDurationMs() != null);
    }

    @Test
    @DisplayName("门禁：P95 延迟超限 → passed=false（即使正确率达标）")
    void evaluate_p95Exceeded_failsGate() throws Exception {
        // 每条用例打分耗时 5ms，保证 P95 > 0，从而超过 0ms 上限
        when(judge.score(anyString(), anyString(), anyString())).thenAnswer(inv -> {
            Thread.sleep(5);
            return 4;
        });
        gateConfig.setP95LatencyMs(0L);   // 上限 0ms，任何耗时都超限
        service = new RagEvalServiceImpl(loader, stubDataSource(true), judge, gateConfig);

        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setKnowledgeBase("product-manual");
        req.setHitRateThreshold(0.6);
        req.setLlmScoreThreshold(3.5);

        RagEvalReport report = service.evaluate(req);

        assertThat(report.isPassed()).isFalse();
    }

    @Test
    @DisplayName("门禁：平均 Token 超限 → passed=false")
    void evaluate_avgTokenExceeded_failsGate() {
        when(judge.score(anyString(), anyString(), anyString())).thenReturn(4);
        when(judge.lastTotalTokens()).thenReturn(500);   // 每条用例 500 token
        gateConfig.setAvgTokensPerRequest(100L);         // 上限 100
        service = new RagEvalServiceImpl(loader, stubDataSource(true), judge, gateConfig);

        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setKnowledgeBase("product-manual");
        req.setHitRateThreshold(0.6);
        req.setLlmScoreThreshold(3.5);

        RagEvalReport report = service.evaluate(req);

        assertThat(report.getAvgTokensPerRequest()).isEqualTo(500.0);
        assertThat(report.isPassed()).isFalse();
    }

    @Test
    @DisplayName("门禁：平均 Token 未超限 → 不阻断门禁")
    void evaluate_avgTokenOk_passes() {
        when(judge.score(anyString(), anyString(), anyString())).thenReturn(4);
        when(judge.lastTotalTokens()).thenReturn(50);   // 每条用例 50 token
        gateConfig.setAvgTokensPerRequest(100L);
        service = new RagEvalServiceImpl(loader, stubDataSource(true), judge, gateConfig);

        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setKnowledgeBase("product-manual");
        req.setHitRateThreshold(0.6);
        req.setLlmScoreThreshold(3.5);

        RagEvalReport report = service.evaluate(req);

        assertThat(report.isPassed()).isTrue();
    }

    @Test
    @DisplayName("门禁：P95 边界值（等于上限）不超限")
    void evaluate_p95EqualToLimit_passes() {
        when(judge.score(anyString(), anyString(), anyString())).thenReturn(4);
        // 上限极大（1 天），实际 P95 必然 <= 上限
        gateConfig.setP95LatencyMs(86_400_000L);
        service = new RagEvalServiceImpl(loader, stubDataSource(true), judge, gateConfig);

        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setKnowledgeBase("product-manual");
        req.setHitRateThreshold(0.6);
        req.setLlmScoreThreshold(3.5);

        RagEvalReport report = service.evaluate(req);

        assertThat(report.isPassed()).isTrue();
        assertThat(report.getP95LatencyMs()).isLessThanOrEqualTo(86_400_000L);
    }
}