package com.aics.order.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.bo.PriceCalcBO;
import com.aics.order.client.ProductClient;
import com.aics.order.entity.CartItem;
import com.aics.order.entity.Coupon;
import com.aics.order.entity.Order;
import com.aics.order.entity.OrderItem;
import com.aics.order.enums.CouponStatus;
import com.aics.order.enums.OrderStatus;
import com.aics.order.mapper.CartItemMapper;
import com.aics.order.mapper.CouponMapper;
import com.aics.order.mapper.OrderItemMapper;
import com.aics.order.mapper.OrderMapper;
import com.aics.order.service.PromotionService;
import com.aics.order.vo.OrderVO;
import io.seata.spring.annotation.GlobalTransactional;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下单事务边界（独立 Bean）。
 *
 * <p><b>为什么单独拆一个类</b>：{@code @Transactional} 与 {@code @GlobalTransactional}
 * 都依赖 Spring 代理，只有「外部调用方 → 代理 → 目标方法」这条路径才会开启事务。
 * 若放在 {@link OrderServiceImpl} 里由 {@code createOrder} 直接 {@code this.doCreateOrder(...)} 调用，
 * 会绕过代理导致事务注解失效（该问题由 {@code OrderCreateTxWiringTest} 复现并回归，
 * 详见 docs/28 故障案例 C1）。</p>
 *
 * <p>拆成独立 Bean 后，{@link OrderServiceImpl#createOrder} 注入本类并调用，
 * 代理生效，事务边界才成立；分布式锁仍然留在事务外层，避免锁持有时间等于事务时间。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateTxService {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final CouponMapper couponMapper;
    private final PromotionService promotionService;
    private final ProductClient productClient;
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${order.timeout-minutes:30}")
    private int timeoutMinutes;

    /**
     * 创建订单：由购物车条目生成订单，应用优惠券并计算应付金额。
     *
     * <p><b>事务边界</b>：本方法必须由外部 Bean 调用（经代理）才能开启本地事务与全局事务。</p>
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
    @GlobalTransactional(rollbackFor = Exception.class, name = "order-create")
    @Transactional(rollbackFor = Exception.class)
    public OrderVO doCreateOrder(Long userId, List<Long> cartItemIds, Long couponId, String paymentMethod) {
        // 1. 获取购物车商品
        List<CartItem> cartItems = cartItemMapper.selectBatchIds(cartItemIds);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ResultCode.ORDER_CART_EMPTY);
        }

        // 2. 实时扣减库存（以商品服务 DB 为权威源，原子扣减）
        //    学习要点（Seata AT）：扣库存是 Feign 调用 product 的分支事务——XID 随请求头传播，
        //    product 侧数据源代理生成 undo_log；任一环节失败（如库存不足），
        //    全局事务二阶段回滚时按 undo_log 反向补偿已提交的分支，无需手写「失败回滚」。
        //    注意：MQ 不是 Seata 资源，全局回滚时下方延迟消息已发出——但消费侧
        //    cancelExpiredOrder 查不到订单会直接返回，天然幂等安全。
        for (CartItem item : cartItems) {
            productClient.deductStock(item.getProductId(), item.getQuantity());
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

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return timestamp + "01" + String.format("%04d", seq);
    }
}
