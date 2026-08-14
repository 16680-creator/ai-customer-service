package com.aics.gateway.filter;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 滑动窗口限流器（3.2 网关限流，纯 Java 实现，便于单元测试）。
 *
 * <p>按 key（用户ID 或客户端 IP）维护时间戳窗口：窗口内请求数达到上限即拒绝
 * （返回 false），过期的时间戳在每次获取时惰性清理。</p>
 */
public class SlidingWindowRateLimiter {

    /** key -> 最近请求时间戳队列（升序） */
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    /**
     * 尝试获取一个请求配额。
     *
     * @param key          限流键（用户ID/IP）
     * @param maxRequests  窗口内最大请求数
     * @param windowSeconds 窗口时长（秒）
     * @return true=放行；false=限流
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        // 学习点：滑动窗口 vs 固定窗口——
        // 固定窗口（如 Redis INCR+EXPIRE）在窗口边界存在“双倍放行”漏洞
        // （00:59:59 与 01:00:00 各能放满一窗）；滑动窗口维护“最近 windowSeconds 内”
        // 的请求时间戳队列，边界平滑，代价是多存一条时间戳。
        // 惰性清理：只清当前 key 的过期时间戳，不启动定时任务，实现简单且无并发扫描。
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;
        Deque<Long> timestamps = windows.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            // 惰性清理窗口外的旧时间戳
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    /**
     * 清空全部窗口（测试用）。
     */
    public void clear() {
        windows.clear();
    }
}
