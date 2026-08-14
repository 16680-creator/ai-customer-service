package com.aics.chat.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型用量统计 VO（chat 侧，与 ai-cs-message 的 ModelUsageStatsVO 对齐，用于 Feign 回读）
 *
 * <h3>【AI 技术详解】聚合统计为什么用 Long/BigDecimal 而不是 Integer/Double？</h3>
 * <ul>
 *   <li><b>token 累计可能溢出 int</b>：单次调用 token 是 Integer，但按天/月聚合后
 *       数量级可达十亿级，用 Long 承载求和结果；</li>
 *   <li><b>费用用 BigDecimal 而非 Double</b>：金额不允许浮点误差（0.1+0.2≠0.3），
 *       BigDecimal 保证跨服务统计口径一致；</li>
 *   <li><b>一套 VO 两个用途</b>：配额检查（对比 quotaTokens）与成本看板（展示消耗）
 *       复用同一聚合口径，避免两处统计口径漂移。</li>
 * </ul>
 */
@Data
public class ModelUsageStatsVO {

    /** 调用次数 */
    // 含失败调用：统计"真实请求量"用于评估 QPS 与成本，而非只看成功数
    private Long callCount;

    /** 输入 Token 总数 */
    private Long inputTokens;

    /** 输出 Token 总数 */
    private Long outputTokens;

    /** 总 Token 数 */
    private Long totalTokens;

    /** 估算费用合计（元） */
    // 合计口径与单条一致：估算标记的用量按估算价累加，保证"总量=明细之和"
    private BigDecimal estimatedCost;
}
