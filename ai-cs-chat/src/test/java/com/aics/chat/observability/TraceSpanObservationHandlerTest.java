package com.aics.chat.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceSpanObservationHandler 单元测试：Observation 组装为 TraceSpan 挂到当前 TraceContext。
 */
class TraceSpanObservationHandlerTest {

    private final ObservabilityProperties properties = new ObservabilityProperties();

    private ObservationRegistry newRegistry() {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(new TraceSpanObservationHandler(properties));
        return registry;
    }

    @Test
    @DisplayName("onStop 把 Observation 组装为 TraceSpan（spanType/provider/model/status）")
    void onStop_assemblesSpan() {
        properties.setLogExport(false);
        ObservabilityProperties props = new ObservabilityProperties();
        props.setEnabled(true);
        props.setSampleRate(1.0);
        TraceContext ctx = TraceContextHolder.begin(props, 1L, "s1", "chat");
        ObservationRegistry registry = newRegistry();

        Observation.createNotStarted("chat.llm", registry)
                .lowCardinalityKeyValue("span.type", "LLM")
                .lowCardinalityKeyValue("provider", "deepseek")
                .lowCardinalityKeyValue("model", "deepseek-chat")
                .highCardinalityKeyValue("promptTokens", "100")
                .observe(() -> {
                });

        assertEquals(1, ctx.getSpans().size());
        TraceSpan span = ctx.getSpans().get(0);
        assertEquals("chat.llm", span.getName());
        assertEquals("LLM", span.getSpanType());
        assertEquals("deepseek", span.getProvider());
        assertEquals("deepseek-chat", span.getModel());
        assertEquals(100, span.getPromptTokens());
        assertEquals("SUCCESS", span.getStatus());
        TraceContextHolder.clear();
    }

    @Test
    @DisplayName("Observation 异常时 span 标记 FAILED 并记录错误摘要")
    void onStop_errorMarksFailed() {
        properties.setLogExport(false);
        ObservabilityProperties props = new ObservabilityProperties();
        props.setEnabled(true);
        props.setSampleRate(1.0);
        TraceContext ctx = TraceContextHolder.begin(props, 1L, "s1", "chat");
        ObservationRegistry registry = newRegistry();

        Observation observation = Observation.createNotStarted("chat.llm", registry)
                .lowCardinalityKeyValue("span.type", "LLM");
        observation.start();
        observation.error(new RuntimeException("boom"));
        observation.stop();

        assertEquals(1, ctx.getSpans().size());
        assertEquals("FAILED", ctx.getSpans().get(0).getStatus());
        assertTrue(ctx.getSpans().get(0).getErrorSummary().contains("boom"));
        TraceContextHolder.clear();
    }

    @Test
    @DisplayName("当前线程无 TraceContext 时 handler 跳过（埋点零开销）")
    void onStop_noContext_skips() {
        properties.setLogExport(false);
        ObservationRegistry registry = newRegistry();

        assertDoesNotThrow(() -> Observation.createNotStarted("chat.llm", registry)
                .lowCardinalityKeyValue("span.type", "LLM")
                .observe(() -> {
                }));
    }
}
