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
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>消费组 knowledge-sync-group，selectorExpression=* 接收所有 tag（CREATE/UPDATE/DELETE）</li>
 *   <li>消息体 {@link KnowledgeSyncMessage} 已携带文档内容，消费时无需回查 DB</li>
 *   <li>幂等考虑：Chroma 向量库基于 documentId 元数据做 delete + add，
 *       重复消费 UPDATE 会先删除旧分块再写入新分块，结果幂等</li>
 *   <li>重试策略：消费异常被 catch 吞掉（不抛出），由 RocketMQ 默认重试 16 次后进入死信队列，
 *       避免单条消息阻塞后续消费</li>
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

    /** 知识向量化服务（切块 + Embedding + 写 Chroma） */
    private final KnowledgeVectorService knowledgeVectorService;

    /**
     * 消费文档同步消息
     *
     * <p>处理流程：根据 action 分发到向量化或删除分支；
     * 任何异常都被 catch 并记录日志，不向上抛出，避免阻塞消费链路。</p>
     *
     * @param message 文档同步消息体
     */
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