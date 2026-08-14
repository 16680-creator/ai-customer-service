-- H2 测试用建表 SQL（兼容 MySQL 模式）

CREATE TABLE IF NOT EXISTS cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    selected BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    pay_amount DECIMAL(10,2) NOT NULL,
    full_reduction_amount DECIMAL(10,2) DEFAULT 0,
    coupon_amount DECIMAL(10,2) DEFAULT 0,
    coupon_id BIGINT,
    payment_method VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAY',
    pay_time TIMESTAMP,
    expire_time TIMESTAMP,
    cancel_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coupon_name VARCHAR(200) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2) DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'UNUSED',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    use_time TIMESTAMP,
    order_no VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS full_reduction_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(200) NOT NULL,
    threshold_amount DECIMAL(10,2) NOT NULL,
    reduction_amount DECIMAL(10,2) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 售后申请表（结构对齐 deploy/mysql/after-sales-agent-init.sql，去除 MySQL 专有语法）
CREATE TABLE IF NOT EXISTS after_sale_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_no VARCHAR(32) NOT NULL UNIQUE,
    run_id VARCHAR(64),
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(200),
    quantity INT NOT NULL DEFAULT 1,
    action_type VARCHAR(20) NOT NULL,
    reason VARCHAR(512),
    evidence_summary VARCHAR(1024),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
