package com.aics.chat.rag.rewrite;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 查询改写 + HyDE 服务 —— 让 LLM 当"检索查询优化师"。
 *
 * <h3>学习要点（技术：查询改写 / HyDE / 结构化输出）</h3>
 * <ul>
 *   <li><b>查询改写</b>：用户口语问题（"那个功能怎么用"）直接检索往往召回差；
 *       让 LLM 生成多个精确子查询，可显著扩大召回面。</li>
 *   <li><b>HyDE</b>：Hypothetical Document Embeddings —— 先生成"假设性标准答案"，
 *       再对这段文档做向量化检索。假设文档比问题包含更多实体词，与真实知识文档更相似。</li>
 *   <li><b>结构化输出</b>：要求 LLM 只输出 JSON（subQueries + hydeDocument），
 *       再解析——这是与大模型交互的常见模式，比自由文本更可靠。</li>
 *   <li><b>降级</b>：任何异常（超时/非法 JSON）返回空列表，调用方用原始问题检索，主流程不中断。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriteService {

    private static final int MAX_SUB_QUERY_LENGTH = 200;
    private static final int MAX_HYDE_LENGTH = 800;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * 改写查询：LLM 生成子查询 + HyDE 文档。
     *
     * @param question 原始问题
     * @return 改写结果；失败时 subQueries 为空（调用方降级用原问题）
     */
    public RewriteResult rewrite(String question) {
        RewriteResult result = new RewriteResult();
        result.setOriginalQuery(question);
        result.setSubQueries(new ArrayList<>());   // 初始化为空，调用方无需判空
        if (!StringUtils.hasText(question)) {
            return result;                         // 空问题直接返回，不发 LLM 请求
        }
        try {
            // 提示词要求 LLM 只输出 JSON：子查询数组 + HyDE 假设文档，便于程序化解析
            String prompt = """
                    请把下面的用户问题改写成 %d 个更精确、适合知识库检索的子查询，并生成一条假设性的标准答案文档（HyDE），
                    用于提升检索召回。
                    输出 JSON，格式严格如下（不要输出其他内容）：
                    {"subQueries": ["子查询1", "子查询2", ...], "hydeDocument": "假设性文档内容"}

                    用户问题：%s
                    """.formatted(3, question);
            String content = chatClient.prompt().system(
                            "你是检索查询优化专家，只输出指定 JSON。")
                    .user(prompt)
                    .call()
                    .content();   // 同步调用 DeepSeek（经 ChatClient）
            result.setSubQueries(parseSubQueries(content));   // 解析并去重子查询
            result.setHydeDocument(parseHyde(content));       // 提取 HyDE 假设文档
            log.info("查询改写完成: question={}, subQueries={}, hyde={}",
                    question, result.getSubQueries().size(),
                    StringUtils.hasText(result.getHydeDocument()));
        } catch (Exception e) {
            // 任何异常（超时/非法 JSON）降级：返回空列表，调用方用原始问题检索
            log.warn("查询改写失败，降级为原始问题: question={}, err={}", question, e.getMessage());
            result.setSubQueries(List.of());
            result.setHydeDocument(null);
        }
        return result;
    }

    private List<String> parseSubQueries(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        try {
            var node = objectMapper.readTree(content);
            if (node.has("subQueries")) {
                List<String> list = objectMapper.convertValue(node.get("subQueries"), new TypeReference<List<String>>() {
                });
                Set<String> dedup = new LinkedHashSet<>();
                for (String q : list) {
                    if (StringUtils.hasText(q) && q.length() <= MAX_SUB_QUERY_LENGTH) {
                        dedup.add(q.trim());
                    }
                }
                return new ArrayList<>(dedup);
            }
        } catch (Exception e) {
            log.warn("子查询 JSON 解析失败: err={}", e.getMessage());
        }
        return List.of();
    }

    private String parseHyde(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            var node = objectMapper.readTree(content);
            if (node.has("hydeDocument")) {
                String hyde = node.get("hydeDocument").asText();
                if (StringUtils.hasText(hyde) && hyde.length() <= MAX_HYDE_LENGTH) {
                    return hyde.trim();
                }
            }
        } catch (Exception e) {
            log.warn("HyDE 文档解析失败: err={}", e.getMessage());
        }
        return null;
    }
}