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

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientAiService {

    private static final String OBS_LLM = "chat.llm";

    private final ModelRouter modelRouter;
    private final ChatModelRegistry modelRegistry;
    private final ModelHealthRegistry healthRegistry;
    private final QuotaService quotaService;
    private final ModelRouterProperties routerProperties;
    private final ObservationRegistry observationRegistry;
    private final ModelUsageRecorder modelUsageRecorder;

    @FunctionalInterface
    private interface NonStreamCall {
        ChatResponse call(ModelClientHolder holder) throws Exception;
    }

    @FunctionalInterface
    private interface StreamCall {
        Flux<String> call(ModelClientHolder holder) throws Exception;
    }

    public CompletableFuture<String> callChat(ModelScenario scenario, List<org.springframework.ai.chat.messages.Message> messages) {
        return invokeNonStream(scenario,
                holder -> holder.getChatClient().prompt().messages(messages).call().chatResponse());
    }

    public CompletableFuture<String> callRagChat(ModelScenario scenario, String prompt) {
        return invokeNonStream(scenario,
                holder -> holder.getChatClient().prompt().user(prompt).call().chatResponse());
    }

    public CompletableFuture<String> callSummary(ModelScenario scenario, Prompt prompt) {
        return invokeNonStream(scenario, holder -> holder.getChatModel().call(prompt));
    }

    public CompletableFuture<Flux<String>> callSseStream(ModelScenario scenario, List<org.springframework.ai.chat.messages.Message> messages) {
        return invokeStream(scenario,
                holder -> holder.getChatClient().prompt().messages(messages).stream().content());
    }

    public CompletableFuture<Flux<String>> callSseRagStream(ModelScenario scenario, String prompt) {
        return invokeStream(scenario,
                holder -> holder.getChatClient().prompt().user(prompt).stream().content());
    }

    private CompletableFuture<String> invokeNonStream(ModelScenario scenario, NonStreamCall call) {
        TraceContext captured = TraceContextHolder.capture();
        Long userId = ChatUserContext.getUserId();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                RouteDecision decision = route(scenario, userId);
                if (decision.getSelectedModelId() == null) {
                    log.warn("模型路由无可用模型: scenario={}, reason={}", scenario, decision.getReason());
                    return fallbackText(scenario);
                }
                List<String> chain = new ArrayList<>();
                chain.add(decision.getSelectedModelId());
                chain.addAll(decision.getFallbackChain());

                String fallbackFrom = null;
                int attempt = 0;
                Throwable lastError = null;
                for (String modelId : chain) {
                    attempt++;
                    try {
                        ModelClientHolder holder = modelRegistry.get(modelId);
                        Observation observation = startLlmObservation(scenario, holder, decision, fallbackFrom, attempt);
                        try {
                            ChatResponse response = callWithTransientRetry(holder, call);
                            String text = response == null || response.getResult() == null
                                    ? "" : response.getResult().getOutput().getText();
                            Usage usage = response == null || response.getMetadata() == null
                                    ? null : response.getMetadata().getUsage();
                            finishLlmObservation(observation, usage, null);
                            modelUsageRecorder.record(scenarioId(scenario), holder.getDefinition().getProvider(),
                                    holder.getDefinition().getModel(),
                                    usage == null ? null : usage.getPromptTokens(),
                                    usage == null ? null : usage.getCompletionTokens(),
                                    "SUCCESS", null, holder.getDefinition().getId());
                            return text;
                        } catch (Exception e) {
                            lastError = e;
                            fallbackFrom = modelId;
                            finishLlmObservation(observation, null, e);
                            modelUsageRecorder.record(scenarioId(scenario), holder.getDefinition().getProvider(),
                                    holder.getDefinition().getModel(), null, null, "FAILED", e.getMessage(),
                                    holder.getDefinition().getId());
                        }
                    } catch (Exception e) {
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

    private CompletableFuture<Flux<String>> invokeStream(ModelScenario scenario, StreamCall call) {
        TraceContext captured = TraceContextHolder.capture();
        Long userId = ChatUserContext.getUserId();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                RouteDecision decision = route(scenario, userId);
                if (decision.getSelectedModelId() == null) {
                    return Flux.just("[ERROR]AI 助手暂时繁忙，请稍后重试。");
                }
                ModelClientHolder holder = null;
                CircuitBreaker breaker = null;
                try {
                    holder = modelRegistry.get(decision.getSelectedModelId());
                    breaker = healthRegistry.breaker(holder.getDefinition().getId());
                    ModelClientHolder selectedHolder = holder;
                    CircuitBreaker selectedBreaker = breaker;
                    Flux<String> flux = call.call(holder);
                    AtomicReference<Usage> usageRef = new AtomicReference<>();
                    AtomicReference<Throwable> errorRef = new AtomicReference<>();
                    AtomicLong firstTokenMs = new AtomicLong(-1);
                    long start = System.currentTimeMillis();
                    TraceContext streamTrace = TraceContextHolder.capture();
                    return flux
                            .doOnNext(chunk -> {
                                if (firstTokenMs.get() < 0) {
                                    firstTokenMs.set(System.currentTimeMillis() - start);
                                }
                            })
                            .doOnError(errorRef::set)
                            .doFinally(signal -> {
                                TraceContextHolder.restore(streamTrace);
                                try {
                                    long durationMs = System.currentTimeMillis() - start;
                                    if (signal == SignalType.ON_COMPLETE) {
                                        selectedBreaker.onSuccess(durationMs, TimeUnit.MILLISECONDS);
                                    } else if (signal == SignalType.ON_ERROR) {
                                        Throwable streamError = errorRef.get();
                                        selectedBreaker.onError(durationMs, TimeUnit.MILLISECONDS,
                                                streamError == null ? new RuntimeException("stream error") : streamError);
                                    }
                                    Usage usage = usageRef.get();
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
                        breaker.onError(0, TimeUnit.MILLISECONDS, e);
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

    private ChatResponse callWithTransientRetry(ModelClientHolder holder, NonStreamCall call) throws Exception {
        CircuitBreaker breaker = healthRegistry.breaker(holder.getDefinition().getId());
        try {
            return timedCall(breaker, holder, call);
        } catch (ResourceAccessException | SocketTimeoutException | TimeoutException e) {
            log.warn("模型瞬时故障，重试一次: modelId={}, err={}",
                    holder.getDefinition().getId(), e.getMessage());
            return timedCall(breaker, holder, call);
        }
    }

    private ChatResponse timedCall(CircuitBreaker breaker, ModelClientHolder holder, NonStreamCall call) throws Exception {
        return breaker.executeCallable(() -> {
            CompletableFuture<ChatResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return call.call(holder);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
            try {
                return future.get(holder.getDefinition().getTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw e;
            } catch (ExecutionException | CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof CompletionException && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                if (cause instanceof Exception ex) {
                    throw ex;
                }
                throw new RuntimeException(cause == null ? e : cause);
            }
        });
    }

    private RouteDecision route(ModelScenario scenario, Long userId) {
        boolean quotaExceeded = routerProperties.getQuota().isEnabled()
                && quotaService.check(userId, scenarioId(scenario)).isExceeded();
        return modelRouter.route(RouteRequest.builder()
                .scenario(scenario)
                .quotaExceeded(quotaExceeded)
                .requiredCapabilities(requiredCapabilities(scenario))
                .build());
    }

    private Set<ModelCapability> requiredCapabilities(ModelScenario scenario) {
        return switch (scenario) {
            case CHAT, RAG, AGENT, NL2SQL -> EnumSet.of(ModelCapability.TOOL_CALLING);
            default -> EnumSet.noneOf(ModelCapability.class);
        };
    }

    private String scenarioId(ModelScenario scenario) {
        return scenario.name().toLowerCase(Locale.ROOT);
    }

    private String fallbackText(ModelScenario scenario) {
        if (scenario == ModelScenario.SUMMARY) {
            return "";
        }
        return "AI 助手暂时繁忙，请稍后重试。";
    }

    private Observation startLlmObservation(ModelScenario scenario, ModelClientHolder holder,
                                            RouteDecision decision, String fallbackFrom, int attempt) {
        return Observation.createNotStarted(OBS_LLM, observationRegistry)
                .lowCardinalityKeyValue("span.type", "LLM")
                .lowCardinalityKeyValue("provider", holder.getDefinition().getProvider())
                .lowCardinalityKeyValue("model", holder.getDefinition().getModel())
                .highCardinalityKeyValue("scenario", scenarioId(scenario))
                .highCardinalityKeyValue("modelId", holder.getDefinition().getId())
                .highCardinalityKeyValue("routeReason", decision.getReason().name())
                .highCardinalityKeyValue("fallbackFrom", fallbackFrom == null ? "" : fallbackFrom)
                .highCardinalityKeyValue("attempt", String.valueOf(attempt))
                .start();
    }

    private void finishLlmObservation(Observation observation, Usage usage, Throwable error) {
        try {
            if (usage != null) {
                observation.highCardinalityKeyValue("promptTokens", String.valueOf(usage.getPromptTokens()))
                        .highCardinalityKeyValue("completionTokens", String.valueOf(usage.getCompletionTokens()));
            }
            if (error != null) {
                observation.error(error);
            }
        } finally {
            observation.stop();
        }
    }
}
