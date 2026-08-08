package com.aics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 加入购物车请求
 */
@Data
@Schema(description = "加入购物车请求")
public class CartAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID", example = "1001")
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Schema(description = "购买数量", example = "1")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 99, message = "单次最多购买99件")
    private Integer quantity;
}