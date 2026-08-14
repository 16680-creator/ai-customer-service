package com.aics.chat.modelrouter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void rebuild_reusesHolderWhenDefinitionUnchanged() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek", true)));
        ChatModelRegistry registry = new ChatModelRegistry(
                props, builder -> builder.defaultSystem("test system"),
                mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));
        registry.init();
        ModelClientHolder first = registry.get("m1");

        registry.rebuild();

        assertSame(first, registry.get("m1"));
    }

    @Test
    void rebuild_createsNewHolderWhenModelChanges() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek", true)));
        ChatModelRegistry registry = new ChatModelRegistry(
                props, builder -> builder.defaultSystem("test system"),
                mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));
        registry.init();
        ModelClientHolder first = registry.get("m1");

        ModelDefinition changed = model("m1", "deepseek", true);
        changed.setModel("deepseek-v3");
        props.setModels(List.of(changed));
        registry.rebuild();

        assertNotSame(first, registry.get("m1"));
    }

    @Test
    void rebuild_keepsOldSnapshotOnInvalidConfig() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek", true)));
        ChatModelRegistry registry = new ChatModelRegistry(
                props, builder -> builder.defaultSystem("test system"),
                mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));
        registry.init();
        ModelClientHolder first = registry.get("m1");

        props.setModels(List.of(model("m1", "deepseek", true), model("m1", "siliconflow", true)));
        registry.rebuild();

        assertSame(first, registry.get("m1"));
    }

    @Test
    void rebuild_initialFailureRethrowsIllegalState() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek", true), model("m1", "siliconflow", true)));
        ChatModelRegistry registry = new ChatModelRegistry(
                props, builder -> builder.defaultSystem("test system"),
                mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));

        assertThrows(IllegalStateException.class, registry::init);
        assertFalse(registry.contains("m1"));
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
