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
 * <h3>学习要点（技术：GraphRAG 检索链路）</h3>
 * <ul>
 *   <li><b>流程</b>：图谱未启用 → 返回空；启用后从问题中抽取候选实体
 *       （与图谱已有实体做子串匹配，MVP 用关键词匹配、后续可换 LLM 实体抽取）
 *       → 对每个实体做多跳展开 → 去重返回三元组。</li>
 *   <li><b>降级</b>：未启用/未命中都返回空列表，由调用方（HybridRetriever）回退普通 RAG，
 *       保证图谱不是"必需依赖"——这是可选增强的典型设计。</li>
 *   <li><b>实体匹配为什么用子串</b>：确定性、零额外成本；LLM 实体抽取是后续增强项，
 *       已在任务清单 T039 登记。</li>
 * </ul>
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