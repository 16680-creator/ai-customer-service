package com.aics.message.mapper;

import com.aics.message.entity.SecurityEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 安全事件 Mapper（3.2 F7 审计留痕）。
 */
@Mapper
public interface SecurityEventMapper extends BaseMapper<SecurityEvent> {
}
