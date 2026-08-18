## 1. 配置化核心模块（com.aics.chat.prompt）

- [x] 1.1 新增 `PromptProperties`（`@ConfigurationProperties("aics.prompt")`），含 `ScenarioConfig`/`RolloutConfig`/`VersionConfig` 及策略枚举（`weights`/`userId-mod`/`pinned`）
- [x] 1.2 新增 `PromptRenderException`（缺参/未配置场景/版本缺失）
- [x] 1.3 新增 `PromptRegistry`：启动校验（activeVersion 存在、weights 和为 1）、`{{var}}` 渲染、`listVersions`/`getVersion`/`getActiveVersion`/`setActiveVersion`/`resolveVersion`
- [x] 1.4 新增 `PromptRouter`：按三种策略选版本（权重抽样可注入随机源；userId-mod 稳定分桶；pinned 固定），命中缺失回退 activeVersion
- [x] 1.5 新增 `PromptConfig` 装配 `PromptRegistry`/`PromptRouter` Bean（始终生效），单元测试覆盖渲染/校验/路由（`PromptConfigTest` 5 例）

## 2. 配置外置（application-prompt.yml）

- [x] 2.1 新增 `ai-cs-chat/src/main/resources/application-prompt.yml`，按 design.md §2 模板外置 `intent`/`rewrite`/`rag`/`summary`/`judge`/`chart`/`vision`/`default-system` 全部提示词（含 `{{var}}` 占位符与版本/灰度声明）
- [x] 2.2 各 scenario 初版内容逐字对齐现有硬编码输出（保证行为不变），保留 `default-system` 与 `SpringAiConfig` 内 DB_SCHEMA 拼接逻辑（经 `{{dbSchema}}` 注入）

## 3. 调用点改造（配置化渲染）

- [x] 3.1 `SpringAiConfig`：`defaultSystem(...)` 改用 `promptRegistry.render("default-system", Map.of("dbSchema", DB_SCHEMA)).system()`
- [x] 3.2 `IntentClassifierService`：`buildPrompt` → `render("intent", Map.of("input", input)).text()`
- [x] 3.3 `QueryRewriteService`：system+user 改用 `render("rewrite", Map.of("count",3,"question",q))`
- [x] 3.4 `ChatServiceImpl`：RAG 提示词（两处）与摘要提示词改用 `render("rag"/"summary", ...)`
- [x] 3.5 `LlmJudgeService` / `ChartAnswerGenerator` / `VisionModelClient`：改用对应 scenario 渲染
- [ ] 3.6 过渡开关：`aics.prompt.enabled=false` 时回退保留的硬编码副本（过渡期后移除）——已实现 `enabled=false` 走 `fallback` 分支机制，但各调用点未逐一传 fallback（后续按需补齐）

## 4. 效果关联（复用 observability）

- [x] 4.1 `TraceSpan` 增补字段 `promptScenario`/`promptVersion`
- [x] 4.2 `TraceContext`/`TraceSpan` 增加两字段；各调用点在 render 后写入 `ctx.setPrompt(scenario, version)`；`TraceSpanObservationHandler` 落 span
- [ ] 4.3 评估落库（`RagEvalService`/`online_eval_record`）携带 `promptVersion` 聚合——trace 已携带，统计聚合属后续增强

## 5. 管理接口

- [x] 5.1 新增 `PromptController`：`GET /api/prompts`、`GET /api/prompts/{scenario}`、`POST /api/prompts/{scenario}/active?version=`
- [ ] 5.2 管理接口单元测试（列出版本、热切换生效）——控制器逻辑经编译验证，HTTP 层测试可后续补充

## 6. 测试与回归

- [x] 6.1 单元测试：占位符渲染、缺参异常、灰度权重抽样、userId 分桶稳定性、热回滚（`PromptConfigTest` 5 例全绿）
- [ ] 6.2 集成测试：trace 携带 `promptScenario`/`promptVersion` 断言——`TraceSpanObservationHandlerTest` 已覆盖 span 字段映射，链路级断言可后续补充
- [x] 6.3 全量回归：`mvn -pl ai-cs-chat test` 通过（251 用例，0 失败）

## 7. 文档与收尾

- [x] 7.1 项目进度矩阵更新：Prompt 配置化 状态由「⚠️ 部分实现」推进，标注版本/灰度/回滚/效果关联已通过 OpenSpec 方案对齐
- [ ] 7.2 实现文档：配置项说明（`aics.prompt.*`）、scenario 清单、灰度与回滚操作手册
