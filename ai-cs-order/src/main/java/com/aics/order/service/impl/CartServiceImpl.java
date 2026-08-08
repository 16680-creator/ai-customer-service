package com.aics.order.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
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
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;

    @Override
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
    public CartVO addToCart(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }

        // 1. 从商品服务获取商品信息（名称/价格/库存/上下架状态）
        ProductRemoteDTO remote;
        try {
            remote = restTemplate.getForObject(
                    "http://ai-cs-product/product/{id}", ProductRemoteDTO.class, productId);
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
    public CartVO updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }

        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "购物车项不存在");
        }

        // 校验库存
        String stockStr = stringRedisTemplate.opsForValue().get("stock:" + cartItem.getProductId());
        if (stockStr != null) {
            int stock = Integer.parseInt(stockStr);
            if (quantity > stock) {
                throw new BusinessException(ResultCode.ORDER_STOCK_INSUFFICIENT,
                        "库存不足，最多可购买 " + stock + " 件");
            }
        }

        cartItem.setQuantity(quantity);
        cartItemMapper.updateById(cartItem);

        return getCartList(userId);
    }

    @Override
    public void deleteCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null || !cartItem.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "购物车项不存在");
        }
        cartItemMapper.deleteById(cartItemId);
    }

    @Override
    public void selectCartItems(Long userId, List<Long> cartItemIds, Boolean selected) {
        cartItemMapper.update(null,
                new LambdaUpdateWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .in(CartItem::getId, cartItemIds)
                        .set(CartItem::getSelected, selected));
    }

    private int availableStock(Long productId, Integer productStock) {
        String stockStr = stringRedisTemplate.opsForValue().get("stock:" + productId);
        if (stockStr != null) {
            try {
                return Integer.parseInt(stockStr);
            } catch (NumberFormatException ignored) {
                // 忽略 Redis 中异常库存值，回退到商品快照
            }
        }
        return productStock == null ? Integer.MAX_VALUE : productStock;
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