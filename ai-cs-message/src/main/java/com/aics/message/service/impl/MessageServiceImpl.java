package com.aics.message.service.impl;

import com.aics.message.entity.ChatMessage;
import com.aics.message.entity.ChatSession;
import com.aics.message.mapper.ChatMessageMapper;
import com.aics.message.mapper.ChatSessionMapper;
import com.aics.message.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;

    @Override
    public void saveMessage(ChatMessage message) {
        log.info("保存聊天消息: sessionId={}, role={}", message.getSessionId(), message.getRole());
        chatMessageMapper.insert(message);
        log.info("聊天消息保存成功: id={}", message.getId());
    }

    @Override
    public ChatSession createSession(Long userId, String title) {
        log.info("创建会话: userId={}, title={}", userId, title);
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setStatus(1);
        chatSessionMapper.insert(session);
        log.info("会话创建成功: id={}", session.getId());
        return session;
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        log.info("获取会话消息列表: sessionId={}", sessionId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt);
        return chatMessageMapper.selectList(wrapper);
    }

    @Override
    public List<ChatSession> getUserSessions(Long userId) {
        log.info("获取用户会话列表: userId={}", userId);
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdatedAt);
        return chatSessionMapper.selectList(wrapper);
    }
}
