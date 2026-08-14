package com.aics.chat.modelrouter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModelHealthRegistry {

    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreaker breaker(String modelId) {
        // 设计要点：熔断器按 modelId 隔离——若共享一个熔断器，主模型故障会连累备用模型，路由降级就失去意义
        return breakers.computeIfAbsent(modelId, id -> circuitBreakerRegistry.circuitBreaker(id, config()));
    }

    public boolean isAvailable(String modelId) {
        CircuitBreaker breaker = breakers.get(modelId);
        if (breaker == null) {
            return true;
        }
        // 设计要点：可用性只看“是否 OPEN”——CLOSED 正常放行，HALF_OPEN 也要放行以完成恢复探测；只有 OPEN 才从路由候选剔除
        return breaker.getState() == CircuitBreaker.State.CLOSED
                || breaker.getState() == CircuitBreaker.State.HALF_OPEN;
    }

    // 学习点：HALF_OPEN 是恢复探测窗口——允许少量请求试探下游，成功则关断，失败则重新 OPEN，避免下游刚恢复就被流量打垮
    private CircuitBreakerConfig config() {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
    }
}
