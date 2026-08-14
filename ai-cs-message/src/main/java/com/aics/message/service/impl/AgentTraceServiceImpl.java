package com.aics.message.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.message.dto.AgentConfirmationDTO;
import com.aics.message.dto.AgentRunDTO;
import com.aics.message.dto.AgentStepDTO;
import com.aics.message.dto.HandoffTicketDTO;
import com.aics.message.entity.AgentConfirmation;
import com.aics.message.entity.AgentRun;
import com.aics.message.entity.AgentStep;
import com.aics.message.entity.HandoffTicket;
import com.aics.message.mapper.AgentConfirmationMapper;
import com.aics.message.mapper.AgentRunMapper;
import com.aics.message.mapper.AgentStepMapper;
import com.aics.message.mapper.HandoffTicketMapper;
import com.aics.message.service.AgentTraceService;
import com.aics.message.vo.AgentRunDetailVO;
import com.aics.message.vo.HandoffTicketVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Agent 执行轨迹服务实现
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：实现 {@link AgentTraceService}，基于 MyBatis-Plus Mapper 完成四张轨迹表的读写。
 * 设计要点：
 * <ul>
 *     <li>创建/追加/记录均先查后写，实现幂等语义（重复上报返回首次结果或覆盖更新）；</li>
 *     <li>状态更新与详情查询前校验 run 存在，不存在统一抛 {@link BusinessException}(AGENT_RUN_NOT_FOUND)；</li>
 *     <li>工单号 = HF + yyyyMMddHHmmss + 4 位随机数字（{@link RandomUtil#randomNumbers(int)}），
 *     由服务端生成以保证唯一；</li>
 *     <li>详情查询对步骤做内存二次升序排序（防御性），保证审计回放顺序稳定。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTraceServiceImpl implements AgentTraceService {

    /** 工单号时间戳格式：yyyyMMddHHmmss */
    private static final DateTimeFormatter TICKET_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** Agent 执行记录 Mapper */
    private final AgentRunMapper agentRunMapper;
    /** Agent 步骤轨迹 Mapper */
    private final AgentStepMapper agentStepMapper;
    /** Agent 写操作确认 Mapper */
    private final AgentConfirmationMapper agentConfirmationMapper;
    /** 转人工工单 Mapper */
    private final HandoffTicketMapper handoffTicketMapper;

    /**
     * 创建执行记录：runId 已存在时幂等返回已有 runId，否则插入新记录。
     * 默认状态 RUNNING、当前步骤 0 由实体字段初始值保证（DTO 未传时生效）。
     */
    @Override
    public String createRun(AgentRunDTO dto) {
        // 1. 幂等检查：按 runId 查询，已存在则直接返回首次创建的 runId，避免重复建单
        AgentRun existing = agentRunMapper.selectById(dto.getRunId());
        if (existing != null) {
            log.info("Agent 执行记录已存在，幂等返回: runId={}", dto.getRunId());
            return existing.getRunId();
        }
        // 2. 组装新执行记录（可选字段为空时沿用实体默认值：RUNNING / 0）
        AgentRun run = new AgentRun();
        run.setRunId(dto.getRunId());
        run.setSessionId(dto.getSessionId());
        run.setUserId(dto.getUserId());
        run.setIntent(dto.getIntent());
        run.setSentiment(dto.getSentiment());
        if (dto.getStatus() != null) {
            run.setStatus(dto.getStatus());
        }
        if (dto.getCurrentStep() != null) {
            run.setCurrentStep(dto.getCurrentStep());
        }
        run.setPromptVersion(dto.getPromptVersion());
        run.setErrorSummary(dto.getErrorSummary());
        // 3. 落库并返回新 runId（createTime/updateTime 由 MetaObjectHandler 自动填充）
        agentRunMapper.insert(run);
        log.info("Agent 执行记录创建成功: runId={}", run.getRunId());
        return run.getRunId();
    }

    /**
     * 更新执行状态：按 runId 更新 status/currentStep/errorSummary（null 字段不参与 SET）。
     * 记录不存在时抛 {@link ResultCode#AGENT_RUN_NOT_FOUND}。
     */
    @Override
    public void updateRunStatus(String runId, String status, Integer currentStep, String errorSummary) {
        // 1. 前置校验：执行记录不存在则抛 AGENT_RUN_NOT_FOUND，避免对孤儿记录做状态流转
        AgentRun existing = agentRunMapper.selectById(runId);
        if (existing == null) {
            throw new BusinessException(ResultCode.AGENT_RUN_NOT_FOUND);
        }
        // 2. 构建更新对象并落库（updateById 仅更新非 null 字段，支持 status/currentStep/errorSummary 部分更新）
        AgentRun update = new AgentRun();
        update.setRunId(runId);
        update.setStatus(status);
        update.setCurrentStep(currentStep);
        update.setErrorSummary(errorSummary);
        agentRunMapper.updateById(update);
        log.info("Agent 执行状态已更新: runId={}, status={}, currentStep={}", runId, status, currentStep);
    }

    /**
     * 追加步骤：校验 run 存在；同 (runId, stepNo) 已存在时覆盖更新（幂等），否则插入。
     * 默认状态 SUCCESS、耗时 0 由实体字段初始值保证。
     */
    @Override
    public void appendStep(String runId, AgentStepDTO dto) {
        // 1. 前置校验：执行记录必须存在，避免写入孤儿步骤轨迹
        AgentRun run = agentRunMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException(ResultCode.AGENT_RUN_NOT_FOUND);
        }
        // 2. 按 (runId, stepNo) 查询：同一步骤已存在则覆盖更新（幂等）
        LambdaQueryWrapper<AgentStep> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentStep::getRunId, runId).eq(AgentStep::getStepNo, dto.getStepNo());
        AgentStep existing = agentStepMapper.selectOne(wrapper);
        if (existing != null) {
            // 覆盖更新已有步骤的内容（保留原主键 id 与创建时间）
            existing.setStepType(dto.getStepType());
            existing.setToolName(dto.getToolName());
            existing.setInputDigest(dto.getInputDigest());
            existing.setOutputDigest(dto.getOutputDigest());
            if (dto.getDurationMs() != null) {
                existing.setDurationMs(dto.getDurationMs());
            }
            if (dto.getStatus() != null) {
                existing.setStatus(dto.getStatus());
            }
            existing.setErrorSummary(dto.getErrorSummary());
            agentStepMapper.updateById(existing);
            log.info("Agent 步骤已覆盖更新（幂等）: runId={}, stepNo={}", runId, dto.getStepNo());
            return;
        }
        // 3. 该步骤首次上报：插入新步骤轨迹（默认状态 SUCCESS、耗时 0 由实体初始值保证）
        AgentStep step = new AgentStep();
        step.setRunId(runId);
        step.setStepNo(dto.getStepNo());
        step.setStepType(dto.getStepType());
        step.setToolName(dto.getToolName());
        step.setInputDigest(dto.getInputDigest());
        step.setOutputDigest(dto.getOutputDigest());
        if (dto.getDurationMs() != null) {
            step.setDurationMs(dto.getDurationMs());
        }
        if (dto.getStatus() != null) {
            step.setStatus(dto.getStatus());
        }
        step.setErrorSummary(dto.getErrorSummary());
        agentStepMapper.insert(step);
        log.info("Agent 步骤已追加: runId={}, stepNo={}", runId, dto.getStepNo());
    }

    /**
     * 记录写操作确认：同 (runId, action) 已存在时覆盖更新（幂等），否则插入。
     * 默认状态 PENDING 由实体字段初始值保证。
     */
    @Override
    public void recordConfirmation(String runId, AgentConfirmationDTO dto) {
        // 1. 按 (runId, action) 查询：同一执行内同一动作仅保留一条确认记录
        LambdaQueryWrapper<AgentConfirmation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConfirmation::getRunId, runId).eq(AgentConfirmation::getAction, dto.getAction());
        AgentConfirmation existing = agentConfirmationMapper.selectOne(wrapper);
        if (existing != null) {
            // 2. 已存在则覆盖更新（幂等）：支持 PENDING → CONFIRMED/REJECTED 的状态流转
            existing.setPayloadDigest(dto.getPayloadDigest());
            if (dto.getStatus() != null) {
                existing.setStatus(dto.getStatus());
            }
            existing.setConfirmedBy(dto.getConfirmedBy());
            existing.setConfirmedAt(dto.getConfirmedAt());
            existing.setTimeoutAt(dto.getTimeoutAt());
            agentConfirmationMapper.updateById(existing);
            log.info("Agent 确认记录已覆盖更新（幂等）: runId={}, action={}", runId, dto.getAction());
            return;
        }
        // 3. 首次记录：插入新确认记录（默认状态 PENDING 由实体初始值保证）
        AgentConfirmation confirmation = new AgentConfirmation();
        confirmation.setRunId(runId);
        confirmation.setAction(dto.getAction());
        confirmation.setPayloadDigest(dto.getPayloadDigest());
        if (dto.getStatus() != null) {
            confirmation.setStatus(dto.getStatus());
        }
        confirmation.setConfirmedBy(dto.getConfirmedBy());
        confirmation.setConfirmedAt(dto.getConfirmedAt());
        confirmation.setTimeoutAt(dto.getTimeoutAt());
        agentConfirmationMapper.insert(confirmation);
        log.info("Agent 确认记录已记录: runId={}, action={}", runId, dto.getAction());
    }

    /**
     * 创建转人工工单：生成唯一工单号 HF + yyyyMMddHHmmss + 4 位随机数字，
     * 默认状态 OPEN（实体字段初始值），插入后返回工单号与状态。
     */
    @Override
    public HandoffTicketVO createHandoffTicket(HandoffTicketDTO dto) {
        // 1. 生成唯一工单号：HF + yyyyMMddHHmmss + 4 位随机数字（服务端生成，保证全局唯一）
        String ticketNo = "HF" + TICKET_NO_FORMAT.format(LocalDateTime.now()) + RandomUtil.randomNumbers(4);
        // 2. 组装工单实体并落库（默认状态 OPEN、优先级 NORMAL 由实体初始值保证）
        HandoffTicket ticket = new HandoffTicket();
        ticket.setTicketNo(ticketNo);
        ticket.setRunId(dto.getRunId());
        ticket.setSessionId(dto.getSessionId());
        ticket.setUserId(dto.getUserId());
        ticket.setReason(dto.getReason());
        if (dto.getPriority() != null) {
            ticket.setPriority(dto.getPriority());
        }
        ticket.setOrderNo(dto.getOrderNo());
        ticket.setSentiment(dto.getSentiment());
        ticket.setProblemSummary(dto.getProblemSummary());
        ticket.setExecutedSteps(dto.getExecutedSteps());
        handoffTicketMapper.insert(ticket);
        log.info("转人工工单已创建: ticketNo={}, runId={}", ticketNo, dto.getRunId());
        // 3. 返回工单号与状态，供调用方展示并触发后续转人工通知
        return new HandoffTicketVO(ticket.getTicketNo(), ticket.getStatus());
    }

    /**
     * 查询执行详情：run 不存在抛 {@link ResultCode#AGENT_RUN_NOT_FOUND}；
     * 步骤按 stepNo 升序（SQL 排序 + 内存防御性二次排序）。
     */
    @Override
    public AgentRunDetailVO getRunDetail(String runId) {
        // 1. 前置校验：执行记录不存在则抛 AGENT_RUN_NOT_FOUND
        AgentRun run = agentRunMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException(ResultCode.AGENT_RUN_NOT_FOUND);
        }
        // 2. 查询该执行下全部步骤，SQL 侧按 stepNo 升序
        LambdaQueryWrapper<AgentStep> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentStep::getRunId, runId).orderByAsc(AgentStep::getStepNo);
        List<AgentStep> steps = agentStepMapper.selectList(wrapper);
        // 3. 内存防御性二次排序：即使 SQL 排序被忽略，也保证审计回放顺序稳定
        steps.sort(Comparator.comparing(AgentStep::getStepNo));

        // 4. 组装详情 VO：run 元数据 + 步骤轨迹（实体转 DTO）
        AgentRunDetailVO vo = new AgentRunDetailVO();
        vo.setRunId(run.getRunId());
        vo.setSessionId(run.getSessionId());
        vo.setUserId(run.getUserId());
        vo.setIntent(run.getIntent());
        vo.setSentiment(run.getSentiment());
        vo.setStatus(run.getStatus());
        vo.setCurrentStep(run.getCurrentStep());
        vo.setPromptVersion(run.getPromptVersion());
        vo.setErrorSummary(run.getErrorSummary());
        vo.setCreateTime(run.getCreateTime());
        vo.setUpdateTime(run.getUpdateTime());
        vo.setSteps(steps.stream().map(AgentTraceServiceImpl::toStepDTO).toList());
        return vo;
    }

    /**
     * 实体转 DTO（步骤）
     */
    private static AgentStepDTO toStepDTO(AgentStep step) {
        // 实体转 DTO：字段逐一拷贝，供详情响应使用
        AgentStepDTO dto = new AgentStepDTO();
        dto.setRunId(step.getRunId());
        dto.setStepNo(step.getStepNo());
        dto.setStepType(step.getStepType());
        dto.setToolName(step.getToolName());
        dto.setInputDigest(step.getInputDigest());
        dto.setOutputDigest(step.getOutputDigest());
        dto.setDurationMs(step.getDurationMs());
        dto.setStatus(step.getStatus());
        dto.setErrorSummary(step.getErrorSummary());
        return dto;
    }
}
