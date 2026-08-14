package com.aics.chat.modelrouter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutedChatClientFactoryTest {

    @Test
    void chatClientFor_returnsSelectedModelClient() {
        ModelRouter router = mock(ModelRouter.class);
        ChatModelRegistry registry = mock(ChatModelRegistry.class);
        ModelClientHolder holder = mock(ModelClientHolder.class);
        ChatClient client = mock(ChatClient.class);
        when(holder.getChatClient()).thenReturn(client);
        when(registry.get("deepseek-chat")).thenReturn(holder);
        when(router.route(org.mockito.ArgumentMatchers.any(RouteRequest.class)))
                .thenReturn(RouteDecision.builder()
                        .selectedModelId("deepseek-chat")
                        .fallbackChain(List.of())
                        .reason(RouteReason.SCENARIO_DEFAULT)
                        .build());

        RoutedChatClientFactory factory = new RoutedChatClientFactory(router, registry);
        assertSame(client, factory.chatClientFor(ModelScenario.REWRITE));
    }

    @Test
    void chatClientFor_throwsWhenNoEligibleModel() {
        ModelRouter router = mock(ModelRouter.class);
        ChatModelRegistry registry = mock(ChatModelRegistry.class);
        when(router.route(org.mockito.ArgumentMatchers.any(RouteRequest.class)))
                .thenReturn(RouteDecision.builder()
                        .selectedModelId(null)
                        .fallbackChain(List.of())
                        .reason(RouteReason.NO_ELIGIBLE_MODEL)
                        .build());

        RoutedChatClientFactory factory = new RoutedChatClientFactory(router, registry);
        assertThrows(IllegalStateException.class, () -> factory.chatClientFor(ModelScenario.REWRITE));
    }
}
