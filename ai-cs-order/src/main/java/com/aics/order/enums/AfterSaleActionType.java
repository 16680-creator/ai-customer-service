package com.aics.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后动作类型枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleActionType {

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
            return null;
        }
        for (AfterSaleActionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
