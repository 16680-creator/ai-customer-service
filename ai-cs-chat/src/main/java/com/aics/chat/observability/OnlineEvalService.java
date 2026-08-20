package com.aics.chat.observability;

import com.aics.chat.dto.OnlineEvalRecordDTO;
import com.aics.chat.feign.OnlineEvalFeignClient;
import com.aics.chat.rag.eval.LlmJudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 线上采样评估服务：对采样请求的真实回答执行 LLM-as-Judge 评分并落库。
 *
 * <p>设计（见 design.md D6）：
 * <ul>
 *   <li><b>异步评分</b>：采样命中后提交独立线程池执行 Judge，不增加用户请求延迟；</li>
 *   <li><b>失败标记</b>：评分异常时落库 {@code judgeStatus=FAILED} 并记录摘要，不重试；</li>
 *   <li><b>复用</b>：直接复用离线评估的 {@link LlmJudgeService}（同一打分提示词与解析逻辑）。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class OnlineEvalService {

    private final OnlineEvalProperties properties;
    private final OnlineEvalSampler sampler;
    private final LlmJudgeService llmJudgeService;
    private final OnlineEvalFeignClient onlineEvalFeignClient;
    private final ThreadPoolTaskExecutor evalExecutor;

    public OnlineEvalService(OnlineEvalProperties properties,
                             OnlineEvalSampler sampler,
                             LlmJudgeService llmJudgeService,
                             OnlineEvalFeignClient onlineEvalFeignClient,
                             @Qualifier("evalExecutor") ThreadPoolTaskExecutor evalExecutor) {
        this.properties = properties;
        this.sampler = sampler;
        this.llmJudgeService = llmJudgeService;
        this.onlineEvalFeignClient = onlineEvalFeignClient;
        this.evalExecutor = evalExecutor;
    }

    /**
     * 对一次线上回答执行采样评估（未启用/未命中采样时静默跳过）。
     *
     * @param requestId 请求 ID（关联 llm_trace）
     * @param sessionId 会话 ID（可空）
     * @param userId    用户 ID（可空）
     * @param question  用户问题
     * @param answer    模型回答
     */
    public void evaluateAsync(String requestId, Long sessionId, Long userId, String question, String answer) {
        // 三层门禁：总开关 → 采样率 → 数据完整性，任一不过直接跳过
        // 学习点：线上评估是"锦上添花"的质量观测，绝不能反过来增加用户请求延迟或成本失控，
        // 所以默认关闭（enabled=false）、默认 1% 采样（sampleRate=0.01），且全部异步执行
        if (!properties.isEnabled() || !sampler.shouldSample(properties.getSampleRate())) {
            return;
        }
        if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
            return;
        }
        // 异步评分：把 LLM-as-Judge 调用丢到独立线程池（evalExecutor），
        // 用户请求已返回，评分在后台完成——这就是"线上采样评估"与离线评估的本质区别：
        // 离线评估跑 golden 集，线上评估偷偷抽真实流量评分，不打扰用户
        evalExecutor.execute(() -> {
            OnlineEvalRecordDTO dto = new OnlineEvalRecordDTO();
            dto.setRequestId(requestId);
            dto.setSessionId(sessionId);
            dto.setUserId(userId);
            // 摘要截断：问题/回答原文可能很长，落库只存截断摘要，控制表体积同时避免敏感信息全量留存
            dto.setQuestionDigest(truncate(question, 1000));
            dto.setAnswerDigest(truncate(answer, 2000));
            try {
                Integer score = llmJudgeService.score(question, answer, null);
                if (score == null) {
                    dto.setJudgeStatus("FAILED");
                    dto.setErrorSummary("Judge 返回空评分");
                } else {
                    dto.setJudgeStatus("SUCCESS");
                    dto.setLlmScore(score);
                }
            } catch (Exception e) {
                // 评分失败不重试：标记 FAILED 落库
                // 学习点：评估链路失败不重试的原因——Judge 是 LLM 调用，重试会重复花钱；
                // 且采样评估是统计用途，丢一条样本不影响整体质量结论，标记 FAILED 供事后分析即可
                dto.setJudgeStatus("FAILED");
                dto.setErrorSummary(truncate(e.getMessage(), 500));
            }
            try {
                onlineEvalFeignClient.recordEval(dto);
            } catch (Exception e) {
                // 落库失败仅告警：不影响主链路
                log.warn("线上评估记录落库失败: requestId={}, err={}", requestId, e.getMessage());
            }
        });
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
