package com.aics.knowledge.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 知识文档同步消息体
 *
 * <p>由 ai-cs-knowledge 生产者投递到 RocketMQ topic=knowledge-doc-sync-topic，
 * ai-cs-knowledge 消费者（同模块内）消费后执行向量化/删除操作。</p>
 */
@Data
public class KnowledgeSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 操作类型：CREATE / UPDATE / DELETE */
    private String action;

    /** 文档 ID（KnowledgeDocument.id） */
    private Long documentId;

    /** 知识库标识 */
    private String knowledgeBase;

    /** 文档标题 */
    private String title;

    /** 文档内容 */
    private String content;

    /** 消息时间戳 */
    private Long timestamp;
}