package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Component
@RefreshScope
@ConfigurationProperties(prefix = "aics.model-router")
public class ModelRouterProperties {

    private boolean enabled = true;
    private List<ModelDefinition> models = new ArrayList<>();
    private Map<ModelScenario, ScenarioRoute> scenarios = new EnumMap<>(ModelScenario.class);
    private QuotaRouteProperties quota = new QuotaRouteProperties();

    public void validate() {
        Set<String> ids = new HashSet<>();
        for (ModelDefinition def : models) {
            if (!StringUtils.hasText(def.getId())) {
                throw new IllegalArgumentException("model id must not be blank");
            }
            if (!ids.add(def.getId())) {
                throw new IllegalArgumentException("duplicate model id: " + def.getId());
            }
            if (!StringUtils.hasText(def.getBaseUrl())
                    || !StringUtils.hasText(def.getApiKey())
                    || !StringUtils.hasText(def.getModel())) {
                throw new IllegalArgumentException("model " + def.getId() + " must configure base-url, api-key and model");
            }
        }
        for (Map.Entry<ModelScenario, ScenarioRoute> entry : scenarios.entrySet()) {
            ModelScenario scenario = entry.getKey();
            ScenarioRoute route = entry.getValue();
            if (route == null || !StringUtils.hasText(route.getPrimary())) {
                throw new IllegalArgumentException("scenario " + scenario + " must configure primary");
            }
            if (!ids.contains(route.getPrimary())) {
                throw new IllegalArgumentException("unknown primary model for scenario " + scenario + ": " + route.getPrimary());
            }
            for (String fallback : route.getFallbacks()) {
                if (!ids.contains(fallback)) {
                    throw new IllegalArgumentException("unknown fallback model for scenario " + scenario + ": " + fallback);
                }
            }
        }
    }
}
