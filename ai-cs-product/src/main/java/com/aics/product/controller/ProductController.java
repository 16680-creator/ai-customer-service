package com.aics.product.controller;

import com.aics.common.result.Result;
import com.aics.product.dto.ProductCreateDTO;
import com.aics.product.dto.ProductUpdateDTO;
import com.aics.product.entity.ProductCategory;
import com.aics.product.service.ProductService;
import com.aics.product.vo.ProductVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 创建商品
     */
    @PostMapping
    public Result<ProductVO> createProduct(@Valid @RequestBody ProductCreateDTO dto) {
        ProductVO vo = productService.createProduct(dto);
        return Result.success("商品创建成功", vo);
    }

    /**
     * 分页查询商品列表
     */
    @GetMapping("/list")
    public Result<IPage<ProductVO>> listProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        IPage<ProductVO> result = productService.getProductList(page, size, keyword, categoryId, status);
        return Result.success(result);
    }

    /**
     * 查询商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable Long id) {
        ProductVO vo = productService.getProductDetail(id);
        return Result.success(vo);
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    public Result<ProductVO> updateProduct(@PathVariable Long id,
                                           @Valid @RequestBody ProductUpdateDTO dto) {
        ProductVO vo = productService.updateProduct(id, dto);
        return Result.success("商品更新成功", vo);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    /**
     * 扣减库存
     */
    @PutMapping("/{id}/stock/deduct")
    public Result<Void> deductStock(@PathVariable Long id,
                                    @RequestParam int quantity) {
        productService.deductStock(id, quantity);
        return Result.success();
    }

    /**
     * 恢复库存
     */
    @PutMapping("/{id}/stock/restore")
    public Result<Void> restoreStock(@PathVariable Long id,
                                     @RequestParam int quantity) {
        productService.restoreStock(id, quantity);
        return Result.success();
    }

    /**
     * 创建分类
     */
    @PostMapping("/category")
    public Result<ProductCategory> createCategory(@RequestParam String name,
                                                  @RequestParam(defaultValue = "0") Long parentId) {
        ProductCategory category = productService.createCategory(name, parentId);
        return Result.success("分类创建成功", category);
    }

    /**
     * 查询分类列表
     */
    @GetMapping("/categories")
    public Result<List<ProductCategory>> listCategories() {
        List<ProductCategory> categories = productService.listCategories();
        return Result.success(categories);
    }
}
