package com.aics.chat.nl2sql.chart;

import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 问数图表控制器 —— 智能问数（NL2SQL）结果的"可视化出口"。
 *
 * <h3>学习要点（技术：NL2SQL + 图表生成）</h3>
 * <ul>
 *   <li><b>场景</b>：NL2SQL 查到的是 JSON 数据行，用户看不懂；
 *       本接口把它们转成「自然语言结论 + ECharts 配置」，前端直接渲染成图表。</li>
 *   <li><b>输入</b>：问题 + 查询结果行（List&lt;Map&gt;）；输出 ChartAnswer
 *       （conclusion/chartType/echartsOption）。</li>
 *   <li><b>与前端解耦</b>：前端只消费标准 ECharts option，不感知后端如何生成，
 *       换图表库只需改前端。</li>
 * </ul>
 */
@Tag(name = "问数图表")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChartController {

    private final ChartAnswerGenerator chartAnswerGenerator;

    @Operation(summary = "生成问数图表（结论 + ECharts 配置）")
    @PostMapping("/chart")
    public Result<ChartAnswer> chart(@RequestBody ChartRequest request) {
        return Result.success(chartAnswerGenerator.generate(request.getQuestion(), request.getRows()));
    }

    @Data
    public static class ChartRequest {
        private String question;
        private List<Map<String, Object>> rows;
    }
}