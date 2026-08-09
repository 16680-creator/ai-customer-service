package com.aics.pay.mapper;

import com.aics.pay.entity.PayTransaction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付流水 Mapper
 */
@Mapper
public interface PayTransactionMapper extends BaseMapper<PayTransaction> {
}