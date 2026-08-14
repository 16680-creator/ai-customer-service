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

/**
 * golden 集端到端评估测试（CI 门禁，-Peval profile）。
 *
 * <p>使用与 golden 集一致的桩数据源（命中期望文档），验证完整评估管线
 * （加载 → 检索 → 指标 → LLM 打分 → 汇总 → 门禁）输出正确。</p>
 */
class RagEvaluationTest {

    private RagEvalServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        GoldenCaseLoader loader = new GoldenCaseLoader(new ObjectMapper());
        List<GoldenCase> cases = loader.load("classpath:eval/golden-set.json");
        RagEvalDataSource stub = (kb, query, topK) -> {
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
        RagAnswerJudge stubJudge = (q, a, r) -> 4;
        service = new RagEvalServiceImpl(loader, stub, stubJudge, new EvalGateConfig());
    }

    @Test
    @DisplayName("golden 集（20 条）评估通过阈值门禁")
    void evaluateGoldenSet_passes() {
        RagEvalRequest req = new RagEvalRequest();
        req.setGoldenSetPath("classpath:eval/golden-set.json");
        req.setKnowledgeBase("product-manual");
        req.setMode("VECTOR");
        req.setTopK(5);
        req.setHitRateThreshold(0.9);
        req.setLlmScoreThreshold(3.5);

        RagEvalReport report = service.evaluate(req);

        assertThat(report.getMetrics().getHitRate()).isEqualTo(1.0);
        assertThat(report.getMetrics().getRecallAtK()).isEqualTo(1.0);
        assertThat(report.getAvgLlmScore()).isEqualTo(4.0);
        assertThat(report.getCaseResults()).hasSize(20);
        assertThat(report.isPassed()).isTrue();
    }
}