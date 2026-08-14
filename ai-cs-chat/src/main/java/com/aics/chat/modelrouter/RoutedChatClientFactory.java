package com.aics.chat.modelrouter;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class RoutedChatClientFactory {

    private final ModelRouter modelRouter;
    private final ChatModelRegistry modelRegistry;

    public ChatClient chatClientFor(ModelScenario scenario) {
        RouteDecision decision = modelRouter.route(RouteRequest.builder()
                .scenario(scenario)
                .quotaExceeded(false)
                .requiredCapabilities(Set.of())
                .build());
        if (decision.getSelectedModelId() == null) {
            throw new IllegalStateException("no eligible model for scenario: " + scenario);
        }
        return modelRegistry.get(decision.getSelectedModelId()).getChatClient();
    }
}
