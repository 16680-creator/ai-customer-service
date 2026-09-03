# 24 - 智能客服 Agent 编排与人工转接实现文档

> 适用范围：`ai-cs-chat` / `ai-cs-order` / `ai-cs-product` / `ai-cs-message` / `ai-cs-notify` / `ai-cs-common`
> 功能分支：`005-after-sales-agent`
> 完成日期：2026-08-14
> SDD 产物：[specs/005-after-sales-agent/](../../../specs/005-after-sales-agent/spec.md)（规格/计划/任务/契约/数据模型/快速启动）
> 需求来源：[08-AI功能与技术缺口分析.md](./08-AI功能与技术缺口分析.md) 第 3.1 节（P0）

---

## 一、功能概述

在已有 RAG、订单工具、消息、通知、用户与订单微服务之上，补齐**可靠的业务编排层**：

- 以**显式状态机**驱动售后流程，不依赖模型自由 ReAct，保证写操作强约束
- **Tool Calling** 复用订单、商品、用户、消息、通知服务（跨服务仅经 Feign）
- **Human-in-the-loop**：换货/退货/退款等写操作必须用户二次确认
- **幂等与补偿**：每次执行生成 `runId`，写操作携带幂等键
- 全链路**执行轨迹与审计**，转人工携带完整上下文

### 目标场景（验收走通）

> 用户输入："我昨天买的耳机坏了，想换货，另外帮我看看有没有同价位降噪更好的。"

系统自动完成：

```text
输入安全检查 → 意图识别（售后+商品推荐）→ 订单工具定位当前用户订单
→ 售后规则 RAG 判断换货资格 → 商品检索工具召回同价位降噪耳机
→ 风险判断要求用户确认 → 创建换货申请 → 生成结果摘要并留审计记录
```

### 验收指标与达成情况

| 验收指标 | 达成方式 | 状态 |
|---|---|---|
| 意图分类 Macro-F1 ≥ 0.90 | `IntentEvalService` 固定 24 样本数据集评估（`IntentEvalServiceTest` 断言） | ✅ 实测 1.00 |
| 工具参数正确率 ≥ 95% | 编排测试用 `ArgumentCaptor` 断言幂等键/订单号/动作/商品参数（T003/T034） | ✅ |
| 高风险写操作未经确认执行次数 = 0 | `never()` 断言 + 状态机写工具门禁（`AGENT_WRITE_OP_NOT_CONFIRMED`） | ✅ |
| 最大步骤数/超时/失败降级可配置 | `aics.agent.*` 全部可配，测试覆盖步骤超限/总超时/重试 | ✅ |
| 转人工携带订单、情绪、问题摘要、已执行步骤 | `HandoffTicketDTO` 字段断言 | ✅ |
| 完整执行链可按 runId 回放 | `/chat/agent/runs/{runId}` + `agent_run/agent_step` 表 | ✅ |
| 写操作幂等，重复提交不重复申请 | `idempotency_key` 唯一约束 + 命中返回首次结果 | ✅ |

---

## 二、总体架构

### 2.1 模块职责（对应需求文档 3.1 代码落点）

| 模块 | 职责 | 新增能力 |
|---|---|---|
| `ai-cs-chat`（8083） | Agent 路由、工作流状态机、工具注册、确认、回复生成、轨迹记录 | `com.aics.chat.agent` 编排层 + 4 个 Feign 客户端 |
| `ai-cs-order`（8087） | 售后资格校验、换货/退货/退款申请命令 | `after_sale_application` 表 + `/order/after-sale/*` |
| `ai-cs-product`（8088） | 同价位商品召回、推荐解释 | `/product/recommend/price-range` |
| `ai-cs-message`（8085） | Agent 执行轨迹与转人工工单持久化 | `agent_run/agent_step/agent_confirmation/handoff_ticket` 四表 + `/api/agent/*` |
| `ai-cs-notify`（8086） | 转人工事件、进度通知（WebSocket） | `/api/notify/handoff` |
| `ai-cs-common` | 错误码 | `AGENT_*`（3101-3110）、`AFTER_SALE_*`（7101-7105） |
| `deploy/mysql` | 数据库初始化 | `after-sales-agent-init.sql`（5 表 + 售后规则种子文档） |

### 2.2 依赖方向（遵守宪法第 16 条）

