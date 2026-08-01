package com.aics.product.controller;

import com.aics.common.result.Result;
import com.aics.product.dto.ProductCreateDTO;
import com.aics.product.dto.ProductUpdateDTO;
import com.aics.product.entity.ProductCategory;
import com.aics.product.service.ProductService;
import com.aics.product.vo.ProductVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 商品控制器单元测试（TDD Red 阶段编写）
 * 采用直接调用 Controller 方法的方式（与 ai-cs-order 保持一致）
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductVO sampleProductVO;

    @BeforeEach
    void setUp() {
        sampleProductVO = new ProductVO();
        sampleProductVO.setId(1L);
        sampleProductVO.setName("无线蓝牙耳机");
        sampleProductVO.setDescription("高品质降噪蓝牙耳机");
        sampleProductVO.setPrice(new BigDecimal("199.00"));
        sampleProductVO.setStock(100);
        sampleProductVO.setCategoryId(10L);
        sampleProductVO.setCategoryName("数码配件");
        sampleProductVO.setImage("https://img.example.com/earphone.jpg");
        sampleProductVO.setStatus(1);
        sampleProductVO.setSales(50);
        sampleProductVO.setCreateTime(LocalDateTime.now());
    }

    // ==================== 创建商品 ====================

    @Test
    @DisplayName("创建商品 - 返回成功及商品数据")
    void createProduct_shouldReturnSuccess() {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("无线蓝牙耳机");
        dto.setPrice(new BigDecimal("199.00"));
        dto.setStock(100);
        dto.setCategoryId(10L);

        when(productService.createProduct(any(ProductCreateDTO.class))).thenReturn(sampleProductVO);

        Result<ProductVO> result = productController.createProduct(dto);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("无线蓝牙耳机", result.getData().getName());
        verify(productService).createProduct(dto);
    }

    // ==================== 商品列表 ====================

    @Test
    @DisplayName("分页查询商品列表 - 返回分页数据")
    void listProducts_shouldReturnPagedResult() {
        Page<ProductVO> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleProductVO));
        page.setTotal(1);

        when(productService.getProductList(eq(1), eq(10), isNull(), isNull(), isNull()))
                .thenReturn(page);

        Result<IPage<ProductVO>> result = productController.listProducts(1, 10, null, null, null);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
        assertEquals("无线蓝牙耳机", result.getData().getRecords().get(0).getName());
    }

    // ==================== 商品详情 ====================

    @Test
    @DisplayName("查询商品详情 - 返回完整商品信息")
    void getProductDetail_shouldReturnProduct() {
        when(productService.getProductDetail(1L)).thenReturn(sampleProductVO);

        Result<ProductVO> result = productController.getProductDetail(1L);

        assertEquals(200, result.getCode());
        assertEquals(1L, result.getData().getId());
        assertEquals("无线蓝牙耳机", result.getData().getName());
        assertEquals(new BigDecimal("199.00"), result.getData().getPrice());
        assertEquals(100, result.getData().getStock());
    }

    // ==================== 更新商品 ====================

    @Test
    @DisplayName("更新商品 - 返回更新后的数据")
    void updateProduct_shouldReturnUpdated() {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setName("升级版蓝牙耳机");
        dto.setPrice(new BigDecimal("259.00"));

        ProductVO updatedVO = new ProductVO();
        updatedVO.setId(1L);
        updatedVO.setName("升级版蓝牙耳机");
        updatedVO.setPrice(new BigDecimal("259.00"));

        when(productService.updateProduct(eq(1L), any(ProductUpdateDTO.class))).thenReturn(updatedVO);

        Result<ProductVO> result = productController.updateProduct(1L, dto);

        assertEquals(200, result.getCode());
        assertEquals("升级版蓝牙耳机", result.getData().getName());
        assertEquals(new BigDecimal("259.00"), result.getData().getPrice());
    }

    // ==================== 删除商品 ====================

    @Test
    @DisplayName("删除商品 - 返回成功")
    void deleteProduct_shouldReturnSuccess() {
        doNothing().when(productService).deleteProduct(1L);

        Result<Void> result = productController.deleteProduct(1L);

        assertEquals(200, result.getCode());
        verify(productService).deleteProduct(1L);
    }

    // ==================== 库存管理 ====================

    @Test
    @DisplayName("扣减库存 - 返回成功")
    void deductStock_shouldReturnSuccess() {
        doNothing().when(productService).deductStock(1L, 5);

        Result<Void> result = productController.deductStock(1L, 5);

        assertEquals(200, result.getCode());
        verify(productService).deductStock(1L, 5);
    }

    @Test
    @DisplayName("恢复库存 - 返回成功")
    void restoreStock_shouldReturnSuccess() {
        doNothing().when(productService).restoreStock(1L, 10);

        Result<Void> result = productController.restoreStock(1L, 10);

        assertEquals(200, result.getCode());
        verify(productService).restoreStock(1L, 10);
    }

    // ==================== 分类管理 ====================

    @Test
    @DisplayName("创建分类 - 返回成功")
    void createCategory_shouldReturnSuccess() {
        ProductCategory category = new ProductCategory();
        category.setId(10L);
        category.setName("数码配件");

        when(productService.createCategory("数码配件", 0L)).thenReturn(category);

        Result<ProductCategory> result = productController.createCategory("数码配件", 0L);

        assertEquals(200, result.getCode());
        assertEquals("数码配件", result.getData().getName());
    }

    @Test
    @DisplayName("查询分类列表 - 返回所有分类")
    void listCategories_shouldReturnAll() {
        ProductCategory cat1 = new ProductCategory();
        cat1.setId(10L);
        cat1.setName("数码配件");
        ProductCategory cat2 = new ProductCategory();
        cat2.setId(20L);
        cat2.setName("生活用品");

        when(productService.listCategories()).thenReturn(Arrays.asList(cat1, cat2));

        Result<List<ProductCategory>> result = productController.listCategories();

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }
}
