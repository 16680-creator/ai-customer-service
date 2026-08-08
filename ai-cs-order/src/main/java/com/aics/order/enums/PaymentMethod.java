package com.aics.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentMethod {

    MOCK("MOCK", "模拟支付"),
    WECHAT("WECHAT", "微信支付"),
    ALIPAY("ALIPAY", "支付宝"),
    BANK_CARD("BANK_CARD", "银行卡"),
    UNIONPAY("UNIONPAY", "银联云闪付");

    private final String code;
    private final String description;
}
