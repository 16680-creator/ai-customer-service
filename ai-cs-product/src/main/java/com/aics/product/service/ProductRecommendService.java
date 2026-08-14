package com.aics.product.service;

import com.aics.product.dto.ProductRecommendQuery;
import com.aics.product.vo.ProductRecommendVO;

import java.util.List;

/**
 * 同价位商品召回与推荐解释服务
 */
public interface ProductRecommendService { // 同价位商品召回 + 关键词过滤 + 排序 + 推荐解释拼接的对外门面

    /**
     * 同价位商品推荐：
     * <ul>
     *   <li>价格区间 = [basePrice*(1-tolerance), basePrice*(1+tolerance)]（tolerance 默认 0.15）</li>
     *   <li>召回：上架商品、价格区间内、分类匹配（可选）</li>
     *   <li>关键词过滤：每个关键词需命中名称或描述（忽略大小写），全部命中才保留</li>
     *   <li>排序：关键词命中数降序 → |price-basePrice| 升序 → 销量降序，截断 limit</li>
     *   <li>matchReason 只由真实字段拼接</li>
     * </ul>
     *
     * @param query 查询参数
     * @return 推荐结果（无匹配时返回空列表）
     */
    List<ProductRecommendVO> recommend(ProductRecommendQuery query);
}
