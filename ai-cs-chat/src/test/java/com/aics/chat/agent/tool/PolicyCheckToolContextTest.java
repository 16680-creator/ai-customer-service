package com.aics.chat.agent.tool;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.model.AfterSaleActionType;
import com.aics.chat.service.KnowledgeBaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyCheckToolContextTest {

    @Test
    @DisplayName("存在多个 RuleProvider 时 PolicyCheckTool 使用知识库规则提供者")
    void context_usesKnowledgeRuleProviderWhenMultipleProvidersExist() {
        new ApplicationContextRunner()
                .withUserConfiguration(PolicyCheckToolContextConfig.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    PolicyCheckTool tool = context.getBean(PolicyCheckTool.class);
                    KnowledgeBaseService knowledgeBaseService = context.getBean(KnowledgeBaseService.class);

                    tool.check(AfterSaleActionType.EXCHANGE, LocalDateTime.now().minusDays(1));

                    assertNotNull(tool);
                    verify(knowledgeBaseService).search(anyString(), anyString(), anyInt(), anyDouble());
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(PolicyCheckTool.class)
    static class PolicyCheckToolContextConfig {

        @Bean
        AgentProperties agentProperties() {
            return new AgentProperties();
        }

        @Bean
        KnowledgeBaseService knowledgeBaseService() {
            KnowledgeBaseService service = mock(KnowledgeBaseService.class);
            when(service.search(anyString(), anyString(), anyInt(), anyDouble())).thenReturn(List.of());
            return service;
        }

        @Bean(name = "staticRuleProvider")
        StaticRuleProvider staticRuleProvider() {
            return new StaticRuleProvider();
        }

        @Bean(name = "knowledgeRuleProvider")
        KnowledgeRuleProvider knowledgeRuleProvider(KnowledgeBaseService knowledgeBaseService,
                                                    AgentProperties agentProperties,
                                                    StaticRuleProvider staticRuleProvider) {
            return new KnowledgeRuleProvider(knowledgeBaseService, agentProperties, staticRuleProvider);
        }
    }
}
