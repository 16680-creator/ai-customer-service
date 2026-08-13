package com.aics.product.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.product.dto.ProductCreateDTO;
import com.aics.product.dto.ProductUpdateDTO;
import com.aics.product.entity.Product;
import com.aics.product.entity.ProductCategory;
import com.aics.product.mapper.ProductCategoryMapper;
import com.aics.product.mapper.ProductMapper;
import com.aics.product.service.ImageDescriptionService;
import com.aics.product.service.ProductService;
import com.aics.product.vo.ProductVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ProductVectorService productVectorService;
    private final ImageDescriptionService imageDescriptionService;

    @Override
    public ProductVO createProduct(ProductCreateDTO dto) {
        // 校验名称唯一
        Long count = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getName, dto.getName()));
        if (count > 0) {
            throw new BusinessException(ResultCode.PRODUCT_NAME_DUPLICATE, "商品名称已存在");
        }

        // 校验分类存在
        ProductCategory category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.PRODUCT_CATEGORY_NOT_FOUND, "商品分类不存在");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategoryId(dto.getCategoryId());
        product.setImage(dto.getImage());
        // 图片非空时生成视觉描述并落库，供向量检索复用
        fillImageDescription(product);
        product.setStatus(1);
        product.setSales(0);
        productMapper.insert(product);

        // 初始化 Redis 库存
        stringRedisTemplate.opsForValue().set("stock:" + product.getId(), String.valueOf(dto.getStock()));

        // 建立商品向量索引
        productVectorService.indexProduct(product);

        log.info("商品创建成功: id={}, name={}", product.getId(), product.getName());
        return toVO(product, category.getName());
    }

    @Override
    public IPage<ProductVO> getProductList(int page, int size, String keyword, Long categoryId, Integer status) {
        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreateTime);

        IPage<Product> productPage = productMapper.selectPage(pageParam, wrapper);

        // 转换为 VO
        Page<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream()
                .map(p -> toVO(p, getCategoryName(p.getCategoryId())))
                .toList());
        return voPage;
    }

    @Override
    public ProductVO getProductDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在");
        }
        return toVO(product, getCategoryName(product.getCategoryId()));
    }

    @Override
    public ProductVO updateProduct(Long id, ProductUpdateDTO dto) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在");
        }

        if (StringUtils.hasText(dto.getName())) {
            product.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }
        if (dto.getStock() != null) {
            product.setStock(dto.getStock());
            stringRedisTemplate.opsForValue().set("stock:" + id, String.valueOf(dto.getStock()));
        }
        if (dto.getCategoryId() != null) {
            product.setCategoryId(dto.getCategoryId());
        }
        if (dto.getImage() != null) {
            product.setImage(dto.getImage());
            // 图片变更时重新生成视觉描述
            fillImageDescription(product);
        }
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }

        productMapper.updateById(product);
        // 更新商品向量索引
        productVectorService.indexProduct(product);
        log.info("商品更新成功: id={}", id);
        return toVO(product, getCategoryName(product.getCategoryId()));
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在");
        }
        productMapper.deleteById(id);
        stringRedisTemplate.delete("stock:" + id);
        // 删除商品向量索引
        productVectorService.removeProduct(id);
        log.info("商品删除成功: id={}", id);
    }

    /**
     * 生成商品图片视觉描述并落库（视觉不可用或失败时保持为空，不阻断商品主流程）。
     */
    private void fillImageDescription(Product product) {
        if (!StringUtils.hasText(product.getImage())) {
            return;
        }
        try {
            String description = imageDescriptionService.describe(product.getImage());
            if (StringUtils.hasText(description)) {
                product.setImageDescription(description);
            }
        } catch (Exception e) {
            log.warn("商品图片描述生成失败: id={}, err={}", product.getId(), e.getMessage());
        }
    }

    @Override
    public void deductStock(Long productId, int quantity) {
        // 实时库存扣减：以 DB 为权威源，用「条件更新」保证原子性，避免并发超卖。
        // 仅当当前库存 >= 扣减数量时才执行 stock = stock - qty，返回受影响行数（0 表示不足/不存在）。
        int rows = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - " + quantity)
                .setSql("sales = sales + " + quantity));
        if (rows == 0) {
            // 扣减失败：商品不存在 或 库存不足
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在");
            }
            throw new BusinessException(ResultCode.PRODUCT_STOCK_INSUFFICIENT,
                    "商品库存不足，当前库存: " + product.getStock());
        }
        // 同步 Redis 镜像，使「商品详情/列表」与「购物车校验」读到一致的实时库存
        Product product = productMapper.selectById(productId);
        stringRedisTemplate.opsForValue().set("stock:" + productId, String.valueOf(product.getStock()));
        log.info("库存扣减成功(实时): productId={}, quantity={}, remaining={}", productId, quantity, product.getStock());
    }

    @Override
    public void restoreStock(Long productId, int quantity) {
        // 实时回补：DB 原子 stock = stock + qty，sales 同步回退（不低于 0）
        int rows = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("stock = stock + " + quantity)
                .setSql("sales = GREATEST(0, sales - " + quantity + ")"));
        if (rows == 0) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在");
        }
        Product product = productMapper.selectById(productId);
        stringRedisTemplate.opsForValue().set("stock:" + productId, String.valueOf(product.getStock()));
        log.info("库存回补成功(实时): productId={}, quantity={}, current={}", productId, quantity, product.getStock());
    }

    @Override
    public void syncStockCache() {
        // 运维校准：历史订单曾只扣 Redis 镜像、从不回写 DB，导致 Redis 与 DB 不一致。
        // 切换为「DB 权威源」后，部署完成时全量把 DB 同步到 Redis，使镜像立即对齐。
        List<Product> all = productMapper.selectList(null);
        for (Product p : all) {
            stringRedisTemplate.opsForValue().set("stock:" + p.getId(), String.valueOf(p.getStock()));
        }
        log.info("库存缓存校准完成(DB全量同步Redis): count={}", all.size());
    }

    @Override
    public ProductCategory createCategory(String name, Long parentId) {
        ProductCategory category = new ProductCategory();
        category.setName(name);
        category.setParentId(parentId != null ? parentId : 0L);
        category.setSort(0);
        categoryMapper.insert(category);
        log.info("分类创建成功: id={}, name={}", category.getId(), name);
        return category;
    }

    @Override
    public List<ProductCategory> listCategories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSort));
    }

    // ==================== 私有方法 ====================

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) return null;
        ProductCategory category = categoryMapper.selectById(categoryId);
        return category != null ? category.getName() : null;
    }

    private ProductVO toVO(Product product, String categoryName) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setDescription(product.getDescription());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setImage(product.getImage());
        vo.setStatus(product.getStatus());
        vo.setSales(product.getSales());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }
}
