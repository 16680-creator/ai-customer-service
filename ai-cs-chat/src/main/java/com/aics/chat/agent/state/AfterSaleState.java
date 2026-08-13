package com.aics.chat.agent.state;

/**
 * 售后 Agent 显式状态机状态定义
 *
 * <pre>
 * START
 *   ↓
 * CLASSIFY_INTENT
 *   ↓
 * LOCATE_ORDER ──找不到/多候选──> 停留在 LOCATE_ORDER 询问用户
 *   ↓
 * CHECK_POLICY ──不满足──> HANDOFF
 *   ↓
 * COLLECT_EVIDENCE ──缺参数──> 停留在 COLLECT_EVIDENCE 询问用户
 *   ↓
 * CONFIRM_ACTION ──拒绝──> CANCELLED
 *   ↓
 * EXECUTE_AFTER_SALE ──成功──> COMPLETED
 *                     └─失败──> HANDOFF
 * </pre>
 */
public enum AfterSaleState {

    /** 开始 */
    START,

    /** 意图识别 */
    CLASSIFY_INTENT,

    /** 订单定位 */
    LOCATE_ORDER,

    /** 售后规则校验 */
    CHECK_POLICY,

    /** 收集证据/必要参数 */
    COLLECT_EVIDENCE,

    /** 写操作确认 */
    CONFIRM_ACTION,

    /** 执行售后申请 */
    EXECUTE_AFTER_SALE,

    /** 完成 */
    COMPLETED,

    /** 取消 */
    CANCELLED,

    /** 转人工 */
    HANDOFF,

    /** 失败中止 */
    FAILED;

    public boolean isTerminal() {
        // 终态判定：完成/取消/转人工/失败后不再驱动状态机
        return this == COMPLETED || this == CANCELLED || this == HANDOFF || this == FAILED;
    }
}
