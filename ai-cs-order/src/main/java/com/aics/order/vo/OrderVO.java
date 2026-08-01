package com.aics.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单展示 VO
 */
@Data
public class OrderVO {

    private String orderNo;

    private String status;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private BigDecimal fullReductionAmount;

    private BigDecimal couponAmount;

    private String paymentMethod;

    private List<OrderItemVO> items;

    private LocalDateTime createTime;

    private LocalDateTime expireTime;

    private String payUrl;

    @Data
    public static class OrderItemVO {
        private Long productId;
        private String productName;
        private BigDecimal productPrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}
