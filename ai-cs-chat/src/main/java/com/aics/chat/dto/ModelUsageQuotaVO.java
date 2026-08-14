package com.aics.chat.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型用量配额 VO（chat 侧，与 ai-cs-message 的 ModelUsageQuotaVO 对齐，用于 Feign 回读）
 *
 * <h3>【AI 技术详解】配额模型：按窗口 + 按用户/场景</h3>
 * <ul>
 *   <li><b>为什么按 (userId, scenario) 配额度</b>：不同场景成本差异大（RAG 检索+生成
 *       比普通对话贵），全局一个配额会互相挤占；按场景隔离可针对高成本场景单独设限。</li>
 *   <li><b>窗口类型 DAILY/WEEKLY/MONTHLY</b>：配额按时间窗口滚动重置，message 侧按
 *       periodStart 起算窗口内累计用量；窗口内超限即拒绝，窗口结束自动恢复。</li>
 *   <li><b>NULL 语义 = 不限</b>：quotaTokens 与 quotaCost 任一为 null 表示该维度不设限
 *       （如只限费用不限 token），避免"填 0 表示不限"的歧义。</li>
 * </ul>
 */
@Data
public class ModelUsageQuotaVO {

    /** 用户 ID */
    private Long userId;

    /** 场景 */
    private String scenario;

    /** 窗口：DAILY/WEEKLY/MONTHLY */
    private String windowType;

    /** Token 配额（NULL=不限） */
    // 双维度配额可只配其一：cost 维度 null 时只看 token，反之亦然；都配时任一超限即拒绝
    private Long quotaTokens;

    /** 费用配额（元，NULL=不限） */
    private BigDecimal quotaCost;

    /** 窗口起始时间 */
    // 配额判定锚点：message 侧累计 periodStart 之后的用量，窗口翻转时该值随周期前移
    private LocalDateTime periodStart;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
