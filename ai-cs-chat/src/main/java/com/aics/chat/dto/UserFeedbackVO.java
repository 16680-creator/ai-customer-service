package com.aics.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户反馈查询 VO（chat 侧，与 ai-cs-message 的 UserFeedbackVO 对齐）
 *
 * <p>与 DTO 的差异仅在于多出服务端生成的 id / createTime：反馈记录的主键与落库时间
 * 由 message 侧生成，回读后用于列表展示与按时间过滤。读写契约分离，见
 * {@link LlmTraceDTO} 类注释中的 DTO/VO 分离说明。</p>
 */
@Data
public class UserFeedbackVO {

    /** 主键 */
    // 服务端自增主键（VO 独有）：用于前端列表 key 与后续"删除/追评"等按 id 操作
    private Long id;

    /** 请求 ID */
    private String requestId;

    /** 会话 ID */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 反馈类型：LIKE/DISLIKE */
    private String feedbackType;

    /** 评分（1-5，可选） */
    private Integer score;

    /** 补充文本 */
    private String comment;

    /** 创建时间 */
    // 反馈时间即用户"体验发生时刻"的近似，时间窗口统计（如近 7 天点赞率）以它为准
    private LocalDateTime createTime;
}
