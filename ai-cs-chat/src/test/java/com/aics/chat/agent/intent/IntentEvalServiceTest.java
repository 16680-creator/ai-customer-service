package com.aics.chat.agent.intent;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.service.impl.ResilientAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 意图分类评估测试（SC-001：意图分类 Macro-F1 ≥ 0.90）
 */
class IntentEvalServiceTest {

    @Test
    void 固定数据集MacroF1不低于090() {
        IntentClassifierService classifier = new IntentClassifierService(
                new AgentProperties(), null, new ObjectMapper());
        IntentEvalService evalService = new IntentEvalService(classifier);
        IntentEvalService.EvalReport report = evalService.evaluate();
        assertEquals(24, report.sampleCount());
        assertTrue(report.macroF1() >= 0.90,
                "Macro-F1 应 >= 0.90，实际: " + report.macroF1() + "，分项: " + report.perClassF1());
    }
}
