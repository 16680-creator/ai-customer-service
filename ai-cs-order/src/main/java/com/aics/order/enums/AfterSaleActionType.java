package com.aics.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后动作类型枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleActionType {

    // 支持的售后动作：换货 / 退货 / 退款
    EXCHANGE("EXCHANGE", "换货"),
    RETURN("RETURN", "退货"),
    REFUND("REFUND", "退款");

    private final String code;
    private final String description;

    /**
     * 根据 code 查找枚举，无效返回 null
     */
    public static AfterSaleActionType fromCode(String code) {
        if (code == null) {
            return null; // 空值视为无效动作
        }
        for (AfterSaleActionType type : values()) {
            if (type.code.equals(code)) {
                return type; // 命中 code 即返回对应枚举
            }
        }
        return null; // 未匹配到任何枚举值（客户端传了未知动作）
    }
}
