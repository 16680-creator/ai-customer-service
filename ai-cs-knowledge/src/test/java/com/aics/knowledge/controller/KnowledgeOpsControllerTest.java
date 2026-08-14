package com.aics.knowledge.controller;

import com.aics.knowledge.ops.ClusterReport;
import com.aics.knowledge.ops.FaqService;
import com.aics.knowledge.ops.QuestionClusterService;
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
 * KnowledgeOpsController 契约测试。
 */
class KnowledgeOpsControllerTest {

    private MockMvc mockMvc;
    private QuestionClusterService questionClusterService;
    private FaqService faqService;

    @BeforeEach
    void setUp() {
        questionClusterService = mock(QuestionClusterService.class);
        faqService = mock(FaqService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new KnowledgeOpsController(questionClusterService, faqService)).build();
    }

    @Test
    @DisplayName("POST /knowledge/ops/cluster 返回 ClusterReport")
    void cluster() throws Exception {
        ClusterReport report = new ClusterReport();
        report.setPeriod("p");
        report.setTotalQuestions(3);
        report.setStatus("OK");
        report.setTopics(List.of());
        report.setGapTopics(List.of());
        when(questionClusterService.cluster(anyString(), any(), any())).thenReturn(report);

        mockMvc.perform(post("/knowledge/ops/cluster")
                        .contentType("application/json")
                        .content("{\"period\":\"p\",\"questions\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OK"))
                .andExpect(jsonPath("$.data.totalQuestions").value(3));
    }

    @Test
    @DisplayName("POST /knowledge/ops/faq 收录并触发向量化")
    void adoptFaq() throws Exception {
        when(faqService.adopt(any())).thenReturn(Map.of("faqId", 1L, "vectorized", true));

        mockMvc.perform(post("/knowledge/ops/faq")
                        .contentType("application/json")
                        .content("{\"question\":\"怎么申请退款？\",\"answer\":\"进入订单详情页点击申请退款。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vectorized").value(true));
    }
}