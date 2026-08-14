package com.aics.message.mapper;

import com.aics.message.entity.AgentStep;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 步骤轨迹 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link AgentStep}（agent_step 表）的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD；按 runId+stepNo 的幂等追加/覆盖
 * 由 {@link com.aics.message.service.impl.AgentTraceServiceImpl} 基于 LambdaQueryWrapper 组合实现。
 * </p>
 */
// 注册为 MyBatis Mapper 接口，由 MyBatis-Plus 自动生成代理实现
@Mapper
public interface AgentStepMapper extends BaseMapper<AgentStep> {
    // 无需自定义 SQL：按 (runId, stepNo) 的幂等追加/覆盖由 Service 层基于 LambdaQueryWrapper 组合实现
}
