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
 * 问数图表控制器：把查询结果转自然语言结论 + ECharts 配置。
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