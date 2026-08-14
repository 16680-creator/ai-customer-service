package com.aics.chat.dto;

import lombok.Data;

/**
 * 线上采样评估记录上报 DTO（chat 侧，与 ai-cs-message 的 OnlineEvalRecordDTO 对齐）
 *
 * <h3>【AI 技术详解】为什么线上评估只存"摘要"而不存全文？</h3>
 * <ul>
 *   <li><b>存储成本</b>：线上评估按采样率持续产生（每天可能成千上万条），全文入库
 *       成本高且隐私风险大；截断的问题/回答摘要足以支撑质量趋势统计。</li>
 *   <li><b>评估语义</b>：LLM-as-Judge 打分是 1-5 的量化信号，统计侧关注"分数分布与
 *       趋势"，不需要原文；需要复盘原文时可通过 requestId 反查 trace 与消息记录。</li>
 *   <li><b>judgeStatus 三态</b>：SUCCESS/FAILED/SKIPPED —— Judge 调用失败或采样在
 *       评分前被中断时记 SKIPPED，保证"样本数 = 三态之和"，统计不丢样本。</li>
 * </ul>
 */
@Data
public class OnlineEvalRecordDTO {

    /** 请求 ID（关联 llm_trace） */
    private String requestId;

    /** 会话 ID */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 问题摘要（截断） */
    // 截断长度由 chat 侧控制（如 200 字），仅保留语义要点，兼顾统计价值与存储成本
    private String questionDigest;

    /** 回答摘要（截断） */
    private String answerDigest;

    /** LLM-as-Judge 评分（1-5） */
    // 1-5 档位与用户反馈的 score 对齐，便于把"机器评分"与"用户反馈"放在同一把尺子上对比
    private Integer llmScore;

    /** 评分状态：SUCCESS/FAILED/SKIPPED */
    // Judge 是独立 LLM 调用，也会失败/超时；三态记录让统计侧知道缺失分数的原因，而非静默丢样本
    private String judgeStatus;

    /** 评分失败摘要 */
    private String errorSummary;
}
