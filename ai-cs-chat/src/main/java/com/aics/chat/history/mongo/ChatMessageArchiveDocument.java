package com.aics.chat.history.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** MongoDB 对话审计文档：保留完整内容与扩展字段，供分页审计/运营检索。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("chat_message_archive")
@CompoundIndex(name = "session_created_idx", def = "{'sessionKey': 1, 'createdAt': -1}")
public class ChatMessageArchiveDocument {

    @Id
    private String id;
    @Indexed
    private String sessionKey;
    @Indexed
    private String userId;
    private String role;
    private String content;
    private Instant createdAt;
}
