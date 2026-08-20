package com.aics.chat.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceInterceptor 单元测试：请求入口创建上下文、场景推导、异常标记 FAILED、清理。
 */
class TraceInterceptorTest {

    private final ObservabilityProperties properties = new ObservabilityProperties();
    private final TraceRecorder recorder = new TraceRecorder(null, null) {
        // 不触发 Feign（null client 会 NPE，这里覆盖为 no-op）
        @Override
        public void record(TraceContext ctx) {
        }
    };
    private final TraceInterceptor interceptor = new TraceInterceptor(properties, () -> recorder);

    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
    }

    @Test
    @DisplayName("preHandle 从请求头/参数创建上下文，afterCompletion 落库并清理")
    void preHandle_createsContext() {
        properties.setSampleRate(1.0);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat/rag");
        request.addHeader("X-User-Id", "42");
        request.addParameter("sessionId", "s100");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        TraceContext ctx = TraceContextHolder.current();
        assertNotNull(ctx);
        assertEquals(42L, ctx.getUserId());
        assertEquals("s100", ctx.getSessionId());
        assertEquals("rag", ctx.getScenario());

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertNull(TraceContextHolder.current());
    }

    @Test
    @DisplayName("异常时 afterCompletion 标记整体 FAILED")
    void afterCompletion_exception_marksFailed() {
        properties.setSampleRate(1.0);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat/send");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        TraceContext ctx = TraceContextHolder.current();
        assertNotNull(ctx);

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(),
                new RuntimeException("boom"));

        assertEquals("FAILED", ctx.getStatus());
        assertTrue(ctx.getErrorSummary().contains("boom"));
        assertNull(TraceContextHolder.current());
    }

    @Test
    @DisplayName("采样率 0 时不创建上下文")
    void preHandle_zeroSample_noContext() {
        properties.setSampleRate(0);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat/send");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertNull(TraceContextHolder.current());
    }

    @Test
    @DisplayName("preHandle 不解析 TraceRecorder，避免启动期提前创建 Feign 客户端")
    void preHandle_doesNotResolveTraceRecorder() {
        properties.setSampleRate(1.0);
        AtomicBoolean resolved = new AtomicBoolean(false);
        ObjectFactory<TraceRecorder> recorderProvider = () -> {
            resolved.set(true);
            throw new IllegalStateException("TraceRecorder should be lazy");
        };
        TraceInterceptor lazyInterceptor = new TraceInterceptor(properties, recorderProvider);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat/send");

        assertTrue(lazyInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        assertFalse(resolved.get());
        assertNotNull(TraceContextHolder.current());
    }

    @Test
    @DisplayName("场景推导：agent/vision/stream/history/chat")
    void resolveScenario_mapping() {
        assertScenario("/agent", "agent");
        assertScenario("/chat/vision", "vision");
        assertScenario("/chat/stream/sse", "sse");
        assertScenario("/chat/history", "history");
        assertScenario("/chat/send", "chat");
    }

    private void assertScenario(String uri, String expected) {
        properties.setSampleRate(1.0);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        TraceContext ctx = TraceContextHolder.current();
        assertNotNull(ctx);
        assertEquals(expected, ctx.getScenario());
        TraceContextHolder.clear();
    }
}
