package com.aics.chat.nl2sql.chart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ECharts option 构建器（纯 Java，不依赖 LLM，避免幻觉非法 JSON）。
 */
public final class EChartsOptionBuilder {

    private EChartsOptionBuilder() {
    }

    /**
     * 构建 ECharts option。
     *
     * @param chartType 图表类型
     * @param rows      数据行
     * @return ECharts option（LinkedHashMap 保持顺序）
     */
    public static Map<String, Object> build(ChartType chartType, List<Map<String, Object>> rows) {
        return switch (chartType) {
            case PIE -> pie(rows);
            case LINE -> line(rows);
            case BAR -> bar(rows);
            default -> new LinkedHashMap<>();
        };
    }

    private static Map<String, Object> pie(List<Map<String, Object>> rows) {
        List<Map<String, Object>> data = new ArrayList<>();
        String nameKey = firstKey(rows);
        String valueKey = firstNumericKey(rows, nameKey);
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", String.valueOf(row.get(nameKey)));
            item.put("value", numericValue(row.get(valueKey)));
            data.add(item);
        }
        Map<String, Object> series = new LinkedHashMap<>();
        series.put("type", "pie");
        series.put("radius", "60%");
        series.put("data", data);
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("tooltip", Map.of("trigger", "item"));
        option.put("series", List.of(series));
        return option;
    }

    private static Map<String, Object> bar(List<Map<String, Object>> rows) {
        List<String> categories = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        String nameKey = firstKey(rows);
        String valueKey = firstNumericKey(rows, nameKey);
        for (Map<String, Object> row : rows) {
            categories.add(String.valueOf(row.get(nameKey)));
            values.add(numericValue(row.get(valueKey)));
        }
        Map<String, Object> xAxis = new LinkedHashMap<>();
        xAxis.put("type", "category");
        xAxis.put("data", categories);
        Map<String, Object> yAxis = new LinkedHashMap<>();
        yAxis.put("type", "value");
        Map<String, Object> series = new LinkedHashMap<>();
        series.put("type", "bar");
        series.put("data", values);
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("tooltip", Map.of("trigger", "axis"));
        option.put("xAxis", xAxis);
        option.put("yAxis", yAxis);
        option.put("series", List.of(series));
        return option;
    }

    private static Map<String, Object> line(List<Map<String, Object>> rows) {
        Map<String, Object> option = bar(rows);
        @SuppressWarnings("unchecked")
        Map<String, Object> series = (Map<String, Object>) ((List<?>) option.get("series")).get(0);
        series.put("type", "line");
        series.put("smooth", true);
        return option;
    }

    private static String firstKey(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return rows.get(0).keySet().iterator().next();
    }

    private static String firstNumericKey(List<Map<String, Object>> rows, String exclude) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        for (String key : rows.get(0).keySet()) {
            if (key.equals(exclude)) {
                continue;
            }
            Object v = rows.get(0).get(key);
            if (v instanceof Number) {
                return key;
            }
        }
        // 兜底：取第一个非 exclude 列
        for (String key : rows.get(0).keySet()) {
            if (!key.equals(exclude)) {
                return key;
            }
        }
        return null;
    }

    private static Number numericValue(Object v) {
        if (v instanceof Number n) {
            return n;
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}