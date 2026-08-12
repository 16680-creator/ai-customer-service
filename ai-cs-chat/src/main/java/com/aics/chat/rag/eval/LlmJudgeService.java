package com.aics.chat.rag.eval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-as-Judge 打分实现（复用现有 ChatClient/DeepSeek）。
 *
 * <p>对"问题 + 回答 + 参考答案"进行 1-5 分打分，解析输出中的首个数字；
 * 任何异常（超时/解析失败/无参考）返回 null，由调用方降级处理。</p>
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