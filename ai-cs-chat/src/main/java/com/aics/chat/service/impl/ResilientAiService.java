package com.aics.chat.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
        // supplyAsync 在独立线程执行阻塞的 LLM 调用，便于 TimeLimiter 通过 Future.get(timeout) 实现超时
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("弹性调用 LLM (非流式), messages={}", messages.size());
                String response = chatClient.prompt()
                        .messages(messages)   // 多轮消息直接透传给模型（含历史/系统提示）
                        .call()
                        .content();
                log.debug("LLM 非流式调用成功, responseLength={}", response.length());
                return response;
            } catch (Exception e) {
                throw new CompletionException(e);   // 包装为 CompletionException，供外层 Future.get 解包
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("弹性调用 LLM (RAG 非流式)");
                String response = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
                log.debug("LLM RAG 非流式调用成功, responseLength={}", response.length());
                return response;
            } catch (Exception e) {
                throw new CompletionException(e);
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("弹性调用 LLM (摘要)");
                return chatModel.call(prompt)
                        .getResult()
                        .getOutput()
                        .getText();
            } catch (Exception e) {
                throw new CompletionException(e);
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("弹性调用 LLM (SSE 流式), messages={}", messages.size());
                return chatClient.prompt()
                        .messages(messages)
                        .stream()
                        .content();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * 弹性流式 RAG 调用（SSE）。
     */
    @TimeLimiter(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    @CircuitBreaker(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    public CompletableFuture<Flux<String>> callSseRagStream(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("弹性调用 LLM (SSE RAG 流式)");
                return chatClient.prompt()
                        .user(prompt)
                        .stream()
                        .content();
            } catch (Exception e) {
                throw new CompletionException(e);
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
}