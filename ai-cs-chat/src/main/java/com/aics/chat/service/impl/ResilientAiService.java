package com.aics.chat.service.impl;

import com.aics.chat.modelrouter.ChatModelRegistry;
import com.aics.chat.modelrouter.ModelCapability;
import com.aics.chat.modelrouter.ModelClientHolder;
import com.aics.chat.modelrouter.ModelHealthRegistry;
import com.aics.chat.modelrouter.ModelRouter;
import com.aics.chat.modelrouter.ModelRouterProperties;
import com.aics.chat.modelrouter.ModelScenario;
import com.aics.chat.modelrouter.RouteDecision;
import com.aics.chat.modelrouter.RouteRequest;
import com.aics.chat.observability.ModelUsageRecorder;
import com.aics.chat.observability.QuotaService;
import com.aics.chat.observability.TraceContext;
import com.aics.chat.observability.TraceContextHolder;
import com.aics.chat.util.ChatUserContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 弹性 AI 调用服务 —— 项目里所有 LLM 调用的统一入口。
 *
 * <h3>核心职责（面试高频考点）</h3>
 * <ol>
 *   <li><b>模型路由</b>：根据 {@link ModelScenario} 场景 + 配额状态 + 能力需求，选出主模型和 fallback 链</li>
 *   <li><b>弹性容错</b>：非流式走 fallback 链逐模型降级；流式不重试，交给熔断器 + 观测</li>
 *   <li><b>熔断保护</b>：每个模型独立 {@link CircuitBreaker}，防止单模型故障拖垮全局</li>
 *   <li><b>瞬时重试</b>：仅对连接/超时类瞬时故障重试一次，避免放大下游压力</li>
 *   <li><b>可观测性</b>：通过 Micrometer {@link Observation} 记录 LLM span（provider/model/token/耗时），接入链路追踪</li>
 *   <li><b>配额管控</b>：调用前检查用户+场景维度配额，超额时路由降级到免费/低成本模型</li>
 * </ol>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>非流式 vs 流式走两条独立路径 —— 因为 Flux 一旦开始发射就不可重放，重试策略完全不同</li>
 *   <li>一次调用只路由一次，结果连同 fallback 链固定给整次调用使用，避免重试时配置/配额抖动导致请求跳到不可预期模型</li>
 *   <li>ThreadLocal（TraceContext、ChatUserContext）跨 supplyAsync 异步线程不传播，必须在进入异步前捕获</li>
 * </ul>
 *
 * <h3>关键类协作</h3>
 * <pre>
 *   Controller → ChatServiceImpl → 【ResilientAiService】→ ModelRouter → ChatModelRegistry → 真实 LLM
 *                                        ↓
 *                              ModelHealthRegistry (熔断器)
 *                              QuotaService (配额)
 *                              ObservationRegistry + ModelUsageRecorder (可观测)
 * </pre>
 *
 * @see ModelRouter
 * @see ModelHealthRegistry
 * @see QuotaService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientAiService {

    /** Micrometer Observation 的 LLM span 名称，用于链路追踪和指标采集 */
    private static final String OBS_LLM = "chat.llm";

    // —— 核心依赖：路由 → 注册表 → 熔断 → 配额 → 观测 ——
    private final ModelRouter modelRouter;
    private final ChatModelRegistry modelRegistry;
    private final ModelHealthRegistry healthRegistry;
    private final QuotaService quotaService;
    private final ModelRouterProperties routerProperties;
    private final ObservationRegistry observationRegistry;
    private final ModelUsageRecorder modelUsageRecorder;

    // 学习点：用函数式接口把"如何调用 LLM"抽象成策略——非流式返回 ChatResponse，流式返回 Flux<String>，
    // 调用方只需传入 lambda，核心模板（路由/熔断/观测/降级）由 invokeNonStream/invokeStream 统一处理
    @FunctionalInterface
    private interface NonStreamCall {
        ChatResponse call(ModelClientHolder holder) throws Exception;
    }

    @FunctionalInterface
    private interface StreamCall {
        Flux<String> call(ModelClientHolder holder) throws Exception;
    }

    // —— 公开 API：5 个方法覆盖所有 LLM 调用场景，分为非流式和流式两组 ——

    /**
     * 多轮对话（非流式）：携带完整消息历史，用于普通聊天场景。
     * 学习点：messages 参数是 Spring AI 的 Message 列表，包含 SystemMessage + 历史 UserMessage/AssistantMessage
     */
    public CompletableFuture<String> callChat(ModelScenario scenario, List<org.springframework.ai.chat.messages.Message> messages) {
        return invokeNonStream(scenario,
                holder -> holder.getChatClient().prompt().messages(messages).call().chatResponse()); // .call() 同步阻塞；.chatResponse() 拿完整响应（含 Usage 元数据）
    }

    /**
     * RAG 问答（非流式）：单条 prompt 字符串，已拼接好知识库上下文。
     * 学习点：与 callChat 的区别在于入参形式——RAG 场景的 prompt 由上游 ChatServiceImpl 拼好，这里只负责发送
     */
    public CompletableFuture<String> callRagChat(ModelScenario scenario, String prompt) {
        return invokeNonStream(scenario,
                holder -> holder.getChatClient().prompt().user(prompt).call().chatResponse());
    }

    /**
     * 会话摘要（非流式）：对历史消息做压缩总结。
     * 学习点：这里直接调用 ChatModel 而非 ChatClient，因为摘要场景不需要 prompt 模板，直接传 Prompt 对象
     */
    public CompletableFuture<String> callSummary(ModelScenario scenario, Prompt prompt) {
        return invokeNonStream(scenario, holder -> holder.getChatModel().call(prompt)); // 注意：ChatModel 是最底层接口，跳过 ChatClient 的 Advisor 链，摘要场景不需要 RAG/日志等拦截器
    }

    /**
     * SSE 流式对话：返回 CompletableFuture<Flux<String>>，外层 Future 用于异步路由，内层 Flux 是 token 流。
     * 学习点：双层异步结构 —— supplyAsync 里做路由选模型（同步），返回的 Flux 才是真正的 token 流（异步 reactive）
     */
    public CompletableFuture<Flux<String>> callSseStream(ModelScenario scenario, List<org.springframework.ai.chat.messages.Message> messages) {
        return invokeStream(scenario,
                holder -> holder.getChatClient().prompt().messages(messages).stream().content()); // .stream() 返回 Flux<ChatResponse>；.content() 只提取文本内容部分，丢弃元数据
    }

    /**
     * SSE 流式 RAG：与 callSseStream 类似，但入参为单条 prompt。
     */
    public CompletableFuture<Flux<String>> callSseRagStream(ModelScenario scenario, String prompt) {
        return invokeStream(scenario,
                holder -> holder.getChatClient().prompt().user(prompt).stream().content());
    }

    /**
     * 非流式调用核心模板 —— 路由 → fallback 链逐模型尝试 → 观测/用量记录。
     *
     * 学习点（关键方法）：
     * 1. 进入 supplyAsync 前先捕获 ThreadLocal（TraceContext + userId），异步线程不会自动继承
     * 2. fallback 链遍历：主模型失败后按优先级依次尝试备用模型，用 fallbackFrom/attempt 记录降级路径
     * 3. 每个模型调用前后都创建/结束 Observation span，用于链路追踪和耗时统计
     * 4. 无论成功失败都调用 modelUsageRecorder.record，保证用量数据完整
     * 5. 所有模型都失败时返回兜底文案，不抛异常 —— 保证用户体验不崩溃
     */
    private CompletableFuture<String> invokeNonStream(ModelScenario scenario, NonStreamCall call) {
        TraceContext captured = TraceContextHolder.capture(); // 快照当前线程的 traceId/spanId，后面在 supplyAsync 线程里恢复
        // 学习点：ChatUserContext 与 TraceContext 一样是 ThreadLocal，跨 supplyAsync 异步线程不传播——必须在进入异步前捕获 userId，否则配额会按匿名用户计算
        Long userId = ChatUserContext.getUserId();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured); // 在 ForkJoinPool 线程里恢复 trace 上下文，后续日志/观测才能关联到原始请求
            try {
                // ① 路由决策：根据场景+配额+能力需求，选出主模型和 fallback 链
                RouteDecision decision = route(scenario, userId);
                if (decision.getSelectedModelId() == null) {
                    log.warn("模型路由无可用模型: scenario={}, reason={}", scenario, decision.getReason());
                    return fallbackText(scenario);
                }
                // ② 组装调用链：主模型 + 备用模型，按优先级排列
                List<String> chain = new ArrayList<>();
                chain.add(decision.getSelectedModelId()); // 第一个是主模型
                chain.addAll(decision.getFallbackChain()); // 后面是按优先级排列的备用模型

                String fallbackFrom = null; // 记录上一个失败的模型 ID，用于观测数据里追溯降级路径
                int attempt = 0;
                Throwable lastError = null;
                // ③ fallback 链遍历：逐模型尝试，一个失败就试下一个
                for (String modelId : chain) {
                    attempt++;
                    try {
                        // ④ 从注册表拿到模型客户端实例（ChatClient/ChatModel）
                        ModelClientHolder holder = modelRegistry.get(modelId);
                        // ⑤ 开启观测 span：记录本次调用用了哪个模型、为什么选它、是第几次尝试
                        Observation observation = startLlmObservation(scenario, holder, decision, fallbackFrom, attempt);
                        try {
                            // ⑥ 实际调用 LLM（带瞬时重试 + 熔断 + 超时保护）
                            ChatResponse response = callWithTransientRetry(holder, call);
                            String text = response == null || response.getResult() == null
                                    ? "" : response.getResult().getOutput().getText(); // 防御性判空：模型可能返回空响应（如 content filter 触发时）
                            Usage usage = response == null || response.getMetadata() == null
                                    ? null : response.getMetadata().getUsage();
                            // ⑦ 结束观测 span：补录 token 用量，触发 span 上报
                            finishLlmObservation(observation, usage, null);
                            // ⑧ 记录用量明细：哪个场景、哪个供应商、花了多少 token —— 用于费用统计/配额扣减
                            modelUsageRecorder.record(scenarioId(scenario), holder.getDefinition().getProvider(),
                                    holder.getDefinition().getModel(),
                                    usage == null ? null : usage.getPromptTokens(),
                                    usage == null ? null : usage.getCompletionTokens(),
                                    "SUCCESS", null, holder.getDefinition().getId());
                            return text;
                        } catch (Exception e) {
                            lastError = e;
                            fallbackFrom = modelId;
                            // 调用失败也要结束观测 + 记录用量，保证数据完整
                            finishLlmObservation(observation, null, e);
                            modelUsageRecorder.record(scenarioId(scenario), holder.getDefinition().getProvider(),
                                    holder.getDefinition().getModel(), null, null, "FAILED", e.getMessage(),
                                    holder.getDefinition().getId());
                        }
                    } catch (Exception e) {
                        // 模型准备阶段就失败了（如 get() 拿不到客户端），跳过进入下一个
                        lastError = e;
                        fallbackFrom = modelId;
                        log.warn("模型准备失败，跳过: modelId={}, err={}", modelId, e.getMessage());
                    }
                }
                log.error("所有模型调用失败: scenario={}, lastError={}",
                        scenario, lastError == null ? null : lastError.getMessage());
                return fallbackText(scenario);
            } finally {
                TraceContextHolder.clear();
            }
        });
    }

    /**
     * 流式调用核心模板 —— 路由 → 单次调用 → reactive 管道（doOnNext/doOnError/doFinally）。
     *
     * 学习点（与非流式的关键差异）：
     * 1. 流式不重试、不走 fallback 链 —— Flux 一旦开始发射 token 就无法重放，中途换模型会导致前端收到重复/乱序 token
     * 2. 路由只执行一次，选定模型后绑定到整个流的生命周期
     * 3. doFinally 里统一处理：熔断器上报成功/失败、Observation 结束、用量记录 —— 因为流结果只有结束时才可知
     * 4. firstTokenMs 记录首 token 延迟（TTFT），是衡量流式体验的核心指标
     * 5. doFinally 可能跑在 reactor 线程而非发起线程，需要先 restore TraceContext
     */
    private CompletableFuture<Flux<String>> invokeStream(ModelScenario scenario, StreamCall call) {
        TraceContext captured = TraceContextHolder.capture();
        Long userId = ChatUserContext.getUserId();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                // ① 路由决策（同非流式，但结果只取主模型，不走 fallback 链）
                RouteDecision decision = route(scenario, userId);
                if (decision.getSelectedModelId() == null) {
                    return Flux.just("[ERROR]AI 助手暂时繁忙，请稍后重试。");
                }
                ModelClientHolder holder = null;
                CircuitBreaker breaker = null;
                try {
                    // ② 拿到模型客户端 + 该模型专属的熔断器实例
                    holder = modelRegistry.get(decision.getSelectedModelId());
                    breaker = healthRegistry.breaker(holder.getDefinition().getId());
                    ModelClientHolder selectedHolder = holder;       // 赋值给 effectively final 变量，才能在 lambda 里引用
                    CircuitBreaker selectedBreaker = breaker;        // 同上，lambda 捕获的变量必须是 final 或 effectively final
                    // ③ 发起流式调用：返回 Flux，此时还没真正请求 LLM（lazy），订阅后才开始
                    Flux<String> flux = call.call(holder);
                    // ④ 准备三个"跨管道传递状态"的容器：流式场景里结果只能在 doFinally 里拿到
                    AtomicReference<Usage> usageRef = new AtomicReference<>();      // 流式场景里 Usage 只能在最后一个 chunk 里拿到，用 AtomicReference 跨 reactive 管道传递
                    AtomicReference<Throwable> errorRef = new AtomicReference<>();  // 同理，doOnError 里捕获异常，doFinally 里读取并上报
                    AtomicLong firstTokenMs = new AtomicLong(-1);                   // -1 表示"还没收到第一个 token"，用 AtomicLong 保证 doOnNext 里 CAS 只记录一次
                    long start = System.currentTimeMillis();
                    TraceContext streamTrace = TraceContextHolder.capture();
                    return flux
                            // ⑤ doOnNext：每收到一个 chunk 触发，这里只记录首 token 延迟（TTFT）
                            .doOnNext(chunk -> {
                                if (firstTokenMs.get() < 0) { // < 0 说明是第一个 chunk，记录 TTFT（Time To First Token）
                                    firstTokenMs.set(System.currentTimeMillis() - start);
                                }
                            })
                            // ⑥ doOnError：流出错时捕获异常对象，留给 doFinally 上报
                            .doOnError(errorRef::set)
                            // ⑦ doFinally：流结束（无论成功/失败/取消）统一收尾 —— 通知熔断器、记录观测、记录用量
                            .doFinally(signal -> {
                                TraceContextHolder.restore(streamTrace);
                                try {
                                    long durationMs = System.currentTimeMillis() - start;
                                    if (signal == SignalType.ON_COMPLETE) { // 流正常结束 → 通知熔断器成功
                                        selectedBreaker.onSuccess(durationMs, TimeUnit.MILLISECONDS);
                                    } else if (signal == SignalType.ON_ERROR) { // 流出错 → 通知熔断器失败，熔断器累计失败次数达到阈值后会自动打开
                                        Throwable streamError = errorRef.get();
                                        selectedBreaker.onError(durationMs, TimeUnit.MILLISECONDS,
                                                streamError == null ? new RuntimeException("stream error") : streamError);
                                    }
                                    Usage usage = usageRef.get();
                                    // ⑧ 流结束后才能创建完整观测（因为 token 数只有结束时才知道）
                                    Observation observation = startLlmObservation(
                                            scenario, selectedHolder, decision, null, 1);
                                    if (usage != null) {
                                        observation.highCardinalityKeyValue("promptTokens",
                                                String.valueOf(usage.getPromptTokens()))
                                                .highCardinalityKeyValue("completionTokens",
                                                        String.valueOf(usage.getCompletionTokens()));
                                    }
                                    if (firstTokenMs.get() >= 0) {
                                        observation.highCardinalityKeyValue("firstTokenMs",
                                                String.valueOf(firstTokenMs.get()));
                                    }
                                    // ⑨ 记录用量明细（同非流式，但状态由 signal 决定 SUCCESS/FAILED）
                                    modelUsageRecorder.record(scenarioId(scenario),
                                            selectedHolder.getDefinition().getProvider(),
                                            selectedHolder.getDefinition().getModel(),
                                            usage == null ? null : usage.getPromptTokens(),
                                            usage == null ? null : usage.getCompletionTokens(),
                                            signal == SignalType.ON_ERROR ? "FAILED" : "SUCCESS",
                                            signal == SignalType.ON_ERROR ? "stream error" : null,
                                            selectedHolder.getDefinition().getId());
                                    finishLlmObservation(observation, usage,
                                            signal == SignalType.ON_ERROR
                                                    ? new RuntimeException("stream error") : null);
                                } finally {
                                    TraceContextHolder.clear();
                                }
                            });
                } catch (Exception e) {
                    if (breaker != null) {
                        breaker.onError(0, TimeUnit.MILLISECONDS, e); // 流还没开始就失败了，duration=0，但必须通知熔断器，否则失败计数不准
                    }
                    log.warn("模型流式调用初始化失败: modelId={}, err={}",
                            decision.getSelectedModelId(), e.getMessage());
                    return Flux.just("[ERROR]AI 助手暂时繁忙，请稍后重试。");
                }
            } finally {
                TraceContextHolder.clear();
            }
        });
    }

    /**
     * 瞬时故障重试器 —— 只对连接超时、Socket 超时等网络类瞬时故障重试一次。
     *
     * 学习点：
     * 1. 为什么只重试一次？LLM 调用成本高，多次重试会放大下游压力和费用
     * 2. 为什么只重试瞬时故障？4xx/业务异常重试没有意义，模型返回"无法回答"不会因重试而改变
     * 3. ResourceAccessException = Spring 的网络异常包装；SocketTimeoutException = TCP 层超时；TimeoutException = 我们自己的超时控制
     */
    // 学习点：只对连接/超时类瞬时故障重试且仅一次——4xx 或业务异常重试没有意义，过多重试会放大下游压力与费用
    private ChatResponse callWithTransientRetry(ModelClientHolder holder, NonStreamCall call) throws Exception {
        // 拿到该模型的熔断器：熔断器会记住最近 N 次调用的成功/失败率，失败率超阈值就自动"打开"，直接快速失败不再调用下游
        CircuitBreaker breaker = healthRegistry.breaker(holder.getDefinition().getId());
        try {
            return timedCall(breaker, holder, call); // 第一次尝试：带熔断+超时的调用
        } catch (ResourceAccessException | SocketTimeoutException | TimeoutException e) {
            // 只有网络类瞬时故障才重试，业务异常（如模型返回错误码）不重试
            log.warn("模型瞬时故障，重试一次: modelId={}, err={}",
                    holder.getDefinition().getId(), e.getMessage());
            return timedCall(breaker, holder, call); // 第二次（最后一次）尝试
        }
    }

    /**
     * 带熔断 + 超时的单次模型调用 —— 弹性保护的最小执行单元。
     *
     * 学习点：
     * 1. 熔断器粒度：每个模型独立 CircuitBreaker（不是全局共享），主模型故障不会连累备用模型
     * 2. 超时控制：用 CompletableFuture.get(timeout) 而非 HTTP 层超时，好处是超时后 cancel(true) 能及时释放挂起调用
     * 3. 异常解包：CompletableFuture 会把真实异常包成 ExecutionException/CompletionException，
     *    必须逐层 unwrap 才能正确判断"是瞬时故障还是业务失败"，否则 fallback 原因会记错
     */
    private ChatResponse timedCall(CircuitBreaker breaker, ModelClientHolder holder, NonStreamCall call) throws Exception {
        // 设计要点：熔断与超时都在单个模型粒度执行——共享熔断器会让主模型故障连累备用模型；超时后 cancel(true) 及时释放挂起调用
        // breaker.executeCallable：把整个调用包在熔断器里，熔断器打开时会直接抛 CallNotPermittedException，不真正调用下游
        return breaker.executeCallable(() -> {
            CompletableFuture<ChatResponse> future = CompletableFuture.supplyAsync(() -> { // 异步执行 LLM 调用，这样才能用 future.get(timeout) 做超时控制
                try {
                    return call.call(holder); // 真正发起 LLM HTTP 调用
                } catch (Exception e) {
                    throw new CompletionException(e); // CompletableFuture 要求 lambda 里用 CompletionException 包装 checked exception
                }
            });
            try {
                return future.get(holder.getDefinition().getTimeoutMs(), TimeUnit.MILLISECONDS); // 核心：按模型配置的 timeoutMs 阻塞等待，超时抛 TimeoutException
            } catch (TimeoutException e) {
                future.cancel(true); // true = 发送 interrupt() 中断挂起的 HTTP 连接，释放线程池资源
                throw e;
            } catch (ExecutionException | CompletionException e) {
                // 学习点：CompletableFuture 会把真实异常包成 ExecutionException/CompletionException——必须先逐层解包，否则"瞬时故障/业务失败"判定会失真，fallback 原因也会记错
                Throwable cause = e.getCause();
                if (cause instanceof CompletionException && cause.getCause() != null) { // 双层解包：supplyAsync 包一层 CompletionException，future.get 又包一层 ExecutionException
                    cause = cause.getCause();
                }
                if (cause instanceof Exception ex) {
                    throw ex; // 解包后重新抛出，让外层 catch 能正确判断是瞬时故障还是业务失败
                }
                throw new RuntimeException(cause == null ? e : cause);
            }
        });
    }

    /**
     * 路由决策 —— 一次调用只路由一次，结果（含 fallback 链）在整次调用中固定不变。
     *
     * 学习点：
     * 1. 配额检查在路由前完成：quotaExceeded=true 时，路由器会跳过付费模型，降级到免费/低成本模型
     * 2. requiredCapabilities 告诉路由器"这个场景需要模型支持什么能力"（如 TOOL_CALLING），不满足的模型会被过滤
     * 3. 为什么不在重试时重新路由？配置/配额可能在两次路由之间发生变化，导致请求跳到不可预期的模型
     */
    // 设计要点：一次调用只路由一次，结果连同 fallback 链固定给整次调用使用——重试时不重新路由，避免配置/配额抖动导致同一次请求跳到不可预期模型
    private RouteDecision route(ModelScenario scenario, Long userId) {
        // ① 配额检查：quotaService.check() 查 Redis 里该用户+场景的调用次数/ token 总量是否超限
        boolean quotaExceeded = routerProperties.getQuota().isEnabled()
                && quotaService.check(userId, scenarioId(scenario)).isExceeded(); // 短路求值：配额功能未开启时直接跳过 check，避免无谓的 Redis 调用
        // ② 路由决策：modelRouter 根据 场景+配额状态+能力需求 从配置里选出主模型和 fallback 链
        return modelRouter.route(RouteRequest.builder()
                .scenario(scenario)
                .quotaExceeded(quotaExceeded)
                .requiredCapabilities(requiredCapabilities(scenario))
                .build());
    }

    /**
     * 场景 → 能力映射：告诉路由器该场景需要模型具备什么能力。
     * 学习点：CHAT/RAG/AGENT/NL2SQL 都需要 TOOL_CALLING（函数调用），因为可能涉及工具使用；SUMMARY/REWRITE 等纯文本场景不需要
     */
    private Set<ModelCapability> requiredCapabilities(ModelScenario scenario) {
        return switch (scenario) {
            // 需要工具调用能力的场景：CHAT 可能触发搜索、RAG 触发检索、AGENT 触发函数调用、NL2SQL 触发 SQL 执行
            case CHAT, RAG, AGENT, NL2SQL -> EnumSet.of(ModelCapability.TOOL_CALLING);
            // 纯文本场景不需要任何特殊能力，任意模型都能处理
            default -> EnumSet.noneOf(ModelCapability.class);
        };
    }

    /** 场景枚举 → 小写字符串，用于配额键、观测标签等需要字符串标识的地方 */
    private String scenarioId(ModelScenario scenario) {
        return scenario.name().toLowerCase(Locale.ROOT); // 用 Locale.ROOT 避免土耳其语等特殊 locale 下 "CHAT" → "ıchat" 的坑
    }

    /**
     * 兜底文案：所有模型都失败时的降级响应。
     * 学习点：SUMMARY 场景返回空串而非提示文案，因为摘要结果会直接拼入上下文，文案会污染后续对话
     */
    private String fallbackText(ModelScenario scenario) {
        if (scenario == ModelScenario.SUMMARY) {
            return ""; // 摘要场景：返回空串，避免文案被拼入上下文污染后续对话
        }
        return "AI 助手暂时繁忙，请稍后重试。"; // 普通场景：返回友好提示文案给前端展示
    }

    /**
     * 创建 LLM 调用观测 span —— 接入 Micrometer Observation API，可对接 Prometheus/Jaeger/Zipkin。
     *
     * 学习点：
     * 1. lowCardinalityKeyValue：低基数标签（provider/model），适合做 Prometheus 指标维度，不会导致时间序列爆炸
     * 2. highCardinalityKeyValue：高基数标签（scenario/modelId/attempt），只用于链路追踪详情，不进指标
     * 3. fallbackFrom + attempt 让观测数据能回答"为什么最终落到了备用模型"这个问题
     */
    private Observation startLlmObservation(ModelScenario scenario, ModelClientHolder holder,
                                            RouteDecision decision, String fallbackFrom, int attempt) {
        // 创建 LLM 调用观测 span：每次调用 LLM 前 start 一个 span，结束后 stop，
        // 数据会发送到 Jaeger/Zipkin 做链路追踪，或 Prometheus 做指标聚合
        return Observation.createNotStarted(OBS_LLM, observationRegistry) // createNotStarted = 手动控制 start() 时机，而非构造时自动开始
                .lowCardinalityKeyValue("span.type", "LLM")
                .lowCardinalityKeyValue("provider", holder.getDefinition().getProvider()) // low = 低基数，适合 Prometheus 指标维度
                .lowCardinalityKeyValue("model", holder.getDefinition().getModel())
                .highCardinalityKeyValue("scenario", scenarioId(scenario)) // high = 高基数，只用于链路追踪详情，不进 Prometheus 指标
                .highCardinalityKeyValue("modelId", holder.getDefinition().getId())
                .highCardinalityKeyValue("routeReason", decision.getReason().name())
                .highCardinalityKeyValue("fallbackFrom", fallbackFrom == null ? "" : fallbackFrom)
                .highCardinalityKeyValue("attempt", String.valueOf(attempt))
                .start();
    }

    /**
     * 结束观测 span —— 补录 token 用量和异常信息，最后 stop() 触发 span 上报。
     * 学习点：observation.stop() 必须放在 finally 里，确保无论成功/失败都结束 span，否则链路追踪会出现"幽灵 span"（只有开始没有结束）
     */
    private void finishLlmObservation(Observation observation, Usage usage, Throwable error) {
        try {
            if (usage != null) {
                // 补录 token 用量到 span：promptTokens = 输入 token，completionTokens = 输出 token
                observation.highCardinalityKeyValue("promptTokens", String.valueOf(usage.getPromptTokens()))
                        .highCardinalityKeyValue("completionTokens", String.valueOf(usage.getCompletionTokens())); // token 用量是高基数信息，只在 trace 详情里看
            }
            if (error != null) {
                observation.error(error); // 标记 span 为 error 状态，Jaeger/Zipkin 里会高亮显示
            }
        } finally {
            observation.stop(); // stop() 触发 span 上报到链路追踪系统，必须在 finally 里保证一定执行
        }
    }
}
