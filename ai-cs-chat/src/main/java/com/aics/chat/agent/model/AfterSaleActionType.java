package com.aics.chat.agent.model;

/**
 * 售后动作类型（chat 侧枚举，与 ai-cs-order 的 AfterSaleActionType 字符串值一致）
 */
public enum AfterSaleActionType {

    /** 换货 */
    EXCHANGE("EXCHANGE", "换货"),

    /** 退货 */
    RETURN("RETURN", "退货"),

    /** 退款 */
    REFUND("REFUND", "退款");

    private final String code;
    private final String desc;

    AfterSaleActionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按字符串值解析（兼容大小写）
     */
    public static AfterSaleActionType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AfterSaleActionType t : values()) {
            if (t.code.equalsIgnoreCase(code.trim())) {
                return t;
            }
        }
        return null;
    }
}
