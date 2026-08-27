-- ============================================================
-- 红框六菜单 - 可选 MySQL 测试数据（仅列表展示用途）
-- 警告：
--   1) 推荐优先使用 seed-redbox-data.ps1（经 API 写入会触发自动向量化）。
--   2) 本脚本直插数据库【不会】触发向量化：知识库管理列表/全文搜索 MySQL 侧可见，
--      但向量检索、RAG 评估命中、Agent 规则检索不会生效。
--   3) 若已执行过种子脚本，请勿再执行本脚本（ID 冲突，已用 ON DUPLICATE 兜底）。
-- 覆盖：knowledge_db.kb_document（2001~2006，与 golden-set-demo.json 期望文档一致）
-- ============================================================

CREATE DATABASE IF NOT EXISTS knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO knowledge_db.kb_document
(id, title, content, doc_type, summary, tags, category_id, status, create_by, deleted)
VALUES
(2001, '型号 ABC-123 保修期说明', '型号 ABC-123 保修期为 1 年，自签收之日起计算。保修期内非人为损坏可享受免费维修服务，人为损坏或超期维修需收取配件费用。', 'txt', 'ABC-123 保修 1 年', '保修,售后', NULL, 1, 1, 0),
(2002, '订单退款申请流程', '订单怎么申请退款？进入订单详情页点击申请退款，填写退款原因提交即可。审核通过后退款 1-3 个工作日原路退回。', 'txt', '订单详情页申请退款', '退款,订单', NULL, 1, 1, 0),
(2003, '退货运费承担规则', '运费由谁承担？非质量问题退货运费由买家承担；质量问题退货运费由卖家承担。', 'txt', '非质量问题运费买家承担', '运费,退货', NULL, 1, 1, 0),
(2004, '平台支持的支付方式', '支持哪些支付方式？支持微信、支付宝、银行卡支付，当前不支持货到付款。', 'txt', '微信/支付宝/银行卡', '支付', NULL, 1, 1, 0),
(2005, '发货与送达时效', '发货后多久能收到？现货商品支付成功后 24 小时内发货，一般 3-5 个工作日送达。', 'txt', '24 小时发货，3-5 天送达', '发货,物流', NULL, 1, 1, 0),
(2006, '修改收货地址规则', '如何修改收货地址？在订单详情页修改收货地址，发货前可改；已发货需联系客服协调改址。', 'txt', '发货前可改地址', '地址,订单', NULL, 1, 1, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), status = VALUES(status);

-- 验证：
--   SELECT id, title, status FROM knowledge_db.kb_document WHERE id BETWEEN 2001 AND 2006;
