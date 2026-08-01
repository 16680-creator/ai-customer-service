package com.aics.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新商品请求 DTO
 */
@Data
public class ProductUpdateDTO {

    private String name;

    private String description;

    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private Long categoryId;

    private String image;

    /** 状态：0-下架 1-上架 */
    private Integer status;
}
