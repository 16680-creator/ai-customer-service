package com.aics.chat.observability;

import com.aics.chat.dto.ModelUsageDTO;
import com.aics.chat.feign.ModelUsageFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 模型用量记录器：每次 LLM 调用完成后计量 Token 与估算费用，异步经 Feign 落库。
 *
 * <p>设计（见 design.md D3/D4）：
 * <ul>
 *   <li><b>费用估算</b>：按 {@link ModelUsageProperties#getPricing()} 中模型单价计算，
 *       未配置单价走默认单价；估算费用 = 输入/1e6×输入单价 + 输出/1e6×输出单价；</li>
 *   <li><b>异步落库</b>：独立线程池执行 Feign 上报，失败仅告警，不阻塞主链路；</li>
 *   <li><b>场景归属</b>：由调用方传入 scenario（chat/rag/agent/summary/vision/nl2sql/eval），
 *       用量记录可区分统计。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelUsageRecorder {

    private static final BigDecimal PER_MILLION = new BigDecimal("1000000");

    private final ModelUsageProperties properties;
    private final ModelUsageFeignClient modelUsageFeignClient;
    private final ThreadPoolTaskExecutor usageExecutor;

    /**
     * 记录一次 LLM 调用用量（异步落库，失败仅告警）。
     *
     * @param scenario    场景（chat/rag/agent/summary/vision/nl2sql/eval）
     * @param provider    模型供应商
     * @param model       模型名
     * @param inputTokens 输入 Token（可空）
     * @param outputTokens 输出 Token（可空）
     * @param status      状态 SUCCESS/FAILED
     * @param errorSummary 错误摘要（可空）
     */
    // 设计要点：保留旧重载并默认 pricingKey=model——存量调用方无需改动，新路由链路显式传内部模型 ID
    public void record(String scenario, String provider, String model,
                       Integer inputTokens, Integer outputTokens,
                       String status, String errorSummary) {
        record(scenario, provider, model, inputTokens, outputTokens, status, errorSummary, model);
    }

    /**
     * 记录一次 LLM 调用用量（异步落库，失败仅告警）。
     *
     * @param scenario    场景（chat/rag/agent/summary/vision/nl2sql/eval）
     * @param provider    模型供应商
     * @param model       模型名（展示用）
     * @param inputTokens 输入 Token（可空）
     * @param outputTokens 输出 Token（可空）
     * @param status      状态 SUCCESS/FAILED
     * @param errorSummary 错误摘要（可空）
     * @param pricingKey  费用查询键（通常为模型 ID，与展示模型名分离）
     */
    public void record(String scenario, String provider, String model,
                       Integer inputTokens, Integer outputTokens,
                       String status, String errorSummary, String pricingKey) {
        // 计量总开关：关闭时零开销返回（成本治理本身也要可控成本）
        if (!properties.isEnabled()) {
            return;
        }
        // 从当前 TraceContext 关联 requestId/userId：让"这一次用量"能回溯到"哪一次请求"，
        // 这是 trace 与 cost 打通的关键——按 requestId 既能看调用链也能看花了多少钱
        TraceContext trace = TraceContextHolder.current();
        ModelUsageDTO dto = new ModelUsageDTO();
        dto.setRequestId(trace == null ? null : trace.getRequestId());
        dto.setUserId(trace == null ? null : trace.getUserId());
        dto.setSessionId(trace == null ? null : parseSessionId(trace.getSessionId()));
        dto.setScenario(scenario);
        dto.setProvider(provider);
        dto.setModel(model);
        int in = inputTokens == null ? 0 : inputTokens;
        int out = outputTokens == null ? 0 : outputTokens;
        dto.setInputTokens(in);
        dto.setOutputTokens(out);
        dto.setTotalTokens(in + out);
        // 学习点：pricingKey 与展示 model 分离——单价配置按内部模型 id（如 siliconflow-qwen3-32b），供应商展示名（如 Qwen/Qwen3-32B）变化不会导致费用查不到单价
        dto.setEstimatedCost(estimateCost(pricingKey, in, out));
        // estimated 标记：流式调用常取不到精确 usage，此时按估算记且打标，
        // 统计时可按标记过滤，避免"估算当精确"误导成本决策
        dto.setEstimated(inputTokens == null || outputTokens == null);
        dto.setStatus(status == null ? "SUCCESS" : status);
        dto.setErrorSummary(errorSummary);

        // 异步落库：Feign 调用是网络 IO，若同步执行会拖慢用户请求；
        // 独立线程池隔离 + 失败仅告警，保证"计量不影响业务"（见 ObservabilityExecutorConfig）
        usageExecutor.execute(() -> {
            try {
                modelUsageFeignClient.recordUsage(dto);
            } catch (Exception e) {
                // 落库失败仅告警：计量不阻断主链路
                log.warn("模型用量上报失败: requestId={}, model={}, err={}",
                        dto.getRequestId(), model, e.getMessage());
            }
        });
    }

    /**
     * 估算费用：输入/1e6×输入单价 + 输出/1e6×输出单价（元）。
     * 按 pricingKey 查找单价（通常为模型 ID），未配置单价的键走默认单价。
     *
     * <p>学习点：为什么用 BigDecimal 而不是 double？
     * 费用金额涉及精确计算，double 的浮点误差（如 0.1+0.2≠0.3）会污染账目；
     * 单价按"每百万 Token"配置（业界惯例），故先除以 1e6 再乘单价。
     * divide 指定 10 位小数 + HALF_UP 是为了避免除不尽抛 ArithmeticException，
     * 最终结果保留 6 位小数（毫分精度）落库。</p>
     */
    BigDecimal estimateCost(String pricingKey, int inputTokens, int outputTokens) {
        ModelUsageProperties.ModelPrice price = properties.getPricing().getOrDefault(
                pricingKey, properties.getDefaultPricing());
        BigDecimal inCost = BigDecimal.valueOf(inputTokens)
                .divide(PER_MILLION, 10, RoundingMode.HALF_UP)
                .multiply(price.getInput());
        BigDecimal outCost = BigDecimal.valueOf(outputTokens)
                .divide(PER_MILLION, 10, RoundingMode.HALF_UP)
                .multiply(price.getOutput());
        return inCost.add(outCost).setScale(6, RoundingMode.HALF_UP);
    }

    /** 字符串会话 ID 转 Long（非数字返回 null，兼容 sessionKey） */
    private static Long parseSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(sessionId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
