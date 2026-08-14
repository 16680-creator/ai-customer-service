package com.aics.message.service;

import com.aics.message.entity.ChatMessage;
import com.aics.message.entity.ChatSession;

import java.util.List;

/**
 * 消息服务接口
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：定义会话与聊天消息的核心业务能力，包括消息保存、会话创建、
 * 历史消息查询（按 sessionId 或 sessionKey）以及用户会话列表查询。
 * 实现类：{@link com.aics.message.service.impl.MessageServiceImpl}。
 * 调用方：{@link com.aics.message.controller.MessageController}（同步查询/创建）
 * 与 {@link com.aics.message.consumer.ChatMessageConsumer}（异步消费落库）。
 * </p>
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
     * 按会话标识 sessionKey 获取消息列表（按创建时间升序）
     *
     * @param sessionKey 会话标识
     * @return 消息列表
     */
    List<ChatMessage> getMessagesBySessionKey(String sessionKey);

    /**
     * 获取用户会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> getUserSessions(Long userId);

    /**
     * 删除会话（逻辑删除会话记录，物理删除其下所有消息）
     *
     * @param sessionId 会话ID
     */
    void deleteSession(Long sessionId);
}