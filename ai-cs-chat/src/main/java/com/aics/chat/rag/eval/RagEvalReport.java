package com.aics.chat.rag.eval;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * RAG 评估报告。
 *
 * <p>一条报告 = 检索质量（RetrievalMetrics）+ 回答质量（avgLlmScore）+ 成本信号
 * （p95LatencyMs / avgTokensPerRequest）+ 门禁结论（passed）。</p>
 */
@Data
public class RagEvalReport {

    /** 报告 ID（时间戳） */
    private String evalId;

    /** 检索模式 */
    private String retrievalMode;

    /** 检索 Top-K */
    private int topK;

    /** 汇总检索指标（仅统计含期望文档的用例） */
    private RetrievalMetrics metrics;

    /** LLM 均分（可空） */
    private Double avgLlmScore;

    /** 逐条明细 */
    private List<RagEvalCaseResult> caseResults;

    /** 是否通过阈值门禁 */
    // 门禁 = 正确率通过 && LLM 均分通过 && P95 通过 && 平均 Token 通过；
    // 未配置/无数据的维度恒为"通过"，见 EvalGateConfig 的 null 语义说明
    private boolean passed;

    /** P95 延迟（毫秒，全用例执行耗时，可空） */
    // 为什么用 P95 而非平均：单个超时用例会把平均拉高，P95 反映"绝大多数用例"的体验，
    // 更贴近用户真实感受；null 表示无样本（用例为空），此时门禁不判定该维度
    private Long p95LatencyMs;

    /** 单请求平均 Token（有 usage 用例的平均值，可空） */
    // 只对有 usage 的用例求平均：Judge 未暴露 usage 的用例不计入分母，
    // 避免"采样不全"导致平均数被稀释；null 时门禁不判定该维度
    private Double avgTokensPerRequest;

    /** 执行时间 */
    // 用 Instant（UTC 时间戳）而非 LocalDateTime：评估报告可能被 CI 归档/跨时区比对，
    // Instant 无时区歧义，前端展示时再转本地时区
    private Instant executedAt;
}