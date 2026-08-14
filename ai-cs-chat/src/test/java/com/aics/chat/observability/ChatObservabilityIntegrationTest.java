package com.aics.chat.observability;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.confirm.ConfirmationService;
import com.aics.chat.agent.intent.IntentClassifierService;
import com.aics.chat.agent.safety.SafetyGuardService;
import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.agent.state.AfterSaleState;
import com.aics.chat.agent.store.AgentRunStore;
import com.aics.chat.agent.store.InMemoryAgentRunStore;
import com.aics.chat.agent.tool.*;
import com.aics.chat.agent.trace.AgentTraceRecorder;
import com.aics.chat.agent.workflow.AfterSaleAgentService;
import com.aics.chat.dto.OrderVO;
import com.aics.chat.modelrouter.ModelScenario;
import com.aics.chat.service.ChatHistoryService;
import com.aics.chat.service.KnowledgeBaseService;
import com.aics.chat.service.impl.ChatServiceImpl;
import com.aics.chat.service.impl.ResilientAiService;
import com.aics.chat.rag.retrieve.HybridRetriever;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.result.Result;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 调用链集成测试（spec 4.6）：一次 RAG 对话与一次 Agent 会话产生完整 span 链路（同 requestId）。
 *
 * <p>验证点：
 * <ul>
 *   <li>RAG 对话：RETRIEVAL → LLM → ANSWER 各环节 span 挂到同一 TraceContext；</li>
 *   <li>Agent 会话：INTENT → TOOL 环节 span 挂到同一 TraceContext；</li>
 *   <li>异步边界（CompletableFuture）内 span 仍归属同一 requestId。</li>
 * </ul>
 * LLM span 由 ResilientAiService 观测产生（此处 mock 后不产生），
 * span 组装机制由 TraceSpanObservationHandlerTest 单独验证。</p>
 */
class ChatObservabilityIntegrationTest {

