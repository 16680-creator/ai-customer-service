package com.aics.chat.service.impl;

import com.aics.chat.dto.ChatHistoryMessage;
import com.aics.chat.dto.ChatRagResponseDTO;
import com.aics.chat.dto.CitationItemDTO;
import com.aics.chat.modelrouter.ModelScenario;
import com.aics.chat.observability.OnlineEvalService;
import com.aics.chat.observability.TraceContext;
import com.aics.chat.observability.TraceContextHolder;
import com.aics.chat.observability.TraceSpans;
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
import io.micrometer.observation.ObservationRegistry;
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
import java.util.stream.Collectors;

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
    private final ObservationRegistry observationRegistry;
    private final OnlineEvalService onlineEvalService;
    // ===== 3.2 AI 安全网关与 Guardrails：内容安全（F4）/ RAG ACL（F5）/ 审计（F7） =====
    private final com.aics.chat.security.ContentSafetyService contentSafetyService;
    private final com.aics.chat.security.RagAclFilter ragAclFilter;
    private final com.aics.chat.security.SecurityAuditRecorder securityAuditRecorder;
    private final com.aics.chat.prompt.PromptRegistry promptRegistry;

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
     * 输入 Guardrail（3.2 F4）：违规输入返回拦截提示文案（审计已记录），否则返回 null。
     * 调用方应短路返回，不调用模型。
     *
     * <p>学习点：输入审核放在“加载历史/调 LLM”之前——违规输入不进历史、
     * 不花 Token、不污染上下文；输出审核放在“清洗之后、落库之前”——
     * 兜底文案替换后再持久化，历史里也绝不出现违规回答。</p>
     */
    private String guardInput(String message) {
        com.aics.chat.security.ContentReviewResult result = contentSafetyService.reviewInput(message);
        return result.passed() ? null : "抱歉，我无法回答这个问题。";
    }

    /**
     * 输出 Guardrail（3.2 F4）：违规回答替换为兜底文案（审计已记录），否则原样返回。
     */
    private String guardOutput(String response) {
        if (response == null) {
            return null;
        }
        com.aics.chat.security.ContentReviewResult result = contentSafetyService.reviewOutput(response);
        return result.passed() ? response : "抱歉，该回答未通过安全审核，已为您转人工处理。";
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
            // 学习点：摘要场景显式固定为 SUMMARY——压缩失败仍走原截断兜底，路由只是把模型选择交给统一策略
            com.aics.chat.prompt.PromptRegistry.RenderedPrompt summaryRp = promptRegistry.render("summary",
                    java.util.Map.of("history", conversation));
            com.aics.chat.observability.TraceContext sc = com.aics.chat.observability.TraceContextHolder.current();
            if (sc != null) {
                sc.setPrompt(summaryRp.getScenario(), summaryRp.getVersion());
            }
            String summary = resilientAiService.callSummary(ModelScenario.SUMMARY,
                    new Prompt(summaryRp.text())
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

        // 输入 Guardrail（3.2 F4）：违规输入拒答，不调用模型
        String blockMessage = guardInput(message);
        if (blockMessage != null) {
            log.warn("对话输入被内容审核拦截: sessionId={}", sessionId);
            return Result.success(blockMessage);
        }

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
            // 学习点：对话场景显式传 CHAT 让路由按场景选模型——业务层不再感知具体模型，后续换模型只改配置
            String response = resilientAiService.callChat(ModelScenario.CHAT, history).get();

            // 过滤思考过程
            response = cleanResponse(response);

            // 输出 Guardrail（3.2 F4）：违规回答拦截为兜底文案
            response = guardOutput(response);

            // 记录 AI 回复到历史
            chatHistoryService.append(sessionId, "assistant", response);
            history.add(new AssistantMessage(response));

            // answer 环节观测：回答长度 + 用户反馈闭环（线上采样评估）
            recordAnswerSpan(sessionId, message, response, 0);
            triggerOnlineEval(sessionId, message, response);

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

        // 输入 Guardrail（3.2 F4）：违规输入拒答，不调用模型
        String blockMessage = guardInput(message);
        if (blockMessage != null) {
            log.warn("RAG对话输入被内容审核拦截: sessionId={}, knowledgeBase={}", sessionId, knowledgeBase);
            return Result.success(new ChatRagResponseDTO(blockMessage, List.of()));
        }

        try {
            // 检索：纯向量 / Hybrid / 改写（增强失败自动降级纯向量）
            List<Document> docs = retrieveRagDocs(knowledgeBase, message, 5, 0.5, hybrid, rewrite);
            // RAG ACL 过滤（3.2 F5）：按租户/角色/文档 ACL 剔除无权限文档，回答不得引用
            docs = ragAclFilter.filter(knowledgeBase, docs, com.aics.chat.util.ChatUserContext.getUserId());
            // 把命中的知识片段拼成【知识库资料】上下文，注入 Prompt
            String context = knowledgeBaseService.buildContext(docs);

            // 构建 RAG Prompt（外置到 application-prompt.yml scenario=rag）
            com.aics.chat.prompt.PromptRegistry.RenderedPrompt ragRp = promptRegistry.render("rag",
                    java.util.Map.of("context", context.isBlank() ? "（未检索到相关资料）" : context, "message", message));
            com.aics.chat.observability.TraceContext rc = com.aics.chat.observability.TraceContextHolder.current();
            if (rc != null) {
                rc.setPrompt(ragRp.getScenario(), ragRp.getVersion());
            }
            String ragPrompt = ragRp.text();

            // 通过 ResilientAiService 弹性调用 LLM（超时/重试/熔断/降级）
            String response = resilientAiService.callRagChat(ModelScenario.RAG, ragPrompt).get();
            response = cleanResponse(response);   // 去掉模型思考过程标签，只留正式回答

            // 输出 Guardrail（3.2 F4）：违规回答拦截为兜底文案
            response = guardOutput(response);

            // 从检索结果构建引用溯源列表（documentId/title/page/score/content），随回答返回前端
            List<CitationItemDTO> citations = buildCitations(docs);

            // answer 环节观测：引用数 + 线上采样评估
            recordAnswerSpan(sessionId, message, response, citations.size());
            triggerOnlineEval(sessionId, message, response);

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

        // 输入 Guardrail（3.2 F4）：违规输入直接返回拦截事件，不调用模型
        String blockMessage = guardInput(message);
        if (blockMessage != null) {
            log.warn("SSE对话输入被内容审核拦截: sessionId={}", sessionId);
            try {
                emitter.send(SseEmitter.event().data(Map.of("content", blockMessage)));
                emitter.send(SseEmitter.event().data(Map.of("done", true, "citations", List.of())));
            } catch (Exception ignore) {
                // 客户端可能已断开，尽力推送
            }
            emitter.complete();
            return emitter;
        }

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
                String ragPrompt = promptRegistry.render("rag",
                        java.util.Map.of("context", context.isBlank() ? "（未检索到相关资料）" : context, "message", message)).text();
                // 设计要点：流式场景在订阅前完成路由——RAG 与普通对话分别用 RAG/CHAT 场景，保持同一次 SSE 请求的决策一致
                futureFlux = resilientAiService.callSseRagStream(ModelScenario.RAG, ragPrompt);
                // 缓存引用溯源，完成事件时随 done 一起推送
                citations = buildCitations(docs);
            } else {
                futureFlux = resilientAiService.callSseStream(ModelScenario.CHAT, streamHistory);
                citations = List.of();
            }

            // 等待 Flux 就绪（受 TimeLimiter 保护，不会无限等待）
            Flux<String> flux = futureFlux.get();

            // ──────────────────────────────────────────────────────────────────────────
            // 3. 订阅 Flux 响应式流，桥接到 SSE（Server-Sent Events）推送通道
            // ──────────────────────────────────────────────────────────────────────────
            //
            // 【技术背景：Reactor Flux 与 SSE 的桥接】
            //
            //   Flux<String> 来自 Spring WebFlux 的响应式编程模型（Reactive Streams 规范实现），
            //   代表一个异步的、可背压的 0..N 元素数据流。在这里，每个元素就是 LLM 生成的一个
            //   token（或一小段文本 chunk），由 ResilientAiService 内部通过 Spring AI 的
            //   ChatClient.stream() 获得。
            //
            //   SseEmitter 则是 Spring MVC（Servlet 栈）提供的 SSE 发射器，基于 HTTP 长连接
            //   实现服务端→客户端的单向推送。SSE 协议（W3C 标准）天然支持：
            //     - 自动重连（浏览器端 EventSource API 内置重连机制）
            //     - 文本格式（Content-Type: text/event-stream）
            //     - 逐条事件推送（每条 data: 行就是一个事件）
            //
            //   这里用 flux.subscribe(onNext, onError, onComplete) 三参数订阅模式，
            //   将 Reactive 世界的数据流手动桥接到 Servlet 世界的 SseEmitter：
            //     ┌─────────────┐   Flux<String>    ┌──────────────┐   SSE event    ┌────────┐
            //     │  LLM (AI)   │ ───────────────→  │  subscribe() │ ────────────→  │ 前端   │
            //     │  token流    │  响应式异步推送     │  三个回调     │  HTTP长连接推送  │ 浏览器  │
            //     └─────────────┘                   └──────────────┘                └────────┘
            //
            //   为什么不用 @ResponseBody 直接返回 Flux？
            //   → 因为本项目是 Spring MVC（Servlet 容器），不是 WebFlux（Netty 容器）。
            //     Spring MVC 无法直接将 Flux 渲染为 SSE 响应，必须通过 SseEmitter 手动桥接。
            //
            // 【技术背景：StringBuilder 累积模式】
            //
            //   full（StringBuilder）用于在流式过程中累积所有 chunk，流结束后得到完整回复文本。
            //   为什么不在 onComplete 时重新调用 LLM？
            //   → 因为每次 LLM 调用都有成本（Token 计费）和延迟，累积是最经济的方式。
            //   注意：StringBuilder 非线程安全，但 Flux 的 subscribe 回调在同一个串行调度器上执行
            //   （Reactive Streams 规范要求 onNext 串行调用），所以这里不存在并发写入问题。
            //
            // 【工具调用（Tool Call）标注】流式场景下，若模型决定调用工具（如订单查询/SQL 执行），
            //   执行点在 Spring AI 框架内部（ChatModel 层的 ToolCallingManager），不在本类：
            //   工具调用/结果回传/重新请求模型都在 Flux 内部完成，本处 subscribe 收到的 chunk
            //   永远是最终文本，对工具执行全程无感知。详见 ResilientAiService.callSseStream 注释。
            //
            // ──────────────────────────────────────────────────────────────────────────
            StringBuilder full = new StringBuilder();
            flux.subscribe(
                    // ┌─────────────────────────────────────────────────────────────┐
                    // │ onNext 回调：每收到一个 chunk（token）时触发                  │
                    // │                                                             │
                    // │ 【降级错误检测】                                              │
                    // │ ResilientAiService 在 LLM 调用失败（熔断/超时/异常）时，       │
                    // │ 不会让 Flux 抛 onError，而是发射一个以 "[ERROR]" 开头的        │
                    // │ 特殊 chunk 作为"软错误"标记。这是一种"优雅降级"模式：           │
                    // │   - 避免 SSE 连接因异常中断（用户已看到部分内容）               │
                    // │   - 前端收到 error 事件后可展示友好提示，而非连接断开           │
                    // │   - 后端日志仍记录降级原因，便于排查                           │
                    // │                                                             │
                    // │ 【SSE 事件格式】                                              │
                    // │ 正常 chunk → {"content": "你好"}    // 前端拼接实现打字机效果  │
                    // │ 降级错误 → {"error": "服务暂时不可用"} // 前端展示错误提示       │
                    // └─────────────────────────────────────────────────────────────┘
                    chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            // ── 降级错误检测 ──
                            // Resilience4j 的 CircuitBreaker 熔断 或 TimeLimiter 超时后，
                            // ResilientAiService 将异常信息包装为 "[ERROR]xxx" 格式的 chunk，
                            // 通过 Flux.just("[ERROR]服务繁忙") 发射，而非 Flux.error()。
                            // 这样走 onNext 而非 onError，流正常结束（onComplete 仍会触发），
                            // 但我们在此处拦截并提前终止 SSE 连接，避免把错误文本当正常内容推送。
                            if (chunk.startsWith("[ERROR]")) {
                                String errorMsg = chunk.substring(7);
                                log.warn("SSE流式降级: sessionId={}, msg={}", sessionId, errorMsg);
                                try {
                                    // 推送 error 事件给前端，前端据此展示友好提示
                                    // Map.of() 生成不可变单条目 Map，Jackson 序列化为 {"error":"..."}
                                    emitter.send(SseEmitter.event().data(Map.of("error", errorMsg)));
                                } catch (Exception ignore) {
                                    // 客户端可能已断开连接（浏览器关闭标签页），发送失败属正常情况
                                    // 此处不需要重试或上报，因为 SSE 本身就是"尽力推送"模式
                                    // ignore
                                }
                                // 正常关闭 SSE 连接（发送 HTTP 连接关闭信号）
                                // 注意：这里用 complete() 而非 completeWithError()，
                                // 因为降级是"预期的容错行为"，不是程序异常
                                emitter.complete();
                                return;
                            }
                            // ── 正常 token 处理 ──
                            full.append(chunk);   // 累积到 StringBuilder，供流结束后入库（持久化历史）
                            try {
                                // 逐 token 推送给前端，实现"打字机效果"
                                // SseEmitter.event().data(Map.of("content", chunk))
                                //   → 序列化为 SSE 事件：data:{"content":"你"}\n\n
                                //   → 前端 EventSource.onmessage 收到后拼接展示
                                emitter.send(SseEmitter.event().data(Map.of("content", chunk)));
                            } catch (Exception e) {
                                // 发送失败的常见原因：
                                //   1. 客户端已断开（浏览器关闭/刷新）→ 连接已不可用
                                //   2. SseEmitter 已超时（SSE_EMITTER_TIMEOUT=5min）
                                // 不中断 Flux 订阅，因为 LLM 仍在生成内容，
                                // 后续的 onComplete 仍需要把完整回复入库（即使前端已断开）
                                log.warn("SSE发送失败: sessionId={}, err={}", sessionId, e.getMessage());
                            }
                        }
                    },
                    // ┌─────────────────────────────────────────────────────────────┐
                    // │ onError 回调：Flux 流本身发生异常时触发                       │
                    // │                                                             │
                    // │ 与上面的"[ERROR] 降级"不同，这里是 Reactive Streams 层面的     │
                    // │ 真实异常（如网络中断、序列化错误、上游 Publisher 内部崩溃）。    │
                    // │                                                             │
                    // │ 【Reactive Streams 规范】                                    │
                    // │ onError 被调用后，订阅自动终止，不会再收到 onNext 或           │
                    // │ onComplete。因此必须在这里关闭 SseEmitter，否则连接会泄漏。    │
                    // │                                                             │
                    // │ 【completeWithError vs complete】                            │
                    // │ completeWithError(error)：                                 │
                    // │   - 通知 Servlet 容器"响应异常终止"                           │
                    // │   - 容器会记录异常日志，但 HTTP 响应已部分提交（chunked），      │
                    // │     无法再修改状态码                                          │
                    // │   - 触发 emitter.onError() 回调（下方 534 行）                │
                    // └─────────────────────────────────────────────────────────────┘
                    error -> {
                        log.error("SSE流式对话异常: sessionId={}", sessionId, error);
                        try {
                            // 将异常消息转为 error 事件推送给前端
                            // String.valueOf() 防御 getMessage() 返回 null 的情况
                            // （如 NullPointerException 的 message 通常为 null）
                            emitter.send(SseEmitter.event().data(Map.of("error", String.valueOf(error.getMessage()))));
                        } catch (Exception ignore) {
                            // 同上：客户端可能已断开，尽力推送
                            // ignore
                        }
                        // 以异常状态关闭 SSE 连接
                        // Servlet 容器会记录此异常，但不会中断已部分发送的响应
                        emitter.completeWithError(error);
                    },
                    // ┌─────────────────────────────────────────────────────────────┐
                    // │ onComplete 回调：Flux 流正常结束（所有 token 推送完毕）时触发  │
                    // │                                                             │
                    // │ 这是流式对话的"收尾阶段"，需要完成三件事：                     │
                    // │   ① 清洗回复文本（移除 <thinking> 思考标签）                  │
                    // │   ② 持久化到会话历史（Redis + MySQL）                        │
                    // │   ③ 推送 done 事件通知前端"回复结束"                          │
                    // │                                                             │
                    // │ 【为什么 done 事件不携带完整回复正文？】                       │
                    // │   正文已通过逐 token 的 onNext 推送给前端，前端已拼接完成。     │
                    // │   done 事件仅作为"结束信号"，附带 citations（引用溯源列表），  │
                    // │   避免重复传输大量文本（节省带宽、减少序列化开销）。            │
                    // │                                                             │
                    // │ 【done 事件格式】                                            │
                    // │   {"done": true, "citations": [{"title":"...", ...}]}       │
                    // │   前端收到 done=true 后：                                    │
                    // │   - 停止"正在输入..."动画                                    │
                    // │   - 渲染引用溯源卡片（论文/文档链接）                          │
                    // │   - 关闭 EventSource 连接                                   │
                    // └─────────────────────────────────────────────────────────────┘
                    () -> {
                        // ① 清洗思考标签：移除 <thinking>...</thinking> 内容
                        // 某些模型（DeepSeek-R1 等推理模型）会在输出中夹杂思考过程，
                        // 这些内容不应展示给用户（影响体验，可能泄露 Prompt 策略）
                        String response = cleanResponse(full.toString());
                        // 输出 Guardrail（3.2 F4）：违规回答替换为兜底文案（审计已记录），
                        // 历史只保存兜底文案，done 事件附带 warning 提示前端
                        String guardedResponse = guardOutput(response);
                        boolean outputBlocked = !guardedResponse.equals(response);
                        response = guardedResponse;
                        if (outputBlocked) {
                            log.warn("SSE回答未通过输出审核，已拦截: sessionId={}", sessionId);
                        }
                        try {
                            // ② 持久化会话历史
                            // ChatHistoryService 内部采用 Redis 热缓存 + MySQL 异步落库双写策略：
                            //   - Redis：保证下次对话能快速加载最近历史（毫秒级）
                            //   - MySQL：保证历史数据不丢失（持久化）
                            chatHistoryService.append(sessionId, "assistant", response);
                            // 同时追加到内存中的 streamHistory（本次请求的上下文副本），
                            // 供后续可能的同请求内多轮调用使用
                            streamHistory.add(new AssistantMessage(response));
                            // ③ 构建并推送 done 事件
                            // 使用 HashMap 而非 Map.of()，因为需要 put 多个不同类型的值
                            // （boolean + List），Map.of() 的类型推断不够灵活
                            Map<String, Object> doneEvent = new HashMap<>();
                            doneEvent.put("done", true);           // 结束信号标记
                            doneEvent.put("citations", citations); // RAG 引用溯源列表
                            if (outputBlocked) {
                                doneEvent.put("warning", "回答未通过安全审核，已拦截");
                            }
                            emitter.send(SseEmitter.event().data(doneEvent));
                        } catch (Exception e) {
                            // 即使发送 done 事件失败，也要执行下面的 emitter.complete()
                            // 否则 SSE 连接会一直挂起直到超时（5分钟），浪费服务端资源
                            log.warn("SSE完成事件发送失败: sessionId={}, err={}", sessionId, e.getMessage());
                        }
                        // 正常关闭 SSE 连接：发送 HTTP 连接关闭信号
                        // 此后 emitter 进入"已完成"状态，再调用 send() 会抛 IllegalStateException
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
     * 回调式流式对话：与 {@link #chatStreamSse} 复用同一套历史组装与弹性流式调用，
     * 区别仅在消费端——不绑定 SseEmitter，而是逐 token 回调给调用方，由调用方自行决定
     * 推送通道（Agent 编排的 SSE 桥接即此用法）。同步阻塞至流结束，返回完整回复。
     */
    @Override
    public String streamReply(String sessionId, String message, java.util.function.Consumer<String> onToken) {
        // 输入 Guardrail：违规输入短路返回兜底文案，不调模型、不进历史
        String blockMessage = guardInput(message);
        if (blockMessage != null) {
            log.warn("流式对话输入被内容审核拦截: sessionId={}", sessionId);
            return blockMessage;
        }

        try {
            // 1. 组装会话历史（与 chatStreamSse 保持一致）
            List<Message> history = toSpringMessages(chatHistoryService.load(sessionId));
            injectUserContext(history);
            history.add(new UserMessage(message));
            chatHistoryService.append(sessionId, "user", message);
            if (history.size() > MAX_HISTORY_SIZE) {
                history = compressHistory(history);
            }

            // 2. 弹性获取流式 Flux 并等待就绪（受 TimeLimiter 保护）
            Flux<String> flux = resilientAiService.callSseStream(ModelScenario.CHAT, history).get();

            // 3. 同步消费 token 流：逐 chunk 回调 + 累积。
            //    "[ERROR]" 软错误标记（熔断/超时降级）转为异常抛给调用方，
            //    由其决定如何呈现（对齐 chatStreamSse 中 error 事件的语义）
            StringBuilder full = new StringBuilder();
            flux.doOnNext(chunk -> {
                if (chunk == null || chunk.isEmpty()) {
                    return;
                }
                if (chunk.startsWith("[ERROR]")) {
                    throw new IllegalStateException(chunk.substring(7));
                }
                full.append(chunk);
                onToken.accept(chunk);
            }).blockLast();

            // 4. 清洗思考标签 + 输出 Guardrail，落库后返回全文
            String response = cleanResponse(full.toString());
            response = guardOutput(response);
            chatHistoryService.append(sessionId, "assistant", response);
            return response;
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            log.error("流式对话初始化异常: sessionId={}, cause={}", sessionId, cause.getMessage());
            throw new IllegalStateException(getFriendlyMessage(cause), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("流式对话被中断", e);
        }
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
     * 检索环节计入 LLM 调用链观测（retrieval span：query、召回数、文档 ID、耗时）。
     */
    private List<Document> retrieveRagDocs(String knowledgeBase, String message, int topK, double threshold,
                                           boolean hybrid, boolean rewrite) {
        // retrieval 环节观测：query、召回数、文档 ID、耗时（handler 在 onStop 时组装 span）
        io.micrometer.observation.Observation observation = io.micrometer.observation.Observation
                .createNotStarted("rag.retrieval", observationRegistry)
                .lowCardinalityKeyValue("span.type", "RETRIEVAL")
                .lowCardinalityKeyValue("knowledgeBase", knowledgeBase == null ? "" : knowledgeBase)
                .highCardinalityKeyValue("query", message)
                .highCardinalityKeyValue("topK", String.valueOf(topK))
                .highCardinalityKeyValue("hybrid", String.valueOf(hybrid))
                .highCardinalityKeyValue("rewrite", String.valueOf(rewrite))
                .start();
        try {
            List<Document> docs;
            if (!hybrid && !rewrite) {
                docs = knowledgeBaseService.search(knowledgeBase, message, topK, threshold);
            } else {
                RetrievalMode mode = rewrite ? RetrievalMode.HYBRID_QUERY_REWRITE : RetrievalMode.HYBRID;
                try {
                    RetrieveResult result = hybridRetriever.retrieve(knowledgeBase, message, mode, topK);
                    log.info("RAG增强检索: mode={}, degraded={}, hits={}",
                            result.getMode(), result.isDegraded(), result.getDocuments().size());
                    docs = result.getDocuments();
                } catch (Exception e) {
                    log.warn("RAG增强检索失败，降级纯向量: mode={}, err={}", mode, e.getMessage());
                    docs = knowledgeBaseService.search(knowledgeBase, message, topK, threshold);
                }
            }
            // 明细：召回数 + 命中文档 ID（截断，防敏感信息进 trace）
            // 学习点：detail 只放文档 ID 而非文档全文——trace 数据可能被导出到第三方
            // 追踪平台（Langfuse/Phoenix），文档全文属业务敏感内容，ID 足够还原检索路径
            String docIds = docs.stream()
                    .map(d -> String.valueOf(d.getMetadata().getOrDefault("documentId", d.getId())))
                    .limit(20).collect(Collectors.joining(","));
            observation.highCardinalityKeyValue("hitCount", String.valueOf(docs.size()))
                    .highCardinalityKeyValue("detail", docIds);
            return docs;
        } catch (Exception e) {
            // 检索异常也要标记观测失败：看板上能区分"检索没命中"与"检索报错"
            observation.error(e);
            throw e;
        } finally {
            observation.stop();
        }
    }

    /**
     * answer 环节观测：引用数、回答长度（安全检测结果由 Agent 链路另行记录）。
     *
     * <p>学习点：answer span 是调用链的"终点环"——docs/15 的链路模型里
     * answer 记录"引用数、安全结果、用户反馈"，这里用空 Runnable 只为了
     * 让 Observation 走完 start→stop 生命周期挂一个 ANSWER span；引用数/长度
     * 是回答质量的基础信号（引用太少可能答非所问、回答过长可能啰嗦）。</p>
     */
    private void recordAnswerSpan(String sessionId, String question, String answer, int citationCount) {
        TraceSpans.observe(observationRegistry, "ANSWER", "chat.answer",
                Map.of(),
                Map.of("citationCount", String.valueOf(citationCount),
                        "answerLength", String.valueOf(answer == null ? 0 : answer.length())),
                () -> {
                });
    }

    /**
     * 线上采样评估触发：对本次真实回答异步执行 LLM-as-Judge 评分（未命中采样则跳过）。
     *
     * <p>学习点：这里把"线上真实回答"交给评估服务——与离线 golden 集评估不同，
     * 线上评估直接拿用户实际收到的回答打分，能发现 golden 集覆盖不到的长尾质量问题；
     * 采样判定在 OnlineEvalService 内部完成，对调用方透明。</p>
     */
    private void triggerOnlineEval(String sessionId, String question, String answer) {
        TraceContext trace = TraceContextHolder.current();
        onlineEvalService.evaluateAsync(
                trace == null ? null : trace.getRequestId(),
                parseSessionId(sessionId),
                trace == null ? null : trace.getUserId(),
                question, answer);
    }

    /** 字符串 sessionId 转 Long（非数字返回 null，Agent 流程的 Long 会话 ID 不受影响） */
    private static Long parseSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        try {
            return Long.valueOf(sessionId.trim());
        } catch (NumberFormatException e) {
            return null;
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