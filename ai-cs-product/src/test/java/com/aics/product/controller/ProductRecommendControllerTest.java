package com.aics.product.controller;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.product.dto.ProductRecommendQuery;
import com.aics.product.service.ProductRecommendService;
import com.aics.product.vo.ProductRecommendVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 同价位商品推荐 控制器单元测试（TDD Red 阶段编写）
 * 采用直接调用 Controller 方法的方式（与 ai-cs-product 现有测试保持一致）
 */
@ExtendWith(MockitoExtension.class)
class ProductRecommendControllerTest {

    @Mock
    private ProductRecommendService productRecommendService;

    @InjectMocks
    private ProductRecommendController controller;

    private ProductRecommendVO sampleVO;

    @BeforeEach
    void setUp() {
        sampleVO = new ProductRecommendVO();
        sampleVO.setProductId(1001L);
        sampleVO.setName("无线蓝牙耳机");
        sampleVO.setPrice(new BigDecimal("199.00"));
        sampleVO.setCategoryId(4L);
        sampleVO.setDescription("高品质降噪蓝牙耳机，续航30小时");
        sampleVO.setImage("https://img.example.com/earphone.jpg");
        sampleVO.setSales(50);
        sampleVO.setMatchReason("同价位 ¥199，描述包含「降噪」「蓝牙」，销量 50");
    }

    @Test
    @DisplayName("同价位推荐 - 参数正确传递并返回 Result 结构")
    void recommend_shouldPassQueryAndReturnResult() {
        ProductRecommendQuery query = new ProductRecommendQuery();
        query.setBasePrice(new BigDecimal("199.00"));
        query.setPriceTolerance(new BigDecimal("0.15"));
        query.setCategoryId(4L);
        query.setKeywords("降噪,蓝牙");
        query.setLimit(5);

        when(productRecommendService.recommend(any(ProductRecommendQuery.class)))
                .thenReturn(List.of(sampleVO));

        Result<List<ProductRecommendVO>> result = controller.recommendByPriceRange(query);

        assertEquals(200, result.getCode());
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("无线蓝牙耳机", result.getData().get(0).getName());
        assertEquals(new BigDecimal("199.00"), result.getData().get(0).getPrice());
        assertNotNull(result.getData().get(0).getMatchReason());

        verify(productRecommendService).recommend(argThat(q ->
                q.getBasePrice().compareTo(new BigDecimal("199.00")) == 0 &&
                q.getCategoryId().equals(4L) &&
                q.getLimit() == 5));
    }

    @Test
    @DisplayName("同价位推荐 - basePrice 为空抛 BAD_REQUEST")
    void recommend_missingBasePrice_shouldThrowBadRequest() {
        ProductRecommendQuery query = new ProductRecommendQuery();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.recommendByPriceRange(query));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(productRecommendService, never()).recommend(any());
    }

    @Test
    @DisplayName("同价位推荐 - query 为 null 抛 BAD_REQUEST")
    void recommend_nullQuery_shouldThrowBadRequest() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.recommendByPriceRange(null));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(productRecommendService, never()).recommend(any());
    }

    @Test
    @DisplayName("同价位推荐 - limit 超出 [1,10] 抛 BAD_REQUEST")
    void recommend_invalidLimit_shouldThrowBadRequest() {
        ProductRecommendQuery query = new ProductRecommendQuery();
        query.setBasePrice(new BigDecimal("199.00"));

        query.setLimit(11);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.recommendByPriceRange(query));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());

        query.setLimit(0);
        assertThrows(BusinessException.class, () -> controller.recommendByPriceRange(query));

        verify(productRecommendService, never()).recommend(any());
    }
}
