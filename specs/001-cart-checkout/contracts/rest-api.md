# REST API 契约：电商购物车结算流程

**日期**: 2026-08-01
**基础路径**: `/api/order`（经网关路由）
**认证**: Bearer Token（JWT）
**统一响应**: `Result<T>` { code, message, data }

---

## 购物车接口

### 1. 获取购物车列表

```
GET /api/order/cart/list
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "productId": 1001,
        "productName": "无线蓝牙耳机",
        "productPrice": 199.00,
        "quantity": 2,
        "selected": true,
        "subtotal": 398.00
      }
    ],
    "totalAmount": 398.00,
    "selectedCount": 1
  }
}
```

---

### 2. 修改购物车商品数量

```
PUT /api/order/cart/quantity
```

**请求体**:
```json
{
  "cartItemId": 1,
  "quantity": 3
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cartItemId": 1,
    "quantity": 3,
    "subtotal": 597.00,
    "totalAmount": 597.00
  }
}
```

**错误响应**（库存不足）:
```json
{
  "code": 40001,
  "message": "库存不足，最多可购买 5 件",
  "data": { "maxQuantity": 5 }
}
```

---

### 3. 删除购物车商品

```
DELETE /api/order/cart/{cartItemId}
```

**响应**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

### 4. 切换商品选中状态

```
PUT /api/order/cart/select
```

**请求体**:
```json
{
  "cartItemIds": [1, 2, 3],
  "selected": true
}
```

---

## 结算接口

### 5. 获取结算确认页信息

```
POST /api/order/checkout/confirm
```

**请求体**:
```json
{
  "cartItemIds": [1, 2]
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [...],
    "totalAmount": 450.00,
    "fullReduction": {
      "applied": true,
      "ruleName": "满200减30",
      "amount": 30.00
    },
    "availableCoupons": [
      {
        "id": 101,
        "couponName": "新人券",
        "amount": 15.00,
        "minOrderAmount": 100.00,
        "usable": true
      },
      {
        "id": 102,
        "couponName": "大额券",
        "amount": 50.00,
        "minOrderAmount": 500.00,
        "usable": false,
        "reason": "未满500元"
      }
    ],
    "payAmount": 420.00
  }
}
```

---

## 订单接口

### 6. 提交订单

```
POST /api/order/create
```

**请求体**:
```json
{
  "cartItemIds": [1, 2],
  "couponId": 101,
  "paymentMethod": "WECHAT"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "下单成功",
  "data": {
    "orderNo": "20260801143022010001",
    "payAmount": 405.00,
    "paymentMethod": "WECHAT",
    "payUrl": "https://pay.weixin.qq.com/...",
    "expireTime": "2026-08-01T15:00:22"
  }
}
```

**错误响应**（库存不足）:
```json
{
  "code": 40002,
  "message": "商品库存不足，订单无法提交",
  "data": { "failedProducts": ["无线蓝牙耳机"] }
}
```

---

### 7. 查询订单详情

```
GET /api/order/{orderNo}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderNo": "20260801143022010001",
    "status": "PENDING_PAY",
    "totalAmount": 450.00,
    "discountAmount": 45.00,
    "payAmount": 405.00,
    "fullReductionAmount": 30.00,
    "couponAmount": 15.00,
    "paymentMethod": "WECHAT",
    "items": [...],
    "createTime": "2026-08-01T14:30:22",
    "expireTime": "2026-08-01T15:00:22"
  }
}
```

---

### 8. 取消订单

```
PUT /api/order/{orderNo}/cancel
```

**响应**:
```json
{
  "code": 200,
  "message": "订单已取消",
  "data": null
}
```

---

## 支付回调接口

### 9. 支付结果回调（内部）

```
POST /api/order/pay/callback/{paymentMethod}
```

**说明**: 由支付渠道异步通知，非用户调用。幂等处理，重复回调不重复变更状态。

**响应**: 返回支付渠道要求的确认格式

---

### 10. 更换支付方式重试

```
PUT /api/order/{orderNo}/retry-pay
```

**请求体**:
```json
{
  "paymentMethod": "ALIPAY"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "payUrl": "https://openapi.alipay.com/...",
    "expireTime": "2026-08-01T15:00:22"
  }
}
```

---

## 错误码定义

| 错误码 | 说明 |
|--------|------|
| 40001 | 库存不足 |
| 40002 | 下单失败（库存/商品状态异常） |
| 40003 | 优惠券不可用（过期/不满足门槛/非本人） |
| 40004 | 订单不存在或状态不允许操作 |
| 40005 | 支付方式无效 |
| 40006 | 购物车为空或未选择商品 |
