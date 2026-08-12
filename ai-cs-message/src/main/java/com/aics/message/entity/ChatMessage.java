package com.aics.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息实体（对齐 chat_message 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载单条聊天消息的持久化数据，包括用户发言、AI/客服回复等。
 * 与 {@link ChatSession} 的关系：一个会话包含多条消息（1:N），
 * 关联方式有二：数据库主键 {@link #sessionId} 或跨服务字符串标识 {@link #sessionKey}。
 * 设计说明：同时保留 sessionId 与 sessionKey，是为了兼容 chat 模块以字符串
 * sessionKey 维护上下文、而 message 模块以自增主键管理会话的双轨场景。
 * </p>
 */
@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID（数据库会话主键，可为空） */
    private Long sessionId;

    /** 会话标识（chat 服务的字符串会话ID，兼容跨服务会话） */
    @TableField("session_key")
    private String sessionKey;

    /** 角色：user/assistant/agent */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}