package com.aics.pay.service;

import com.aics.pay.entity.PayTransaction;

import java.math.BigDecimal;

/**
 * 支付流水服务
 */
public interface PayTransactionService {

    String STATUS_PENDING = "PENDING";
    String STATUS_SUCCESS = "SUCCESS";
    String STATUS_CLOSED = "CLOSED";
    String STATUS_REFUNDING = "REFUNDING";
    String STATUS_REFUNDED = "REFUNDED";

    /**
     * 创建/更新待支付流水（下单时记录）
     */
    PayTransaction createOrUpdatePending(String orderNo, Long userId, String paymentMethod, BigDecimal payAmount);

    /**
     * 标记支付成功（幂等；返回是否首次标记成功）
     */
    boolean markSuccess(String orderNo, String tradeNo, BigDecimal amount);

    /**
     * 标记已关闭（取消/超时关单）
     */
    boolean markClosed(String orderNo);

    /**
     * 标记退款中
     */
    boolean markRefunding(String orderNo);

    /**
     * 标记已退款（幂等）
     */
    boolean markRefunded(String orderNo);

    /**
     * 查询支付流水
     */
    PayTransaction getByOrderNo(String orderNo);
}