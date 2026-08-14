# 实施计划：智能客服 Agent 编排与人工转接

**分支**: `005-after-sales-agent` | **日期**: 2026-08-14 | **规格**: [spec.md](spec.md)
**输入**: 来自 `/specs/005-after-sales-agent/spec.md` 的功能规格

## 摘要

在现有 Spring Cloud 微服务（chat/order/product/message/notify）之上新增"售后 Agent 编排层"：

1. **ai-cs-chat**：新增 `com.aics.chat.agent` 包——输入安全检查（SafetyGuardService）、意图识别（IntentClassifierService，LLM 结构化输出 + 规则兜底，内置评估数据集核算 Macro-F1）、显式状态机（AfterSaleState + AgentStateMachine）、工具注册中心（AgentToolRegistry，写操作声明需确认）、确认服务（ConfirmationService，Token + 摘要 + 超时）、编排器（AfterSaleAgentService，多轮可恢复）、轨迹记录（AgentTraceRecorder，runId/step 摘要落库）、Agent REST 接口（/chat/agent、/chat/agent/confirm、/chat/agent/runs/{runId}）。
2. **ai-cs-order**：新增 `after_sale_application` 表 + 售后资格校验 + 换货/退货/退款申请命令（幂等键去重）。
3. **ai-cs-product**：新增同价位 + 属性关键词召回接口（/product/recommend/price-range），推荐解释基于真实商品字段。
4. **ai-cs-message**：新增 `agent_run` / `agent_step` / `agent_confirmation` / `handoff_ticket` 四张表与持久化接口（/api/agent/*）。
5. **ai-cs-notify**：新增转人工通知接口（POST /api/notify/handoff），复用 WebSocket 定向推送。
6. **ai-cs-common**：ResultCode 新增 AGENT_*（31xx）与 AFTER_SALE_*（71xx）错误码。
7. **deploy/mysql**：新增 `after-sales-agent-init.sql`（5 张新表 + 售后规则种子文档），并入 all-init.sql。

## 技术上下文

**语言/版本**：Java 17 / Maven（JDK 21 编译）
**主要依赖**：Spring Boot 3.2.5、Spring AI 1.1.4（chat 侧已有 ChatClient + @Tool 基础设施）、MyBatis-Plus 3.5.6（order/message）、OpenFeign（chat 侧跨服务调用）、Redis（chat 侧 run 状态存储）、Lombok、Hutool
**存储**：MySQL（业务表）、Redis（Agent run 多轮状态）、知识库 RAG（售后规则检索，复用 KnowledgeBaseService）
**测试**：JUnit 5 + Mockito（纯单测，chat 模块不加载 Spring 上下文）；order/message 模块 H2 + Embedded Redis 集成测试；JaCoCo 门禁（行 ≥ 40%、分支 ≥ 30%）
**目标平台**：Linux 服务器（微服务）
**项目类型**：Spring Cloud 微服务（多模块）
**性能目标**：工具调用 P95 < 500ms；轨迹读写 P95 < 200ms
**约束**：状态机自研轻量实现（不引入 Spring StateMachine 新依赖）；写操作未经确认零执行；幂等键唯一约束；敏感参数摘要化
**规模/范围**：5 个微服务模块 + 公共模块 + SQL 交付物，约 40 个新增/变更 Java 文件

## 宪法检查

- 第2条 SDD 流程：spec → clarify → plan → tasks → implement 顺序推进 ✔
- 第2-1条 TDD：每个实现任务先写失败测试（Red 证据）再实现（Green）再重构（Refactor）✔
- 第12条 文档中文、代码英文命名、日志英文、Conventional Commits 中文描述 ✔
- 第13-1条 SQL 交付物：新表统一输出 deploy/mysql/，幂等 SQL ✔
- 第16条 依赖方向：chat → order/product/message/notify 仅经 Feign；不产生循环依赖 ✔
- 第20条 编码规范：Result<T> / ResultCode / BusinessException / GlobalExceptionHandler / 构造器注入 / Lombok ✔
- 第20-1条 Spring AI：ChatClient 经 Builder 构建；@Tool 必须带 description；结构化输出 temperature ≤ 0.3 ✔

## 项目结构

### 文档（本功能）

```text
specs/005-after-sales-agent/
├── plan.md              # 本文件
├── research.md          # 现状调研（docs/15 三/五章 + 三模块探索报告）
├── data-model.md        # 数据模型（5 张新表）
├── quickstart.md        # 快速启动与演示脚本
├── contracts/           # 跨服务 REST/Feign 契约
├── checklists/          # 规格质量检查清单
└── tasks.md             # 任务列表（/speckit-tasks 输出）
```

### 源代码（仓库根目录）

```text
ai-cs-common/src/main/java/com/aics/common/result/ResultCode.java      # 新增 AGENT_*(31xx) / AFTER_SALE_*(71xx)
ai-cs-chat/src/main/java/com/aics/chat/agent/
├── AgentProperties.java                 # aics.agent.* 配置（步骤上限/超时/阈值/价格容差）
├── model/                               # AgentIntentType/IntentResult/SafetyCheckResult/PolicyCheckResult/AgentActionPlan/AgentTurnResult/SentimentType/...
├── safety/SafetyGuardService.java       # Prompt 注入/违规内容检测（规则库可配置）
├── intent/IntentClassifierService.java  # LLM 结构化输出 + 规则兜底 + 多意图拆分
├── intent/IntentEvalService.java        # 固定数据集 Macro-F1 评估（SC-001）
├── state/AfterSaleState.java            # 状态枚举
├── state/AgentStateMachine.java         # 迁移表 + 每状态工具集 + 终态判定
├── context/AfterSaleContext.java        # 单次 run 的上下文
├── store/AgentRunStore.java             # run 状态存取（接口）
├── store/RedisAgentRunStore.java        # Redis 实现（多轮/多实例）
├── store/InMemoryAgentRunStore.java     # 测试用内存实现
├── tool/AgentTool.java                  # 工具抽象（name/risk/requiresConfirmation/execute）
├── tool/AgentToolRegistry.java          # 工具注册中心
├── tool/OrderLocatorTool.java           # 订单定位（Feign，归属校验）
├── tool/PolicyCheckTool.java            # 售后规则 RAG 资格判断（带引用）
├── tool/ProductRecommendTool.java       # 同价位商品召回（Feign）
├── tool/CreateAfterSaleTool.java        # 写操作：创建售后申请（幂等键=runId）
├── tool/HandoffTool.java                # 转人工工单 + 通知
├── tool/RuleProvider.java               # 规则来源抽象（知识库/静态）
├── confirm/ConfirmationService.java     # 确认 Token 生成/校验/超时
├── trace/AgentTraceRecorder.java        # step 轨迹记录（摘要化）
├── workflow/AfterSaleAgentService.java  # 编排器：状态机驱动多轮执行
└── AgentController.java                 # POST /chat/agent、/chat/agent/confirm、/chat/agent/runs/{runId}
ai-cs-chat/src/main/java/com/aics/chat/feign/
├── AfterSaleFeignClient.java            # ai-cs-order 售后接口
├── ProductRecommendFeignClient.java     # ai-cs-product 推荐接口
├── AgentTraceFeignClient.java           # ai-cs-message 轨迹接口
└── NotifyFeignClient.java               # ai-cs-notify 转人工通知
ai-cs-order/src/main/java/com/aics/order/
├── entity/AfterSaleApplication.java     # after_sale_application
├── enums/AfterSaleActionType.java       # EXCHANGE/RETURN/REFUND
├── enums/AfterSaleStatus.java           # PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED
├── dto/AfterSaleApplyDTO.java  dto/EligibilityQueryDTO.java
├── vo/EligibilityVO.java  vo/AfterSaleApplyVO.java
├── service/AfterSaleService.java  service/impl/AfterSaleServiceImpl.java
├── controller/AfterSaleController.java  # /order/after-sale/*
└── mapper/AfterSaleApplicationMapper.java
ai-cs-product/src/main/java/com/aics/product/
├── dto/ProductRecommendQuery.java  vo/ProductRecommendVO.java
├── service/ProductRecommendService.java  service/impl/ProductRecommendServiceImpl.java
└── controller/ProductRecommendController.java  # GET /product/recommend/price-range
ai-cs-message/src/main/java/com/aics/message/
├── entity/AgentRun.java  AgentStep.java  AgentConfirmation.java  HandoffTicket.java
├── mapper/AgentRunMapper.java  AgentStepMapper.java  AgentConfirmationMapper.java  HandoffTicketMapper.java
├── service/AgentTraceService.java  service/impl/AgentTraceServiceImpl.java
├── controller/AgentTraceController.java  # /api/agent/*
└── dto/*.java  vo/*.java
ai-cs-notify/src/main/java/com/aics/notify/
├── dto/HandoffNoticeDTO.java
└── controller/NotifyController.java     # 新增 POST /api/notify/handoff
deploy/mysql/after-sales-agent-init.sql   # 5 张新表 + 售后规则种子文档
deploy/mysql/all-init.sql / order-init.sql # 并入新表
```

**结构决策**：所有跨服务调用沿用 chat 侧 Feign + X-User-Id 透传模式（与 OrderFeignClient 一致）；chat 侧 DTO 自持副本（与 OrderVO 模式一致）；状态机与确认服务为纯 Java 类（无 Spring 依赖），便于单测。

## 复杂度追踪

| 违规项 | 为何需要 | 拒绝更简单替代方案的原因 |
|--------|----------|--------------------------|
| 自研轻量状态机（而非直接调 LLM ReAct） | 售后写操作必须确定性可控 | 模型自由 ReAct 无法保证"写操作前必确认"的强约束与可回放审计 |
| Redis 持久化 run 状态（而非内存 Map） | 多轮确认跨请求、多实例共享 | 内存态在重启/多实例下丢失确认上下文，违反幂等与审计要求 |
| 规则判断走知识库 RAG + 关键词兜底（而非纯 LLM） | 规则回答必须可引用、可复核、无依据不编造 | 纯 LLM 结论不可追溯，无法满足验收"规则回答必须返回引用" |
