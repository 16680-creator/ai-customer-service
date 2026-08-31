package com.aics.chat.history.mongo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Mongo 归档服务测试：归档最佳努力，不得影响 AI 主回复链路。 */
class MongoChatHistoryArchiveServiceTest {

    @Test
    @DisplayName("正常归档 - 写入文档，携带 session/role/content")
    void shouldSaveArchiveDocument() {
        ChatMessageArchiveRepository repository = mock(ChatMessageArchiveRepository.class);
        MongoChatHistoryArchiveService service = new MongoChatHistoryArchiveService(repository);

        service.archive("session-1", "user", "hello");

        verify(repository).save(argThat(doc -> "session-1".equals(doc.getSessionKey())
                && "user".equals(doc.getRole()) && "hello".equals(doc.getContent())
                && doc.getCreatedAt() != null));
    }

    @Test
    @DisplayName("Mongo 故障 - 只告警不抛，对话主链路不中断")
    void archiveFailureMustNotPropagate() {
        ChatMessageArchiveRepository repository = mock(ChatMessageArchiveRepository.class);
        doThrow(new RuntimeException("mongo unavailable")).when(repository).save(any());
        MongoChatHistoryArchiveService service = new MongoChatHistoryArchiveService(repository);

        assertDoesNotThrow(() -> service.archive("session-1", "assistant", "answer"));
    }
}
