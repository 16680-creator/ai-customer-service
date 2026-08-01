package com.aics.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentMethod {

    WECHAT("WECHAT", "微信支付"),
    ALIPAY("ALIPAY", "支付宝"),
    BANK_CARD("BANK_CARD", "银行卡");

    private final String code;
    private final String description;
}
