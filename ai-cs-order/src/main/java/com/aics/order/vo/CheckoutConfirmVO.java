package com.aics.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算确认页展示 VO
 */
@Data
public class CheckoutConfirmVO {

    private List<CartVO.CartItemVO> items;

    private BigDecimal totalAmount;

    private FullReductionVO fullReduction;

    private List<CouponVO> availableCoupons;

    private BigDecimal payAmount;

    @Data
    public static class FullReductionVO {
        private Boolean applied;
        private String ruleName;
        private BigDecimal amount;
    }

    @Data
    public static class CouponVO {
        private Long id;
        private String couponName;
        private BigDecimal amount;
        private BigDecimal minOrderAmount;
        private Boolean usable;
        private String reason;
    }
}
