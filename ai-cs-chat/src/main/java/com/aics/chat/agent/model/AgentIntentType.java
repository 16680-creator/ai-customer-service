package com.aics.chat.agent.model;

/**
 * Agent 意图类型
 */
public enum AgentIntentType {

    /** 售后（换货/退货/退款） */
    AFTER_SALE,

    /** 商品推荐 */
    PRODUCT_RECOMMEND,

    /** 普通咨询（路由回普通对话） */
    NORMAL_CHAT,

    /** 转人工 */
    HUMAN_HANDOFF,

    /** 其他/未识别 */
    OTHER
}
