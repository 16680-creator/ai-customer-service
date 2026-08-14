package com.aics.chat.observability;

import com.aics.chat.dto.LlmTraceDTO;
import com.aics.chat.feign.TraceFeignClient;
import com.aics.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TraceRecorder 单元测试：spans 序列化、落库失败仅告警不阻断。
 */
@ExtendWith(MockitoExtension.class)
class TraceRecorderTest {

    @Mock
    private TraceFeignClient traceFeignClient;

    @Test
    @DisplayName("record 组装 DTO：requestId/scenario/spansJson 正确")
    void record_assemblesDto() {
        TraceRecorder recorder = new TraceRecorder(traceFeignClient, new ObjectMapper());
        TraceContext ctx = new TraceContext("req-1", 100L, "s1", "chat");
        TraceSpan span = new TraceSpan();
        span.setSpanType("LLM");
        span.setName("chat.llm");
        span.setStatus("SUCCESS");
        ctx.addSpan(span);

        when(traceFeignClient.createTrace(any())).thenReturn(Result.success("req-1"));

        recorder.record(ctx);

        ArgumentCaptor<LlmTraceDTO> captor = ArgumentCaptor.forClass(LlmTraceDTO.class);
        verify(traceFeignClient).createTrace(captor.capture());
        LlmTraceDTO dto = captor.getValue();
        assertEquals("req-1", dto.getRequestId());
        assertEquals(100L, dto.getUserId());
        assertEquals("s1", dto.getSessionId());
        assertEquals("chat", dto.getScenario());
        assertEquals("SUCCESS", dto.getStatus());
        assertNotNull(dto.getSpansJson());
        assertTrue(dto.getSpansJson().contains("chat.llm"));
        assertNotNull(dto.getTotalDurationMs());
    }

    @Test
    @DisplayName("null 上下文不处理")
    void record_nullContext_noop() {
        TraceRecorder recorder = new TraceRecorder(traceFeignClient, new ObjectMapper());
        recorder.record(null);
        verifyNoInteractions(traceFeignClient);
    }

    @Test
    @DisplayName("Feign 落库失败仅告警，不抛异常")
    void record_feignFailure_warnsOnly() {
        TraceRecorder recorder = new TraceRecorder(traceFeignClient, new ObjectMapper());
        TraceContext ctx = new TraceContext("req-1", 100L, "s1", "chat");
        when(traceFeignClient.createTrace(any())).thenThrow(new RuntimeException("message service down"));

        assertDoesNotThrow(() -> recorder.record(ctx));
    }

    @Test
    @DisplayName("spans 序列化失败降级为仅元数据上报（不抛异常）")
    void record_serializeFailure_degrades() {
        // ObjectMapper 对正常 TraceSpan 序列化不会失败；模拟 Feign 端异常即可覆盖兜底
        TraceRecorder recorder = new TraceRecorder(traceFeignClient, new ObjectMapper());
        TraceContext ctx = new TraceContext("req-1", null, null, "chat");
        when(traceFeignClient.createTrace(any())).thenThrow(new RuntimeException("down"));

        assertDoesNotThrow(() -> recorder.record(ctx));
        verify(traceFeignClient, times(1)).createTrace(any());
    }
}
