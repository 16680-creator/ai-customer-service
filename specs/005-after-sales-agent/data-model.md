# 数据模型：智能客服 Agent 编排与人工转接

> 新表统一输出到 `deploy/mysql/after-sales-agent-init.sql`，并并入 `deploy/mysql/all-init.sql`（订单表并入 `order-init.sql`）。所有 SQL 幂等（IF NOT EXISTS / ON DUPLICATE KEY UPDATE）。

## 1. after_sale_application（售后申请表，订单库 ai_customer_service）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| application_no | VARCHAR(32) UNIQUE | 申请单号（AS+yyyyMMddHHmmss+4位随机） |
| run_id | VARCHAR(64) | Agent 执行 runId（来源可追溯） |
| idempotency_key | VARCHAR(64) UNIQUE | 幂等键（= Agent runId + 动作），重复提交返回首次结果 |
| user_id | BIGINT | 申请用户 |
| order_no | VARCHAR(32) | 关联订单号 |
| product_id | BIGINT | 商品 ID（整单售后可为空） |
| product_name | VARCHAR(200) | 商品名称快照 |
| quantity | INT | 售后数量 |
| action_type | VARCHAR(20) | 售后动作：EXCHANGE/RETURN/REFUND |
| reason | VARCHAR(512) | 售后原因 |
| evidence_summary | VARCHAR(1024) | 证据/规则引用摘要 |
| status | VARCHAR(20) | PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED |
| create_time / update_time | DATETIME | 时间戳 |

索引：uk_idempotency_key、uk_application_no、idx_user_id、idx_order_no、idx_status

## 2. agent_run（Agent 执行记录，对话库 chat_db）

| 字段 | 类型 | 说明 |
|------|------|------|
| run_id | VARCHAR(64) PK | 执行 ID（UUID） |
| session_id | BIGINT | 会话 ID |
| user_id | BIGINT | 用户 ID |
| intent | VARCHAR(64) | 识别意图（多意图逗号分隔） |
| sentiment | VARCHAR(20) | 情绪：POSITIVE/NEUTRAL/NEGATIVE/ANGRY |
| status | VARCHAR(20) | RUNNING/WAITING_CONFIRM/COMPLETED/CANCELLED/HANDOFF/FAILED |
| current_step | INT | 当前步骤号 |
| prompt_version | VARCHAR(32) | Prompt/规则版本 |
| error_summary | VARCHAR(1024) | 失败摘要 |
| create_time / update_time | DATETIME | 时间戳 |

## 3. agent_step（Agent 步骤轨迹，对话库 chat_db）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| run_id | VARCHAR(64) | 所属 run |
| step_no | INT | 步骤序号 |
| step_type | VARCHAR(32) | SAFETY/INTENT/LOCATE_ORDER/CHECK_POLICY/RECOMMEND/CONFIRM/EXECUTE/HANDOFF |
| tool_name | VARCHAR(64) | 工具名（无工具为空） |
| input_digest | VARCHAR(256) | 输入摘要（敏感字段 SHA-256 截断） |
| output_digest | VARCHAR(1024) | 输出摘要 |
| duration_ms | BIGINT | 耗时 |
| status | VARCHAR(20) | SUCCESS/FAILED/SKIPPED |
| error_summary | VARCHAR(1024) | 错误摘要 |
| create_time | DATETIME | 时间戳 |

索引：idx_run_id（run_id, step_no）

## 4. agent_confirmation（写操作确认记录，对话库 chat_db）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| run_id | VARCHAR(64) | 所属 run |
| action | VARCHAR(32) | 待确认动作：CREATE_EXCHANGE/CREATE_RETURN/CREATE_REFUND |
| payload_digest | VARCHAR(256) | 操作摘要（订单/商品/数量/原因）的 SHA-256 |
| status | VARCHAR(20) | PENDING/CONFIRMED/REJECTED/EXPIRED |
| confirmed_by | BIGINT | 确认人（用户 ID） |
| confirmed_at | DATETIME | 确认时间 |
| timeout_at | DATETIME | 确认超时时间 |
| create_time | DATETIME | 时间戳 |

索引：idx_run_id、uk_run_action（run_id, action）

## 5. handoff_ticket（转人工工单，对话库 chat_db）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| ticket_no | VARCHAR(32) UNIQUE | 工单号（HF+yyyyMMddHHmmss+4位随机） |
| run_id | VARCHAR(64) | 所属 run |
| session_id | BIGINT | 会话 ID |
| user_id | BIGINT | 用户 ID |
| reason | VARCHAR(32) | 触发原因：POLICY_NOT_MET/NEGATIVE_SENTIMENT/EXECUTION_FAILED/USER_REQUEST |
| priority | VARCHAR(16) | HIGH/NORMAL |
| order_no | VARCHAR(32) | 关联订单（可为空） |
| sentiment | VARCHAR(20) | 情绪 |
| problem_summary | VARCHAR(2048) | 问题摘要 |
| executed_steps | TEXT(JSON) | 已执行步骤清单（JSON 数组） |
| status | VARCHAR(20) | OPEN/ASSIGNED/CLOSED |
| assigned_agent | BIGINT | 分配坐席（MVP 为空） |
| create_time / update_time | DATETIME | 时间戳 |

索引：uk_ticket_no、idx_run_id、idx_user_id、idx_status

## 6. 售后规则种子文档（知识库 chat_db.kb_document，knowledge_base 标识 "after-sale-rules"）

以 kb_document 行形式交付 3 条规则（标题 ASR-001/002/003），内容含条款编号、适用动作、条件、期限、引用编号，供 PolicyCheckTool RAG 检索与集成测试使用：

- ASR-001 换货规则：签收后 15 天内质量问题可换货；非人为损坏；引用编号 ASR-001
- ASR-002 退货规则：7 天无理由退货；需商品完好；引用编号 ASR-002
- ASR-003 退款规则：退货完成后 3 个工作日内退款；引用编号 ASR-003

## 实体 ↔ 表映射（MyBatis-Plus）

| 模块 | 实体 | 表 |
|------|------|-----|
| ai-cs-order | AfterSaleApplication | after_sale_application |
| ai-cs-message | AgentRun / AgentStep / AgentConfirmation / HandoffTicket | agent_run / agent_step / agent_confirmation / handoff_ticket |
