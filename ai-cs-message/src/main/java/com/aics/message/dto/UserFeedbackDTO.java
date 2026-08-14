package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户反馈请求 DTO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载上报用户反馈（user_feedback 表）的入参，字段与实体对齐。
 * feedbackType 必填（LIKE/DISLIKE）；requestId 未知时可传 null（照常插入，不校验存在性）；
 * score 为 1-5（可选）。
 *
 * <h3>【设计原理】为什么 requestId 不做存在性校验（保持可空）</h3>
 * <p>反馈是用户侧独立信号：可能来自 trace 未建立、请求来源未知的场景，强制要求
 * requestId 存在会丢弃真实反馈；因此本 DTO 的 requestId 无任何校验注解，
 * 服务层 saveFeedback 也直接落库，不做跨表存在性查询（与"强一致性"的 Agent 轨迹语义区分）。</p>
 * </p>
 */
@Data
@Schema(description = "用户反馈请求")
public class UserFeedbackDTO {

    @Schema(description = "请求ID（未知时为 NULL）", example = "trace-uuid-001")
    private String requestId; // 软关联：可为 null，仅用于"按请求回溯反馈"

    @Schema(description = "会话ID", example = "1001")
    private Long sessionId;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "反馈类型：LIKE/DISLIKE", example = "LIKE")
    @NotBlank(message = "反馈类型不能为空")
    private String feedbackType;

    @Schema(description = "评分（1-5，可选）", example = "4")
    private Integer score;

    @Schema(description = "补充文本", example = "回答很清晰")
    private String comment;
}
