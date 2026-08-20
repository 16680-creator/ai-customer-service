package com.aics.chat.observability;

import com.aics.chat.dto.OnlineEvalRecordDTO;
import com.aics.chat.feign.OnlineEvalFeignClient;
import com.aics.chat.rag.eval.LlmJudgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OnlineEvalService 单元测试：采样命中评分落库、评分失败标记 FAILED、未启用跳过。
 */
@ExtendWith(MockitoExtension.class)
class OnlineEvalServiceTest {

    @Mock
    private LlmJudgeService llmJudgeService;

    @Mock
    private OnlineEvalFeignClient feignClient;

    private OnlineEvalProperties properties;

    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        properties = new OnlineEvalProperties();
        properties.setEnabled(true);
        properties.setSampleRate(1.0);
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("test-eval-");
        executor.initialize();
    }

    private OnlineEvalService newService() {
        return new OnlineEvalService(properties, new OnlineEvalSampler(), llmJudgeService, feignClient, executor);
    }

    @Test
    @DisplayName("采样命中：异步评分并落库 SUCCESS")
    void evaluateAsync_success() throws Exception {
        when(llmJudgeService.score(any(), any(), any())).thenReturn(4);
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(feignClient).recordEval(any());

        newService().evaluateAsync("req-1", 10L, 1L, "问题", "回答");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        ArgumentCaptor<OnlineEvalRecordDTO> captor = ArgumentCaptor.forClass(OnlineEvalRecordDTO.class);
        verify(feignClient).recordEval(captor.capture());
        OnlineEvalRecordDTO dto = captor.getValue();
        assertEquals("req-1", dto.getRequestId());
        assertEquals("SUCCESS", dto.getJudgeStatus());
        assertEquals(4, dto.getLlmScore());
    }

    @Test
    @DisplayName("评分返回 null：标记 FAILED 落库")
    void evaluateAsync_scoreNull_failed() throws Exception {
        when(llmJudgeService.score(any(), any(), any())).thenReturn(null);
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(feignClient).recordEval(any());

        newService().evaluateAsync("req-2", null, null, "问题", "回答");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        ArgumentCaptor<OnlineEvalRecordDTO> captor = ArgumentCaptor.forClass(OnlineEvalRecordDTO.class);
        verify(feignClient).recordEval(captor.capture());
        assertEquals("FAILED", captor.getValue().getJudgeStatus());
    }

    @Test
    @DisplayName("评分异常：标记 FAILED 且不重试")
    void evaluateAsync_scoreThrows_failed() throws Exception {
        when(llmJudgeService.score(any(), any(), any())).thenThrow(new RuntimeException("judge down"));
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(feignClient).recordEval(any());

        newService().evaluateAsync("req-3", null, null, "问题", "回答");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        verify(feignClient, times(1)).recordEval(any());
    }

    @Test
    @DisplayName("线上评估关闭时跳过（零调用）")
    void evaluateAsync_disabled_skips() {
        properties.setEnabled(false);
        newService().evaluateAsync("req-4", null, null, "问题", "回答");
        verifyNoInteractions(llmJudgeService);
        verifyNoInteractions(feignClient);
    }

    @Test
    @DisplayName("采样率 0 时跳过")
    void evaluateAsync_zeroSample_skips() {
        properties.setSampleRate(0);
        newService().evaluateAsync("req-5", null, null, "问题", "回答");
        verifyNoInteractions(llmJudgeService);
    }

    @Test
    @DisplayName("落库失败仅告警，不抛异常")
    void evaluateAsync_persistFailure_warnsOnly() throws Exception {
        when(llmJudgeService.score(any(), any(), any())).thenReturn(4);
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            throw new RuntimeException("down");
        }).when(feignClient).recordEval(any());

        assertDoesNotThrow(() -> newService().evaluateAsync("req-6", null, null, "问题", "回答"));
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("存在多个观测线程池时使用 evalExecutor 装配")
    void context_usesEvalExecutorWhenMultipleExecutorsExist() {
        new ApplicationContextRunner()
                .withUserConfiguration(OnlineEvalServiceContextConfig.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    OnlineEvalService service = context.getBean(OnlineEvalService.class);
                    ThreadPoolTaskExecutor evalExecutor = context.getBean("evalExecutor",
                            ThreadPoolTaskExecutor.class);

                    assertSame(evalExecutor, ReflectionTestUtils.getField(service, "evalExecutor"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({com.aics.chat.config.ObservabilityExecutorConfig.class, OnlineEvalService.class})
    static class OnlineEvalServiceContextConfig {

        @Bean
        OnlineEvalProperties onlineEvalProperties() {
            return new OnlineEvalProperties();
        }

        @Bean
        OnlineEvalSampler onlineEvalSampler() {
            return new OnlineEvalSampler();
        }

        @Bean
        LlmJudgeService llmJudgeService() {
            return mock(LlmJudgeService.class);
        }

        @Bean
        OnlineEvalFeignClient onlineEvalFeignClient() {
            return mock(OnlineEvalFeignClient.class);
        }
    }
}
