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
        return breakers.computeIfAbsent(modelId, id -> circuitBreakerRegistry.circuitBreaker(id, config()));
    }

    public boolean isAvailable(String modelId) {
        CircuitBreaker breaker = breakers.get(modelId);
        if (breaker == null) {
            return true;
        }
        return breaker.getState() == CircuitBreaker.State.CLOSED
                || breaker.getState() == CircuitBreaker.State.HALF_OPEN;
    }

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
