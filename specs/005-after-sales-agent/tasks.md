# 任务：智能客服 Agent 编排与人工转接

**输入**: 来自 `/specs/005-after-sales-agent/` 的设计文档
**前置条件**: plan.md（必填）、spec.md（用户故事必填）、data-model.md、contracts/rest-api.md

**测试**: 采用 TDD 方式（宪法第2-1条），测试任务必选。每个实现任务先写测试并验证失败（Red），再实现使其通过（Green），再重构（Refactor）。

## 阶段 1：基础层（阻塞性前置条件）

**目的**：任何用户故事实施前必须完成的核心基础设施

- [ ] T001 在 `ai-cs-common/src/main/java/com/aics/common/result/ResultCode.java` 新增 AGENT_*(3101-3109) 与 AFTER_SALE_*(7101-7105) 错误码
- [ ] T002 编写 `deploy/mysql/after-sales-agent-init.sql`（5 张新表 + 售后规则种子文档，幂等 SQL），并入 `deploy/mysql/all-init.sql` 与 `deploy/mysql/order-init.sql`

**检查点**：基础层就绪（错误码与 SQL 先行，供各模块实现引用）

---

## 阶段 2：用户故事 - ai-cs-order 售后命令（P1）

**目标**：售后资格校验 + 换货/退货/退款申请创建（幂等）
**独立测试**：`AfterSaleServiceTest` 纯 Mockito 单测 + `AfterSaleControllerTest`；H2 schema-test.sql 新增 after_sale_application 表

### 测试（必选 - TDD Red 阶段）⚠️

- [ ] T003 `ai-cs-order/src/test/.../service/AfterSaleServiceTest.java`：归属校验（他人订单→拒绝）、状态校验（非 PAID→不满足）、重复申请（存在 PENDING→拒绝）、幂等键去重（重复提交返回首次结果）、创建成功返回申请单号、订单不存在
- [ ] T004 `ai-cs-order/src/test/.../controller/AfterSaleControllerTest.java`：eligibility/apply/list/detail 四个端点委托正确性与 Result 结构
- [ ] T005 `ai-cs-order/src/test/resources/schema-test.sql` 新增 after_sale_application 表（与 deploy SQL 一致）

### 实施

