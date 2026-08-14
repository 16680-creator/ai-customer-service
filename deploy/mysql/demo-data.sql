-- ============================================================
-- AI客服系统 - 业务演示数据（Demo Data）
-- 覆盖: user_db / knowledge_db / chat_db / product_db / ai_customer_service
-- 特性: 全表名带库名前缀，任意库上下文均可直接执行；可重复执行
-- 账号密码: 所有新增用户密码均为 123456（BCrypt 加密存储）
-- 图片地址: 使用已配置的 MinIO（http://123.60.31.79:9000，桶 ai-cs）
-- ============================================================

-- 确保数据库存在
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS chat_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ai_customer_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ==================== 1. user_db 用户数据库 ====================

-- 系统用户（id: 1=admin 已由 init.sql 创建；2~3=客服，4~6=普通用户）
INSERT INTO user_db.sys_user (id, username, password, nickname, phone, email, avatar, status, role) VALUES
(2, 'agent01', '$2a$10$niXVDigXCqD5CxlDHyEVSuA3dwgEuh8vmAHx4D.O8D1TelQmyCbNG', '客服小美', '13800000002', 'agent01@aics.com', 'http://123.60.31.79:9000/ai-cs/avatar/agent01.png', 1, 'agent'),
(3, 'agent02', '$2a$10$niXVDigXCqD5CxlDHyEVSuA3dwgEuh8vmAHx4D.O8D1TelQmyCbNG', '客服小帅', '13800000003', 'agent02@aics.com', 'http://123.60.31.79:9000/ai-cs/avatar/agent02.png', 1, 'agent'),
(4, 'zhangsan', '$2a$10$niXVDigXCqD5CxlDHyEVSuA3dwgEuh8vmAHx4D.O8D1TelQmyCbNG', '张三', '13900000004', 'zhangsan@aics.com', 'http://123.60.31.79:9000/ai-cs/avatar/zhangsan.png', 1, 'user'),
(5, 'lisi', '$2a$10$niXVDigXCqD5CxlDHyEVSuA3dwgEuh8vmAHx4D.O8D1TelQmyCbNG', '李四', '13900000005', 'lisi@aics.com', 'http://123.60.31.79:9000/ai-cs/avatar/lisi.png', 1, 'user'),
(6, 'wangwu', '$2a$10$niXVDigXCqD5CxlDHyEVSuA3dwgEuh8vmAHx4D.O8D1TelQmyCbNG', '王五', '13900000006', 'wangwu@aics.com', 'http://123.60.31.79:9000/ai-cs/avatar/wangwu.png', 1, 'user')
ON DUPLICATE KEY UPDATE username = VALUES(username), nickname = VALUES(nickname), role = VALUES(role);

-- 用户角色关联（role_id: 2=客服, 3=普通用户，由 init.sql 初始化）
INSERT INTO user_db.sys_user_role (id, user_id, role_id) VALUES
(2, 2, 2),
(3, 3, 2),
(4, 4, 3),
(5, 5, 3),
(6, 6, 3)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), role_id = VALUES(role_id);


-- ==================== 2. knowledge_db 知识库数据库 ====================

