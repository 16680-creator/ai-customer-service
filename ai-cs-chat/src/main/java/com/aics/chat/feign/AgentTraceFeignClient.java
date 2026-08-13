package com.aics.chat.feign;

import com.aics.chat.dto.AgentConfirmationDTO;
import com.aics.chat.dto.AgentRunDTO;
import com.aics.chat.dto.AgentRunStatusDTO;
import com.aics.chat.dto.AgentStepDTO;
import com.aics.chat.dto.HandoffTicketDTO;
import com.aics.chat.dto.HandoffTicketVO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 消息服务 Agent 轨迹 Feign 客户端（调用 ai-cs-message 持久化执行轨迹与工单）
 */
@FeignClient(name = "ai-cs-message")
public interface AgentTraceFeignClient {

    /**
     * 创建 Agent 执行记录（幂等：runId 已存在返回原 runId）
     */
    @PostMapping("/api/agent/runs")
    Result<String> createRun(@RequestBody AgentRunDTO dto);

    /**
     * 更新 Agent 执行状态
     */
    @PutMapping("/api/agent/runs/{runId}/status")
    Result<Void> updateRunStatus(@PathVariable("runId") String runId,
                                 @RequestBody AgentRunStatusDTO dto);

    /**
     * 追加步骤轨迹（同 runId+stepNo 幂等）
     */
    @PostMapping("/api/agent/runs/{runId}/steps")
    Result<Void> appendStep(@PathVariable("runId") String runId,
                            @RequestBody AgentStepDTO dto);

    /**
     * 记录写操作确认
     */
    @PostMapping("/api/agent/runs/{runId}/confirmations")
    Result<Void> recordConfirmation(@PathVariable("runId") String runId,
                                    @RequestBody AgentConfirmationDTO dto);

    /**
     * 创建转人工工单
     */
    @PostMapping("/api/agent/handoff-tickets")
    Result<HandoffTicketVO> createHandoffTicket(@RequestBody HandoffTicketDTO dto);

    /**
     * 查询 run 详情（审计回放）
     */
    @GetMapping("/api/agent/runs/{runId}")
    Result<Map<String, Object>> getRunDetail(@PathVariable("runId") String runId);
}
