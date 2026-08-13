package com.aics.message.mapper;

import com.aics.message.entity.AgentConfirmation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 写操作确认记录 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link AgentConfirmation}（agent_confirmation 表）的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD；按 runId+action 的幂等记录/覆盖
 * 由 {@link com.aics.message.service.impl.AgentTraceServiceImpl} 基于 LambdaQueryWrapper 组合实现。
 * </p>
 */
@Mapper
public interface AgentConfirmationMapper extends BaseMapper<AgentConfirmation> {
}
