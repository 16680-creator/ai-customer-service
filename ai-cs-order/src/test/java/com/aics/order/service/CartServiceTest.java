package com.aics.order.service;

import com.aics.common.exception.BusinessException;
import com.aics.order.entity.CartItem;
import com.aics.order.mapper.CartItemMapper;
import com.aics.order.service.impl.CartServiceImpl;
import com.aics.order.vo.CartVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 购物车服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CartServiceImpl cartService;

    private CartItem sampleCartItem;

    @BeforeEach
    void setUp() {
        sampleCartItem = new CartItem();
        sampleCartItem.setId(1L);
        sampleCartItem.setUserId(100L);
        sampleCartItem.setProductId(1001L);
        sampleCartItem.setProductName("无线蓝牙耳机");
        sampleCartItem.setProductPrice(new BigDecimal("199.00"));
        sampleCartItem.setQuantity(2);
        sampleCartItem.setSelected(true);
    }

    @Test
    @DisplayName("获取购物车列表 - 正常返回")
    void getCartList_shouldReturnItems() {
        when(cartItemMapper.selectList(any())).thenReturn(Arrays.asList(sampleCartItem));

        CartVO result = cartService.getCartList(100L);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("398.00"), result.getTotalAmount());
        assertEquals(1, result.getSelectedCount());
    }

    @Test
    @DisplayName("获取购物车列表 - 空购物车")
    void getCartList_emptyCart_shouldReturnEmptyList() {
        when(cartItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        CartVO result = cartService.getCartList(100L);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
    }

    @Test
    @DisplayName("修改数量 - 正常修改")
    void updateQuantity_shouldUpdateSuccessfully() {
        when(cartItemMapper.selectById(1L)).thenReturn(sampleCartItem);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("stock:1001")).thenReturn("10");
        when(cartItemMapper.updateById(any())).thenReturn(1);

        CartVO result = cartService.updateQuantity(100L, 1L, 3);

        assertNotNull(result);
        verify(cartItemMapper).updateById(argThat(item -> item.getQuantity() == 3));
    }

    @Test
    @DisplayName("修改数量 - 超出库存应抛出异常")
    void updateQuantity_exceedStock_shouldThrowException() {
        when(cartItemMapper.selectById(1L)).thenReturn(sampleCartItem);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("stock:1001")).thenReturn("5");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> cartService.updateQuantity(100L, 1L, 10));

        assertTrue(exception.getMessage().contains("库存不足"));
    }

    @Test
    @DisplayName("修改数量 - 数量为零或负数应抛出异常")
    void updateQuantity_zeroOrNegative_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateQuantity(100L, 1L, 0));
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateQuantity(100L, 1L, -1));
    }

    @Test
    @DisplayName("删除购物车商品 - 正常删除")
    void deleteCartItem_shouldDeleteSuccessfully() {
        when(cartItemMapper.selectById(1L)).thenReturn(sampleCartItem);
        when(cartItemMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> cartService.deleteCartItem(100L, 1L));
        verify(cartItemMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除购物车商品 - 非本人购物车项应抛出异常")
    void deleteCartItem_notOwner_shouldThrowException() {
        when(cartItemMapper.selectById(1L)).thenReturn(sampleCartItem);

        assertThrows(BusinessException.class,
                () -> cartService.deleteCartItem(999L, 1L));
    }
}
