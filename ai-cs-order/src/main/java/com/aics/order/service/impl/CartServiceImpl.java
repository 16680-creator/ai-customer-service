package com.aics.order.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.client.ProductClient;
import com.aics.order.dto.ProductRemoteDTO;
import com.aics.order.entity.CartItem;
import com.aics.order.mapper.CartItemMapper;
import com.aics.order.service.CartService;
import com.aics.order.vo.CartVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ProductClient productClient;

    @Override
    /** 查询购物车列表（含商品信息与选中状态，供结算试算使用） */
    public CartVO getCartList(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .orderByDesc(CartItem::getCreateTime));

        CartVO vo = new CartVO();
        vo.setItems(items.stream().map(this::toCartItemVO).collect(Collectors.toList()));
        vo.setTotalAmount(items.stream()
                .filter(CartItem::getSelected)
                .map(item -> item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setSelectedCount((int) items.stream().filter(CartItem::getSelected).count());
        return vo;
    }

    @Override
    /**
     * 加购：校验库存与重复条目。
     * <p><b>学习要点</b>：同商品已存在则累加数量并重新校验库存上限，
     * 这是"幂等加购"的关键（AI 客服"帮我加购"工具即调用本方法）。</p>
     */
    public CartVO addToCart(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }

        // 1. 从商品服务获取商品信息（名称/价格/库存/上下架状态）
        ProductRemoteDTO remote;
        try {
            remote = productClient.getProduct(productId);
        } catch (Exception e) {
            log.error("获取商品信息失败: productId={}", productId, e);
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品服务暂时不可用，请稍后再试");
        }
        if (remote == null || remote.getData() == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在");
        }
        ProductRemoteDTO.ProductData product = remote.getData();
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(ResultCode.PRODUCT_OFF_SHELF, "商品已下架");
        }

        // 2. 校验库存（优先 Redis 扣减库存，其次商品快照库存）
        int availableStock = availableStock(productId, product.getStock());
        if (quantity > availableStock) {
            throw new BusinessException(ResultCode.ORDER_STOCK_INSUFFICIENT,
                    "库存不足，最多可购买 " + availableStock + " 件");
        }

        // 3. 购物车已有该商品则累加数量，否则新增（uk_user_product 唯一键）
        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId));
        if (existing != null) {
            int newQuantity = existing.getQuantity() + quantity;
            if (newQuantity > availableStock) {
                throw new BusinessException(ResultCode.ORDER_STOCK_INSUFFICIENT,
                        "库存不足，购物车已有 " + existing.getQuantity() + " 件，最多再加 "
                                + (availableStock - existing.getQuantity()) + " 件");
            }
            existing.setQuantity(newQuantity);
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setProductName(product.getName());
            item.setProductPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setSelected(true);
            cartItemMapper.insert(item);
        }

        return getCartList(userId);
    }

    @Override
    /** 修改购物车条目数量（再次校验库存，数量为 0 视为删除） */
    public CartVO updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }

        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "购物车项不存在");
        }

        // 实时校验库存：优先取商品服务实时库存，其次 Redis 镜像兜底
        int available = currentStock(cartItem.getProductId());
        if (quantity > available) {
            throw new BusinessException(ResultCode.ORDER_STOCK_INSUFFICIENT,
                    "库存不足，最多可购买 " + available + " 件");
        }

        cartItem.setQuantity(quantity);
        cartItemMapper.updateById(cartItem);

        return getCartList(userId);
    }

    @Override
    /** 删除购物车条目（校验归属，防止删他人条目） */
    public void deleteCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "购物车项不存在");
        }
        cartItemMapper.deleteById(cartItemId);
    }

    @Override
    /** 勾选/取消勾选购物车条目（只有勾选条目参与结算试算） */
    public void selectCartItems(Long userId, List<Long> cartItemIds, Boolean selected) {
        cartItemMapper.update(null,
                new LambdaUpdateWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .in(CartItem::getId, cartItemIds)
                        .set(CartItem::getSelected, selected));
    }

    private int availableStock(Long productId, Integer productStock) {
        // 实时库存以商品服务返回的 DB 库存为准；Redis 镜像仅在商品服务不可用时兜底
        if (productStock != null) {
            return productStock;
        }
        String stockStr = stringRedisTemplate.opsForValue().get("stock:" + productId);
        if (stockStr != null) {
            try {
                int v = Integer.parseInt(stockStr);
                if (v >= 0) return v; // 历史脏数据（负数）视为无效，继续走下方兜底
            } catch (NumberFormatException ignored) {
                // 忽略 Redis 中异常库存值
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 获取商品实时可用库存：优先商品服务 DB 库存，其次 Redis 镜像兜底。
     */
    private int currentStock(Long productId) {
        try {
            ProductRemoteDTO remote = productClient.getProduct(productId);
            if (remote != null && remote.getData() != null && remote.getData().getStock() != null) {
                return remote.getData().getStock();
            }
        } catch (Exception e) {
            log.warn("获取实时库存失败，改用 Redis 兜底: productId={}", productId, e);
        }
        String stockStr = stringRedisTemplate.opsForValue().get("stock:" + productId);
        if (stockStr != null) {
            try {
                int v = Integer.parseInt(stockStr);
                if (v >= 0) return v; // 历史脏数据（负数）视为无效，继续走下方兜底
            } catch (NumberFormatException ignored) {
                // 忽略异常值
            }
        }
        return Integer.MAX_VALUE;
    }

    private CartVO.CartItemVO toCartItemVO(CartItem item) {
        CartVO.CartItemVO vo = new CartVO.CartItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setProductName(item.getProductName());
        vo.setProductPrice(item.getProductPrice());
        vo.setQuantity(item.getQuantity());
        vo.setSelected(item.getSelected());
        vo.setSubtotal(item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return vo;
    }
}