package com.aics.chat.controller;

import com.aics.chat.rag.retrieve.HybridRetriever;
import com.aics.chat.rag.retrieve.RetrieveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RetrieveController 契约测试：/chat/retrieve/test。
 */
class RetrieveControllerTest {

    private MockMvc mockMvc;
    private HybridRetriever hybridRetriever;

    @BeforeEach
    void setUp() {
        hybridRetriever = mock(HybridRetriever.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RetrieveController(hybridRetriever)).build();
    }

    @Test
    @DisplayName("GET /chat/retrieve/test 返回检索结果结构")
    void retrieveTest() throws Exception {
        Document doc = new Document("d1", "text", Map.of("documentId", "d1", "knowledgeBase", "kb"));
        RetrieveResult result = new RetrieveResult();
        result.setQuery("q");
        result.setDocuments(List.of(doc));
        result.setMode("HYBRID");
        result.setDegraded(false);
        when(hybridRetriever.retrieve(anyString(), anyString(), any(), anyInt())).thenReturn(result);

        mockMvc.perform(get("/chat/retrieve/test")
                        .param("knowledgeBase", "kb")
                        .param("query", "q")
                        .param("mode", "HYBRID")
                        .param("topK", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("HYBRID"))
                .andExpect(jsonPath("$.data.degraded").value(false))
                .andExpect(jsonPath("$.data.documents[0].id").value("d1"));
    }

    @Test
    @DisplayName("非法 mode: 降级为 VECTOR 不报错")
    void retrieveTest_invalidMode() throws Exception {
        RetrieveResult result = new RetrieveResult();
        result.setDocuments(List.of());
        result.setMode("VECTOR");
        when(hybridRetriever.retrieve(anyString(), anyString(), any(), anyInt())).thenReturn(result);
        mockMvc.perform(get("/chat/retrieve/test")
                        .param("knowledgeBase", "kb")
                        .param("query", "q")
                        .param("mode", "UNKNOWN"))
                .andExpect(status().isOk());
    }
}