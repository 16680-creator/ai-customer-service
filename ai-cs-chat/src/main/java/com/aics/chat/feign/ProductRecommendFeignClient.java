package com.aics.chat.feign;

import com.aics.chat.dto.ProductRecommendVO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品服务推荐 Feign 客户端（调用 ai-cs-product 的同价位召回）
 */
@FeignClient(name = "ai-cs-product")
public interface ProductRecommendFeignClient {

    /**
     * 同价位商品召回
     *
     * @param basePrice      基准价
     * @param priceTolerance 价格容差（±百分比）
     * @param categoryId     类目（可选）
     * @param keywords       特性关键词（逗号分隔，可选）
     * @param limit          数量上限
     * @return 推荐商品列表（matchReason 仅由真实字段拼接）
     */
    @GetMapping("/product/recommend/price-range")
    Result<List<ProductRecommendVO>> recommend(
            @RequestParam("basePrice") BigDecimal basePrice,
            @RequestParam(value = "priceTolerance", required = false) Double priceTolerance,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "limit", required = false) Integer limit);
}
