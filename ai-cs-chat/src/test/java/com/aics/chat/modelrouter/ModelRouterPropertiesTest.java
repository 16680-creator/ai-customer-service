package com.aics.chat.modelrouter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelRouterPropertiesTest {

    @Test
    void validate_rejectsDuplicateModelId() {
        ModelRouterProperties props = new ModelRouterProperties();
        ModelDefinition a = model("m1", "deepseek");
        ModelDefinition b = model("m1", "siliconflow");
        props.setModels(List.of(a, b));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, props::validate);
        assertEquals("duplicate model id: m1", ex.getMessage());
    }

    @Test
    void validate_rejectsScenarioWithUnknownPrimary() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek")));
        ScenarioRoute route = new ScenarioRoute();
        route.setPrimary("missing");
        route.setFallbacks(List.of());
        props.setScenarios(new EnumMap<>(Map.of(ModelScenario.CHAT, route)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, props::validate);
        assertEquals("unknown primary model for scenario CHAT: missing", ex.getMessage());
    }

    @Test
    void validate_rejectsScenarioWithUnknownFallback() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek")));
        ScenarioRoute route = new ScenarioRoute();
        route.setPrimary("m1");
        route.setFallbacks(List.of("missing"));
        props.setScenarios(new EnumMap<>(Map.of(ModelScenario.CHAT, route)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, props::validate);
        assertEquals("unknown fallback model for scenario CHAT: missing", ex.getMessage());
    }

    @Test
    void validate_acceptsValidConfig() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek"), model("m2", "siliconflow")));
        ScenarioRoute route = new ScenarioRoute();
        route.setPrimary("m1");
        route.setFallbacks(List.of("m2"));
        props.setScenarios(new EnumMap<>(Map.of(ModelScenario.CHAT, route)));
        props.validate();
        assertEquals(2, props.getModels().size());
    }

    private static ModelDefinition model(String id, String provider) {
        ModelDefinition def = new ModelDefinition();
        def.setId(id);
        def.setProvider(provider);
        def.setBaseUrl("https://example.test");
        def.setApiKey("test-key");
        def.setModel(id);
        def.setTier("standard");
        return def;
    }
}
