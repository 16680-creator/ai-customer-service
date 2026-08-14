package com.aics.knowledge.ops;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * QuestionClusterService 单元测试：数据不足与缺口标记。
 */
class QuestionClusterServiceTest {

    private EmbeddingClusterService embeddingClusterService;
    private KnowledgeGapDetector gapDetector;
    private OpsProperties properties;
    private QuestionClusterService service;

    @BeforeEach
    void setUp() {
        embeddingClusterService = mock(EmbeddingClusterService.class);
        gapDetector = mock(KnowledgeGapDetector.class);
        properties = new OpsProperties();
        properties.setMinQuestions(2);
        service = new QuestionClusterService(embeddingClusterService, gapDetector, properties);
    }

    @Test
    @DisplayName("提问数不足: 返回 INSUFFICIENT_DATA")
    void cluster_insufficientData() {
        ClusterReport report = service.cluster("p", List.of(new QuestionItem()), null);
        assertThat(report.getStatus()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(report.getTopics()).isEmpty();
    }

    @Test
    @DisplayName("命中率低于阈值: 主题被标记缺口")
    void cluster_gapDetected() {
        List<QuestionItem> questions = new ArrayList<>();
        for (long i = 1; i <= 3; i++) {
            QuestionItem q = new QuestionItem();
            q.setId(i);
            q.setText("问题" + i);
            questions.add(q);
        }
        ClusterTopic topic = new ClusterTopic();
        topic.setTopic("问题1");
        topic.setCount(3);
        topic.setRatio(1.0);
        topic.setRepresentativeQuestions(List.of("问题1"));
        when(embeddingClusterService.cluster(any())).thenReturn(List.of(topic));
        when(gapDetector.hitRate(any(), anyString())).thenReturn(0.3);

        ClusterReport report = service.cluster("p", questions, 0.4);

        assertThat(report.getStatus()).isEqualTo("OK");
        assertThat(report.getGapTopics()).hasSize(1);
        assertThat(report.getGapTopics().get(0).isGapFlag()).isTrue();
    }

    @Test
    @DisplayName("命中率达标: 不标记缺口")
    void cluster_noGap() {
        List<QuestionItem> questions = new ArrayList<>();
        for (long i = 1; i <= 3; i++) {
            QuestionItem q = new QuestionItem();
            q.setId(i);
            q.setText("问题" + i);
            questions.add(q);
        }
        ClusterTopic topic = new ClusterTopic();
        topic.setTopic("问题1");
        topic.setCount(3);
        topic.setRepresentativeQuestions(List.of("问题1"));
        when(embeddingClusterService.cluster(any())).thenReturn(List.of(topic));
        when(gapDetector.hitRate(any(), anyString())).thenReturn(0.9);

        ClusterReport report = service.cluster("p", questions, 0.4);
        assertThat(report.getGapTopics()).isEmpty();
    }
}