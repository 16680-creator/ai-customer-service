package com.aics.chat.agent.confirm;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.context.AfterSaleContext;
import com.aics.chat.agent.model.AfterSaleActionType;
import com.aics.chat.agent.model.AgentActionPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 写操作确认服务测试：Token 生成/校验、超时过期、摘要一致性
 */
class ConfirmationServiceTest {

    private final AgentProperties properties = new AgentProperties();
    private final ConfirmationService service = new ConfirmationService(properties, new ObjectMapper());

    private AgentActionPlan plan() {
        return new AgentActionPlan(AfterSaleActionType.EXCHANGE, "ORD001", 1001L,
                "无线蓝牙耳机", 1, "耳机损坏", "满足规则 ASR-001", new BigDecimal("199"));
    }

    @Test
    void 签发凭证并校验通过() {
        AfterSaleContext ctx = new AfterSaleContext();
        AgentActionPlan plan = plan();
        String token = service.issue(ctx, plan);
        assertNotNull(token);
        assertNotNull(ctx.getPayloadDigest());
        assertNotNull(ctx.getConfirmationExpiresAt());
        assertTrue(service.validate(ctx, plan));
    }

    @Test
    void 操作内容变化后校验失败() {
        AfterSaleContext ctx = new AfterSaleContext();
        AgentActionPlan plan = plan();
        service.issue(ctx, plan);
        AgentActionPlan tampered = new AgentActionPlan(AfterSaleActionType.RETURN, "ORD001", 1001L,
                "无线蓝牙耳机", 1, "不想要了", "满足规则 ASR-002", new BigDecimal("199"));
        assertFalse(service.validate(ctx, tampered));
    }

    @Test
    void 超时后校验失败() {
        AfterSaleContext ctx = new AfterSaleContext();
        AgentActionPlan plan = plan();
        service.issue(ctx, plan);
        ctx.setConfirmationExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertTrue(service.isExpired(ctx));
        assertFalse(service.validate(ctx, plan));
    }

    @Test
    void 未签发时校验失败() {
        AfterSaleContext ctx = new AfterSaleContext();
        assertFalse(service.validate(ctx, plan()));
    }

    @Test
    void 摘要对相同计划稳定且不同计划不同() {
        AfterSaleContext ctx1 = new AfterSaleContext();
        service.issue(ctx1, plan());
        String d1 = ctx1.getPayloadDigest();
        AfterSaleContext ctx2 = new AfterSaleContext();
        service.issue(ctx2, plan());
        assertEquals(d1, ctx2.getPayloadDigest());

        AfterSaleContext ctx3 = new AfterSaleContext();
        service.issue(ctx3, new AgentActionPlan(AfterSaleActionType.REFUND, "ORD002", 1002L,
                "手机壳", 2, "质量问题", null, null));
        assertNotEquals(d1, ctx3.getPayloadDigest());
    }
}
