package com.aics.message.mapper;

import com.aics.message.entity.OnlineEvalRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 线上采样评估记录 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link OnlineEvalRecord}（online_eval_record 表）的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD；评估写入与统计聚合
 * 由 {@link com.aics.message.service.impl.OnlineEvalServiceImpl} 基于 insert/selectList 组合实现
 * （统计采用 selectList + 内存聚合，不写自定义 SQL，保证可 mock 单测）。
 *
 * <h3>【设计原理】为什么评估与反馈分两张表、两个 Mapper</h3>
 * <p>评估记录（LLM-as-Judge 结果）与用户反馈（点赞/点踩）来源、字段、统计口径都不同，
 * 分表避免互相污染；统计时各自独立 selectList 再在 Service 层合并成
 * {@link com.aics.message.vo.OnlineEvalStatsVO}，语义清晰且各自可 mock。</p>
 * </p>
 */
// @Mapper 把接口注册进 MyBatis 的 MapperRegistry：启动时由 MyBatis 生成 JDK 动态代理
@Mapper
public interface OnlineEvalRecordMapper extends BaseMapper<OnlineEvalRecord> {
    // 接口保持"零方法"：统计聚合在 Service 层完成，Mapper 只提供基础查询
}
