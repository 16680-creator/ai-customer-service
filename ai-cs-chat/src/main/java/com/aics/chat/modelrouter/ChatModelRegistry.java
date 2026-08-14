package com.aics.chat.modelrouter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RefreshScope
@RequiredArgsConstructor
public class ChatModelRegistry {

    private final ModelRouterProperties properties;
    private final ChatClientCustomizer chatClientCustomizer;
    private final ToolCallbackProvider toolCallbackProvider;
    private final QuestionAnswerAdvisor ragAdvisor;

    private volatile Map<String, ModelClientHolder> clients = Map.of();

    @PostConstruct
    void init() {
        rebuild();
    }

    public synchronized void rebuild() {
        properties.validate();
        Map<String, ModelClientHolder> next = new LinkedHashMap<>();
        for (ModelDefinition definition : properties.getModels()) {
            if (!definition.isEnabled()) {
                continue;
            }
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(definition.getBaseUrl())
                    .apiKey(definition.getApiKey())
                    .build();
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model(definition.getModel()).build())
                    .build();
            ChatClient.Builder builder = chatClientCustomizer.customize(ChatClient.builder(chatModel));
            if (definition.getCapabilities().contains(ModelCapability.TOOL_CALLING)) {
                builder = builder.defaultToolCallbacks(toolCallbackProvider);
            }
            next.put(definition.getId(),
                    new ModelClientHolder(definition, chatModel, builder.build()));
        }
        this.clients = Map.copyOf(next);
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
