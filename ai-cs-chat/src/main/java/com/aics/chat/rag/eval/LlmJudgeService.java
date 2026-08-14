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
 * <h3>【AI 技术详解】LLM-as-Judge（大模型评判）</h3>
 * <ul>
 *   <li><b>什么是 LLM-as-Judge</b>：用大模型来评估 AI 回答的质量，代替人工评分</li>
 *   <li><b>为什么需要</b>：回答质量（准确性/完整性/简洁性）难以用规则量化，
 *       大模型能理解语义并给出 1-5 分</li>
 *   <li><b>评分维度</b>：
 *       <ul>
 *         <li><b>准确性</b>：回答是否与参考答案一致、有无编造</li>
 *         <li><b>完整性</b>：回答是否覆盖要点</li>
 *         <li><b>简洁性</b>：回答是否简洁明了</li>
 *       </ul>
 *   </li>
 *   <li><b>提示词设计</b>：明确评分维度 + 只输出数字，降低模型自由发挥导致的解析失败</li>
 * </ul>
 *
 * <h3>【AI 技术详解】RAG 评估体系</h3>
 * <ul>
 *   <li><b>检索质量指标</b>：Recall@k、MRR、HitRate（度量"召回全不全 / 排得前不前 / 有没有命中"）</li>
 *   <li><b>回答质量指标</b>：LLM-as-Judge 评分（1-5 分，度量"回答好不好"）</li>
 *   <li><b>综合评估</b>：检索指标 + 回答指标 = RAG 整体质量</li>
 * </ul>
 *
 * <h3>【技术关联】与 RagEvalServiceImpl 的关系</h3>
 * <pre>
 *   RagEvalServiceImpl.evaluate()
 *       ├── 检索：RagEvalDataSource.retrieve()
 *       ├── 指标：RetrievalMetrics.compute()
 *       └── 打分：LlmJudgeService.score()  ← 本类
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmJudgeService implements RagAnswerJudge {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    /** 最近一次打分的总 Token（线程内传递，评估门禁读取） */
    private static final ThreadLocal<Integer> LAST_TOTAL_TOKENS = new ThreadLocal<>();

    private final ChatClient chatClient;

    @Override
    public Integer score(String question, String answer, String referenceAnswer) {
        if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
            return null;
        }
        try {
            // 提示词明确评分维度 + 只输出数字，降低模型自由发挥导致的解析失败
            String prompt = """
                    你是一名 RAG 回答质量评估员。请根据参考答案对 AI 客服的回答打分（1-5 分，5 为最佳）。
                    评分维度：准确性（是否与参考答案一致、有无编造）、完整性（是否覆盖要点）、简洁性。
                    只输出一个数字分数，不要输出其他内容。

                    问题：%s
                    AI 回答：%s
                    参考答案：%s
                    """.formatted(question, answer, referenceAnswer == null ? "（无）" : referenceAnswer);
            org.springframework.ai.chat.model.ChatResponse response = chatClient.prompt().system(
                            "你是严谨的 RAG 质量评估员，只输出 1-5 的整数分数。")
                    .user(prompt)
                    .call()
                    .chatResponse();
            String content = response == null || response.getResult() == null
                    ? null : response.getResult().getOutput().getText();
            // 记录本次打分 Token 用量（供评估门禁计算单请求平均 Token）
            recordTokenUsage(response);
            if (!StringUtils.hasText(content)) {
                return null;
            }
            Matcher m = NUMBER_PATTERN.matcher(content);
            if (m.find()) {
                int score = Math.round(Float.parseFloat(m.group()));   // 解析输出中的首个数字
                return Math.max(1, Math.min(5, score));                // 钳制到 1-5 分区间
            }
            return null;   // 解析失败返回 null（调用方降级）
        } catch (Exception e) {
            log.warn("LLM Judge 打分失败，降级返回 null: err={}", e.getMessage());
            return null;
        }
    }

    @Override
    public Integer lastTotalTokens() {
        // 读取并清除线程内 Token 记录，避免跨用例污染
        Integer tokens = LAST_TOTAL_TOKENS.get();
        LAST_TOTAL_TOKENS.remove();
        return tokens;
    }

    /**
     * 记录最近一次打分的 Token 用量（从 ChatResponse 元数据读取 usage）。
     * <p>非流式 {@code chatClient.call()} 返回的响应元数据携带 usage（prompt/completion tokens），
     * 写入线程内供评估门禁读取；取不到时清空（返回 null）。</p>
     *
     * <p>学习点：为什么用 ThreadLocal 而不是返回值传递？
     * {@link RagAnswerJudge#score} 接口签名只返回分数，改动签名会波及全部实现与 mock；
     * 评估流程是单线程顺序执行的（一条用例打分完再打下一条），ThreadLocal 恰好
     * 承担"方法间隐式传值"的角色——RagEvalServiceImpl 在 score() 之后读取，
     * 读后即清，避免跨用例污染。这是接口稳定性与数据传递的务实折中。</p>
     */
    private void recordTokenUsage(org.springframework.ai.chat.model.ChatResponse response) {
        try {
            if (response != null && response.getMetadata() != null
                    && response.getMetadata().getUsage() != null) {
                int total = response.getMetadata().getUsage().getTotalTokens();
                LAST_TOTAL_TOKENS.set(total);
                return;
            }
        } catch (Exception e) {
            log.debug("Judge Token 用量读取失败: {}", e.getMessage());
        }
        LAST_TOTAL_TOKENS.remove();
    }
}