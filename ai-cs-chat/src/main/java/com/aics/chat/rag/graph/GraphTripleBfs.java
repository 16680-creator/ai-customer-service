package com.aics.chat.rag.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图谱多跳展开工具（BFS，纯函数）。
 *
 * <p>在给定三元组集合上，从实体出发按 subject ↔ object 双向展开指定深度，
 * 通过 visited 集合防止循环边导致无限递归。供内存版与 Neo4j 版图存储复用。</p>
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