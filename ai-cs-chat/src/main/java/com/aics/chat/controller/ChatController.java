package com.aics.chat.controller;

import com.aics.chat.dto.ChatHistoryMessage;
import com.aics.chat.dto.ChatRagResponseDTO;
import com.aics.chat.dto.VisionChatRequest;
import com.aics.chat.dto.VisionChatResponse;
import com.aics.chat.service.ChatService;
import com.aics.chat.service.VisionChatService;
import com.aics.chat.config.SentinelRules;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.common.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
@Slf4j
@RequestMapping("/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;
    private final VisionChatService visionChatService;
    private final FileStorageService fileStorageService;

    /** 图片对话支持的图片格式 */
    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** 图片大小上限 5MB */
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;

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
    @SentinelResource(value = SentinelRules.RESOURCE_CHAT_SEND, blockHandler = "chatSendBlocked")
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
     * {@code /chat/send} 触发 Sentinel 流控/降级规则时的兜底响应。
     *
     * <p>学习要点：blockHandler 的方法签名必须与原方法一致，且末尾多一个
     * {@link BlockException} 参数——Sentinel 拦截到规则命中后把异常从这里交给业务，
     * 由业务决定限流响应（此处返回 429 语义的业务码，而非直接抛 500）。</p>
     */
    public Result<String> chatSendBlocked(String sessionId, String message, Long userId, BlockException ex) {
        log.warn("对话接口触发Sentinel流控: resource={}, rule={}", SentinelRules.RESOURCE_CHAT_SEND,
                ex.getRule() != null ? ex.getRule().getClass().getSimpleName() : "unknown");
        return Result.fail(ResultCode.TOO_MANY_REQUESTS, "当前对话人数过多，请稍后再试");
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
                                                  @RequestParam(value = "hybrid", defaultValue = "false") boolean hybrid,
                                                  @RequestParam(value = "rewrite", defaultValue = "false") boolean rewrite,
                                                  @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            ChatUserContext.setUserId(userId);
            return chatService.chatWithRag(sessionId, message, knowledgeBase, hybrid, rewrite);
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
                                    @RequestParam(value = "hybrid", defaultValue = "false") boolean hybrid,
                                    @RequestParam(value = "rewrite", defaultValue = "false") boolean rewrite,
                                    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            ChatUserContext.setUserId(userId);
            return chatService.chatStreamSse(sessionId, message, knowledgeBase, hybrid, rewrite);
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

    /**
     * 上传图片（图片对话前置）。
     *
     * <p>复用 {@code ai-cs-common} 的 {@link FileStorageService}（MinIO），目录 {@code chat/images}；
     * 校验格式与大小，与商品图限制保持一致。</p>
     */
    @Operation(summary = "上传图片")
    @PostMapping("/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalName.substring(dotIndex + 1).toLowerCase();
        }
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 jpg/png/webp/gif 格式");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片大小不能超过 5MB");
        }
        String url = fileStorageService.upload(file, "chat/images");
        return Result.success("图片上传成功", url);
    }

    /**
     * 图片对话（多模态图生文）。
     *
     * <p>两段式：视觉模型理解图片 → 描述文本走 RAG 检索 → DeepSeek 回答。
     * 视觉不可用时降级为纯文本对话（有文字）或返回明确提示（仅图片）。</p>
     */
    @Operation(summary = "图片对话")
    @PostMapping("/vision")
    public Result<VisionChatResponse> chatWithVision(@RequestParam("sessionId") @NotBlank(message = "会话ID不能为空") String sessionId,
                                                     @RequestParam("imageUrl") @NotBlank(message = "图片地址不能为空") String imageUrl,
                                                     @RequestParam(value = "message", required = false) String message,
                                                     @RequestParam(value = "knowledgeBase", required = false) String knowledgeBase,
                                                     @RequestParam(value = "hybrid", defaultValue = "false") boolean hybrid,
                                                     @RequestParam(value = "rewrite", defaultValue = "false") boolean rewrite,
                                                     @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            ChatUserContext.setUserId(userId);
            return visionChatService.chatWithVision(buildVisionRequest(sessionId, imageUrl, message, knowledgeBase, hybrid, rewrite));
        } finally {
            ChatUserContext.clear();
        }
    }

    /**
     * 图片对话（SSE 流式，逐 token 推送）。
     */
    @Operation(summary = "图片对话（SSE 流式）")
    @PostMapping(value = "/vision/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithVisionSse(@RequestParam("sessionId") @NotBlank(message = "会话ID不能为空") String sessionId,
                                        @RequestParam("imageUrl") @NotBlank(message = "图片地址不能为空") String imageUrl,
                                        @RequestParam(value = "message", required = false) String message,
                                        @RequestParam(value = "knowledgeBase", required = false) String knowledgeBase,
                                        @RequestParam(value = "hybrid", defaultValue = "false") boolean hybrid,
                                        @RequestParam(value = "rewrite", defaultValue = "false") boolean rewrite,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            ChatUserContext.setUserId(userId);
            return visionChatService.chatWithVisionSse(buildVisionRequest(sessionId, imageUrl, message, knowledgeBase, hybrid, rewrite));
        } finally {
            ChatUserContext.clear();
        }
    }

    /**
     * 组装图片对话请求 DTO。
     */
    private VisionChatRequest buildVisionRequest(String sessionId, String imageUrl, String message,
                                                 String knowledgeBase, boolean hybrid, boolean rewrite) {
        VisionChatRequest request = new VisionChatRequest();
        request.setSessionId(sessionId);
        request.setImageUrl(imageUrl);
        request.setMessage(message);
        request.setKnowledgeBase(knowledgeBase);
        request.setHybrid(hybrid);
        request.setRewrite(rewrite);
        return request;
    }
}