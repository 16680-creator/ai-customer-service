package com.aics.chat.nl2sql.chart;

/**
 * 图表类型枚举。
 */
public enum ChartType {
    /** 饼图（分类分布，类别较少） */
    PIE,
    /** 柱状图（数值分布/类别较多） */
    BAR,
    /** 折线图（时间趋势） */
    LINE,
    /** 不生成图表（单行/空数据） */
    NONE
}
