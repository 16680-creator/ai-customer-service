package com.aics.chat.controller;

import com.aics.chat.nl2sql.chart.ChartAnswer;
import com.aics.chat.nl2sql.chart.ChartAnswerGenerator;
import com.aics.chat.nl2sql.chart.ChartController;
import com.aics.chat.nl2sql.chart.ChartType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChartController 契约测试：POST /chat/chart。
 */
class ChartControllerTest {

    private MockMvc mockMvc;
    private ChartAnswerGenerator generator;

    @BeforeEach
    void setUp() {
        generator = mock(ChartAnswerGenerator.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ChartController(generator)).build();
    }

    @Test
    @DisplayName("POST /chat/chart 返回 ChartAnswer 结构")
    void chart() throws Exception {
        ChartAnswer answer = new ChartAnswer();
        answer.setQuestion("各分类销量分布");
        answer.setConclusion("手机销量最高。");
        answer.setChartType(ChartType.PIE);
        answer.setEchartsOption(Map.of("series", List.of()));
        answer.setRows(List.of());
        when(generator.generate(anyString(), any())).thenReturn(answer);

        mockMvc.perform(post("/chat/chart")
                        .contentType("application/json")
                        .content("{\"question\":\"各分类销量分布\",\"rows\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conclusion").value("手机销量最高。"))
                .andExpect(jsonPath("$.data.chartType").value("PIE"));
    }
}