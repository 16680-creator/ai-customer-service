package com.aics.message.mapper;

import com.aics.message.entity.UserFeedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户反馈 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link UserFeedback}（user_feedback 表）的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD；反馈写入与列表查询
 * 由 {@link com.aics.message.service.impl.OnlineEvalServiceImpl} 基于 insert/selectList 组合实现。
 *
 * <h3>【设计原理】反馈写入为什么不做存在性校验</h3>
 * <p>反馈是"独立信号"：即使 requestId 在 llm_trace 中不存在（未知来源的点赞/点踩），
 * 该信号本身仍有统计价值，应照常入库；因此本模块的反馈写入路径不查 trace 表，
 * requestId 仅作软关联，避免了不必要的跨表查询与失败丢弃。</p>
 * </p>
 */
// @Mapper 把接口注册进 MyBatis 的 MapperRegistry：启动时由 MyBatis 生成 JDK 动态代理
@Mapper
public interface UserFeedbackMapper extends BaseMapper<UserFeedback> {
    // 接口保持"零方法"：反馈条件查询（requestId/时间范围/倒序）在 Service 层拼装
}
