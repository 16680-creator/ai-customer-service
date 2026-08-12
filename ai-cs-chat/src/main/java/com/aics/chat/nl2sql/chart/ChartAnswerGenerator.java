package com.aics.chat.nl2sql.chart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 问数回答生成器：自然语言结论（LLM，失败降级模板）+ 图表类型判定 + ECharts 配置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChartAnswerGenerator {

    private final ChatClient chatClient;

    /**
     * 生成问数回答。
     *
     * @param question 用户问题
     * @param rows     查询结果行
     * @return ChartAnswer（含结论、图表类型与 ECharts 配置）
     */
    public ChartAnswer generate(String question, List<Map<String, Object>> rows) {
        ChartType chartType = ChartTypeDetector.detect(rows);
        String conclusion = generateConclusion(question, rows);
        Map<String, Object> option = EChartsOptionBuilder.build(chartType, rows);

        ChartAnswer answer = new ChartAnswer();
        answer.setQuestion(question);
        answer.setConclusion(conclusion);
        answer.setChartType(chartType);
        answer.setEchartsOption(chartType == ChartType.NONE ? Map.of() : option);
        answer.setRows(rows);
        return answer;
    }

    /**
     * 生成自然语言结论；LLM 失败时降级为模板摘要。
     */
    private String generateConclusion(String question, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "查询无数据。";
        }
        if (rows.size() == 1) {
            Map<String, Object> row = rows.get(0);
            StringBuilder sb = new StringBuilder();
            row.forEach((k, v) -> sb.append(k).append("=").append(v).append("，"));
            return "查询结果为：" + sb.substring(0, sb.length() - 1) + "。";
        }
        try {
            String prompt = """
                    请基于下面的查询结果数据，用 1-2 句简洁的中文总结结论（面向运营人员，突出关键数字与趋势）。
                    只输出结论，不要输出其他内容。

                    用户问题：%s
                    查询结果（JSON 数组）：%s
                    """.formatted(question, rows);
            String content = chatClient.prompt().system("你是数据分析师，只输出结论。")
                    .user(prompt)
                    .call()
                    .content();
            if (StringUtils.hasText(content)) {
                return content.trim();
            }
        } catch (Exception e) {
            log.warn("图表结论 LLM 生成失败，降级模板: err={}", e.getMessage());
        }
        return "共 " + rows.size() + " 行数据，详见图表。";
    }
}