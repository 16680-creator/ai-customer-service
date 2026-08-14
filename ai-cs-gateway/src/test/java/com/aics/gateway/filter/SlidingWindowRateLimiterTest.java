package com.aics.gateway.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 滑动窗口限流器单元测试（3.2 网关限流）。
 */
class SlidingWindowRateLimiterTest {

    private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter();

    @Test
    void 窗口内未超限_全部放行() {
        // 窗口 60s 上限 3 次：前 3 次放行
        assertTrue(limiter.tryAcquire("u:1", 3, 60));
        assertTrue(limiter.tryAcquire("u:1", 3, 60));
        assertTrue(limiter.tryAcquire("u:1", 3, 60));
    }

    @Test
    void 窗口内超限_第4次被拒() {
        assertTrue(limiter.tryAcquire("u:1", 3, 60));
        assertTrue(limiter.tryAcquire("u:1", 3, 60));
        assertTrue(limiter.tryAcquire("u:1", 3, 60));
        // 第 4 次触发限流
        assertFalse(limiter.tryAcquire("u:1", 3, 60));
    }

    @Test
    void 不同用户互不影响() {
        assertTrue(limiter.tryAcquire("u:1", 1, 60));
        // 用户 2 拥有独立窗口
        assertTrue(limiter.tryAcquire("u:2", 1, 60));
        // 用户 1 再次请求被拒
        assertFalse(limiter.tryAcquire("u:1", 1, 60));
    }

    @Test
    void 窗口过期后恢复() throws InterruptedException {
        assertTrue(limiter.tryAcquire("u:1", 1, 1));
        assertFalse(limiter.tryAcquire("u:1", 1, 1));
        // 等待窗口过期（1s）后恢复配额
        Thread.sleep(1100);
        assertTrue(limiter.tryAcquire("u:1", 1, 1));
    }
}