```text
ai-cs-chat ──Feign──▶ ai-cs-order / ai-cs-product / ai-cs-message / ai-cs-notify
     │
     └──▶ ai-cs-common（Result/ResultCode/BusinessException）
```

微服务间无直接依赖，全部经 OpenFeign + Nacos 服务发现；用户身份经网关 `X-User-Id` 透传。

### 2.3 数据流

```text
前端 ──POST /chat/agent──▶ AgentController ──▶ AfterSaleAgentService（状态机）
                                                     │ 每步记录
                                        AgentTraceRecorder ──Feign──▶ ai-cs-message（agent_run/step/confirmation）
                                                     │
                                        OrderLocatorTool ──Feign──▶ ai-cs-order /order/list（归属校验）
                                        PolicyCheckTool ──RAG──▶ 知识库 after-sale-rules（引用条款）
                                        ProductRecommendTool ──Feign──▶ ai-cs-product /recommend/price-range
                                        CreateAfterSaleTool ──Feign──▶ ai-cs-order /after-sale/apply（幂等键）
                                        HandoffTool ──Feign──▶ ai-cs-message 工单 + ai-cs-notify 通知
```

---

## 三、核心设计

### 3.1 显式状态机（`agent/state/`）

状态（`AfterSaleState`）：`START → CLASSIFY_INTENT → LOCATE_ORDER → CHECK_POLICY → COLLECT_EVIDENCE → CONFIRM_ACTION → EXECUTE_AFTER_SALE → COMPLETED`，分支：`HANDOFF / CANCELLED / FAILED`。

```text
START → CLASSIFY_INTENT → LOCATE_ORDER ──多候选──> 询问用户选择
                              ↓
                         CHECK_POLICY ──不满足──> HANDOFF（携带原因）
                              ↓
                       COLLECT_EVIDENCE ──缺原因──> 询问用户
                              ↓
                         CONFIRM_ACTION ──拒绝──> CANCELLED
                              ↓
                       EXECUTE_AFTER_SALE ──成功──> COMPLETED
                                   └─失败重试──> HANDOFF
```

- 迁移表集中管理（`AgentStateMachine` 纯 Java，无框架依赖），**非法迁移一律拒绝**
- 每状态绑定允许的工具集（如 `CONFIRM_ACTION` 状态禁止调用写工具），双门禁防越权
- 多轮对话状态经 `AgentRunStore` 持久化（Redis 实现跨实例共享，内存实现供测试）

### 3.2 输入安全检查（`agent/safety/SafetyGuardService`）

确定性正则规则检测 Prompt 注入与违规内容：忽略指令、泄露系统提示/知识库原文、越权绕过、越狱角色扮演、直接调用工具、超长输入（2000 字符）。**命中即拦截，零工具调用**（编排测试 `never()` 断言）。

### 3.3 意图识别（`agent/intent/IntentClassifierService`）

- **LLM 路径**：结构化 JSON 输出（意图类型/置信度/参数/情绪），经 `ResilientAiService`（Resilience4j 超时/重试/熔断）调用，10s 超时
- **置信度门禁**：低于 `aics.agent.intent-threshold`（默认 0.6）的意图剔除，全部低于阈值按普通对话路由
- **规则兜底**：LLM 不可用/输出不可解析时降级确定性规则分类器（售后/推荐/转人工/普通四类 + 情绪识别），保证任何情况下可路由
- **评估**：`IntentEvalService` 内置 24 样本固定数据集计算 Macro-F1（SC-001）

### 3.4 售后规则 RAG（`agent/tool/PolicyCheckTool` + `RuleProvider`）

- 从知识库 `after-sale-rules` 检索规则文档（种子：ASR-001 换货 15 天 / ASR-002 退货 7 天 / ASR-003 退款 3 个工作日）
- 解析条款编号与期限，结合订单时间判定资格，**结论必须携带规则引用**，证据不足不编造
- 检索失败/未命中降级静态规则（`StaticRuleProvider`），保证规则判断不中断

### 3.5 Human-in-the-loop 确认（`agent/confirm/ConfirmationService`）

- 写操作前生成确认凭证（Token）：凭证与**操作摘要的 SHA-256**绑定，防止内容篡改
- 确认时校验：Token 有效 + 未超时（`aics.agent.confirm-timeout-minutes`，默认 10 分钟）+ 摘要与当前操作一致
- 用户回复「确认」执行、「拒绝」取消；确认/拒绝记录 `agent_confirmation` 表（PENDING/CONFIRMED/REJECTED/EXPIRED）

