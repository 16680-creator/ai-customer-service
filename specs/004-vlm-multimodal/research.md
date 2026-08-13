# 技术调研与决策：多模态图生文（VLM）

> 第 0 阶段输出（/speckit-plan），解决 spec 中所有 NEEDS CLARIFICATION 与技术选型。

## 决策总览

| # | 决策点 | 结论 | 理由 |
|---|--------|------|------|
| D1 | 视觉模型选型 | 硅基流动 `Qwen/Qwen2.5-VL-72B-Instruct`，OpenAI 兼容协议，复用 Spring AI `OpenAiChatModel` | DeepSeek 不支持视觉；硅基流动已聚合该多模态模型且协议与现有 DeepSeek 对接方式完全一致；零新增 SDK，符合宪法第17条"技术优先" |
| D2 | 架构形态 | 两段式：VLM 负责"看懂图"输出文本描述 → 描述文本走现有 RAG（Hybrid/改写/GraphRAG）+ DeepSeek 生成回答 | DeepSeek 无视觉，"看图"与"回答"必须分属两个模型；最大化复用 003 已落地的 RAG 能力，避免重复建设 |
| D3 | 视觉模型 Bean 装配 | 在 `SpringAiConfig` 手动装配 `visionChatModel`（`OpenAiChatModel`），`@Qualifier("visionChatModel")` 区分，配置走 Nacos `aics.vision.*` | 与现有 embedding 模型手动装配同理；Spring Boot 自动装配只生成一个 ChatModel，第二个模型需手动装配 |
| D4 | 多模态消息构造 | Spring AI `UserMessage(text, List<Media>)`，图片 URL 作为 `Media` 传入 | Spring AI 1.1.4 原生支持，`OpenAiChatModel` 自动将 Media 转为 OpenAI `image_url` content part，无需手写协议 |
| D5 | 图片上传方式 | chat 服务新增 `POST /chat/upload-image`，复用 `ai-cs-common` 的 `FileStorageService`（MinIO），目录 `chat/images` | 图片对话是 chat 的能力，上传入口应落在 chat 自治；存储复用 common 公共能力，符合宪法第13-2条"复用公共能力" |
| D6 | 视觉不可用降级 | 有文字 → 退化为纯文本对话；仅图片 → 返回明确提示（`ResultCode` 新增码位），不抛 5xx | spec FR-005/FR-006；视觉是"增强项"，不可用时核心对话不受影响 |
| D7 | 商品图描述实现 | product 服务独立装配视觉 bean + `SiliconFlowImageDescriptionService` 替换 `NoopImageDescriptionService`；视觉配置走 Nacos 共享（`aics.vision.*`） | product 不应依赖 chat 内部接口（宪法第16条禁止跨模块直接依赖）；共享的是"硅基流动视觉模型"这一外部依赖，各自装配 Bean 不构成重复实现 |
| D8 | 图片安全 | SSRF 白名单校验（仅允许 MinIO 内网地址/白名单域名）+ 视觉结果 PII 脱敏 + 敏感词过滤 | 图片 URL 与视觉结果均是外部输入，需防 SSRF 与敏感信息泄漏；MVP 先做文本侧过滤，图片违规识别留后续 |
| D9 | 测试策略 | 视觉 LLM 调用属 AI 端到端行为，不纳入单元覆盖率强制范围（宪法第2-1条）；单元测试聚焦确定性逻辑（降级分支、请求校验、SSRF 校验、描述编排） | 视觉模型不可在单测中真实调用；确定性逻辑用 Mock 覆盖 |
| D10 | 数据落库 | product 表新增 `image_description` 字段，SQL 幂等迁移输出到 `deploy/mysql/vlm-multimodal-init.sql` | spec FR-008 要求"写入商品可检索字段"；该字段供 ES 检索与后续向量化使用 |

## 需要澄清项（已用默认决策解决）

| 澄清点 | 默认决策 | 影响 |
|--------|---------|------|
| 视觉模型供应商 | 硅基流动 Qwen2.5-VL | 复用现有硅基流动账号，无需新开户 |
| 单图 vs 多图 | 单图起步，多图后续扩展 | Media 列表天然支持多图，不阻塞 MVP |
| 图片大小/格式 | 与商品图一致（jpg/png/webp/gif，≤5MB） | 复用 product 服务的校验思路 |
| 商品图描述是否落库 | 落库 `image_description` 字段 | 供 ES/向量检索，长期价值 |
| 图片违规检测 | MVP 先做文本侧敏感词过滤，图片违规识别留后续 | 避免引入第三方审核依赖 |

## 架构约束确认

- 不新增微服务模块；能力落在 ai-cs-chat / ai-cs-product / ai-cs-frontend / ai-cs-common
- chat 与 product 各自装配视觉模型 Bean，共享 Nacos `aics.vision.*` 配置，不互相依赖
- 图片上传复用 ai-cs-common `FileStorageService`（MinIO），不重复造轮子
- 所有视觉 LLM 调用复用弹性治理（Resilience4j），禁止裸调
- 新增代码遵循 TDD：先写失败测试（Red）→ 最小实现（Green）→ 重构（Refactor）
