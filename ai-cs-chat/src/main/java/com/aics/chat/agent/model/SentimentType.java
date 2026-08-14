package com.aics.chat.agent.model;

/**
 * 用户情绪（结构化输出，MVP 不引入独立情绪模型）
 */
public enum SentimentType {

    /** 正面 */
    POSITIVE,

    /** 中性 */
    NEUTRAL,

    /** 负面 */
    NEGATIVE,

    /** 愤怒（触发转人工） */
    ANGRY
}
