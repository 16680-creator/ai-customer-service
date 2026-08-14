package com.aics.chat.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Trace span 观测处理器：把 Micrometer Observation 组装为 {@link TraceSpan} 并挂到当前
 * {@link TraceContext}。
 *
 * <p>设计（见 design.md D1/D5）：
 * <ul>
 *   <li><b>统一埋点</b>：业务代码用 {@code Observation.createNotStarted(...)} 埋点，
 *       本处理器在 {@link #onStop} 时读取 low/high cardinality keys 组装 span；</li>
 *   <li><b>上下文解耦</b>：当前线程无 {@link TraceContext}（未启用/采样未命中）时直接跳过，
 *       埋点对业务零侵入；</li>
 *   <li><b>日志导出</b>：未配置 OTLP 后端时，span 以结构化日志输出（{@code aics.observability.log-export}）。</li>
 * </ul>
 * 约定的 key 名（埋点方写入）：
 * <ul>
 *   <li>{@code span.type}（low）：INTENT / RETRIEVAL / RERANK / LLM / TOOL / ANSWER / SAFETY；</li>
 *   <li>{@code provider} / {@code model}（low）；</li>
 *   <li>{@code scenario} / {@code tokens} / {@code detail} / {@code error}（high）。</li>
 * </ul>
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class TraceSpanObservationHandler implements ObservationHandler<Observation.Context> {

    private final ObservabilityProperties properties;

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }

    @Override
    public void onStop(Observation.Context context) {
        // 当前线程没有 TraceContext（未启用/采样未命中）时不组装，埋点零开销
        // 学习点：Observation 的 handler 机制是"观察者模式"——埋点方（业务代码）只负责
        // start/stop 观测，不关心数据去向；handler 在 stop 时被回调，解耦了"埋点"与"消费"
        TraceContext trace = TraceContextHolder.current();
        if (trace == null) {
            return;
        }
        // 组装 span：把 Observation 的 name + key-values 映射为 TraceSpan 字段
        // 学习点：Micrometer 区分 low-cardinality（基数低，如 span.type/provider/model，
        // 适合做聚合维度/索引）与 high-cardinality（基数高，如 query/detail，只能当明细存）。
        // 这沿用了 Prometheus 标签设计哲学：高基数键进标签会撑爆时序库
        TraceSpan span = new TraceSpan();
        span.setName(context.getName());
        span.setSpanType(low(context, "span.type"));
        span.setProvider(low(context, "provider"));
        span.setModel(low(context, "model"));
        span.setPromptTokens(intOf(high(context, "promptTokens")));
        span.setCompletionTokens(intOf(high(context, "completionTokens")));
        span.setRetries(intOf(high(context, "retries")));
        span.setDetail(high(context, "detail"));

        // 错误处理：observation.error(e) 写入的异常
        // 学习点：Observation 的错误传播是"边带"机制——业务代码 catch 到异常后调用
        // observation.error(e) 标记失败，handler 在 stop 时统一读取，无需业务侧自己写 span 状态
        Throwable error = context.getError();
        if (error != null) {
            span.setStatus("FAILED");
            // 错误摘要截断：防止异常 message 过大撑爆 llm_trace 的 spans_json 字段
            span.setErrorSummary(truncate(error.getMessage(), 500));
        } else {
            span.setStatus("SUCCESS");
        }
        trace.addSpan(span);

        // 日志导出（OTLP 未配置时的默认通道）
        // 学习点：可观测性"降级阶梯"——有后端导 OTLP，没后端至少留结构化日志，
        // 保证任何部署形态下都能按 requestId 检索 span，这是生产可观测的最小底线
        if (properties.isLogExport()) {
            log.info("LLM trace span: requestId={}, spanType={}, name={}, provider={}, model={}, status={}",
                    trace.getRequestId(), span.getSpanType(), span.getName(),
                    span.getProvider(), span.getModel(), span.getStatus());
        }
    }

    /** 读取 low-cardinality key（无则返回 null） */
    private static String low(Observation.Context context, String key) {
        return find(context.getLowCardinalityKeyValues(), key);
    }

    /** 读取 high-cardinality key（无则返回 null） */
    private static String high(Observation.Context context, String key) {
        return find(context.getHighCardinalityKeyValues(), key);
    }

    /** 在 KeyValues 中按 key 查找（KeyValues 无 get(String)，需遍历） */
    private static String find(io.micrometer.common.KeyValues keyValues, String key) {
        if (keyValues == null) {
            return null;
        }
        for (io.micrometer.common.KeyValue kv : keyValues) {
            if (kv != null && key.equals(kv.getKey())) {
                String v = kv.getValue();
                return StringUtils.hasText(v) ? v : null;
            }
        }
        return null;
    }

    /** 解析整数 key（无/非法返回 null） */
    private static Integer intOf(String v) {
        if (!StringUtils.hasText(v)) {
            return null;
        }
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