-- 知识库分类
INSERT INTO knowledge_db.kb_category (id, name, parent_id, sort_order, description) VALUES
(1, '商品咨询', 0, 1, '商品参数、功能、使用说明等'),
(2, '订单售后', 0, 2, '下单、支付、退款、售后等'),
(3, '物流配送', 0, 3, '发货、配送、签收、物流查询等'),
(4, '账户问题', 0, 4, '注册、登录、密码、账号安全等')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- 知识文档（status: 1=已索引）
INSERT INTO knowledge_db.kb_document (id, title, content, doc_type, source_url, summary, tags, category_id, status, create_by) VALUES
(1001, '无线蓝牙耳机如何连接手机？', '第一步：确保耳机已充满电；第二步：长按耳机多功能键 3 秒进入配对模式，指示灯红蓝交替闪烁；第三步：打开手机蓝牙，搜索到"AI-CS-Earphone"并点击连接；第四步：连接成功后指示灯常亮。若连接失败，请将耳机放回充电仓并重新取出后重试。', 'markdown', 'https://docs.aics.com/earphone/connect', '蓝牙耳机连接手机的详细步骤与常见问题排查', '蓝牙,连接,耳机', 1, 1, 1),
(1002, '充电宝可以带上飞机吗？', '根据民航规定，额定能量不超过 100Wh（约 27000mAh）的充电宝可以随身携带，禁止托运。本店 20000mAh 充电宝额定能量为 74Wh，可以随身携带登机。携带时请确保充电宝标识清晰、无破损鼓包。', 'markdown', 'https://docs.aics.com/powerbank/flight', '20000mAh充电宝符合民航随身携带规定', '充电宝,航空,安检', 1, 1, 1),
(1003, '订单支付后多久发货？', '现货商品一般在支付成功后 24 小时内发货（工作日），预售商品以商品页标注时间为准。发货后您可在订单详情中查看物流单号。如遇大促或极端天气，发货时间可能顺延，敬请谅解。', 'markdown', NULL, '现货商品24小时内发货，预售以页面标注为准', '发货,物流,订单', 2, 1, 1),
(1004, '如何申请退货退款？', '自签收之日起 7 天内支持无理由退货（不影响二次销售）。操作路径：我的订单 -> 选择订单 -> 申请售后 -> 选择退货退款并填写原因。退款将在商家确认收货后 1-3 个工作日内原路退回。生鲜、定制类商品不支持无理由退货。', 'markdown', NULL, '7天无理由退货，退款1-3个工作日原路退回', '退货,退款,售后', 2, 1, 1),
(1005, '优惠券如何使用？', '下单时在结算页选择可用优惠券即可自动抵扣。优惠券不可叠加使用，每笔订单限用一张；部分优惠券有使用门槛，需满足满减金额才能使用。过期未使用的优惠券自动作废，不支持补发。', 'markdown', NULL, '结算页选择优惠券，不可叠加，过期作废', '优惠券,满减,结算', 2, 1, 1),
(1006, '如何查看物流进度？', '您可以在 我的订单 -> 订单详情 -> 物流信息 中查看实时物流轨迹。若物流信息超过 48 小时未更新，可联系在线客服为您催件。签收时请当面验货，如有破损请拍照留存并拒收。', 'markdown', NULL, '订单详情可查看物流轨迹，异常可联系客服', '物流,查询,签收', 3, 1, 1),
(1007, '忘记密码怎么办？', '在登录页点击"忘记密码"，输入注册手机号，获取短信验证码后即可重置密码。如手机号已停用，请联系在线客服，通过身份验证后为您人工重置。', 'markdown', NULL, '手机号验证码重置密码，停用手机号可联系客服', '密码,登录,账号', 4, 1, 1),
(1008, '如何修改收货地址？', '下单前：我的 -> 收货地址 -> 新增或编辑地址。下单后：若订单未发货，可在订单详情中申请修改地址；若已发货，请及时联系客服协调改址，但不保证一定成功。', 'markdown', NULL, '未发货订单可修改地址，已发货需联系客服', '地址,收货,订单', 2, 1, 1)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), status = VALUES(status);

-- 知识标签
INSERT INTO knowledge_db.kb_tag (id, name) VALUES
(1, '蓝牙'),
(2, '充电宝'),
(3, '退货'),
(4, '优惠券'),
(5, '物流'),
(6, '账号')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 文档-标签关联
INSERT INTO knowledge_db.kb_document_tag (id, document_id, tag_id) VALUES
(1, 1001, 1),
(2, 1002, 2),
(3, 1004, 3),
(4, 1005, 4),
(5, 1006, 5),
(6, 1007, 6)
ON DUPLICATE KEY UPDATE document_id = VALUES(document_id), tag_id = VALUES(tag_id);


-- ==================== 3. chat_db 对话数据库 ====================

-- 会话（status: 0=已结束 1=进行中 2=转人工）
INSERT INTO chat_db.chat_session (id, user_id, agent_id, channel, status, title) VALUES
(1, 4, 2, 'web', 2, '蓝牙耳机连接不上'),
(2, 4, NULL, 'web', 1, '充电宝充电速度慢'),
(3, 5, 3, 'app', 0, '商品退货流程咨询'),
(4, 6, NULL, 'web', 1, '订单一直未发货'),
(5, 5, 2, 'wechat', 2, '优惠券无法使用')
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), status = VALUES(status);

-- 消息（sender_type: 1=用户 2=AI 3=客服）
INSERT INTO chat_db.chat_message (id, session_id, sender_type, sender_id, content, content_type) VALUES
(1, 1, 1, 4, '你好，我买的蓝牙耳机连接不上手机，怎么办？', 'text'),
(2, 1, 2, NULL, '您好，请问耳机指示灯现在是什么状态？可以尝试长按多功能键 3 秒进入配对模式哦。', 'text'),
(3, 1, 1, 4, '按你说的试了还是连不上，能帮我转人工吗？', 'text'),
(4, 1, 3, 2, '您好，我是客服小美，已为您接入人工服务，请描述一下具体现象。', 'text'),
(5, 2, 1, 4, '充电宝充手机很慢，是不是坏了？', 'text'),
(6, 2, 2, NULL, '您好，充电速度受充电线、手机协议影响，建议使用原装快充线和 Type-C 口，预计 2 小时充满。', 'text'),
(7, 3, 1, 5, '我想退货，具体怎么操作？', 'text'),
(8, 3, 2, NULL, '签收 7 天内可无理由退货，在"我的订单"中申请售后即可，退款 1-3 个工作日原路退回。', 'text'),
(9, 3, 1, 5, '好的明白了，谢谢！', 'text'),
(10, 3, 3, 3, '不客气，有问题随时联系我们～', 'text'),
(11, 4, 1, 6, '我昨天下的单，到现在还没发货？', 'text'),
(12, 4, 2, NULL, '您好，现货商品 24 小时内发货，您的订单正在处理中，请耐心等待。', 'text'),
(13, 5, 1, 5, '结算时优惠券选不了，显示不可用。', 'text'),
(14, 5, 2, NULL, '请问优惠券是否满足使用门槛？部分券需要满一定金额才可用。', 'text'),
(15, 5, 3, 2, '您好，已为您转人工，请提供优惠券名称，我帮您核实。', 'text')
ON DUPLICATE KEY UPDATE content = VALUES(content);

