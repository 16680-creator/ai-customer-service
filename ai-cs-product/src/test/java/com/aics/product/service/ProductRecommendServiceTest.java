package com.aics.product.service;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.product.dto.ProductRecommendQuery;
import com.aics.product.entity.Product;
import com.aics.product.mapper.ProductMapper;
import com.aics.product.service.impl.ProductRecommendServiceImpl;
import com.aics.product.vo.ProductRecommendVO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 同价位商品召回与推荐解释 服务单元测试（TDD Red 阶段编写）
 * 采用 Mockito mock ProductMapper（与 ai-cs-product 现有测试保持一致）
 */
@ExtendWith(MockitoExtension.class)
class ProductRecommendServiceTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductRecommendServiceImpl service;

    /**
     * 纯 Mockito 环境未启动 MyBatis-Plus，这里手动注册 Product 的 TableInfo，
     * 使 LambdaQueryWrapper 能解析列名与参数（getSqlSegment / getParamNameValuePairs）。
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MybatisMapperBuilderAssistant(new MybatisConfiguration(), ""), Product.class);
    }

    // ==================== 工具方法 ====================

    private Product product(Long id, String name, String desc, String price, Long categoryId, int sales) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(new BigDecimal(price));
        p.setCategoryId(categoryId);
        p.setSales(sales);
        p.setStatus(1);
        return p;
    }

    private ProductRecommendQuery query(BigDecimal basePrice) {
        ProductRecommendQuery q = new ProductRecommendQuery();
        q.setBasePrice(basePrice);
        return q;
    }

    /** 从 wrapper 参数中提取价格区间（仅 BigDecimal 参数），断言为 [low, high] */
    private void assertPriceRange(LambdaQueryWrapper<Product> wrapper, String low, String high) {
        // MP 3.5.6 在调用 getSqlSegment() 时才物化 paramNameValuePairs，需先触发
        wrapper.getSqlSegment();
        List<BigDecimal> prices = wrapper.getParamNameValuePairs().values().stream()
                .filter(BigDecimal.class::isInstance)
                .map(v -> (BigDecimal) v)
                .toList();
        assertEquals(2, prices.size(), "价格区间应恰好有 low/high 两个参数");
        assertTrue(prices.stream().anyMatch(v -> v.compareTo(new BigDecimal(low)) == 0),
                "价格区间应包含下界 " + low + "，实际: " + prices);
        assertTrue(prices.stream().anyMatch(v -> v.compareTo(new BigDecimal(high)) == 0),
                "价格区间应包含上界 " + high + "，实际: " + prices);
    }

    // ==================== 价格区间过滤 ====================

    @Test
    @DisplayName("同价位推荐 - 价格区间按基准价±15%过滤（status=1 + price between）")
    void recommend_shouldBuildPriceRangeWrapper() {
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "耳机A", "降噪", "170.00", 4L, 10),
                product(2L, "耳机B", "降噪", "230.00", 4L, 20)));

        ProductRecommendQuery q = query(new BigDecimal("200.00"));
        List<ProductRecommendVO> result = service.recommend(q);

        assertEquals(2, result.size());

        ArgumentCaptor<LambdaQueryWrapper<Product>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectList(captor.capture());
        LambdaQueryWrapper<Product> wrapper = captor.getValue();

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("status"), "应过滤上架商品 status=1，实际 SQL: " + sql);
        assertTrue(sql.contains("price") && sql.contains("BETWEEN"), "应按价格区间过滤，实际 SQL: " + sql);
        // 边界 ±15%：200 * (1-0.15) = 170，200 * (1+0.15) = 230
        assertPriceRange(wrapper, "170", "230");
        // status=1 参数
        assertTrue(wrapper.getParamNameValuePairs().containsValue(1), "status 参数应为 1");
    }

    @Test
    @DisplayName("同价位推荐 - 价格边界 ±15% 计算正确（199 → [169.15, 228.85]）")
    void recommend_shouldComputeBoundaryFor199() {
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.recommend(query(new BigDecimal("199.00")));

        ArgumentCaptor<LambdaQueryWrapper<Product>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectList(captor.capture());
        assertPriceRange(captor.getValue(), "169.15", "228.85");
    }

    // ==================== 关键词过滤 ====================

    @Test
    @DisplayName("同价位推荐 - 关键词过滤：描述/名称命中保留，任一关键词不命中剔除")
    void recommend_shouldFilterByKeywords() {
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "无线蓝牙耳机", "高品质降噪", "199.00", 4L, 100),   // 名称+描述均命中
                product(2L, "有线键盘", "降噪蓝牙", "199.00", 4L, 90),        // 仅描述命中
                product(3L, "蓝牙音箱", "便携", "199.00", 4L, 80),            // 缺「降噪」→ 剔除
                product(4L, "充电宝", "大容量", "199.00", 4L, 70)));           // 全不命中 → 剔除

        ProductRecommendQuery q = query(new BigDecimal("199.00"));
        q.setKeywords("降噪, 蓝牙");
        q.setLimit(10);

        List<ProductRecommendVO> result = service.recommend(q);

        assertEquals(2, result.size());
        assertEquals(List.of(1L, 2L), result.stream().map(ProductRecommendVO::getProductId).toList());
    }

    // ==================== 分类过滤 ====================

    @Test
    @DisplayName("同价位推荐 - 指定分类时按分类过滤")
    void recommend_shouldFilterByCategory() {
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "耳机A", "降噪", "199.00", 4L, 10)));

        ProductRecommendQuery q = query(new BigDecimal("199.00"));
        q.setCategoryId(4L);
        service.recommend(q);

        ArgumentCaptor<LambdaQueryWrapper<Product>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectList(captor.capture());
        LambdaQueryWrapper<Product> wrapper = captor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("category_id"), "应按分类过滤，实际 SQL: " + wrapper.getSqlSegment());
        assertTrue(wrapper.getParamNameValuePairs().containsValue(4L), "categoryId 参数应为 4");
    }

    // ==================== 排序 ====================

    @Test
    @DisplayName("同价位推荐 - 排序：关键词命中数降序 → 价格差升序 → 销量降序")
    void recommend_shouldSortByHitsThenPriceDiffThenSales() {
        // 关键词过滤要求全部命中，因此命中数按「名称/描述命中位置」计数：
        // 1号名称+描述均命中（3 处）；2/3/4号各 2 处
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "降噪蓝牙耳机", "降噪", "205.00", 4L, 10),   // 命中3，价差5
                product(2L, "蓝牙耳机", "降噪", "195.00", 4L, 200),    // 命中2，价差5，销量200
                product(3L, "蓝牙键盘", "降噪", "199.00", 4L, 300),    // 命中2，价差1，销量300
                product(4L, "蓝牙音箱", "降噪便携", "205.00", 4L, 100))); // 命中2，价差5，销量100

        ProductRecommendQuery q = query(new BigDecimal("200.00"));
        q.setKeywords("蓝牙,降噪");
        q.setLimit(10);

        List<ProductRecommendVO> result = service.recommend(q);

        assertEquals(List.of(1L, 3L, 2L, 4L),
                result.stream().map(ProductRecommendVO::getProductId).toList());
    }

    @Test
    @DisplayName("同价位推荐 - 无关键词时按价格差升序再按销量降序")
    void recommend_shouldSortByPriceDiffThenSales() {
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "商品A", "描述", "180.00", 4L, 50),   // 价差20
                product(2L, "商品B", "描述", "195.00", 4L, 10),   // 价差5，销量10
                product(3L, "商品C", "描述", "195.00", 4L, 90),   // 价差5，销量90
                product(4L, "商品D", "描述", "200.00", 4L, 500))); // 价差0

        ProductRecommendQuery q = query(new BigDecimal("200.00"));
        q.setLimit(10);

        List<ProductRecommendVO> result = service.recommend(q);

        assertEquals(List.of(4L, 3L, 2L, 1L),
                result.stream().map(ProductRecommendVO::getProductId).toList());
    }

    // ==================== 空结果 / limit 截断 ====================

    @Test
    @DisplayName("同价位推荐 - 无结果返回空列表")
    void recommend_shouldReturnEmptyList_whenNoMatch() {
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<ProductRecommendVO> result = service.recommend(query(new BigDecimal("199.00")));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("同价位推荐 - 按 limit 截断结果")
    void recommend_shouldTruncateByLimit() {
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "商品A", "描述", "199.00", 4L, 100),
                product(2L, "商品B", "描述", "199.00", 4L, 90),
                product(3L, "商品C", "描述", "199.00", 4L, 80),
                product(4L, "商品D", "描述", "199.00", 4L, 70),
                product(5L, "商品E", "描述", "199.00", 4L, 60)));

        ProductRecommendQuery q = query(new BigDecimal("199.00"));
        q.setLimit(3);

        List<ProductRecommendVO> result = service.recommend(q);

        assertEquals(3, result.size());
        assertEquals(List.of(1L, 2L, 3L), result.stream().map(ProductRecommendVO::getProductId).toList());
    }

    // ==================== matchReason 真实性 ====================

    @Test
    @DisplayName("同价位推荐 - matchReason 只由真实字段拼接（价格/关键词/销量）")
    void recommend_matchReason_shouldUseOnlyRealFields() {
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "无线蓝牙耳机", "高品质降噪蓝牙耳机", "199.00", 4L, 50)));

        ProductRecommendQuery q = query(new BigDecimal("199.00"));
        q.setKeywords("降噪,蓝牙");

        ProductRecommendVO vo = service.recommend(q).get(0);

        String reason = vo.getMatchReason();
        assertNotNull(reason);
        assertTrue(reason.contains("¥199"), "应包含真实价格，实际: " + reason);
        assertTrue(reason.contains("「降噪」"), "应包含真实命中的关键词，实际: " + reason);
        assertTrue(reason.contains("「蓝牙」"), "应包含真实命中的关键词，实际: " + reason);
        assertTrue(reason.contains("销量 50"), "应包含真实销量，实际: " + reason);
        // 禁止编造字段/内容
        assertFalse(reason.contains("库存"), "matchReason 不应包含编造内容: " + reason);
        assertFalse(reason.contains("分类"), "matchReason 不应包含编造内容: " + reason);
        assertFalse(reason.contains("评分"), "matchReason 不应包含编造内容: " + reason);
    }

    // ==================== tolerance 默认值与非法值回退 ====================

    @Test
    @DisplayName("同价位推荐 - tolerance 为 null/<=0/>=1 时回退默认15%，合法值生效")
    void recommend_shouldFallbackInvalidTolerance() {
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());

        ProductRecommendQuery q = query(new BigDecimal("200.00"));
        q.setPriceTolerance(null);                     // null → 默认 15%
        service.recommend(q);
        q.setPriceTolerance(BigDecimal.ZERO);          // 0 → 默认
        service.recommend(q);
        q.setPriceTolerance(new BigDecimal("-0.50"));  // 负数 → 默认
        service.recommend(q);
        q.setPriceTolerance(new BigDecimal("1.00"));   // 1 → 默认
        service.recommend(q);
        q.setPriceTolerance(new BigDecimal("1.50"));   // >1 → 默认
        service.recommend(q);
        q.setPriceTolerance(new BigDecimal("0.10"));   // 合法 → 10%
        service.recommend(q);

        ArgumentCaptor<LambdaQueryWrapper<Product>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper, org.mockito.Mockito.times(6)).selectList(captor.capture());
        List<LambdaQueryWrapper<Product>> wrappers = captor.getAllValues();
        // 前 5 次均为默认 ±15%：[170, 230]
        for (int i = 0; i < 5; i++) {
            assertPriceRange(wrappers.get(i), "170", "230");
        }
        // 第 6 次为 ±10%：[180, 220]
        assertPriceRange(wrappers.get(5), "180", "220");
    }

    // ==================== 参数防御 ====================

    @Test
    @DisplayName("同价位推荐 - limit 非正数时回退默认 3")
    void recommend_shouldFallbackLimit_whenNonPositive() {
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(1L, "商品A", "描述", "199.00", 4L, 100),
                product(2L, "商品B", "描述", "199.00", 4L, 90),
                product(3L, "商品C", "描述", "199.00", 4L, 80),
                product(4L, "商品D", "描述", "199.00", 4L, 70),
                product(5L, "商品E", "描述", "199.00", 4L, 60)));

        ProductRecommendQuery q = query(new BigDecimal("199.00"));
        q.setLimit(0);

        List<ProductRecommendVO> result = service.recommend(q);

        assertEquals(3, result.size());
        assertEquals(List.of(1L, 2L, 3L), result.stream().map(ProductRecommendVO::getProductId).toList());
    }

    @Test
    @DisplayName("同价位推荐 - 描述为 null 时关键词仅命中名称也可保留")
    void recommend_shouldMatchNameOnly_whenDescriptionNull() {
        Product p = new Product();
        p.setId(1L);
        p.setName("无线蓝牙耳机");
        p.setDescription(null);
        p.setPrice(new BigDecimal("199.00"));
        p.setCategoryId(4L);
        p.setSales(50);
        p.setStatus(1);
        when(productMapper.selectList(any())).thenReturn(List.of(p));

        ProductRecommendQuery q = query(new BigDecimal("199.00"));
        q.setKeywords("蓝牙");

        List<ProductRecommendVO> result = service.recommend(q);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getProductId());
        assertTrue(result.get(0).getMatchReason().contains("名称包含「蓝牙」"),
                "描述为空时应归入名称包含，实际: " + result.get(0).getMatchReason());
        assertFalse(result.get(0).getMatchReason().contains("描述包含"),
                "描述为空时不应出现「描述包含」，实际: " + result.get(0).getMatchReason());
    }

    @Test
    @DisplayName("同价位推荐 - basePrice 为空抛 BAD_REQUEST")
    void recommend_missingBasePrice_shouldThrow() {
        ProductRecommendQuery q = new ProductRecommendQuery();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recommend(q));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("同价位推荐 - query 为 null 抛 BAD_REQUEST")
    void recommend_nullQuery_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.recommend(null));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }
}
