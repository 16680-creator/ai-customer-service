package com.aics.chat.modelrouter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelHealthRegistryTest {

    private final ModelHealthRegistry registry = new ModelHealthRegistry();

    @Test
    void breaker_isIsolatedPerModelId() {
        CircuitBreaker a = registry.breaker("deepseek-chat");
        CircuitBreaker b = registry.breaker("siliconflow-qwen3-32b");
        assertNotSame(a, b);
        assertSame(a, registry.breaker("deepseek-chat"));
    }

    @Test
    void isAvailable_returnsFalseWhenBreakerOpen() {
        assertTrue(registry.isAvailable("deepseek-chat"));
        registry.breaker("deepseek-chat").transitionToOpenState();
        assertFalse(registry.isAvailable("deepseek-chat"));
    }

    @Test
    void isAvailable_returnsTrueWhenHalfOpen() {
        CircuitBreaker breaker = registry.breaker("deepseek-chat");
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();
        assertTrue(registry.isAvailable("deepseek-chat"));
    }
}
