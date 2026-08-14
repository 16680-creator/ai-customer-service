package com.aics.chat.nl2sql.chart;

import com.aics.chat.modelrouter.ModelScenario;
import com.aics.chat.modelrouter.RoutedChatClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 问数回答生成器 —— 结论（LLM）+ 图表类型判定 + ECharts 配置。
 *
 * <h3>【AI 技术详解】NL2SQL 问数回答生成</h3>
 * <ul>
 *   <li><b>问题</b>：NL2SQL 查询返回的是原始数据行，用户需要理解数据含义</li>
 *   <li><b>方案</b>：LLM 生成自然语言结论 + 自动选择图表类型 + 生成 ECharts 配置</li>
 *   <li><b>价值</b>：运营人员无需看原始数据，直接看结论和图表</li>
 * </ul>
 *
 * <h3>【AI 技术详解】分工设计（LLM vs 确定性逻辑）</h3>
 * <ul>
 *   <li><b>LLM 负责</b>：生成自然语言结论（开放性文本，适合 LLM）</li>
 *   <li><b>确定性逻辑负责</b>：图表类型判定 + ECharts option 构建（避免 LLM 幻觉出非法 JSON）</li>
 *   <li><b>为什么这么分</b>：
 *       <ul>
 *         <li>LLM 擅长自然语言生成，但可能输出非法 JSON</li>
 *         <li>确定性逻辑擅长结构化数据处理，输出稳定可靠</li>
 *         <li>各司其职，质量与稳定性兼得</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】图表类型自动判定</h3>
 * <ul>
 *   <li><b>ChartTypeDetector</b>：根据数据特征自动选择图表类型
 *       <ul>
 *         <li>单列分类数据 → 饼图（PIE）</li>
 *         <li>双列（分类+数值）→ 柱状图（BAR）</li>
 *         <li>时间序列 → 折线图（LINE）</li>
 *         <li>空数据/单行 → 无图表（NONE）</li>
 *       </ul>
 *   </li>
 *   <li><b>EChartsOptionBuilder</b>：根据图表类型生成 ECharts 配置 JSON</li>
 * </ul>
 *
 * <h3>【技术关联】与 Nl2SqlQueryService 的关系</h3>
 * <pre>
 *   NL2SQL 问数完整流程：
 *       用户问"这个月订单量趋势？"
 *           ↓
 *       Nl2SqlQueryService.executeReadOnlyQuery()  // 执行 SQL
 *           ↓
 *       ChartAnswerGenerator.generate()             // 生成回答 ← 本类
 *           ├── ChartTypeDetector.detect()          // 判定图表类型
 *           ├── generateConclusion()                // LLM 生成结论
 *           └── EChartsOptionBuilder.build()        // 生成 ECharts 配置
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChartAnswerGenerator {

    private final RoutedChatClientFactory routedChatClientFactory;

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
            String content = routedChatClientFactory.chatClientFor(ModelScenario.CHART)
                    .prompt()
                    .system("你是数据分析师，只输出结论。")
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