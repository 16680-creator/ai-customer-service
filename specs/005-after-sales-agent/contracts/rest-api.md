# REST 契约：智能客服 Agent 编排与人工转接

> 统一响应 `Result<T>`；用户身份一律 `X-User-Id` 请求头；跨服务调用经 OpenFeign + 注册中心。
> 约定：chat 侧 Feign 客户端 DTO 为自持副本（与 OrderVO 模式一致）。

## 1. ai-cs-chat（Agent 对话入口，端口 8083）

### POST /chat/agent — Agent 多轮对话

请求头：`X-User-Id: Long`（必填）

```json
{
  "sessionId": 1,
  "runId": "可选（续跑时传）",
  "input": "我昨天买的耳机坏了，想换货，另外帮我看看有没有同价位降噪更好的"
}
```

响应 `Result<AgentTurnResult>`：

```json
{
  "code": 200, "message": "操作成功",
  "data": {
    "runId": "uuid",
    "state": "CONFIRM_ACTION",
    "intents": ["AFTER_SALE", "PRODUCT_RECOMMEND"],
    "reply": "已为您定位订单 ORDxxx（¥199 无线蓝牙耳机）。按规则 ASR-001 可申请换货。同价位降噪耳机推荐：…。确认将为您创建换货申请，请回复确认或拒绝。",
    "needsUserInput": true,
    "confirmationToken": "uuid-token",
    "actionPlan": { "actionType": "EXCHANGE", "orderNo": "ORDxxx", "productName": "无线蓝牙耳机", "quantity": 1, "reason": "质量问题" },
    "candidates": ["可选：候选订单/商品列表"],
    "handoff": null
  }
}
```

### POST /chat/agent/confirm — 确认/拒绝写操作

```json
{
  "runId": "uuid",
  "token": "uuid-token",
  "decision": "CONFIRM | REJECT",
  "sessionId": 1
}
```

响应同 `AgentTurnResult`。CONFIRM 后状态机进入 EXECUTE_AFTER_SALE（幂等键=runId 调订单服务）；REJECT 进入 CANCELLED。

### GET /chat/agent/runs/{runId} — 查询执行轨迹（审计回放）

响应 `Result<AgentRunDetailVO>`：run 信息 + 全部 step（类型/工具/输入输出摘要/耗时/状态）。

## 2. ai-cs-order（售后命令，端口 8087）

### POST /order/after-sale/eligibility — 售后资格校验

```json
{ "orderNo": "ORDxxx", "actionType": "EXCHANGE" }
```

响应 `Result<EligibilityVO>`：`{ eligible, reason, orderNo, orderStatus }`（归属校验失败 → 订单不存在或不属于当前用户）。

### POST /order/after-sale/apply — 创建售后申请（写操作，幂等）

```json
{
  "orderNo": "ORDxxx", "productId": 1001, "quantity": 1,
  "actionType": "EXCHANGE", "reason": "耳机损坏",
  "runId": "uuid", "idempotencyKey": "uuid:EXCHANGE"
}
```

响应 `Result<AfterSaleApplyVO>`：`{ applicationNo, status }`。idempotencyKey 唯一约束：重复提交返回首次结果（HTTP 200）。

### GET /order/after-sale/list、GET /order/after-sale/{applicationNo}

响应 `Result<List<AfterSaleApplyVO>>` / `Result<AfterSaleApplyVO>`。

## 3. ai-cs-product（商品推荐，端口 8088）

### GET /product/recommend/price-range

参数：`basePrice`(必填)、`priceTolerance`(默认 0.15)、`categoryId`(可选)、`keywords`(可选，逗号分隔，如 "降噪")、`limit`(默认 3，≤10)

响应 `Result<List<ProductRecommendVO>>`：`{ productId, name, price, categoryId, description, image, sales, matchReason }`——matchReason 由真实字段拼接（如"同价位 ¥199，描述含降噪、续航 30 小时"），禁止模型编造。

## 4. ai-cs-message（Agent 轨迹，端口 8085）

| 端点 | 说明 |
|------|------|
| POST /api/agent/runs | 创建 run（AgentRunDTO） |
| PUT /api/agent/runs/{runId}/status | 更新 run 状态与当前步骤 |
| POST /api/agent/runs/{runId}/steps | 追加 step（AgentStepDTO） |
| POST /api/agent/runs/{runId}/confirmations | 记录确认（AgentConfirmationDTO） |
| POST /api/agent/handoff-tickets | 创建转人工工单（HandoffTicketDTO → HandoffTicketVO{ticketNo, status}） |
| GET /api/agent/runs/{runId} | run + steps 详情（审计回放） |

## 5. ai-cs-notify（通知，端口 8086）

### POST /api/notify/handoff — 转人工通知

```json
{
  "ticketNo": "HFxxx", "userId": 1, "priority": "HIGH",
  "orderNo": "ORDxxx", "summary": "换货资格不满足，已转人工"
}
```

响应 `Result<Void>`；内部经 `NotifyWebSocketHandler.sendMessageToUser` 向坐席端（userId 定向）推送 JSON。

## 6. 错误码新增（ai-cs-common ResultCode）

| 枚举 | code | 说明 |
|------|------|------|
| AGENT_SAFETY_BLOCKED | 3101 | 输入安全检查拦截 |
| AGENT_INTENT_LOW_CONFIDENCE | 3102 | 意图置信度不足 |
| AGENT_RUN_NOT_FOUND | 3103 | Agent 执行记录不存在 |
| AGENT_CONFIRMATION_INVALID | 3104 | 确认凭证无效 |
| AGENT_CONFIRMATION_EXPIRED | 3105 | 确认已超时 |
| AGENT_MAX_STEPS_EXCEEDED | 3106 | 超出最大步骤数 |
| AGENT_TIMEOUT | 3107 | Agent 执行超时 |
| AGENT_WRITE_OP_NOT_CONFIRMED | 3108 | 写操作未确认 |
| AGENT_HANDOFF_CREATE_FAIL | 3109 | 转人工工单创建失败 |
| AFTER_SALE_NOT_ELIGIBLE | 7101 | 订单不满足售后条件 |
| AFTER_SALE_APPLICATION_EXISTS | 7102 | 该订单已存在进行中的售后申请 |
| AFTER_SALE_CREATE_FAIL | 7103 | 售后申请创建失败 |
| AFTER_SALE_APPLICATION_NOT_FOUND | 7104 | 售后申请不存在 |
| AFTER_SALE_ACTION_INVALID | 7105 | 售后动作类型无效 |

## 7. 配置项（ai-cs-chat application 配置，aics.agent.*）

| 键 | 默认值 | 说明 |
|----|--------|------|
| aics.agent.max-steps | 12 | 单次 run 最大步骤数 |
| aics.agent.step-timeout-ms | 15000 | 单步超时 |
| aics.agent.total-timeout-ms | 60000 | run 总超时 |
| aics.agent.confirm-timeout-minutes | 10 | 确认超时（分钟） |
| aics.agent.intent-threshold | 0.6 | 意图置信度阈值 |
| aics.agent.price-tolerance | 0.15 | 同价位容差（±15%） |
| aics.agent.rule-knowledge-base | after-sale-rules | 售后规则知识库标识 |
| aics.agent.rule-top-k | 3 | 规则检索 TopK |
| aics.agent.rule-similarity-threshold | 0.7 | 规则检索相似度阈值 |
| aics.agent.sentiment-handoff | ANGRY | 触发转人工的情绪 |
| aics.agent.write-retry-times | 1 | 写操作失败重试次数 |
| aics.agent.llm-intent-enabled | true | 是否启用 LLM 意图识别（false 走规则兜底） |
