package com.aics.chat.agent;

import com.aics.chat.agent.model.AgentIntentType;
import com.aics.chat.agent.model.AgentTurnResult;
import com.aics.chat.agent.workflow.AfterSaleAgentService;
import com.aics.chat.agent.workflow.AgentTurnListener;
import com.aics.chat.dto.AgentRequestDTO;
import com.aics.chat.feign.AgentTraceFeignClient;
import com.aics.chat.service.ChatService;
import com.aics.chat.util.ChatUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Agent 流式端点测试：监听器委托、普通对话 token 流式、异常不外抛
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentControllerStreamTest {

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
        ChatUserContext.setUserId(1L);
    }

    @AfterEach
    void tearDown() {
        ChatUserContext.clear();
    }

    private AgentRequestDTO request(String input) {
        AgentRequestDTO dto = new AgentRequestDTO();
        dto.setSessionId(10L);
        dto.setInput(input);
        return dto;
    }

    @Test
    void 流式编排委托并携带监听器_结束后清理用户上下文() {
        AgentTurnResult result = AgentTurnResult.of("run-1", "CONFIRM_ACTION",
                List.of(AgentIntentType.AFTER_SALE), "请确认", true, false,
                "token-1", null, List.of(), null, null, null);
        when(afterSaleAgentService.handleTurn(eq(1L), eq(10L), eq(null), eq("耳机坏了"), any()))
                .thenReturn(result);

        SseEmitter emitter = new SseEmitter(60_000L);
        // 直接调用同步执行方法（agentStream 内部以异步方式调用它）
        assertDoesNotThrow(() -> controller.runAgentTurn(emitter, 1L, request("耳机坏了")));

        ArgumentCaptor<AgentTurnListener> captor = ArgumentCaptor.forClass(AgentTurnListener.class);
        verify(afterSaleAgentService).handleTurn(eq(1L), eq(10L), eq(null), eq("耳机坏了"), captor.capture());
        assertNotNull(captor.getValue());
        // 工作线程上下文必须被清理，防止线程复用串号
        assertNull(ChatUserContext.getUserId());
    }

    @Test
    void 普通对话路由时逐token流式回调() {
        AgentTurnResult routed = AgentTurnResult.of("run-2", "NORMAL_CHAT",
                List.of(AgentIntentType.NORMAL_CHAT), null, false, true,
                null, null, List.of(), null, null, null);
        when(afterSaleAgentService.handleTurn(eq(1L), eq(10L), eq(null), eq("你好"), any()))
                .thenReturn(routed);
        AtomicReference<String> streamed = new AtomicReference<>("");
        when(chatService.streamReply(eq("10"), eq("你好"), any())).thenAnswer(inv -> {
            java.util.function.Consumer<String> onToken = inv.getArgument(2);
            onToken.accept("你");
            onToken.accept("好");
            streamed.set("你好");
            return "你好";
        });

        SseEmitter emitter = new SseEmitter(60_000L);
        assertDoesNotThrow(() -> controller.runAgentTurn(emitter, 1L, request("你好")));

        verify(chatService).streamReply(eq("10"), eq("你好"), any());
        assertEquals("你好", streamed.get());
    }

    @Test
    void 编排器抛异常时不向外传播() {
        when(afterSaleAgentService.handleTurn(any(), any(), any(), anyString(), any()))
                .thenThrow(new RuntimeException("boom"));

        SseEmitter emitter = new SseEmitter(60_000L);
        assertDoesNotThrow(() -> controller.runAgentTurn(emitter, 1L, request("任意输入")));
        assertNull(ChatUserContext.getUserId());
    }
}
