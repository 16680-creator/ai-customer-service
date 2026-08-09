package com.aics.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.bo.PriceCalcBO;
import com.aics.order.client.OrderPayClient;
import com.aics.order.entity.*;
import com.aics.order.enums.CouponStatus;
import com.aics.order.enums.OrderStatus;
import com.aics.order.mapper.*;
import com.aics.order.service.OrderService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 订单服务实现（支付相关已拆分为独立 ai-cs-pay 服务）
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
    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final OrderPayClient orderPayClient;

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

        // 8. 组装返回（支付由独立支付服务完成，此处不再生成支付链接）
        OrderVO vo = new OrderVO();
        vo.setOrderNo(orderNo);
        vo.setStatus(OrderStatus.PENDING_PAY.getCode());
        vo.setPayAmount(priceCalc.getPayAmount());
        vo.setPaymentMethod(paymentMethod);
        vo.setExpireTime(order.getExpireTime());

        log.info("订单创建成功: orderNo={}, userId={}, payAmount={}", orderNo, userId, priceCalc.getPayAmount());
        return vo;
    }

    @Override
    public List<OrderVO> listOrders(Long userId) {
        log.info("查询用户订单列表: userId={}", userId);
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime));
        return orders.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public OrderVO getOrderDetail(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return toVO(order);
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
    public void confirmPay(String orderNo, String paymentMethod, BigDecimal amount, String tradeNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.warn("支付确认订单不存在: orderNo={}", orderNo);
            return;
        }

        // 幂等：已支付则直接返回
        if (OrderStatus.PAID.getCode().equals(order.getStatus())) {
            log.info("订单已支付，忽略重复确认: orderNo={}", orderNo);
            return;
        }

        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            log.warn("订单状态异常，忽略确认: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }

        // 金额校验（安全红线：回调金额必须与订单金额一致）
        if (amount != null && order.getPayAmount() != null
                && amount.compareTo(order.getPayAmount()) != 0) {
            log.error("支付金额与订单不一致: orderNo={}, callbackAmount={}, orderAmount={}",
                    orderNo, amount, order.getPayAmount());
            throw new BusinessException(ResultCode.ORDER_PAY_AMOUNT_MISMATCH);
        }

        // 更新订单状态
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 投递支付成功通知（notify 服务消费后 WebSocket 推送）
        try {
            Map<String, String> notify = new LinkedHashMap<>();
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
        if (!productIds.isEmpty()) {
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, order.getUserId())
                    .in(CartItem::getProductId, productIds));
        }

        log.info("支付确认成功: orderNo={}, userId={}, tradeNo={}", orderNo, order.getUserId(), tradeNo);
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
    @Transactional(rollbackFor = Exception.class)
    public void refundConfirm(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null || !OrderStatus.PAID.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "仅已支付订单可退款");
        }

        order.setStatus(OrderStatus.REFUNDED.getCode());
        orderMapper.updateById(order);

        // 回补库存（Redis 扣减库存体系）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo));
        for (OrderItem item : items) {
            stringRedisTemplate.opsForValue().increment("stock:" + item.getProductId(), item.getQuantity());
        }

        log.info("订单退款确认: orderNo={}", orderNo);
    }

    @Override
    public Map<String, Object> getOrderPayDetail(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("userId", order.getUserId());
        data.put("status", order.getStatus());
        data.put("payAmount", order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount());
        data.put("paymentMethod", order.getPaymentMethod());
        data.put("expireTime", order.getExpireTime() == null ? "" : order.getExpireTime().toString());
        return data;
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

        // 通知支付服务关闭渠道订单（使二维码失效）
        closePayChannel(order);

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

    /** 通知支付服务关闭渠道订单（尽力而为） */
    private void closePayChannel(Order order) {
        if (order.getPaymentMethod() == null) {
            return;
        }
        try {
            orderPayClient.closeChannel(order.getPaymentMethod(), order.getOrderNo());
        } catch (Exception e) {
            log.warn("通知支付服务关单失败: orderNo={}, err={}", order.getOrderNo(), e.getMessage());
        }
    }

    private void rollbackStock(List<CartItem> cartItems, CartItem failedItem) {
        for (CartItem item : cartItems) {
            if (item == failedItem) break;
            stringRedisTemplate.opsForValue().increment("stock:" + item.getProductId(), item.getQuantity());
        }
    }

    private OrderVO toVO(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo()));
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

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return timestamp + "01" + String.format("%04d", seq);
    }
}