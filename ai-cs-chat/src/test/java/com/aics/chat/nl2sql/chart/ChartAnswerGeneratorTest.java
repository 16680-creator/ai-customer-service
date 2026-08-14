package com.aics.chat.nl2sql.chart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ChartAnswerGenerator 单元测试：结论生成、图表判定、LLM 失败降级。
 */
class ChartAnswerGeneratorTest {

    private ChatClient chatClient;
    private ChartAnswerGenerator generator;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        generator = new ChartAnswerGenerator(chatClient);
    }

    private Map<String, Object> row(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    @Test
    @DisplayName("多行分类数据: 生成结论 + PIE + ECharts option")
    void generate_pie() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("手机销量最高（1200）。");
        ChartAnswer answer = generator.generate("各分类销量分布",
                List.of(row("category", "手机", "sales", 1200), row("category", "平板", "sales", 800)));
        assertThat(answer.getChartType()).isEqualTo(ChartType.PIE);
        assertThat(answer.getConclusion()).contains("手机销量最高");
        assertThat(answer.getEchartsOption()).containsKey("series");
    }

    @Test
    @DisplayName("单行数据: NONE 且无 echartsOption")
    void generate_singleRow() {
        ChartAnswer answer = generator.generate("总销量", List.of(row("total", 100, "x", 0)));
        assertThat(answer.getChartType()).isEqualTo(ChartType.NONE);
        assertThat(answer.getEchartsOption()).isEmpty();
    }

    @Test
    @DisplayName("空数据: 返回无数据结论")
    void generate_empty() {
        ChartAnswer answer = generator.generate("q", List.of());
        assertThat(answer.getChartType()).isEqualTo(ChartType.NONE);
        assertThat(answer.getConclusion()).contains("无数据");
    }

    @Test
    @DisplayName("LLM 异常: 降级模板结论")
    void generate_llmFail() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("timeout"));
        ChartAnswer answer = generator.generate("各分类销量分布",
                List.of(row("category", "手机", "sales", 1200), row("category", "平板", "sales", 800)));
        assertThat(answer.getChartType()).isEqualTo(ChartType.PIE);
        assertThat(answer.isDegraded()).isFalse();
        assertThat(answer.getConclusion()).contains("行数据");
    }
}