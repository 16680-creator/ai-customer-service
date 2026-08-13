package com.aics.message.service;

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
import com.aics.message.service.impl.AgentTraceServiceImpl;
import com.aics.message.vo.AgentRunDetailVO;
import com.aics.message.vo.HandoffTicketVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Agent 执行轨迹服务单元测试
 * <p>
 * TDD：先写测试（Red），再实现 {@link AgentTraceServiceImpl} 至通过（Green）。
 * 纯 Mockito 单测（与模块既有约定一致，Mapper 全部 mock），不加载 Spring 上下文，
 * 避免引入 RocketMQ / Redis / Nacos 等外部依赖。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AgentTraceServiceTest {

    @Mock
    private AgentRunMapper agentRunMapper;

    @Mock
    private AgentStepMapper agentStepMapper;

    @Mock
    private AgentConfirmationMapper agentConfirmationMapper;

    @Mock
    private HandoffTicketMapper handoffTicketMapper;

    @InjectMocks
    private AgentTraceServiceImpl agentTraceService;

    // ==================== createRun ====================

    @Test
    @DisplayName("创建执行记录 - 成功返回 runId，默认状态 RUNNING/步骤 0")
    void createRun_success() {
        AgentRunDTO dto = buildRunDTO("run-1");
        when(agentRunMapper.selectById("run-1")).thenReturn(null);
        when(agentRunMapper.insert(any(AgentRun.class))).thenReturn(1);

        String runId = agentTraceService.createRun(dto);

        assertEquals("run-1", runId);
        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper).insert(captor.capture());
        AgentRun inserted = captor.getValue();
        assertEquals("run-1", inserted.getRunId());
        assertEquals(100L, inserted.getSessionId());
        assertEquals(1000L, inserted.getUserId());
        assertEquals("AFTER_SALE", inserted.getIntent());
        assertEquals("NEGATIVE", inserted.getSentiment());
        assertEquals("RUNNING", inserted.getStatus());
        assertEquals(0, inserted.getCurrentStep());
    }

    @Test
    @DisplayName("创建执行记录 - runId 已存在时幂等返回已有 runId，不重复插入")
    void createRun_idempotent() {
        AgentRun existing = new AgentRun();
        existing.setRunId("run-1");
        when(agentRunMapper.selectById("run-1")).thenReturn(existing);

        String runId = agentTraceService.createRun(buildRunDTO("run-1"));

        assertEquals("run-1", runId);
        verify(agentRunMapper, never()).insert(any());
    }

    // ==================== updateRunStatus ====================

    @Test
    @DisplayName("更新执行状态 - 正常按 runId 更新状态/当前步骤/错误摘要")
    void updateRunStatus_success() {
        AgentRun existing = new AgentRun();
        existing.setRunId("run-1");
        when(agentRunMapper.selectById("run-1")).thenReturn(existing);

        agentTraceService.updateRunStatus("run-1", "COMPLETED", 5, null);

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper).updateById(captor.capture());
        AgentRun update = captor.getValue();
        assertEquals("run-1", update.getRunId());
        assertEquals("COMPLETED", update.getStatus());
        assertEquals(5, update.getCurrentStep());
        assertNull(update.getErrorSummary());
    }

    @Test
    @DisplayName("更新执行状态 - 执行记录不存在抛出 AGENT_RUN_NOT_FOUND")
    void updateRunStatus_runNotFound_shouldThrow() {
        when(agentRunMapper.selectById("missing")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> agentTraceService.updateRunStatus("missing", "FAILED", 3, "boom"));

        assertEquals(ResultCode.AGENT_RUN_NOT_FOUND.getCode(), ex.getCode());
        verify(agentRunMapper, never()).updateById(any());
    }

    // ==================== appendStep ====================

    @Test
    @DisplayName("追加步骤 - 新步骤正常插入")
    void appendStep_newStep_shouldInsert() {
        when(agentRunMapper.selectById("run-1")).thenReturn(new AgentRun());
        when(agentStepMapper.selectOne(any())).thenReturn(null);

        agentTraceService.appendStep("run-1", buildStepDTO("run-1", 1));

        ArgumentCaptor<AgentStep> captor = ArgumentCaptor.forClass(AgentStep.class);
        verify(agentStepMapper).insert(captor.capture());
        AgentStep step = captor.getValue();
        assertEquals("run-1", step.getRunId());
        assertEquals(1, step.getStepNo());
        assertEquals("INTENT", step.getStepType());
        assertEquals("SUCCESS", step.getStatus());
        assertEquals(0L, step.getDurationMs());
        verify(agentStepMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("追加步骤 - 同 runId+stepNo 已存在时覆盖更新（幂等）")
    void appendStep_sameStepNo_shouldOverwrite() {
        when(agentRunMapper.selectById("run-1")).thenReturn(new AgentRun());
        AgentStep existing = new AgentStep();
        existing.setId(9L);
        existing.setRunId("run-1");
        existing.setStepNo(1);
        existing.setStepType("INTENT");
        existing.setStatus("SUCCESS");
        when(agentStepMapper.selectOne(any())).thenReturn(existing);

        AgentStepDTO dto = buildStepDTO("run-1", 1);
        dto.setStepType("EXECUTE");
        dto.setStatus("FAILED");
        agentTraceService.appendStep("run-1", dto);

        ArgumentCaptor<AgentStep> captor = ArgumentCaptor.forClass(AgentStep.class);
        verify(agentStepMapper).updateById(captor.capture());
        assertEquals(9L, captor.getValue().getId());
        assertEquals("EXECUTE", captor.getValue().getStepType());
        assertEquals("FAILED", captor.getValue().getStatus());
        verify(agentStepMapper, never()).insert(any());
    }

    @Test
    @DisplayName("追加步骤 - 执行记录不存在抛出 AGENT_RUN_NOT_FOUND")
    void appendStep_runNotFound_shouldThrow() {
        when(agentRunMapper.selectById("missing")).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> agentTraceService.appendStep("missing", buildStepDTO("missing", 1)));
        verify(agentStepMapper, never()).insert(any());
    }

    // ==================== recordConfirmation ====================

    @Test
    @DisplayName("记录确认 - 新确认正常插入，默认状态 PENDING")
    void recordConfirmation_new_shouldInsert() {
        when(agentConfirmationMapper.selectOne(any())).thenReturn(null);

        agentTraceService.recordConfirmation("run-1", buildConfirmationDTO("run-1", "CREATE_EXCHANGE"));

        ArgumentCaptor<AgentConfirmation> captor = ArgumentCaptor.forClass(AgentConfirmation.class);
        verify(agentConfirmationMapper).insert(captor.capture());
        AgentConfirmation confirmation = captor.getValue();
        assertEquals("run-1", confirmation.getRunId());
        assertEquals("CREATE_EXCHANGE", confirmation.getAction());
        assertEquals("sha256-digest", confirmation.getPayloadDigest());
        assertEquals("PENDING", confirmation.getStatus());
        assertNotNull(confirmation.getTimeoutAt());
    }

    @Test
    @DisplayName("记录确认 - 同 runId+action 已存在时更新而非重复插入（幂等）")
    void recordConfirmation_idempotent_shouldUpdate() {
        AgentConfirmation existing = new AgentConfirmation();
        existing.setId(5L);
        existing.setRunId("run-1");
        existing.setAction("CREATE_EXCHANGE");
        existing.setStatus("PENDING");
        when(agentConfirmationMapper.selectOne(any())).thenReturn(existing);

        AgentConfirmationDTO dto = buildConfirmationDTO("run-1", "CREATE_EXCHANGE");
        dto.setStatus("CONFIRMED");
        dto.setConfirmedBy(1000L);
        dto.setConfirmedAt(LocalDateTime.now());
        agentTraceService.recordConfirmation("run-1", dto);

        ArgumentCaptor<AgentConfirmation> captor = ArgumentCaptor.forClass(AgentConfirmation.class);
        verify(agentConfirmationMapper).updateById(captor.capture());
        assertEquals(5L, captor.getValue().getId());
        assertEquals("CONFIRMED", captor.getValue().getStatus());
        assertEquals(1000L, captor.getValue().getConfirmedBy());
        verify(agentConfirmationMapper, never()).insert(any());
    }

    // ==================== createHandoffTicket ====================

    @Test
    @DisplayName("创建转人工工单 - 生成 HF 前缀单号并返回状态")
    void createHandoffTicket_shouldGenerateHfTicketNo() {
        HandoffTicketDTO dto = new HandoffTicketDTO();
        dto.setRunId("run-1");
        dto.setUserId(1000L);
        dto.setReason("NEGATIVE_SENTIMENT");
        dto.setPriority("HIGH");
        dto.setOrderNo("ORD001");
        dto.setProblemSummary("用户情绪激动，换货资格不满足");

        HandoffTicketVO vo = agentTraceService.createHandoffTicket(dto);

        assertNotNull(vo.getTicketNo());
        assertTrue(vo.getTicketNo().startsWith("HF"), "工单号应以 HF 开头");
        assertEquals(20, vo.getTicketNo().length(), "HF + 14位时间戳 + 4位随机数");
        assertTrue(vo.getTicketNo().matches("^HF\\d{18}$"), "工单号格式应为 HF + 18 位数字");
        assertEquals("OPEN", vo.getStatus());

        ArgumentCaptor<HandoffTicket> captor = ArgumentCaptor.forClass(HandoffTicket.class);
        verify(handoffTicketMapper).insert(captor.capture());
        HandoffTicket inserted = captor.getValue();
        assertEquals(vo.getTicketNo(), inserted.getTicketNo());
        assertEquals("HIGH", inserted.getPriority());
        assertEquals("NEGATIVE_SENTIMENT", inserted.getReason());
        assertEquals("ORD001", inserted.getOrderNo());
        assertEquals("OPEN", inserted.getStatus());
    }

    // ==================== getRunDetail ====================

    @Test
    @DisplayName("查询执行详情 - 返回 run 信息与按 stepNo 升序的 steps")
    void getRunDetail_success() {
        AgentRun run = new AgentRun();
        run.setRunId("run-1");
        run.setSessionId(100L);
        run.setUserId(1000L);
        run.setStatus("RUNNING");
        run.setCurrentStep(2);
        when(agentRunMapper.selectById("run-1")).thenReturn(run);

        // 故意乱序返回，验证服务层按 stepNo 升序整理
        AgentStep step1 = buildStep("run-1", 1);
        AgentStep step2 = buildStep("run-1", 2);
        when(agentStepMapper.selectList(any())).thenReturn(Arrays.asList(step2, step1));

        AgentRunDetailVO vo = agentTraceService.getRunDetail("run-1");

        assertEquals("run-1", vo.getRunId());
        assertEquals(100L, vo.getSessionId());
        assertEquals(1000L, vo.getUserId());
        assertEquals("RUNNING", vo.getStatus());
        assertEquals(2, vo.getCurrentStep());
        assertEquals(2, vo.getSteps().size());
        assertEquals(1, vo.getSteps().get(0).getStepNo());
        assertEquals(2, vo.getSteps().get(1).getStepNo());
        assertEquals("INTENT", vo.getSteps().get(0).getStepType());
    }

    @Test
    @DisplayName("查询执行详情 - 执行记录不存在抛出 AGENT_RUN_NOT_FOUND")
    void getRunDetail_runNotFound_shouldThrow() {
        when(agentRunMapper.selectById("missing")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> agentTraceService.getRunDetail("missing"));

        assertEquals(ResultCode.AGENT_RUN_NOT_FOUND.getCode(), ex.getCode());
        verify(agentStepMapper, never()).selectList(any());
    }

    // ==================== 测试数据构造 ====================

    private AgentRunDTO buildRunDTO(String runId) {
        AgentRunDTO dto = new AgentRunDTO();
        dto.setRunId(runId);
        dto.setSessionId(100L);
        dto.setUserId(1000L);
        dto.setIntent("AFTER_SALE");
        dto.setSentiment("NEGATIVE");
        return dto;
    }

    private AgentStepDTO buildStepDTO(String runId, int stepNo) {
        AgentStepDTO dto = new AgentStepDTO();
        dto.setRunId(runId);
        dto.setStepNo(stepNo);
        dto.setStepType("INTENT");
        dto.setInputDigest("input-digest");
        dto.setOutputDigest("output-digest");
        return dto;
    }

    private AgentStep buildStep(String runId, int stepNo) {
        AgentStep step = new AgentStep();
        step.setId((long) stepNo);
        step.setRunId(runId);
        step.setStepNo(stepNo);
        step.setStepType("INTENT");
        step.setStatus("SUCCESS");
        return step;
    }

    private AgentConfirmationDTO buildConfirmationDTO(String runId, String action) {
        AgentConfirmationDTO dto = new AgentConfirmationDTO();
        dto.setRunId(runId);
        dto.setAction(action);
        dto.setPayloadDigest("sha256-digest");
        dto.setTimeoutAt(LocalDateTime.now().plusMinutes(10));
        return dto;
    }
}
