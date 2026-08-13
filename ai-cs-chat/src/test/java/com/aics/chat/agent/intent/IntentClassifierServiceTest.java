package com.aics.chat.agent.intent;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.model.AgentIntentType;
import com.aics.chat.agent.model.IntentResult;
import com.aics.chat.agent.model.SentimentType;
import com.aics.chat.service.impl.ResilientAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 意图识别测试：LLM 结构化输出解析、多意图、置信度路由、规则兜底
 */
@ExtendWith(MockitoExtension.class)
class IntentClassifierServiceTest {

    @Mock
    private ResilientAiService resilientAiService;

    private final AgentProperties properties = new AgentProperties();

    private IntentClassifierService newService() {
        return new IntentClassifierService(properties, resilientAiService, new ObjectMapper());
    }

    @Test
    void LLM输出解析为结构化意图() {
        properties.setLlmIntentEnabled(true);
        String json = "{\"intents\":[{\"type\":\"AFTER_SALE\",\"confidence\":0.95,"
                + "\"params\":{\"action\":\"EXCHANGE\",\"reason\":\"坏了\"}},"
                + "{\"type\":\"PRODUCT_RECOMMEND\",\"confidence\":0.8,\"params\":{\"budget\":\"300\"}}],"
                + "\"sentiment\":\"NEGATIVE\",\"needsHandoff\":false}";
        when(resilientAiService.callRagChat(anyString()))
                .thenReturn(CompletableFuture.completedFuture(json));
        IntentResult result = newService().classify("我昨天买的耳机坏了，想换货");
        assertEquals(2, result.intents().size());
        assertEquals(AgentIntentType.AFTER_SALE, result.intents().get(0).type());
        assertEquals("EXCHANGE", result.intents().get(0).params().get("action"));
        assertEquals(SentimentType.NEGATIVE, result.sentiment());
        assertFalse(result.needsHandoff());
    }

    @Test
    void LLM输出带代码块也能解析() {
        properties.setLlmIntentEnabled(true);
        String json = "```json\n{\"intents\":[{\"type\":\"HUMAN_HANDOFF\",\"confidence\":0.9}],"
                + "\"sentiment\":\"ANGRY\",\"needsHandoff\":true}\n```";
        when(resilientAiService.callRagChat(anyString()))
                .thenReturn(CompletableFuture.completedFuture(json));
        IntentResult result = newService().classify("转人工");
        assertEquals(AgentIntentType.HUMAN_HANDOFF, result.intents().get(0).type());
        assertTrue(result.needsHandoff());
    }

    @Test
    void LLM输出垃圾JSON时降级规则分类() {
        properties.setLlmIntentEnabled(true);
        when(resilientAiService.callRagChat(anyString()))
                .thenReturn(CompletableFuture.completedFuture("这不是JSON"));
        IntentResult result = newService().classify("我要退货");
        assertEquals(AgentIntentType.AFTER_SALE, result.intents().get(0).type());
        assertEquals("RETURN", result.intents().get(0).params().get("action"));
    }

    @Test
    void LLM调用异常时降级规则分类() {
        properties.setLlmIntentEnabled(true);
        when(resilientAiService.callRagChat(anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("模型超时")));
        IntentResult result = newService().classify("帮我推荐降噪耳机");
        assertEquals(AgentIntentType.PRODUCT_RECOMMEND, result.intents().get(0).type());
    }

    @Test
    void 规则分类识别多意图() {
        IntentResult result = newService().ruleBasedClassify("我昨天买的耳机坏了想换货，另外帮我看看同价位的推荐");
        assertTrue(result.has(AgentIntentType.AFTER_SALE));
        assertTrue(result.has(AgentIntentType.PRODUCT_RECOMMEND));
        assertEquals("EXCHANGE", result.get(AgentIntentType.AFTER_SALE).params().get("action"));
    }

    @Test
    void 规则分类识别情绪与转人工() {
        IntentResult angry = newService().ruleBasedClassify("你们太差了，我要投诉");
        assertEquals(SentimentType.ANGRY, angry.sentiment());
        assertTrue(angry.needsHandoff());
        assertTrue(angry.has(AgentIntentType.HUMAN_HANDOFF));

        IntentResult neutral = newService().ruleBasedClassify("优惠券怎么使用");
        assertEquals(AgentIntentType.NORMAL_CHAT, neutral.intents().get(0).type());
        assertEquals(SentimentType.NEUTRAL, neutral.sentiment());
        assertFalse(neutral.needsHandoff());
    }

    @Test
    void 规则分类抽取预算与关键词() {
        IntentResult result = newService().ruleBasedClassify("帮我推荐300元以内的降噪耳机");
        AgentIntentType recommend = AgentIntentType.PRODUCT_RECOMMEND;
        assertEquals("300", result.get(recommend).params().get("budget"));
        assertEquals("降噪", result.get(recommend).params().get("keywords"));
    }

    @Test
    void 未知输入归类普通对话() {
        IntentResult result = newService().ruleBasedClassify("今天天气不错");
        assertEquals(AgentIntentType.NORMAL_CHAT, result.intents().get(0).type());
        assertNull(result.intents().get(0).params().get("action"));
    }

    @Test
    void 低置信度意图按普通对话路由() {
        properties.setLlmIntentEnabled(true);
        properties.setIntentThreshold(0.7);
        String json = "{\"intents\":[{\"type\":\"AFTER_SALE\",\"confidence\":0.5,"
                + "\"params\":{\"action\":\"EXCHANGE\"}}],\"sentiment\":\"NEUTRAL\",\"needsHandoff\":false}";
        when(resilientAiService.callRagChat(anyString()))
                .thenReturn(CompletableFuture.completedFuture(json));
        IntentResult result = newService().classify("随便聊聊");
        assertEquals(1, result.intents().size());
        assertEquals(AgentIntentType.NORMAL_CHAT, result.intents().get(0).type());
    }

    @Test
    void 高置信度意图保留多意图() {
        properties.setLlmIntentEnabled(true);
        properties.setIntentThreshold(0.7);
        String json = "{\"intents\":[{\"type\":\"AFTER_SALE\",\"confidence\":0.95,"
                + "\"params\":{\"action\":\"EXCHANGE\"}},"
                + "{\"type\":\"PRODUCT_RECOMMEND\",\"confidence\":0.5,\"params\":{}}],"
                + "\"sentiment\":\"NEUTRAL\",\"needsHandoff\":false}";
        when(resilientAiService.callRagChat(anyString()))
                .thenReturn(CompletableFuture.completedFuture(json));
        IntentResult result = newService().classify("换货加推荐");
        assertEquals(1, result.intents().size());
        assertEquals(AgentIntentType.AFTER_SALE, result.intents().get(0).type());
    }

    @Test
    void LLM禁用时直接走规则() {
        properties.setLlmIntentEnabled(false);
        IntentResult result = newService().classify("我要退货");
        assertEquals(AgentIntentType.AFTER_SALE, result.intents().get(0).type());
        assertNotNull(result);
    }
}
