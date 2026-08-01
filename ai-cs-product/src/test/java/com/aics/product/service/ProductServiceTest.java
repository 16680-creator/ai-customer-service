package com.aics.product.service;

import com.aics.common.exception.BusinessException;
import com.aics.product.dto.ProductCreateDTO;
import com.aics.product.dto.ProductUpdateDTO;
import com.aics.product.entity.Product;
import com.aics.product.entity.ProductCategory;
import com.aics.product.mapper.ProductCategoryMapper;
import com.aics.product.mapper.ProductMapper;
import com.aics.product.service.impl.ProductServiceImpl;
import com.aics.product.vo.ProductVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 商品服务单元测试（TDD Red 阶段编写）
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductCategoryMapper categoryMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;
    private ProductCategory sampleCategory;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("无线蓝牙耳机");
        sampleProduct.setDescription("高品质降噪蓝牙耳机");
        sampleProduct.setPrice(new BigDecimal("199.00"));
        sampleProduct.setStock(100);
        sampleProduct.setCategoryId(10L);
        sampleProduct.setImage("https://img.example.com/earphone.jpg");
        sampleProduct.setStatus(1);
        sampleProduct.setSales(50);
        sampleProduct.setCreateTime(LocalDateTime.now());
        sampleProduct.setUpdateTime(LocalDateTime.now());

        sampleCategory = new ProductCategory();
        sampleCategory.setId(10L);
        sampleCategory.setName("数码配件");
        sampleCategory.setParentId(0L);
        sampleCategory.setSort(1);
    }

    // ==================== 创建商品 ====================

    @Test
    @DisplayName("创建商品 - 成功")
    void createProduct_shouldSuccess() {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("无线蓝牙耳机");
        dto.setDescription("高品质降噪蓝牙耳机");
        dto.setPrice(new BigDecimal("199.00"));
        dto.setStock(100);
        dto.setCategoryId(10L);
        dto.setImage("https://img.example.com/earphone.jpg");

        when(productMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.selectById(10L)).thenReturn(sampleCategory);
        when(productMapper.insert(any(Product.class))).thenReturn(1);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        ProductVO result = productService.createProduct(dto);

        assertNotNull(result);
        assertEquals("无线蓝牙耳机", result.getName());
        assertEquals(new BigDecimal("199.00"), result.getPrice());
        assertEquals(100, result.getStock());
        verify(productMapper).insert(any(Product.class));
    }

    @Test
    @DisplayName("创建商品 - 名称重复应抛出异常")
    void createProduct_duplicateName_shouldThrow() {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("无线蓝牙耳机");
        dto.setPrice(new BigDecimal("199.00"));
        dto.setStock(100);
        dto.setCategoryId(10L);

        when(productMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.createProduct(dto));
        assertTrue(ex.getMessage().contains("已存在"));
        verify(productMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建商品 - 分类不存在应抛出异常")
    void createProduct_categoryNotFound_shouldThrow() {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("新商品");
        dto.setPrice(new BigDecimal("99.00"));
        dto.setStock(50);
        dto.setCategoryId(999L);

        when(productMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.createProduct(dto));
        assertTrue(ex.getMessage().contains("分类不存在"));
    }

    // ==================== 商品列表 ====================

    @Test
    @DisplayName("分页查询商品列表 - 正常返回")
    void getProductList_shouldReturnPaged() {
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleProduct));
        page.setTotal(1);

        when(productMapper.selectPage(any(Page.class), any())).thenReturn(page);

        IPage<ProductVO> result = productService.getProductList(1, 10, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("无线蓝牙耳机", result.getRecords().get(0).getName());
    }

    @Test
    @DisplayName("分页查询商品列表 - 空结果")
    void getProductList_empty_shouldReturnEmptyPage() {
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        when(productMapper.selectPage(any(Page.class), any())).thenReturn(page);

        IPage<ProductVO> result = productService.getProductList(1, 10, null, null, null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== 商品详情 ====================

    @Test
    @DisplayName("查询商品详情 - 成功")
    void getProductDetail_shouldReturn() {
        when(productMapper.selectById(1L)).thenReturn(sampleProduct);

        ProductVO result = productService.getProductDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("无线蓝牙耳机", result.getName());
        assertEquals("高品质降噪蓝牙耳机", result.getDescription());
        assertEquals(new BigDecimal("199.00"), result.getPrice());
        assertEquals(100, result.getStock());
        assertEquals(50, result.getSales());
    }

    @Test
    @DisplayName("查询商品详情 - 不存在应抛出异常")
    void getProductDetail_notFound_shouldThrow() {
        when(productMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.getProductDetail(999L));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    // ==================== 更新商品 ====================

    @Test
    @DisplayName("更新商品 - 成功")
    void updateProduct_shouldSuccess() {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setName("升级版蓝牙耳机");
        dto.setPrice(new BigDecimal("259.00"));

        when(productMapper.selectById(1L)).thenReturn(sampleProduct);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        ProductVO result = productService.updateProduct(1L, dto);

        assertNotNull(result);
        verify(productMapper).updateById(argThat(p ->
                "升级版蓝牙耳机".equals(p.getName()) &&
                new BigDecimal("259.00").equals(p.getPrice())));
    }

    @Test
    @DisplayName("更新商品 - 商品不存在应抛出异常")
    void updateProduct_notFound_shouldThrow() {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setName("不存在的商品");

        when(productMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> productService.updateProduct(999L, dto));
        verify(productMapper, never()).updateById(any());
    }

    // ==================== 删除商品 ====================

    @Test
    @DisplayName("删除商品 - 成功")
    void deleteProduct_shouldSuccess() {
        when(productMapper.selectById(1L)).thenReturn(sampleProduct);
        when(productMapper.deleteById(1L)).thenReturn(1);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除商品 - 不存在应抛出异常")
    void deleteProduct_notFound_shouldThrow() {
        when(productMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> productService.deleteProduct(999L));
        verify(productMapper, never()).deleteById(anyLong());
    }

    // ==================== 库存管理 ====================

    @Test
    @DisplayName("扣减库存 - 成功")
    void deductStock_shouldSuccess() {
        when(productMapper.selectById(1L)).thenReturn(sampleProduct);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        assertDoesNotThrow(() -> productService.deductStock(1L, 5));

        verify(productMapper).updateById(argThat(p -> p.getStock() == 95));
    }

    @Test
    @DisplayName("扣减库存 - 库存不足应抛出异常")
    void deductStock_insufficient_shouldThrow() {
        when(productMapper.selectById(1L)).thenReturn(sampleProduct);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.deductStock(1L, 200));
        assertTrue(ex.getMessage().contains("库存不足"));
        verify(productMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("扣减库存 - 商品不存在应抛出异常")
    void deductStock_productNotFound_shouldThrow() {
        when(productMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> productService.deductStock(999L, 1));
    }

    @Test
    @DisplayName("恢复库存 - 成功")
    void restoreStock_shouldSuccess() {
        when(productMapper.selectById(1L)).thenReturn(sampleProduct);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        assertDoesNotThrow(() -> productService.restoreStock(1L, 10));

        verify(productMapper).updateById(argThat(p -> p.getStock() == 110));
    }

    // ==================== 分类管理 ====================

    @Test
    @DisplayName("创建分类 - 成功")
    void createCategory_shouldSuccess() {
        when(categoryMapper.insert(any(ProductCategory.class))).thenReturn(1);

        ProductCategory result = productService.createCategory("数码配件", 0L);

        assertNotNull(result);
        assertEquals("数码配件", result.getName());
        verify(categoryMapper).insert(any(ProductCategory.class));
    }

    @Test
    @DisplayName("查询分类列表 - 返回所有分类")
    void listCategories_shouldReturnAll() {
        ProductCategory cat2 = new ProductCategory();
        cat2.setId(20L);
        cat2.setName("生活用品");
        cat2.setParentId(0L);
        cat2.setSort(2);

        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(sampleCategory, cat2));

        List<ProductCategory> result = productService.listCategories();

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
