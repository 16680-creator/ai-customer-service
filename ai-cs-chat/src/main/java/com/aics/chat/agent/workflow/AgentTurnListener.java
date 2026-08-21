package com.aics.chat.agent.workflow;

/**
 * Agent 单轮编排的流式事件监听器（SSE 输出用）。
 *
 * <p>编排器在关键节点回调，控制器把事件桥接为 SSE 推送给前端，
 * 实现「步骤进度 + 回复打字机」的流式体验。所有方法均为默认空实现，
 * 监听器只需覆盖关心的事件；传 null 等价于不监听（同步端点行为不变）。</p>
 */
public interface AgentTurnListener {

    /**
     * 编排步骤事件（安全检查/意图识别/订单定位/规则校验/执行等）
     *
     * @param phase  步骤标识（SAFETY/INTENT/LOCATE_ORDER/...）
     * @param detail 步骤摘要（人类可读，供前端进度展示）
     */
    default void onStep(String phase, String detail) {
    }

    /**
     * 回复 token 事件（仅普通对话路由时产生，逐 chunk 回调实现打字机效果）
     *
     * @param chunk 文本片段
     */
    default void onToken(String chunk) {
    }
}
