package com.aics.chat.nl2sql.chart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChartTypeDetector 单元测试：图表类型自动判定。
 */
class ChartTypeDetectorTest {

    private Map<String, Object> row(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    @Test
    @DisplayName("多行分类维度: 类别少时判定 PIE")
    void detect_pie() {
        List<Map<String, Object>> rows = List.of(
                row("category", "手机", "sales", 1200),
                row("category", "平板", "sales", 800));
        assertThat(ChartTypeDetector.detect(rows)).isEqualTo(ChartType.PIE);
    }

    @Test
    @DisplayName("含时间列: 判定 LINE")
    void detect_line() {
        List<Map<String, Object>> rows = List.of(
                row("month", "2026-01", "sales", 100),
                row("month", "2026-02", "sales", 200));
        assertThat(ChartTypeDetector.detect(rows)).isEqualTo(ChartType.LINE);
    }

    @Test
    @DisplayName("纯数值多行: 判定 BAR")
    void detect_bar() {
        List<Map<String, Object>> rows = List.of(
                row("score", 90), row("score", 70));
        assertThat(ChartTypeDetector.detect(rows)).isEqualTo(ChartType.BAR);
    }

    @Test
    @DisplayName("单行或空: 判定 NONE")
    void detect_none() {
        assertThat(ChartTypeDetector.detect(List.of(row("total", 100)))).isEqualTo(ChartType.NONE);
        assertThat(ChartTypeDetector.detect(List.of())).isEqualTo(ChartType.NONE);
        assertThat(ChartTypeDetector.detect(null)).isEqualTo(ChartType.NONE);
    }
}
