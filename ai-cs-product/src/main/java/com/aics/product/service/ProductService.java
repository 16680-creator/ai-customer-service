package com.aics.product.service;

import com.aics.product.dto.ProductCreateDTO;
import com.aics.product.dto.ProductUpdateDTO;
import com.aics.product.entity.ProductCategory;
import com.aics.product.vo.ProductVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 创建商品
     */
    ProductVO createProduct(ProductCreateDTO dto);

    /**
     * 分页查询商品列表
     */
    IPage<ProductVO> getProductList(int page, int size, String keyword, Long categoryId, Integer status);

    /**
     * 查询商品详情
     */
    ProductVO getProductDetail(Long id);

    /**
     * 更新商品
     */
    ProductVO updateProduct(Long id, ProductUpdateDTO dto);

    /**
     * 删除商品
     */
    void deleteProduct(Long id);

    /**
     * 扣减库存
     */
    void deductStock(Long productId, int quantity);

    /**
     * 恢复库存
     */
    void restoreStock(Long productId, int quantity);

    /**
     * 创建分类
     */
    ProductCategory createCategory(String name, Long parentId);

    /**
     * 查询分类列表
     */
    List<ProductCategory> listCategories();
}
