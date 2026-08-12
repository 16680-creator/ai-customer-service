package com.aics.knowledge.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 知识文档同步消息体
 *
 * <p>由 ai-cs-knowledge 生产者投递到 RocketMQ topic=knowledge-doc-sync-topic，
 * ai-cs-knowledge 消费者（同模块内）消费后执行向量化/删除操作。</p>
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>消息体携带文档标题与内容，消费者无需回查数据库，降低 DB 压力</li>
 *   <li>action 区分操作类型（CREATE/UPDATE/DELETE），同时作为 RocketMQ 的 tag 投递</li>
 *   <li>knowledgeBase 字段固定为 "knowledge"，对应 Chroma 中向量分块的元数据，
 *       与 ai-cs-chat 检索时使用的 knowledgeBase 参数一致</li>
 *   <li>timestamp 用于消息时序参考（非强幂等依据）</li>
 * </ul>
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