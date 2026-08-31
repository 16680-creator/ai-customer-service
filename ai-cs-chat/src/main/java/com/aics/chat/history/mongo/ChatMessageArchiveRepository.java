package com.aics.chat.history.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/** 对话归档查询：按会话倒序分页由 Spring Data Page 接口扩展，当前先提供基础查询。 */
public interface ChatMessageArchiveRepository extends MongoRepository<ChatMessageArchiveDocument, String> {
    List<ChatMessageArchiveDocument> findTop50BySessionKeyOrderByCreatedAtDesc(String sessionKey);
}
