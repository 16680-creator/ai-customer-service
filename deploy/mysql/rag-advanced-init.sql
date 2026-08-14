-- ============================================================================
-- RAG 进阶六件套：新增表
-- 适用库：aics_knowledge（知识库库）
-- 1) kb_faq             FAQ 条目（知识库运营闭环 US6）
-- 2) kb_graph_triple    知识图谱三元组（GraphRAG US4，MVP 走进程内存储，表为可选持久化）
-- ============================================================================

CREATE TABLE IF NOT EXISTS kb_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(512) NOT NULL COMMENT 'FAQ 问题',
    answer TEXT NOT NULL COMMENT 'FAQ 答案',
    knowledge_base VARCHAR(64) DEFAULT 'faq' COMMENT '知识库标识',
    topic_id VARCHAR(64) NULL COMMENT '来源聚类主题 ID',
    status VARCHAR(16) DEFAULT 'DRAFT' COMMENT 'DRAFT/FAQ_ADOPTED/IGNORED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_kb (knowledge_base)
) COMMENT='FAQ 条目';

CREATE TABLE IF NOT EXISTS kb_graph_triple (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(255) NOT NULL COMMENT '主体实体',
    predicate VARCHAR(128) NOT NULL COMMENT '关系',
    object VARCHAR(255) NOT NULL COMMENT '客体实体',
    knowledge_base VARCHAR(64) NOT NULL COMMENT '知识库标识',
    source_document_id BIGINT NULL COMMENT '来源文档 ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_kb (knowledge_base),
    KEY idx_subject (subject)
) COMMENT='知识图谱三元组';