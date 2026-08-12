package com.aics.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天会话实体（对齐 chat_session 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载一次会话（用户与 AI/客服之间的一段对话）的元数据，
 * 是 {@link ChatMessage} 的父级聚合。一个会话对应多条消息（1:N）。
 * 关键字段：{@link #userId}（归属用户）、{@link #agentId}（归属客服，可空）、
 * {@link #channel}（接入渠道）、{@link #status}（会话状态机）。
 * 支持逻辑删除（{@link #deleted}），便于审计与历史追溯。
 * </p>
 */
@Data
@TableName("chat_session")
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 客服ID */
    private Long agentId;

    /** 渠道：web/app/wechat/api */
    private String channel;

    /** 状态：0-已结束 1-进行中 2-转人工 */
    private Integer status;

    /** 会话标题 */
    private String title;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间：由 MetaObjectHandler 在插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}