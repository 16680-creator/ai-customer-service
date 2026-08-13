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
 * AI 对话服务实现 —— 对话业务的核心编排层。
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
 *
 * <h3>【AI 技术详解】SSE（Server-Sent Events）流式响应</h3>
 * <ul>
 *   <li><b>什么是 SSE</b>：HTTP 单向推送协议，服务端可以持续向客户端发送事件，
 *       适合"打字机效果"的 AI 对话场景（逐 token 推送）</li>
 *   <li><b>与 WebSocket 的区别</b>：
 *       <ul>
 *         <li>SSE：单向（服务端→客户端）、基于 HTTP、自动重连、适合文本推送</li>
 *         <li>WebSocket：双向、独立协议、需要手动重连、适合实时交互</li>
 *       </ul>
 *   </li>
 *   <li><b>Flux<String> 与 SseEmitter</b>：
 *       <ul>
 *         <li>Flux：Spring WebFlux 的响应式流，代表异步数据流</li>
 *         <li>SseEmitter：Spring MVC 的 SSE 发射器，将 Flux 的数据逐个推送给客户端</li>
 *         <li>订阅模式：{@code flux.subscribe(chunk -> emitter.send(chunk))}</li>
 *       </ul>
 *   </li>
 *   <li><b>为什么流式不配重试</b>：流一旦开始推送就不可重放，重试会导致前端重复接收 token</li>
 * </ul>
 *
 * <h3>【AI 技术详解】多轮对话记忆管理</h3>
 * <ul>
 *   <li><b>问题</b>：LLM 是无状态的，每次调用都是独立的，需要把历史消息一起发送</li>
 *   <li><b>存储架构</b>：Redis 热缓存（最近消息）+ MySQL 持久化（全量历史）</li>
 *   <li><b>上下文窗口限制</b>：LLM 有 Token 限制（如 DeepSeek 64K），历史太长会超限</li>
 *   <li><b>压缩策略</b>：超过 20 条消息时，用 LLM 将旧消息压缩为摘要（保留最近 10 条），
 *       摘要作为 SystemMessage 注入，既保留上下文又节省 Token</li>
 *   <li><b>压缩的好处</b>：
 *       <ul>
 *         <li>减少 Token 消耗（省钱）</li>
 *         <li>避免上下文过长导致模型"遗忘"早期信息</li>
 *         <li>保持对话连贯性（摘要保留关键信息）</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】用户身份注入与 ThreadLocal</h3>
 * <ul>
 *   <li><b>问题</b>：Tool Calling 在异步线程执行，ThreadLocal 不可用</li>
 *   <li><b>解决方案</b>：将 userId 写入 SystemMessage，LLM 在调用工具时会携带该信息</li>
 *   <li><b>为什么不用 ThreadLocal</b>：SSE 流式链路中 LLM 调用与 Tool 回调在异步线程执行，
 *       ThreadLocal 在异步线程中无法传递（线程池复用导致数据污染）</li>
 *   <li><b>安全清理</b>：Controller 的 finally 块中清理 ThreadLocal，防止线程复用导致脏数据</li>
 * </ul>
 *
 * <h3>【AI 技术详解】思考标签过滤（Think Tag Filtering）</h3>
 * <ul>
 *   <li><b>问题</b>：某些模型（如 DeepSeek-R1）会在回答前输出 &lt;think&gt;...&lt;/think&gt; 思考过程</li>
 *   <li><b>为什么过滤</b>：思考过程是模型内部推理，不应展示给用户（影响体验、可能泄露 Prompt）</li>
 *   <li><b>实现</b>：正则匹配 {@code <thinking>...</thinking>} 并移除，只保留正式回答</li>
 * </ul>
 *
 * <h3>【技术关联】调用链路图</h3>
 * <pre>
 *   用户请求 → Controller → ChatServiceImpl
 *                               ├── ChatHistoryService.load()    // 加载历史
 *                               ├── compressHistory()            // 压缩历史（超阈值时）
 *                               ├── KnowledgeBaseService.search() // RAG 检索（可选）
 *                               ├── ResilientAiService.callChat() // 调用 LLM
 *                               │       ├── @TimeLimiter         // 超时控制
 *                               │       ├── @Retry               // 重试
 *                               │       ├── @CircuitBreaker      // 熔断
 *                               │       └── ChatClient.call()    // 实际调用
 *                               ├── cleanResponse()              // 过滤思考标签
 *                               └── ChatHistoryService.append()  // 保存回复
 * </pre>
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
     * 【AI 核心】压缩会话历史：将旧消息交给 AI 生成摘要，替换为一条 SystemMessage。
     *
     * <p><b>【AI 技术详解】上下文压缩（Context Compression）</b>：
     * <ul>
     *   <li><b>为什么需要压缩</b>：LLM 有 Token 限制（如 DeepSeek 64K），
     *       历史消息太长会：①超限报错 ②消耗过多 Token（费用高）③模型"遗忘"早期信息</li>
     *   <li><b>压缩策略</b>：
     *       <ol>
     *         <li>保留最近 KEEP_RECENT_SIZE 条消息（保证最近对话不丢失）</li>
     *         <li>将旧消息交给 LLM 生成摘要（1-3 句话概括关键信息）</li>
     *         <li>摘要作为 SystemMessage 注入，既保留上下文又节省 Token</li>
     *       </ol>
     *   </li>
     *   <li><b>降级策略</b>：压缩失败时回退为"截断模式"（只保留最近消息），不阻塞对话</li>
     * </ul>
     *
     * @param history 完整历史消息列表
     * @return 压缩后的消息列表（摘要 + 最近消息）
     */
    private List<Message> compressHistory(List<Message> history) {
        int splitIndex = history.size() - KEEP_RECENT_SIZE;   // 切分点：保留最近 KEEP_RECENT_SIZE 条
        List<Message> oldMessages = history.subList(0, splitIndex);      // 旧消息 -> 交给 LLM 压缩
        List<Message> recentMessages = new ArrayList<>(history.subList(splitIndex, history.size()));  // 最近消息原样保留

        // 拼接旧消息为文本
        StringBuilder conversation = new StringBuilder();
        for (Message msg : oldMessages) {
            String role = msg instanceof UserMessage ? "用户" : "助手";
            conversation.append(role).append("：").append(msg.getText()).append("\n");
        }

        try {
            // 通过 ResilientAiService 调用 AI 生成摘要（带超时/重试/熔断）
            // 调 LLM 压缩旧消息为摘要（经 ResilientAiService 获得超时/重试/熔断保护）
            String summary = resilientAiService.callSummary(
                    new Prompt("请将以下对话历史压缩为简洁的摘要，保留关键信息（用户名、订单号、重要决定等），"
                            + "用1-3句话概括，作为后续对话的上下文参考：\n\n" + conversation)
            ).get();

            summary = cleanResponse(summary);
            log.info("会话历史压缩完成: {}条消息 -> 摘要({}字)", oldMessages.size(), summary.length());

            // 构建压缩后的历史：摘要(SystemMessage) + 最近消息
            List<Message> compressed = new ArrayList<>();
            compressed.add(new SystemMessage("以下是之前对话的摘要，请参考：\n" + summary));
            compressed.addAll(recentMessages);
            return compressed;
        } catch (Exception e) {
            // 压缩失败不阻塞对话：回退为"截断模式"（只保留最近消息）
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
     * 【AI 核心】RAG 对话：知识库检索 → 拼接【资料】上下文 → 调用 LLM → 构建引用溯源列表。
     *
     * <p><b>【AI 技术详解】RAG 对话完整流程</b>：
     * <ol>
     *   <li><b>检索阶段</b>：用户问题 → EmbeddingModel 向量化 → VectorStore 余弦相似度检索
     *       → 返回 Top-K 相关文档片段</li>
     *   <li><b>上下文构建</b>：将检索结果拼成【资料1】【资料2】...格式，注入 Prompt</li>
     *   <li><b>生成阶段</b>：LLM 基于【资料】+【用户问题】生成回答（不凭空编造）</li>
     *   <li><b>引用溯源</b>：从检索结果构建 CitationItemDTO 列表，前端可展示"参考来源"</li>
     * </ol>
     *
     * <p><b>【技术关联】引用溯源（Citation）的价值</b>：
     * <ul>
     *   <li>用户可以验证回答的准确性（点击查看原文）</li>
     *   <li>运营可以追踪哪些知识库内容被频繁引用（优化知识库）</li>
     *   <li>满足合规要求（金融、医疗等领域需要可追溯的信息来源）</li>
     * </ul>
     *
     * @param sessionId     会话 ID
     * @param message       用户消息
     * @param knowledgeBase 知识库标识
     * @return 回答 + 引用溯源列表
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
            // 把命中的知识片段拼成【知识库资料】上下文，注入 Prompt
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

            // 通过 ResilientAiService 弹性调用 LLM（超时/重试/熔断/降级）
            String response = resilientAiService.callRagChat(ragPrompt).get();
            response = cleanResponse(response);   // 去掉模型思考过程标签，只留正式回答

            // 从检索结果构建引用溯源列表（documentId/title/page/score/content），随回答返回前端
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
                            full.append(chunk);   // 累积完整回复，供流结束后入库
                            try {
                                // 逐 token 推送给前端（打字机效果）
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
                        String response = cleanResponse(full.toString());   // 流结束：清洗思考标签
                        try {
                            chatHistoryService.append(sessionId, "assistant", response);   // 落库历史
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