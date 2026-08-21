package com.aics.chat.agent;

import com.aics.chat.agent.model.AgentTurnResult;
import com.aics.chat.agent.workflow.AfterSaleAgentService;
import com.aics.chat.agent.workflow.AgentTurnListener;
import com.aics.chat.dto.AgentConfirmRequestDTO;
import com.aics.chat.dto.AgentRequestDTO;
import com.aics.chat.feign.AgentTraceFeignClient;
import com.aics.chat.service.ChatService;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 智能客服 Agent 接口：售后工作流、写操作确认、轨迹回放
 */
@Slf4j
@RestController
@RequestMapping("/chat/agent")
@RequiredArgsConstructor
@Tag(name = "智能客服 Agent", description = "售后 Agent 编排、写操作确认与执行轨迹")
public class AgentController {

    private final AfterSaleAgentService afterSaleAgentService;
    private final ChatService chatService;
    private final AgentTraceFeignClient agentTraceFeignClient;

    /**
     * Agent 多轮对话（售后工作流）
     */
    @PostMapping
    @Operation(summary = "Agent 多轮对话", description = "输入安全校验、意图识别、状态机编排；返回回复与确认凭证")
    public Result<AgentTurnResult> agent(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                         @Valid @RequestBody AgentRequestDTO request) {
        Long uid = resolveUserId(userId);
        // 写入线程上下文，供 Feign 调用透传 X-User-Id
        ChatUserContext.setUserId(uid);
        try {
            // 进入 Agent 编排：新 run 或续跑
            AgentTurnResult result = afterSaleAgentService.handleTurn(
                    uid, request.getSessionId(), request.getRunId(), request.getInput());
            // 非售后意图路由回普通对话
            if (result.routeToNormalChat() && request.getInput() != null) {
                // 委托普通对话服务生成回复
                Result<String> chatResult = chatService.chat(
                        String.valueOf(request.getSessionId() == null ? 0 : request.getSessionId()),
                        request.getInput());
                String reply = chatResult != null && chatResult.getData() != null
                        ? chatResult.getData() : "AI 助手暂时繁忙，请稍后重试";
                return Result.success(AgentTurnResult.of(result.runId(), "NORMAL_CHAT", result.intents(),
                        reply, false, false, null, null, result.candidates(),
                        result.handoff(), null, null));
            }
            return Result.success(result);
        } finally {
            // 清理线程上下文，防止用户串号
            ChatUserContext.clear();
        }
    }

    /**
     * 写操作确认/拒绝（Human-in-the-loop）
     */
    @PostMapping("/confirm")
    @Operation(summary = "写操作确认", description = "使用 Agent 返回的确认凭证确认或拒绝售后操作")
    public Result<AgentTurnResult> confirm(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @Valid @RequestBody AgentConfirmRequestDTO request) {
        Long uid = resolveUserId(userId);
        ChatUserContext.setUserId(uid);
        try {
            // 决策转译为确认/拒绝文本，复用 handleTurn 的确认分支
            String decision = "CONFIRM".equalsIgnoreCase(request.getDecision()) ? "确认" : "拒绝";
            AgentTurnResult result = afterSaleAgentService.handleTurn(
                    uid, request.getSessionId(), request.getRunId(), decision);
            return Result.success(result);
        } finally {
            ChatUserContext.clear();
        }
    }

    /**
     * 查询执行轨迹（审计回放）
     */
    @GetMapping("/runs/{runId}")
    @Operation(summary = "查询执行轨迹", description = "按 runId 回放 Agent 执行的完整步骤轨迹")
    public Result<Map<String, Object>> runDetail(@PathVariable("runId") String runId) {
        // 直接透传轨迹服务的审计回放结果
        return agentTraceFeignClient.getRunDetail(runId);
    }

