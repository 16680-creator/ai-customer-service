package com.aics.chat.modelrouter;

import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;

@Getter
public class ModelClientHolder {
    // 设计要点：同一模型的 ChatModel 与 ChatClient 一起持有——摘要等原生调用和统一入口都能从注册表一次取到，避免按 ID 二次查找
    private final ModelDefinition definition;
    private final OpenAiChatModel chatModel;
    private final ChatClient chatClient;

    public ModelClientHolder(ModelDefinition definition, OpenAiChatModel chatModel, ChatClient chatClient) {
        this.definition = definition;
        this.chatModel = chatModel;
        this.chatClient = chatClient;
    }
}
