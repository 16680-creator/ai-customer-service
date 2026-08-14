package com.aics.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aics.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项 Mapper
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
