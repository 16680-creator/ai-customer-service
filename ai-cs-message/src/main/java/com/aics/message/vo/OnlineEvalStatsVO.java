package com.aics.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 线上评估与反馈统计响应 VO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载指定时间范围内的线上评估与用户反馈统计：评估样本数、评分成功数与平均分、
 * 反馈总数与点赞/点踩数，供质量看板使用。
 *
 * <h3>【设计原理】为什么 avgLlmScore 用 Double 且可为 null</h3>
 * <p>平均分 = 分数总和 / 评分成功数，当成功数为 0 时"无样本可评"，
 * 返回 null（而非 0.0）语义更准确：0.0 会被误读为"平均 0 分（很差）"，
 * null 表达"暂无数据"；看板据此展示占位符而非误导性的低分。</p>
 *
 * <h3>【设计原理】为什么评估与反馈指标合并在一个 VO</h3>
 * <p>两者同属"质量看板"视图、同按时间范围聚合，一次接口返回全部指标
 * 减少前端多次调用与时间窗口不一致问题；内部仍是两张表独立统计（见 OnlineEvalServiceImpl）。</p>
 * </p>
 */
@Data
@Schema(description = "线上评估与反馈统计响应")
public class OnlineEvalStatsVO {

    @Schema(description = "评估样本数")
    private Long sampleCount; // 时间范围内全部评估记录数（含 FAILED/SKIPPED）

    @Schema(description = "评分成功数（judgeStatus=SUCCESS）")
    private Long scoredCount;

    @Schema(description = "平均分（评分成功数为 0 时为 null）")
    private Double avgLlmScore;

    @Schema(description = "反馈总数")
    private Long feedbackCount;

    @Schema(description = "点赞数（feedbackType=LIKE）")
    private Long likeCount;

    @Schema(description = "点踩数（feedbackType=DISLIKE）")
    private Long dislikeCount;
}
