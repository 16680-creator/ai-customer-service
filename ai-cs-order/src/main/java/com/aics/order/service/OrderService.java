package com.aics.order.service;

import com.aics.order.vo.OrderVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单服务接口（支付确认/退款由支付服务通过内部接口调用）
 */
public interface OrderService {

    /**
     * 创建订单（待支付；支付由独立支付服务完成）
     */
    OrderVO createOrder(Long userId, List<Long> cartItemIds, Long couponId, String paymentMethod);

    /**
     * 查询用户全部订单列表（AI 客服调用）
     */
    List<OrderVO> listOrders(Long userId);

    /**
     * 查询订单详情
     */
    OrderVO getOrderDetail(Long userId, String orderNo);

    /**
     * 取消订单（待支付）
     */
    void cancelOrder(Long userId, String orderNo);

    /**
     * 支付确认（支付服务回调；幂等 + 金额校验）
     */
    void confirmPay(String orderNo, String paymentMethod, BigDecimal amount, String tradeNo);

    /**
     * 超时取消订单（MQ 消费者调用）
     */
    void cancelExpiredOrder(String orderNo);

    /**
     * 退款确认（支付服务回调：已支付 → 已退款，回补库存）
     */
    void refundConfirm(String orderNo);

    /**
     * 订单支付信息（供支付服务调用，含状态/金额/过期时间）
     */
    Map<String, Object> getOrderPayDetail(String orderNo);
}