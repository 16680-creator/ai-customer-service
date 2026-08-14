package com.aics.chat.observability;

import com.aics.chat.dto.LlmTraceDTO;
import com.aics.chat.feign.TraceFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Trace 记录器：请求结束时把 spans JSON + 元数据经 Feign 落库 llm_trace。
 *
 * <p>审计尽力而为原则（与 {@code AgentTraceRecorder} 一致）：落库失败只告警，
 * 不阻断业务；spans 序列化失败时降级为仅元数据上报。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceRecorder {

    private final TraceFeignClient traceFeignClient;
    private final ObjectMapper objectMapper;

    /**
     * 上报一次请求的完整调用链（幂等键 requestId）。
     *
     * @param ctx 请求上下文（含 spans）
     */
    public void record(TraceContext ctx) {
        if (ctx == null) {
            return;
        }
        try {
            LlmTraceDTO dto = new LlmTraceDTO();
            dto.setRequestId(ctx.getRequestId());
            dto.setUserId(ctx.getUserId());
            dto.setSessionId(ctx.getSessionId());
            dto.setScenario(ctx.getScenario());
            dto.setStatus(ctx.getStatus());
            dto.setTotalDurationMs(ctx.totalDurationMs());
            dto.setErrorSummary(ctx.getErrorSummary());
            // spans 序列化失败时降级为仅元数据上报
            // 学习点：审计链路遵循"尽力而为"原则——trace 是观测数据不是业务数据，
            // 它的丢失绝不能反过来影响业务（比如让用户请求失败）。
            // 因此序列化与 Feign 调用都包在 try-catch 里，失败只打 WARN 日志
            try {
                dto.setSpansJson(objectMapper.writeValueAsString(ctx.getSpans()));
            } catch (Exception e) {
                log.warn("trace spans 序列化失败，仅上报元数据: requestId={}, err={}",
                        ctx.getRequestId(), e.getMessage());
                dto.setSpansJson("[]");
            }
            traceFeignClient.createTrace(dto);
        } catch (Exception e) {
            // 落库失败仅告警：审计尽力而为，不阻断业务
            log.warn("LLM trace 落库失败: requestId={}, err={}", ctx.getRequestId(), e.getMessage());
        }
    }
}
