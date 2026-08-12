package com.aics.chat.rag.eval;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检索质量指标（纯计算，无外部依赖）。
 *
 * <p>基于 golden 测试集的期望文档 ID 与检索返回的文档 ID 计算：
 * <ul>
 *   <li>Recall@k：命中的期望文档数 / 期望文档总数</li>
 *   <li>MRR：首个命中期望文档的倒数排名（未命中为 0）</li>
 *   <li>HitRate：至少命中一条期望文档的用例占比</li>
 * </ul>
 * </p>
 */
@Data
@NoArgsConstructor
public class RetrievalMetrics {

    /** Recall@k */
    private double recallAtK;

    /** 平均倒数排名 */
    private double mrr;

    /** 命中率（至少一条期望文档被命中） */
    private double hitRate;

    /** 检索返回条数 */
    private int retrievedCount;

    /** 期望文档数 */
    private int expectedCount;

    /**
     * 计算单条用例的检索指标。
     *
     * @param retrievedIds 检索命中的文档 ID（按相关度降序）
     * @param expectedIds  期望命中的文档 ID
     * @param topK         只看前 topK 条
     * @return 指标对象；期望文档为空时所有比率为 0（调用方决定是否计入汇总）
     */
    public static RetrievalMetrics compute(List<String> retrievedIds, List<String> expectedIds, int topK) {
        RetrievalMetrics m = new RetrievalMetrics();
        if (retrievedIds != null) {
            m.retrievedCount = retrievedIds.size();
        }
        if (expectedIds == null || expectedIds.isEmpty()) {
            return m;
        }
        m.expectedCount = expectedIds.size();

        int k = Math.min(topK, retrievedIds == null ? 0 : retrievedIds.size());
        int hitCount = 0;
        int firstHitRank = -1;
        for (int i = 0; i < k; i++) {
            String id = retrievedIds.get(i);
            if (expectedIds.contains(id)) {
                hitCount++;
                if (firstHitRank < 0) {
                    firstHitRank = i + 1;
                }
            }
        }
        m.recallAtK = (double) hitCount / expectedIds.size();
        m.hitRate = hitCount > 0 ? 1.0 : 0.0;
        m.mrr = firstHitRank > 0 ? 1.0 / firstHitRank : 0.0;
        return m;
    }
}
