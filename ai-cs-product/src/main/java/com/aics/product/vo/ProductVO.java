package com.aics.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息 VO
 */
@Data
public class ProductVO {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private Long categoryId;

    private String categoryName;

    private String image;

    private Integer status;

    private Integer sales;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
