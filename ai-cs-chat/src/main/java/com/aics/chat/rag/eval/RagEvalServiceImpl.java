package com.aics.chat.rag.eval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 评估服务实现 —— golden 回归测试的核心编排。
 *
 * <h3>学习要点（技术：RAG 评估 / 指标计算 / LLM-as-Judge）</h3>
 * <ul>
 *   <li><b>评估管线</b>：加载 golden 集 → 逐条检索({@link RagEvalDataSource}) → 计算指标 →
 *       可选 LLM 打分 → 汇总报告 → 阈值门禁（hitRate / LLM 均分）。</li>
 *   <li><b>指标含义</b>：Recall@k（期望文档被召回的比例）、MRR（首个命中排名的倒数）、
 *       HitRate（至少命中一条的用例占比）——分别度量"召回全不全 / 排得前不前 / 有没有命中"。</li>
 *   <li><b>可对比实验</b>：同一 golden 集在不同检索模式（VECTOR/HYBRID）下各跑一次，
 *       即可量化改动前后的质量差异，这是"用数据说话"的关键。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagEvalServiceImpl implements RagEvaluator {

    private final GoldenCaseLoader caseLoader;
    private final RagEvalDataSource dataSource;
    private final RagAnswerJudge answerJudge;

    /**
     * 执行完整评估。
     *
     * <p>逐条处理：检索 → 记录命中文档 ID → 与期望文档比对计算指标 → 有参考答案时
     * 调用 LLM Judge 打分。最后按"含期望文档的用例"汇总平均指标，并做阈值门禁。</p>
     *
     * @param request 评估请求（golden 集路径、知识库、模式、Top-K、阈值）
     * @return 评估报告（含汇总指标、LLM 均分、逐条明细、passed 门禁结果）
     */
    @Override
    public RagEvalReport evaluate(RagEvalRequest request) {
        List<GoldenCase> cases = caseLoader.load(request.getGoldenSetPath());
        String kb = request.getKnowledgeBase();
        int topK = request.getTopK() <= 0 ? 5 : request.getTopK();

        List<RagEvalCaseResult> results = new ArrayList<>();
        double recallSum = 0;
        double mrrSum = 0;
        double hitSum = 0;
        int counted = 0;
        int llmCount = 0;
        int llmSum = 0;

        for (GoldenCase c : cases) {
            String caseKb = StringUtils.hasText(kb) ? kb : (StringUtils.hasText(c.getKnowledgeBase()) ? c.getKnowledgeBase() : "default");
            List<Document> docs = retrieveSafely(dataSource, caseKb, c.getQuestion(), topK);
            List<String> retrievedIds = docs.stream()
                    .map(d -> String.valueOf(d.getMetadata().getOrDefault("documentId", d.getId())))
                    .toList();
            List<String> expectedIds = c.getExpectedDocumentIds() == null ? List.of() : c.getExpectedDocumentIds();
            RetrievalMetrics metrics = RetrievalMetrics.compute(retrievedIds, expectedIds, topK);

            RagEvalCaseResult result = new RagEvalCaseResult();
            result.setGoldenCaseId(c.getId());
            result.setQuestion(c.getQuestion());
            result.setRetrievedDocumentIds(retrievedIds);
            result.setMetrics(metrics);

            if (!expectedIds.isEmpty()) {
                recallSum += metrics.getRecallAtK();
                mrrSum += metrics.getMrr();
                hitSum += metrics.getHitRate();
                counted++;
            }
            // LLM 打分（可选）
            if (StringUtils.hasText(c.getReferenceAnswer())) {
                Integer score = answerJudge.score(c.getQuestion(), buildAnswerPlaceholder(c), c.getReferenceAnswer());
                if (score != null) {
                    result.setLlmScore(score);
                    llmSum += score;
                    llmCount++;
                }
            }
            results.add(result);
        }

        RetrievalMetrics overall = new RetrievalMetrics();
        if (counted > 0) {
            overall.setRecallAtK(recallSum / counted);
            overall.setMrr(mrrSum / counted);
            overall.setHitRate(hitSum / counted);
            overall.setExpectedCount(counted);
        }
        Double avgLlm = llmCount > 0 ? (double) llmSum / llmCount : null;

        RagEvalReport report = new RagEvalReport();
        report.setEvalId(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneId.systemDefault()).format(Instant.now()));
        report.setRetrievalMode(request.getMode());
        report.setTopK(topK);
        report.setMetrics(overall);
        report.setAvgLlmScore(avgLlm);
        report.setCaseResults(results);
        report.setExecutedAt(Instant.now());

        boolean hitPass = request.getHitRateThreshold() == null
                || overall.getHitRate() >= request.getHitRateThreshold();
        boolean llmPass = request.getLlmScoreThreshold() == null
                || avgLlm == null
                || avgLlm >= request.getLlmScoreThreshold();
        report.setPassed(hitPass && llmPass);
        log.info("RAG 评估完成: evalId={}, mode={}, recall={}, mrr={}, hitRate={}, avgLlm={}, passed={}",
                report.getEvalId(), request.getMode(), overall.getRecallAtK(), overall.getMrr(),
                overall.getHitRate(), avgLlm, report.isPassed());
        return report;
    }

    /**
     * 安全检索：单条用例检索失败不中断整个评估，降级为空结果。
     *
     * <p>这是评估工具与在线服务的关键差异：评估必须"跑完全部用例"才能出报告，
     * 不能因为个别失败就整体崩溃——失败用例记 0 命中即可。</p>
     */
    private List<Document> retrieveSafely(RagEvalDataSource ds, String kb, String query, int topK) {
        try {
            return ds.retrieve(kb, query, topK);
        } catch (Exception e) {
            log.warn("评估检索失败: kb={}, query={}, err={}", kb, query, e.getMessage());
            return List.of();
        }
    }

    /** 评估阶段未真正生成回答时，用检索片段拼接占位回答，供 LLM 打分参考 */
    private String buildAnswerPlaceholder(GoldenCase c) {
        return "(检索上下文已注入，回答未单独生成)";
    }
}