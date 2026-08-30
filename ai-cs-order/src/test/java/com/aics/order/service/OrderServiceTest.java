package com.aics.order.service;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.client.OrderPayClient;
import com.aics.order.client.ProductStockClient;
import com.aics.order.entity.CartItem;
import com.aics.order.entity.Order;
import com.aics.order.entity.OrderItem;
import com.aics.order.enums.OrderStatus;
import com.aics.order.mapper.CartItemMapper;
import com.aics.order.mapper.CouponMapper;
import com.aics.order.mapper.OrderItemMapper;
import com.aics.order.mapper.OrderMapper;
import com.aics.order.service.impl.OrderServiceImpl;
import com.aics.order.vo.OrderVO;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private CouponMapper couponMapper;
    @Mock
    private PromotionService promotionService;
    @Mock
    private ProductStockClient productStockClient;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private OrderPayClient orderPayClient;
    @Mock
    private com.aics.order.lock.OrderCreateLockService orderCreateLockService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUpLockPassThrough() {
        // 锁服务在单测中直通：拿到锁立即执行业务（分布式锁行为由 OrderCreateLockServiceTest 覆盖）
        // lenient：仅下单用例会实际使用该 stub
        org.mockito.Mockito.lenient().when(orderCreateLockService.withCreateLock(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(1)).get());
    }

    private CartItem cartItem1;
    private CartItem cartItem2;

    @BeforeEach
    void setUp() {
        cartItem1 = new CartItem();
        cartItem1.setId(1L);
        cartItem1.setUserId(100L);
        cartItem1.setProductId(1001L);
        cartItem1.setProductName("无线蓝牙耳机");
        cartItem1.setProductPrice(new BigDecimal("199.00"));
        cartItem1.setQuantity(2);
        cartItem1.setSelected(true);

        cartItem2 = new CartItem();
        cartItem2.setId(2L);
        cartItem2.setUserId(100L);
        cartItem2.setProductId(1002L);
        cartItem2.setProductName("手机壳");
        cartItem2.setProductPrice(new BigDecimal("29.00"));
        cartItem2.setQuantity(1);
        cartItem2.setSelected(true);
    }

    @Test
    @DisplayName("正常下单 - 生成待支付订单")
    void createOrder_shouldSucceed() {
        when(cartItemMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(cartItem1, cartItem2));
        doNothing().when(productStockClient).deductStock(anyLong(), anyInt());
        when(promotionService.calculatePrice(any(), eq(100L), isNull()))
                .thenReturn(buildPriceCalcBO());
        when(orderMapper.insert(any())).thenReturn(1);

        OrderVO result = orderService.createOrder(100L, Arrays.asList(1L, 2L), null, "WECHAT");

        assertNotNull(result);
        assertNotNull(result.getOrderNo());
        assertEquals("PENDING_PAY", result.getStatus());
        verify(orderMapper).insert(any());
        verify(productStockClient).deductStock(eq(1001L), eq(2));
        verify(productStockClient).deductStock(eq(1002L), eq(1));
        verify(rocketMQTemplate).syncSend(eq("order-timeout-topic"), any(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("库存不足 - 拒绝下单")
    void createOrder_stockInsufficient_shouldThrow() {
        when(cartItemMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(cartItem1));
        // 商品服务扣减库存返回 4xx（库存不足），RestTemplate 抛异常 -> 下单拒绝
        doThrow(new BusinessException(ResultCode.ORDER_STOCK_INSUFFICIENT, "库存不足"))
                .when(productStockClient).deductStock(eq(1001L), eq(2));

        assertThrows(BusinessException.class,
                () -> orderService.createOrder(100L, Arrays.asList(1L), null, "WECHAT"));
        // 单个商品即失败，无已扣项需回滚
        verify(productStockClient, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("支付确认 - 幂等：已支付订单重复确认不重复处理")
    void confirmPay_idempotent() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("20260801143022010001");
        order.setStatus(OrderStatus.PAID.getCode());
        order.setUserId(100L);

        when(orderMapper.selectOne(any())).thenReturn(order);

        assertDoesNotThrow(() -> orderService.confirmPay("20260801143022010001", "WECHAT", null, "tx1"));
        verify(orderMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("支付确认 - 金额不一致应抛出异常")
    void confirmPay_amountMismatch_shouldThrow() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("20260801143022010002");
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setPayAmount(new BigDecimal("199.00"));
        order.setUserId(100L);

        when(orderMapper.selectOne(any())).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> orderService.confirmPay("20260801143022010002", "WECHAT", new BigDecimal("0.01"), "tx1"));
    }

    @Test
    @DisplayName("支付确认 - 金额一致更新为已支付")
    void confirmPay_success() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("20260801143022010003");
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setPayAmount(new BigDecimal("199.00"));
        order.setUserId(100L);

        when(orderMapper.selectOne(any())).thenReturn(order);

        orderService.confirmPay("20260801143022010003", "WECHAT", new BigDecimal("199.00"), "tx1");

        verify(orderMapper).updateById(argThat(o -> OrderStatus.PAID.getCode().equals(o.getStatus())));
    }

    @Test
    @DisplayName("超时取消 - 未支付订单自动取消并通知支付服务关单")
    void cancelExpiredOrder_shouldCancel() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("20260801143022010001");
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setUserId(100L);
        order.setPaymentMethod("WECHAT");

        OrderItem item = new OrderItem();
        item.setProductId(1001L);
        item.setQuantity(2);

        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        doNothing().when(productStockClient).restoreStock(eq(1001L), eq(2));

        orderService.cancelExpiredOrder("20260801143022010001");

        verify(orderMapper).updateById(argThat(o ->
                OrderStatus.CANCELLED.getCode().equals(o.getStatus())));
        verify(productStockClient).restoreStock(eq(1001L), eq(2));
        verify(orderPayClient).closeChannel("WECHAT", "20260801143022010001");
    }

    @Test
    @DisplayName("退款确认 - 已支付订单退款成功并回补库存")
    void refundConfirm_paidOrder_shouldRefundAndRestoreStock() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD20260809001");
        order.setUserId(100L);
        order.setStatus(OrderStatus.PAID.getCode());

        OrderItem item = new OrderItem();
        item.setOrderNo("ORD20260809001");
        item.setProductId(1001L);
        item.setQuantity(2);

        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(Arrays.asList(item));
        doNothing().when(productStockClient).restoreStock(eq(1001L), eq(2));

        assertDoesNotThrow(() -> orderService.refundConfirm("ORD20260809001"));
        assertEquals(OrderStatus.REFUNDED.getCode(), order.getStatus());
        verify(productStockClient).restoreStock(eq(1001L), eq(2));
    }

    @Test
    @DisplayName("退款确认 - 非已支付订单应抛出异常")
    void refundConfirm_notPaid_shouldThrow() {
        Order order = new Order();
        order.setId(2L);
        order.setOrderNo("ORD20260809002");
        order.setUserId(100L);
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        when(orderMapper.selectOne(any())).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> orderService.refundConfirm("ORD20260809002"));
    }

    @Test
    @DisplayName("订单支付信息 - 返回状态/金额/过期时间")
    void getOrderPayDetail_shouldReturnMap() {
        Order order = new Order();
        order.setOrderNo("ORD20260809003");
        order.setUserId(100L);
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setPayAmount(new BigDecimal("199.00"));
        order.setPaymentMethod("ALIPAY");
        when(orderMapper.selectOne(any())).thenReturn(order);

        Map<String, Object> data = orderService.getOrderPayDetail("ORD20260809003");

        assertEquals("ORD20260809003", data.get("orderNo"));
        assertEquals("PENDING_PAY", data.get("status"));
        assertEquals("ALIPAY", data.get("paymentMethod"));
    }

    private com.aics.order.bo.PriceCalcBO buildPriceCalcBO() {
        com.aics.order.bo.PriceCalcBO bo = new com.aics.order.bo.PriceCalcBO();
        bo.setTotalAmount(new BigDecimal("427.00"));
        bo.setFullReductionAmount(new BigDecimal("30.00"));
        bo.setFullReductionRuleName("满200减30");
        bo.setCouponAmount(BigDecimal.ZERO);
        bo.setDiscountAmount(new BigDecimal("30.00"));
        bo.setPayAmount(new BigDecimal("397.00"));
        return bo;
    }
}