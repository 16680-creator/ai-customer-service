package com.aics.chat.modelrouter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ModelRouter {

    private final ModelRouterProperties properties;
    private final ChatModelRegistry registry;
    private final ModelHealthRegistry healthRegistry;

    public RouteDecision route(RouteRequest request) {
        ScenarioRoute scenarioRoute = properties.getScenarios().get(request.getScenario());
        if (scenarioRoute == null || !StringUtils.hasText(scenarioRoute.getPrimary())) {
            return noEligible();
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(scenarioRoute.getPrimary());
        if (scenarioRoute.getFallbacks() != null) {
            candidates.addAll(scenarioRoute.getFallbacks());
        }
        Set<ModelCapability> required = request.getRequiredCapabilities() == null
                ? Set.of() : request.getRequiredCapabilities();
        candidates.removeIf(id -> !registry.contains(id)
                || !healthRegistry.isAvailable(id)
                || !hasCapabilities(id, required));

        if (candidates.isEmpty()) {
            return noEligible();
        }

        if (request.isQuotaExceeded() && properties.getQuota().isEnabled()) {
            String cheap = cheapestEligible(required);
            if (cheap != null) {
                return decision(cheap, rest(candidates, cheap), RouteReason.QUOTA_DOWNGRADE);
            }
            return decision(candidates.get(0), rest(candidates, candidates.get(0)),
                    RouteReason.QUOTA_NO_CHEAPER_MODEL);
        }

        RouteReason reason = candidates.get(0).equals(scenarioRoute.getPrimary())
                ? RouteReason.SCENARIO_DEFAULT : RouteReason.PRIMARY_UNAVAILABLE;
        return decision(candidates.get(0), rest(candidates, candidates.get(0)), reason);
    }

    private boolean hasCapabilities(String modelId, Set<ModelCapability> required) {
        Set<ModelCapability> actual = registry.get(modelId).getDefinition().getCapabilities();
        return actual != null && actual.containsAll(required);
    }

    private String cheapestEligible(Set<ModelCapability> required) {
        String targetTier = properties.getQuota().getOverLimitFallbackTier();
        return properties.getModels().stream()
                .map(ModelDefinition::getId)
                .filter(id -> registry.contains(id)
                        && healthRegistry.isAvailable(id)
                        && targetTier.equals(registry.get(id).getDefinition().getTier())
                        && hasCapabilities(id, required))
                .sorted(Comparator.comparingInt((String id) -> registry.get(id).getDefinition().getPriority()).reversed())
                .findFirst()
                .orElse(null);
    }

    private List<String> rest(List<String> candidates, String selected) {
        return candidates.stream().filter(id -> !id.equals(selected)).toList();
    }

    private RouteDecision decision(String selected, List<String> fallbackChain, RouteReason reason) {
        return RouteDecision.builder()
                .selectedModelId(selected)
                .fallbackChain(fallbackChain)
                .reason(reason)
                .build();
    }

    private RouteDecision noEligible() {
        return RouteDecision.builder()
                .selectedModelId(null)
                .fallbackChain(List.of())
                .reason(RouteReason.NO_ELIGIBLE_MODEL)
                .build();
    }
}
