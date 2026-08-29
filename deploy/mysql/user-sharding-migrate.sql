-- ============================================================
-- 用户表分库分表迁移脚本（存量环境专用）
--
-- 适用：已按旧版 init.sql 建有 user_db.sys_user 且存在存量用户数据的环境。
-- 全新环境直接执行新版 init.sql / all-init.sql 即可，无需本脚本。
--
-- 分片规则：取用户ID后四位，库 = 后四位 % 2，表 = 后四位 % 4
-- 与 ai-cs-user 的 UserIdLast4ModShardingAlgorithm 保持一致。
--
-- 执行前提：
--   1. 已执行新版 init.sql（user_db_0 / user_db_1 的 8 张分片表已存在）
--   2. ai-cs-user 服务已停止写入（避免迁移期间数据丢失）
-- ============================================================

-- 1. 原表改名备份（不删数据，可回滚）
RENAME TABLE user_db.sys_user TO user_db.sys_user_migrated;

-- 2. 存量数据按路由规则迁入 8 张分片表
--    路由条件：last4 = CAST(RIGHT(id, 4) AS UNSIGNED)；库 = last4 % 2；表 = last4 % 4

INSERT INTO user_db_0.sys_user_0 SELECT * FROM user_db.sys_user_migrated WHERE CAST(RIGHT(id, 4) AS UNSIGNED) % 2 = 0 AND CAST(RIGHT(id, 4) AS UNSIGNED) % 4 = 0;
INSERT INTO user_db_0.sys_user_1 SELECT * FROM user_db.sys_user_migrated WHERE CAST(RIGHT(id, 4) AS UNSIGNED) % 2 = 0 AND CAST(RIGHT(id, 4) AS UNSIGNED) % 4 = 1;
INSERT INTO user_db_0.sys_user_2 SELECT * FROM user_db.sys_user_migrated WHERE CAST(RIGHT(id, 4) AS UNSIGNED) % 2 = 0 AND CAST(RIGHT(id, 4) AS UNSIGNED) % 4 = 2;
INSERT INTO user_db_0.sys_user_3 SELECT * FROM user_db.sys_user_migrated WHERE CAST(RIGHT(id, 4) AS UNSIGNED) % 2 = 0 AND CAST(RIGHT(id, 4) AS UNSIGNED) % 4 = 3;
INSERT INTO user_db_1.sys_user_0 SELECT * FROM user_db.sys_user_migrated WHERE CAST(RIGHT(id, 4) AS UNSIGNED) % 2 = 1 AND CAST(RIGHT(id, 4) AS UNSIGNED) % 4 = 0;
INSERT INTO user_db_1.sys_user_1 SELECT * FROM user_db.sys_user_migrated WHERE CAST(RIGHT(id, 4) AS UNSIGNED) % 2 = 1 AND CAST(RIGHT(id, 4) AS UNSIGNED) % 4 = 1;
INSERT INTO user_db_1.sys_user_2 SELECT * FROM user_db.sys_user_migrated WHERE CAST(RIGHT(id, 4) AS UNSIGNED) % 2 = 1 AND CAST(RIGHT(id, 4) AS UNSIGNED) % 4 = 2;
INSERT INTO user_db_1.sys_user_3 SELECT * FROM user_db.sys_user_migrated WHERE CAST(RIGHT(id, 4) AS UNSIGNED) % 2 = 1 AND CAST(RIGHT(id, 4) AS UNSIGNED) % 4 = 3;

-- 3. 迁移校验：总量与各分片数量应吻合（无丢行、无重复）
SELECT 'migrated(source)' AS shard, COUNT(*) AS cnt FROM user_db.sys_user_migrated
UNION ALL SELECT 'user_db_0.sys_user_0', COUNT(*) FROM user_db_0.sys_user_0
UNION ALL SELECT 'user_db_0.sys_user_1', COUNT(*) FROM user_db_0.sys_user_1
UNION ALL SELECT 'user_db_0.sys_user_2', COUNT(*) FROM user_db_0.sys_user_2
UNION ALL SELECT 'user_db_0.sys_user_3', COUNT(*) FROM user_db_0.sys_user_3
UNION ALL SELECT 'user_db_1.sys_user_0', COUNT(*) FROM user_db_1.sys_user_0
UNION ALL SELECT 'user_db_1.sys_user_1', COUNT(*) FROM user_db_1.sys_user_1
UNION ALL SELECT 'user_db_1.sys_user_2', COUNT(*) FROM user_db_1.sys_user_2
UNION ALL SELECT 'user_db_1.sys_user_3', COUNT(*) FROM user_db_1.sys_user_3;

-- 期望：source 总数 = 8 张分片表之和

-- 4. 业务验证通过后，手动清理备份表（保留观察一段时间再删更稳妥）：
--    DROP TABLE user_db.sys_user_migrated;
