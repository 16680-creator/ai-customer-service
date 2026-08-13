package com.aics.chat.agent;

import com.aics.chat.agent.model.AgentIntentType;
import com.aics.chat.agent.model.AgentTurnResult;
import com.aics.chat.agent.workflow.AfterSaleAgentService;
import com.aics.chat.dto.AgentConfirmRequestDTO;
import com.aics.chat.dto.AgentRequestDTO;
import com.aics.chat.feign.AgentTraceFeignClient;
import com.aics.chat.service.ChatService;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Agent 控制器测试：端点委托、用户上下文透传、普通对话路由
 */
@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock
    private AfterSaleAgentService afterSaleAgentService;
    @Mock
    private ChatService chatService;
    @Mock
    private AgentTraceFeignClient agentTraceFeignClient;

    private AgentController controller;

    @BeforeEach
    void setUp() {
        controller = new AgentController(afterSaleAgentService, chatService, agentTraceFeignClient);
    }

    @AfterEach
    void tearDown() {
        ChatUserContext.clear();
    }

    @Test
    void agent端点委托编排器并透传用户身份() {
        AgentTurnResult expected = AgentTurnResult.of("run-1", "CONFIRM_ACTION",
                List.of(AgentIntentType.AFTER_SALE), "请确认", true, false,
                "token-1", null, List.of(), null, null, null);
        when(afterSaleAgentService.handleTurn(eq(1L), eq(10L), eq(null), eq("耳机坏了"))).thenReturn(expected);

        AgentRequestDTO request = new AgentRequestDTO();
        request.setSessionId(10L);
        request.setInput("耳机坏了");
        Result<AgentTurnResult> result = controller.agent(1L, request);

        assertTrue(result.isSuccess());
        assertEquals("run-1", result.getData().runId());
        assertEquals("CONFIRM_ACTION", result.getData().state());
        verify(afterSaleAgentService).handleTurn(1L, 10L, null, "耳机坏了");
        assertNull(ChatUserContext.getUserId()); // finally 清理
    }

    @Test
    void 普通意图路由回普通对话() {
        AgentTurnResult routed = AgentTurnResult.of("run-2", "NORMAL_CHAT",
                List.of(AgentIntentType.NORMAL_CHAT), null, false, true,
                null, null, List.of(), null, null, null);
        when(afterSaleAgentService.handleTurn(eq(1L), eq(10L), eq(null), eq("优惠券怎么用"))).thenReturn(routed);
        when(chatService.chat("10", "优惠券怎么用"))
                .thenReturn(Result.success("优惠券在结算页选择可用优惠券即可抵扣"));

        AgentRequestDTO request = new AgentRequestDTO();
        request.setSessionId(10L);
        request.setInput("优惠券怎么用");
        Result<AgentTurnResult> result = controller.agent(1L, request);

        assertTrue(result.isSuccess());
        assertEquals("NORMAL_CHAT", result.getData().state());
        assertEquals("优惠券在结算页选择可用优惠券即可抵扣", result.getData().reply());
        verify(chatService).chat("10", "优惠券怎么用");
    }

    @Test
    void 确认端点把决策转译为对话输入() {
        AgentTurnResult expected = AgentTurnResult.of("run-3", "COMPLETED",
                List.of(AgentIntentType.AFTER_SALE), "已完成", false, false,
                null, null, List.of(), null, "AS0001", null);
        when(afterSaleAgentService.handleTurn(eq(1L), eq(10L), eq("run-3"), eq("确认"))).thenReturn(expected);

        AgentConfirmRequestDTO request = new AgentConfirmRequestDTO();
        request.setRunId("run-3");
        request.setToken("token-3");
        request.setDecision("CONFIRM");
        request.setSessionId(10L);
        Result<AgentTurnResult> result = controller.confirm(1L, request);

        assertTrue(result.isSuccess());
        assertEquals("AS0001", result.getData().applicationNo());
        verify(afterSaleAgentService).handleTurn(1L, 10L, "run-3", "确认");
    }

    @Test
    void 拒绝决策转译为拒绝输入() {
        AgentTurnResult expected = AgentTurnResult.of("run-4", "CANCELLED",
                List.of(AgentIntentType.AFTER_SALE), "已取消", false, false,
                null, null, List.of(), null, null, null);
        when(afterSaleAgentService.handleTurn(eq(1L), eq(10L), eq("run-4"), eq("拒绝"))).thenReturn(expected);

        AgentConfirmRequestDTO request = new AgentConfirmRequestDTO();
        request.setRunId("run-4");
        request.setToken("token-4");
        request.setDecision("REJECT");
        request.setSessionId(10L);
        Result<AgentTurnResult> result = controller.confirm(1L, request);

        assertTrue(result.isSuccess());
        assertEquals("CANCELLED", result.getData().state());
        verify(afterSaleAgentService).handleTurn(1L, 10L, "run-4", "拒绝");
    }

    @Test
    void 轨迹查询透传消息服务() {
        Map<String, Object> detail = Map.of("runId", "run-1", "status", "COMPLETED");
        when(agentTraceFeignClient.getRunDetail("run-1")).thenReturn(Result.success(detail));
        Result<Map<String, Object>> result = controller.runDetail("run-1");
        assertTrue(result.isSuccess());
        assertEquals("run-1", result.getData().get("runId"));
    }
}
