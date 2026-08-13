package com.aics.chat.agent.model;

import java.math.BigDecimal;

/**
 * 待执行的写操作计划（用户确认的对象）
 *
 * @param actionType   售后动作
 * @param orderNo      订单号
 * @param productId    商品 ID
 * @param productName  商品名称
 * @param quantity     数量
 * @param reason       原因
 * @param evidenceSummary 规则引用/证据摘要
 * @param basePrice    订单商品单价（用于同价位推荐，可为空）
 */
public record AgentActionPlan(AfterSaleActionType actionType, String orderNo, Long productId,
                              String productName, int quantity, String reason,
                              String evidenceSummary, BigDecimal basePrice) {
}
