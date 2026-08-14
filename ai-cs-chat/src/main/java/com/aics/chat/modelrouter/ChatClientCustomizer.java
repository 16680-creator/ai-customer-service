package com.aics.chat.modelrouter;

import org.springframework.ai.chat.client.ChatClient;

@FunctionalInterface
public interface ChatClientCustomizer {
    // 设计要点：公共 ChatClient 配置抽成函数式 Customizer——模型注册表为每个模型构建客户端时统一应用，避免多模型配置漂移
    ChatClient.Builder customize(ChatClient.Builder builder);
}
