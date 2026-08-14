package com.aics.chat.modelrouter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ChatModelRegistryTest {

    @Test
    void rebuild_registersEnabledModelsOnly() {
        ModelRouterProperties props = new ModelRouterProperties();
        ModelDefinition enabled = model("deepseek-chat", "deepseek", true);
        ModelDefinition disabled = model("siliconflow-qwen3-32b", "siliconflow", false);
        props.setModels(List.of(enabled, disabled));

        ChatClientCustomizer customizer = builder -> builder.defaultSystem("test system");
        ChatModelRegistry registry = new ChatModelRegistry(
                props, customizer, mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));
        registry.init();

        assertTrue(registry.contains("deepseek-chat"));
        assertFalse(registry.contains("siliconflow-qwen3-32b"));
        ModelClientHolder holder = registry.get("deepseek-chat");
        assertNotNull(holder.getChatModel());
        assertNotNull(holder.getChatClient());
        assertNotNull(holder.getDefinition());
    }

    @Test
    void rebuild_appliesCustomizer() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek", true)));
        ChatClientCustomizer customizer = builder -> builder.defaultSystem("custom system");
        ChatModelRegistry registry = new ChatModelRegistry(
                props, customizer, mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));
        registry.init();

        ChatClient client = registry.get("m1").getChatClient();
        assertNotNull(client);
    }

    private static ModelDefinition model(String id, String provider, boolean enabled) {
        ModelDefinition def = new ModelDefinition();
        def.setId(id);
        def.setProvider(provider);
        def.setBaseUrl("https://example.test");
        def.setApiKey("test-key");
        def.setModel(id);
        def.setTier("standard");
        def.setEnabled(enabled);
        return def;
    }
}
