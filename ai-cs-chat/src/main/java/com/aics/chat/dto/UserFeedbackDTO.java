package com.aics.chat.dto;

import lombok.Data;

/**
 * 用户反馈上报 DTO（chat 侧，与 ai-cs-message 的 UserFeedbackDTO 对齐）
 *
 * <h3>【AI 技术详解】requestId 为什么可空？</h3>
 * <p>用户反馈发生在对话结束后的任意时刻，前端可能已经丢失当时的 requestId（页面刷新、
 * 会话切换等）。若强制要求 requestId 非空，反馈会因 trace 缺失而丢——用户主动反馈
 * 是比 trace 更珍贵的数据，因此约定 requestId 未知时照常插入（按 sessionId 关联）。</p>
 */
@Data
public class UserFeedbackDTO {

    /** 请求 ID（未知时为 null，照常插入） */
    private String requestId;

    /** 会话 ID */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 反馈类型：LIKE/DISLIKE */
    // 枚举值以字符串传输而非数字：跨服务接口可读性好，新增类型（如 NEUTRAL）无需迁移旧数据
    private String feedbackType;

    /** 评分（1-5，可选） */
    // 与 Judge 评分同尺度：便于后续做"用户评分 vs 机器评分"的一致性分析
    private Integer score;

    /** 补充文本 */
    private String comment;
}
