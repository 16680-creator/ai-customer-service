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
 * <p>使用 Resilience4j 注解（@TimeLimiter / @Retry / @CircuitBreaker）包装底层 LLM 调用，
 * 防止因 AI 服务响应慢或不可用导致线程挂起或雪崩。</p>
 *
 * <h3>调用链</h3>
 * <pre>
 *   Controller → ChatServiceImpl → ResilientAiService
 *                                       ├── [@TimeLimiter] 超时控制
 *                                       ├── [@Retry]       临时故障重试
 *                                       ├── [@CircuitBreaker] 熔断保护
 *                                       └── [@Fallback]    降级响应
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientAiService {

    private final ChatClient chatClient;
    private final OpenAiChatModel chatModel;

    // ==================== 非流式调用（普通对话） ====================

    /**
     * 弹性非流式 LLM 调用。
     * 组合了 TimeLimiter（30s 超时）、Retry（最多 3 次）、CircuitBreaker（熔断）。
     */
    @TimeLimiter(name = "chatService", fallbackMethod = "fallbackChat")
    @Retry(name = "chatService", fallbackMethod = "fallbackChat")
    @CircuitBreaker(name = "chatService", fallbackMethod = "fallbackChat")
    public CompletableFuture<String> callChat(List<Message> messages) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("弹性调用 LLM (非流式), messages={}", messages.size());
                String response = chatClient.prompt()
                        .messages(messages)
                        .call()
                        .content();
                log.debug("LLM 非流式调用成功, responseLength={}", response.length());
                return response;
            } catch (Exception e) {
                throw new CompletionException(e);
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
     * 弹性流式 LLM 调用（SSE）。
     * 使用 TimeLimiter 限制首次 token 到达时间，防止 SSE 连接挂起。
     */
    @TimeLimiter(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    @CircuitBreaker(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    public CompletableFuture<Flux<String>> callSseStream(List<Message> messages) {
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