package com.aics.chat.modelrouter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelRegistry {

    private final ModelRouterProperties properties;
    private final ChatClientCustomizer chatClientCustomizer;
    private final ToolCallbackProvider toolCallbackProvider;
    private final QuestionAnswerAdvisor ragAdvisor;

    // 设计要点：注册表保持长生命周期单例，只换不可变 Map 快照；运行时健康状态不与 RefreshScope Bean 一起重建
    private volatile Map<String, ModelClientHolder> clients = Map.of();

    @PostConstruct
    void init() {
        rebuild();
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefreshScopeRefreshed(RefreshScopeRefreshedEvent event) {
        rebuild();
    }

    public synchronized void rebuild() {
        try {
            properties.validate();
            Map<String, ModelClientHolder> next = new LinkedHashMap<>();
            for (ModelDefinition definition : properties.getModels()) {
                if (!definition.isEnabled()) {
                    continue;
                }
                ModelClientHolder existing = clients.get(definition.getId());
                // 学习点：配置未变化时复用旧客户端——重建 OpenAiChatModel 有初始化成本，刷新时批量重建会造成不必要的连接抖动
                if (existing != null && sameDefinition(existing.getDefinition(), definition)) {
                    next.put(definition.getId(), existing);
                    continue;
                }
                // 设计要点：每个模型持有独立 OpenAiApi/OpenAiChatModel——不同 baseUrl/apiKey 无法共享底层客户端，隔离也防止单模型配置污染其他模型
                OpenAiApi api = OpenAiApi.builder()
                        .baseUrl(definition.getBaseUrl())
                        .apiKey(definition.getApiKey())
                        .build();
                OpenAiChatModel chatModel = OpenAiChatModel.builder()
                        .openAiApi(api)
                        .defaultOptions(OpenAiChatOptions.builder().model(definition.getModel()).build())
                        .build();
                ChatClient.Builder builder = chatClientCustomizer.customize(ChatClient.builder(chatModel));
                // 设计要点：只有声明 TOOL_CALLING 能力的模型才挂工具回调，避免不支持函数调用的模型收到工具声明后报错
                if (definition.getCapabilities().contains(ModelCapability.TOOL_CALLING)) {
                    builder = builder.defaultToolCallbacks(toolCallbackProvider);
                }
                next.put(definition.getId(),
                        new ModelClientHolder(definition, chatModel, builder.build()));
            }
            // 设计要点：先构建完整 next 再一次性替换 volatile 引用——运行中的请求永远看不到半成品注册表，替换对并发读取原子可见
            this.clients = Map.copyOf(next);
        } catch (Exception e) {
            // 学习点：启动失败必须 fail-fast（没有旧快照可回退）；刷新失败则保留旧配置，可用性优先于配置即时生效
            if (clients.isEmpty()) {
                throw new IllegalStateException("failed to initialize model registry", e);
            }
            log.error("模型路由配置刷新失败，保留旧配置: err={}", e.getMessage(), e);
        }
    }

    private boolean sameDefinition(ModelDefinition existing, ModelDefinition next) {
        return Objects.equals(existing.getId(), next.getId())
                && Objects.equals(existing.getProvider(), next.getProvider())
                && Objects.equals(existing.getBaseUrl(), next.getBaseUrl())
                && Objects.equals(existing.getApiKey(), next.getApiKey())
                && Objects.equals(existing.getModel(), next.getModel())
                && existing.isEnabled() == next.isEnabled()
                && existing.getPriority() == next.getPriority()
                && Objects.equals(existing.getTier(), next.getTier())
                && Objects.equals(existing.getCapabilities(), next.getCapabilities())
                && existing.getContextWindow() == next.getContextWindow()
                && existing.getTimeoutMs() == next.getTimeoutMs();
    }

    public boolean contains(String modelId) {
        return clients.containsKey(modelId);
    }

    public ModelClientHolder get(String modelId) {
        ModelClientHolder holder = clients.get(modelId);
        if (holder == null) {
            throw new IllegalStateException("model not registered: " + modelId);
        }
        return holder;
    }
}
