# 调研报告：智能客服 Agent 编排与人工转接

> 来源：docs/15-AI功能与技术缺口分析.md（3.1 节、5 节）+ 2026-08-14 三路代码探索报告。

## 现状盘点（事实）

| 能力 | 现状 | 本功能动作 |
|------|------|-----------|
| Tool Calling | ✅ ai-cs-chat 已有 @Tool + MethodToolCallbackProvider，工具：订单查询 ×2、NL2SQL ×1 | 新增 Agent 工具注册中心，写操作工具声明风险等级 |
| 订单查询 | ✅ OrderFeignClient（/order/list、/order/{orderNo}，X-User-Id 透传，归属校验在订单侧） | 复用为 OrderLocatorTool |
| 售后能力 | ❌ 无售后实体/表/接口；仅支付回调整单退款（refundConfirm） | ai-cs-order 新增 after_sale_application + 资格校验 + 申请命令 |
| 商品筛选 | ⚠️ 仅 keyword+categoryId+status，无价格区间/属性筛选；有向量召回 | ai-cs-product 新增同价位+关键词召回接口 |
| 会话转人工 | ⚠️ chat_session.status=2 与 agent_id 字段已预留但无代码 | handoff_ticket 工单 + 通知事件（不改 chat_session 状态位，避免破坏现有链路） |
| 消息持久化 | ✅ RocketMQ 异步链路 + Feign 查询 | 新增 agent_run/step/confirmation/handoff_ticket 四表（同库） |
| 通知推送 | ✅ NotifyWebSocketHandler.sendMessageToUser 定向推送 | 新增 /api/notify/handoff 端点 |
| 用户身份 | ✅ X-User-Id 请求头（网关透传）+ ChatUserContext（chat 侧 ThreadLocal） | 沿用 |
| 模型调用容错 | ✅ ResilientAiService（Resilience4j：timelimiter/retry/circuitbreaker） | 意图识别/情绪走该入口；步骤级失败走状态机降级 |
| 状态机 | ❌ 无 | 自研轻量状态机（AfterSaleState + AgentStateMachine），不引入新依赖 |

## 关键技术决策

1. **显式状态机而非 ReAct**：写操作强约束（未确认零执行）与可回放审计只能靠显式状态。
2. **多轮恢复**：run 状态存 Redis（RedisAgentRunStore），确认跨请求、多实例一致；测试用 InMemory 实现。
3. **规则判断走 RAG + 关键词兜底**：PolicyCheckTool 检索"after-sale-rules"知识库，规则文本含条款编号；种子文档随 SQL 交付；RuleProvider 抽象使测试可注入静态规则。
4. **意图识别 LLM + 规则兜底**：LLM 结构化输出（JSON），失败降级规则分类器；内置评估数据集计算 Macro-F1（SC-001 可验证）。
5. **幂等**：写操作幂等键 = runId + action，订单侧 unique(idempotency_key) 保证重复提交返回首次结果。
6. **确认**：ConfirmationService 生成 token（含 payload 摘要与超时），确认接口校验 token 与摘要一致性。
7. **轨迹审计**：AgentTraceRecorder 记录 step 级摘要（敏感字段 SHA-256 截断），经 Feign 落 ai-cs-message。
8. **推荐解释**：matchReason 由商品真实字段（名称/价格/描述/销量）拼接，杜绝模型编造。

## 风险与对策

| 风险 | 对策 |
|------|------|
| LLM 输出不稳定导致测试 flaky | 意图/情绪/总结全部有规则兜底路径；核心状态机与编排为确定性代码，单测不依赖 LLM |
| 跨服务联调环境不完整 | 各模块契约先行（contracts/rest-api.md），单测 Mock Feign；全量构建验证编译与测试 |
| 状态机与确认遗漏场景 | 状态迁移表集中管理 + 覆盖测试（含非法迁移拒绝） |
