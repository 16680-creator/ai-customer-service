package com.aics.chat.observability;

import com.aics.chat.dto.ModelUsageDTO;
import com.aics.chat.feign.ModelUsageFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ModelUsageRecorder 单元测试：费用计算（含默认单价）、异步落库、失败仅告警、场景归属。
 */
@ExtendWith(MockitoExtension.class)
class ModelUsageRecorderTest {

    @Mock
    private ModelUsageFeignClient feignClient;

    private ModelUsageProperties properties;

    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        properties = new ModelUsageProperties();
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("test-usage-");
        executor.initialize();
    }

    private ModelUsageRecorder newRecorder() {
        return new ModelUsageRecorder(properties, feignClient, executor);
    }

    @Test
    @DisplayName("费用计算：命中配置单价（输入1元/百万，输出2元/百万）")
    void estimateCost_configuredPricing() {
        properties.getPricing().put("deepseek-chat", price("1.0", "2.0"));
        ModelUsageRecorder recorder = newRecorder();

        // 100 万输入 × 1 + 50 万输出 × 2 = 1 + 1 = 2 元
        BigDecimal cost = recorder.estimateCost("deepseek-chat", 1_000_000, 500_000);
        assertEquals(0, cost.compareTo(new BigDecimal("2.000000")));
    }

    @Test
    @DisplayName("费用计算：未配置单价走默认单价（0 元兜底）")
    void estimateCost_defaultPricing() {
        properties.getDefaultPricing().setInput(new BigDecimal("0.5"));
        properties.getDefaultPricing().setOutput(new BigDecimal("0.5"));
        ModelUsageRecorder recorder = newRecorder();

        // 200 万输入 × 0.5 + 200 万输出 × 0.5 = 2 元
        BigDecimal cost = recorder.estimateCost("unknown-model", 2_000_000, 2_000_000);
        assertEquals(0, cost.compareTo(new BigDecimal("2.000000")));
    }

    @Test
    @DisplayName("record 异步落库：场景归属与 token/费用正确")
    void record_asyncPersist() throws Exception {
        properties.getPricing().put("deepseek-chat", price("1.0", "2.0"));
        ModelUsageRecorder recorder = newRecorder();
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(feignClient).recordUsage(any());

        // 有 TraceContext 时自动关联 requestId/userId
        ObservabilityProperties obs = new ObservabilityProperties();
        obs.setSampleRate(1.0);
        TraceContext ctx = TraceContextHolder.begin(obs, 7L, "s1", "chat");
        recorder.record("agent", "deepseek", "deepseek-chat", 1000, 500, "SUCCESS", null);
        TraceContextHolder.clear();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        ArgumentCaptor<ModelUsageDTO> captor = ArgumentCaptor.forClass(ModelUsageDTO.class);
        verify(feignClient).recordUsage(captor.capture());
        ModelUsageDTO dto = captor.getValue();
        assertEquals("agent", dto.getScenario());
        assertEquals(7L, dto.getUserId());
        assertEquals("deepseek-chat", dto.getModel());
        assertEquals(1000, dto.getInputTokens());
        assertEquals(500, dto.getOutputTokens());
        assertEquals(1500, dto.getTotalTokens());
        assertEquals("SUCCESS", dto.getStatus());
        // 1500 token ≈ 0.0015 元（输入 1 元/百万 + 输出 2 元/百万）
        assertTrue(dto.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("record 使用 pricingKey 计费，展示 model 保持实际模型名")
    void record_usesPricingKeyForCost() throws Exception {
        properties.getPricing().put("siliconflow-qwen3-32b", price("1.0", "2.0"));
        ModelUsageRecorder recorder = newRecorder();
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(feignClient).recordUsage(any());

        recorder.record("chat", "siliconflow", "Qwen/Qwen3-32B", 1_000_000, 500_000,
                "SUCCESS", null, "siliconflow-qwen3-32b");
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        ArgumentCaptor<ModelUsageDTO> captor = ArgumentCaptor.forClass(ModelUsageDTO.class);
        verify(feignClient).recordUsage(captor.capture());
        ModelUsageDTO dto = captor.getValue();
        assertEquals("Qwen/Qwen3-32B", dto.getModel());
        assertEquals(0, dto.getEstimatedCost().compareTo(new BigDecimal("2.000000")));
    }

    @Test
    @DisplayName("record 失败仅告警不抛异常（落库线程内）")
    void record_failureWarnsOnly() throws Exception {
        ModelUsageRecorder recorder = newRecorder();
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            throw new RuntimeException("message down");
        }).when(feignClient).recordUsage(any());

        recorder.record("chat", "deepseek", "deepseek-chat", 10, 10, "SUCCESS", null);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        // 不抛异常即通过
    }

    @Test
    @DisplayName("计量关闭时不落库")
    void record_disabled_skips() {
        properties.setEnabled(false);
        ModelUsageRecorder recorder = newRecorder();
        recorder.record("chat", "deepseek", "deepseek-chat", 10, 10, "SUCCESS", null);
        verifyNoInteractions(feignClient);
    }

    @Test
    @DisplayName("场景归属：summary/eval 等独立场景可区分统计")
    void record_scenarioMapping() throws Exception {
        ModelUsageRecorder recorder = newRecorder();
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(feignClient).recordUsage(any());

        recorder.record("eval", "deepseek", "deepseek-chat", 5, 5, "SUCCESS", null);
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        ArgumentCaptor<ModelUsageDTO> captor = ArgumentCaptor.forClass(ModelUsageDTO.class);
        verify(feignClient).recordUsage(captor.capture());
        assertEquals("eval", captor.getValue().getScenario());
    }

    private static ModelUsageProperties.ModelPrice price(String input, String output) {
        ModelUsageProperties.ModelPrice p = new ModelUsageProperties.ModelPrice();
        p.setInput(new BigDecimal(input));
        p.setOutput(new BigDecimal(output));
        return p;
    }
}
