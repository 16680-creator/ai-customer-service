package com.aics.chat.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OTLP 导出观测处理器：把 Micrometer Observation 同步导出为 OpenTelemetry span。
 *
 * <p>仅在配置了 {@code aics.observability.otlp-endpoint} 时由 {@link ObservationConfig}
 * 条件装配（{@code @ConditionalOnProperty}），未配置时不创建，导出失败只告警不阻断业务。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class OtlpObservationHandler implements ObservationHandler<Observation.Context> {

    private final Tracer tracer;

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }

    @Override
    public void onStart(Observation.Context context) {
        // 学习点：Observation 与 OTel span 的桥接——Observation 是 Micrometer 的抽象，
        // OTel span 是 OpenTelemetry 的模型；handler 在 start 时创建 OTel span 并通过
        // context.put() 暂存（Observation.Context 本身是个可挂载任意对象的 Map 容器），
        // 在 stop 时再取出 end()。span.makeCurrent() 建立"当前上下文"（类似 ThreadLocal），
        // 让 span 期间产生的日志/子 span 自动关联到它，scope 必须在 stop 时关闭防泄漏
        Span span = tracer.spanBuilder(context.getName()).startSpan();
        context.put("otel.span", span);
        Scope scope = span.makeCurrent();
        context.put("otel.scope", scope);
    }

    @Override
    public void onStop(Observation.Context context) {
        // 从 Observation.Context 取回 start 时暂存的 span/scope（跨 handler 生命周期传值）
        Span span = context.getOrDefault("otel.span", null);
        Scope scope = context.getOrDefault("otel.scope", null);
        try {
            if (span != null) {
                // span.end()：标记 span 结束并触发 BatchSpanProcessor 批量导出
                // （见 ObservationConfig.otelTracer 的装配说明）
                span.end();
            }
        } catch (Exception e) {
            // 导出失败仅告警：可观测链路不阻断主业务
            log.warn("OTLP span 导出失败: name={}, err={}", context.getName(), e.getMessage());
        } finally {
            // scope.close() 放在 finally：即使 end() 抛异常也要还原当前上下文，
            // 否则后续代码会错误地认为还处在某个 span 内（上下文串线）
            if (scope != null) {
                scope.close();
            }
        }
    }
}
