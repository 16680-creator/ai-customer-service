package com.aics.chat.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.chat.observability.ModelUsageRecorder;
import com.aics.chat.observability.TraceContext;
import com.aics.chat.observability.TraceContextHolder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 弹性 AI 调用服务 —— 为 LLM 调用提供超时/重试/熔断/降级能力。
 *
 * <h3>【AI 技术详解】为什么 LLM 调用需要弹性容错？</h3>
 * <ul>
 *   <li><b>LLM 调用的不确定性</b>：
 *       <ul>
 *         <li>响应时间波动大：简单问题 1s，复杂问题可能 30s+</li>
 *         <li>网络不稳定：API 服务可能超时、限流、临时不可用</li>
 *         <li>配额限制：付费 API 有 QPS/TPM 限制，超限会 429</li>
 *       </ul>
 *   </li>
 *   <li><b>没有容错会怎样</b>：
 *       <ul>
 *         <li>超时：用户无限等待，体验极差</li>
 *         <li>故障蔓延：一个 LLM 调用卡住，阻塞整个请求线程</li>
 *         <li>雪崩效应：下游故障导致上游线程耗尽，整个系统崩溃</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】Resilience4j 四大容错机制</h3>
 * <ul>
 *   <li><b>@TimeLimiter（超时控制）</b>：
 *       <ul>
 *         <li>原理：通过 {@code Future.get(timeout, TimeUnit)} 实现，超时后 cancel Future</li>
 *         <li>非流式 30s：等待完整响应，超过即认为失败</li>
 *         <li>流式 60s：只限制"首 token 到达"时间，一旦开始流式返回就不受限制</li>
 *         <li>为什么返回 CompletableFuture：TimeLimiter 只能拦截 Future 类型的返回值</li>
 *       </ul>
 *   </li>
 *   <li><b>@Retry（重试）</b>：
 *       <ul>
 *         <li>策略：指数退避（1s → 2s → 4s），最多 3 次</li>
 *         <li>只重试网络异常（超时、连接拒绝），不重试业务异常（4xx 鉴权失败）</li>
 *         <li>流式不配重试：流一旦开始推送就不可重放，重试会导致前端重复接收 token</li>
 *       </ul>
 *   </li>
 *   <li><b>@CircuitBreaker（熔断器）</b>：
 *       <ul>
 *         <li>原理：滑动窗口统计失败率，超过阈值（50%）即"打开"熔断</li>
 *         <li>状态机：CLOSED（正常）→ OPEN（熔断，快速失败）→ HALF_OPEN（试探恢复）</li>
 *         <li>价值：防止故障蔓延，快速失败保护下游服务</li>
 *       </ul>
 *   </li>
 *   <li><b>@Fallback（降级）</b>：
 *       <ul>
 *         <li>超时/熔断/重试耗尽时返回友好提示，而不是把异常抛给用户</li>
 *         <li>流式 fallback 返回 {@code Flux.just("[ERROR]" + msg)}，由 SSE 推送给前端</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>调用链</h3>
 * <pre>
 *   Controller → ChatServiceImpl → ResilientAiService
 *                                       ├── [@TimeLimiter] 超时控制
 *                                       ├── [@Retry]       临时故障重试
 *                                       ├── [@CircuitBreaker] 熔断保护
 *                                       └── [@Fallback]    降级响应
 * </pre>
 *
 * <h3>【技术关联】流式与非流式为何用独立实例（不同 name）</h3>
 * <p>非流式方法用 {@code name = "chatService"}，流式方法用 {@code name = "sseChatService"}，
 * 两套配置在 {@code application.yml} 中分别定义，超时阈值不同：</p>
 * <ul>
 *   <li>非流式 {@code chatService}：30s TimeLimiter —— 等待完整响应，超过即认为失败。</li>
 *   <li>流式 {@code sseChatService}：60s TimeLimiter —— 只限制"首次 token 到达"的时间，
 *       一旦开始流式返回就不再受 TimeLimiter 约束（流本身的超时由 SseEmitter 兜底）。</li>
 * </ul>
 *
 * <h3>【技术关联】与 Spring AI ChatClient 的关系</h3>
 * <p>本类是对 ChatClient 的弹性封装：
 * <ul>
 *   <li><b>ChatClient</b>：负责 LLM 调用（发送请求、接收响应）</li>
 *   <li><b>ResilientAiService</b>：负责容错（超时、重试、熔断、降级）</li>
 *   <li><b>ChatServiceImpl</b>：负责业务编排（历史管理、RAG 检索、结果处理）</li>
 * </ul>
 * 三层分离，各司其职。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientAiService {

    private final ChatClient chatClient;
    private final OpenAiChatModel chatModel;
    private final ObservationRegistry observationRegistry;
    private final ModelUsageRecorder modelUsageRecorder;

    /** LLM 调用观测名（OTLP span 名） */
    private static final String OBS_LLM = "chat.llm";

    /** 场景：普通对话 */
    private static final String SCENARIO_CHAT = "chat";

    /** 场景：RAG 对话 */
    private static final String SCENARIO_RAG = "rag";

    /** 场景：摘要压缩 */
    private static final String SCENARIO_SUMMARY = "summary";

    // ==================== 非流式调用（普通对话） ====================

    /**
     * 【AI 核心】弹性非流式 LLM 调用。
     * 组合了 TimeLimiter（30s 超时）、Retry（最多 3 次）、CircuitBreaker（熔断）。
     *
     * <p><b>【AI 技术详解】非流式调用特点</b>：
     * <ul>
     *   <li>等待完整响应：LLM 生成全部内容后才返回，适合短回答、工具调用等场景</li>
     *   <li>超时 30s：足够大多数问题生成回答，超时即认为失败</li>
     *   <li>可重试：因为是完整响应，失败后重试不会导致重复</li>
     * </ul>
     *
     * <p><b>【技术关联】为什么用 CompletableFuture</b>：
     * <ul>
     *   <li>TimeLimiter 通过 {@code Future.get(timeout)} 实现超时</li>
     *   <li>supplyAsync 在独立线程执行阻塞的 LLM 调用</li>
     *   <li>TimeLimiter 在外层调度 watchdog，到期后 cancel Future</li>
     * </ul>
     */
    @TimeLimiter(name = "chatService", fallbackMethod = "fallbackChat")
    @Retry(name = "chatService", fallbackMethod = "fallbackChat")
    @CircuitBreaker(name = "chatService", fallbackMethod = "fallbackChat")
    public CompletableFuture<String> callChat(List<Message> messages) {
        // 异步边界显式传播 TraceContext（见 observability.TraceContextHolder）
        // 学习点：这里必须在 supplyAsync 之前 capture、在 lambda 内 restore——
        // CompletableFuture 的异步线程读不到调用线程的 ThreadLocal，不显式传播
        // 会导致 LLM span 落不到请求的 TraceContext 上（trace 断裂）
        TraceContext captured = TraceContextHolder.capture();
        // supplyAsync 在独立线程执行阻塞的 LLM 调用，便于 TimeLimiter 通过 Future.get(timeout) 实现超时
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                log.debug("弹性调用 LLM (非流式), messages={}", messages.size());
                // LLM 环节观测 + Token/费用计量（scenario=chat）
                // 学习点：观测与计量"双轨并行"——Observation 回答"这次调用发生了什么"（span），
                // ModelUsageRecorder 回答"这次调用花了多少钱"（usage），两者共用 TraceContext 的
                // requestId 关联，落库后可按 requestId 把调用链与费用拼回同一张图
                Observation observation = startLlmObservation(SCENARIO_CHAT);
                try {
                    ChatResponse response = chatClient.prompt()
                            .messages(messages)   // 多轮消息直接透传给模型（含历史/系统提示）
                            .call()
                            .chatResponse();
                    String text = response == null || response.getResult() == null
                            ? "" : response.getResult().getOutput().getText();
                    // Spring AI 非流式响应元数据携带 usage（prompt/completion tokens），
                    // 这是 Token 计量的权威来源；流式则常取不到（见 callSseStream 的估算处理）
                    Usage usage = response == null || response.getMetadata() == null
                            ? null : response.getMetadata().getUsage();
                    finishLlmObservation(observation, usage, null);
                    modelUsageRecorder.record(SCENARIO_CHAT, provider(), model(),
                            usage == null ? null : usage.getPromptTokens(),
                            usage == null ? null : usage.getCompletionTokens(),
                            "SUCCESS", null);
                    log.debug("LLM 非流式调用成功, responseLength={}", text.length());
                    return text;
                } catch (Exception e) {
                    // 失败路径同样要收尾观测与计量：span 标记 FAILED、usage 记失败次数，
                    // 这样 trace 看板上能直接看到"哪些请求失败了、失败在哪个环节"
                    finishLlmObservation(observation, null, e);
                    modelUsageRecorder.record(SCENARIO_CHAT, provider(), model(),
                            null, null, "FAILED", e.getMessage());
                    throw new CompletionException(e);   // 包装为 CompletionException，供外层 Future.get 解包
                }
            } finally {
                // 无论成功失败都清理 ThreadLocal：异步线程池复用，不清理会串上下文
                TraceContextHolder.clear();
            }
        });
    }

    /**
     * 弹性 RAG 对话调用（非流式）。
     */
    @TimeLimiter(name = "chatService", fallbackMethod = "fallbackChat")
    @Retry(name = "chatService", fallbackMethod = "fallbackChat")
    @CircuitBreaker(name = "chatService", fallbackMethod = "fallbackChat")
    public CompletableFuture<String> callRagChat(String prompt) {
        TraceContext captured = TraceContextHolder.capture();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                log.debug("弹性调用 LLM (RAG 非流式)");
                Observation observation = startLlmObservation(SCENARIO_RAG);
                try {
                    ChatResponse response = chatClient.prompt()
                            .user(prompt)
                            .call()
                            .chatResponse();
                    String text = response == null || response.getResult() == null
                            ? "" : response.getResult().getOutput().getText();
                    Usage usage = response == null || response.getMetadata() == null
                            ? null : response.getMetadata().getUsage();
                    finishLlmObservation(observation, usage, null);
                    modelUsageRecorder.record(SCENARIO_RAG, provider(), model(),
                            usage == null ? null : usage.getPromptTokens(),
                            usage == null ? null : usage.getCompletionTokens(),
                            "SUCCESS", null);
                    log.debug("LLM RAG 非流式调用成功, responseLength={}", text.length());
                    return text;
                } catch (Exception e) {
                    finishLlmObservation(observation, null, e);
                    modelUsageRecorder.record(SCENARIO_RAG, provider(), model(),
                            null, null, "FAILED", e.getMessage());
                    throw new CompletionException(e);
                }
            } finally {
                TraceContextHolder.clear();
            }
        });
    }

    /**
     * 弹性摘要调用（用于会话历史压缩）。
     */
    @TimeLimiter(name = "chatService", fallbackMethod = "fallbackSummary")
    @Retry(name = "chatService", fallbackMethod = "fallbackSummary")
    @CircuitBreaker(name = "chatService", fallbackMethod = "fallbackSummary")
    public CompletableFuture<String> callSummary(Prompt prompt) {
        TraceContext captured = TraceContextHolder.capture();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                log.debug("弹性调用 LLM (摘要)");
                Observation observation = startLlmObservation(SCENARIO_SUMMARY);
                try {
                    ChatResponse response = chatModel.call(prompt);
                    String text = response == null || response.getResult() == null
                            ? "" : response.getResult().getOutput().getText();
                    Usage usage = response == null || response.getMetadata() == null
                            ? null : response.getMetadata().getUsage();
                    finishLlmObservation(observation, usage, null);
                    modelUsageRecorder.record(SCENARIO_SUMMARY, provider(), model(),
                            usage == null ? null : usage.getPromptTokens(),
                            usage == null ? null : usage.getCompletionTokens(),
                            "SUCCESS", null);
                    return text;
                } catch (Exception e) {
                    finishLlmObservation(observation, null, e);
                    modelUsageRecorder.record(SCENARIO_SUMMARY, provider(), model(),
                            null, null, "FAILED", e.getMessage());
                    throw new CompletionException(e);
                }
            } finally {
                TraceContextHolder.clear();
            }
        });
    }

    // ==================== 流式调用（SSE） ====================

    /**
     * 【AI 核心】弹性流式 LLM 调用（SSE）。
     * 使用 TimeLimiter 限制首次 token 到达时间，防止 SSE 连接挂起。
     *
     * <p><b>【AI 技术详解】流式调用特点</b>：
     * <ul>
     *   <li>逐 token 推送：LLM 每生成一个 token 就立即返回，实现"打字机效果"</li>
     *   <li>TimeLimiter 只限"首 token"：一旦开始流式返回就不受超时限制（由 SseEmitter 兜底）</li>
     *   <li>不配 Retry：流一旦开始推送就不可重放，重试会导致前端重复接收 token</li>
     *   <li>返回 Flux&lt;String&gt;：响应式流，订阅后逐个接收 token</li>
     * </ul>
     *
     * <p><b>【技术关联】Flux 与 SseEmitter 的协作</b>：
     * <pre>
     *   ResilientAiService.callSseStream()
     *       └── ChatClient.prompt().stream().content()  // 返回 Flux<String>
     *               └── flux.subscribe(
     *                       chunk -> emitter.send(chunk),  // 逐 token 推送
     *                       error -> emitter.completeWithError(error),
     *                       () -> emitter.complete()
     *                   )
     * </pre>
     */
    @TimeLimiter(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    @CircuitBreaker(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    public CompletableFuture<Flux<String>> callSseStream(List<Message> messages) {
        // 流式：返回 Flux<String>（逐 token）；TimeLimiter 只限制"首 token 到达"时间
        TraceContext captured = TraceContextHolder.capture();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                log.debug("弹性调用 LLM (SSE 流式), messages={}", messages.size());
                // 流式 Token 计量在流结束后通过 doFinally 统一上报（usage 取不到时按估算）
                // 学习点：流式调用是"懒执行"——chatClient.prompt().stream() 只构建 Flux，
                // 真正的 HTTP 请求发生在订阅（subscribe）时。所以不能像非流式那样在方法体内
                // 同步计量，必须用 Reactor 操作符把收尾逻辑挂到流的生命周期上：
                //   doOnNext  → 每个 chunk 到达时（这里只用于测首 Token 延迟）
                //   doFinally → 流完成/取消/出错时（统一上报观测与用量）
                AtomicReference<Usage> usageRef = new AtomicReference<>();
                AtomicLong firstTokenMs = new AtomicLong(-1);
                long start = System.currentTimeMillis();
                Flux<String> flux = chatClient.prompt()
                        .messages(messages)
                        .stream()
                        .content()
                        .doOnNext(chunk -> {
                            // 记录首 Token 延迟（首次收到内容片段）
                            // 学习点：首 Token 延迟（TTFT, Time To First Token）是流式体验的
                            // 核心指标——用户感知的"响应快慢"取决于它，而非流式总时长；
                            // AtomicLong 初值 -1 作为"是否已记录"的哨兵，避免每个 chunk 都判断
                            if (firstTokenMs.get() < 0) {
                                firstTokenMs.set(System.currentTimeMillis() - start);
                            }
                        })
                        .doFinally(signal -> {
                            // 流结束：记录 LLM 观测 + 用量（估算）
                            Usage usage = usageRef.get();
                            Observation observation = startLlmObservation(SCENARIO_CHAT);
                            if (usage != null) {
                                observation.highCardinalityKeyValue("promptTokens", String.valueOf(usage.getPromptTokens()))
                                        .highCardinalityKeyValue("completionTokens", String.valueOf(usage.getCompletionTokens()));
                            }
                            if (firstTokenMs.get() >= 0) {
                                observation.highCardinalityKeyValue("firstTokenMs", String.valueOf(firstTokenMs.get()));
                            }
                            // 流式 usage 通常取不到，按估算上报（estimated=true）
                            // 学习点：OpenAI 兼容接口的流式响应通常只在最后一个 chunk 携带 usage，
                            // 而 .content() 只保留文本丢掉了元数据；这里无法精确计量，只能按估算
                            // 打标（estimated=true），统计时与精确值区分开
                            modelUsageRecorder.record(SCENARIO_CHAT, provider(), model(),
                                    usage == null ? null : usage.getPromptTokens(),
                                    usage == null ? null : usage.getCompletionTokens(),
                                    signal == SignalType.ON_ERROR ? "FAILED" : "SUCCESS",
                                    signal == SignalType.ON_ERROR ? "stream error" : null);
                            finishLlmObservation(observation, usage, signal == SignalType.ON_ERROR
                                    ? new RuntimeException("stream error") : null);
                        });
                return flux;
            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                TraceContextHolder.clear();
            }
        });
    }

    /**
     * 【AI 核心】弹性流式 RAG 对话调用（SSE）。
     * 与 {@link #callSseStream(List)} 的核心区别：入参是"检索增强后的单条 prompt"——
     * 上游（ChatServiceImpl.chatStreamSse）已完成 检索→拼装，本方法只负责纯"生成"环节。
     *
     * <p><b>【AI 技术详解】RAG 场景的流式调用</b>：
     * <ul>
     *   <li>单 prompt 而非多轮消息：RAG 把知识片段拼进一条提示词（回答规则 + 检索资料 + 用户问题），
     *       模型按"阅读材料作答"的模式生成；{@code .user()} 只设置这一条消息，对话历史不透传</li>
     *   <li>prompt 显著更长：携带知识片段的 prompt 往往是普通对话的数倍，
     *       首 token 延迟（TTFT）随 prompt 变长而上升，是 RAG 流式体验的主要瓶颈</li>
     *   <li>弹性策略与 callSseStream 完全一致：TimeLimiter（60s）只限"首 token"，
     *       不配 Retry（流不可重放），熔断/超时统一降级到 fallbackSseStream</li>
     * </ul>
     *
     * <p><b>【技术关联】RAG 数据流中的位置</b>：
     * <pre>
     *   用户提问（message + knowledgeBase）
     *     → 向量检索：知识库 topK 相似片段           ← retrieveRagDocs（支持 hybrid/查询改写）
     *     → 拼装增强 prompt：防幻觉规则 + 资料 + 问题 ← ChatServiceImpl（上游完成）
     *     → callSseRagStream(ragPrompt)             ← 本方法：纯 LLM 生成环节
     *     → Flux&lt;String&gt; 逐 token 返回 → SseEmitter 推给前端
     *     → done 事件携带 citations（引用溯源）      ← buildCitations，支撑 RAG 可信度
     * </pre>
     */
    @TimeLimiter(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    @CircuitBreaker(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    public CompletableFuture<Flux<String>> callSseRagStream(String prompt) {
        // 复用 callSseStream 的模式：异步边界显式传播 TraceContext（原理见 callChat 的学习点），
        // 保证 RAG 链路的"检索 span + 生成 span"挂在同一个 TraceContext 上
        TraceContext captured = TraceContextHolder.capture();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                log.debug("弹性调用 LLM (SSE RAG 流式)");
                // usageRef 预留：.content() 只保留文本、丢掉元数据，流式 usage 取不到（同 callSseStream）
                AtomicReference<Usage> usageRef = new AtomicReference<>();
                long start = System.currentTimeMillis();
                Flux<String> flux = chatClient.prompt()
                        .user(prompt)   // RAG 关键差异：增强 prompt 作为单条用户消息（区别于 .messages() 多轮透传）
                        .stream()
                        .content()
                        .doFinally(signal -> {
                            // 流结束收尾：观测 + 用量计量。scenario=rag 与普通 chat 区分，
                            // 落库后可分场景统计成本/成功率/延迟分布
                            // 学习点：与 callSseStream 的口径差异——那里用 doOnNext + AtomicLong
                            // 哨兵记录真实 TTFT（首个 chunk 到达时刻）；这里没有 doOnNext，
                            // doFinally 里算出的 firstTokenMs 实际是"整条流的耗时"（含全部
                            // token 生成时间，偏大）。对比监控数据时注意口径不一致，
                            // 需要精确 TTFT 时可对照 callSseStream 的写法补 doOnNext
                            Usage usage = usageRef.get();
                            Observation observation = startLlmObservation(SCENARIO_RAG);
                            if (usage != null) {
                                observation.highCardinalityKeyValue("promptTokens", String.valueOf(usage.getPromptTokens()))
                                        .highCardinalityKeyValue("completionTokens", String.valueOf(usage.getCompletionTokens()));
                            }
                            // 注意：见上方学习点，这里的 firstTokenMs 实为整流耗时而非 TTFT
                            observation.highCardinalityKeyValue("firstTokenMs",
                                    String.valueOf(System.currentTimeMillis() - start));
                            modelUsageRecorder.record(SCENARIO_RAG, provider(), model(),
                                    usage == null ? null : usage.getPromptTokens(),
                                    usage == null ? null : usage.getCompletionTokens(),
                                    signal == SignalType.ON_ERROR ? "FAILED" : "SUCCESS",
                                    signal == SignalType.ON_ERROR ? "stream error" : null);
                            finishLlmObservation(observation, usage, signal == SignalType.ON_ERROR
                                    ? new RuntimeException("stream error") : null);
                        });
                return flux;
            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                TraceContextHolder.clear();
            }
        });
    }

    // ==================== 降级方法（Fallback） ====================

    /**
     * 非流式对话降级。
     * 当 LLM 调用超时/熔断/重试耗尽时，返回友好的降级提示。
     */
    @SuppressWarnings("unused")
    private CompletableFuture<String> fallbackChat(Throwable e) {
        log.warn("LLM 非流式调用降级: {}", e.getMessage());
        // 熔断器打开时的特殊提示
        if (e instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            return CompletableFuture.completedFuture(
                    "AI 助手当前负载较高，服务暂时不可用，请稍后再试。"
            );
        }
        if (e instanceof java.util.concurrent.TimeoutException
                || e.getMessage().contains("timeout")) {
            return CompletableFuture.completedFuture(
                    "AI 助手响应超时，请稍后重试或简化您的问题。"
            );
        }
        return CompletableFuture.completedFuture(
                "AI 助手暂时繁忙，请稍后重试。"
        );
    }

    /**
     * 摘要调用降级。
     */
    @SuppressWarnings("unused")
    private CompletableFuture<String> fallbackSummary(Throwable e) {
        log.warn("LLM 摘要调用降级: {}", e.getMessage());
        return CompletableFuture.completedFuture("");
    }

    /**
     * SSE 流式调用降级。
     * 返回一个包含错误标记的 Flux，让调用方可以通过 SSE 推送错误事件。
     */
    @SuppressWarnings("unused")
    private CompletableFuture<Flux<String>> fallbackSseStream(Throwable e) {
        log.warn("LLM SSE 流式调用降级: {}", e.getMessage());
        String errorMsg;
        if (e instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            errorMsg = "AI 助手当前负载较高，服务暂时不可用。";
        } else if (e instanceof java.util.concurrent.TimeoutException
                || e.getMessage().contains("timeout")) {
            errorMsg = "AI 助手响应超时，请稍后重试。";
        } else {
            errorMsg = "AI 助手暂时繁忙，请稍后重试。";
        }
        return CompletableFuture.completedFuture(Flux.just("[ERROR]" + errorMsg));
    }

    // ==================== LLM 观测辅助 ====================

    /**
     * 启动 LLM 环节观测（Observation 起点）。
     *
     * <p>low-cardinality：span.type / provider / model；high-cardinality：scenario。
     * 由 {@code TraceSpanObservationHandler} 在 onStop 时组装为 TraceSpan 挂到当前 TraceContext；
     * 未开启 trace（采样未命中）时 handler 自动跳过，观测零开销。</p>
     *
     * <p>学习点：这里的 key 分类不是随意的——
     * <ul>
     *   <li><b>low-cardinality</b>（provider/model/span.type）：取值集合小且稳定，
     *       适合作为 OTLP span attribute 与日志索引维度，可安全聚合；</li>
     *   <li><b>high-cardinality</b>（scenario）：取值稍多但仍可枚举，放 high 侧
     *       避免污染低基数标签空间；真正的高基数明细（query、文档 ID）由各业务
     *       埋点点用 detail key 传入。</li>
     * </ul>
     * 统一观测名 OBS_LLM（chat.llm）：同一类调用的 span 名保持一致，后端才能按名聚合。</p>
     */
    private Observation startLlmObservation(String scenario) {
        return Observation.createNotStarted(OBS_LLM, observationRegistry)
                .lowCardinalityKeyValue("span.type", "LLM")
                .lowCardinalityKeyValue("provider", provider())
                .lowCardinalityKeyValue("model", model())
                .highCardinalityKeyValue("scenario", scenario)
                .start();
    }

    /**
     * 结束 LLM 环节观测（成功/失败）。
     *
     * <p>学习点：token 数在 stop 之前才写入 key——Observation 允许在生命周期内随时
     * 追加 key-values，stop 时 handler 统一读取；把 usage 元数据映射为 high-cardinality key
     * 传递，让 span 自带"这次调用消耗了多少 Token"的明细，避免二次查询。</p>
     */
    private void finishLlmObservation(Observation observation, Usage usage, Throwable error) {
        try {
            if (usage != null) {
                observation.highCardinalityKeyValue("promptTokens", String.valueOf(usage.getPromptTokens()))
                        .highCardinalityKeyValue("completionTokens", String.valueOf(usage.getCompletionTokens()));
            }
            if (error != null) {
                // observation.error(e)：把异常"边带"进观测上下文，
                // TraceSpanObservationHandler 在 onStop 时读取 context.getError() 标记 span 失败
                observation.error(error);
            }
        } finally {
            // 必须 stop：Observation 生命周期是 start→stop，漏 stop 会导致 span 永不落库
            observation.stop();
        }
    }

    /** 模型供应商（OpenAI 兼容协议，当前为 DeepSeek） */
    private String provider() {
        return "deepseek";
    }

    /** 当前模型名（从 ChatModel 默认选项读取，避免硬编码） */
    // 学习点：不硬编码模型名而是从 ChatModel 配置读取——模型升级/切换时观测数据
    // 自动跟随，否则换模型后 trace 里的 model 字段还是旧值，成本统计会失真
    private String model() {
        try {
            return chatModel.getDefaultOptions().getModel();
        } catch (Exception e) {
            return "deepseek-chat";
        }
    }
}