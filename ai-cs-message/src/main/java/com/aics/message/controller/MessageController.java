package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.entity.ChatMessage;
import com.aics.message.entity.ChatSession;
import com.aics.message.producer.ChatMessageProducer;
import com.aics.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息控制器
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：对外暴露会话与聊天消息的 REST 接口，包括：
 * <ul>
 *     <li>发送消息（经 RocketMQ 异步入库）；</li>
 *     <li>创建会话；</li>
 *     <li>按数据库 sessionId 查询会话历史消息；</li>
 *     <li>按 sessionKey（chat 模块字符串会话标识）查询会话历史消息；</li>
 *     <li>查询用户会话列表。</li>
 * </ul>
 * 关键协作：发送走 {@link ChatMessageProducer} → RocketMQ → Consumer 落库；
 * 查询直接走 {@link MessageService} 读取 DB。
 * 统一返回 {@link Result} 包装结构。
 * </p>
 */
@Tag(name = "消息管理")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
@Validated
public class MessageController {

    /** 消息服务，处理会话/消息的查询与创建 */
    private final MessageService messageService;

    /** 聊天消息生产者，用于异步投递消息到 RocketMQ */
    private final ChatMessageProducer chatMessageProducer;

    /**
     * 发送消息：不直接入库，而是投递到 RocketMQ，由消费者异步落库。
     * <p>
     * 设计为异步发送以解耦发送方与持久化逻辑，提升接口响应速度，
     * 同时在 DB 故障时不会阻塞聊天链路。</p>
     *
     * @param message 待发送的聊天消息
     * @return 空结果包装
     */
    @Operation(summary = "发送消息（通过RocketMQ）")
    @PostMapping("/send")
    public Result<Void> sendMessage(@RequestBody ChatMessage message) {
        chatMessageProducer.send(message);
        return Result.success();
    }

    /**
     * 创建会话：默认渠道 web、状态 1（进行中），见 {@link MessageServiceImpl#createSession}。
     *
     * @param userId 用户ID
     * @param title  会话标题
     * @return 创建后的会话信息（含自增主键 id）
     */
    @Operation(summary = "创建会话")
    @PostMapping("/session")
    public Result<ChatSession> createSession(@RequestParam("userId") Long userId,
                                              @RequestParam("title") String title) {
        ChatSession session = messageService.createSession(userId, title);
        return Result.success(session);
    }

    /**
     * 按数据库 sessionId 查询该会话的历史消息（按创建时间升序）。
     *
     * @param sessionId 会话主键ID
     * @return 消息列表
     */
    @Operation(summary = "获取会话消息列表")
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<ChatMessage>> getSessionMessages(@PathVariable("sessionId") Long sessionId) {
        List<ChatMessage> messages = messageService.getSessionMessages(sessionId);
        return Result.success(messages);
    }

    /**
     * 按 sessionKey 查询会话历史消息：用于跨服务对齐 chat 模块的字符串会话标识。
     * <p>
     * chat 模块使用字符串 sessionKey 维护会话上下文，message 模块通过同一 sessionKey
     * 关联落库的消息，便于前端按相同标识拉取历史记录。
     * 默认上限见 {@link MessageServiceImpl#getMessagesBySessionKey}（200 条）。
     * </p>
     *
     * @param sessionKey chat 模块使用的会话标识
     * @return 消息列表（按创建时间升序）
     */
    @Operation(summary = "获取会话消息列表（按 sessionKey）")
    @GetMapping("/session-key/{sessionKey}/messages")
    public Result<List<ChatMessage>> getMessagesBySessionKey(@PathVariable("sessionKey") String sessionKey) {
        List<ChatMessage> messages = messageService.getMessagesBySessionKey(sessionKey);
        return Result.success(messages);
    }

    /**
     * 查询某用户的全部会话列表（按更新时间倒序），用于会话侧边栏展示。
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    @Operation(summary = "获取用户会话列表")
    @GetMapping("/sessions")
    public Result<List<ChatSession>> getUserSessions(@RequestParam("userId") Long userId) {
        List<ChatSession> sessions = messageService.getUserSessions(userId);
        return Result.success(sessions);
    }
}