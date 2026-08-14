package com.aics.chat.modelrouter;

import org.springframework.ai.chat.client.ChatClient;

@FunctionalInterface
public interface ChatClientCustomizer {
    ChatClient.Builder customize(ChatClient.Builder builder);
}
