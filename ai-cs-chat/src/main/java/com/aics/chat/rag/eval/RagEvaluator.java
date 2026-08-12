package com.aics.chat.rag.eval;

/**
 * RAG 评估器。
 */
public interface RagEvaluator {

    /**
     * 执行 golden 集评估。
     *
     * @param request 评估请求
     * @return 评估报告
     */
    RagEvalReport evaluate(RagEvalRequest request);
}