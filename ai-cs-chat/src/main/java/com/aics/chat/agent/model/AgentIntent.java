package com.aics.chat.agent.model;

import java.util.Map;

/**
 * 单个意图及其结构化参数
 *
 * @param type       意图类型
 * @param confidence 置信度（0~1）
 * @param params     抽取的结构化参数（如 action=EXCHANGE、budget=300、keywords=降噪、reason=质量问题）
 */
public record AgentIntent(AgentIntentType type, double confidence, Map<String, String> params) {

    public static AgentIntent of(AgentIntentType type, double confidence, Map<String, String> params) {
        return new AgentIntent(type, confidence, params == null ? Map.of() : params);
    }
}
