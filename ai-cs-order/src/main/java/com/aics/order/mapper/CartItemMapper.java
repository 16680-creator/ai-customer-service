package com.aics.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aics.order.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车项 Mapper
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
