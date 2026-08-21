package com.aics.chat.agent.workflow;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.confirm.ConfirmationService;
import com.aics.chat.agent.intent.IntentClassifierService;
import com.aics.chat.agent.safety.SafetyGuardService;
import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.agent.store.AgentRunStore;
import com.aics.chat.agent.store.InMemoryAgentRunStore;
import com.aics.chat.agent.store.RedisAgentRunStore;
import com.aics.chat.agent.tool.AgentToolRegistry;
import com.aics.chat.agent.tool.CreateAfterSaleTool;
import com.aics.chat.agent.tool.HandoffTool;
import com.aics.chat.agent.tool.OrderLocatorTool;
import com.aics.chat.agent.tool.PolicyCheckTool;
import com.aics.chat.agent.tool.ProductRecommendTool;
import com.aics.chat.agent.trace.AgentTraceRecorder;
import com.aics.chat.security.ContentSafetyService;
import com.aics.chat.security.SecurityAuditRecorder;
import com.aics.chat.security.ToolAuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AfterSaleAgentServiceContextTest {

    @Test
    @DisplayName("存在多个 AgentRunStore 时售后 Agent 使用 Redis 存储")
    void context_usesRedisRunStoreWhenMultipleStoresExist() {
        new ApplicationContextRunner()
                .withUserConfiguration(AfterSaleAgentServiceContextConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AfterSaleAgentService service = context.getBean(AfterSaleAgentService.class);

                    Object runStore = ReflectionTestUtils.getField(service, "runStore");

                    assertThat(runStore).isInstanceOf(RedisAgentRunStore.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AfterSaleAgentService.class)
    static class AfterSaleAgentServiceContextConfig {

        @Bean
        AgentProperties agentProperties() {
            return new AgentProperties();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        @Bean(name = "inMemoryAgentRunStore")
        InMemoryAgentRunStore inMemoryAgentRunStore() {
            return new InMemoryAgentRunStore();
        }

        @Bean(name = "redisAgentRunStore")
        RedisAgentRunStore redisAgentRunStore(StringRedisTemplate stringRedisTemplate,
                                              ObjectMapper objectMapper,
                                              AgentProperties agentProperties) {
            return new RedisAgentRunStore(stringRedisTemplate, objectMapper, agentProperties);
        }

        @Bean
        SafetyGuardService safetyGuardService() {
            return mock(SafetyGuardService.class);
        }

        @Bean
        IntentClassifierService intentClassifierService() {
            return mock(IntentClassifierService.class);
        }

        @Bean
        AgentStateMachine agentStateMachine() {
            return mock(AgentStateMachine.class);
        }

        @Bean
        AgentToolRegistry agentToolRegistry() {
            return mock(AgentToolRegistry.class);
        }

        @Bean
        ConfirmationService confirmationService() {
            return mock(ConfirmationService.class);
        }

        @Bean
        AgentTraceRecorder agentTraceRecorder() {
            return mock(AgentTraceRecorder.class);
        }

        @Bean
        OrderLocatorTool orderLocatorTool() {
            return mock(OrderLocatorTool.class);
        }

        @Bean
        PolicyCheckTool policyCheckTool() {
            return mock(PolicyCheckTool.class);
        }

        @Bean
        ProductRecommendTool productRecommendTool() {
            return mock(ProductRecommendTool.class);
        }

        @Bean
        CreateAfterSaleTool createAfterSaleTool() {
            return mock(CreateAfterSaleTool.class);
        }

        @Bean
        HandoffTool handoffTool() {
            return mock(HandoffTool.class);
        }

        @Bean
        ObservationRegistry observationRegistry() {
            return ObservationRegistry.create();
        }

        @Bean
        ContentSafetyService contentSafetyService() {
            return mock(ContentSafetyService.class);
        }

        @Bean
        ToolAuthorizationService toolAuthorizationService() {
            return mock(ToolAuthorizationService.class);
        }

        @Bean
        SecurityAuditRecorder securityAuditRecorder() {
            return mock(SecurityAuditRecorder.class);
        }
    }
}
