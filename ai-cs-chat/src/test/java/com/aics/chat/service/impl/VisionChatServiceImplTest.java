package com.aics.chat.service.impl;

import com.aics.chat.dto.ChatRagResponseDTO;
import com.aics.chat.dto.CitationItemDTO;
import com.aics.chat.dto.VisionChatRequest;
import com.aics.chat.dto.VisionChatResponse;
import com.aics.chat.service.ChatService;
import com.aics.chat.util.ImageUrlValidator;
import com.aics.chat.util.PiiMasker;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * VisionChatServiceImpl 两段式编排与降级测试。
 */
class VisionChatServiceImplTest {

    private VisionModelClient visionModelClient;
    private ImageUrlValidator imageUrlValidator;
    private ChatService chatService;
    private VisionChatServiceImpl service;

    @BeforeEach
    void setUp() {
        visionModelClient = mock(VisionModelClient.class);
        imageUrlValidator = mock(ImageUrlValidator.class);
        chatService = mock(ChatService.class);
        service = new VisionChatServiceImpl(visionModelClient, imageUrlValidator, chatService, new PiiMasker());
    }

    private VisionChatRequest request(String imageUrl, String message, String knowledgeBase) {
        VisionChatRequest r = new VisionChatRequest();
        r.setSessionId("s1");
        r.setImageUrl(imageUrl);
        r.setMessage(message);
        r.setKnowledgeBase(knowledgeBase);
        return r;
    }

    @Test
    @DisplayName("图片 URL 校验失败抛 CHAT_IMAGE_URL_INVALID")
    void invalidImageUrlThrows() {
        when(imageUrlValidator.isValid(anyString())).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.chatWithVision(request("http://evil.com/x.png", "hi", "kb")));
        assertEquals("图片地址无效或不允许访问", ex.getMessage());
    }

    @Test
    @DisplayName("视觉理解成功：两段式编排走 RAG 返回引用与描述")
    void visionSuccessGoesRag() {
        when(imageUrlValidator.isValid(anyString())).thenReturn(true);
        when(visionModelClient.describeAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture("截图显示错误码 E10086"));

        ChatRagResponseDTO rag = new ChatRagResponseDTO();
        rag.setContent("根据错误码 E10086，请重试下单。");
        rag.setCitations(List.of(new CitationItemDTO().setTitle("下单失败排查手册")));
        when(chatService.chatWithRag(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Result.success(rag));

        Result<VisionChatResponse> result = service.chatWithVision(
                request("http://minio.internal/x.png", "怎么解决", "kb"));

        assertNotNull(result.getData());
        assertEquals("根据错误码 E10086，请重试下单。", result.getData().getAnswer());
        assertEquals("截图显示错误码 E10086", result.getData().getImageDescription());
        assertFalse(result.getData().isDegraded());
    }

    @Test
    @DisplayName("视觉失败 + 有文字：降级纯文本 degraded=true")
    void visionFailWithTextDegrades() {
        when(imageUrlValidator.isValid(anyString())).thenReturn(true);
        when(visionModelClient.describeAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(chatService.chat(anyString(), anyString())).thenReturn(Result.success("纯文本回答"));

        Result<VisionChatResponse> result = service.chatWithVision(
                request("http://minio.internal/x.png", "怎么解决", "kb"));

        assertEquals("纯文本回答", result.getData().getAnswer());
        assertTrue(result.getData().isDegraded());
    }

    @Test
    @DisplayName("视觉失败 + 仅图片：抛 CHAT_VISION_SERVICE_UNAVAILABLE")
    void visionFailWithoutTextThrows() {
        when(imageUrlValidator.isValid(anyString())).thenReturn(true);
        when(visionModelClient.describeAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThrows(BusinessException.class,
                () -> service.chatWithVision(request("http://minio.internal/x.png", null, "kb")));
    }
}
