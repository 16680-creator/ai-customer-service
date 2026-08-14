package com.aics.chat.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Trace span 埋点工具：统一封装 Micrometer Observation 的创建与低/高基数 key 写入。
 *
 * <p>各环节（retrieval/rerank/intent/tool/answer）埋点时调用 {@link #observe}，
 * 由 {@link TraceSpanObservationHandler} 在 onStop 时组装为 TraceSpan 挂到当前 TraceContext；
 * 当前线程无 TraceContext（未启用/采样未命中）时 handler 跳过，观测零开销。</p>
 */
public final class TraceSpans {

    private TraceSpans() {
    }

    /**
     * 执行动作并记录一个 span（同步）。
     *
     * @param registry ObservationRegistry
     * @param spanType 环节类型（INTENT/RETRIEVAL/RERANK/LLM/TOOL/ANSWER/SAFETY）
     * @param name     观测名（OTLP span 名）
     * @param lowKeys  low-cardinality key（span.type/provider/model 等）
     * @param highKeys high-cardinality key（detail/query/耗时等明细）
     * @param action   被观测的动作
     *
     * <p>学习点：这是"装饰器模式"的简化应用——把观测逻辑从业务代码中抽出来，
     * 业务侧只需声明"我要观测什么"，无需关心 observation 的 start/stop 与异常传播；
     * 与直接内联 Observation 相比，既统一了 spanType 的 key 命名，也让埋点代码可复用。</p>
     */
    public static void observe(ObservationRegistry registry, String spanType, String name,
                               Map<String, String> lowKeys, Map<String, String> highKeys,
                               Runnable action) {
        if (registry == null) {
            action.run();
            return;
        }
        Observation observation = Observation.createNotStarted(name, registry)
                .lowCardinalityKeyValue("span.type", spanType);
        lowKeys.forEach(observation::lowCardinalityKeyValue);
        highKeys.forEach(observation::highCardinalityKeyValue);
        // observe(action)：内部就是 start → action.run() → stop 的完整生命周期，
        // 异常时自动 observation.error(e) 再重抛，保证观测与业务异常传播都不丢失
        observation.observe(action);
    }

    /**
     * 执行动作并记录一个 span（带返回值）。
     */
    public static <T> T observeReturn(ObservationRegistry registry, String spanType, String name,
                                      Map<String, String> lowKeys, Map<String, String> highKeys,
                                      Supplier<T> action) {
        if (registry == null) {
            return action.get();
        }
        Observation observation = Observation.createNotStarted(name, registry)
                .lowCardinalityKeyValue("span.type", spanType);
        lowKeys.forEach(observation::lowCardinalityKeyValue);
        highKeys.forEach(observation::highCardinalityKeyValue);
        return observation.observe(action);
    }
}