-- 反馈（rating: 1=不满意 2=一般 3=满意 4=非常满意）
INSERT INTO chat_db.chat_feedback (id, session_id, message_id, user_id, rating, comment) VALUES
(1, 3, 10, 5, 4, '客服回答很耐心，问题解决了'),
(2, 1, 4, 4, 3, '转人工等待时间有点长'),
(3, 2, 6, 4, 2, '回答比较官方，希望更具体')
ON DUPLICATE KEY UPDATE rating = VALUES(rating), comment = VALUES(comment);


-- ==================== 4. product_db 商品数据库 ====================

-- 商品分类
INSERT INTO product_db.product_category (id, name, parent_id, sort, description) VALUES
(6, '图书文具', 0, 4, '图书、文具、办公用品'),
(7, '运动户外', 0, 5, '运动器材、户外装备')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 商品（image 使用 MinIO ai-cs 桶地址）
INSERT INTO product_db.product (id, name, description, price, stock, category_id, image, status, sales) VALUES
(1004, '智能手环 Pro', '心率血氧监测，14天超长续航，5ATM防水', 399.00, 60, 7, 'http://123.60.31.79:9000/ai-cs/product/images/band-pro.jpg', 1, 88),
(1005, 'Type-C 快充数据线', '100W 大功率快充，编织线身，1.5米', 89.00, 300, 5, 'http://123.60.31.79:9000/ai-cs/product/images/usbc-cable.jpg', 1, 350),
(1006, '机械键盘 87 键', '红轴，RGB 背光，有线/无线双模', 169.00, 45, 1, 'http://123.60.31.79:9000/ai-cs/product/images/keyboard87.jpg', 1, 76),
(1007, '降噪头戴耳机', '主动降噪，Hi-Res 音质，40小时续航', 259.00, 35, 4, 'http://123.60.31.79:9000/ai-cs/product/images/headphone-nc.jpg', 1, 42),
(1008, '便携保温杯 500ml', '316不锈钢内胆，保温12小时', 59.00, 200, 2, 'http://123.60.31.79:9000/ai-cs/product/images/thermos.jpg', 1, 210),
(1009, '旅行背包 40L', '防泼水面料，独立电脑仓，适合短途出行', 129.00, 90, 7, 'http://123.60.31.79:9000/ai-cs/product/images/backpack40.jpg', 1, 65),
(1010, '办公笔记本套装', 'A5 横线笔记本 2 本 + 中性笔 3 支', 25.00, 500, 6, 'http://123.60.31.79:9000/ai-cs/product/images/notebook-set.jpg', 0, 12)
ON DUPLICATE KEY UPDATE name = VALUES(name), price = VALUES(price), stock = VALUES(stock);


-- ==================== 5. ai_customer_service 订单数据库 ====================

-- 满减规则
INSERT INTO ai_customer_service.full_reduction_rule (id, rule_name, threshold_amount, reduction_amount, start_time, end_time, enabled) VALUES
(1, '满100减10', 100.00, 10.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1),
(2, '满300减50', 300.00, 50.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1),
(3, '满500减100', 500.00, 100.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1)
ON DUPLICATE KEY UPDATE rule_name = VALUES(rule_name);

-- 优惠券（status: UNUSED/USED/EXPIRED）
INSERT INTO ai_customer_service.coupon (id, user_id, coupon_name, amount, min_order_amount, status, expire_time, use_time, order_no) VALUES
(1, 4, '新人满100减10', 10.00, 100.00, 'UNUSED', '2026-12-31 23:59:59', NULL, NULL),
(2, 4, '满300减30券', 30.00, 300.00, 'UNUSED', '2026-12-31 23:59:59', NULL, NULL),
(3, 5, '满100减20券', 20.00, 100.00, 'USED', '2026-08-03 10:12:00', '2026-08-03 10:12:00', '20260803101200030004'),
(4, 5, '满200减20券', 20.00, 200.00, 'EXPIRED', '2026-06-30 23:59:59', NULL, NULL),
(5, 6, '满50减5券', 5.00, 50.00, 'UNUSED', '2026-12-31 23:59:59', NULL, NULL)
ON DUPLICATE KEY UPDATE coupon_name = VALUES(coupon_name), status = VALUES(status);

