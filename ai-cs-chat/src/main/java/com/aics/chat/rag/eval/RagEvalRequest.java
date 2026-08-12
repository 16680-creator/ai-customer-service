package com.aics.chat.rag.eval;

import lombok.Data;

/**
 * 评估请求。
 */
@Data
public class RagEvalRequest {

    /** golden 集路径：classpath:eval/golden-set.json 或 file:/abs/path.json */
    private String goldenSetPath = "classpath:eval/golden-set.json";

    /** 知识库标识（覆盖 golden 集内的默认值） */
    private String knowledgeBase;

    /** 检索模式：VECTOR / HYBRID / HYBRID_QUERY_REWRITE */
    private String mode = "VECTOR";

    /** 检索 Top-K */
    private int topK = 5;

    /** LLM 分数阈值（null 表示不校验） */
    private Double llmScoreThreshold = 3.5;

    /** 命中率阈值 */
    private Double hitRateThreshold = 0.6;
}