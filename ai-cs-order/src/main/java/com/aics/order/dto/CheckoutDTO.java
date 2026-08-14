package com.aics.order.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 结算确认请求
 */
@Data
public class CheckoutDTO {

    @NotEmpty(message = "请选择要结算的商品")
    private List<Long> cartItemIds;
}
