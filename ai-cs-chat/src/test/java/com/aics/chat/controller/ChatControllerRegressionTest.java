package com.aics.chat.controller;

import com.aics.chat.dto.ChatRagResponseDTO;
import com.aics.chat.service.ChatService;
import com.aics.chat.service.VisionChatService;
import com.aics.common.result.Result;
import com.aics.common.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 存量文本对话回归测试：验证视觉能力上线后，普通文本对话端点（/chat/send、/chat/rag）
 * 的委托行为不变（Controller 仅新增 vision 字段依赖，不影响存量端点）。
 */
class ChatControllerRegressionTest {

    private MockMvc mockMvc;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        VisionChatService visionChatService = mock(VisionChatService.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ChatController(chatService, visionChatService, fileStorageService)).build();
    }

    @Test
    @DisplayName("POST /chat/send 委托 chatService.chat 且返回文本")
    void sendDelegatesToChat() throws Exception {
        when(chatService.chat(eq("s1"), eq("你好"))).thenReturn(Result.success("你好呀"));

        mockMvc.perform(post("/chat/send")
                        .param("sessionId", "s1")
                        .param("message", "你好"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("你好呀"));

        verify(chatService).chat("s1", "你好");
    }

    @Test
    @DisplayName("POST /chat/rag 委托 chatService.chatWithRag 且返回回答与引用")
    void ragDelegatesToChatWithRag() throws Exception {
        ChatRagResponseDTO dto = new ChatRagResponseDTO();
        dto.setContent("根据资料回答");
        dto.setCitations(List.of());
        when(chatService.chatWithRag(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Result.success(dto));

        mockMvc.perform(post("/chat/rag")
                        .param("sessionId", "s1")
                        .param("message", "怎么退款")
                        .param("knowledgeBase", "kb")
                        .param("hybrid", "true")
                        .param("rewrite", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("根据资料回答"));
    }
}
