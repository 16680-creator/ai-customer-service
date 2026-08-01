package com.aics.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    PENDING_PAY("PENDING_PAY", "待支付"),
    PAID("PAID", "已支付"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;
}