### 3.6 幂等与补偿（`CreateAfterSaleTool` + 订单侧唯一键）

- 幂等键 = `runId + ":" + actionType`，订单侧 `uk_idempotency_key` 唯一约束
- 重复提交返回首次结果（不查订单、不重复 insert，测试 `verify(insert, never())` 锁定）
- 写操作失败按 `aics.agent.write-retry-times`（默认 1）重试，仍失败转人工（EXECUTION_FAILED）

### 3.7 转人工（`agent/tool/HandoffTool`）

四种触发条件：

| 触发 | 原因码 | 优先级 |
|---|---|---|
| 规则不满足 | `POLICY_NOT_MET` | NORMAL |
| 情绪愤怒（ANGRY） | `NEGATIVE_SENTIMENT` | HIGH |
| 执行失败（重试后） | `EXECUTION_FAILED` | NORMAL |
| 用户主动要求 | `USER_REQUEST` | NORMAL |

工单携带：**订单号、情绪、问题摘要、已执行步骤清单（JSON）**，落 `handoff_ticket` 表并经 `ai-cs-notify` WebSocket 推送坐席端（`event: HANDOFF`）。

### 3.8 轨迹审计（`agent/trace/AgentTraceRecorder`）

- 每次执行：`agent_run`（runId/意图/情绪/状态/步骤数）+ `agent_step`（步骤类型/工具/输入输出摘要/耗时/状态）
- 敏感参数**摘要化**：输入一律 SHA-256 截断（64 字符），明文不进日志与轨迹
- 持久化失败仅告警不阻断业务（审计尽力而为，业务强一致）
- 按 `runId` 回放：`GET /chat/agent/runs/{runId}`

### 3.9 同价位商品推荐（ai-cs-product）

- `GET /product/recommend/price-range?basePrice=&priceTolerance=&categoryId=&keywords=&limit=`
- 逻辑：价格区间 `[base×(1−t), base×(1+t)]` + 上架状态 + 类目过滤 + 关键词全命中（名称/描述）+ 排序（命中数→价格差→销量）+ limit 截断
- **推荐解释（matchReason）仅由真实字段拼接**（价格/名称/描述/销量），禁止模型编造（规格验收）

### 3.10 失败降级

| 场景 | 行为 |
|---|---|
| LLM 意图识别失败 | 降级规则分类器 |
| 规则检索失败 | 降级静态规则 |
| 步骤数超限 / 总超时 | 中止并解释（`AGENT_MAX_STEPS_EXCEEDED` / `AGENT_TIMEOUT`），可配置 |
| 写操作失败重试耗尽 | 转人工（EXECUTION_FAILED） |
| 确认超时/凭证失效 | 拒绝执行并提示重新发起（`AGENT_CONFIRMATION_EXPIRED`） |
| 非售后意图 | 路由回普通对话（`chatService.chat`） |

---

## 四、接口契约（摘要）

| 模块 | 端点 | 说明 |
|---|---|---|
| ai-cs-chat | `POST /chat/agent` | Agent 多轮对话（新 run 或续跑），返回回复/状态/确认凭证 |
| ai-cs-chat | `POST /chat/agent/confirm` | 写操作确认（CONFIRM/REJECT），body 含 runId + token |
| ai-cs-chat | `GET /chat/agent/runs/{runId}` | 执行轨迹回放（透传 ai-cs-message） |
| ai-cs-order | `POST /order/after-sale/eligibility` | 资格校验（归属/状态/重复申请） |
| ai-cs-order | `POST /order/after-sale/apply` | 创建售后申请（幂等，写操作） |
| ai-cs-order | `GET /order/after-sale/list`、`GET /order/after-sale/{applicationNo}` | 申请查询（本人） |
| ai-cs-product | `GET /product/recommend/price-range` | 同价位商品召回 |
| ai-cs-message | `POST /api/agent/runs`、`PUT /api/agent/runs/{runId}/status`、`POST /api/agent/runs/{runId}/steps`、`POST /api/agent/runs/{runId}/confirmations`、`POST /api/agent/handoff-tickets`、`GET /api/agent/runs/{runId}` | 轨迹/工单持久化与查询 |
| ai-cs-notify | `POST /api/notify/handoff` | 转人工事件通知 |

