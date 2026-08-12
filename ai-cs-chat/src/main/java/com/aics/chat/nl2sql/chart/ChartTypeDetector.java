package com.aics.chat.nl2sql.chart;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图表类型自动判定 —— 纯函数，根据数据特征选图表。
 *
 * <h3>学习要点（规则引擎式判定）</h3>
 * <ul>
 *   <li><b>单行/空 → NONE</b>：没有分布维度，硬画图反而误导。</li>
 *   <li><b>时间列 → LINE</b>：时间趋势用折线最直观（键名含 time/date/month/月份等）。</li>
 *   <li><b>分类列 → PIE/BAR</b>：类别少（≤12）用饼图看占比，多则柱状图更清晰。</li>
 *   <li><b>全数值 → BAR</b>：数值型分布默认柱状图。</li>
 *   <li>作为纯函数可独立单测，是"确定性逻辑不走 LLM"原则的示例。</li>
 * </ul>
 */
public final class ChartTypeDetector {

    private static final int PIE_MAX_CATEGORIES = 12;

    private ChartTypeDetector() {
    }

    public static ChartType detect(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty() || rows.size() < 2) {
            return ChartType.NONE;
        }
        Map<String, Object> first = rows.get(0);
        if (first == null || first.isEmpty()) {
            return ChartType.NONE;
        }
        // 1. 时间列
        if (hasTimeColumn(first)) {
            return ChartType.LINE;
        }
        // 2. 分类维度
        String categoryKey = findCategoryColumn(rows);
        if (categoryKey != null) {
            Set<Object> distinct = new HashSet<>();
            for (Map<String, Object> row : rows) {
                distinct.add(row.get(categoryKey));
            }
            return distinct.size() <= PIE_MAX_CATEGORIES ? ChartType.PIE : ChartType.BAR;
        }
        // 3. 全数值 → BAR
        return ChartType.BAR;
    }

    private static boolean hasTimeColumn(Map<String, Object> row) {
        for (String key : row.keySet()) {
            String lower = key.toLowerCase();
            if (lower.contains("time") || lower.contains("date") || lower.contains("month")
                    || lower.contains("日期") || lower.contains("时间") || lower.contains("月份")) {
                return true;
            }
        }
        return false;
    }

    private static String findCategoryColumn(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> first = rows.get(0);
        for (Map.Entry<String, Object> entry : first.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String s && !isNumeric(s)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static boolean isNumeric(String s) {
        try {
            Double.parseDouble(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
