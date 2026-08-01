package com.aics.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 购物车数量修改请求
 */
@Data
public class CartUpdateDTO {

    @NotNull(message = "购物车项ID不能为空")
    private Long cartItemId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
}
