package com.aics.chat.agent.state;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 状态机单元测试：合法/非法迁移、每状态工具集、终态判定
 */
class AgentStateMachineTest {

    private final AgentStateMachine machine = new AgentStateMachine();

    @Test
    void 主流程迁移全部合法() {
        assertTrue(machine.canTransit(AfterSaleState.START, AfterSaleState.CLASSIFY_INTENT));
        assertTrue(machine.canTransit(AfterSaleState.CLASSIFY_INTENT, AfterSaleState.LOCATE_ORDER));
        assertTrue(machine.canTransit(AfterSaleState.LOCATE_ORDER, AfterSaleState.CHECK_POLICY));
        assertTrue(machine.canTransit(AfterSaleState.CHECK_POLICY, AfterSaleState.COLLECT_EVIDENCE));
        assertTrue(machine.canTransit(AfterSaleState.COLLECT_EVIDENCE, AfterSaleState.CONFIRM_ACTION));
        assertTrue(machine.canTransit(AfterSaleState.CONFIRM_ACTION, AfterSaleState.EXECUTE_AFTER_SALE));
        assertTrue(machine.canTransit(AfterSaleState.EXECUTE_AFTER_SALE, AfterSaleState.COMPLETED));
    }

    @Test
    void 分支迁移合法() {
        assertTrue(machine.canTransit(AfterSaleState.CLASSIFY_INTENT, AfterSaleState.HANDOFF));
        assertTrue(machine.canTransit(AfterSaleState.CHECK_POLICY, AfterSaleState.HANDOFF));
        assertTrue(machine.canTransit(AfterSaleState.EXECUTE_AFTER_SALE, AfterSaleState.HANDOFF));
        assertTrue(machine.canTransit(AfterSaleState.CONFIRM_ACTION, AfterSaleState.CANCELLED));
        assertTrue(machine.canTransit(AfterSaleState.LOCATE_ORDER, AfterSaleState.LOCATE_ORDER));
        assertTrue(machine.canTransit(AfterSaleState.LOCATE_ORDER, AfterSaleState.COMPLETED));
    }

    @Test
    void 非法迁移被拒绝() {
        assertFalse(machine.canTransit(AfterSaleState.START, AfterSaleState.EXECUTE_AFTER_SALE));
        assertFalse(machine.canTransit(AfterSaleState.CLASSIFY_INTENT, AfterSaleState.COMPLETED));
        assertFalse(machine.canTransit(AfterSaleState.CONFIRM_ACTION, AfterSaleState.CHECK_POLICY));
        assertFalse(machine.canTransit(AfterSaleState.COMPLETED, AfterSaleState.LOCATE_ORDER));
        assertFalse(machine.canTransit(null, AfterSaleState.HANDOFF));
        assertFalse(machine.canTransit(AfterSaleState.START, null));
    }

    @Test
    void 终态判定() {
        assertTrue(machine.isTerminal(AfterSaleState.COMPLETED));
        assertTrue(machine.isTerminal(AfterSaleState.CANCELLED));
        assertTrue(machine.isTerminal(AfterSaleState.HANDOFF));
        assertTrue(machine.isTerminal(AfterSaleState.FAILED));
        assertFalse(machine.isTerminal(AfterSaleState.CONFIRM_ACTION));
        assertFalse(machine.isTerminal(AfterSaleState.LOCATE_ORDER));
        assertFalse(machine.isTerminal(null));
    }

    @Test
    void 每状态允许的工具集正确() {
        assertEquals(Set.of(AgentStateMachine.TOOL_ORDER_LOCATOR, AgentStateMachine.TOOL_PRODUCT_RECOMMEND),
                machine.allowedTools(AfterSaleState.LOCATE_ORDER));
        assertEquals(Set.of(AgentStateMachine.TOOL_POLICY_CHECK),
                machine.allowedTools(AfterSaleState.CHECK_POLICY));
        assertEquals(Set.of(AgentStateMachine.TOOL_CREATE_AFTER_SALE),
                machine.allowedTools(AfterSaleState.EXECUTE_AFTER_SALE));
        assertEquals(Set.of(AgentStateMachine.TOOL_HANDOFF),
                machine.allowedTools(AfterSaleState.HANDOFF));
        // 写操作状态与终态不允许工具调用
        assertTrue(machine.allowedTools(AfterSaleState.CONFIRM_ACTION).isEmpty());
        assertTrue(machine.allowedTools(AfterSaleState.COMPLETED).isEmpty());
        assertTrue(machine.allowedTools(AfterSaleState.CLASSIFY_INTENT).isEmpty());
    }

    @Test
    void 工具权限判定() {
        assertTrue(machine.isToolAllowed(AfterSaleState.EXECUTE_AFTER_SALE, AgentStateMachine.TOOL_CREATE_AFTER_SALE));
        assertFalse(machine.isToolAllowed(AfterSaleState.CONFIRM_ACTION, AgentStateMachine.TOOL_CREATE_AFTER_SALE));
        assertFalse(machine.isToolAllowed(AfterSaleState.LOCATE_ORDER, AgentStateMachine.TOOL_CREATE_AFTER_SALE));
    }
}
