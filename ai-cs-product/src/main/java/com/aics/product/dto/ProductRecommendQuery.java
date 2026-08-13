package com.aics.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 同价位商品推荐查询参数（GET 请求参数绑定）
 */
@Data
@Schema(description = "同价位商品推荐查询参数")
public class ProductRecommendQuery {

    /** 基准价格（用户正在查看/意向商品的价格） */
    @NotNull(message = "基准价格不能为空")
    @Schema(description = "基准价格", example = "199.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal basePrice;

    /** 价格浮动比例（默认 0.15；null、<=0 或 >=1 时按默认 0.15 处理） */
    @Schema(description = "价格浮动比例，默认 0.15（0 < tolerance < 1 生效）", example = "0.15")
    private BigDecimal priceTolerance = new BigDecimal("0.15");

    /** 商品分类ID（可选，指定后仅在该分类下召回） */
    @Schema(description = "商品分类ID（可选）", example = "4")
    private Long categoryId;

    /** 关键词（逗号分隔，可选；每个关键词需命中商品名称或描述，全部命中才保留） */
    @Schema(description = "关键词，逗号分隔（可选），如：降噪,蓝牙", example = "降噪,蓝牙")
    private String keywords;

    /** 返回条数（默认 3，最大 10） */
    @Max(value = 10, message = "limit 最大不能超过 10")
    @Schema(description = "返回条数，默认 3，最大 10", example = "3")
    private int limit = 3;
}
