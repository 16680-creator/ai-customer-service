-- ============================================================
-- AI客服系统 - RocketMQ 工单消息闭环演示（ai-cs-mq 模块）
-- 库:   mq_db
-- 表:   work_order        工单主表（NEW -> DONE / FAILED）
--       mq_fail_record    消费失败记录（死信落库，供前端查看与重推）
-- 全部语句幂等，可重复执行
-- 用法: mysql -uroot -p < mq-workorder-init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS mq_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mq_db;

-- 工单表（演示：生产者创建工单 -> 消费者消费并更新状态）
CREATE TABLE IF NOT EXISTS work_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    ticket_no       VARCHAR(64)     NOT NULL COMMENT '工单号（业务唯一键，WO+时间戳+序号）',
    title           VARCHAR(128)    NOT NULL COMMENT '工单标题',
    content         VARCHAR(1024)   DEFAULT NULL COMMENT '工单内容',
    status          VARCHAR(16)     NOT NULL DEFAULT 'NEW' COMMENT '状态：NEW-待消费 DONE-消费完成 FAILED-消费失败(死信)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表（RocketMQ 演示）';

-- 消费失败记录表（死信消费者落库；msg_id 唯一约束保证落库幂等）
CREATE TABLE IF NOT EXISTS mq_fail_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    msg_id          VARCHAR(64)     NOT NULL COMMENT 'RocketMQ 消息ID',
    topic           VARCHAR(128)    NOT NULL COMMENT '来源业务主题',
    consumer_group  VARCHAR(128)    NOT NULL COMMENT '消费组',
    biz_key         VARCHAR(64)     DEFAULT NULL COMMENT '业务键（工单号）',
    payload         TEXT            DEFAULT NULL COMMENT '原始消息体（JSON，重推时原样回放）',
    fail_reason     VARCHAR(512)    DEFAULT NULL COMMENT '失败原因',
    reconsume_times INT             NOT NULL DEFAULT 0 COMMENT 'MQ 已重试次数',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待重推 REPUSHED-已重推',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '落库时间',
    repush_time     DATETIME        DEFAULT NULL COMMENT '重推时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_msg_id (msg_id),
    KEY idx_biz_key (biz_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MQ 消费失败记录表';
