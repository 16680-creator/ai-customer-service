package com.aics.order.service;

import com.aics.order.bo.PriceCalcBO;
import com.aics.order.entity.Coupon;
import com.aics.order.entity.FullReductionRule;
import com.aics.order.mapper.CouponMapper;
import com.aics.order.mapper.FullReductionRuleMapper;
import com.aics.order.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 优惠计算服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private FullReductionRuleMapper fullReductionRuleMapper;

    @Mock
    private CouponMapper couponMapper;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    private FullReductionRule rule100;
    private FullReductionRule rule200;
    private Coupon coupon15;

    @BeforeEach
    void setUp() {
        rule100 = new FullReductionRule();
        rule100.setId(1L);
        rule100.setRuleName("满100减10");
        rule100.setThresholdAmount(new BigDecimal("100.00"));
        rule100.setReductionAmount(new BigDecimal("10.00"));
        rule100.setStartTime(LocalDateTime.now().minusDays(1));
        rule100.setEndTime(LocalDateTime.now().plusDays(1));
        rule100.setEnabled(true);

        rule200 = new FullReductionRule();
        rule200.setId(2L);
        rule200.setRuleName("满200减30");
        rule200.setThresholdAmount(new BigDecimal("200.00"));
        rule200.setReductionAmount(new BigDecimal("30.00"));
        rule200.setStartTime(LocalDateTime.now().minusDays(1));
        rule200.setEndTime(LocalDateTime.now().plusDays(1));
        rule200.setEnabled(true);

        coupon15 = new Coupon();
        coupon15.setId(101L);
        coupon15.setUserId(100L);
        coupon15.setCouponName("新人券");
        coupon15.setAmount(new BigDecimal("15.00"));
        coupon15.setMinOrderAmount(new BigDecimal("100.00"));
        coupon15.setStatus("UNUSED");
        coupon15.setExpireTime(LocalDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("单条满减命中 - 满200减30")
    void calculatePrice_singleRuleHit() {
        when(fullReductionRuleMapper.selectList(any())).thenReturn(Collections.singletonList(rule200));

        PriceCalcBO result = promotionService.calculatePrice(new BigDecimal("250.00"), 100L, null);

        assertEquals(new BigDecimal("30.00"), result.getFullReductionAmount());
        assertEquals(new BigDecimal("220.00"), result.getPayAmount());
        assertEquals("满200减30", result.getFullReductionRuleName());
    }

    @Test
    @DisplayName("多条满减取最优 - 满200减30优于满100减10")
    void calculatePrice_multipleRulesTakeBest() {
        when(fullReductionRuleMapper.selectList(any())).thenReturn(Arrays.asList(rule100, rule200));

        PriceCalcBO result = promotionService.calculatePrice(new BigDecimal("250.00"), 100L, null);

        assertEquals(new BigDecimal("30.00"), result.getFullReductionAmount());
        assertEquals("满200减30", result.getFullReductionRuleName());
    }

    @Test
    @DisplayName("无满减命中 - 金额不满足任何门槛")
    void calculatePrice_noRuleHit() {
        when(fullReductionRuleMapper.selectList(any())).thenReturn(Arrays.asList(rule100, rule200));

        PriceCalcBO result = promotionService.calculatePrice(new BigDecimal("50.00"), 100L, null);

        assertEquals(BigDecimal.ZERO, result.getFullReductionAmount());
        assertEquals(new BigDecimal("50.00"), result.getPayAmount());
    }

    @Test
    @DisplayName("优惠券可用 - 满足门槛")
    void calculatePrice_couponUsable() {
        when(fullReductionRuleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(couponMapper.selectById(101L)).thenReturn(coupon15);

        PriceCalcBO result = promotionService.calculatePrice(new BigDecimal("180.00"), 100L, 101L);

        assertEquals(new BigDecimal("15.00"), result.getCouponAmount());
        assertEquals(new BigDecimal("165.00"), result.getPayAmount());
    }

    @Test
    @DisplayName("优惠券不满足门槛 - 不抵扣")
    void calculatePrice_couponBelowThreshold() {
        when(fullReductionRuleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(couponMapper.selectById(101L)).thenReturn(coupon15);

        PriceCalcBO result = promotionService.calculatePrice(new BigDecimal("80.00"), 100L, 101L);

        assertEquals(BigDecimal.ZERO, result.getCouponAmount());
        assertEquals(new BigDecimal("80.00"), result.getPayAmount());
    }

    @Test
    @DisplayName("满减+优惠券叠加 - 先满减后优惠券")
    void calculatePrice_stackFullReductionAndCoupon() {
        when(fullReductionRuleMapper.selectList(any())).thenReturn(Collections.singletonList(rule200));
        when(couponMapper.selectById(101L)).thenReturn(coupon15);

        PriceCalcBO result = promotionService.calculatePrice(new BigDecimal("250.00"), 100L, 101L);

        assertEquals(new BigDecimal("30.00"), result.getFullReductionAmount());
        assertEquals(new BigDecimal("15.00"), result.getCouponAmount());
        assertEquals(new BigDecimal("45.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("205.00"), result.getPayAmount());
    }

    @Test
    @DisplayName("优惠券已过期 - 不抵扣")
    void calculatePrice_couponExpired() {
        coupon15.setExpireTime(LocalDateTime.now().minusDays(1));
        when(fullReductionRuleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(couponMapper.selectById(101L)).thenReturn(coupon15);

        PriceCalcBO result = promotionService.calculatePrice(new BigDecimal("180.00"), 100L, 101L);

        assertEquals(BigDecimal.ZERO, result.getCouponAmount());
        assertEquals(new BigDecimal("180.00"), result.getPayAmount());
    }

    @Test
    @DisplayName("获取可用优惠券列表")
    void getAvailableCoupons_shouldReturnWithUsability() {
        Coupon coupon50 = new Coupon();
        coupon50.setId(102L);
        coupon50.setUserId(100L);
        coupon50.setCouponName("大额券");
        coupon50.setAmount(new BigDecimal("50.00"));
        coupon50.setMinOrderAmount(new BigDecimal("500.00"));
        coupon50.setStatus("UNUSED");
        coupon50.setExpireTime(LocalDateTime.now().plusDays(7));

        when(couponMapper.selectList(any())).thenReturn(Arrays.asList(coupon15, coupon50));

        List<PromotionService.CouponAvailability> result =
                promotionService.getAvailableCoupons(100L, new BigDecimal("250.00"));

        assertEquals(2, result.size());
        assertTrue(result.get(0).isUsable());
        assertFalse(result.get(1).isUsable());
        assertEquals("未满500.00元", result.get(1).getReason());
    }
}
