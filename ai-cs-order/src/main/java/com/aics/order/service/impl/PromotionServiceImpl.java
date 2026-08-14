package com.aics.order.service.impl;

import com.aics.order.bo.PriceCalcBO;
import com.aics.order.entity.Coupon;
import com.aics.order.entity.FullReductionRule;
import com.aics.order.enums.CouponStatus;
import com.aics.order.mapper.CouponMapper;
import com.aics.order.mapper.FullReductionRuleMapper;
import com.aics.order.service.PromotionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠计算服务实现
 * 计算顺序：原价 → 满减（取最优） → 优惠券（用户选择） → 应付金额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final FullReductionRuleMapper fullReductionRuleMapper;
    private final CouponMapper couponMapper;

    @Override
    /**
     * 结算试算：商品合计 -> 满减/优惠券抵扣 -> 应付金额。
     * <p><b>学习要点</b>：促销规则（满减、优惠券）统一在此计算，
     * 下单时必须用同一套规则保证"试算价 == 实付价"。</p>
     */
    public PriceCalcBO calculatePrice(BigDecimal totalAmount, Long userId, Long couponId) {
        PriceCalcBO bo = new PriceCalcBO();
        bo.setTotalAmount(totalAmount);
        bo.setFullReductionAmount(BigDecimal.ZERO);
        bo.setCouponAmount(BigDecimal.ZERO);

        // 1. 满减计算：取最优（减免金额最大的一条）
        BigDecimal afterReduction = applyFullReduction(totalAmount, bo);

        // 2. 优惠券计算
        BigDecimal afterCoupon = applyCoupon(afterReduction, userId, couponId, bo);

        // 3. 汇总
        BigDecimal discountAmount = bo.getFullReductionAmount().add(bo.getCouponAmount());
        bo.setDiscountAmount(discountAmount);
        bo.setPayAmount(afterCoupon.max(BigDecimal.ZERO));

        return bo;
    }

    @Override
    /** 查询用户可用优惠券（按订单金额过滤门槛，AI 客服"有哪些券能用"工具的服务端） */
    public List<CouponAvailability> getAvailableCoupons(Long userId, BigDecimal orderAmount) {
        List<Coupon> coupons = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getUserId, userId)
                        .eq(Coupon::getStatus, CouponStatus.UNUSED.getCode())
                        .gt(Coupon::getExpireTime, LocalDateTime.now()));

        return coupons.stream().map(coupon -> {
            CouponAvailability ca = new CouponAvailability();
            ca.setId(coupon.getId());
            ca.setCouponName(coupon.getCouponName());
            ca.setAmount(coupon.getAmount());
            ca.setMinOrderAmount(coupon.getMinOrderAmount());

            if (orderAmount.compareTo(coupon.getMinOrderAmount()) >= 0) {
                ca.setUsable(true);
            } else {
                ca.setUsable(false);
                ca.setReason("未满" + coupon.getMinOrderAmount() + "元");
            }
            return ca;
        }).collect(Collectors.toList());
    }

    /**
     * 应用满减规则（取最优）
     */
    private BigDecimal applyFullReduction(BigDecimal totalAmount, PriceCalcBO bo) {
        LocalDateTime now = LocalDateTime.now();
        List<FullReductionRule> rules = fullReductionRuleMapper.selectList(
                new LambdaQueryWrapper<FullReductionRule>()
                        .eq(FullReductionRule::getEnabled, true)
                        .le(FullReductionRule::getStartTime, now)
                        .ge(FullReductionRule::getEndTime, now));

        // 筛选满足门槛的规则，取减免金额最大的一条
        rules.stream()
                .filter(rule -> totalAmount.compareTo(rule.getThresholdAmount()) >= 0)
                .max(Comparator.comparing(FullReductionRule::getReductionAmount))
                .ifPresent(bestRule -> {
                    bo.setFullReductionAmount(bestRule.getReductionAmount());
                    bo.setFullReductionRuleName(bestRule.getRuleName());
                });

        return totalAmount.subtract(bo.getFullReductionAmount());
    }

    /**
     * 应用优惠券
     */
    private BigDecimal applyCoupon(BigDecimal amountAfterReduction, Long userId, Long couponId, PriceCalcBO bo) {
        if (couponId == null) {
            return amountAfterReduction;
        }

        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            return amountAfterReduction;
        }

        // 校验：本人、未使用、未过期、满足门槛
        boolean valid = coupon.getUserId().equals(userId)
                && CouponStatus.UNUSED.getCode().equals(coupon.getStatus())
                && coupon.getExpireTime().isAfter(LocalDateTime.now())
                && amountAfterReduction.compareTo(coupon.getMinOrderAmount()) >= 0;

        if (valid) {
            bo.setCouponAmount(coupon.getAmount());
            return amountAfterReduction.subtract(coupon.getAmount());
        }

        return amountAfterReduction;
    }
}
