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
        return new IntentResult(intents == null ? List.of() : intents,
                sentiment == null ? SentimentType.NEUTRAL : sentiment, needsHandoff, null);
    }

    public static IntentResult of(List<AgentIntent> intents, SentimentType sentiment,
                                  boolean needsHandoff, String rawJson) {
        return new IntentResult(intents == null ? List.of() : intents,
                sentiment == null ? SentimentType.NEUTRAL : sentiment, needsHandoff, rawJson);
    }

    public boolean has(AgentIntentType type) {
        return intents.stream().anyMatch(i -> i.type() == type);
    }

    public AgentIntent get(AgentIntentType type) {
        return intents.stream().filter(i -> i.type() == type).findFirst().orElse(null);
    }
}
