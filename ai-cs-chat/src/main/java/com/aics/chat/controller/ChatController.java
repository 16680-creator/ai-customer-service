package com.aics.chat.controller;

import com.aics.chat.dto.ChatHistoryMessage;
import com.aics.chat.dto.ChatRagResponseDTO;
import com.aics.chat.service.ChatService;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI 对话控制器
 *
 * <p>对外暴露 5 个对话端点，分别对应不同的使用场景：</p>
 * <ul>
 *   <li>{@code POST /chat/send}     —— 普通同步对话，一次性返回完整回复（适合小程序/轻量调用）。</li>
 *   <li>{@code POST /chat/rag}      —— RAG 对话，返回"回答 + 引用溯源"列表（前端展示依据）。</li>
 *   <li>{@code POST /chat/stream}   —— 流式对话占位接口（仅返回提示，真正的流走 SSE）。</li>
 *   <li>{@code POST /chat/stream/sse} —— SSE 流式对话，逐 token 推送，打字机效果（推荐前端使用）。</li>
 *   <li>{@code GET  /chat/history}  —— 拉取历史会话消息，用于历史回看。</li>
 * </ul>
 *
 * <p>用户身份：所有需要识别用户的接口从请求头 {@code X-User-Id} 读取（由网关鉴权后透传），
 * 写入 {@link ChatUserContext}（ThreadLocal）供下游 Tool Calling 使用，
 * 在 {@code finally} 块中清理避免线程复用导致的脏数据。</p>
 */
@Tag(name = "AI对话")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送对话消息（普通同步对话）。
     *
     * <p>整条调用链：Controller → ChatServiceImpl → ResilientAiService（带超时/重试/熔断）→ DeepSeek。
     * 历史会话超阈值时自动压缩；本接口不参与 RAG 检索。</p>
     *
     * @param sessionId 会话 ID（用于历史隔离与持久化）
     * @param message   用户消息内容
     * @param userId    用户 ID，从 {@code X-User-Id} 请求头读取（可空）
     * @return AI 完整回复文本
     */
    @Operation(summary = "发送对话消息")
    @PostMapping("/send")
    public Result<String> chat(@RequestParam("sessionId") @NotBlank(message = "会话ID不能为空") String sessionId,
                               @RequestParam("message") @NotBlank(message = "消息内容不能为空") String message,
                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            // 把用户 ID 放进 ThreadLocal，供 @Tool 方法（订单查询）读取当前用户
            ChatUserContext.setUserId(userId);
            return chatService.chat(sessionId, message);
        } finally {
            // 必须清理：Tomcat 线程被复用，否则下个请求会读到上个用户的 ID
            ChatUserContext.clear();
        }
    }

    /**
     * RAG 对话（带知识库引用溯源）。
     *
     * <p>调用链：先在指定知识库做两阶段检索（向量宽召回 + Rerank 精排），
     * 再把命中的资料片段拼成【资料】上下文喂给 LLM，最后返回回答 + 引用列表。</p>
     *
     * @param sessionId     会话 ID
     * @param message       用户消息
     * @param knowledgeBase 知识库标识（如 {@code "product-manual"}）
     * @param userId        用户 ID，从 {@code X-User-Id} 读取（可空）
     * @return 回答正文 + 引用来源列表
     */
    @Operation(summary = "RAG对话")
    @PostMapping("/rag")
    public Result<ChatRagResponseDTO> chatWithRag(@RequestParam("sessionId") @NotBlank(message = "会话ID不能为空") String sessionId,
                                                  @RequestParam("message") @NotBlank(message = "消息内容不能为空") String message,
                                                  @RequestParam("knowledgeBase") @NotBlank(message = "知识库标识不能为空") String knowledgeBase,
                                                  @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            ChatUserContext.setUserId(userId);
            return chatService.chatWithRag(sessionId, message, knowledgeBase);
        } finally {
            ChatUserContext.clear();
        }
    }

    /**
     * 流式对话占位接口。
     *
     * <p>真正的流式响应走 SSE 端点 {@link #chatStreamSse}，本接口仅返回提示信息，
     * 提示调用方改用 {@code /chat/stream/sse} 接收 SSE 事件流。</p>
     *
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @return 提示信息 Map
     */
    @Operation(summary = "流式对话")
    @PostMapping("/stream")
    public Result<Map<String, Object>> chatStream(@RequestParam("sessionId") @NotBlank(message = "会话ID不能为空") String sessionId,
                                                   @RequestParam("message") @NotBlank(message = "消息内容不能为空") String message) {
        return chatService.chatStream(sessionId, message);
    }

    /**
     * SSE 流式对话（逐 token 推送，打字机效果）。
     *
     * <p>返回 {@link SseEmitter}，业务层订阅 LLM 的 {@code Flux<String>}，
     * 每个 token 通过 {@code data: {"content":"..."}} 事件推送；流结束时推送
     * {@code {"done":true,"citations":[...]}} 事件（正文已逐 token 推送，不重复携带）。</p>
     *
     * <p>当 {@code knowledgeBase} 非空时走 RAG 流式（带引用溯源），否则走普通流式。</p>
     *
     * @param sessionId     会话 ID
     * @param message       用户消息
     * @param knowledgeBase 知识库标识（可空：空则普通对话，非空则 RAG 对话）
     * @param userId        用户 ID，从 {@code X-User-Id} 读取（可空）
     * @return SSE 发射器，Spring MVC 自动处理 SSE 协议
     */
    @Operation(summary = "SSE 流式对话（逐 token 推送）")
    @PostMapping(value = "/stream/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamSse(@RequestParam("sessionId") @NotBlank(message = "会话ID不能为空") String sessionId,
                                    @RequestParam("message") @NotBlank(message = "消息内容不能为空") String message,
                                    @RequestParam(value = "knowledgeBase", required = false) String knowledgeBase,
                                    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            ChatUserContext.setUserId(userId);
            return chatService.chatStreamSse(sessionId, message, knowledgeBase);
        } finally {
            ChatUserContext.clear();
        }
    }

    /**
     * 查询会话历史（历史回看）。
     *
     * <p>读取顺序：Redis 热缓存优先，未命中时回源 ai-cs-message 服务并重建缓存。</p>
     *
     * @param sessionKey 会话标识
     * @return 按时间升序的历史消息列表
     */
    @Operation(summary = "查询会话历史（历史回看）")
    @GetMapping("/history")
    public Result<List<ChatHistoryMessage>> getHistory(@RequestParam("sessionKey") @NotBlank(message = "会话标识不能为空") String sessionKey) {
        return chatService.getHistory(sessionKey);
    }
}