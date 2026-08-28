package com.aics.gateway.filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌桶限流器（3.2 网关限流，纯 Java 实现，便于单元测试）。
 *
 * <p>按 key 维护一个以恒定速率补充令牌的桶：补充速率 = qps 个/秒，桶容量 = burst。
 * 桶内有令牌则放行并扣减 1 个，否则拒绝。允许消费桶内存量令牌产生"突发"，
 * 长期平均速率被限制在 qps —— 这是与滑动窗口的本质区别：
 * 滑动窗口严格限制"窗口内总次数"，令牌桶限制"长期平均速率 + 允许短时突发"。</p>
 *
 * <h3>学习点：惰性补充 vs 定时任务</h3>
 * <ul>
 *   <li>不启动定时线程周期性补充令牌，而是在每次 tryAcquire 时按"距离上次的经过时间"
 *       一次性补足（tokens += elapsed * qps，上限 burst）——无后台线程、无并发扫描。</li>
 *   <li>令牌用 double 存储以保留小数部分：1 秒 0.5 QPS 这类低速率在两次请求间隔
 *       不足 2 秒时，若用整数截断会永远凑不齐 1 个令牌（饿死）。</li>
 * </ul>
 */
public class TokenBucketRateLimiter {

    /** key -> 令牌桶（用户ID/IP 维度） */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 尝试获取一个令牌。
     *
     * @param key   限流键（用户ID/IP）
     * @param qps   每秒补充令牌数（长期平均速率上限）
     * @param burst 桶容量（允许的最大突发请求数）
     * @return true=放行；false=限流
     */
    public boolean tryAcquire(String key, int qps, int burst) {
        int capacity = Math.max(1, burst);
        long now = System.nanoTime();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now));
        synchronized (bucket) {
            // 惰性补充：按经过时间一次性补足，封顶桶容量
            long elapsedNano = now - bucket.lastRefillNano;
            if (elapsedNano > 0) {
                double refill = elapsedNano / 1_000_000_000.0 * Math.max(1, qps);
                bucket.tokens = Math.min(capacity, bucket.tokens + refill);
                bucket.lastRefillNano = now;
            }
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    /**
     * 清空全部令牌桶（测试用）。
     */
    public void clear() {
        buckets.clear();
    }

    /** 单个 key 的令牌桶状态（tokens 为当前余量，lastRefillNano 为上次补充时刻） */
    private static final class Bucket {
        private double tokens;
        private long lastRefillNano;

        private Bucket(double tokens, long lastRefillNano) {
            this.tokens = tokens;
            this.lastRefillNano = lastRefillNano;
        }
    }
}
