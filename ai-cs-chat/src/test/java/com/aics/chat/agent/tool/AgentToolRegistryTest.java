package com.aics.chat.agent.tool;

import com.aics.chat.agent.state.AfterSaleState;
import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工具注册中心测试：注册/查找、写操作确认声明、状态权限门禁
 */
class AgentToolRegistryTest {

    private final AgentStateMachine stateMachine = new AgentStateMachine();

    /** 只读工具 */
    private final AgentTool readTool = new AgentTool() {
        @Override
        public String name() {
            return AgentStateMachine.TOOL_ORDER_LOCATOR;
        }

        @Override
        public RiskLevel riskLevel() {
            return RiskLevel.READ;
        }

        @Override
        public boolean requiresConfirmation() {
            return false;
        }
    };

    /** 写工具（需确认） */
    private final AgentTool writeTool = new AgentTool() {
        @Override
        public String name() {
            return AgentStateMachine.TOOL_CREATE_AFTER_SALE;
        }

        @Override
        public RiskLevel riskLevel() {
            return RiskLevel.WRITE;
        }

        @Override
        public boolean requiresConfirmation() {
            return true;
        }
    };

    private AgentToolRegistry newRegistry() {
        return new AgentToolRegistry(stateMachine, List.of(readTool, writeTool));
    }

    @Test
    void 注册与查找() {
        AgentToolRegistry registry = newRegistry();
        assertTrue(registry.contains(AgentStateMachine.TOOL_ORDER_LOCATOR));
        assertNotNull(registry.get(AgentStateMachine.TOOL_CREATE_AFTER_SALE));
        assertThrows(BusinessException.class, () -> registry.get("not_exist"));
        assertFalse(registry.contains("not_exist"));
    }

    @Test
    void 写操作工具声明需确认() {
        AgentToolRegistry registry = newRegistry();
        assertTrue(registry.requiresConfirmation(AgentStateMachine.TOOL_CREATE_AFTER_SALE));
        assertFalse(registry.requiresConfirmation(AgentStateMachine.TOOL_ORDER_LOCATOR));
        assertFalse(registry.requiresConfirmation("not_exist"));
    }

    @Test
    void 状态权限门禁() {
        AgentToolRegistry registry = newRegistry();
        assertDoesNotThrow(() -> registry.assertToolAllowed(AfterSaleState.EXECUTE_AFTER_SALE,
                AgentStateMachine.TOOL_CREATE_AFTER_SALE));
        // CONFIRM_ACTION 状态下不允许调用写工具
        assertThrows(BusinessException.class, () -> registry.assertToolAllowed(AfterSaleState.CONFIRM_ACTION,
                AgentStateMachine.TOOL_CREATE_AFTER_SALE));
        // 订单定位状态不允许调用写工具
        assertThrows(BusinessException.class, () -> registry.assertToolAllowed(AfterSaleState.LOCATE_ORDER,
                AgentStateMachine.TOOL_CREATE_AFTER_SALE));
    }
}
