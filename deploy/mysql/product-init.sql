-- ============================================================
-- 商品服务 - 数据库初始化脚本
-- 包含: product_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE product_db;

-- 商品分类表
CREATE TABLE IF NOT EXISTS product_category (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name            VARCHAR(128)    NOT NULL COMMENT '分类名称',
    parent_id       BIGINT          NOT NULL DEFAULT 0 COMMENT '父分类ID，0为顶级',
    sort            INT             NOT NULL DEFAULT 0 COMMENT '排序序号',
    description     VARCHAR(512)    DEFAULT NULL COMMENT '分类描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name            VARCHAR(200)    NOT NULL COMMENT '商品名称',
    description     TEXT            DEFAULT NULL COMMENT '商品描述',
    price           DECIMAL(10,2)   NOT NULL COMMENT '商品价格',
    stock           INT             NOT NULL DEFAULT 0 COMMENT '库存数量',
    category_id     BIGINT          DEFAULT NULL COMMENT '所属分类ID',
    image           VARCHAR(512)    DEFAULT NULL COMMENT '商品主图URL',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    sales           INT             NOT NULL DEFAULT 0 COMMENT '销量',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_status (status),
    KEY idx_name (name),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 初始化分类数据
INSERT INTO product_category (id, name, parent_id, sort) VALUES
(1, '数码配件', 0, 1),
(2, '生活用品', 0, 2),
(3, '食品饮料', 0, 3),
(4, '耳机音箱', 1, 1),
(5, '手机配件', 1, 2)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 初始化商品数据
INSERT INTO product (id, name, description, price, stock, category_id, image, status, sales) VALUES
(1001, '无线蓝牙耳机', '高品质降噪蓝牙耳机，续航30小时', 199.00, 100, 4, 'https://img.example.com/earphone.jpg', 1, 50),
(1002, '手机壳', '防摔透明手机壳，适配多种机型', 29.00, 500, 5, 'https://img.example.com/case.jpg', 1, 200),
(1003, '便携充电宝', '20000mAh大容量，支持快充', 129.00, 80, 5, 'https://img.example.com/powerbank.jpg', 1, 120)
ON DUPLICATE KEY UPDATE name = VALUES(name);
