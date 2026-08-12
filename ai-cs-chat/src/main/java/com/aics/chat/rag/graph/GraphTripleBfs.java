package com.aics.chat.rag.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图谱多跳展开工具 —— 基于 BFS（广度优先搜索）的纯函数。
 *
 * <h3>学习要点（算法：BFS 多跳遍历）</h3>
 * <ul>
 *   <li><b>为什么是 BFS</b>：从起始实体出发，按"直接关系 → 再跳一层 → …"逐层扩散，
 *       与"多跳"的定义天然吻合；每层用 frontier 队列保存待访问实体。</li>
 *   <li><b>双向边</b>：三元组既是 subject→object，也反向 object→subject，
 *       所以匹配条件为 {@code current.equals(subject) || current.equals(object)}。</li>
 *   <li><b>防循环</b>：visitedEntities 记录已访问实体、visitedTripleIds 记录已用三元组，
 *       防止 A→B→A 这类循环边导致死循环。</li>
 *   <li><b>纯函数</b>：不依赖任何存储，内存版与 Neo4j 版复用同一套展开逻辑，保证语义一致。</li>
 * </ul>
 */
public final class GraphTripleBfs {

    private GraphTripleBfs() {
    }

    /**
     * 多跳展开。
     *
     * @param entity     起始实体
     * @param allTriples 候选三元组（已按知识库过滤）
     * @param depth      展开深度（0 表示仅直接关系）
     * @return 命中的三元组（去重）
     */
    public static List<GraphTriple> expand(String entity, List<GraphTriple> allTriples, int depth) {
        if (entity == null || entity.isBlank() || allTriples == null || allTriples.isEmpty()) {
            return Collections.emptyList();
        }
        List<GraphTriple> result = new ArrayList<>();
        Set<String> visitedEntities = new LinkedHashSet<>();
        Set<Long> visitedTripleIds = new LinkedHashSet<>();
        visitedEntities.add(entity.trim());

        List<String> frontier = new ArrayList<>(visitedEntities);
        int currentDepth = 0;
        while (!frontier.isEmpty() && currentDepth <= depth) {
            List<String> next = new ArrayList<>();
            for (String current : frontier) {
                for (GraphTriple t : allTriples) {
                    boolean matches = current.equals(t.getSubject()) || current.equals(t.getObject());
                    if (!matches || visitedTripleIds.contains(t.getId())) {
                        continue;
                    }
                    visitedTripleIds.add(t.getId());
                    result.add(t);
                    String neighbor = current.equals(t.getSubject()) ? t.getObject() : t.getSubject();
                    if (visitedEntities.add(neighbor)) {
                        next.add(neighbor);
                    }
                }
            }
            frontier = next;
            currentDepth++;
        }
        return result;
    }
}