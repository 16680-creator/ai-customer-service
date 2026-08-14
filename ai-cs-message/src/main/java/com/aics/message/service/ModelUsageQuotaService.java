package com.aics.message.service;

import com.aics.message.dto.ModelUsageQuotaDTO;
import com.aics.message.vo.ModelUsageQuotaVO;

/**
 * 模型用量配额服务接口
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：定义模型用量配额（model_usage_quota 表）的设置与查询能力，供成本治理
 * 配置与预检查使用。
 * upsert 约定：按 (userId, scenario) 幂等 upsert——已存在则更新
 * （windowType/quotaTokens/quotaCost/periodStart，可空字段不覆盖原值），不存在则插入；
 * windowType 未传时默认 DAILY（实体初始值保证）。
 * 查询约定：getQuota 不存在时返回 null（不抛异常）。
 * 实现类：{@link com.aics.message.service.impl.ModelUsageQuotaServiceImpl}。
 * 调用方：{@link com.aics.message.controller.ModelUsageQuotaController}。
 *
 * <h3>【设计原理】为什么是 upsert（先查后写）而非"插入 + 唯一键异常捕获"</h3>
 * <ul>
 *   <li>upsert 先按 (userId, scenario) 查出已有记录，可以做到"部分字段更新、可空字段保留原值"
 *       ——这正是配额运营的诉求（改 windowType 时不想清掉 quotaCost）；</li>
 *   <li>并发下的重复插入由表级唯一键 uk_user_scenario 兜底拒绝，
 *       先查后写只是常态路径，不需要依赖异常流转做控制流；</li>
 *   <li>getQuota 缺失返回 null：配额"没配过"是正常初始态（NULL=不限），
 *       不应被当作错误上报。</li>
 * </ul>
 * </p>
 */
public interface ModelUsageQuotaService {

    /**
     * 设置/更新模型用量配额（按 userId+scenario 幂等 upsert）
     *
     * @param dto 配额信息
     */
    void upsertQuota(ModelUsageQuotaDTO dto);

    /**
     * 查询模型用量配额（不存在返回 null，不抛异常）
     *
     * @param userId   用户ID
     * @param scenario 场景
     * @return 配额信息，不存在时为 null
     */
    ModelUsageQuotaVO getQuota(Long userId, String scenario);
}