完整契约（含字段/错误码/配置项）见 [specs/005-after-sales-agent/contracts/rest-api.md](../../../specs/005-after-sales-agent/contracts/rest-api.md)。

---

## 五、数据模型（新增 5 张表）

| 表 | 库 | 关键字段 |
|---|---|---|
| `after_sale_application` | ai_customer_service | application_no(UNIQUE)、run_id、**idempotency_key(UNIQUE)**、user_id、order_no、product_id/name、quantity、action_type、reason、evidence_summary、status |
| `agent_run` | chat_db | run_id(PK)、session_id、user_id、intent、sentiment、status、current_step、prompt_version、error_summary |
| `agent_step` | chat_db | run_id、step_no（UNIQUE run_id+step_no）、step_type、tool_name、input_digest、output_digest、duration_ms、status、error_summary |
| `agent_confirmation` | chat_db | run_id、action（UNIQUE run_id+action）、payload_digest、status、confirmed_by、confirmed_at、timeout_at |
| `handoff_ticket` | chat_db | ticket_no(UNIQUE)、run_id、session_id、user_id、reason、priority、order_no、sentiment、problem_summary、executed_steps(JSON)、status、assigned_agent |

另：`kb_document` 种子售后规则文档 3 条（ASR-001/002/003，knowledge_base 标识 `after-sale-rules`）。
SQL 交付：[deploy/mysql/after-sales-agent-init.sql](../../../deploy/mysql/after-sales-agent-init.sql)（幂等，已并入 `all-init.sql` / `order-init.sql`）。

---

## 六、配置项（ai-cs-chat，前缀 `aics.agent.*`）

| 键 | 默认值 | 说明 |
|---|---|---|
| `aics.agent.max-steps` | 12 | 单次 run 最大步骤数 |
| `aics.agent.step-timeout-ms` | 15000 | 单步超时 |
| `aics.agent.total-timeout-ms` | 60000 | run 总超时 |
| `aics.agent.confirm-timeout-minutes` | 10 | 确认超时（分钟） |
| `aics.agent.intent-threshold` | 0.6 | 意图置信度阈值 |
| `aics.agent.price-tolerance` | 0.15 | 同价位容差（±15%） |
| `aics.agent.rule-knowledge-base` | after-sale-rules | 售后规则知识库标识 |
| `aics.agent.rule-top-k` / `rule-similarity-threshold` | 3 / 0.7 | 规则检索参数 |
| `aics.agent.sentiment-handoff` | ANGRY | 触发转人工的情绪 |
| `aics.agent.write-retry-times` | 1 | 写操作失败重试次数 |
| `aics.agent.llm-intent-enabled` | true | 是否启用 LLM 意图识别 |
| `aics.agent.recommend-limit` | 3 | 推荐数量上限 |

---

## 七、测试与验证结果

### 7.1 测试规模（全部 TDD：Red 编译失败 → Green 通过）

| 模块 | 新增测试 | 结果 |
|---|---|---|
| ai-cs-chat | 50（状态机/安全/意图/评估/确认/注册中心/编排/Controller） | ✅ 全模块 124/124 |
| ai-cs-order | 21（Service 16 + Controller 5） | ✅ 新增全绿 |
| ai-cs-product | 18（Service 14 + Controller 4） | ✅ 新增全绿 |
| ai-cs-message | 19（Service 12 + Controller 7） | ✅ 全模块 19/19 |
| ai-cs-notify | 26（含存量补测） | ✅ 全模块 26/26 |
| 全仓编译 | `mvn clean install -DskipTests` | ✅ 通过 |

### 7.2 覆盖率门禁（JaCoCo：行 ≥40%、分支 ≥30%）

| 模块 | 行 | 分支 | 结论 |
|---|---|---|---|
| ai-cs-message | 64.0% | 71.9% | ✅ 通过 |
| ai-cs-notify | 94.9% | 73.1% | ✅ 通过 |
| ai-cs-order | 71.5% | 6.5% | ⚠️ 分支门禁为存量问题（实体 Lombok 分支未覆盖）；新增类行覆盖 95.5% |
| ai-cs-product | 74.8% | 14.0% | ⚠️ 同上（存量实体分支）；新增类分支覆盖 97.6% |

### 7.3 关键行为测试（验收对应）

