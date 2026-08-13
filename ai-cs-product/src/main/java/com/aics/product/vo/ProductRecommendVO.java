package com.aics.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 同价位商品推荐结果
 */
@Data
@Schema(description = "同价位商品推荐结果")
public class ProductRecommendVO {

    /** 商品ID */
    @Schema(description = "商品ID")
    private Long productId;

    /** 商品名称 */
    @Schema(description = "商品名称")
    private String name;

    /** 价格 */
    @Schema(description = "价格")
    private BigDecimal price;

    /** 商品分类ID */
    @Schema(description = "商品分类ID")
    private Long categoryId;

    /** 商品描述 */
    @Schema(description = "商品描述")
    private String description;

    /** 商品图片 */
    @Schema(description = "商品图片")
    private String image;

    /** 销量 */
    @Schema(description = "销量")
    private Integer sales;

    /** 推荐解释（只由真实字段拼接：价格、命中的关键词、销量） */
    @Schema(description = "推荐解释（由真实字段拼接，如：同价位 ¥199，描述包含「降噪」「蓝牙」，销量 50）")
    private String matchReason; // 由真实字段拼接的推荐解释，AI 客服可直接引用
}
