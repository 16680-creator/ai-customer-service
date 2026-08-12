package com.aics.chat.service.impl;

import com.aics.chat.dto.ChatHistoryMessage;
import com.aics.chat.dto.ChatRagResponseDTO;
import com.aics.chat.dto.CitationItemDTO;
import com.aics.chat.service.ChatHistoryService;
import com.aics.chat.service.ChatService;
import com.aics.chat.service.KnowledgeBaseService;
import com.aics.chat.rag.retrieve.HybridRetriever;
import com.aics.chat.rag.retrieve.RetrievalMode;
import com.aics.chat.rag.retrieve.RetrieveResult;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * AI 对话服务实现
 *
 * <p>所有 LLM 调用均通过 {@link ResilientAiService} 执行，获得超时/重试/熔断/降级能力。</p>
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li>会话历史管理：从 {@link ChatHistoryService} 加载历史；超阈值时调用 LLM 压缩为摘要。</li>
 *   <li>RAG 检索编排：调用 {@link KnowledgeBaseService#search} 拿到相关片段，
 *       用 {@link KnowledgeBaseService#buildContext} 拼成【资料】上下文注入 Prompt。</li>
 *   <li>引用溯源：从检索结果构建 {@link CitationItemDTO} 列表，随回答一起返回前端。</li>
 *   <li>SSE 流式：通过 {@link ResilientAiService#callSseStream} 拿到 {@code Flux<String>}，
 *       订阅后逐 token 推送给 {@link SseEmitter}；流结束推送 done 事件（含完整回复 + citations）。</li>
 *   <li>异常友好化：把 Resilience4j 抛出的超时/熔断异常转成用户可读提示。</li>
 * </ul>

 * <h3>学习要点（技术：SSE 流式 / RAG 多轮 / 历史压缩 / 工具调用）</h3>
 * <ul>
 *   <li><b>SSE 流式</b>：Flux 逐 token 推送给 SseEmitter（打字机效果）；RAG 模式先检索再流式，
 *       结束后用 done 事件补发引用溯源。流一旦开始不可重放，因此流式不配重试。</li>
 *   <li><b>多轮记忆</b>：历史存 Redis 热缓存 + RocketMQ到MySQL（ai-cs-message），
 *       超 20 条用 LLM 压缩为摘要（保留最近 10 条），避免上下文爆炸。</li>
 *   <li><b>用户身份注入</b>：网关把 X-User-Id 透传进来，写入 SystemMessage，
 *       让订单查询等工具在异步线程中也能拿到当前用户（ThreadLocal 在异步不可用）。</li>
 *   <li><b>思考标签过滤</b>：cleanResponse 去掉模型输出中的 &lt;think&gt; 过程，只留正式回答。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ResilientAiService resilientAiService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatHistoryService chatHistoryService;
    private final HybridRetriever hybridRetriever;

    /** 最大历史消息数，超过时触发压缩 */
    private static final int MAX_HISTORY_SIZE = 20;

    /** 压缩后保留的最近消息数 */
    private static final int KEEP_RECENT_SIZE = 10;

    /** SSE 超时时间：5 分钟（与 TimeLimiter 60s 配合，Limiter 先触发，Emiter 兜底） */
    private static final long SSE_EMITTER_TIMEOUT = 5 * 60 * 1000L;

    /** 过滤模型思考过程标签 */
    private static final Pattern THINK_PATTERN = Pattern.compile(" thinking.*? response", Pattern.DOTALL);

    /**
     * 清除 AI 回复中的思考过程标签
     */
    private String cleanResponse(String response) {
        if (response == null) return "";
        return THINK_PATTERN.matcher(response).replaceAll("").trim();
    }

    /**
     * 将当前登录用户身份注入消息上下文头部，供 Tool Calling 获取用户ID。
     * <p>SSE 流式链路中 LLM 调用与 Tool 回调在异步线程执行，ThreadLocal 不可用，
     * 因此把 userId 写入 SystemMessage，保证订单查询等用户维度工具能拿到正确身份。</p>
     */
    private void injectUserContext(List<Message> history) {
        Long userId = ChatUserContext.getUserId();
        if (userId != null) {
            history.add(0, new SystemMessage(
                    "当前登录用户ID为 " + userId + "，调用订单查询等工具时必须使用该ID（纯数字）作为参数。"));
        }
    }

    /**
     * 将持久化历史 DTO 转换为 Spring AI Message 列表
     */
    private List<Message> toSpringMessages(List<ChatHistoryMessage> history) {
        List<Message> messages = new ArrayList<>(history.size());
        for (ChatHistoryMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return messages;
    }

    /**
     * 压缩会话历史：将旧消息交给 AI 生成摘要，替换为一条 SystemMessage
     * 保留最近的 KEEP_RECENT_SIZE 条消息
     */
    private List<Message> compressHistory(List<Message> history) {
        int splitIndex = history.size() - KEEP_RECENT_SIZE;
        List<Message> oldMessages = history.subList(0, splitIndex);
        List<Message> recentMessages = new ArrayList<>(history.subList(splitIndex, history.size()));

        // 拼接旧消息为文本
        StringBuilder conversation = new StringBuilder();
        for (Message msg : oldMessages) {
            String role = msg instanceof UserMessage ? "用户" : "助手";
            conversation.append(role).append("：").append(msg.getText()).append("\n");
        }

        try {
            // 通过 ResilientAiService 调用 AI 生成摘要（带超时/重试/熔断）
            String summary = resilientAiService.callSummary(
                    new Prompt("请将以下对话历史压缩为简洁的摘要，保留关键信息（用户名、订单号、重要决定等），"
                            + "用1-3句话概括，作为后续对话的上下文参考：\n\n" + conversation)
            ).get();

            summary = cleanResponse(summary);
            log.info("会话历史压缩完成: {}条消息 -> 摘要({}字)", oldMessages.size(), summary.length());

            // 构建压缩后的历史：摘要 + 最近消息
            List<Message> compressed = new ArrayList<>();
            compressed.add(new SystemMessage("以下是之前对话的摘要，请参考：\n" + summary));
            compressed.addAll(recentMessages);
            return compressed;
        } catch (Exception e) {
            log.warn("会话压缩失败，回退为截断模式", e);
            return recentMessages;
        }
    }

    /**
     * 普通同步对话：加载历史 → 追加用户消息 → 压缩历史（若超阈值）→ 调用 LLM → 清洗 → 落库。
     */
    @Override
    public Result<String> chat(String sessionId, String message) {
        log.info("对话请求: sessionId={}, message={}", sessionId, message);

        try {
            // 从持久化历史加载（Redis 优先，未命中回源 message 表）
            List<Message> history = toSpringMessages(chatHistoryService.load(sessionId));
            injectUserContext(history);
            history.add(new UserMessage(message));
            chatHistoryService.append(sessionId, "user", message);

            // 历史超过上限时，压缩旧消息为摘要
            if (history.size() > MAX_HISTORY_SIZE) {
                history = compressHistory(history);
            }

            // 通过 ResilientAiService 弹性调用 LLM（超时/重试/熔断）
            String response = resilientAiService.callChat(history).get();

            // 过滤思考过程
            response = cleanResponse(response);

            // 记录 AI 回复到历史
            chatHistoryService.append(sessionId, "assistant", response);
            history.add(new AssistantMessage(response));

            log.info("对话完成: sessionId={}, responseLength={}", sessionId, response.length());
            return Result.success(response);
        } catch (ExecutionException e) {
            // CompletableFuture.get() 抛 ExecutionException，cause 才是真实异常
            Throwable cause = e.getCause();
            log.error("AI 服务调用异常: sessionId={}, cause={}", sessionId, cause != null ? cause.getMessage() : e.getMessage());
            throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + getFriendlyMessage(cause));
        } catch (Exception e) {
            log.error("对话异常: sessionId={}", sessionId, e);
            throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + getFriendlyMessage(e));
        }
    }

    /**
     * RAG 对话：知识库检索 → 拼接【资料】上下文 → 调用 LLM → 构建引用溯源列表。
     */
    @Override
    public Result<ChatRagResponseDTO> chatWithRag(String sessionId, String message, String knowledgeBase) {
        return chatWithRag(sessionId, message, knowledgeBase, false, false);
    }

    /**
     * RAG 对话（支持 Hybrid / 查询改写增强）。
     *
     * @param hybrid  是否启用 ES+向量混合检索（默认 false 保持纯向量）
     * @param rewrite 是否启用查询改写/HyDE（默认 false）
     */
    public Result<ChatRagResponseDTO> chatWithRag(String sessionId, String message, String knowledgeBase,
                                                  boolean hybrid, boolean rewrite) {
        log.info("RAG对话请求: sessionId={}, knowledgeBase={}, hybrid={}, rewrite={}",
                sessionId, knowledgeBase, hybrid, rewrite);

        try {
            // 检索：纯向量 / Hybrid / 改写（增强失败自动降级纯向量）
            List<Document> docs = retrieveRagDocs(knowledgeBase, message, 5, 0.5, hybrid, rewrite);
            String context = knowledgeBaseService.buildContext(docs);

            // 构建 RAG Prompt
            String ragPrompt = """
                    请严格基于下面的【知识库资料】回答用户问题。
                    重要规则：
                    1. 如果资料中没有相关信息，请如实告知："我暂时没有这方面的资料"，不要编造内容
                    2. 回答时优先引用资料中的内容，不要提及"根据资料/检索结果"之类的表述

                    【知识库资料】
                    %s

                    【用户问题】
                    %s
                    """.formatted(context.isBlank() ? "（未检索到相关资料）" : context, message);

            // 通过 ResilientAiService 弹性调用 LLM
            String response = resilientAiService.callRagChat(ragPrompt).get();
            response = cleanResponse(response);

            // 从检索结果构建引用溯源列表（documentId/title/page/score/content）
            List<CitationItemDTO> citations = buildCitations(docs);

            log.info("RAG对话完成: sessionId={}, 检索命中{}条, 引用{}条", sessionId, docs.size(), citations.size());
            return Result.success(new ChatRagResponseDTO(response, citations));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.error("RAG对话异常: sessionId={}, cause={}", sessionId, cause != null ? cause.getMessage() : e.getMessage());
            throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + getFriendlyMessage(cause));
        } catch (Exception e) {
            log.error("RAG对话异常: sessionId={}", sessionId, e);
            throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + getFriendlyMessage(e));
        }
    }

    /**
     * 流式对话占位接口实现：仅返回提示信息，引导调用方改用 SSE 端点。
     */
    @Override
    public Result<Map<String, Object>> chatStream(String sessionId, String message) {
        log.info("流式对话请求: sessionId={}", sessionId);

        try {
            // 该端点仅为占位：真正的流式响应需要 SSE 协议，请改用 /chat/stream/sse
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("status", "streaming");
            result.put("message", "流式对话已启动，请使用 /chat/stream/sse 端点接收 SSE 流式响应");
            return Result.success(result);
        } catch (Exception e) {
            log.error("流式对话异常: sessionId={}", sessionId, e);
            throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * SSE 流式对话：根据 knowledgeBase 是否为空，走 RAG 流式或普通流式；
     * 订阅 Flux 逐 token 推送，流结束推送 done 事件（含完整回复 + citations）。
     */
    @Override
    public SseEmitter chatStreamSse(String sessionId, String message, String knowledgeBase) {
        return chatStreamSse(sessionId, message, knowledgeBase, false, false);
    }

    /**
     * SSE 流式对话（支持 Hybrid / 查询改写增强）。
     */
    public SseEmitter chatStreamSse(String sessionId, String message, String knowledgeBase,
                                    boolean hybrid, boolean rewrite) {
        log.info("SSE流式对话请求: sessionId={}, knowledgeBase={}, hybrid={}, rewrite={}",
                sessionId, knowledgeBase, hybrid, rewrite);
        // 使用合理的超时时间，替代原来的 0L（永不超时）
        SseEmitter emitter = new SseEmitter(SSE_EMITTER_TIMEOUT);
        boolean hasKb = StringUtils.hasText(knowledgeBase);

        try {
            // 1. 维护会话历史
            List<Message> history = toSpringMessages(chatHistoryService.load(sessionId));
            injectUserContext(history);
            history.add(new UserMessage(message));
            chatHistoryService.append(sessionId, "user", message);
            if (history.size() > MAX_HISTORY_SIZE) {
                history = compressHistory(history);
            }

            final List<Message> streamHistory = history;

            // 2. 通过 ResilientAiService 弹性获取流式 Flux
            //    hasKb=true 走 RAG 流式（同时检索知识库 + 缓存 citations 用于 done 事件）；
            //    hasKb=false 走普通流式
            CompletableFuture<Flux<String>> futureFlux;
            final List<CitationItemDTO> citations;
            if (hasKb) {
                List<Document> docs = retrieveRagDocs(knowledgeBase, message, 5, 0.5, hybrid, rewrite);
                String context = knowledgeBaseService.buildContext(docs);
                String ragPrompt = """
                        请严格基于下面的【知识库资料】回答用户问题。
                        重要规则：
                        1. 如果资料中没有相关信息，请如实告知："我暂时没有这方面的资料"，不要编造内容
                        2. 回答时优先引用资料中的内容，不要提及"根据资料/检索结果"之类的表述

                        【知识库资料】
                        %s

                        【用户问题】
                        %s
                        """.formatted(context.isBlank() ? "（未检索到相关资料）" : context, message);
                futureFlux = resilientAiService.callSseRagStream(ragPrompt);
                // 缓存引用溯源，完成事件时随 done 一起推送
                citations = buildCitations(docs);
            } else {
                futureFlux = resilientAiService.callSseStream(streamHistory);
                citations = List.of();
            }

            // 等待 Flux 就绪（受 TimeLimiter 保护，不会无限等待）
            Flux<String> flux = futureFlux.get();

            // 3. 订阅流：逐 token 通过 SSE 推送，结束后更新历史
            //    - onNext：检查是否为降级错误标记 [ERROR]，是则推送 error 事件并结束；
            //             否则把 chunk 累积到 full 并以 {"content":"..."} 推送给前端
            //    - onError：推送 error 事件并以异常结束 emitter
            //    - onComplete：把累积的 full 文本清洗后入库，推送 {"done":true,"content":"...","citations":[...]}
            StringBuilder full = new StringBuilder();
            flux.subscribe(
                    chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            // 检查是否为降级错误标记
                            if (chunk.startsWith("[ERROR]")) {
                                String errorMsg = chunk.substring(7);
                                log.warn("SSE流式降级: sessionId={}, msg={}", sessionId, errorMsg);
                                try {
                                    emitter.send(SseEmitter.event().data(Map.of("error", errorMsg)));
                                } catch (Exception ignore) {
                                    // ignore
                                }
                                emitter.complete();
                                return;
                            }
                            full.append(chunk);
                            try {
                                emitter.send(SseEmitter.event().data(Map.of("content", chunk)));
                            } catch (Exception e) {
                                log.warn("SSE发送失败: sessionId={}, err={}", sessionId, e.getMessage());
                            }
                        }
                    },
                    error -> {
                        log.error("SSE流式对话异常: sessionId={}", sessionId, error);
                        try {
                            emitter.send(SseEmitter.event().data(Map.of("error", String.valueOf(error.getMessage()))));
                        } catch (Exception ignore) {
                            // ignore
                        }
                        emitter.completeWithError(error);
                    },
                    () -> {
                        String response = cleanResponse(full.toString());
                        try {
                            chatHistoryService.append(sessionId, "assistant", response);
                            streamHistory.add(new AssistantMessage(response));
                            // done 事件仅携带引用溯源列表（正文已逐 token 推送，不再重复携带）
                            Map<String, Object> doneEvent = new HashMap<>();
                            doneEvent.put("done", true);
                            doneEvent.put("citations", citations);
                            emitter.send(SseEmitter.event().data(doneEvent));
                        } catch (Exception e) {
                            log.warn("SSE完成事件发送失败: sessionId={}, err={}", sessionId, e.getMessage());
                        }
                        emitter.complete();
                    }
            );
        } catch (ExecutionException e) {
            // futureFlux.get() 抛 ExecutionException，cause 是真实异常（如熔断/超时）
            Throwable cause = e.getCause();
            String errorMsg = getFriendlyMessage(cause);
            log.error("SSE流式初始化异常: sessionId={}, cause={}", sessionId, errorMsg);
            try {
                emitter.send(SseEmitter.event().data(Map.of("error", errorMsg)));
            } catch (Exception ignore) {
                // ignore
            }
            emitter.complete();
        } catch (Exception e) {
            String errorMsg = getFriendlyMessage(e);
            log.error("SSE流式对话初始化异常: sessionId={}", sessionId, e);
            try {
                emitter.send(SseEmitter.event().data(Map.of("error", errorMsg)));
            } catch (Exception ignore) {
                // ignore
            }
            emitter.complete();
        }
        // 设置 SSE 超时回调，超时时自动清理
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: sessionId={}", sessionId);
            emitter.complete();
        });
        emitter.onError(throwable -> {
            log.warn("SSE连接异常: sessionId={}, err={}", sessionId, throwable.getMessage());
            emitter.complete();
        });
        return emitter;
    }

    /**
     * 查询会话历史（历史回看），直接委托 {@link ChatHistoryService#load}。
     */
    @Override
    public Result<List<ChatHistoryMessage>> getHistory(String sessionKey) {
        log.info("查询会话历史: sessionKey={}", sessionKey);
        return Result.success(chatHistoryService.load(sessionKey));
    }

    /**
     * RAG 检索入口：纯向量 / Hybrid / 改写；增强失败自动降级纯向量。
     */
    private List<Document> retrieveRagDocs(String knowledgeBase, String message, int topK, double threshold,
                                           boolean hybrid, boolean rewrite) {
        if (!hybrid && !rewrite) {
            return knowledgeBaseService.search(knowledgeBase, message, topK, threshold);
        }
        RetrievalMode mode = rewrite ? RetrievalMode.HYBRID_QUERY_REWRITE : RetrievalMode.HYBRID;
        try {
            RetrieveResult result = hybridRetriever.retrieve(knowledgeBase, message, mode, topK);
            log.info("RAG增强检索: sessionId=?, mode={}, degraded={}, hits={}",
                    result.getMode(), result.isDegraded(), result.getDocuments().size());
            return result.getDocuments();
        } catch (Exception e) {
            log.warn("RAG增强检索失败，降级纯向量: mode={}, err={}", mode, e.getMessage());
            return knowledgeBaseService.search(knowledgeBase, message, topK, threshold);
        }
    }

    /**
     * 从 RAG 检索结果构建引用溯源列表。
     *
     * <p>字段来源：documentId/title 来自入库时写入的 metadata；
     * page 来自 metadata 的 {@code page_number}（PagePdfDocumentReader 写入）；
     * score 为检索相似度；content 为命中片段原文。</p>
     */
    private List<CitationItemDTO> buildCitations(List<Document> docs) {
        List<CitationItemDTO> citations = new ArrayList<>(docs.size());
        for (Document doc : docs) {
            Map<String, Object> meta = doc.getMetadata();
            CitationItemDTO item = new CitationItemDTO();

            // documentId：入库时写入的是 chunk.getId() 字符串，可转 Long 时转（UUID 等非数字忽略）
            Object docId = meta.get("documentId");
            if (docId != null) {
                try {
                    item.setDocumentId(Long.valueOf(String.valueOf(docId)));
                } catch (NumberFormatException ignore) {
                    // 非数字 ID 不设置
                }
            }

            Object title = meta.get("title");
            if (title != null) {
                item.setTitle(String.valueOf(title));
            }

            Object page = meta.get("page_number");
            if (page instanceof Number pageNum) {
                item.setPage(pageNum.intValue());
            }

            if (doc.getScore() != null) {
                item.setScore(doc.getScore().doubleValue());
            }

            item.setContent(doc.getText());
            citations.add(item);
        }
        return citations;
    }

    /**
     * 将异常转换为友好的用户提示。
     */
    private String getFriendlyMessage(Throwable e) {
        if (e == null) return "未知错误";
        if (e instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            return "AI服务当前负载较高，已被熔断保护，请稍后再试";
        }
        if (e instanceof java.util.concurrent.TimeoutException
                || e.getMessage() != null && e.getMessage().contains("TimeLimiter")
                || e.getMessage() != null && e.getMessage().contains("timeout")) {
            return "AI服务响应超时，请稍后重试";
        }
        String msg = e.getMessage();
        return msg != null ? msg : "AI服务调用异常";
    }
}