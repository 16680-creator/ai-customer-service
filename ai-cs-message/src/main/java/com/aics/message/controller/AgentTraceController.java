package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.AgentConfirmationDTO;
import com.aics.message.dto.AgentRunDTO;
import com.aics.message.dto.AgentStepDTO;
import com.aics.message.dto.HandoffTicketDTO;
import com.aics.message.service.AgentTraceService;
import com.aics.message.vo.AgentRunDetailVO;
import com.aics.message.vo.HandoffTicketVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Agent 执行轨迹控制器
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：对外暴露 Agent 执行轨迹的 REST 接口（/api/agent/*），供 chat 模块 Agent
 * 编排链路（经 OpenFeign）记录与回放执行过程，包括：
 * <ul>
 *     <li>创建执行记录（幂等）；</li>
 *     <li>更新执行状态；</li>
 *     <li>追加步骤轨迹（幂等）；</li>
 *     <li>记录写操作确认（幂等）；</li>
 *     <li>创建转人工工单；</li>
 *     <li>查询执行详情（审计回放）。</li>
 * </ul>
 * 统一返回 {@link Result} 包装结构。
 * </p>
 */
@Tag(name = "Agent 执行轨迹")
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
public class AgentTraceController {

    /** Agent 执行轨迹服务 */
    private final AgentTraceService agentTraceService;

    /**
     * 创建 Agent 执行记录（幂等：runId 已存在时返回已有 runId）
     *
     * @param dto 执行记录信息
     * @return 执行ID（runId）
     */
    @Operation(summary = "创建 Agent 执行记录")
    @PostMapping("/runs")
    public Result<String> createRun(@Valid @RequestBody AgentRunDTO dto) {
        return Result.success(agentTraceService.createRun(dto));
    }

    /**
     * 更新 Agent 执行状态与当前步骤
     *
     * @param runId 执行ID
     * @param body  状态更新体：status（必填）、currentStep（可选）、errorSummary（可选）
     * @return 空结果包装
     */
    @Operation(summary = "更新 Agent 执行状态")
    @PutMapping("/runs/{runId}/status")
    public Result<Void> updateRunStatus(@PathVariable("runId") String runId,
                                        @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        Integer currentStep = body.get("currentStep") == null
                ? null : ((Number) body.get("currentStep")).intValue();
        String errorSummary = (String) body.get("errorSummary");
        agentTraceService.updateRunStatus(runId, status, currentStep, errorSummary);
        return Result.success();
    }

    /**
     * 追加 Agent 步骤轨迹（同 runId+stepNo 已存在时覆盖更新）
     *
     * @param runId 执行ID
     * @param dto   步骤信息
     * @return 空结果包装
     */
    @Operation(summary = "追加 Agent 步骤轨迹")
    @PostMapping("/runs/{runId}/steps")
    public Result<Void> appendStep(@PathVariable("runId") String runId,
                                   @Valid @RequestBody AgentStepDTO dto) {
        agentTraceService.appendStep(runId, dto);
        return Result.success();
    }

    /**
     * 记录 Agent 写操作确认（同 runId+action 已存在时覆盖更新）
     *
     * @param runId 执行ID
     * @param dto   确认信息
     * @return 空结果包装
     */
    @Operation(summary = "记录 Agent 写操作确认")
    @PostMapping("/runs/{runId}/confirmations")
    public Result<Void> recordConfirmation(@PathVariable("runId") String runId,
                                           @Valid @RequestBody AgentConfirmationDTO dto) {
        agentTraceService.recordConfirmation(runId, dto);
        return Result.success();
    }

    /**
     * 创建转人工工单
     *
     * @param dto 工单信息
     * @return 工单号 + 状态
     */
    @Operation(summary = "创建转人工工单")
    @PostMapping("/handoff-tickets")
    public Result<HandoffTicketVO> createHandoffTicket(@Valid @RequestBody HandoffTicketDTO dto) {
        return Result.success(agentTraceService.createHandoffTicket(dto));
    }

    /**
     * 查询 Agent 执行详情（run + 按 stepNo 升序的 steps，审计回放）
     *
     * @param runId 执行ID
     * @return 执行详情
     */
    @Operation(summary = "查询 Agent 执行轨迹详情")
    @GetMapping("/runs/{runId}")
    public Result<AgentRunDetailVO> getRunDetail(@PathVariable("runId") String runId) {
        return Result.success(agentTraceService.getRunDetail(runId));
    }
}