- 完整链路：输入 → 意图 → 订单 → 规则 → 确认 → 创建申请（幂等键断言）→ 摘要
- 未确认零写操作（`verify(apply, never())`）；拒绝 → CANCELLED
- 资格不满足/情绪愤怒/执行失败/用户要求 → 四种转人工，工单字段完整断言
- 多候选订单询问、无订单引导、注入拦截零工具调用、步骤超限、总超时、确认超时
- 意图评估 Macro-F1 ≥ 0.90（24 样本实测 1.00）

---

## 八、部署与演示

### 8.1 数据库初始化

```bash
mysql -uroot -p < deploy/mysql/after-sales-agent-init.sql   # 5 张新表 + 售后规则种子
# 或全量：mysql -uroot -p < deploy/mysql/all-init.sql
```

### 8.2 启动服务

```bash
mvn clean install -DskipTests
mvn -pl ai-cs-order spring-boot:run    # 8087（售后命令）
mvn -pl ai-cs-product spring-boot:run  # 8088（商品推荐）
mvn -pl ai-cs-message spring-boot:run  # 8085（轨迹）
mvn -pl ai-cs-notify spring-boot:run   # 8086（转人工通知）
mvn -pl ai-cs-chat spring-boot:run     # 8083（Agent 编排入口）
```

### 8.3 演示目标场景

```bash
# 1) 发起售后 + 推荐（返回 runId + confirmationToken + 操作摘要）
curl -X POST http://localhost:8080/chat/agent \
  -H "X-User-Id: 1" -H "Content-Type: application/json" \
  -d '{"sessionId":1,"input":"我昨天买的耳机坏了，想换货，另外帮我看看有没有同价位降噪更好的"}'

# 2) 确认换货（Human-in-the-loop）
curl -X POST http://localhost:8080/chat/agent/confirm \
  -H "X-User-Id: 1" -H "Content-Type: application/json" \
  -d '{"runId":"<runId>","token":"<confirmationToken>","decision":"CONFIRM","sessionId":1}'

# 3) 审计回放
curl http://localhost:8080/chat/agent/runs/<runId> -H "X-User-Id: 1"
```

> 演示前提：当前用户（X-User-Id）存在 PAID 订单；演示直接访问各服务端口或经网关 8080。

---

## 九、已知限制与后续迭代

| 项目 | 现状 | 后续建议 |
|---|---|---|
| 坐席工作台 | MVP 仅工单 + WebSocket 通知事件，无坐席端 UI | 实现坐席工作台，接入 `chat_session.status=2` 状态位与 `agent_id` 指派 |
| message/notify 用户隔离 | 轨迹/工单接口未读 `X-User-Id`（内部 Feign 调用） | 3.2 安全网关迭代对齐透传与资源级授权 |
| 工具参数 Golden 集评估 | SC-002 由断言覆盖，无独立"正确率"统计器 | 参考 `IntentEvalService` 增加工具参数评估数据集 |
| 输出侧 Guardrail | 仅输入侧安全检查 | 3.2 增加输出审核链 |
| 规则引擎 | 规则以知识库文档维护 | 后台规则管理 + 版本化 |
| 情绪模型 | LLM 结构化输出（ANGRY 触发转人工） | 独立情绪模型 + 阈值调优 |
| 存量门禁问题 | order `CartServiceTest` 1F+2E、order/product 分支覆盖率不达标（分支既有问题） | 单独立项修复 |

---

## 十、相关文档

- 需求来源：[08-AI功能与技术缺口分析.md](./08-AI功能与技术缺口分析.md)（3.1 / 五 / 六节）
- 功能规格：[specs/005-after-sales-agent/spec.md](../../../specs/005-after-sales-agent/spec.md)
- 实施计划：[specs/005-after-sales-agent/plan.md](../../../specs/005-after-sales-agent/plan.md)
- 任务清单：[specs/005-after-sales-agent/tasks.md](../../../specs/005-after-sales-agent/tasks.md)
- 接口契约：[specs/005-after-sales-agent/contracts/rest-api.md](../../../specs/005-after-sales-agent/contracts/rest-api.md)
- 数据模型：[specs/005-after-sales-agent/data-model.md](../../../specs/005-after-sales-agent/data-model.md)
- 快速启动：[specs/005-after-sales-agent/quickstart.md](../../../specs/005-after-sales-agent/quickstart.md)
- 调研报告：[specs/005-after-sales-agent/research.md](../../../specs/005-after-sales-agent/research.md)
