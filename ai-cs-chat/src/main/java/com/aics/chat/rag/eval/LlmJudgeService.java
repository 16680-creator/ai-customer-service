package com.aics.chat.rag.eval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-as-Judge 打分实现 —— 让大模型当"阅卷老师"。
 *
 * <h3>学习要点（技术：LLM-as-Judge / 可观测性）</h3>
 * <ul>
 *   <li><b>为什么用 LLM 打分</b>：回答质量（准确性/完整性/简洁性）难以用规则量化，
 *       大模型能理解语义并给出 1-5 分，是 RAG 评估中"回答质量"维度的常用做法。</li>
 *   <li><b>提示词设计</b>：明确评分维度（准确性/完整性/简洁性）+ 只输出数字，
 *       降低模型自由发挥导致的解析失败。</li>
 *   <li><b>健壮性</b>：任何异常（超时/解析失败）返回 null，调用方降级，
 *       保证评估管线不会因单个打分失败而中断。</li>
 *   <li>复用现有 {@link ChatClient}（DeepSeek），不额外引入模型供应商。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmJudgeService implements RagAnswerJudge {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    private final ChatClient chatClient;

    @Override
    public Integer score(String question, String answer, String referenceAnswer) {
        if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
            return null;
        }
        try {
            String prompt = """
                    你是一名 RAG 回答质量评估员。请根据参考答案对 AI 客服的回答打分（1-5 分，5 为最佳）。
                    评分维度：准确性（是否与参考答案一致、有无编造）、完整性（是否覆盖要点）、简洁性。
                    只输出一个数字分数，不要输出其他内容。

                    问题：%s
                    AI 回答：%s
                    参考答案：%s
                    """.formatted(question, answer, referenceAnswer == null ? "（无）" : referenceAnswer);
            String content = chatClient.prompt().system(
                            "你是严谨的 RAG 质量评估员，只输出 1-5 的整数分数。")
                    .user(prompt)
                    .call()
                    .content();
            if (!StringUtils.hasText(content)) {
                return null;
            }
            Matcher m = NUMBER_PATTERN.matcher(content);
            if (m.find()) {
                int score = Math.round(Float.parseFloat(m.group()));
                return Math.max(1, Math.min(5, score));
            }
            return null;
        } catch (Exception e) {
            log.warn("LLM Judge 打分失败，降级返回 null: err={}", e.getMessage());
            return null;
        }
    }
}