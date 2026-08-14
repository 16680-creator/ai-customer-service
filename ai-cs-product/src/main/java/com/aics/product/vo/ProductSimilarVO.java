package com.aics.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 相似商品结果
 */
@Data
public class ProductSimilarVO {

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String name;

    /** 商品图片 */
    private String image;

    /** 价格 */
    private BigDecimal price;

    /** 相似度得分 */
    private double score;
}
