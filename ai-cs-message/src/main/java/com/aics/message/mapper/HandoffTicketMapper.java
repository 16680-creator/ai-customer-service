package com.aics.message.mapper;

import com.aics.message.entity.HandoffTicket;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 转人工工单 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link HandoffTicket}（handoff_ticket 表）的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD；工单号生成与创建由
 * {@link com.aics.message.service.impl.AgentTraceServiceImpl} 实现。
 * </p>
 */
@Mapper
public interface HandoffTicketMapper extends BaseMapper<HandoffTicket> {
}
