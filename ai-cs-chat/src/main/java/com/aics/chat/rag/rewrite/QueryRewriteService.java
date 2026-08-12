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
 * 查询改写 + HyDE 服务。
 *
 * <p>把模糊问题交给 LLM：生成多个精确子查询（JSON 数组）+ 一条假设性回答文档（HyDE）。
 * 任何异常（超时/解析失败）返回空子查询，由调用方降级为原始问题检索。</p>
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
     * 改写查询。
     *
     * @param question 原始问题
     * @return 改写结果；失败时 subQueries 为空
     */
    public RewriteResult rewrite(String question) {
        RewriteResult result = new RewriteResult();
        result.setOriginalQuery(question);
        result.setSubQueries(new ArrayList<>());
        if (!StringUtils.hasText(question)) {
            return result;
        }
        try {
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
                    .content();
            result.setSubQueries(parseSubQueries(content));
            result.setHydeDocument(parseHyde(content));
            log.info("查询改写完成: question={}, subQueries={}, hyde={}",
                    question, result.getSubQueries().size(),
                    StringUtils.hasText(result.getHydeDocument()));
        } catch (Exception e) {
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