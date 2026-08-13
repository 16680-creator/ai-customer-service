package com.aics.message.mapper;

import com.aics.message.entity.AgentRun;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 执行记录 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link AgentRun}（agent_run 表）的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD；按 runId 的幂等创建与状态更新
 * 由 {@link com.aics.message.service.impl.AgentTraceServiceImpl} 基于 selectById/updateById 组合实现。
 * </p>
 */
@Mapper
public interface AgentRunMapper extends BaseMapper<AgentRun> {
}
