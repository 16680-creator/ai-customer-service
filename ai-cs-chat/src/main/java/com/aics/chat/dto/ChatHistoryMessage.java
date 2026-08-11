package com.aics.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话历史消息 DTO
 * 用于 Redis 序列化、回源 message 表后重建，以及历史回看接口返回
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryMessage {

    /** 角色：user/assistant */
    private String role;

    /** 消息内容 */
    private String content;
}