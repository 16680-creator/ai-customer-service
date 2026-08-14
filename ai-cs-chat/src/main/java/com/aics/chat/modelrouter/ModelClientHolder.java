package com.aics.chat.modelrouter;

import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;

@Getter
public class ModelClientHolder {
    private final ModelDefinition definition;
    private final OpenAiChatModel chatModel;
    private final ChatClient chatClient;

    public ModelClientHolder(ModelDefinition definition, OpenAiChatModel chatModel, ChatClient chatClient) {
        this.definition = definition;
        this.chatModel = chatModel;
        this.chatClient = chatClient;
    }
}
