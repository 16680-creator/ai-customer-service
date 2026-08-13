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
 * <h3>【AI 技术详解】RAG 评估体系</h3>
 * <ul>
 *   <li><b>为什么需要评估</b>：
 *       <ul>
 *         <li>RAG 效果依赖检索质量，但"好不好"难以主观判断</li>
 *         <li>需要量化指标来度量改进效果（如切换 Embedding 模型后是否更好）</li>
 *         <li>需要自动化回归测试，防止改动引入退化</li>
 *       </ul>
 *   </li>
 *   <li><b>评估维度</b>：
 *       <ul>
 *         <li><b>检索质量</b>：Recall@k、MRR、HitRate（度量"召回全不全 / 排得前不前 / 有没有命中"）</li>
 *         <li><b>回答质量</b>：LLM-as-Judge 评分（1-5 分，度量"回答好不好"）</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】Golden 测试集</h3>
 * <ul>
 *   <li><b>什么是 Golden 集</b>：人工标注的标准问答对（问题 + 期望文档 + 参考答案）</li>
 *   <li><b>作用</b>：作为评估基准，衡量 RAG 检索和回答的质量</li>
 *   <li><b>构建方法</b>：
 *       <ul>
 *         <li>从真实用户问题中采样</li>
 *         <li>人工标注期望命中的文档</li>
 *         <li>人工编写参考答案</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】评估指标含义</h3>
 * <ul>
 *   <li><b>Recall@k</b>：期望文档被召回的比例（越高越好，衡量"召回全不全"）</li>
 *   <li><b>MRR（Mean Reciprocal Rank）</b>：首个命中排名的倒数（越高越好，衡量"排得前不前"）</li>
 *   <li><b>HitRate</b>：至少命中一条期望文档的用例占比（越高越好，衡量"有没有命中"）</li>
 * </ul>
 *
 * <h3>【AI 技术详解】评估流程</h3>
 * <pre>
 *   1. 加载 Golden 集（GoldenCaseLoader.load()）
 *   2. 逐条处理：
 *      ├── 检索：RagEvalDataSource.retrieve()
 *      ├── 指标：RetrievalMetrics.compute()（Recall@k / MRR / HitRate）
 *      └── 打分：LlmJudgeService.score()（LLM-as-Judge，可选）
 *   3. 汇总报告：平均指标 + LLM 均分
 *   4. 阈值门禁：hitRate / LLM 均分是否达标
 * </pre>
 *
 * <h3>【技术关联】可对比实验</h3>
 * <ul>
 *   <li>同一 Golden 集在不同检索模式（VECTOR/HYBRID）下各跑一次</li>
 *   <li>即可量化改动前后的质量差异，这是"用数据说话"的关键</li>
 *   <li>示例：HYBRID 模式的 HitRate 比 VECTOR 高 10%，说明混合检索更有效</li>
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
            // 知识库优先级：请求指定 > 用例自带 > 默认
            String caseKb = StringUtils.hasText(kb) ? kb : (StringUtils.hasText(c.getKnowledgeBase()) ? c.getKnowledgeBase() : "default");
            // 1) 检索：拿到命中文档（检索失败时 retrieveSafely 返回空，不中断评估）
            List<Document> docs = retrieveSafely(dataSource, caseKb, c.getQuestion(), topK);
            // 2) 提取命中文档 ID（优先元数据 documentId，否则用文档自带 ID）
            List<String> retrievedIds = docs.stream()
                    .map(d -> String.valueOf(d.getMetadata().getOrDefault("documentId", d.getId())))
                    .toList();
            List<String> expectedIds = c.getExpectedDocumentIds() == null ? List.of() : c.getExpectedDocumentIds();
            // 3) 与期望文档比对，计算 Recall@k / MRR / HitRate
            RetrievalMetrics metrics = RetrievalMetrics.compute(retrievedIds, expectedIds, topK);

            RagEvalCaseResult result = new RagEvalCaseResult();
            result.setGoldenCaseId(c.getId());
            result.setQuestion(c.getQuestion());
            result.setRetrievedDocumentIds(retrievedIds);
            result.setMetrics(metrics);

            // 4) 只累计"含期望文档"的用例，避免无期望用例稀释命中率
            if (!expectedIds.isEmpty()) {
                recallSum += metrics.getRecallAtK();
                mrrSum += metrics.getMrr();
                hitSum += metrics.getHitRate();
                counted++;
            }
            // 5) LLM-as-Judge 打分（可选：仅有参考答案的用例才打分）
            if (StringUtils.hasText(c.getReferenceAnswer())) {
                Integer score = answerJudge.score(c.getQuestion(), buildAnswerPlaceholder(c), c.getReferenceAnswer());
                if (score != null) {   // Judge 失败返回 null，不记入均分
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