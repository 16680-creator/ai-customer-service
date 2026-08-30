-- ============================================================
-- Seata AT 模式 undo_log 表（每个参与分支事务的业务库各建一张）
--
-- 学习要点：AT 模式一阶段，分支事务提交前 Seata 数据源代理会把「前后镜像」
-- 序列化进 undo_log 并与业务 SQL 同一本地事务提交；二阶段全局提交时异步删除，
-- 全局回滚时按镜像生成反向补偿 SQL。表名固定 undo_log，不可改名。
--
-- 适用库：ai_customer_service（order）、product_db（product）
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_customer_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_customer_service.undo_log (
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文（序列化方式等）',
    rollback_info LONGBLOB     NOT NULL COMMENT '前后镜像数据',
    log_status    INT          NOT NULL COMMENT '状态：0-正常 1-防御状态',
    log_created   DATETIME(6)  NOT NULL COMMENT '创建时间',
    log_modified  DATETIME(6)  NOT NULL COMMENT '修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata AT undo_log（order 库）';

CREATE TABLE IF NOT EXISTS product_db.undo_log (
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文（序列化方式等）',
    rollback_info LONGBLOB     NOT NULL COMMENT '前后镜像数据',
    log_status    INT          NOT NULL COMMENT '状态：0-正常 1-防御状态',
    log_created   DATETIME(6)  NOT NULL COMMENT '创建时间',
    log_modified  DATETIME(6)  NOT NULL COMMENT '修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata AT undo_log（product 库）';
