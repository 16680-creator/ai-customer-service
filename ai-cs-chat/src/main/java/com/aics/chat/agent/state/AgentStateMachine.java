package com.aics.chat.agent.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Agent 显式状态机：迁移表 + 每状态允许的工具集 + 终态判定。
 *
 * <p>纯 Java 实现（无 Spring 依赖），便于单元测试。迁移规则集中管理，
 * 非法的状态迁移一律拒绝，保证写操作强约束。</p>
 */
public class AgentStateMachine {

    /** 状态迁移表：from → 允许的 to 集合 */
    private static final Map<AfterSaleState, Set<AfterSaleState>> TRANSITIONS = buildTransitions();

    /** 每状态允许的工具集 */
    private static final Map<AfterSaleState, Set<String>> ALLOWED_TOOLS = buildAllowedTools();

    /** 工具名常量（与 AgentToolRegistry 注册名一致） */
    public static final String TOOL_ORDER_LOCATOR = "order_locator";
    public static final String TOOL_POLICY_CHECK = "policy_check";
    public static final String TOOL_PRODUCT_RECOMMEND = "product_recommend";
    public static final String TOOL_CREATE_AFTER_SALE = "create_after_sale";
    public static final String TOOL_HANDOFF = "handoff";

    private static Map<AfterSaleState, Set<AfterSaleState>> buildTransitions() {
        Map<AfterSaleState, Set<AfterSaleState>> map = new EnumMap<>(AfterSaleState.class);
        map.put(AfterSaleState.START, Set.of(AfterSaleState.CLASSIFY_INTENT));
        map.put(AfterSaleState.CLASSIFY_INTENT, Set.of(AfterSaleState.LOCATE_ORDER, AfterSaleState.HANDOFF));
        map.put(AfterSaleState.LOCATE_ORDER, Set.of(AfterSaleState.CHECK_POLICY, AfterSaleState.LOCATE_ORDER,
                AfterSaleState.COMPLETED, AfterSaleState.FAILED));
        map.put(AfterSaleState.CHECK_POLICY, Set.of(AfterSaleState.COLLECT_EVIDENCE, AfterSaleState.HANDOFF,
                AfterSaleState.FAILED));
        map.put(AfterSaleState.COLLECT_EVIDENCE, Set.of(AfterSaleState.CONFIRM_ACTION, AfterSaleState.COLLECT_EVIDENCE,
                AfterSaleState.FAILED));
        map.put(AfterSaleState.CONFIRM_ACTION, Set.of(AfterSaleState.EXECUTE_AFTER_SALE, AfterSaleState.CANCELLED,
                AfterSaleState.CONFIRM_ACTION, AfterSaleState.FAILED));
        map.put(AfterSaleState.EXECUTE_AFTER_SALE, Set.of(AfterSaleState.COMPLETED, AfterSaleState.HANDOFF,
                AfterSaleState.FAILED));
        // 终态无出边（一旦进入即结束本轮执行）
        map.put(AfterSaleState.COMPLETED, Set.of());
        map.put(AfterSaleState.CANCELLED, Set.of());
        map.put(AfterSaleState.HANDOFF, Set.of());
        map.put(AfterSaleState.FAILED, Set.of());
        return map;
    }

    private static Map<AfterSaleState, Set<String>> buildAllowedTools() {
        Map<AfterSaleState, Set<String>> map = new EnumMap<>(AfterSaleState.class);
        map.put(AfterSaleState.LOCATE_ORDER, Set.of(TOOL_ORDER_LOCATOR, TOOL_PRODUCT_RECOMMEND));
        map.put(AfterSaleState.CHECK_POLICY, Set.of(TOOL_POLICY_CHECK));
        map.put(AfterSaleState.EXECUTE_AFTER_SALE, Set.of(TOOL_CREATE_AFTER_SALE));
        map.put(AfterSaleState.HANDOFF, Set.of(TOOL_HANDOFF));
        // START/CLASSIFY_INTENT/COLLECT_EVIDENCE/CONFIRM_ACTION/COMPLETED/CANCELLED/FAILED 不允许工具调用
        return map;
    }

    /**
     * 是否允许 from → to 迁移
     */
    public boolean canTransit(AfterSaleState from, AfterSaleState to) {
        if (from == null || to == null) {
            return false;
        }
        // 查迁移表：未登记的 from 默认空集（非法迁移一律拒绝）
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * 从某状态允许迁移到的状态集合
     */
    public Set<AfterSaleState> allowedTransitions(AfterSaleState from) {
        return TRANSITIONS.getOrDefault(from, Set.of());
    }

    /**
     * 是否终态
     */
    public boolean isTerminal(AfterSaleState state) {
        return state != null && state.isTerminal();
    }

    /**
     * 某状态允许调用的工具集
     */
    public Set<String> allowedTools(AfterSaleState state) {
        return ALLOWED_TOOLS.getOrDefault(state, Set.of());
    }

    /**
     * 校验状态是否允许调用指定工具
     */
    public boolean isToolAllowed(AfterSaleState state, String toolName) {
        // 工具调用门禁：仅允许当前状态授权的工具
        return allowedTools(state).contains(toolName);
    }
}
