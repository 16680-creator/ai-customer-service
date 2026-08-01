package com.aics.order.service;

import com.aics.order.vo.OrderVO;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     */
    OrderVO createOrder(Long userId, List<Long> cartItemIds, Long couponId, String paymentMethod);

    /**
     * 查询订单详情
     */
    OrderVO getOrderDetail(Long userId, String orderNo);

    /**
     * 取消订单
     */
    void cancelOrder(Long userId, String orderNo);

    /**
     * 处理支付回调（幂等）
     */
    void handlePayCallback(String orderNo, String paymentMethod);

    /**
     * 超时取消订单（MQ消费者调用）
     */
    void cancelExpiredOrder(String orderNo);

    /**
     * 更换支付方式重试
     */
    OrderVO retryPay(Long userId, String orderNo, String paymentMethod);
}
