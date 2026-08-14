package com.aics.chat.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Trace 请求拦截器：在请求入口创建 TraceContext，请求结束时落库。
 *
 * <p>设计（见 design.md D5）：
 * <ul>
 *   <li><b>preHandle</b>：按采样率 {@code TraceContextHolder.begin(...)} 创建上下文，
 *       从请求头 {@code X-User-Id} 取用户、从参数 {@code sessionId}/{@code sessionKey} 取会话、
 *       按 URI 推导场景；</li>
 *   <li><b>afterCompletion</b>：标记整体状态（异常则 FAILED），经 {@link TraceRecorder}
 *       落库 llm_trace（失败仅告警），并清理 ThreadLocal/MDC（线程复用防脏数据）。</li>
 * </ul>
 * 异步链路（SSE 订阅回调、CompletableFuture）中通过 {@code TraceContextHolder.capture()/restore()}
 * 显式传播上下文。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TraceInterceptor implements HandlerInterceptor {

    private final ObservabilityProperties properties;
    private final TraceRecorder traceRecorder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头取用户（网关鉴权后透传）
        // 学习点：userId 走请求头 X-User-Id 而非登录态解析——网关已完成鉴权并透传，
        // 服务间不重复认证；非法值只降级为 null（不阻断请求），观测不能成为业务故障点
        Long userId = null;
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                userId = Long.valueOf(userIdHeader.trim());
            } catch (NumberFormatException ignore) {
                // 非法用户头不阻断请求
            }
        }
        // 从参数取会话（chat 接口 sessionId；history 接口 sessionKey）
        // 学习点：两个参数名并存是因为接口演进——history 接口用 sessionKey，
        // 对话接口用 sessionId；统一兜底读取保证所有入口都有会话维度可查
        String sessionId = request.getParameter("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = request.getParameter("sessionKey");
        }
        // 按 URI 推导场景
        String scenario = resolveScenario(request.getRequestURI());
        TraceContext ctx = TraceContextHolder.begin(properties, userId, sessionId, scenario);
        if (ctx != null) {
            log.debug("trace 开始: requestId={}, scenario={}", ctx.getRequestId(), scenario);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TraceContext ctx = TraceContextHolder.current();
        if (ctx == null) {
            return;
        }
        // 请求异常或 5xx：标记整体失败
        // 学习点：afterCompletion 是 Spring MVC 拦截器链的"最终收尾"——无论业务成功、
        // 抛异常还是响应 5xx，都会走到这里；在此统一标记整体状态并落库，
        // 业务代码无需关心 trace 何时持久化（SSE 场景则在 emitter 完成后触发）
        if (ex != null) {
            ctx.markFailed(truncate(ex.getMessage(), 500));
        } else if (response.getStatus() >= 500) {
            ctx.markFailed("HTTP " + response.getStatus());
        }
        // 落库（失败仅告警），并清理线程上下文与 MDC
        traceRecorder.record(ctx);
        TraceContextHolder.clear();
    }

    /** 按 URI 推导场景标识（用于用量与 trace 统计） */
    private String resolveScenario(String uri) {
        if (uri == null) {
            return "chat";
        }
        if (uri.contains("/agent")) {
            return "agent";
        }
        if (uri.contains("/vision")) {
            return "vision";
        }
        if (uri.contains("/rag")) {
            return "rag";
        }
        if (uri.contains("/stream")) {
            return "sse";
        }
        if (uri.contains("/history")) {
            return "history";
        }
        return "chat";
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