    /**
     * 健康检查（前端 Agent 页面加载时探测服务可用性）
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "探测 Agent 编排服务是否可用")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of("status", "UP"));
    }

    /** 流式编排 SSE 超时：需覆盖编排总超时（AgentProperties.totalTimeoutMs）+ LLM 生成耗时 */
    private static final long AGENT_STREAM_TIMEOUT = 120_000L;

    /**
     * Agent 流式对话（SSE）：步骤进度与回复 token 实时推送
     *
     * <p>事件协议（对齐普通对话 SSE 风格）：</p>
     * <ul>
     *   <li>{@code {"step":"INTENT","detail":"..."}} —— 编排步骤进度</li>
     *   <li>{@code {"content":"..."}} —— 回复 token（仅普通对话路由时产生）</li>
     *   <li>{@code {"done":true,"result":{...}}} —— 结束信号，携带完整 AgentTurnResult</li>
     *   <li>{@code {"error":"..."}} —— 失败原因</li>
     * </ul>
     */
    @PostMapping(value = "/stream/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Agent 流式对话", description = "SSE 推送编排步骤事件与回复 token，done 事件携带完整结果")
    public SseEmitter agentStream(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                  @Valid @RequestBody AgentRequestDTO request) {
        Long uid = resolveUserId(userId);
        SseEmitter emitter = new SseEmitter(AGENT_STREAM_TIMEOUT);
        // 立即返回 emitter，编排提交工作线程异步执行——步骤事件随编排进度实时推送，
        // 若在控制器线程同步执行，响应未提交前 send() 会失败
        CompletableFuture.runAsync(() -> runAgentTurn(emitter, uid, request));
        return emitter;
    }

    /**
     * 执行一轮流式编排（包级可见便于单测同步调用）。
     * 工作线程内必须重设 ChatUserContext：runTool 的工具授权依赖它读取用户身份。
     */
    void runAgentTurn(SseEmitter emitter, Long uid, AgentRequestDTO request) {
        try {
            ChatUserContext.setUserId(uid);
            AgentTurnListener listener = new AgentTurnListener() {
                @Override
                public void onStep(String phase, String detail) {
                    sendEvent(emitter, Map.of("step", phase, "detail", detail == null ? "" : detail));
                }

                @Override
                public void onToken(String chunk) {
                    sendEvent(emitter, Map.of("content", chunk));
                }
            };
            AgentTurnResult result = afterSaleAgentService.handleTurn(
                    uid, request.getSessionId(), request.getRunId(), request.getInput(), listener);
            // 非售后意图路由回普通对话：token 流式生成回复后构造最终结果
            if (result.routeToNormalChat() && request.getInput() != null) {
                String sessionId = String.valueOf(
                        request.getSessionId() == null ? 0 : request.getSessionId());
                String reply = chatService.streamReply(sessionId, request.getInput(), listener::onToken);
                result = AgentTurnResult.of(result.runId(), "NORMAL_CHAT", result.intents(),
                        reply, false, false, null, null, result.candidates(),
                        result.handoff(), null, null);
            }
            sendEvent(emitter, Map.of("done", true, "result", result));
            emitter.complete();
        } catch (Exception e) {
            log.error("Agent 流式编排失败: sessionId={}, err={}", request.getSessionId(), e.getMessage(), e);
            sendEvent(emitter, Map.of("error", e.getMessage() == null ? "系统内部错误" : e.getMessage()));
            emitter.completeWithError(e);
        } finally {
            // 清理线程上下文，防止线程池复用导致用户串号
            ChatUserContext.clear();
        }
    }

    /** 尽力推送：客户端已断开或 emitter 未就绪时静默忽略（SSE 尽力推送语义） */
    private void sendEvent(SseEmitter emitter, Object payload) {
        try {
            emitter.send(SseEmitter.event().data(payload));
        } catch (Exception ignore) {
            // ignore
        }
    }

    private static Long resolveUserId(Long userId) {
        return userId == null ? 0L : userId;
    }
}
