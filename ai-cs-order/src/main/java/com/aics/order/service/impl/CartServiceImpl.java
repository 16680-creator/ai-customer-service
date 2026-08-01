package com.aics.order.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
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
