package com.aics.chat.rag.graph;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图谱检索编排服务 —— GraphRAG 与对话检索的衔接层。
 *
 * <h3>【AI 技术详解】GraphRAG（图谱检索增强生成）</h3>
 * <ul>
 *   <li><b>什么是 GraphRAG</b>：结合知识图谱的 RAG，适合实体关联查询</li>
 *   <li><b>适用场景</b>：
 *       <ul>
 *         <li>"张三买了什么商品？"（用户→订单→商品）</li>
 *         <li>"这个商品有哪些评价？"（商品→评价）</li>
 *         <li>"退款流程是什么？"（问题→流程步骤）</li>
 *       </ul>
 *   </li>
 *   <li><b>与向量检索的区别</b>：
 *       <ul>
 *         <li><b>向量检索</b>：语义相似（"退款"≈"退货"），但无法处理关系遍历</li>
 *         <li><b>图谱检索</b>：关系遍历（用户→订单→商品），但需要预先构建图谱</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】知识图谱（Knowledge Graph）</h3>
 * <ul>
 *   <li><b>三元组（Triple）</b>：知识图谱的基本单位，格式为 (subject, predicate, object)
 *       <ul>
 *         <li>示例：(张三, 购买了, iPhone 15)</li>
 *         <li>示例：(iPhone 15, 属于, 电子产品)</li>
 *       </ul>
 *   </li>
 *   <li><b>多跳展开</b>：从一个实体出发，沿着关系边遍历多层
 *       <ul>
 *         <li>1 跳：张三 → 购买了 → iPhone 15</li>
 *         <li>2 跳：iPhone 15 → 属于 → 电子产品</li>
 *         <li>3 跳：电子产品 → 有品牌 → Apple</li>
 *       </ul>
 *   </li>
 *   <li><b>BFS 遍历</b>：广度优先搜索，逐层扩展，避免深度优先的路径爆炸</li>
 * </ul>
 *
 * <h3>【AI 技术详解】实体抽取（Entity Extraction）</h3>
 * <ul>
 *   <li><b>MVP 方案</b>：与图谱中已有 subject/object 做子串匹配（简单、零成本）</li>
 *   <li><b>增强方案</b>：用 LLM 做命名实体识别（NER），更准确但有调用成本</li>
 *   <li><b>示例</b>：问题"张三买了什么" → 抽取实体"张三" → 在图谱中查找"张三"相关的三元组</li>
 * </ul>
 *
 * <h3>【技术关联】与 HybridRetriever 的关系</h3>
 * <pre>
 *   HybridRetriever.graph()
 *       ├── GraphRagService.retrieveWithGraph()  ← 本类（图谱检索）
 *       ├── VectorStore.similaritySearch()        // 向量检索
 *       └── 合并结果（图谱上下文置于最前）
 * </pre>
 */
@Slf4j
@Service
public class GraphRagService {

    private final GraphStore graphStore;
    private final GraphProperties properties;

    public GraphRagService(GraphStore graphStore, GraphProperties properties) {
        this.graphStore = graphStore;
        this.properties = properties;
    }

    /**
     * 基于问题做图谱多跳检索。
     *
     * @param question      用户问题
     * @param knowledgeBase 知识库标识
     * @return 命中的三元组（去重）；未启用/未命中返回空列表
     */
    public List<GraphTriple> retrieveWithGraph(String question, String knowledgeBase) {
        if (!properties.isEnabled()) {
            log.info("图谱检索未启用，跳过: question={}", question);
            return List.of();
        }
        if (!StringUtils.hasText(question) || knowledgeBase == null) {
            return List.of();
        }
        Set<String> entities = extractEntities(question, knowledgeBase);
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<Long> visited = new LinkedHashSet<>();
        List<GraphTriple> hits = new ArrayList<>();
        for (String entity : entities) {
            List<GraphTriple> triples = graphStore.queryMultiHop(entity, knowledgeBase, properties.getMaxDepth());
            for (GraphTriple t : triples) {
                if (visited.add(t.getId())) {
                    hits.add(t);
                }
            }
        }
        log.info("图谱检索完成: question={}, entities={}, hits={}", question, entities.size(), hits.size());
        return hits;
    }

    /**
     * 实体抽取（MVP）：与图谱中已有 subject/object 做子串匹配。
     */
    private Set<String> extractEntities(String question, String knowledgeBase) {
        Set<String> entities = new LinkedHashSet<>();
        List<GraphTriple> triples = graphStore.listByKnowledgeBase(knowledgeBase);
        for (GraphTriple t : triples) {
            if (question.contains(t.getSubject())) {
                entities.add(t.getSubject());
            }
            if (question.contains(t.getObject())) {
                entities.add(t.getObject());
            }
        }
        return entities;
    }
}