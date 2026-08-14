package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.AgentConfirmationDTO;
import com.aics.message.dto.AgentRunDTO;
import com.aics.message.dto.AgentStepDTO;
import com.aics.message.dto.HandoffTicketDTO;
import com.aics.message.service.AgentTraceService;
import com.aics.message.vo.AgentRunDetailVO;
import com.aics.message.vo.HandoffTicketVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Agent 执行轨迹控制器单元测试
 * <p>
 * TDD：验证控制器正确委托 Service 层并返回统一 {@link Result} 结构。
 * 纯 Mockito 直接调用（与模块既有约定一致），不加载 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AgentTraceControllerTest {

    @Mock
    private AgentTraceService agentTraceService;

    @InjectMocks
    private AgentTraceController agentTraceController;

    // ==================== POST /api/agent/runs ====================

    @Test
    @DisplayName("创建执行记录 - 委托 Service 并返回 runId")
    void createRun_delegatesAndReturnsResult() {
        AgentRunDTO dto = new AgentRunDTO();
        dto.setRunId("run-1");
        when(agentTraceService.createRun(dto)).thenReturn("run-1");

        Result<String> result = agentTraceController.createRun(dto);

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals("run-1", result.getData());
        verify(agentTraceService).createRun(dto);
    }

    // ==================== PUT /api/agent/runs/{runId}/status ====================

    @Test
    @DisplayName("更新执行状态 - 委托 Service 并返回空结果")
    void updateRunStatus_delegatesAndReturnsResult() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "COMPLETED");
        body.put("currentStep", 5);
        body.put("errorSummary", null);

        Result<Void> result = agentTraceController.updateRunStatus("run-1", body);

        assertEquals(200, result.getCode());
        verify(agentTraceService).updateRunStatus("run-1", "COMPLETED", 5, null);
    }

    @Test
    @DisplayName("更新执行状态 - currentStep/errorSummary 缺省时传 null")
    void updateRunStatus_missingFields_passNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "FAILED");

        Result<Void> result = agentTraceController.updateRunStatus("run-1", body);

        assertEquals(200, result.getCode());
        verify(agentTraceService).updateRunStatus("run-1", "FAILED", null, null);
    }

    // ==================== POST /api/agent/runs/{runId}/steps ====================

    @Test
    @DisplayName("追加步骤 - 委托 Service 并返回空结果")
    void appendStep_delegatesAndReturnsResult() {
        AgentStepDTO dto = new AgentStepDTO();
        dto.setRunId("run-1");
        dto.setStepNo(1);
        dto.setStepType("INTENT");

        Result<Void> result = agentTraceController.appendStep("run-1", dto);

        assertEquals(200, result.getCode());
        verify(agentTraceService).appendStep("run-1", dto);
    }

    // ==================== POST /api/agent/runs/{runId}/confirmations ====================

    @Test
    @DisplayName("记录确认 - 委托 Service 并返回空结果")
    void recordConfirmation_delegatesAndReturnsResult() {
        AgentConfirmationDTO dto = new AgentConfirmationDTO();
        dto.setRunId("run-1");
        dto.setAction("CREATE_EXCHANGE");
        dto.setPayloadDigest("digest");
        dto.setTimeoutAt(LocalDateTime.now().plusMinutes(5));

        Result<Void> result = agentTraceController.recordConfirmation("run-1", dto);

        assertEquals(200, result.getCode());
        verify(agentTraceService).recordConfirmation("run-1", dto);
    }

    // ==================== POST /api/agent/handoff-tickets ====================

    @Test
    @DisplayName("创建转人工工单 - 委托 Service 并返回工单 VO")
    void createHandoffTicket_delegatesAndReturnsResult() {
        HandoffTicketDTO dto = new HandoffTicketDTO();
        dto.setUserId(1000L);
        dto.setReason("USER_REQUEST");
        HandoffTicketVO vo = new HandoffTicketVO("HF202601011200001234", "OPEN");
        when(agentTraceService.createHandoffTicket(dto)).thenReturn(vo);

        Result<HandoffTicketVO> result = agentTraceController.createHandoffTicket(dto);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("HF202601011200001234", result.getData().getTicketNo());
        assertEquals("OPEN", result.getData().getStatus());
        verify(agentTraceService).createHandoffTicket(dto);
    }

    // ==================== GET /api/agent/runs/{runId} ====================

    @Test
    @DisplayName("查询执行详情 - 委托 Service 并返回详情 VO")
    void getRunDetail_delegatesAndReturnsResult() {
        AgentRunDetailVO vo = new AgentRunDetailVO();
        vo.setRunId("run-1");
        vo.setStatus("RUNNING");
        when(agentTraceService.getRunDetail("run-1")).thenReturn(vo);

        Result<AgentRunDetailVO> result = agentTraceController.getRunDetail("run-1");

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("run-1", result.getData().getRunId());
        assertEquals("RUNNING", result.getData().getStatus());
        verify(agentTraceService).getRunDetail("run-1");
    }
}
