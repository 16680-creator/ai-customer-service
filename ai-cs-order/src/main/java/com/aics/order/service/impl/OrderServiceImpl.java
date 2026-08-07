package com.aics.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.bo.PriceCalcBO;
import com.aics.order.entity.*;
import com.aics.order.enums.CouponStatus;
import com.aics.order.enums.OrderStatus;
import com.aics.order.mapper.*;
import com.aics.order.service.OrderService;
import com.aics.order.service.PaymentService;
import com.aics.order.service.PromotionService;
import com.aics.order.vo.OrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final CouponMapper couponMapper;
    private final PromotionService promotionService;
    private final PaymentService paymentService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${order.timeout-minutes:30}")
    private int timeoutMinutes;

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, List<Long> cartItemIds, Long couponId, String paymentMethod) {
        // 1. 获取购物车商品
        List<CartItem> cartItems = cartItemMapper.selectBatchIds(cartItemIds);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ResultCode.ORDER_CART_EMPTY);
        }

        // 2. Redis 库存预扣
        for (CartItem item : cartItems) {
            Long remaining = stringRedisTemplate.opsForValue()
                    .decrement("stock:" + item.getProductId(), item.getQuantity());
            if (remaining == null || remaining < 0) {
                // 回补已扣库存
                rollbackStock(cartItems, item);
                throw new BusinessException(ResultCode.ORDER_STOCK_INSUFFICIENT,
                        "商品库存不足: " + item.getProductName());
            }
        }

        // 3. 计算价格
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PriceCalcBO priceCalc = promotionService.calculatePrice(totalAmount, userId, couponId);

        // 4. 生成订单
        String orderNo = generateOrderNo();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(priceCalc.getTotalAmount());
        order.setDiscountAmount(priceCalc.getDiscountAmount());
        order.setPayAmount(priceCalc.getPayAmount());
        order.setFullReductionAmount(priceCalc.getFullReductionAmount());
        order.setCouponAmount(priceCalc.getCouponAmount());
        order.setCouponId(couponId);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setExpireTime(LocalDateTime.now().plusMinutes(timeoutMinutes));
        orderMapper.insert(order);

        // 5. 生成订单项
        for (CartItem item : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(orderNo);
            orderItem.setProductId(item.getProductId());
            orderItem.setProductName(item.getProductName());
            orderItem.setProductPrice(item.getProductPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(orderItem);
        }

        // 6. 核销优惠券
        if (couponId != null && priceCalc.getCouponAmount().compareTo(BigDecimal.ZERO) > 0) {
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon != null) {
                coupon.setStatus(CouponStatus.USED.getCode());
                coupon.setUseTime(LocalDateTime.now());
                coupon.setOrderNo(orderNo);
                couponMapper.updateById(coupon);
            }
        }

        // 7. 发送延迟消息（超时取消）
        rocketMQTemplate.syncSend("order-timeout-topic",
                MessageBuilder.withPayload(orderNo).build(),
                5000, 16); // delayLevel 16 ≈ 30min

        // 8. 创建支付
        String payUrl = paymentService.createPayment(orderNo, priceCalc.getPayAmount(), paymentMethod);

        // 9. 组装返回
        OrderVO vo = new OrderVO();
        vo.setOrderNo(orderNo);
        vo.setStatus(OrderStatus.PENDING_PAY.getCode());
        vo.setPayAmount(priceCalc.getPayAmount());
        vo.setPaymentMethod(paymentMethod);
        vo.setPayUrl(payUrl);
        vo.setExpireTime(order.getExpireTime());

        log.info("订单创建成功: orderNo={}, userId={}, payAmount={}", orderNo, userId, priceCalc.getPayAmount());
        return vo;
    }

    @Override
    public java.util.List<OrderVO> listOrders(Long userId) {
        log.info("查询用户订单列表: userId={}", userId);
        java.util.List<Order> orders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime));
        return orders.stream().map(this::toVO).collect(java.util.stream.Collectors.toList());
    }

    private OrderVO toVO(Order order) {
        java.util.List<OrderItem> items = orderItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo()));
        OrderVO vo = new OrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setFullReductionAmount(order.getFullReductionAmount());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setCreateTime(order.getCreateTime());
        vo.setExpireTime(order.getExpireTime());
        vo.setItems(items.stream().map(item -> {
            OrderVO.OrderItemVO itemVO = new OrderVO.OrderItemVO();
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductPrice(item.getProductPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getSubtotal());
            return itemVO;
        }).collect(java.util.stream.Collectors.toList()));
        return vo;
    }
    public OrderVO getOrderDetail(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo));

        OrderVO vo = new OrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setFullReductionAmount(order.getFullReductionAmount());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setCreateTime(order.getCreateTime());
        vo.setExpireTime(order.getExpireTime());
        vo.setItems(items.stream().map(item -> {
            OrderVO.OrderItemVO itemVO = new OrderVO.OrderItemVO();
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductPrice(item.getProductPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getSubtotal());
            return itemVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getUserId, userId));
        if (order == null || !OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或状态不允许取消");
        }
        doCancelOrder(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayCallback(String orderNo, String paymentMethod) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.warn("支付回调订单不存在: orderNo={}", orderNo);
            return;
        }

        // 幂等：已支付则直接返回
        if (OrderStatus.PAID.getCode().equals(order.getStatus())) {
            log.info("订单已支付，忽略重复回调: orderNo={}", orderNo);
            return;
        }

        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            log.warn("订单状态异常，忽略回调: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }

        // 更新订单状态
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

          // 投递支付成功通知（notify 服务消费后 WebSocket 推送）
          try {
              java.util.Map<String, String> notify = new java.util.HashMap<>();
              notify.put("userId", String.valueOf(order.getUserId()));
              notify.put("message", "您的订单 " + orderNo + " 已支付成功，感谢您的购买！");
              rocketMQTemplate.convertAndSend("notify-topic", notify);
              log.info("支付成功通知已投递: orderNo={}, userId={}", orderNo, order.getUserId());
          } catch (Exception e) {
              log.warn("支付成功通知投递失败: orderNo={}, err={}", orderNo, e.getMessage());
          }

        // 清除购物车中已下单商品
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo));
        List<Long> productIds = items.stream().map(OrderItem::getProductId).toList();
        if (!productIds.isEmpty()) {                 cartItemMapper.delete(                         new LambdaQueryWrapper<CartItem>()                                 .eq(CartItem::getUserId, order.getUserId())                                 .in(CartItem::getProductId, productIds));         }

        log.info("支付成功: orderNo={}, userId={}", orderNo, order.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredOrder(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null || !OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            return; // 已支付或已取消，忽略
        }
        doCancelOrder(order);
        log.info("订单超时自动取消: orderNo={}", orderNo);
    }

    @Override
    public OrderVO retryPay(Long userId, String orderNo, String paymentMethod) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getUserId, userId));
        if (order == null || !OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或已非待支付状态");
        }

        // 更新支付方式
        order.setPaymentMethod(paymentMethod);
        orderMapper.updateById(order);

        // 重新创建支付
        String payUrl = paymentService.createPayment(orderNo, order.getPayAmount(), paymentMethod);

        OrderVO vo = new OrderVO();
        vo.setOrderNo(orderNo);
        vo.setPayUrl(payUrl);
        vo.setExpireTime(order.getExpireTime());
        return vo;
    }

    // ==================== 私有方法 ====================

    private void doCancelOrder(Order order) {
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 归还库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo()));
        for (OrderItem item : items) {
            stringRedisTemplate.opsForValue().increment("stock:" + item.getProductId(), item.getQuantity());
        }

        // 退回优惠券
        if (order.getCouponId() != null) {
            Coupon coupon = couponMapper.selectById(order.getCouponId());
            if (coupon != null && CouponStatus.USED.getCode().equals(coupon.getStatus())) {
                coupon.setStatus(CouponStatus.UNUSED.getCode());
                coupon.setUseTime(null);
                coupon.setOrderNo(null);
                couponMapper.updateById(coupon);
            }
        }
    }

    private void rollbackStock(List<CartItem> cartItems, CartItem failedItem) {
        for (CartItem item : cartItems) {
            if (item == failedItem) break;
            stringRedisTemplate.opsForValue().increment("stock:" + item.getProductId(), item.getQuantity());
        }
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return timestamp + "01" + String.format("%04d", seq);
    }
}
