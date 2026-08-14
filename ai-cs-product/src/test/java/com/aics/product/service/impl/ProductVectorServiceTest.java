package com.aics.product.service.impl;

import com.aics.common.ai.embedding.HashEmbeddingModel;
import com.aics.product.entity.Product;
import com.aics.product.mapper.ProductMapper;
import com.aics.product.service.ImageDescriptionService;
import com.aics.product.vo.ProductSimilarVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 商品向量服务测试（真实 SimpleVectorStore + 本地 Embedding）
 */
@ExtendWith(MockitoExtension.class)
class ProductVectorServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ImageDescriptionService imageDescriptionService;

    private ProductVectorService productVectorService;

    @BeforeEach
    void setUp() {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(new HashEmbeddingModel()).build();
        productVectorService = new ProductVectorService(vectorStore, productMapper, imageDescriptionService);
    }

    private Product product(Long id, String name, String description) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal("99.00"));
        return product;
    }

    @Test
    @DisplayName("索引后可检索到相似商品")
    void searchByText_shouldReturnSimilarProducts() {
        productVectorService.indexProduct(product(1L, "无线蓝牙耳机", "高品质降噪，续航持久"));
        productVectorService.indexProduct(product(2L, "有线机械键盘", "青轴，游戏办公两用"));
        productVectorService.indexProduct(product(3L, "头戴式降噪耳机", "包耳式，主动降噪"));

        when(productMapper.selectById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return product(id, id == 1L ? "无线蓝牙耳机" : id == 3L ? "头戴式降噪耳机" : "有线机械键盘",
                    id == 1L ? "高品质降噪，续航持久" : id == 3L ? "包耳式，主动降噪" : "青轴，游戏办公两用");
        });

        List<ProductSimilarVO> result = productVectorService.searchByText("降噪耳机", 3);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(vo -> vo.getProductId().equals(1L) || vo.getProductId().equals(3L)),
                "应召回耳机类商品");
        assertTrue(result.stream().anyMatch(vo -> vo.getScore() > 0), "应返回相似度得分");
    }

    @Test
    @DisplayName("按商品检索相似商品应排除自身")
    void searchByProduct_shouldExcludeSelf() {
        productVectorService.indexProduct(product(1L, "无线蓝牙耳机", "高品质降噪"));
        productVectorService.indexProduct(product(2L, "蓝牙音箱", "无线便携"));
        productVectorService.indexProduct(product(3L, "数据线", "type-c 快充"));

        when(productMapper.selectById(1L)).thenReturn(product(1L, "无线蓝牙耳机", "高品质降噪"));
        when(productMapper.selectById(2L)).thenReturn(product(2L, "蓝牙音箱", "无线便携"));
        when(productMapper.selectById(3L)).thenReturn(product(3L, "数据线", "type-c 快充"));

        List<ProductSimilarVO> result = productVectorService.searchByProduct(1L, 5);

        assertTrue(result.stream().noneMatch(vo -> vo.getProductId().equals(1L)), "结果不应包含自身");
        assertTrue(result.stream().anyMatch(vo -> vo.getProductId().equals(2L)), "应召回蓝牙音箱");
    }

    @Test
    @DisplayName("删除索引后商品不再被召回")
    void removeProduct_shouldExcludeFromSearch() {
        productVectorService.indexProduct(product(1L, "无线蓝牙耳机", "高品质降噪"));
        productVectorService.removeProduct(1L);

        List<ProductSimilarVO> result = productVectorService.searchByText("降噪耳机", 5);

        assertTrue(result.isEmpty(), "删除索引后不应召回该商品");
    }

    @Test
    @DisplayName("检索文本为空时应抛出业务异常")
    void searchByText_blankShouldThrow() {
        assertThrows(com.aics.common.exception.BusinessException.class,
                () -> productVectorService.searchByText("  ", 5));
    }
}
