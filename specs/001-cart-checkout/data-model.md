# 数据模型：电商购物车结算流程

**日期**: 2026-08-01
**数据库**: MySQL 8.0
**字符集**: utf8mb4

---

## 实体关系概览

```
User(1) ──── (N)CartItem
User(1) ──── (N)Order
User(1) ──── (N)Coupon
Order(1) ──── (N)OrderItem
Order(N) ──── (1)Coupon (可选使用)
FullReductionRule 独立配置表
```

---

## 表结构定义

### 1. cart_item（购物车项）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL, INDEX | 用户ID |
| product_id | BIGINT | NOT NULL | 商品ID |
| product_name | VARCHAR(200) | NOT NULL | 商品名称（冗余快照） |
| product_price | DECIMAL(10,2) | NOT NULL | 加入时单价 |
| quantity | INT | NOT NULL, DEFAULT 1 | 数量 |
| selected | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否选中 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**唯一约束**: `UNIQUE(user_id, product_id)` — 同一用户同一商品只有一条记录

---

### 2. orders（订单表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_no | VARCHAR(32) | NOT NULL, UNIQUE | 订单编号 |
| user_id | BIGINT | NOT NULL, INDEX | 用户ID |
| total_amount | DECIMAL(10,2) | NOT NULL | 商品总金额（原价） |
| discount_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | 优惠总金额 |
| pay_amount | DECIMAL(10,2) | NOT NULL | 应付金额 |
| full_reduction_amount | DECIMAL(10,2) | DEFAULT 0 | 满减优惠金额 |
| coupon_amount | DECIMAL(10,2) | DEFAULT 0 | 优惠券抵扣金额 |
| coupon_id | BIGINT | NULL | 使用的优惠券ID |
| payment_method | VARCHAR(20) | NULL | 支付方式（WECHAT/ALIPAY/BANK_CARD） |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING_PAY' | 订单状态 |
| pay_time | DATETIME | NULL | 支付时间 |
| cancel_time | DATETIME | NULL | 取消时间 |
| expire_time | DATETIME | NOT NULL | 支付截止时间 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**状态枚举（OrderStatus）**:
- `PENDING_PAY` — 待支付
- `PAID` — 已支付
- `CANCELLED` — 已取消（超时/用户取消）

---

### 3. order_item（订单项）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_id | BIGINT | NOT NULL, INDEX | 订单ID |
| order_no | VARCHAR(32) | NOT NULL | 订单编号（冗余） |
| product_id | BIGINT | NOT NULL | 商品ID |
| product_name | VARCHAR(200) | NOT NULL | 商品名称快照 |
| product_price | DECIMAL(10,2) | NOT NULL | 成交单价快照 |
| quantity | INT | NOT NULL | 购买数量 |
| subtotal | DECIMAL(10,2) | NOT NULL | 小计金额 |

---

### 4. coupon（优惠券）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL, INDEX | 持有用户ID |
| coupon_name | VARCHAR(100) | NOT NULL | 优惠券名称 |
| amount | DECIMAL(10,2) | NOT NULL | 面额 |
| min_order_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | 使用门槛（满X元可用） |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'UNUSED' | 状态 |
| expire_time | DATETIME | NOT NULL | 过期时间 |
| use_time | DATETIME | NULL | 使用时间 |
| order_no | VARCHAR(32) | NULL | 关联订单号 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**状态枚举（CouponStatus）**:
- `UNUSED` — 未使用
- `USED` — 已使用
- `EXPIRED` — 已过期

---

### 5. full_reduction_rule（满减规则）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| rule_name | VARCHAR(100) | NOT NULL | 规则名称 |
| threshold_amount | DECIMAL(10,2) | NOT NULL | 满足金额门槛 |
| reduction_amount | DECIMAL(10,2) | NOT NULL | 减免金额 |
| start_time | DATETIME | NOT NULL | 生效开始时间 |
| end_time | DATETIME | NOT NULL | 生效结束时间 |
| enabled | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否启用 |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

## 状态机

### 订单状态流转

```
[创建订单] → PENDING_PAY
                ├── [支付成功回调] → PAID（终态）
                ├── [超时30分钟] → CANCELLED（终态）
                └── [用户主动取消] → CANCELLED（终态）
```

### 优惠券状态流转

```
[发放] → UNUSED
           ├── [结算使用] → USED（终态）
           ├── [过期] → EXPIRED（终态）
           └── [订单取消退回] → UNUSED
```

---

## 验证规则

| 实体 | 字段 | 规则 |
|------|------|------|
| CartItem | quantity | ≥ 1 且 ≤ 商品库存 |
| Order | pay_amount | = total_amount - discount_amount，且 ≥ 0 |
| Order | expire_time | = create_time + 30 分钟 |
| Coupon | amount | > 0 |
| Coupon | min_order_amount | ≥ 0 |
| FullReductionRule | reduction_amount | > 0 且 < threshold_amount |
