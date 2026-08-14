package com.aics.message.service;

import com.aics.message.dto.AgentConfirmationDTO;
import com.aics.message.dto.AgentRunDTO;
import com.aics.message.dto.AgentStepDTO;
import com.aics.message.dto.HandoffTicketDTO;
import com.aics.message.vo.AgentRunDetailVO;
import com.aics.message.vo.HandoffTicketVO;

/**
 * Agent 执行轨迹服务接口
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：定义 Agent 执行轨迹（agent_run/agent_step/agent_confirmation/handoff_ticket 四表）
 * 的持久化与查询能力，供 chat 模块 Agent 编排链路通过 Feign 调用，实现审计回放与人工转接。
 * 幂等约定：
 * <ul>
 *     <li>createRun：runId 已存在时直接返回已有 runId；</li>
 *     <li>appendStep：同 (runId, stepNo) 已存在时覆盖更新；</li>
 *     <li>recordConfirmation：同 (runId, action) 已存在时覆盖更新。</li>
 * </ul>
 * 实现类：{@link com.aics.message.service.impl.AgentTraceServiceImpl}。
 * 调用方：{@link com.aics.message.controller.AgentTraceController}。
 * </p>
 */
// 幂等键约定：createRun 按 runId 幂等；appendStep 按 (runId, stepNo)、recordConfirmation 按 (runId, action) 覆盖更新
public interface AgentTraceService {

    // ==================== 执行轨迹写入（均幂等） ====================

    /**
     * 创建 Agent 执行记录（幂等：runId 已存在直接返回）
     *
     * @param dto 执行记录信息
     * @return 执行ID（runId）
     */
    String createRun(AgentRunDTO dto);

    /**
     * 更新 Agent 执行状态（不存在抛 AGENT_RUN_NOT_FOUND）
     *
     * @param runId        执行ID
     * @param status       目标状态
     * @param currentStep  当前步骤号（可为空）
     * @param errorSummary 失败摘要（可为空）
     */
    void updateRunStatus(String runId, String status, Integer currentStep, String errorSummary);

    /**
     * 追加 Agent 步骤轨迹（同 runId+stepNo 已存在时覆盖更新，幂等）
     *
     * @param runId 执行ID
     * @param dto   步骤信息
     */
    void appendStep(String runId, AgentStepDTO dto);

    /**
     * 记录 Agent 写操作确认（同 runId+action 已存在时覆盖更新，幂等）
     *
     * @param runId 执行ID
     * @param dto   确认信息
     */
    void recordConfirmation(String runId, AgentConfirmationDTO dto);

    // ==================== 人工转接与审计查询 ====================

    /**
     * 创建转人工工单（生成 HF 前缀唯一工单号）
     *
     * @param dto 工单信息
     * @return 工单号 + 状态
     */
    HandoffTicketVO createHandoffTicket(HandoffTicketDTO dto);

    /**
     * 查询 Agent 执行详情（run + 按 stepNo 升序的 steps；不存在抛 AGENT_RUN_NOT_FOUND）
     *
     * @param runId 执行ID
     * @return 执行详情
     */
    AgentRunDetailVO getRunDetail(String runId);
}