-- 购物车
INSERT INTO ai_customer_service.cart_item (id, user_id, product_id, product_name, product_price, quantity, selected) VALUES
(1, 4, 1002, '手机壳', 29.00, 1, 1),
(2, 4, 1004, '智能手环 Pro', 399.00, 1, 1),
(3, 5, 1001, '无线蓝牙耳机', 199.00, 2, 1),
(4, 6, 1006, '机械键盘 87 键', 169.00, 1, 1)
ON DUPLICATE KEY UPDATE quantity = VALUES(quantity);

-- 订单（status: PENDING_PAY/PAID/CANCELLED）
INSERT INTO ai_customer_service.orders (id, order_no, user_id, total_amount, discount_amount, pay_amount, full_reduction_amount, coupon_amount, coupon_id, payment_method, status, pay_time, cancel_time, expire_time, create_time, update_time) VALUES
(1, '20260801103000010001', 4, 527.00, 100.00, 427.00, 100.00, 0.00, NULL, 'WECHAT', 'PAID', '2026-08-01 10:35:00', NULL, '2026-08-01 11:00:00', '2026-08-01 10:30:00', '2026-08-01 10:35:00'),
(2, '20260802111500010002', 4, 29.00, 0.00, 29.00, 0.00, 0.00, NULL, 'ALIPAY', 'CANCELLED', NULL, '2026-08-02 11:30:00', '2026-08-02 11:45:00', '2026-08-02 11:15:00', '2026-08-02 11:30:00'),
(3, '20260803093000020003', 5, 577.00, 100.00, 477.00, 100.00, 0.00, NULL, 'WECHAT', 'PENDING_PAY', NULL, NULL, '2026-08-03 10:00:00', '2026-08-03 09:30:00', '2026-08-03 09:30:00'),
(4, '20260803101200030004', 5, 169.00, 20.00, 149.00, 0.00, 20.00, 3, 'BANK_CARD', 'PAID', '2026-08-03 10:12:00', NULL, '2026-08-03 10:42:00', '2026-08-03 10:12:00', '2026-08-03 10:12:00'),
(5, '20260804140000030005', 6, 259.00, 0.00, 259.00, 0.00, 0.00, NULL, NULL, 'PENDING_PAY', NULL, NULL, '2026-08-04 14:30:00', '2026-08-04 14:00:00', '2026-08-04 14:00:00'),
(6, '20260805160000030006', 6, 317.00, 50.00, 267.00, 50.00, 0.00, NULL, 'ALIPAY', 'PAID', '2026-08-05 16:20:00', NULL, '2026-08-05 16:30:00', '2026-08-05 16:00:00', '2026-08-05 16:20:00')
ON DUPLICATE KEY UPDATE status = VALUES(status), pay_amount = VALUES(pay_amount);

-- 订单明细
INSERT INTO ai_customer_service.order_item (id, order_id, order_no, product_id, product_name, product_price, quantity, subtotal) VALUES
(1, 1, '20260801103000010001', 1001, '无线蓝牙耳机', 199.00, 2, 398.00),
(2, 1, '20260801103000010001', 1003, '便携充电宝', 129.00, 1, 129.00),
(3, 2, '20260802111500010002', 1002, '手机壳', 29.00, 1, 29.00),
(4, 3, '20260803093000020003', 1004, '智能手环 Pro', 399.00, 1, 399.00),
(5, 3, '20260803093000020003', 1005, 'Type-C 快充数据线', 89.00, 2, 178.00),
(6, 4, '20260803101200030004', 1006, '机械键盘 87 键', 169.00, 1, 169.00),
(7, 5, '20260804140000030005', 1007, '降噪头戴耳机', 259.00, 1, 259.00),
(8, 6, '20260805160000030006', 1001, '无线蓝牙耳机', 199.00, 1, 199.00),
(9, 6, '20260805160000030006', 1008, '便携保温杯 500ml', 59.00, 2, 118.00)
ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), subtotal = VALUES(subtotal);

-- ============================================================
-- 完成。可用以下命令验证：
--   SELECT COUNT(*) FROM user_db.sys_user;
--   SELECT COUNT(*) FROM knowledge_db.kb_document;
--   SELECT COUNT(*) FROM chat_db.chat_session;
--   SELECT COUNT(*) FROM product_db.product;
--   SELECT COUNT(*) FROM ai_customer_service.orders;
-- ============================================================