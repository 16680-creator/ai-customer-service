package com.aics.chat.rag.rewrite;

import com.aics.chat.modelrouter.ModelScenario;
import com.aics.chat.modelrouter.RoutedChatClientFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 查询改写 + HyDE 服务 —— 让 LLM 当"检索查询优化师"。
 *
 * <h3>【AI 技术详解】查询改写（Query Rewrite）</h3>
 * <ul>
 *   <li><b>问题</b>：用户口语化问题（"那个功能怎么用"）直接检索往往召回差，
 *       因为知识库文档通常用正式表述（"退款功能使用指南"）</li>
 *   <li><b>方案</b>：让 LLM 把模糊问题拆成多个精确子查询，扩大召回面</li>
 *   <li><b>示例</b>：
 *       <ul>
 *         <li>原始问题："那个功能怎么用"</li>
 *         <li>改写结果：["退款功能使用方法", "如何申请退款", "退款流程步骤"]</li>
 *         <li>每个子查询独立检索，最后用 RRF 融合去重</li>
 *       </ul>
 *   </li>
 *   <li><b>价值</b>：显著提升模糊/口语化问题的召回率</li>
 * </ul>
 *
 * <h3>【AI 技术详解】HyDE（Hypothetical Document Embeddings）</h3>
 * <ul>
 *   <li><b>全称</b>：Hypothetical Document Embeddings（假设性文档嵌入）</li>
 *   <li><b>原理</b>：让 LLM 先生成"假设性标准答案文档"，用它的向量去检索</li>
 *   <li><b>为什么有效</b>：
 *       <ul>
 *         <li>问题通常是短句（"如何退款"），向量信息量有限</li>
 *         <li>假设文档是完整段落（"退款流程如下：1. 登录..."），包含更多关键词</li>
 *         <li>假设文档的向量与真实知识文档更相似，命中率更高</li>
 *       </ul>
 *   </li>
 *   <li><b>流程</b>：
 *       <ol>
 *         <li>用户问"如何退款"</li>
 *         <li>LLM 生成假设文档："退款流程如下：1. 登录账号 2. 进入订单详情 3. 点击申请退款..."</li>
 *         <li>用假设文档的向量去检索，命中率更高</li>
 *       </ol>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】结构化输出（Structured Output）</h3>
 * <ul>
 *   <li><b>问题</b>：LLM 输出是自由文本，直接解析容易失败（格式不一致、多余文字等）</li>
 *   <li><b>方案</b>：要求 LLM 只输出 JSON（subQueries + hydeDocument），再用 Jackson 解析</li>
 *   <li><b>提示词技巧</b>：明确格式要求 + 示例 + "不要输出其他内容"</li>
 *   <li><b>降级</b>：JSON 解析失败时返回空列表，调用方用原始问题检索</li>
 * </ul>
 *
 * <h3>【技术关联】与 HybridRetriever 的协作</h3>
 * <pre>
 *   HybridRetriever.rewriteHybrid()
 *       ├── QueryRewriteService.rewrite(query)     // 获取子查询 + HyDE
 *       ├── 对每个子查询做向量检索
 *       ├── 对 HyDE 文档做向量检索
 *       └── MultiQueryMerger.merge()               // RRF 融合去重
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriteService {

    private static final int MAX_SUB_QUERY_LENGTH = 200;
    private static final int MAX_HYDE_LENGTH = 800;

    private final RoutedChatClientFactory routedChatClientFactory;
    private final ObjectMapper objectMapper;

    /**
     * 【AI 核心】改写查询：LLM 生成子查询 + HyDE 文档。
     *
     * <p><b>【AI 技术详解】LLM 作为"检索查询优化师"</b>：
     * <ul>
     *   <li><b>角色</b>：LLM 不是直接回答问题，而是优化检索查询</li>
     *   <li><b>输入</b>：用户原始问题（可能模糊、口语化）</li>
     *   <li><b>输出</b>：多个精确子查询 + 假设性标准答案文档</li>
     *   <li><b>价值</b>：显著提升模糊/口语化问题的召回率</li>
     * </ul>
     *
     * <p><b>【技术关联】与 EmbeddingModel 的协作</b>：
     * <ul>
     *   <li>本方法只生成文本（子查询 + HyDE），不涉及向量化</li>
     *   <li>向量化由调用方 HybridRetriever 完成（对每个子查询调用 VectorStore）</li>
     *   <li>分离关注点：查询优化 vs 向量检索</li>
     * </ul>
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
            // 设计要点：改写固定走 REWRITE 场景路由，原有“失败降级为原始问题检索”保留——路由失败不应拖垮检索主流程
            String content = routedChatClientFactory.chatClientFor(ModelScenario.REWRITE)
                    .prompt()
                    .system("你是检索查询优化专家，只输出指定 JSON。")
                    .user(prompt)
                    .call()
                    .content();
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