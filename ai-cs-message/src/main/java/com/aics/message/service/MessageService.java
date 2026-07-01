package com.aics.message.service;

import com.aics.message.entity.ChatMessage;
import com.aics.message.entity.ChatSession;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {

    /**
     * 保存消息
     *
     * @param message 消息信息
     */
    void saveMessage(ChatMessage message);

    /**
     * 创建会话
     *
     * @param userId 用户ID
     * @param title  会话标题
     * @return 会话信息
     */
    ChatSession createSession(Long userId, String title);

    /**
     * 获取会话消息列表
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> getSessionMessages(Long sessionId);

    /**
     * 获取用户会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> getUserSessions(Long userId);
}
