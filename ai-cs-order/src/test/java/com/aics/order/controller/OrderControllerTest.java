package com.aics.order.controller;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.order.service.OrderService;
import com.aics.order.vo.OrderVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单控制器单元测试
 * TDD: 验证控制器正确委托 Service 层、异常传播、返回结构
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private OrderVO buildSampleOrderVO() {
        OrderVO vo = new OrderVO();
        vo.setOrderNo("20260801120000010001");
        vo.setStatus("PENDING_PAY");
        vo.setPayAmount(new BigDecimal("190.00"));
        vo.setPaymentMethod("WECHAT");
        vo.setPayUrl("https://pay.weixin.qq.com/pay?order=20260801120000010001");
        vo.setExpireTime(LocalDateTime.now().plusMinutes(30));
        return vo;
    }

    // ==================== createOrder ====================

    @Test
    @DisplayName("提交订单 - 成功返回订单信息和支付URL")
    void createOrder_success() {
        OrderVO vo = buildSampleOrderVO();
        when(orderService.createOrder(eq(100L), anyList(), isNull(), eq("WECHAT"))).thenReturn(vo);

        var dto = new com.aics.order.dto.OrderCreateDTO();
        dto.setCartItemIds(List.of(1L, 2L));
        dto.setPaymentMethod("WECHAT");

        Result<OrderVO> result = orderController.createOrder(100L, dto);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("20260801120000010001", result.getData().getOrderNo());
        assertNotNull(result.getData().getPayUrl());
        assertEquals("PENDING_PAY", result.getData().getStatus());
    }

    @Test
    @DisplayName("提交订单 - 购物车为空应抛出业务异常")
    void createOrder_emptyCart_shouldThrowBusinessException() {
        when(orderService.createOrder(eq(100L), anyList(), isNull(), eq("WECHAT")))
                .thenThrow(new BusinessException(ResultCode.ORDER_CART_EMPTY));

        var dto = new com.aics.order.dto.OrderCreateDTO();
        dto.setCartItemIds(List.of(99L));
        dto.setPaymentMethod("WECHAT");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderController.createOrder(100L, dto));
        assertEquals(7006, ex.getCode());
    }

    @Test
    @DisplayName("提交订单 - 库存不足应抛出业务异常")
    void createOrder_stockInsufficient_shouldThrowBusinessException() {
        when(orderService.createOrder(eq(100L), anyList(), isNull(), eq("ALIPAY")))
                .thenThrow(new BusinessException(ResultCode.ORDER_STOCK_INSUFFICIENT, "商品库存不足: 蓝牙耳机"));

        var dto = new com.aics.order.dto.OrderCreateDTO();
        dto.setCartItemIds(List.of(1L));
        dto.setPaymentMethod("ALIPAY");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderController.createOrder(100L, dto));
        assertEquals(7001, ex.getCode());
        assertTrue(ex.getMessage().contains("蓝牙耳机"));
    }

    // ==================== getOrderDetail ====================

    @Test
    @DisplayName("查询订单详情 - 成功返回")
    void getOrderDetail_success() {
        OrderVO vo = buildSampleOrderVO();
        when(orderService.getOrderDetail(100L, "20260801120000010001")).thenReturn(vo);

        Result<OrderVO> result = orderController.getOrderDetail(100L, "20260801120000010001");

        assertEquals(200, result.getCode());
        assertEquals("20260801120000010001", result.getData().getOrderNo());
        assertEquals(new BigDecimal("190.00"), result.getData().getPayAmount());
    }

    @Test
    @DisplayName("查询订单详情 - 订单不存在应抛出异常")
    void getOrderDetail_notFound_shouldThrow() {
        when(orderService.getOrderDetail(100L, "NON_EXIST"))
                .thenThrow(new BusinessException(ResultCode.ORDER_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderController.getOrderDetail(100L, "NON_EXIST"));
        assertEquals(7004, ex.getCode());
    }

    // ==================== cancelOrder ====================

    @Test
    @DisplayName("取消订单 - 成功")
    void cancelOrder_success() {
        doNothing().when(orderService).cancelOrder(100L, "ORD001");

        Result<Void> result = orderController.cancelOrder(100L, "ORD001");

        assertEquals(200, result.getCode());
        verify(orderService).cancelOrder(100L, "ORD001");
    }

    @Test
    @DisplayName("取消订单 - 已支付订单不可取消")
    void cancelOrder_alreadyPaid_shouldThrow() {
        doThrow(new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或状态不允许取消"))
                .when(orderService).cancelOrder(100L, "ORD_PAID");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderController.cancelOrder(100L, "ORD_PAID"));
        assertEquals(7004, ex.getCode());
    }
}
