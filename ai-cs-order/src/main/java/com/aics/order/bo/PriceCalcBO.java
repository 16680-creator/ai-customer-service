package com.aics.order.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格计算业务对象
 */
@Data
public class PriceCalcBO {

    /** 商品原价总计 */
    private BigDecimal totalAmount;

    /** 满减优惠金额 */
    private BigDecimal fullReductionAmount;

    /** 满减规则名称 */
    private String fullReductionRuleName;

    /** 优惠券抵扣金额 */
    private BigDecimal couponAmount;

    /** 优惠总金额 */
    private BigDecimal discountAmount;

    /** 应付金额 */
    private BigDecimal payAmount;
}
