package com.aics.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 令牌桶限流器测试：桶容量突发放行、长期速率限制、key 隔离。
 */
class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new TokenBucketRateLimiter();
    }

    @Test
    void 桶内令牌耗尽后拒绝() {
        // qps=2, burst=2：初始满桶 2 个令牌，连续第 3 次请求被拒
        assertTrue(limiter.tryAcquire("u:1", 2, 2));
        assertTrue(limiter.tryAcquire("u:1", 2, 2));
        assertFalse(limiter.tryAcquire("u:1", 2, 2), "桶空后应拒绝");
    }

    @Test
    void 令牌按速率补充_等待后恢复放行() throws InterruptedException {
        // qps=10：耗尽后等 ~300ms 应补回 2+ 个令牌
        assertTrue(limiter.tryAcquire("u:1", 10, 2));
        assertTrue(limiter.tryAcquire("u:1", 10, 2));
        assertFalse(limiter.tryAcquire("u:1", 10, 2));
        Thread.sleep(350);
        assertTrue(limiter.tryAcquire("u:1", 10, 2), "补充后应恢复放行");
    }

    @Test
    void 不同key互不影响() {
        assertTrue(limiter.tryAcquire("u:1", 1, 1));
        assertFalse(limiter.tryAcquire("u:1", 1, 1));
        assertTrue(limiter.tryAcquire("u:2", 1, 1), "u:2 有独立令牌桶");
    }
}
