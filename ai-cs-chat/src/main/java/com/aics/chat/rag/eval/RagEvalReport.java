package com.aics.chat.rag.eval;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * RAG 评估报告。
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
    private boolean passed;

    /** 执行时间 */
    private Instant executedAt;
}