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
    private RagEvalServiceImpl service;
    private List<GoldenCase> cases;

    @BeforeEach
    void setUp() throws Exception {
        loader = new GoldenCaseLoader(new ObjectMapper());
        cases = loader.load("classpath:eval/golden-set.json");
        judge = mock(RagAnswerJudge.class);
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
        service = new RagEvalServiceImpl(loader, stubDataSource(true), judge);

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
        service = new RagEvalServiceImpl(loader, stubDataSource(false), judge);

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
        service = new RagEvalServiceImpl(loader, stubDataSource(true), judge);

        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setHitRateThreshold(0.6);
        req.setLlmScoreThreshold(3.5);

        RagEvalReport report = service.evaluate(req);

        assertThat(report.getAvgLlmScore()).isNull();
        assertThat(report.isPassed()).isTrue();
    }
}