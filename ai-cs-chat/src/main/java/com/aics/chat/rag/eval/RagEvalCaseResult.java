package com.aics.chat.rag.eval;

import lombok.Data;

import java.util.List;

/**
 * 单条 golden 用例的评估结果。
 *
 * <p>一条结果 = 检索指标（Recall@k/MRR/HitRate）+ 回答质量（LLM 分数）+ 成本信号
 * （耗时、Token）。新增的 durationMs/totalTokens 字段服务于门禁扩展
 * （P95 延迟与单请求平均 Token），与 RagEvalReport 的聚合字段一一对应。</p>
 */
@Data
public class RagEvalCaseResult {

    /** 用例 ID */
    private String goldenCaseId;

    /** 问题 */
    private String question;

    /** 检索命中的文档 ID（按相关度降序） */
    private List<String> retrievedDocumentIds;

    /** 检索指标 */
    private RetrievalMetrics metrics;

    /** LLM-as-Judge 分数（1-5，可空） */
    // 可空：仅"有参考答案"的用例才打分，且 Judge 失败返回 null 不记入均分，
    // 避免把失败样本当成 0 分拉低均分
    private Integer llmScore;

    /** 生成的回答（可空） */
    private String answer;

    /** 用例执行耗时（毫秒，检索 + 打分） */
    // 单个用例耗时：评估报告按全部用例耗时升序取 95% 分位得到 P95，
    // 因此每个用例都必须记录耗时（无论打分成功与否）
    private Long durationMs;

    /** 用例 LLM 调用总 Token（可空：Judge 未提供 usage 时为空） */
    // 与 RagAnswerJudge.lastTotalTokens() 的 null 语义对齐：
    // 未提供 usage 就不计入平均 Token 的分子分母，保证统计口径一致
    private Integer totalTokens;
}