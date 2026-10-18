package com.aics.order.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.client.PayClient;
import com.aics.order.client.ProductClient;
import com.aics.order.entity.*;
import com.aics.order.enums.CouponStatus;
import com.aics.order.enums.OrderStatus;
import com.aics.order.event.OrderPaidEvent;
import com.aics.order.mapper.*;
import com.aics.order.statemachine.OrderEvent;
import com.aics.order.statemachine.OrderStateTransitionService;
import com.aics.order.service.OrderService;
import com.aics.order.vo.OrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.aics.order.lock.OrderCreateLockService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ProductClient productClient;
    private final OrderCreateLockService orderCreateLockService;
    private final OrderCreateTxService orderCreateTxService;
    private final PayClient payClient;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderStateTransitionService orderStateTransitionService;

    @Override
    /**
     * 创建订单（入口）：分布式锁防重 + 委托 {@link OrderCreateTxService#doCreateOrder} 执行全局事务。
     *
     * <p>锁在全局事务外层：同一用户的并发下单请求快速失败，拿到锁才进入
     * {@code @GlobalTransactional} 边界，避免锁内空占事务资源。</p>
     */
    public OrderVO createOrder(Long userId, List<Long> cartItemIds, Long couponId, String paymentMethod) {
        return orderCreateLockService.withCreateLock(userId,
                () -> orderCreateTxService.doCreateOrder(userId, cartItemIds, couponId, paymentMethod));
    }

    @Override
    /**
     * 按用户查询订单列表（AI 客服"我的订单"工具的服务端）。
     *
     * @param userId 用户 ID（由网关 X-User-Id 透传，保证只能查自己的订单——数据权限）
     * @return 该用户订单列表，按创建时间倒序
     */
    public List<OrderVO> listOrders(Long userId) {
        log.info("查询用户订单列表: userId={}", userId);
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime));
        return orders.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    /**
     * 查询订单详情（AI 客服"按订单号查单"工具的服务端）。
     *
     * @param userId  用户 ID（校验订单归属，防止越权查他人订单）
     * @param orderNo 订单号
     * @return 订单详情（含商品、金额、物流信息）
     */
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
    /**
     * 取消订单：仅未发货/未支付订单可取消，取消后释放库存。
     *
     * <p><b>学习要点</b>：取消是状态机迁移 + 库存补偿，必须保证幂等（重复取消不报错）。</p>
     */
    public void cancelOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或状态不允许取消");
        }
        doCancelOrder(order, OrderEvent.CANCEL);
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

        // 状态机是迁移唯一入口：PAID 回调仍幂等，其他非法态忽略。
        if (OrderStatus.PAID.getCode().equals(order.getStatus())) {
            log.info("订单已支付，忽略重复确认: orderNo={}", orderNo);
            return;
        }
        try {
            order.setStatus(orderStateTransitionService.transit(order.getStatus(), OrderEvent.PAY).getCode());
        } catch (BusinessException e) {
            log.warn("订单状态不允许支付确认，忽略: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }

        // 金额校验（安全红线：回调金额必须与订单金额一致）
        if (amount != null && order.getPayAmount() != null
                && amount.compareTo(order.getPayAmount()) != 0) {
            log.error("支付金额与订单不一致: orderNo={}, callbackAmount={}, orderAmount={}",
                    orderNo, amount, order.getPayAmount());
            throw new BusinessException(ResultCode.ORDER_PAY_AMOUNT_MISMATCH);
        }

        // 更新订单状态（前面已由状态机计算 PENDING_PAY -> PAID）
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 事务内只发布领域事实；通知 MQ 投递由 OrderPaidEventListener 在 AFTER_COMMIT 执行。
        // 这样订单事务若在后续「清购物车」等步骤回滚，用户不会收到错误的支付成功通知。
        eventPublisher.publishEvent(new OrderPaidEvent(orderNo, order.getUserId()));

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
        doCancelOrder(order, OrderEvent.TIMEOUT);
        log.info("订单超时自动取消: orderNo={}", orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundConfirm(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "仅已支付订单可退款");
        }

        // 退款是两段迁移：先校验 PAID -> REFUNDING，库存回补成功后再落 REFUNDED。
        order.setStatus(orderStateTransitionService.transit(order.getStatus(), OrderEvent.REFUND_REQUEST).getCode());
        orderMapper.updateById(order);

        // 回补库存（实时回补商品服务 DB 库存）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo));
        for (OrderItem item : items) {
            productClient.restoreStock(item.getProductId(), item.getQuantity());
        }
        order.setStatus(orderStateTransitionService.transit(order.getStatus(), OrderEvent.REFUND_SUCCESS).getCode());
        orderMapper.updateById(order);

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

    private void doCancelOrder(Order order, OrderEvent event) {
        order.setStatus(orderStateTransitionService.transit(order.getStatus(), event).getCode());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 归还库存（实时回补商品服务 DB 库存）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo()));
        for (OrderItem item : items) {
            productClient.restoreStock(item.getProductId(), item.getQuantity());
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

    /**
     * 通知支付服务关闭渠道订单（尽力而为）：关单失败不阻断取消主流程，
     * 仅告警日志，渠道订单残留由支付服务侧超时兜底。
     */
    private void closePayChannel(Order order) {
        if (order.getPaymentMethod() == null) {
            return;
        }
        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("orderNo", order.getOrderNo());
            body.put("paymentMethod", order.getPaymentMethod());
            payClient.closeOrder(body);
            log.info("已通知支付服务关单: orderNo={}, method={}", order.getOrderNo(), order.getPaymentMethod());
        } catch (Exception e) {
            log.warn("通知支付服务关单失败: orderNo={}, err={}", order.getOrderNo(), e.getMessage());
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

}