package com.aics.knowledge.ops;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EmbeddingClusterService 单元测试：贪心聚类分组、阈值与代表问题。
 */
class EmbeddingClusterServiceTest {

    private EmbeddingModel embeddingModel;
    private OpsProperties properties;
    private EmbeddingClusterService service;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        properties = new OpsProperties();
        properties.setSimilarityThreshold(0.82);
        service = new EmbeddingClusterService(embeddingModel, properties);
    }

    private QuestionItem item(Long id, String text) {
        QuestionItem q = new QuestionItem();
        q.setId(id);
        q.setText(text);
        return q;
    }

    @Test
    @DisplayName("相同意图聚为一类，不同意图分开")
    void cluster_groupsBySimilarity() {
        when(embeddingModel.embed("怎么退款")).thenReturn(new float[]{1f, 0f});
        when(embeddingModel.embed("如何申请退款")).thenReturn(new float[]{1f, 0f});
        when(embeddingModel.embed("今天天气怎么样")).thenReturn(new float[]{0f, 1f});

        List<ClusterTopic> topics = service.cluster(List.of(
                item(1L, "怎么退款"),
                item(2L, "如何申请退款"),
                item(3L, "今天天气怎么样")));

        assertThat(topics).hasSize(2);
        assertThat(topics.get(0).getCount()).isEqualTo(2);
        assertThat(topics.get(1).getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("空文本与向量化失败条目跳过")
    void cluster_skipsInvalid() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1f});
        List<ClusterTopic> topics = service.cluster(List.of(
                item(1L, "   "),
                item(2L, "有效问题")));
        assertThat(topics).hasSize(1);
        assertThat(topics.get(0).getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("占比计算正确且主题按成员数降序")
    void cluster_ratioAndOrder() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1f, 0f});
        when(embeddingModel.embed("A")).thenReturn(new float[]{1f, 0f});
        when(embeddingModel.embed("B")).thenReturn(new float[]{0f, 1f});
        List<ClusterTopic> topics = service.cluster(List.of(
                item(1L, "A"), item(2L, "A"), item(3L, "B")));
        assertThat(topics).hasSize(2);
        assertThat(topics.get(0).getRatio()).isEqualTo(2.0 / 3.0);
    }
}