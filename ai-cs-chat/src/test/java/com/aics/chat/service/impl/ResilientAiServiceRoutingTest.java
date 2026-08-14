package com.aics.chat.service.impl;

import com.aics.chat.dto.QuotaCheckResult;
import com.aics.chat.modelrouter.*;
import com.aics.chat.observability.ModelUsageRecorder;
import com.aics.chat.observability.ObservabilityProperties;
import com.aics.chat.observability.QuotaService;
import com.aics.chat.observability.TraceContext;
import com.aics.chat.observability.TraceContextHolder;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResilientAiServiceRoutingTest {

    private ModelRouter modelRouter;
    private ChatModelRegistry registry;
    private ModelHealthRegistry health;
    private QuotaService quotaService;
    private ModelUsageRecorder usageRecorder;
    private ResilientAiService service;

    @BeforeEach
    void setUp() {
        modelRouter = mock(ModelRouter.class);
        registry = mock(ChatModelRegistry.class);
        health = new ModelHealthRegistry();
        quotaService = mock(QuotaService.class);
        usageRecorder = mock(ModelUsageRecorder.class);
        ModelRouterProperties props = new ModelRouterProperties();
        props.getQuota().setEnabled(false);
        service = new ResilientAiService(modelRouter, registry, health, quotaService, props,
                ObservationRegistry.create(), usageRecorder);
    }

    @Test
    void callChat_returnsFallbackWhenNoEligibleModel() throws Exception {
        when(modelRouter.route(any(RouteRequest.class)))
                .thenReturn(RouteDecision.builder()
                        .selectedModelId(null)
                        .fallbackChain(List.of())
                        .reason(RouteReason.NO_ELIGIBLE_MODEL)
                        .build());
        String result = service.callChat(ModelScenario.CHAT, List.of()).get();
        assertTrue(result.contains("繁忙"));
    }

    @Test
    void callChat_switchesToFallbackModel() throws Exception {
        ModelClientHolder primary = holder("deepseek-chat", mock(ChatClient.class, RETURNS_DEEP_STUBS));
        ModelClientHolder fallback = holder("siliconflow-qwen3-32b", mock(ChatClient.class, RETURNS_DEEP_STUBS));
        when(registry.get("deepseek-chat")).thenReturn(primary);
        when(registry.get("siliconflow-qwen3-32b")).thenReturn(fallback);
        when(modelRouter.route(any(RouteRequest.class)))
                .thenReturn(RouteDecision.builder()
                        .selectedModelId("deepseek-chat")
                        .fallbackChain(List.of("siliconflow-qwen3-32b"))
                        .reason(RouteReason.PRIMARY_UNAVAILABLE)
                        .build());
        when(primary.getChatClient().prompt().messages(anyList()).call().chatResponse())
                .thenThrow(new RuntimeException("primary down"));
        ChatResponse response = chatResponse("backup answer");
        when(fallback.getChatClient().prompt().messages(anyList()).call().chatResponse()).thenReturn(response);

        String result = service.callChat(ModelScenario.CHAT, List.of()).get();
        assertEquals("backup answer", result);
    }

    private static ModelClientHolder holder(String id, ChatClient client) {
        ModelDefinition def = new ModelDefinition();
        def.setId(id);
        def.setProvider("test");
        def.setBaseUrl("https://example.test");
        def.setApiKey("test");
        def.setModel(id);
        def.setTimeoutMs(1000);
        return new ModelClientHolder(def, null, client);
    }

    private static ChatResponse chatResponse(String text) {
        Generation generation = new Generation(new AssistantMessage(text));
        return ChatResponse.builder().generations(List.of(generation)).build();
    }
}
