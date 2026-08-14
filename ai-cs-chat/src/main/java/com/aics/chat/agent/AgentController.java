package com.aics.chat.agent;

import com.aics.chat.agent.model.AgentTurnResult;
import com.aics.chat.agent.workflow.AfterSaleAgentService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    private static Long resolveUserId(Long userId) {
        return userId == null ? 0L : userId;
    }
}
