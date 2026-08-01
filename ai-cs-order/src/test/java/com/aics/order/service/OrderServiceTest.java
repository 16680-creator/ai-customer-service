package com.aics.order.service;

import com.aics.common.exception.BusinessException;
import com.aics.order.entity.CartItem;
import com.aics.order.entity.Order;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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
    private PaymentService paymentService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

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
    @DisplayName("正常下单 - 生成订单并返回支付信息")
    void createOrder_shouldSucceed() {
        when(cartItemMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(cartItem1, cartItem2));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(eq("stock:1001"), eq(2L))).thenReturn(8L);
        when(valueOperations.decrement(eq("stock:1002"), eq(1L))).thenReturn(9L);
        when(promotionService.calculatePrice(any(), eq(100L), isNull()))
                .thenReturn(buildPriceCalcBO());
        when(orderMapper.insert(any())).thenReturn(1);
        when(paymentService.createPayment(anyString(), any(), anyString())).thenReturn("https://pay.example.com");

        OrderVO result = orderService.createOrder(100L, Arrays.asList(1L, 2L), null, "WECHAT");

        assertNotNull(result);
        assertNotNull(result.getOrderNo());
        assertEquals("PENDING_PAY", result.getStatus());
        verify(orderMapper).insert(any());
        verify(rocketMQTemplate).syncSend(eq("order-timeout-topic"), any(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("库存不足 - 拒绝下单")
    void createOrder_stockInsufficient_shouldThrow() {
        when(cartItemMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(cartItem1));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(eq("stock:1001"), eq(2L))).thenReturn(-1L);

        assertThrows(BusinessException.class,
                () -> orderService.createOrder(100L, Arrays.asList(1L), null, "WECHAT"));
    }

    @Test
    @DisplayName("支付成功回调 - 幂等处理")
    void handlePayCallback_idempotent() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("20260801143022010001");
        order.setStatus(OrderStatus.PAID.getCode());
        order.setUserId(100L);

        when(orderMapper.selectOne(any())).thenReturn(order);

        // 已支付订单再次回调不应重复处理
        assertDoesNotThrow(() -> orderService.handlePayCallback("20260801143022010001", "WECHAT"));
        verify(orderMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("超时取消 - 未支付订单自动取消")
    void cancelExpiredOrder_shouldCancel() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("20260801143022010001");
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setUserId(100L);
        order.setCouponId(101L);

        when(orderMapper.selectOne(any())).thenReturn(order);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        orderService.cancelExpiredOrder("20260801143022010001");

        verify(orderMapper).updateById(argThat(o ->
                OrderStatus.CANCELLED.getCode().equals(o.getStatus())));
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
