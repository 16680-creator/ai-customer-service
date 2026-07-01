package com.aics.chat.controller;

import com.aics.chat.service.ChatService;
import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 对话控制器
 */
@Tag(name = "AI对话")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "发送对话消息")
    @PostMapping("/send")
    public Result<String> chat(@RequestParam @NotBlank(message = "会话ID不能为空") String sessionId,
                               @RequestParam @NotBlank(message = "消息内容不能为空") String message) {
        return chatService.chat(sessionId, message);
    }

    @Operation(summary = "RAG对话")
    @PostMapping("/rag")
    public Result<String> chatWithRag(@RequestParam @NotBlank(message = "会话ID不能为空") String sessionId,
                                      @RequestParam @NotBlank(message = "消息内容不能为空") String message,
                                      @RequestParam @NotBlank(message = "知识库标识不能为空") String knowledgeBase) {
        return chatService.chatWithRag(sessionId, message, knowledgeBase);
    }

    @Operation(summary = "流式对话")
    @PostMapping("/stream")
    public Result<Map<String, Object>> chatStream(@RequestParam @NotBlank(message = "会话ID不能为空") String sessionId,
                                                   @RequestParam @NotBlank(message = "消息内容不能为空") String message) {
        return chatService.chatStream(sessionId, message);
    }
}
