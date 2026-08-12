package com.aics.chat.nl2sql.chart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ECharts option 构建器 —— 用纯 Java 拼标准图表配置。
 *
 * <h3>学习要点（技术：ECharts 配置结构）</h3>
 * <ul>
 *   <li><b>为什么不用 LLM 生成 option</b>：ECharts 配置结构是确定的
 *       （series/xAxis/yAxis 等），LLM 可能输出非法 JSON 或错误字段名，
 *       纯 Java 构建零风险、可单测。</li>
 *   <li><b>三种图表</b>：pie（分类占比）、bar（数值对比）、line（时间趋势），
 *       关键是把数据行拆成 name/value 或 categories/values 两个数组。</li>
 *   <li><b>LinkedHashMap</b>：保持字段顺序，让生成的 JSON 稳定可读。</li>
 * </ul>
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
        String nameKey = firstKey(rows);                 // 第一列作为分类名（如"手机"）
        String valueKey = firstNumericKey(rows, nameKey); // 第一个数值列作为值
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", String.valueOf(row.get(nameKey)));
            item.put("value", numericValue(row.get(valueKey)));
            data.add(item);   // 组装 ECharts pie 的 data 数组
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