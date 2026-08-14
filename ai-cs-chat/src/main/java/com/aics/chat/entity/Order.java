package com.aics.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /** 订单ID */
    private String orderId;

    /** 用户ID */
    private String userId;

    /** 订单状态: PENDING/PAID/SHIPPED/DELIVERED/COMPLETED/CANCELLED */
    private String status;

    /** 订单状态描述 */
    private String statusDesc;

    /** 商品列表 */
    private List<OrderItem> items;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 收货地址 */
    private String shippingAddress;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 快递公司 */
    private String expressCompany;

    /** 快递单号 */
    private String expressNo;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 备注 */
    private String remark;

    /**
     * 订单商品项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        /** 商品ID */
        private String productId;
        /** 商品名称 */
        private String productName;
        /** 商品规格 */
        private String spec;
        /** 单价 */
        private BigDecimal price;
        /** 数量 */
        private Integer quantity;
        /** 小计 */
        private BigDecimal subtotal;
    }
}
