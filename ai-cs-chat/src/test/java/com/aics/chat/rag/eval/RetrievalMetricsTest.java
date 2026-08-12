package com.aics.chat.rag.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RetrievalMetrics 单元测试：Recall@k / MRR / HitRate 计算。
 */
class RetrievalMetricsTest {

    @Test
    @DisplayName("recallAtK: 命中 2/3 期望文档")
    void compute_recall() {
        RetrievalMetrics m = RetrievalMetrics.compute(
                List.of("d1", "d2", "d3", "d4", "d5"),
                List.of("d1", "d3", "d9"), 5);
        assertThat(m.getRecallAtK()).isEqualTo(2.0 / 3.0);
        assertThat(m.getHitRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("mrr: 首个命中在排名 2")
    void compute_mrr() {
        RetrievalMetrics m = RetrievalMetrics.compute(
                List.of("x", "d2", "d1"), List.of("d1", "d2"), 5);
        assertThat(m.getMrr()).isEqualTo(1.0 / 2.0);
    }

    @Test
    @DisplayName("hitRate: 未命中任何期望文档为 0")
    void compute_noHit() {
        RetrievalMetrics m = RetrievalMetrics.compute(
                List.of("a", "b"), List.of("z"), 5);
        assertThat(m.getMrr()).isZero();
        assertThat(m.getRecallAtK()).isZero();
        assertThat(m.getHitRate()).isZero();
    }

    @Test
    @DisplayName("期望文档为空: 全部指标为 0 且不抛异常")
    void compute_emptyExpected() {
        RetrievalMetrics m = RetrievalMetrics.compute(List.of("a"), List.of(), 5);
        assertThat(m.getHitRate()).isZero();
        assertThat(m.getMrr()).isZero();
        assertThat(m.getRetrievedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("topK 大于检索结果数: 不越界")
    void compute_topKExceeds() {
        RetrievalMetrics m = RetrievalMetrics.compute(List.of("d1"), List.of("d1"), 100);
        assertThat(m.getRecallAtK()).isEqualTo(1.0);
    }
}
