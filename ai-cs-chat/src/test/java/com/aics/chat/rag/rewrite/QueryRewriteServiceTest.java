package com.aics.chat.rag.rewrite;

import com.aics.chat.modelrouter.ModelScenario;
import com.aics.chat.modelrouter.RoutedChatClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * QueryRewriteService 单元测试：JSON 解析、去重、异常降级。
 */
class QueryRewriteServiceTest {

    private ChatClient chatClient;
    private RoutedChatClientFactory routedChatClientFactory;
    private QueryRewriteService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        routedChatClientFactory = mock(RoutedChatClientFactory.class);
        when(routedChatClientFactory.chatClientFor(ModelScenario.REWRITE)).thenReturn(chatClient);
        com.aics.chat.prompt.PromptRegistry registry = mock(com.aics.chat.prompt.PromptRegistry.class);
        when(registry.render(anyString(), anyMap())).thenReturn(
                new com.aics.chat.prompt.PromptRegistry.RenderedPrompt("sys", "user-text", "rewrite", "v1"));
        service = new QueryRewriteService(routedChatClientFactory, new ObjectMapper(), registry);
    }

    @Test
    @DisplayName("LLM 返回合法 JSON: 解析子查询与 HyDE")
    void rewrite_parses() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("{\"subQueries\": [\"退款功能怎么用\", \"申请退款入口在哪\", \"退款功能怎么用\"], \"hydeDocument\": \"假设文档\"}");
        RewriteResult result = service.rewrite("那个功能怎么用");
        assertThat(result.getSubQueries()).hasSize(2); // 去重
        assertThat(result.getHydeDocument()).isEqualTo("假设文档");
    }

    @Test
    @DisplayName("LLM 返回非法 JSON: 降级为空子查询")
    void rewrite_invalidJson() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("不是 JSON");
        RewriteResult result = service.rewrite("那个功能怎么用");
        assertThat(result.getSubQueries()).isEmpty();
        assertThat(result.getHydeDocument()).isNull();
    }

    @Test
    @DisplayName("LLM 异常: 降级为空子查询")
    void rewrite_exception() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("timeout"));
        RewriteResult result = service.rewrite("那个功能怎么用");
        assertThat(result.getSubQueries()).isEmpty();
    }

    @Test
    @DisplayName("空问题: 直接返回空结果")
    void rewrite_blankQuestion() {
        RewriteResult result = service.rewrite("  ");
        assertThat(result.getSubQueries()).isEmpty();
    }
}