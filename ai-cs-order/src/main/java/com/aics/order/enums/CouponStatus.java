package com.aics.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券状态枚举
 */
@Getter
@AllArgsConstructor
public enum CouponStatus {

    UNUSED("UNUSED", "未使用"),
    USED("USED", "已使用"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String description;
}