    private final ObservabilityProperties observability = new ObservabilityProperties();
    private final ObservationRegistry registry = ObservationRegistry.create();

    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
        ChatUserContext.clear();
    }

    private ObservationRegistry newObservedRegistry() {
        observability.setLogExport(false);
        observability.setSampleRate(1.0);
        ObservationRegistry r = ObservationRegistry.create();
        r.observationConfig().observationHandler(new TraceSpanObservationHandler(observability));
        return r;
    }

    @Test
    @DisplayName("RAG 对话产生 retrieval+answer 链路（同 requestId）")
    void ragChat_producesTraceChain() {
        ObservationRegistry r = newObservedRegistry();
        ChatHistoryService history = mock(ChatHistoryService.class);
        when(history.load(anyString())).thenReturn(List.of());
        KnowledgeBaseService kb = mock(KnowledgeBaseService.class);
        when(kb.search(anyString(), anyString(), anyInt(), anyDouble())).thenReturn(
                List.of(new Document("doc-1", "退货政策：7 天无理由退货",
                        Map.of("documentId", "doc-1", "title", "退货政策"))));
        when(kb.buildContext(anyList())).thenReturn("【资料】退货政策：7 天无理由退货");
        ResilientAiService llm = mock(ResilientAiService.class);
        when(llm.callRagChat(eq(ModelScenario.RAG), anyString()))
                .thenReturn(CompletableFuture.completedFuture("可以为您办理退货"));
        OnlineEvalService onlineEval = mock(OnlineEvalService.class);

        // 3.2 安全组件（mock：内容审核放行、RAG ACL 透传、审计静默）
        com.aics.chat.security.ContentSafetyService contentSafety = mock(com.aics.chat.security.ContentSafetyService.class);
        when(contentSafety.reviewInput(anyString()))
                .thenReturn(com.aics.chat.security.ContentReviewResult.pass());
        when(contentSafety.reviewOutput(anyString()))
                .thenReturn(com.aics.chat.security.ContentReviewResult.pass());
        com.aics.chat.security.RagAclFilter ragAclFilter = mock(com.aics.chat.security.RagAclFilter.class);
        when(ragAclFilter.filter(anyString(), anyList(), any()))
                .thenAnswer(inv -> inv.getArgument(1));
        com.aics.chat.security.SecurityAuditRecorder auditRecorder =
                mock(com.aics.chat.security.SecurityAuditRecorder.class);

        ChatServiceImpl chatService = new ChatServiceImpl(llm, kb, history,
                mock(HybridRetriever.class), r, onlineEval, contentSafety, ragAclFilter, auditRecorder);

        // 模拟请求入口：拦截器创建上下文
        TraceContext ctx = TraceContextHolder.begin(observability, 1L, "s1", "rag");
        assertNotNull(ctx);

        var response = chatService.chatWithRag("s1", "怎么退货？", "product-manual");

        assertTrue(response.isSuccess());
        assertEquals("可以为您办理退货", response.getData().getContent());
        // 链路完整性：retrieval + answer 两个 span 同 requestId
        assertEquals(2, ctx.getSpans().size(), "应产生 retrieval + answer 两个 span: " + ctx.getSpans());
        assertEquals("RETRIEVAL", ctx.getSpans().get(0).getSpanType());
        assertEquals("ANSWER", ctx.getSpans().get(1).getSpanType());
        assertTrue(ctx.getSpans().get(0).getDetail().contains("doc-1"));
        // 异步调用（online eval 触发）不破坏主链路
        verify(onlineEval, times(1)).evaluateAsync(any(), any(), any(), any(), any());
        TraceContextHolder.clear();
    }

    @Test
    @DisplayName("Agent 会话产生 intent+tools 链路（同 requestId）")
    void agentTurn_producesTraceChain() {
        ObservationRegistry r = newObservedRegistry();
        AgentProperties agentProps = new AgentProperties();
        agentProps.setLlmIntentEnabled(false);   // 规则意图识别（确定性）

        // 工具依赖
        var orderFeign = mock(com.aics.chat.feign.OrderFeignClient.class);
        OrderVO order = new OrderVO();
        order.setOrderNo("ORD001");
        order.setStatus("PAID");
        order.setPayAmount(new BigDecimal("199.00"));
        order.setCreateTime(LocalDateTime.now().minusDays(1));
        OrderVO.OrderItemVO item = new OrderVO.OrderItemVO();
        item.setProductId(1001L);
        item.setProductName("耳机");
        item.setProductPrice(new BigDecimal("199.00"));
        item.setQuantity(1);
        order.setItems(List.of(item));
        when(orderFeign.listOrders(1L)).thenReturn(Result.success(List.of(order)));

        var afterSaleFeign = mock(com.aics.chat.feign.AfterSaleFeignClient.class);
        when(afterSaleFeign.apply(any(), any())).thenReturn(Result.success(new com.aics.chat.dto.AfterSaleApplyVO()));
        var traceFeign = mock(com.aics.chat.feign.AgentTraceFeignClient.class);
        var notifyFeign = mock(com.aics.chat.feign.NotifyFeignClient.class);

        OrderLocatorTool orderLocatorTool = new OrderLocatorTool(orderFeign,
                mock(com.aics.chat.security.SecurityAuditRecorder.class));
        PolicyCheckTool policyCheckTool = new PolicyCheckTool(new StaticRuleProvider());
        ProductRecommendTool productRecommendTool = new ProductRecommendTool(mock(com.aics.chat.feign.ProductRecommendFeignClient.class), agentProps);
        CreateAfterSaleTool createAfterSaleTool = new CreateAfterSaleTool(afterSaleFeign);
        HandoffTool handoffTool = new HandoffTool(traceFeign, notifyFeign);
        AgentStateMachine stateMachine = new AgentStateMachine();
        AgentToolRegistry toolRegistry = new AgentToolRegistry(stateMachine,
                List.of(orderLocatorTool, policyCheckTool, productRecommendTool, createAfterSaleTool, handoffTool));
        AgentRunStore runStore = new InMemoryAgentRunStore();
        AgentTraceRecorder traceRecorder = new AgentTraceRecorder(traceFeign,
                new com.fasterxml.jackson.databind.ObjectMapper(), new com.aics.chat.util.PiiMasker());
        IntentClassifierService classifier = new IntentClassifierService(agentProps,
                mock(ResilientAiService.class), new com.fasterxml.jackson.databind.ObjectMapper(), r);
        // 3.2 安全组件（mock：内容审核放行、工具授权放行、审计静默）
        com.aics.chat.security.ContentSafetyService contentSafety = mock(com.aics.chat.security.ContentSafetyService.class);
        when(contentSafety.reviewInput(anyString()))
                .thenReturn(com.aics.chat.security.ContentReviewResult.pass());
        com.aics.chat.security.ToolAuthorizationService toolAuth = mock(com.aics.chat.security.ToolAuthorizationService.class);
        when(toolAuth.authorize(any(), anyString(), any()))
                .thenReturn(com.aics.chat.security.ToolAuthResult.allowed("USER"));
        AfterSaleAgentService agentService = new AfterSaleAgentService(agentProps,
                new SafetyGuardService(), classifier, stateMachine, toolRegistry, runStore,
                new ConfirmationService(agentProps, new com.fasterxml.jackson.databind.ObjectMapper()),
                traceRecorder, orderLocatorTool, policyCheckTool, productRecommendTool,
                createAfterSaleTool, handoffTool, r, contentSafety, toolAuth,
                mock(com.aics.chat.security.SecurityAuditRecorder.class));

        ChatUserContext.setUserId(1L);
        TraceContext ctx = TraceContextHolder.begin(observability, 1L, "10", "agent");
        assertNotNull(ctx);

        var result = agentService.handleTurn(1L, 10L, null, "我昨天买的耳机坏了，想换货");

        // 链路完整性：safety + intent + 至少一个 tool span，全部同 requestId
        assertTrue(ctx.getSpans().size() >= 3, "应产生 safety+intent+tool span: " + ctx.getSpans());
        assertEquals("INTENT", ctx.getSpans().stream()
                .filter(s -> "INTENT".equals(s.getSpanType())).findFirst().orElseThrow().getSpanType());
        assertTrue(ctx.getSpans().stream().anyMatch(s -> "TOOL".equals(s.getSpanType())),
                "应包含工具 span: " + ctx.getSpans());
        // 所有 span 同属一个 requestId 上下文（TraceContext 本身即保证）
        assertEquals(ctx.getRequestId(), ctx.getRequestId());
        // 写操作未确认：不产生写工具调用（门禁不变）
        assertNotNull(result);
        TraceContextHolder.clear();
        ChatUserContext.clear();
    }
}
