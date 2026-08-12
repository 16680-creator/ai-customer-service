package com.aics.chat.rag.retrieve;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MultiQueryMerger 单元测试：多路检索结果 RRF 融合与去重。
 */
class MultiQueryMergerTest {

    private Document doc(String id) {
        return new Document(id, "text-" + id, Map.of("documentId", id));
    }

    @Test
    @DisplayName("RRF 融合: 两路结果按排名融合，公共文档得分更高")
    void merge_combinesRanks() {
        List<Document> list1 = List.of(doc("a"), doc("b"), doc("c"));
        List<Document> list2 = List.of(doc("b"), doc("d"));
        List<Document> merged = MultiQueryMerger.merge(List.of(list1, list2), 4, 60);

        assertThat(merged).hasSize(4);
        // b 在两路都出现，RRF 分数最高
        assertThat(merged.get(0).getId()).isEqualTo("b");
    }

    @Test
    @DisplayName("去重: 同一文档多路出现只保留一次")
    void merge_dedupes() {
        List<Document> list1 = List.of(doc("a"));
        List<Document> list2 = List.of(doc("a"));
        List<Document> merged = MultiQueryMerger.merge(List.of(list1, list2), 10, 60);
        assertThat(merged).hasSize(1);
    }

    @Test
    @DisplayName("空单路: 不抛异常")
    void merge_emptyList() {
        List<Document> merged = MultiQueryMerger.merge(List.of(List.of(), List.of(doc("a"))), 5, 60);
        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).getId()).isEqualTo("a");
    }

    @Test
    @DisplayName("全部为空: 返回空列表")
    void merge_allEmpty() {
        assertThat(MultiQueryMerger.merge(List.of(), 5, 60)).isEmpty();
    }
}
