package com.aics.chat.rag.eval;

import lombok.Data;

import java.util.List;

/**
 * 单条 golden 用例的评估结果。
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
    private Integer llmScore;

    /** 生成的回答（可空） */
    private String answer;
}