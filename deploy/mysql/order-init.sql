-- ============================================
-- 订单模块建表 SQL
-- 数据库: ai_customer_service
-- 字符集: utf8mb4
-- ============================================

-- 购物车项表
CREATE TABLE IF NOT EXISTS `cart_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称（冗余快照）',
    `product_price` DECIMAL(10,2) NOT NULL COMMENT '加入时单价',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `selected` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否选中',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车项';

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '商品总金额（原价）',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠总金额',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '应付金额',
    `full_reduction_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '满减优惠金额',
    `coupon_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠券抵扣金额',
    `coupon_id` BIGINT DEFAULT NULL COMMENT '使用的优惠券ID',
    `payment_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式（WECHAT/ALIPAY/BANK_CARD）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    `expire_time` DATETIME NOT NULL COMMENT '支付截止时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- 订单项表
CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号（冗余）',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称快照',
    `product_price` DECIMAL(10,2) NOT NULL COMMENT '成交单价快照',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单项';

-- 优惠券表
CREATE TABLE IF NOT EXISTS `coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '持有用户ID',
    `coupon_name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '面额',
    `min_order_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛（满X元可用）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT '状态（UNUSED/USED/EXPIRED）',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `use_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `order_no` VARCHAR(32) DEFAULT NULL COMMENT '关联订单号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券';

-- 满减规则表
CREATE TABLE IF NOT EXISTS `full_reduction_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `threshold_amount` DECIMAL(10,2) NOT NULL COMMENT '满足金额门槛',
    `reduction_amount` DECIMAL(10,2) NOT NULL COMMENT '减免金额',
    `start_time` DATETIME NOT NULL COMMENT '生效开始时间',
    `end_time` DATETIME NOT NULL COMMENT '生效结束时间',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_enabled_time` (`enabled`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='满减规则';
