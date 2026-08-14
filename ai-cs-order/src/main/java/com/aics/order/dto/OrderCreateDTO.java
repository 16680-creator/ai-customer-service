package com.aics.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 下单请求
 */
@Data
public class OrderCreateDTO {

    @NotEmpty(message = "请选择要购买的商品")
    private List<Long> cartItemIds;

    /** 优惠券ID（可选） */
    private Long couponId;

    @NotNull(message = "请选择支付方式")
    private String paymentMethod;
}
