package com.aics.order.controller;

import com.aics.common.result.Result;
import com.aics.order.bo.PriceCalcBO;
import com.aics.order.dto.CheckoutDTO;
import com.aics.order.entity.CartItem;
import com.aics.order.mapper.CartItemMapper;
import com.aics.order.service.CartService;
import com.aics.order.service.PromotionService;
import com.aics.order.vo.CheckoutConfirmVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 结算确认接口单元测试
 * TDD: 验证结算确认页数据组装、优惠计算
 */
@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private CartItemMapper cartItemMapper;

    @InjectMocks
    private CartController cartController;

    private CartItem buildCartItem(Long id, String name, String price, int qty) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(100L);
        item.setProductId(1000L + id);
        item.setProductName(name);
        item.setProductPrice(new BigDecimal(price));
        item.setQuantity(qty);
        item.setSelected(true);
        return item;
    }

    private PriceCalcBO buildPriceCalc(String total, String discount, String pay) {
        PriceCalcBO bo = new PriceCalcBO();
        bo.setTotalAmount(new BigDecimal(total));
        bo.setFullReductionAmount(new BigDecimal(discount));
        bo.setFullReductionRuleName("满200减30");
        bo.setCouponAmount(BigDecimal.ZERO);
        bo.setDiscountAmount(new BigDecimal(discount));
        bo.setPayAmount(new BigDecimal(pay));
        return bo;
    }

    @Test
    @DisplayName("结算确认 - 正常返回商品列表和优惠信息")
    void checkoutConfirm_success() {
        List<CartItem> items = List.of(
                buildCartItem(1L, "蓝牙耳机", "199.00", 1),
                buildCartItem(2L, "手机壳", "29.00", 2)
        );
        when(cartItemMapper.selectBatchIds(anyList())).thenReturn(items);
        when(promotionService.calculatePrice(any(BigDecimal.class), eq(100L), isNull()))
                .thenReturn(buildPriceCalc("257.00", "30.00", "227.00"));
        when(promotionService.getAvailableCoupons(eq(100L), any(BigDecimal.class)))
                .thenReturn(Collections.emptyList());

        CheckoutDTO dto = new CheckoutDTO();
        dto.setCartItemIds(List.of(1L, 2L));

        Result<CheckoutConfirmVO> result = cartController.checkoutConfirm(100L, dto);

        assertEquals(200, result.getCode());
        CheckoutConfirmVO vo = result.getData();
        assertEquals(2, vo.getItems().size());
        assertEquals(new BigDecimal("257.00"), vo.getTotalAmount());
        assertTrue(vo.getFullReduction().getApplied());
        assertEquals(new BigDecimal("30.00"), vo.getFullReduction().getAmount());
        assertEquals(new BigDecimal("227.00"), vo.getPayAmount());
    }

    @Test
    @DisplayName("结算确认 - 无满减时 applied 为 false")
    void checkoutConfirm_noFullReduction() {
        List<CartItem> items = List.of(buildCartItem(1L, "数据线", "19.90", 1));
        when(cartItemMapper.selectBatchIds(anyList())).thenReturn(items);

        PriceCalcBO bo = new PriceCalcBO();
        bo.setTotalAmount(new BigDecimal("19.90"));
        bo.setFullReductionAmount(BigDecimal.ZERO);
        bo.setFullReductionRuleName(null);
        bo.setCouponAmount(BigDecimal.ZERO);
        bo.setDiscountAmount(BigDecimal.ZERO);
        bo.setPayAmount(new BigDecimal("19.90"));
        when(promotionService.calculatePrice(any(BigDecimal.class), eq(100L), isNull())).thenReturn(bo);
        when(promotionService.getAvailableCoupons(eq(100L), any(BigDecimal.class)))
                .thenReturn(Collections.emptyList());

        CheckoutDTO dto = new CheckoutDTO();
        dto.setCartItemIds(List.of(1L));

        Result<CheckoutConfirmVO> result = cartController.checkoutConfirm(100L, dto);

        assertFalse(result.getData().getFullReduction().getApplied());
        assertEquals(new BigDecimal("19.90"), result.getData().getPayAmount());
    }

    @Test
    @DisplayName("结算确认 - 返回可用优惠券列表")
    void checkoutConfirm_withAvailableCoupons() {
        List<CartItem> items = List.of(buildCartItem(1L, "键盘", "299.00", 1));
        when(cartItemMapper.selectBatchIds(anyList())).thenReturn(items);
        when(promotionService.calculatePrice(any(BigDecimal.class), eq(100L), isNull()))
                .thenReturn(buildPriceCalc("299.00", "30.00", "269.00"));

        PromotionService.CouponAvailability coupon = new PromotionService.CouponAvailability();
        coupon.setId(10L);
        coupon.setCouponName("新人券满100减15");
        coupon.setAmount(new BigDecimal("15.00"));
        coupon.setMinOrderAmount(new BigDecimal("100.00"));
        coupon.setUsable(true);
        coupon.setReason(null);
        when(promotionService.getAvailableCoupons(eq(100L), any(BigDecimal.class)))
                .thenReturn(List.of(coupon));

        CheckoutDTO dto = new CheckoutDTO();
        dto.setCartItemIds(List.of(1L));

        Result<CheckoutConfirmVO> result = cartController.checkoutConfirm(100L, dto);

        assertEquals(1, result.getData().getAvailableCoupons().size());
        assertEquals("新人券满100减15", result.getData().getAvailableCoupons().get(0).getCouponName());
        assertTrue(result.getData().getAvailableCoupons().get(0).getUsable());
    }
}
