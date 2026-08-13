package com.aics.chat.agent.model;

import java.util.List;

/**
 * 意图识别结果（结构化输出）
 *
 * @param intents     意图列表（支持多意图，如 售后 + 商品推荐）
 * @param sentiment   用户情绪
 * @param needsHandoff 是否强制转人工（情绪愤怒等）
 * @param rawJson     LLM 原始输出（审计用）
 */
public record IntentResult(List<AgentIntent> intents, SentimentType sentiment,
                           boolean needsHandoff, String rawJson) {

    public static IntentResult of(List<AgentIntent> intents, SentimentType sentiment, boolean needsHandoff) {
        // 兜底：空列表归一为空、情绪缺省为中性
        return new IntentResult(intents == null ? List.of() : intents,
                sentiment == null ? SentimentType.NEUTRAL : sentiment, needsHandoff, null);
    }

    public static IntentResult of(List<AgentIntent> intents, SentimentType sentiment,
                                  boolean needsHandoff, String rawJson) {
        return new IntentResult(intents == null ? List.of() : intents,
                sentiment == null ? SentimentType.NEUTRAL : sentiment, needsHandoff, rawJson);
    }

    public boolean has(AgentIntentType type) {
        // 是否命中指定意图类型（支持多意图）
        return intents.stream().anyMatch(i -> i.type() == type);
    }

    public AgentIntent get(AgentIntentType type) {
        // 取首个指定类型意图（无则返回 null）
        return intents.stream().filter(i -> i.type() == type).findFirst().orElse(null);
    }
}
