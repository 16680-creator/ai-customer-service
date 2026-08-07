package com.aics.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息实体（对齐 chat_message 表）
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

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}