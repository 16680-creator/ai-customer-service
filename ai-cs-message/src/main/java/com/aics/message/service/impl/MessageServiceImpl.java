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
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：实现 {@link MessageService}，负责会话与消息的数据库读写。
 * 持久化策略说明：
 * <ul>
 *     <li>当前为单写 MySQL（通过 MyBatis-Plus Mapper），未引入 Redis 缓存双写；
 *     若后续需要高频读取会话历史，可在此层叠加 Redis 缓存，但需注意缓存一致性。</li>
 *     <li>消息写入入口为 {@link #saveMessage}，由 RocketMQ 消费者调用，支持异步落库。</li>
 *     <li>会话历史支持两种查询维度：数据库主键 sessionId 与跨服务字符串 sessionKey。</li>
 * </ul>
 * 关键协作：依赖 {@link ChatMessageMapper}、{@link ChatSessionMapper} 完成数据访问。
 * </p>

 * <h3>学习要点（技术：异步落库 / 缓存一致性）</h3>
 * <ul>
 *   <li><b>写入路径</b>：AI 对话在 chat 服务发 RocketMQ，本服务消费者落 MySQL，
 *       与对话主流程解耦；chat 侧另有 Redis 热缓存加速历史读取。</li>
 *   <li><b>双维度查询</b>：支持 sessionId（DB 主键）与跨服务 sessionKey 两种查询，
 *       兼容 chat 的字符串会话标识。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    /** 聊天消息 Mapper，负责 chat_message 表读写 */
    private final ChatMessageMapper chatMessageMapper;
    /** 聊天会话 Mapper，负责 chat_session 表读写 */
    private final ChatSessionMapper chatSessionMapper;

    /**
     * 保存聊天消息：直接 insert 到 chat_message 表。
     * <p>
     * 由 {@link com.aics.message.consumer.ChatMessageConsumer#onMessage} 在消费 RocketMQ 消息时调用，
     * 因此本方法为异步链路的最终落库点。createTime 由 MyBatis-Plus 自动填充，无需手动设置。
     * </p>
     *
     * @param message 待保存的聊天消息
     */
    @Override
    public void saveMessage(ChatMessage message) {
        log.info("保存聊天消息: sessionId={}, sessionKey={}, role={}", message.getSessionId(), message.getSessionKey(), message.getRole());
        chatMessageMapper.insert(message);
        log.info("聊天消息保存成功: id={}", message.getId());
    }

    /**
     * 创建会话：默认渠道 web、状态 1（进行中）。
     * <p>
     * createTime/updateTime 由 MyBatis-Plus 自动填充；
     * 返回的 session 已包含自增主键 id，可供调用方立即使用。
     * </p>
     *
     * @param userId 用户ID
     * @param title  会话标题
     * @return 已持久化的会话对象（含主键）
     */
    @Override
    public ChatSession createSession(Long userId, String title) {
        log.info("创建会话: userId={}, title={}", userId, title);
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setStatus(1);
        session.setChannel("web");
        chatSessionMapper.insert(session);
        log.info("会话创建成功: id={}", session.getId());
        return session;
    }

    /**
     * 按数据库 sessionId 查询会话历史消息，按创建时间升序返回。
     * <p>
     * 使用 LambdaQueryWrapper 构造条件，无分页，适用于会话消息量可控的场景；
     * 若会话过长建议后续改为分页查询。
     * </p>
     *
     * @param sessionId 会话主键ID
     * @return 消息列表（按时间升序）
     */
    @Override
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        log.info("获取会话消息列表: sessionId={}", sessionId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(wrapper);
    }

    /**
     * 按 sessionKey 查询会话历史消息，复用 {@link ChatMessageMapper#selectBySessionKey}。
     * <p>
     * sessionKey 是 chat 模块使用的字符串会话标识，用于跨服务对齐会话上下文。
     * 当前上限固定为 200 条，按创建时间升序返回，覆盖一般会话长度。
     * </p>
     *
     * @param sessionKey chat 模块使用的会话标识
     * @return 消息列表（按时间升序）
     */
    @Override
    public List<ChatMessage> getMessagesBySessionKey(String sessionKey) {
        log.info("获取会话消息列表: sessionKey={}", sessionKey);
        return chatMessageMapper.selectBySessionKey(sessionKey, 200);
    }

    /**
     * 查询某用户的全部会话，按 updateTime 倒序返回（最近活跃优先）。
     * <p>
     * 由于 ChatSession 配置了 {@code @TableLogic}，MyBatis-Plus 会自动过滤已逻辑删除的记录。
     * </p>
     *
     * @param userId 用户ID
     * @return 会话列表（按 updateTime 倒序）
     */
    @Override
    public List<ChatSession> getUserSessions(Long userId) {
        log.info("获取用户会话列表: userId={}", userId);
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime);
        return chatSessionMapper.selectList(wrapper);
    }

    /**
     * 删除会话：逻辑删除 chat_session 记录，物理删除其下所有消息。
     * <p>
     * 消息表无逻辑删除字段，因此直接物理删除；
     * 会话通过 {@code @TableLogic} 自动转为 UPDATE deleted=1。
     * 删除条件同时覆盖 session_id 与 session_key 两个维度，
     * 兼容历史遗留（仅 session_key 有值）的消息记录。
     * </p>
     */
    @Override
    public void deleteSession(Long sessionId) {
        log.info("删除会话: sessionId={}", sessionId);
        // 1. 删除会话下所有消息（物理删除）
        LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessage::getSessionId, sessionId)
                .or()
                .eq(ChatMessage::getSessionKey, String.valueOf(sessionId));
        int deletedMsg = chatMessageMapper.delete(msgWrapper);
        // 2. 逻辑删除会话
        int deletedSession = chatSessionMapper.deleteById(sessionId);
        log.info("会话删除完成: sessionId={}, deletedMsg={}, deletedSession={}",
                sessionId, deletedMsg, deletedSession);
    }
}