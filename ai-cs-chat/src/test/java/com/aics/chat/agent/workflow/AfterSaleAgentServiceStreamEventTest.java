package com.aics.chat.agent.workflow;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.confirm.ConfirmationService;
import com.aics.chat.agent.intent.IntentClassifierService;
import com.aics.chat.agent.model.AgentIntent;
import com.aics.chat.agent.model.AgentIntentType;
import com.aics.chat.agent.model.AgentTurnResult;
import com.aics.chat.agent.model.IntentResult;
import com.aics.chat.agent.model.SentimentType;
import com.aics.chat.agent.safety.SafetyGuardService;
import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.agent.store.AgentRunStore;
import com.aics.chat.agent.store.InMemoryAgentRunStore;
import com.aics.chat.agent.tool.AgentToolRegistry;
import com.aics.chat.agent.tool.CreateAfterSaleTool;
import com.aics.chat.agent.tool.HandoffTool;
import com.aics.chat.agent.tool.OrderLocatorTool;
import com.aics.chat.agent.tool.PolicyCheckTool;
import com.aics.chat.agent.tool.ProductRecommendTool;
import com.aics.chat.agent.tool.StaticRuleProvider;
import com.aics.chat.agent.trace.AgentTraceRecorder;
import com.aics.chat.dto.AfterSaleApplyVO;
import com.aics.chat.dto.OrderVO;
import com.aics.chat.feign.AfterSaleFeignClient;
import com.aics.chat.feign.NotifyFeignClient;
import com.aics.chat.feign.OrderFeignClient;
import com.aics.chat.feign.ProductRecommendFeignClient;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 售后 Agent 流式事件测试：编排过程中步骤事件按序发射、监听器为 null 时原路径不受影响
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AfterSaleAgentServiceStreamEventTest {

    @Mock
    private IntentClassifierService intentClassifierService;
    @Mock
    private AgentTraceRecorder traceRecorder;
    @Mock
    private OrderFeignClient orderFeignClient;
    @Mock
    private ProductRecommendFeignClient productRecommendFeignClient;
    @Mock
    private AfterSaleFeignClient afterSaleFeignClient;
    @Mock
    private com.aics.chat.feign.AgentTraceFeignClient agentTraceFeignClient;
    @Mock
    private NotifyFeignClient notifyFeignClient;
    @Mock
    private com.aics.chat.security.ContentSafetyService contentSafetyService;
    @Mock
    private com.aics.chat.security.ToolAuthorizationService toolAuthorizationService;
    @Mock
    private com.aics.chat.security.SecurityAuditRecorder securityAuditRecorder;

    private final AgentProperties properties = new AgentProperties();
    private final AgentRunStore runStore = new InMemoryAgentRunStore();
    private AfterSaleAgentService service;

    private OrderVO paidOrder;

    @BeforeEach
    void setUp() {
        ChatUserContext.setUserId(1L);
        paidOrder = new OrderVO();
        paidOrder.setOrderNo("ORD001");
        paidOrder.setStatus("PAID");
        paidOrder.setPayAmount(new BigDecimal("199.00"));
        paidOrder.setCreateTime(LocalDateTime.now().minusDays(1));
        OrderVO.OrderItemVO item = new OrderVO.OrderItemVO();
        item.setProductId(1001L);
        item.setProductName("无线蓝牙耳机");
        item.setProductPrice(new BigDecimal("199.00"));
        item.setQuantity(1);
        paidOrder.setItems(List.of(item));

        OrderLocatorTool orderLocatorTool = new OrderLocatorTool(orderFeignClient, securityAuditRecorder);
        PolicyCheckTool policyCheckTool = new PolicyCheckTool(new StaticRuleProvider());
        ProductRecommendTool productRecommendTool = new ProductRecommendTool(productRecommendFeignClient, properties);
        CreateAfterSaleTool createAfterSaleTool = new CreateAfterSaleTool(afterSaleFeignClient);
        HandoffTool handoffTool = new HandoffTool(agentTraceFeignClient, notifyFeignClient);
        AgentStateMachine stateMachine = new AgentStateMachine();
        AgentToolRegistry registry = new AgentToolRegistry(stateMachine, List.of(
                orderLocatorTool, policyCheckTool, productRecommendTool, createAfterSaleTool, handoffTool));

        when(contentSafetyService.reviewInput(anyString()))
                .thenReturn(com.aics.chat.security.ContentReviewResult.pass());
        when(toolAuthorizationService.authorize(any(), anyString(), any()))
                .thenReturn(com.aics.chat.security.ToolAuthResult.allowed("USER"));

        service = new AfterSaleAgentService(properties, new SafetyGuardService(),
                intentClassifierService, stateMachine, registry, runStore,
                new ConfirmationService(properties, new ObjectMapper()), traceRecorder,
                orderLocatorTool, policyCheckTool, productRecommendTool,
                createAfterSaleTool, handoffTool,
                io.micrometer.observation.ObservationRegistry.create(),
                contentSafetyService, toolAuthorizationService, securityAuditRecorder);

        when(orderFeignClient.listOrders(1L)).thenReturn(Result.success(List.of(paidOrder)));
        when(intentClassifierService.classify(anyString())).thenReturn(
                IntentResult.of(List.of(AgentIntent.of(AgentIntentType.AFTER_SALE, 0.95,
                        Map.of("action", "EXCHANGE", "reason", "耳机损坏"))), SentimentType.NEUTRAL, false));
        when(traceRecorder.executedStepsJson(any())).thenReturn("[\"安全检查\"]");
    }

    @AfterEach
    void tearDown() {
        ChatUserContext.clear();
    }

    /** 记录步骤事件的监听器 */
    private static List<String> recordingListener(AgentTurnListener[] holder) {
        List<String> phases = new ArrayList<>();
        holder[0] = new AgentTurnListener() {
            @Override
            public void onStep(String phase, String detail) {
                phases.add(phase);
            }
        };
        return phases;
    }

    @Test
    void 首轮编排按顺序发射全部步骤事件() {
        when(afterSaleFeignClient.apply(any(), any())).thenReturn(Result.success(applyVO("AS001")));
        AgentTurnListener[] holder = new AgentTurnListener[1];
        List<String> phases = recordingListener(holder);

        AgentTurnResult turn1 = service.handleTurn(1L, 10L, null, "我昨天买的耳机坏了，想换货", holder[0]);

        assertEquals("CONFIRM_ACTION", turn1.state());
        // 步骤事件与编排顺序一致：安全 → 内容审核 → 意图 → 订单定位 → 规则校验 → 参数收集 → 确认请求
        assertEquals(List.of("SAFETY", "CONTENT_REVIEW", "INTENT", "LOCATE_ORDER",
                "CHECK_POLICY", "COLLECT_EVIDENCE", "CONFIRM_REQUEST"), phases);
    }

    @Test
    void 续跑确认轮发射执行事件() {
        when(afterSaleFeignClient.apply(any(), any())).thenReturn(Result.success(applyVO("AS002")));
        AgentTurnListener[] holder = new AgentTurnListener[1];
        List<String> phases = recordingListener(holder);

        AgentTurnResult turn1 = service.handleTurn(1L, 10L, null, "我昨天买的耳机坏了，想换货", holder[0]);
        phases.clear();
        AgentTurnResult turn2 = service.handleTurn(1L, 10L, turn1.runId(), "确认", holder[0]);

        assertEquals("COMPLETED", turn2.state());
        assertEquals(List.of("EXECUTE"), phases);
    }

    @Test
    void 监听器为null时编排不受影响() {
        when(afterSaleFeignClient.apply(any(), any())).thenReturn(Result.success(applyVO("AS003")));

        // 5 参重载传 null：等价于原 4 参行为
        AgentTurnResult turn1 = service.handleTurn(1L, 10L, null, "我昨天买的耳机坏了，想换货", null);
        assertEquals("CONFIRM_ACTION", turn1.state());
        assertNotNull(turn1.confirmationToken());

        AgentTurnResult turn2 = service.handleTurn(1L, 10L, turn1.runId(), "确认");
        assertEquals("COMPLETED", turn2.state());
    }

    private AfterSaleApplyVO applyVO(String applicationNo) {
        AfterSaleApplyVO vo = new AfterSaleApplyVO();
        vo.setApplicationNo(applicationNo);
        vo.setStatus("PENDING");
        vo.setActionType("EXCHANGE");
        vo.setOrderNo("ORD001");
        return vo;
    }
}
