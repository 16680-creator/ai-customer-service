package com.aics.message.mapper;

import com.aics.message.entity.ModelUsageQuota;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型用量配额 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link ModelUsageQuota}（model_usage_quota 表）的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD；按 (userId, scenario) 的 upsert 与查询
 * 由 {@link com.aics.message.service.impl.ModelUsageQuotaServiceImpl} 基于 selectOne/insert/updateById 组合实现。
 *
 * <h3>【设计原理】upsert 为什么不用 MySQL 的 ON DUPLICATE KEY</h3>
 * <ul>
 *   <li>服务层"先查后写"能拿到已有记录并做部分字段更新（可空字段不覆盖），
 *       ON DUPLICATE KEY 只能整行覆盖或按表达式更新，表达不了"保留原值"；</li>
 *   <li>并发安全由表级 UNIQUE KEY uk_user_scenario 兜底：即便先查后写有竞态，
 *       重复插入会被唯一键拒绝，不会产生脏数据；</li>
 *   <li>先查后写还让 upsert 返回"新建/更新"的日志语义更清晰，便于审计。</li>
 * </ul>
 * </p>
 */
// @Mapper 把接口注册进 MyBatis 的 MapperRegistry：启动时由 MyBatis 生成 JDK 动态代理
@Mapper
public interface ModelUsageQuotaMapper extends BaseMapper<ModelUsageQuota> {
    // 接口保持"零方法"：upsert 逻辑（查→改/插）在 Service 层组合实现
}
