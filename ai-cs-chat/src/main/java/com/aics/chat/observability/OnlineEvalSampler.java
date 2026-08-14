package com.aics.chat.observability;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 线上采样器：按采样率决定是否抽取一次请求做线上评估。
 *
 * <p>边界语义（对应 spec 场景）：
 * <ul>
 *   <li>{@code rate <= 0}：永不采样；</li>
 *   <li>{@code rate >= 1}：全量采样；</li>
 *   <li>否则按概率采样（{@link ThreadLocalRandom}，线程安全）。</li>
 * </ul>
 * </p>
 */
@Component
public class OnlineEvalSampler {

    /**
     * 是否采样本次请求。
     *
     * @param rate 采样率（0~1）
     * @return true=采样
     *
     * <p>学习点：为什么边界值单独处理？{@code rate<=0} 直接短路返回 false 可以避免
     * {@code nextDouble()} 的无谓调用；{@code rate>=1} 直接返回 true 保证"全量采样"语义
     * 不被浮点随机数误差破坏（nextDouble 返回 [0,1)，永远小于 1，若不特判 1.0 会漏采）。
     * 中间区间用 {@link ThreadLocalRandom}：比 {@code Math.random()} 的全局锁竞争更低，
     * 高并发采样判定场景下吞吐更好。</p>
     */
    public boolean shouldSample(double rate) {
        if (rate <= 0) {
            return false;
        }
        if (rate >= 1) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < rate;
    }
}
