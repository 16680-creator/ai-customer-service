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
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>投递时机：DB 写入成功后由 {@link com.aics.knowledge.service.impl.KnowledgeServiceImpl} 调用</li>
 *   <li>使用 syncSend 同步发送，确保消息可靠落盘；发送失败仅告警不影响 DB 主流程</li>
 *   <li>消息体见 {@link KnowledgeSyncMessage}，包含文档内容，消费者无需回查 DB</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSyncProducer {

    /** 知识文档同步主题 */
    private static final String TOPIC = "knowledge-doc-sync-topic";

    /** RocketMQ 模板，由 spring-boot-starter 自动装配 */
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 投递文档同步消息
     *
     * <p>使用 syncSend 同步发送到 TOPIC:action（tag 为操作类型）。
     * 发送失败仅记录告警日志，不抛异常，保证 DB 事务不被 MQ 故障回滚——
     * 极端情况下向量库可能与 DB 短暂不一致，可通过全量重建补偿。</p>
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
            // TOPIC:action 形式：action 作为 RocketMQ tag，便于消费者按 tag 过滤
            rocketMQTemplate.syncSend(TOPIC + ":" + action, message);
            log.info("投递知识文档同步消息: action={}, documentId={}, title={}", action, doc.getId(), doc.getTitle());
        } catch (Exception e) {
            // 发送失败不影响 DB 操作，仅告警（向量库一致性靠补偿机制保证）
            log.warn("知识文档同步消息投递失败（不影响 DB 操作）: action={}, documentId={}, err={}",
                    action, doc.getId(), e.getMessage());
        }
    }
}