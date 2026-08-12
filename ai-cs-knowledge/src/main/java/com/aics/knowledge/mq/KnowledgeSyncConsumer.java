package com.aics.knowledge.mq;

import com.aics.knowledge.entity.KnowledgeDocument;
import com.aics.knowledge.service.KnowledgeVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 知识文档增量同步 RocketMQ 消费者
 *
 * <p>消费 topic=knowledge-doc-sync-topic 的文档同步消息（所有 tag），
 * 根据 action 执行对应的向量化/删除操作：</p>
 * <ul>
 *   <li>CREATE / UPDATE → 调用 KnowledgeVectorService.vectorize 向量化入库（Chroma）</li>
 *   <li>DELETE → 调用 KnowledgeVectorService.deleteByDocumentId 删除向量</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "knowledge-doc-sync-topic",
        consumerGroup = "knowledge-sync-group",
        selectorExpression = "*"
)
public class KnowledgeSyncConsumer implements RocketMQListener<KnowledgeSyncMessage> {

    private final KnowledgeVectorService knowledgeVectorService;

    @Override
    public void onMessage(KnowledgeSyncMessage message) {
        String action = message.getAction();
        log.info("消费知识文档同步消息: action={}, documentId={}", action, message.getDocumentId());
        try {
            if ("DELETE".equals(action)) {
                knowledgeVectorService.deleteByDocumentId(message.getDocumentId());
                log.info("向量删除完成: documentId={}", message.getDocumentId());
            } else {
                // CREATE / UPDATE：构造文档对象后直接向量化入库
                KnowledgeDocument doc = new KnowledgeDocument();
                doc.setId(message.getDocumentId());
                doc.setTitle(message.getTitle());
                doc.setContent(message.getContent());
                int chunks = knowledgeVectorService.vectorize(doc);
                if (chunks > 0) {
                    log.info("向量化完成: action={}, documentId={}, chunks={}", action, message.getDocumentId(), chunks);
                } else {
                    log.warn("向量化无内容/失败: action={}, documentId={}", action, message.getDocumentId());
                }
            }
        } catch (Exception e) {
            log.error("知识文档同步消息消费失败: action={}, documentId={}, err={}",
                    action, message.getDocumentId(), e.getMessage());
            // 不抛异常（RocketMQ 默认重试 16 次后进入死信队列），避免阻塞后续消息消费
        }
    }
}