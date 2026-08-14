package com.aics.chat.modelrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class ModelRouterTest {

    private ModelRouterProperties props;
    private ChatModelRegistry registry;
    private ModelHealthRegistry health;
    private ModelRouter router;

    @BeforeEach
    void setUp() {
        props = new ModelRouterProperties();
        ModelDefinition primary = model("deepseek-chat", "deepseek", "standard", Set.of(ModelCapability.TOOL_CALLING), 100);
        ModelDefinition fallback = model("siliconflow-qwen3-32b", "siliconflow", "standard", Set.of(ModelCapability.TOOL_CALLING), 90);
        ModelDefinition cheap = model("siliconflow-qwen3-8b", "siliconflow", "cheap", Set.of(), 80);
        props.setModels(List.of(primary, fallback, cheap));

        ScenarioRoute chat = new ScenarioRoute();
        chat.setPrimary("deepseek-chat");
        chat.setFallbacks(List.of("siliconflow-qwen3-32b"));
        props.setScenarios(new EnumMap<>(Map.of(ModelScenario.CHAT, chat)));

        ChatClientCustomizer customizer = builder -> builder.defaultSystem("test");
        registry = new ChatModelRegistry(props, customizer, mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));
        registry.init();
        health = new ModelHealthRegistry();
        router = new ModelRouter(props, registry, health);
    }

    @Test
    void route_selectsHealthyPrimary() {
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(false)
                .requiredCapabilities(Set.of(ModelCapability.TOOL_CALLING))
                .build());
        assertEquals("deepseek-chat", decision.getSelectedModelId());
        assertEquals(List.of("siliconflow-qwen3-32b"), decision.getFallbackChain());
        assertEquals(RouteReason.SCENARIO_DEFAULT, decision.getReason());
    }

    @Test
    void route_fallsBackWhenPrimaryCircuitOpen() {
        health.breaker("deepseek-chat").transitionToOpenState();
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(false)
                .requiredCapabilities(Set.of(ModelCapability.TOOL_CALLING))
                .build());
        assertEquals("siliconflow-qwen3-32b", decision.getSelectedModelId());
        assertEquals(RouteReason.PRIMARY_UNAVAILABLE, decision.getReason());
    }

    @Test
    void route_quotaDowngradesToCheapTier() {
        props.getQuota().setEnabled(true);
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(true)
                .requiredCapabilities(Set.of())
                .build());
        assertEquals("siliconflow-qwen3-8b", decision.getSelectedModelId());
        assertEquals(RouteReason.QUOTA_DOWNGRADE, decision.getReason());
    }

    @Test
    void route_returnsNoEligibleWhenAllUnavailable() {
        health.breaker("deepseek-chat").transitionToOpenState();
        health.breaker("siliconflow-qwen3-32b").transitionToOpenState();
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(false)
                .requiredCapabilities(Set.of(ModelCapability.TOOL_CALLING))
                .build());
        assertNull(decision.getSelectedModelId());
        assertEquals(RouteReason.NO_ELIGIBLE_MODEL, decision.getReason());
    }

    @Test
    void route_returnsNoEligibleWhenRouterDisabled() {
        props.setEnabled(false);
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(false)
                .requiredCapabilities(Set.of(ModelCapability.TOOL_CALLING))
                .build());
        assertNull(decision.getSelectedModelId());
        assertEquals(RouteReason.NO_ELIGIBLE_MODEL, decision.getReason());
    }

    @Test
    void route_returnsQuotaNoCheaperModelWhenNoCheapEligible() {
        props.getQuota().setEnabled(true);
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(true)
                .requiredCapabilities(Set.of(ModelCapability.TOOL_CALLING))
                .build());
        assertEquals("deepseek-chat", decision.getSelectedModelId());
        assertEquals(List.of("siliconflow-qwen3-32b"), decision.getFallbackChain());
        assertEquals(RouteReason.QUOTA_NO_CHEAPER_MODEL, decision.getReason());
    }

    private static ModelDefinition model(String id, String provider, String tier,
                                         Set<ModelCapability> capabilities, int priority) {
        ModelDefinition def = new ModelDefinition();
        def.setId(id);
        def.setProvider(provider);
        def.setBaseUrl("https://example.test");
        def.setApiKey("test-key");
        def.setModel(id);
        def.setTier(tier);
        def.setCapabilities(capabilities);
        def.setPriority(priority);
        return def;
    }
}
