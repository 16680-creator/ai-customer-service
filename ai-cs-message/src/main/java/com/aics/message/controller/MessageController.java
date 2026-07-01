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
 */
@Tag(name = "消息管理")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
@Validated
public class MessageController {

    private final MessageService messageService;
    private final ChatMessageProducer chatMessageProducer;

    @Operation(summary = "发送消息（通过RocketMQ）")
    @PostMapping("/send")
    public Result<Void> sendMessage(@RequestBody ChatMessage message) {
        chatMessageProducer.send(message);
        return Result.success();
    }

    @Operation(summary = "创建会话")
    @PostMapping("/session")
    public Result<ChatSession> createSession(@RequestParam Long userId,
                                              @RequestParam String title) {
        ChatSession session = messageService.createSession(userId, title);
        return Result.success(session);
    }

    @Operation(summary = "获取会话消息列表")
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<ChatMessage>> getSessionMessages(@PathVariable Long sessionId) {
        List<ChatMessage> messages = messageService.getSessionMessages(sessionId);
        return Result.success(messages);
    }

    @Operation(summary = "获取用户会话列表")
    @GetMapping("/sessions")
    public Result<List<ChatSession>> getUserSessions(@RequestParam Long userId) {
        List<ChatSession> sessions = messageService.getUserSessions(userId);
        return Result.success(sessions);
    }
}
