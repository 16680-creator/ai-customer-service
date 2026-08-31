package com.aics.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 对话服务启动类
 * 排除 OpenAiEmbeddingAutoConfiguration：由 SpringAiConfig 手动提供 EmbeddingModel Bean，
 * 指向硅基流动 API（DeepSeek 不支持 /v1/embeddings）。
 */
@EnableFeignClients
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.aics.chat"}, exclude = {
        org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class
})
public class ChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
