package com.aics.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户反馈响应 VO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载用户反馈（user_feedback 表）的查询响应，字段与实体对齐，
 * 供按 requestId/时间范围回溯用户反馈使用。
 *
 * <h3>【设计原理】为什么 VO 带 id 而 DTO 不带</h3>
 * <p>id 是数据库生成的主键，只存在于"已持久化"的查询结果中，
 * 写入侧（DTO）不感知 id——这正是"响应 VO 与请求 DTO 分离"的典型收益：
 * 两类契约各自只暴露自己需要的信息，互不越界。</p>
 * </p>
 */
@Data
@Schema(description = "用户反馈响应")
public class UserFeedbackVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "请求ID（未知时为 NULL）")
    private String requestId;

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "反馈类型：LIKE/DISLIKE")
    private String feedbackType;

    @Schema(description = "评分（1-5，可选）")
    private Integer score;

    @Schema(description = "补充文本")
    private String comment;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
