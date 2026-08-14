package com.aics.chat.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceContextHolder 单元测试：begin/current/capture/restore/clear 与采样开关、MDC 关联。
 */
class TraceContextHolderTest {

    @Test
    @DisplayName("begin 创建上下文并写入 MDC requestId")
    void begin_createsContextAndMdc() {
        ObservabilityProperties props = new ObservabilityProperties();
        props.setEnabled(true);
        props.setSampleRate(1.0);

        TraceContext ctx = TraceContextHolder.begin(props, 100L, "session-1", "chat");

        assertNotNull(ctx);
        assertEquals(100L, ctx.getUserId());
        assertEquals("session-1", ctx.getSessionId());
        assertEquals("chat", ctx.getScenario());
        assertEquals(ctx.getRequestId(), org.slf4j.MDC.get("requestId"));
        TraceContextHolder.clear();
    }

    @Test
    @DisplayName("关闭观测时 begin 返回 null")
    void begin_disabled_returnsNull() {
        ObservabilityProperties props = new ObservabilityProperties();
        props.setEnabled(false);

        assertNull(TraceContextHolder.begin(props, 100L, "s1", "chat"));
        TraceContextHolder.clear();
    }

    @Test
    @DisplayName("采样率 0 时不创建上下文")
    void begin_zeroSampleRate_returnsNull() {
        ObservabilityProperties props = new ObservabilityProperties();
        props.setEnabled(true);
        props.setSampleRate(0);

        assertNull(TraceContextHolder.begin(props, 100L, "s1", "chat"));
        TraceContextHolder.clear();
    }

    @Test
    @DisplayName("capture/restore 支持跨异步边界传播（同一上下文引用，span 可并发追加）")
    void captureRestore_propagatesAcrossAsync() throws Exception {
        ObservabilityProperties props = new ObservabilityProperties();
        props.setEnabled(true);
        props.setSampleRate(1.0);
        TraceContext ctx = TraceContextHolder.begin(props, 1L, "s1", "chat");
        TraceContext captured = TraceContextHolder.capture();

        // 模拟异步线程：restore 后追加 span，主线程可见
        Thread t = new Thread(() -> {
            TraceContextHolder.restore(captured);
            TraceSpan span = new TraceSpan();
            span.setSpanType("LLM");
            span.setName("chat.llm");
            span.setStatus("SUCCESS");
            TraceContextHolder.current().addSpan(span);
            TraceContextHolder.clear();
        });
        t.start();
        t.join();

        assertEquals(1, ctx.getSpans().size());
        assertEquals("LLM", ctx.getSpans().get(0).getSpanType());
        TraceContextHolder.clear();
    }

    @Test
    @DisplayName("clear 清理 ThreadLocal 与 MDC")
    void clear_removesContextAndMdc() {
        ObservabilityProperties props = new ObservabilityProperties();
        props.setEnabled(true);
        props.setSampleRate(1.0);
        TraceContextHolder.begin(props, 1L, "s1", "chat");

        TraceContextHolder.clear();

        assertNull(TraceContextHolder.current());
        assertNull(org.slf4j.MDC.get("requestId"));
    }

    @Test
    @DisplayName("无上下文时 current/capture 返回 null，restore(null) 安全")
    void empty_threadIsNullSafe() {
        assertNull(TraceContextHolder.current());
        assertNull(TraceContextHolder.capture());
        assertDoesNotThrow(() -> TraceContextHolder.restore(null));
    }
}
