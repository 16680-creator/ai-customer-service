package com.aics.message.service;

import com.aics.message.dto.ModelUsageDTO;
import com.aics.message.vo.ModelUsageStatsVO;

import java.time.LocalDateTime;

/**
 * 模型用量计量服务接口
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：定义模型用量（model_usage 表）的写入与统计能力，供 chat 模块 LLM 编排链路
 * 上报 Token 用量与费用，并按用户/场景/模型/时间范围聚合统计（成本治理与配额预检查依据）。
 * 统计约定：stats 使用 LambdaQueryWrapper 过滤 + selectList 全量查出后内存聚合，
 * 不写自定义 SQL，保证可 mock 单元测试。
 * 实现类：{@link com.aics.message.service.impl.ModelUsageServiceImpl}。
 * 调用方：{@link com.aics.message.controller.ModelUsageController}。
 *
 * <h3>【设计原理】为什么统计选择"内存聚合"而非 SQL GROUP BY</h3>
 * <ul>
 *   <li>可测试性：Mapper 全 mock 时，自定义 SQL 里的 SUM/COUNT 无法被单元测试覆盖，
 *       而 selectList + 内存求和每一步都能断言（见 ModelUsageServiceTest）；</li>
 *   <li>可演进：聚合规则（如费用口径、估算标记过滤）改在 Java 侧，发布即生效，
 *       不需要 DBA 改 SQL；未来数据量大可无感切换到 SQL/离线聚合，接口签名不变。</li>
 * </ul>
 * </p>
 */
public interface ModelUsageService {

    /**
     * 记录模型用量（totalTokens 未传时按 inputTokens + outputTokens 兜底计算）
     *
     * @param dto 模型用量信息
     */
    void recordUsage(ModelUsageDTO dto);

    /**
     * 按条件统计模型用量（userId/scenario/model/时间范围均可空过滤，内存聚合）
     *
     * @param userId    用户ID（可空）
     * @param scenario  场景（可空）
     * @param model     模型名（可空）
     * @param startTime 起始时间（可空）
     * @param endTime   结束时间（可空）
     * @return 用量统计结果
     */
    ModelUsageStatsVO stats(Long userId, String scenario, String model, LocalDateTime startTime, LocalDateTime endTime);
}
