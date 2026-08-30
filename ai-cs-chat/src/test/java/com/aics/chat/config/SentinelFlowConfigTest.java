package com.aics.chat.config;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sentinel 流控规则装配单元测试
 * TDD: 规则注册数量、资源名、阈值与预热效果
 */
class SentinelFlowConfigTest {

    @Test
    @DisplayName("initFlowRules 注册两个资源的 QPS 规则")
    void registerRules() {
        new SentinelFlowConfig().initFlowRules();

        List<FlowRule> rules = FlowRuleManager.getRules();
        assertEquals(2, rules.size());

        FlowRule sendRule = rules.stream()
                .filter(r -> SentinelRules.RESOURCE_CHAT_SEND.equals(r.getResource()))
                .findFirst().orElseThrow();
        assertEquals(SentinelRules.CHAT_SEND_QPS_THRESHOLD, sendRule.getCount(), 0.001);
        assertEquals(SentinelRules.CONTROL_BEHAVIOR_WARM_UP, sendRule.getControlBehavior());

        FlowRule ragRule = rules.stream()
                .filter(r -> SentinelRules.RESOURCE_CHAT_RAG.equals(r.getResource()))
                .findFirst().orElseThrow();
        assertEquals(SentinelRules.CHAT_RAG_QPS_THRESHOLD, ragRule.getCount(), 0.001);
        assertTrue(ragRule.getCount() < sendRule.getCount(), "RAG 链路更重，阈值应低于普通对话");
    }
}
