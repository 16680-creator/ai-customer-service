package com.aics.chat.rag.eval;

/**
 * 回答质量打分器（LLM-as-Judge）。
 */
public interface RagAnswerJudge {

    /**
     * 对回答打分（1-5）。
     *
     * @param question        用户问题
     * @param answer          模型回答
     * @param referenceAnswer 参考答案（可空）
     * @return 分数；不可用/异常时返回 null
     */
    Integer score(String question, String answer, String referenceAnswer);
}