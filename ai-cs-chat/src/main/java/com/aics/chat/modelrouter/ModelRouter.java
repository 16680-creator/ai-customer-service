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
        if (!properties.isEnabled()) {
            return noEligible();
        }
        ScenarioRoute scenarioRoute = properties.getScenarios().get(request.getScenario());
        if (scenarioRoute == null || !StringUtils.hasText(scenarioRoute.getPrimary())) {
            return noEligible();
        }

        // 设计要点：路由按场景链的 primary/fallbacks 顺序确定性执行，不引入 LLM 判断——模型选择必须是零成本、可解释、可复现的
        List<String> candidates = new ArrayList<>();
        candidates.add(scenarioRoute.getPrimary());
        if (scenarioRoute.getFallbacks() != null) {
            candidates.addAll(scenarioRoute.getFallbacks());
        }
        Set<ModelCapability> required = request.getRequiredCapabilities() == null
                ? Set.of() : request.getRequiredCapabilities();
        // 设计要点：先按“已注册 + 未熔断 + 能力满足”过滤，再取链上第一个可用模型——fallback 永远不会选到不存在或不可用的模型
        candidates.removeIf(id -> !registry.contains(id)
                || !healthRegistry.isAvailable(id)
                || !hasCapabilities(id, required));

        if (candidates.isEmpty()) {
            return noEligible();
        }

        // 设计要点：配额降级是全局扫描 cheap 档，普通路由仍严格走场景链——成本治理可以跳出场景约束，默认行为必须保持可预测
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
                // 学习点：降级仍要经过能力/健康过滤，再按 priority 倒序选——便宜不是唯一标准，缺能力或已熔断的模型即使再便宜也不能用
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

    // 学习点：无可用模型时返回 NO_ELIGIBLE_MODEL 而非抛异常——路由层失败交给上层友好兜底，不让模型选择变成 5xx
    private RouteDecision noEligible() {
        return RouteDecision.builder()
                .selectedModelId(null)
                .fallbackChain(List.of())
                .reason(RouteReason.NO_ELIGIBLE_MODEL)
                .build();
    }
}
