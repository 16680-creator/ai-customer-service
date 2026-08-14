package com.aics.chat.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品推荐结果（chat 侧 DTO，与 ai-cs-product 的 ProductRecommendVO 一致）
 */
@Data
public class ProductRecommendVO {

    /** 商品 ID */
    private Long productId;

    /** 商品名称 */
    private String name;

    /** 价格 */
    private BigDecimal price;

    /** 分类 ID */
    private Long categoryId;

    /** 描述 */
    private String description;

    /** 主图 URL */
    private String image;

    /** 销量 */
    private Integer sales;

    /** 推荐解释（仅由真实字段拼接） */
    private String matchReason;
}
