package com.aics.message.mapper;

import com.aics.message.entity.LlmTrace;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * LLM 调用链追踪 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link LlmTrace}（llm_trace 表）的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD；按 requestId 的幂等创建与分页查询
 * 由 {@link com.aics.message.service.impl.LlmTraceServiceImpl} 基于 selectById/selectPage 组合实现。
 *
 * <h3>【设计原理】为什么继承 BaseMapper 就够了，无需写任何 SQL</h3>
 * <ul>
 *   <li>MyBatis-Plus 启动时通过 TableInfoHelper 解析实体上的
 *       {@code @TableName/@TableId/@TableField} 注解，为每个实体建立"表信息缓存"
 *       （列名映射、主键策略、自动填充字段等）；</li>
 *   <li>{@link BaseMapper} 的 insert/selectById/selectPage 等方法是基于该缓存、
 *       结合 Service 层传入的条件构造器动态拼 SQL 的"模板方法"，
 *       因此本模块零自定义 SQL 即可完成全部读写；</li>
 *   <li>业务语义（幂等、过滤、聚合）放在 Service 层而非 Mapper 层：Mapper 退化为纯数据访问
 *       接口后，单元测试可用 Mockito 直接 mock，无需启动数据库。</li>
 * </ul>
 * </p>
 */
// @Mapper 把接口注册进 MyBatis 的 MapperRegistry：启动时由 MyBatis 生成 JDK 动态代理，
// 代理把接口方法映射到 BaseMapper 内置 SQL 模板执行
@Mapper
public interface LlmTraceMapper extends BaseMapper<LlmTrace> {
    // 接口保持"零方法"：全部能力继承自 BaseMapper，避免自定义方法破坏可 mock 性
}
