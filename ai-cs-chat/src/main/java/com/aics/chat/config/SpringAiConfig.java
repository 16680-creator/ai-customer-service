package com.aics.chat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置
 */
@Configuration
public class SpringAiConfig {

    /**
     * 注册 ChatClient Bean
     * 通过 ChatClient.builder() 构建默认的对话客户端
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个专业的AI客服助手，请根据用户的问题提供准确、友好的回答。如果无法回答，请如实告知。")
                .build();
    }
}
