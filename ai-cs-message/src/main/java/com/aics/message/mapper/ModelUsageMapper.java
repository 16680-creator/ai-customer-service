package com.aics.message.mapper;

import com.aics.message.entity.ModelUsage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型用量计量 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link ModelUsage}（model_usage 表）的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD；用量写入与统计聚合
 * 由 {@link com.aics.message.service.impl.ModelUsageServiceImpl} 基于 insert/selectList 组合实现
 * （统计采用 selectList + 内存聚合，不写自定义 SQL，保证可 mock 单测）。
 *
 * <h3>【设计原理】为什么统计不写 GROUP BY 自定义 SQL</h3>
 * <ul>
 *   <li>聚合逻辑（求和/计数）写在 Service 层肉眼可见、可单测、可加缓存，
 *       而自定义 SQL 里的 SUM/COUNT 无法被 Mockito 覆盖；</li>
 *   <li>用量明细按 userId/scenario/model/时间过滤后量级可控（天级万条以内），
 *       内存聚合的额外开销可忽略，换取的是"Mapper 可 mock"这一测试性红利；</li>
 *   <li>若未来数据量爆炸，再演进为 SQL 聚合或离线数仓，接口签名不变，调用方无感。</li>
 * </ul>
 * </p>
 */
// @Mapper 把接口注册进 MyBatis 的 MapperRegistry：启动时由 MyBatis 生成 JDK 动态代理
@Mapper
public interface ModelUsageMapper extends BaseMapper<ModelUsage> {
    // 接口保持"零方法"：统计聚合全部在 Service 层完成，Mapper 只提供基础查询
}
