package com.aics.order.service;

import com.aics.order.vo.CartVO;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService {

    /**
     * 获取用户购物车列表
     */
    CartVO getCartList(Long userId);

    /**
     * 加入购物车（商品不存在/已下架/库存不足会抛出异常）
     */
    CartVO addToCart(Long userId, Long productId, Integer quantity);

    /**
     * 修改购物车商品数量
     */
    CartVO updateQuantity(Long userId, Long cartItemId, Integer quantity);

    /**
     * 删除购物车商品
     */
    void deleteCartItem(Long userId, Long cartItemId);

    /**
     * 切换商品选中状态
     */
    void selectCartItems(Long userId, List<Long> cartItemIds, Boolean selected);
}