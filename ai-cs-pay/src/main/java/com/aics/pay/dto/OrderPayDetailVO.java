package com.aics.pay.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单支付信息（由订单服务返回）
 */
@Data
public class OrderPayDetailVO {
    private String orderNo;
    private Long userId;
    private String status;
    private BigDecimal payAmount;
    private String paymentMethod;
    private String expireTime;
}