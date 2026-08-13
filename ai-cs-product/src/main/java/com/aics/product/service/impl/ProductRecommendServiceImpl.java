package com.aics.product.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.product.dto.ProductRecommendQuery;
import com.aics.product.entity.Product;
import com.aics.product.mapper.ProductMapper;
import com.aics.product.service.ProductRecommendService;
import com.aics.product.vo.ProductRecommendVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 同价位商品召回与推荐解释服务实现
 *
 * <p>召回策略：以基准价 ± tolerance 构成价格区间（默认 ±15%），召回上架（status=1）
 * 且价格落在区间内的商品（deleted=0 由 @TableLogic 自动处理）；可指定分类收窄召回范围。
 * 关键词按逗号切分后逐个匹配名称/描述（忽略大小写），全部命中才保留。</p>
 *
 * <p>排序：关键词命中数降序 → |price - basePrice| 升序 → 销量降序，最后按 limit 截断。
 * matchReason 只允许拼接真实字段（价格、命中的关键词、销量），禁止编造任何字段。</p>
 */
@Service
@RequiredArgsConstructor
public class ProductRecommendServiceImpl implements ProductRecommendService {

    /** 默认价格浮动比例 */
    private static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("0.15");

    /** 默认返回条数 */
    private static final int DEFAULT_LIMIT = 3;

    private final ProductMapper productMapper;

    @Override
    public List<ProductRecommendVO> recommend(ProductRecommendQuery query) {
        if (query == null || query.getBasePrice() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "基准价格不能为空");
        }

        BigDecimal basePrice = query.getBasePrice();
        BigDecimal tolerance = resolveTolerance(query.getPriceTolerance());
        BigDecimal low = basePrice.multiply(BigDecimal.ONE.subtract(tolerance));
        BigDecimal high = basePrice.multiply(BigDecimal.ONE.add(tolerance));

        // 召回：上架 + 价格区间（deleted=0 由逻辑删除注解自动附加）；分类可选
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .between(Product::getPrice, low, high);
        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }
        List<Product> candidates = productMapper.selectList(wrapper);

        // 关键词过滤：每个关键词需命中名称或描述（忽略大小写），全部命中才保留
        List<String> keywords = parseKeywords(query.getKeywords());
        List<Product> matched = candidates.stream()
                .filter(p -> keywords.isEmpty() || keywords.stream().allMatch(k -> matches(p, k)))
                .toList();

        // 排序：关键词命中数降序 → |price-basePrice| 升序 → 销量降序
        Comparator<Product> comparator = Comparator
                .comparingInt((Product p) -> countHits(p, keywords))
                .reversed()
                .thenComparing(p -> p.getPrice().subtract(basePrice).abs())
                .thenComparing(Product::getSales, Comparator.nullsLast(Comparator.reverseOrder()));

        int limit = query.getLimit() > 0 ? query.getLimit() : DEFAULT_LIMIT;
        return matched.stream()
                .sorted(comparator)
                .limit(limit)
                .map(p -> toVO(p, keywords))
                .toList();
    }

    // ==================== 私有方法 ====================

    /**
     * 解析价格浮动比例：null、<=0、>=1 均按默认 0.15 处理，仅 (0,1) 区间内生效。
     */
    private BigDecimal resolveTolerance(BigDecimal tolerance) {
        if (tolerance == null
                || tolerance.compareTo(BigDecimal.ZERO) <= 0
                || tolerance.compareTo(BigDecimal.ONE) >= 0) {
            return DEFAULT_TOLERANCE;
        }
        return tolerance;
    }

    /**
     * 关键词按逗号切分、去空白、忽略大小写；无有效关键词返回空列表。
     */
    private List<String> parseKeywords(String keywords) {
        if (!StringUtils.hasText(keywords)) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(k -> k.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    /**
     * 关键词是否命中商品（名称或描述，忽略大小写）。
     */
    private boolean matches(Product product, String keyword) {
        return contains(product.getName(), keyword) || contains(product.getDescription(), keyword);
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    /**
     * 统计关键词命中数：每个关键词在名称、描述中各计 1 次（命中位置越多相关性越高）。
     * 注：关键词过滤要求「全部关键词命中」，按关键词是否命中的口径对幸存商品恒等，
     * 因此改为按「名称/描述命中位置」计数，使排序中的「命中数降序」具有区分度。
     */
    private int countHits(Product product, List<String> keywords) {
        int hits = 0;
        for (String keyword : keywords) {
            if (contains(product.getName(), keyword)) {
                hits++;
            }
            if (contains(product.getDescription(), keyword)) {
                hits++;
            }
        }
        return hits;
    }

    private ProductRecommendVO toVO(Product product, List<String> keywords) {
        ProductRecommendVO vo = new ProductRecommendVO();
        vo.setProductId(product.getId());
        vo.setName(product.getName());
        vo.setPrice(product.getPrice());
        vo.setCategoryId(product.getCategoryId());
        vo.setDescription(product.getDescription());
        vo.setImage(product.getImage());
        vo.setSales(product.getSales());
        vo.setMatchReason(buildMatchReason(product, keywords));
        return vo;
    }

    /**
     * 拼接推荐解释，只允许使用真实字段：
     * 例：同价位 ¥199，描述包含「降噪」「蓝牙」，销量 50
     */
    private String buildMatchReason(Product product, List<String> keywords) {
        StringBuilder sb = new StringBuilder();
        sb.append("同价位 ¥")
                .append(product.getPrice().stripTrailingZeros().toPlainString());

        // 区分关键词命中的位置：描述命中优先归入「描述包含」，其余归入「名称包含」
        List<String> descriptionHits = keywords.stream()
                .filter(k -> contains(product.getDescription(), k))
                .toList();
        List<String> nameOnlyHits = keywords.stream()
                .filter(k -> !contains(product.getDescription(), k) && contains(product.getName(), k))
                .toList();
        if (!descriptionHits.isEmpty()) {
            sb.append("，描述包含").append(wrapKeywords(descriptionHits));
        }
        if (!nameOnlyHits.isEmpty()) {
            sb.append("，名称包含").append(wrapKeywords(nameOnlyHits));
        }
        sb.append("，销量 ").append(product.getSales());
        return sb.toString();
    }

    private String wrapKeywords(List<String> hits) {
        return hits.stream()
                .map(k -> "「" + k + "」")
                .collect(Collectors.joining());
    }
}
