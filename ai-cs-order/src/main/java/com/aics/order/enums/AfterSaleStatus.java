package com.aics.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后申请状态枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleStatus {

    PENDING("PENDING", "待处理"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已拒绝"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;
}
