package com.aics.product.controller;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.product.dto.ProductRecommendQuery;
import com.aics.product.service.ProductRecommendService;
import com.aics.product.vo.ProductRecommendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 同价位商品推荐控制器
 */
@Tag(name = "商品推荐", description = "同价位商品召回与推荐解释")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductRecommendController {

    /** limit 上限（与 ProductRecommendQuery 的 @Max(10) 一致） */
    private static final int MAX_LIMIT = 10;

    private final ProductRecommendService productRecommendService;

    /**
     * 同价位商品推荐（AI 客服售后场景：用户觉得当前商品贵/不合适时，推荐同价位替代品）
     *
     * @param query 查询参数（GET 请求参数绑定：basePrice 必填，其余可选）
     */
    @Operation(summary = "同价位商品推荐")
    @GetMapping("/recommend/price-range")
    public Result<List<ProductRecommendVO>> recommendByPriceRange(ProductRecommendQuery query) {
        // 参数校验：基准价格必填（GET 绑定无任何参数时 query 为 null）
        if (query == null || query.getBasePrice() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "基准价格不能为空");
        }
        int limit = query.getLimit();
        // limit 越界（<1 或 >10）直接拒绝（与 DTO @Max(10) 双重校验）
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "limit 必须在 1 到 10 之间");
        }
        // 委托服务层执行召回、过滤、排序与推荐解释拼接
        return Result.success(productRecommendService.recommend(query));
    }
}
