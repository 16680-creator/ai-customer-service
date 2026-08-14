package com.aics.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户反馈实体（对齐 user_feedback 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载用户对 LLM 回答的显式反馈（点赞/点踩/评分/补充文本），是「LLM 可观测性、
 * 评估与成本治理」中质量评估的用户侧信号，可与 llm_trace.requestId 关联溯源。
 * 关键字段：{@link #feedbackType}（LIKE/DISLIKE）、{@link #score}（1-5，可选）。
 *
 * <h3>【设计原理】为什么 requestId 允许为 NULL</h3>
 * <p>用户反馈可能发生在 trace 未建立或请求来源未知的场景（如前端独立点赞组件），
 * 若强制要求 requestId 存在会丢失这类真实信号；因此 saveFeedback 不校验存在性，
 * requestId 只作为"可溯源"的软关联，缺失时也能照常入库。</p>
 *
 * <h3>【设计原理】为什么反馈类型用可读字符串而非数字枚举</h3>
 * <p>LIKE/DISLIKE 这类短枚举用字符串存储，日志、排查与 API 契约都自解释，
 * 免去"1 是什么、2 是什么"的映射心智负担；代价是存储略大，对低频反馈数据可接受。</p>
 * </p>
 */
@Data
@TableName("user_feedback")
public class UserFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO) // 反馈记录无业务主键，由数据库自增生成
    private Long id;

    /** 请求ID（未知时为 NULL） */
    private String requestId;

    /** 会话ID */
    private Long sessionId;

    /** 用户ID */
    private Long userId;

    /** 反馈类型：LIKE/DISLIKE */
    private String feedbackType; // 可读字符串枚举：日志/排查/API 自解释，无需数字映射表

    /** 评分（1-5，可选） */
    private Integer score;

    /** 补充文本 */
    private String comment;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
