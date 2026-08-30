package com.aics.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.bo.PriceCalcBO;
import com.aics.order.client.OrderPayClient;
import com.aics.order.client.ProductStockClient;
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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.aics.order.lock.OrderCreateLockService;
import io.seata.spring.annotation.GlobalTransactional;
import java.util.ArrayList;
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
    private final ProductStockClient productStockClient;
    private final OrderCreateLockService orderCreateLockService;
    private final RocketMQTemplate rocketMQTemplate;
    private final OrderPayClient orderPayClient;

    @Value("${order.timeout-minutes:30}")
    private int timeoutMinutes;

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    @Override
    /**
     * 创建订单（入口）：分布式锁防重 + 委托 {@link #doCreateOrder} 执行全局事务。
     *
     * <p>锁在全局事务外层：同一用户的并发下单请求快速失败，拿到锁才进入
     * {@code @GlobalTransactional} 边界，避免锁内空占事务资源。</p>
     */
    public OrderVO createOrder(Long userId, List<Long> cartItemIds, Long couponId, String paymentMethod) {
        return orderCreateLockService.withCreateLock(userId,
                () -> doCreateOrder(userId, cartItemIds, couponId, paymentMethod));
    }

    @GlobalTransactional(rollbackFor = Exception.class, name = "order-create")
    @Transactional(rollbackFor = Exception.class)
    /**
     * 创建订单：由购物车条目生成订单，应用优惠券并计算应付金额。
     *
     * <p><b>学习要点</b>：下单是典型"多步骤写操作"——校验购物车 → 计算商品合计 →
     * 应用优惠/满减 → 生成订单与订单项 → 清空已下单购物车。AI 客服的"帮我下单"工具即调用本方法。</p>
     *
     * @param userId        用户 ID
     * @param cartItemIds   选中的购物车条目 ID
     * @param couponId      优惠券 ID（可空）
     * @param paymentMethod 支付方式（见 ai-cs-common PaymentMethod）
     * @return 创建后的订单 VO
     */
    public OrderVO doCreateOrder(Long userId, List<Long> cartItemIds, Long couponId, String paymentMethod) {
        // 1. 获取购物车商品
        List<CartItem> cartItems = cartItemMapper.selectBatchIds(cartItemIds);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ResultCode.ORDER_CART_EMPTY);
        }

        // 2. 实时扣减库存（以商品服务 DB 为权威源，原子扣减）
        //    学习要点（Seata AT）：扣库存是 Feign 调用 product 的分支事务——XID 随请求头传播，
        //    product 侧数据源代理生成 undo_log；任一环节失败（如库存不足），
        //    全局事务二阶段回滚时按 undo_log 反向补偿已提交的分支，无需手写「失败回补」。
        //    注意：MQ 不是 Seata 资源，全局回滚时下方延迟消息已发出——但消息消费侧
        //    cancelExpiredOrder 查不到订单会直接返回，天然幂等安全。
        for (CartItem item : cartItems) {
            productStockClient.deductStock(item.getProductId(), item.getQuantity());
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

        // 回补库存（实时回补商品服务 DB 库存）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo));
        for (OrderItem item : items) {
            productStockClient.restoreStock(item.getProductId(), item.getQuantity());
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

        // 归还库存（实时回补商品服务 DB 库存）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo()));
        for (OrderItem item : items) {
            productStockClient.restoreStock(item.getProductId(), item.getQuantity());
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