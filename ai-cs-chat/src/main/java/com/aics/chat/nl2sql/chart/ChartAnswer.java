package com.aics.chat.nl2sql.chart;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 问数回答：自然语言结论 + 图表配置。
 */
@Data
public class ChartAnswer {

    /** 原始问题 */
    private String question;

    /** 自然语言结论 */
    private String conclusion;

    /** 图表类型 */
    private ChartType chartType;

    /** ECharts option（chartType=NONE 时为空） */
    private Map<String, Object> echartsOption;

    /** 原始数据行 */
    private List<Map<String, Object>> rows;

    /** 结论是否降级为模板生成 */
    private boolean degraded;
}