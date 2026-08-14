package com.aics.order.service;

import com.aics.order.bo.PriceCalcBO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠计算服务接口
 */
public interface PromotionService {

    /**
     * 计算价格（满减 + 优惠券）
     *
     * @param totalAmount 商品原价总计
     * @param userId      用户ID
     * @param couponId    优惠券ID（可为null）
     * @return 价格计算结果
     */
    PriceCalcBO calculatePrice(BigDecimal totalAmount, Long userId, Long couponId);

    /**
     * 获取用户可用优惠券列表（含可用性判断）
     */
    List<CouponAvailability> getAvailableCoupons(Long userId, BigDecimal orderAmount);

    /**
     * 优惠券可用性
     */
    @Data
    class CouponAvailability {
        private Long id;
        private String couponName;
        private BigDecimal amount;
        private BigDecimal minOrderAmount;
        private boolean usable;
        private String reason;
    }
}
