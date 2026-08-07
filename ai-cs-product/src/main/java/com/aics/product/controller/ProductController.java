package com.aics.product.controller;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.common.storage.FileStorageService;
import com.aics.product.dto.ProductCreateDTO;
import com.aics.product.dto.ProductUpdateDTO;
import com.aics.product.entity.ProductCategory;
import com.aics.product.service.ProductService;
import com.aics.product.service.impl.ProductVectorService;
import com.aics.product.vo.ProductSimilarVO;
import com.aics.product.vo.ProductVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    /** 支持上传的图片格式 */
    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** 图片大小上限 5MB */
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;

    private final ProductService productService;
    private final ProductVectorService productVectorService;
    private final FileStorageService fileStorageService;

    /**
     * 上传商品图片（存 MinIO，返回图片 URL）
     */
    @PostMapping("/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = originalName == null ? "" :
                originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.PRODUCT_IMAGE_INVALID, "仅支持 jpg/png/webp/gif 格式");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ResultCode.PRODUCT_IMAGE_TOO_LARGE, "图片大小不能超过 5MB");
        }
        String url = fileStorageService.upload(file, "product/images");
        return Result.success("图片上传成功", url);
    }

    /**
     * 按文本检索相似商品（以文搜图）
     */
    @GetMapping("/similar")
    public Result<List<ProductSimilarVO>> searchSimilar(
            @RequestParam("text") String text,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        return Result.success(productVectorService.searchByText(text, topK));
    }

    /**
     * 按商品查找相似商品
     */
    @GetMapping("/{id}/similar")
    public Result<List<ProductSimilarVO>> findSimilar(
            @PathVariable("id") Long id,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        return Result.success(productVectorService.searchByProduct(id, topK));
    }

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
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "status", required = false) Integer status) {
        IPage<ProductVO> result = productService.getProductList(page, size, keyword, categoryId, status);
        return Result.success(result);
    }

    /**
     * 查询商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable("id") Long id) {
        ProductVO vo = productService.getProductDetail(id);
        return Result.success(vo);
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    public Result<ProductVO> updateProduct(@PathVariable("id") Long id,
                                           @Valid @RequestBody ProductUpdateDTO dto) {
        ProductVO vo = productService.updateProduct(id, dto);
        return Result.success("商品更新成功", vo);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    /**
     * 扣减库存
     */
    @PutMapping("/{id}/stock/deduct")
    public Result<Void> deductStock(@PathVariable("id") Long id,
                                    @RequestParam("quantity") int quantity) {
        productService.deductStock(id, quantity);
        return Result.success();
    }

    /**
     * 恢复库存
     */
    @PutMapping("/{id}/stock/restore")
    public Result<Void> restoreStock(@PathVariable("id") Long id,
                                     @RequestParam("quantity") int quantity) {
        productService.restoreStock(id, quantity);
        return Result.success();
    }

    /**
     * 创建分类
     */
    @PostMapping("/category")
    public Result<ProductCategory> createCategory(@RequestParam("name") String name,
                                                  @RequestParam(value = "parentId", defaultValue = "0") Long parentId) {
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
