package com.aics.chat.controller;

import com.aics.chat.dto.VisionChatResponse;
import com.aics.chat.service.ChatService;
import com.aics.chat.service.VisionChatService;
import com.aics.common.result.Result;
import com.aics.common.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatController 图片对话契约测试。
 */
class ChatControllerVisionTest {

    private MockMvc mockMvc;
    private VisionChatService visionChatService;

    @BeforeEach
    void setUp() {
        ChatService chatService = mock(ChatService.class);
        visionChatService = mock(VisionChatService.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ChatController(chatService, visionChatService, fileStorageService)).build();
    }

    @Test
    @DisplayName("POST /chat/vision 返回图片对话结构")
    void visionReturnsStructure() throws Exception {
        VisionChatResponse resp = new VisionChatResponse();
        resp.setAnswer("回答");
        resp.setImageDescription("描述");
        resp.setDegraded(false);
        when(visionChatService.chatWithVision(any())).thenReturn(Result.success(resp));

        mockMvc.perform(post("/chat/vision")
                        .param("sessionId", "s1")
                        .param("imageUrl", "http://minio.internal/x.png")
                        .param("message", "hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("回答"))
                .andExpect(jsonPath("$.data.imageDescription").value("描述"))
                .andExpect(jsonPath("$.data.degraded").value(false));
    }

    @Test
    @DisplayName("POST /chat/vision 缺少 imageUrl 返回参数错误")
    void visionMissingImageUrl() throws Exception {
        mockMvc.perform(post("/chat/vision")
                        .param("sessionId", "s1"))
                .andExpect(status().is4xxClientError());
    }
}
