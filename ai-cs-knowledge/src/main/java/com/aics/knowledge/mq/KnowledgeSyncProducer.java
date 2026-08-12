package com.aics.knowledge.mq;

import com.aics.knowledge.entity.KnowledgeDocument;
import com.aics.knowledge.service.KnowledgeVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * 知识文档增量同步 RocketMQ 生产者
 *
 * <p>将文档 CRUD 事件投递到 knowledge-doc-sync-topic，tag=action（CREATE/UPDATE/DELETE），
 * 由同模块内 {@link KnowledgeSyncConsumer} 消费执行向量化/删除（解耦 DB 事务与向量操作）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSyncProducer {

    /** 知识文档同步主题 */
    private static final String TOPIC = "knowledge-doc-sync-topic";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 投递文档同步消息
     *
     * @param action 操作类型：CREATE / UPDATE / DELETE
     * @param doc    知识文档
     */
    public void send(String action, KnowledgeDocument doc) {
        try {
            KnowledgeSyncMessage message = new KnowledgeSyncMessage();
            message.setAction(action);
            message.setDocumentId(doc.getId());
            message.setKnowledgeBase(KnowledgeVectorService.KNOWLEDGE_BASE);
            message.setTitle(doc.getTitle());
            message.setContent(doc.getContent());
            message.setTimestamp(System.currentTimeMillis());
            rocketMQTemplate.syncSend(TOPIC + ":" + action, message);
            log.info("投递知识文档同步消息: action={}, documentId={}, title={}", action, doc.getId(), doc.getTitle());
        } catch (Exception e) {
            log.warn("知识文档同步消息投递失败（不影响 DB 操作）: action={}, documentId={}, err={}",
                    action, doc.getId(), e.getMessage());
        }
    }
}