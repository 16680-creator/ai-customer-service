package com.aics.order.mapper;

import com.aics.order.entity.AfterSaleApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 售后申请 Mapper
 */
@Mapper
public interface AfterSaleApplicationMapper extends BaseMapper<AfterSaleApplication> { // 继承 MyBatis-Plus 通用 Mapper，自动获得 CRUD 与条件查询能力
}
