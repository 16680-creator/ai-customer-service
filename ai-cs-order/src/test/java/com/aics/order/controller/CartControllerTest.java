package com.aics.order.controller;

import com.aics.common.result.Result;
import com.aics.order.dto.CartUpdateDTO;
import com.aics.order.mapper.CartItemMapper;
import com.aics.order.service.CartService;
import com.aics.order.service.PromotionService;
import com.aics.order.vo.CartVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 购物车控制器单元测试
 * TDD: 验证控制器正确委托 Service 层
 */
@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private CartItemMapper cartItemMapper;

    @InjectMocks
    private CartController cartController;

    @Test
    @DisplayName("获取购物车列表 - 正常返回")
    void getCartList_shouldReturn200() {
        CartVO cartVO = new CartVO();
        cartVO.setItems(Collections.emptyList());
        cartVO.setTotalAmount(BigDecimal.ZERO);
        cartVO.setSelectedCount(0);
        when(cartService.getCartList(100L)).thenReturn(cartVO);

        Result<CartVO> result = cartController.getCartList(100L);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().getItems().isEmpty());
    }

    @Test
    @DisplayName("修改数量 - 成功返回更新后的购物车")
    void updateQuantity_shouldReturn200() {
        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setCartItemId(1L);
        dto.setQuantity(3);

        CartVO cartVO = new CartVO();
        cartVO.setItems(Collections.emptyList());
        cartVO.setTotalAmount(new BigDecimal("597.00"));
        cartVO.setSelectedCount(0);
        when(cartService.updateQuantity(100L, 1L, 3)).thenReturn(cartVO);

        Result<CartVO> result = cartController.updateQuantity(100L, dto);

        assertEquals(200, result.getCode());
        assertEquals(new BigDecimal("597.00"), result.getData().getTotalAmount());
    }

    @Test
    @DisplayName("删除购物车商品 - 成功")
    void deleteCartItem_shouldReturn200() {
        doNothing().when(cartService).deleteCartItem(100L, 1L);

        Result<Void> result = cartController.deleteCartItem(100L, 1L);

        assertEquals(200, result.getCode());
        verify(cartService).deleteCartItem(100L, 1L);
    }

    @Test
    @DisplayName("切换选中状态 - 成功")
    void selectCartItems_shouldReturn200() {
        doNothing().when(cartService).selectCartItems(eq(100L), anyList(), eq(true));

        Map<String, Object> body = Map.of("cartItemIds", List.of(1, 2), "selected", true);
        Result<Void> result = cartController.selectCartItems(100L, body);

        assertEquals(200, result.getCode());
        verify(cartService).selectCartItems(eq(100L), eq(List.of(1L, 2L)), eq(true));
    }
}
