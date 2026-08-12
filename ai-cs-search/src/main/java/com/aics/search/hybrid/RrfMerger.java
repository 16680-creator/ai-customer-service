package com.aics.search.hybrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RRF（Reciprocal Rank Fusion，倒数排名融合）工具类
 *
 * <p>将多路检索结果按排名融合：每路结果的第 rank 名贡献 1/(k + rank) 分，
 * 按总分降序取 topK。k 为平滑常数（默认 60），k 越大排名靠后的结果权重差异越小。</p>
 */
public final class RrfMerger {

    private RrfMerger() {
    }

    /**
     * 融合两路检索结果
     *
     * @param list1 第一路结果（如 ES 关键词检索），按相关性降序
     * @param list2 第二路结果（如向量相似度检索），按相关性降序
     * @param topK  返回前 topK 条
     * @param k     RRF 平滑常数（推荐 60）
     * @return 按融合分数降序的 topK 条结果
     */
    public static List<RankedItem> merge(List<RankedItem> list1, List<RankedItem> list2, int topK, int k) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, RankedItem> items = new HashMap<>();
        // list1 排名：对每个 item，scores[id] += 1/(k + rank)
        addScores(scores, items, list1, k, true);
        addScores(scores, items, list2, k, false);
        // 回填融合分数
        items.values().forEach(item -> item.setScore(scores.getOrDefault(item.getId(), 0.0)));
        return items.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 累加一路结果的 RRF 分数，并按 id 合并条目信息（标题/内容等，先到者优先）。
     *
     * @param scores  分数累加表
     * @param items   条目合并表
     * @param list    当前路结果
     * @param k       RRF 平滑常数
     * @param isFirst 是否为第一路（用于标记 rank1/rank2）
     */
    private static void addScores(Map<String, Double> scores, Map<String, RankedItem> items,
                                  List<RankedItem> list, int k, boolean isFirst) {
        for (int i = 0; i < list.size(); i++) {
            RankedItem item = list.get(i);
            if (item.getId() == null || item.getId().isBlank()) {
                continue;
            }
            int rank = i + 1;
            scores.merge(item.getId(), 1.0 / (k + rank), Double::sum);
            RankedItem merged = items.computeIfAbsent(item.getId(), id -> new RankedItem());
            if (isFirst) {
                merged.setRank1(rank);
            } else {
                merged.setRank2(rank);
            }
            fillMissing(merged, item);
        }
    }

    /** 用当前条目补全合并条目中缺失的字段（先到者优先，防止覆盖） */
    private static void fillMissing(RankedItem target, RankedItem source) {
        if (target.getId() == null) {
            target.setId(source.getId());
        }
        if (target.getTitle() == null) {
            target.setTitle(source.getTitle());
        }
        if (target.getContent() == null) {
            target.setContent(source.getContent());
        }
        if (target.getKnowledgeBase() == null) {
            target.setKnowledgeBase(source.getKnowledgeBase());
        }
        if (target.getPage() == null) {
            target.setPage(source.getPage());
        }
        if (target.getDocType() == null) {
            target.setDocType(source.getDocType());
        }
    }
}
