package com.aics.chat.service.impl;

import com.aics.chat.observability.OnlineEvalService;
import com.aics.chat.prompt.PromptRegistry;
import com.aics.chat.rag.retrieve.HybridRetriever;
import com.aics.chat.security.ContentReviewResult;
import com.aics.chat.security.RagAclFilter;
import com.aics.chat.security.SecurityAuditRecorder;
import com.aics.chat.service.ChatHistoryService;
import com.aics.chat.service.KnowledgeBaseService;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatService.streamReply 测试：token 回调式流式、输入拦截短路、降级错误标记转异常
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplStreamReplyTest {

    @Mock
    private ResilientAiService resilientAiService;
    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    @Mock
    private ChatHistoryService chatHistoryService;
    @Mock
    private HybridRetriever hybridRetriever;
    @Mock
    private OnlineEvalService onlineEvalService;
    @Mock
    private com.aics.chat.security.ContentSafetyService contentSafetyService;
    @Mock
    private RagAclFilter ragAclFilter;
    @Mock
    private SecurityAuditRecorder securityAuditRecorder;
    @Mock
    private PromptRegistry promptRegistry;
    @Mock
    private com.aics.chat.cache.HotQaCacheService hotQaCacheService;
    @Mock
    private com.aics.chat.cache.SemanticCacheService semanticCacheService;

    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(resilientAiService, knowledgeBaseService, chatHistoryService,
                hybridRetriever, ObservationRegistry.create(), onlineEvalService,
                contentSafetyService, ragAclFilter, securityAuditRecorder, promptRegistry,
                hotQaCacheService, semanticCacheService);
        // 默认放行输入/输出审核
        when(contentSafetyService.reviewInput(anyString())).thenReturn(ContentReviewResult.pass());
        when(contentSafetyService.reviewOutput(anyString())).thenReturn(ContentReviewResult.pass());
        when(chatHistoryService.load(anyString())).thenReturn(List.of());
    }

    @Test
    void streamReply逐token回调并返回全文且落库历史() {
        when(resilientAiService.callSseStream(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Flux.just("你", "好", "！")));

        List<String> tokens = new ArrayList<>();
        String full = service.streamReply("10", "你好", tokens::add);

        assertEquals("你好！", full);
        assertEquals(List.of("你", "好", "！"), tokens);
        // 回复需持久化到会话历史，保证多轮上下文连续
        verify(chatHistoryService).append("10", "assistant", "你好！");
    }

    @Test
    void streamReply输入被审核拦截时短路不调模型() {
        when(contentSafetyService.reviewInput(anyString()))
                .thenReturn(ContentReviewResult.block("POLITICS", "违规内容"));

        StringBuilder received = new StringBuilder();
        String full = service.streamReply("10", "违规输入", received::append);

        assertEquals("抱歉，我无法回答这个问题。", full);
        assertTrue(received.isEmpty());
        verify(resilientAiService, never()).callSseStream(any(), any());
    }

    @Test
    void streamReply降级错误标记转为异常() {
        when(resilientAiService.callSseStream(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Flux.just("[ERROR]服务繁忙")));

        assertThrows(IllegalStateException.class,
                () -> service.streamReply("10", "你好", s -> {}));
    }
}
