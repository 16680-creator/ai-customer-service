-- ============================================================
-- 危险操作！仅用于开发环境：删除全部业务库，供 all-init.sql 全量重建
-- 用法: mysql -uroot -p --default-character-set=utf8mb4 < drop-all.sql
--       然后执行 all-init.sql 重建
-- ============================================================

DROP DATABASE IF EXISTS user_db;
DROP DATABASE IF EXISTS knowledge_db;
DROP DATABASE IF EXISTS chat_db;
DROP DATABASE IF EXISTS product_db;
DROP DATABASE IF EXISTS ai_customer_service;
DROP DATABASE IF EXISTS nacos_config;