- [ ] T006 `ai-cs-order` 新建 `enums/AfterSaleActionType.java`、`enums/AfterSaleStatus.java`、`entity/AfterSaleApplication.java`
- [ ] T007 `ai-cs-order` 新建 `dto/AfterSaleApplyDTO.java`、`dto/EligibilityQueryDTO.java`、`vo/EligibilityVO.java`、`vo/AfterSaleApplyVO.java`、`mapper/AfterSaleApplicationMapper.java`
- [ ] T008 `ai-cs-order` 实现 `service/AfterSaleService.java` + `service/impl/AfterSaleServiceImpl.java`（资格校验、幂等创建、查询）
- [ ] T009 `ai-cs-order` 实现 `controller/AfterSaleController.java`（/order/after-sale/*，SpringDoc 注解齐全）
- [ ] T010 运行 ai-cs-order 全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：ai-cs-order 售后能力独立可用

---

## 阶段 3：用户故事 - ai-cs-product 商品推荐（P2）

**目标**：同价位 + 属性关键词召回，推荐解释基于真实字段
**独立测试**：`ProductRecommendServiceTest` 纯 Mockito 单测

### 测试（必选 - TDD Red 阶段）⚠️

- [ ] T011 `ai-cs-product/src/test/.../service/ProductRecommendServiceTest.java`：价格区间过滤、关键词过滤（描述/名称含"降噪"）、空结果如实返回、排序（关键词命中数→价格差→销量）、matchReason 字段全部来自真实商品字段、limit 与 tolerance 边界
- [ ] T012 `ai-cs-product/src/test/.../controller/ProductRecommendControllerTest.java`：参数校验与 Result 结构

### 实施

- [ ] T013 `ai-cs-product` 新建 `dto/ProductRecommendQuery.java`、`vo/ProductRecommendVO.java`
- [ ] T014 `ai-cs-product` 实现 `service/ProductRecommendService.java` + `service/impl/ProductRecommendServiceImpl.java`
- [ ] T015 `ai-cs-product` 实现 `controller/ProductRecommendController.java`（GET /product/recommend/price-range）
- [ ] T016 运行 ai-cs-product 全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：ai-cs-product 推荐能力独立可用

---

## 阶段 4：用户故事 - ai-cs-message Agent 轨迹（P1）

**目标**：agent_run/agent_step/agent_confirmation/handoff_ticket 四表持久化与查询
**独立测试**：`AgentTraceServiceTest` Mockito + H2 集成；H2 schema-test.sql 新增四表

### 测试（必选 - TDD Red 阶段）⚠️

- [ ] T017 `ai-cs-message/src/test/.../service/AgentTraceServiceTest.java`：创建 run、更新状态、追加 step（顺序号）、记录确认、创建工单（ticketNo 生成）、按 runId 查询 run+steps、重复 runId 幂等
- [ ] T018 `ai-cs-message/src/test/.../controller/AgentTraceControllerTest.java`：全部端点委托与 Result 结构
- [ ] T019 `ai-cs-message/src/test/resources/schema-test.sql` 新增四张表

### 实施

- [ ] T020 `ai-cs-message` 新建 `entity/AgentRun.java`、`AgentStep.java`、`AgentConfirmation.java`、`HandoffTicket.java` 与 4 个 Mapper
- [ ] T021 `ai-cs-message` 新建 dto/vo（AgentRunDTO/AgentStepDTO/AgentConfirmationDTO/HandoffTicketDTO/HandoffTicketVO/AgentRunDetailVO）
- [ ] T022 `ai-cs-message` 实现 `service/AgentTraceService.java` + impl
- [ ] T023 `ai-cs-message` 实现 `controller/AgentTraceController.java`（/api/agent/*）
- [ ] T024 运行 ai-cs-message 全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：ai-cs-message 轨迹能力独立可用

---

## 阶段 5：用户故事 - ai-cs-notify 转人工通知（P1）

**目标**：POST /api/notify/handoff 推送转人工事件
**独立测试**：NotifyControllerTest

- [ ] T025 `ai-cs-notify` 测试：handoff 端点校验参数、调用 NotifyWebSocketHandler 推送、Result 结构
- [ ] T026 `ai-cs-notify` 新建 `dto/HandoffNoticeDTO.java`，`controller/NotifyController.java` 新增 POST /api/notify/handoff
- [ ] T027 运行 ai-cs-notify 全部测试与覆盖率校验

**检查点**：ai-cs-notify 转人工通知可用

---

## 阶段 6：用户故事 - ai-cs-chat Agent 编排核心（P1）🎯 MVP

**目标**：安全检查、意图识别、状态机、工具编排、确认、转人工、轨迹
**独立测试**：纯 Mockito 单测（chat 模块测试不加载 Spring 上下文）

### 测试（必选 - TDD Red 阶段）⚠️

- [ ] T028 `AgentStateMachineTest`：合法/非法迁移、每状态工具集、终态判定、最大步骤
- [ ] T029 `SafetyGuardServiceTest`：注入样本（忽略指令/泄露提示词/越权工具）拦截、正常样本放行、BLOCK 后零工具调用
- [ ] T030 `IntentClassifierServiceTest`：LLM 输出 JSON 解析、多意图拆分、置信度阈值路由、规则兜底（LLM 失败时）
- [ ] T031 `IntentEvalServiceTest`：固定数据集 Macro-F1 >= 0.90（SC-001）
- [ ] T032 `ConfirmationServiceTest`：Token 生成/校验、超时过期、摘要一致性
- [ ] T033 `AgentToolRegistryTest`：注册/查找/写操作工具声明 requiresConfirmation
- [ ] T034 `AfterSaleAgentServiceTest`（核心）：完整链路（输入→意图→订单→规则→推荐→确认→执行→完成）；未确认零写操作；拒绝→CANCELLED；资格不满足→转人工（工单字段完整）；多候选订单→询问；无订单→引导；步骤超限→降级；幂等键=runId
- [ ] T035 `AgentControllerTest`：/chat/agent、/chat/agent/confirm、/chat/agent/runs/{runId} 委托与 Result 结构

### 实施

- [ ] T036 `ai-cs-chat` 新建 `agent/AgentProperties.java` + 配置绑定
- [ ] T037 `ai-cs-chat` 新建 `agent/model/`（AgentIntentType/AgentIntent/IntentResult/SentimentType/SafetyCheckResult/PolicyCheckResult/AgentActionPlan/AgentTurnResult 等 Record）
- [ ] T038 `ai-cs-chat` 实现 `agent/safety/SafetyGuardService.java`
- [ ] T039 `ai-cs-chat` 实现 `agent/intent/IntentClassifierService.java`（LLM 结构化输出 + 规则兜底）与 `IntentEvalService.java`
- [ ] T040 `ai-cs-chat` 实现 `agent/state/AfterSaleState.java` + `agent/state/AgentStateMachine.java`
- [ ] T041 `ai-cs-chat` 实现 `agent/store/AgentRunStore.java` + `RedisAgentRunStore` + `InMemoryAgentRunStore`
- [ ] T042 `ai-cs-chat` 实现 `agent/tool/`（AgentTool/AgentToolRegistry/ToolResult/OrderLocatorTool/PolicyCheckTool/ProductRecommendTool/CreateAfterSaleTool/HandoffTool/RuleProvider）
- [ ] T043 `ai-cs-chat` 实现 `agent/confirm/ConfirmationService.java`、`agent/trace/AgentTraceRecorder.java`、`agent/context/AfterSaleContext.java`
- [ ] T044 `ai-cs-chat` 实现 `agent/workflow/AfterSaleAgentService.java`（编排器）
- [ ] T045 `ai-cs-chat` 新建 feign：AfterSaleFeignClient / ProductRecommendFeignClient / AgentTraceFeignClient / NotifyFeignClient + chat 侧 DTO
- [ ] T046 `ai-cs-chat` 实现 `agent/AgentController.java`
- [ ] T047 运行 ai-cs-chat 全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：售后 Agent 全链路可用

---

## 阶段 7：优化与跨切面关注点

**目的**：SQL 交付物、文档、全量构建

- [ ] T048 `deploy/mysql/after-sales-agent-init.sql` 与 data-model.md 逐字段核对（Challenger 检查项）
- [ ] T049 更新 `docs/15-AI功能与技术缺口分析.md` 3.1 节状态（未实现 → 已实现）
- [ ] T050 更新 `specs/README.md` 功能列表新增 005
- [ ] T051 全量构建：`mvn clean install -DskipTests` + 各模块 `mvn verify`（含覆盖率门禁）
- [ ] T052 `speckit-analyze` 跨产物一致性检查并修复问题
- [ ] T053 更新 CLAUDE.md SPECKIT 标记为 005-after-sales-agent
- [ ] T054 按模块分批提交（test 先于实现），Conventional Commits 中文描述

---

## 依赖与执行顺序

### 阶段依赖

- **基础层（阶段 1）**：无依赖 - 可立即开始
- **阶段 2/4（order/message）**：依赖 T001 错误码
- **阶段 6（chat 核心）**：依赖阶段 2/3/4/5 的 Feign 契约确定（契约已在 contracts/rest-api.md 定义，可并行编码，联调在 T051 全量构建验证）
- **阶段 7**：依赖全部实现完成

### 并行机会

- 阶段 2、3、4、5 可并行（不同模块不同文件）
- chat 侧工具实现（T036-T046）依赖外部模块契约而非实现，可与阶段 2-5 并行
