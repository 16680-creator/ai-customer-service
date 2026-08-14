package com.aics.chat.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OnlineEvalSampler 单元测试：采样判定边界（rate=0/1/中间值）。
 */
class OnlineEvalSamplerTest {

    private final OnlineEvalSampler sampler = new OnlineEvalSampler();

    @Test
    @DisplayName("rate=0 永不采样")
    void rateZero_neverSamples() {
        assertFalse(sampler.shouldSample(0));
        assertFalse(sampler.shouldSample(-0.5));
    }

    @Test
    @DisplayName("rate>=1 全量采样")
    void rateOne_alwaysSamples() {
        assertTrue(sampler.shouldSample(1));
        assertTrue(sampler.shouldSample(1.5));
    }

    @Test
    @DisplayName("rate=0.5 按概率采样（多次调用至少出现一次命中与未命中）")
    void rateHalf_probabilistic() {
        boolean sampled = false;
        boolean skipped = false;
        for (int i = 0; i < 200; i++) {
            if (sampler.shouldSample(0.5)) {
                sampled = true;
            } else {
                skipped = true;
            }
        }
        assertTrue(sampled, "200 次采样应至少命中一次");
        assertTrue(skipped, "200 次采样应至少跳过一次");
    }
}
