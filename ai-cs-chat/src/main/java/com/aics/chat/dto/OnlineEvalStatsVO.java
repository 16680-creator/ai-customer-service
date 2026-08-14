package com.aics.chat.dto;

import lombok.Data;

/**
 * 线上评估与反馈统计 VO（chat 侧，与 ai-cs-message 的 OnlineEvalStatsVO 对齐）
 *
 * <h3>【AI 技术详解】为什么把"机器评分"与"用户反馈"放在同一个统计里？</h3>
 * <p>两者互补：LLM-as-Judge 覆盖全量采样（机器视角、客观但未必贴合真实体验），
 * 用户反馈只有少数人主动提交（真实视角、稀疏但金贵）。同一页面并排展示可发现
 * 系统性偏差——例如机器评分高但用户点踩多，说明 Judge 标准与用户体验脱节。</p>
 */
@Data
public class OnlineEvalStatsVO {

    /** 评估样本数 */
    private Long sampleCount;

    /** 评分成功数 */
    private Long scoredCount;

    /** 平均分（评分成功数为 0 时为 null） */
    // 用 null 而非 0 表示"无评分样本"：0 会被误读为"全部 0 分"，null 让前端明确显示"暂无数据"
    private Double avgLlmScore;

    /** 反馈总数 */
    private Long feedbackCount;

    /** 点赞数 */
    // 点赞率 = likeCount / feedbackCount，可作回答质量的在线代理指标（无需等离线评测）
    private Long likeCount;

    /** 点踩数 */
    private Long dislikeCount;
}
