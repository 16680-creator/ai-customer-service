# 任务：多模态图生文（VLM）

**输入**: 来自 `/specs/004-vlm-multimodal/` 的设计文档
**前置条件**: plan.md、spec.md、research.md、data-model.md、contracts/rest-api.md、quickstart.md

**测试**: 采用 TDD 方式（宪法第2-1条），测试任务必选。每个用户故事先写测试并验证失败（Red），再实现使其通过（Green），再重构（Refactor）。视觉 LLM 端到端行为纳入集成测试（Mock 视觉模型），不强制单元覆盖率。

**组织方式**: 任务按用户故事分组，以支持每个故事的独立实施和测试。

## 格式：`[ID] [P?] [故事] 描述`

- **[P]**：可并行执行（不同文件，无依赖）
- **[故事]**：任务所属的用户故事（US1/US2/US3）
- 描述中包含确切的文件路径

## 路径约定

- 后端微服务：`ai-cs-chat/src/main/java/com/aics/chat/`、`ai-cs-product/src/main/java/com/aics/product/`、`ai-cs-common/src/main/java/com/aics/common/`
- 测试：`ai-cs-chat/src/test/java/com/aics/chat/`、`ai-cs-product/src/test/java/com/aics/product/`
- 前端：`ai-cs-frontend/src/`
- SQL：`deploy/mysql/`

---

## 阶段 1：初始化（共享基础设施）

**目的**：配置与数据模型基础

- [X] T001 [P] 新增视觉配置属性类 `VisionProperties`（`@ConfigurationProperties(prefix="aics.vision")`，字段 baseUrl/apiKey/model/enabled/timeout/allowedImageHost）在 `ai-cs-chat/src/main/java/com/aics/chat/config/VisionProperties.java`
- [X] T002 [P] 新增 SQL 迁移文件（product 表幂等加 `image_description VARCHAR(1024) NULL` 列）在 `deploy/mysql/vlm-multimodal-init.sql`
- [X] T003 在 `ai-cs-chat/src/main/resources/application.yml` 补充 `aics.vision.*` 配置项注释与默认值（base-url/api-key/model/enabled/timeout/allowed-image-host）

---

## 阶段 2：基础层（阻塞性前置条件）

**目的**：任何用户故事实施前必须完成的核心基础设施

**⚠️ 关键**：此阶段完成前，不得开始任何用户故事的工作

- [X] T004 在 `SpringAiConfig.java` 手动装配视觉模型 Bean `visionChatModel`（`OpenAiChatModel`，`@Qualifier("visionChatModel")`，读 aics.vision.*），并 `@EnableConfigurationProperties(VisionProperties.class)` 在 `ai-cs-chat/src/main/java/com/aics/chat/config/SpringAiConfig.java`
- [X] T005 [P] 新增图片对话请求 DTO `VisionChatRequest`（sessionId/imageUrl/message/hybrid/rewrite）在 `ai-cs-chat/src/main/java/com/aics/chat/dto/VisionChatRequest.java`
- [X] T006 [P] 新增图片对话响应 DTO `VisionChatResponse`（answer/citations/imageDescription/degraded）在 `ai-cs-chat/src/main/java/com/aics/chat/dto/VisionChatResponse.java`
- [X] T007 [P] 新增图片 URL 安全校验器 `ImageUrlValidator`（SSRF 白名单 + http/https + 格式校验）在 `ai-cs-chat/src/main/java/com/aics/chat/util/ImageUrlValidator.java`
- [X] T008 [P] 在 `ResultCode` 枚举新增图片相关错误码（`VISION_SERVICE_UNAVAILABLE`、`IMAGE_URL_INVALID`）在 `ai-cs-common/src/main/java/com/aics/common/result/ResultCode.java`
- [X] T009 在 `Resilience4jConfig` 与 `application.yml` 新增视觉调用弹性配置（`visionService` timeLimiter/retry 实例，超时 5s）在 `ai-cs-chat/src/main/java/com/aics/chat/config/Resilience4jConfig.java`
- [X] T010 编写基础层单元测试：`VisionPropertiesTest`（配置绑定默认值）、`ImageUrlValidatorTest`（白名单命中/非法 URL/非 http 拒绝），运行确认 Red→Green 在 `ai-cs-chat/src/test/java/com/aics/chat/config/VisionPropertiesTest.java`、`ai-cs-chat/src/test/java/com/aics/chat/util/ImageUrlValidatorTest.java`

**检查点**：基础层就绪 - 可以开始并行实施用户故事

---

## 阶段 3：用户故事 1 - 对话中上传图片提问（优先级：P1）🎯 MVP

**目标**：用户上传图片，视觉模型理解后结合 RAG 生成回答，支持 SSE 流式

**独立测试**：调用 `/chat/vision` 传图片 URL，返回 answer + citations + imageDescription；回答体现对图片关键信息的理解

### 用户故事 1 的测试（必选 - TDD Red 阶段）⚠️

> **注意：先编写这些测试，确保它们在实施前失败**

- [X] T011 [P] [US1] 编写 `VisionChatServiceImplTest`：Mock 视觉模型返回固定描述，校验两段式编排（视觉理解 → RAG 检索 → LLM 回答）与引用溯源在 `ai-cs-chat/src/test/java/com/aics/chat/service/VisionChatServiceImplTest.java`
- [X] T012 [P] [US1] 编写 `ChatControllerVisionTest`：`/chat/vision` 参数校验（imageUrl 空/非法）与返回结构在 `ai-cs-chat/src/test/java/com/aics/chat/controller/ChatControllerVisionTest.java`

### 用户故事 1 的实施

- [X] T013 [US1] 实现 `VisionChatService` 接口（chatWithVision / chatWithVisionSse）在 `ai-cs-chat/src/main/java/com/aics/chat/service/VisionChatService.java`
- [X] T014 [US1] 实现 `VisionChatServiceImpl`：视觉理解（visionChatModel + Media 多模态消息）→ 描述文本走 RAG 检索 → DeepSeek 生成回答，复用现有 `ResilientAiService` 在 `ai-cs-chat/src/main/java/com/aics/chat/service/impl/VisionChatServiceImpl.java`（依赖 T011）
- [X] T015 [US1] `ChatController` 新增 `/chat/upload-image`（复用 `FileStorageService`，目录 `chat/images`，校验格式/大小）在 `ai-cs-chat/src/main/java/com/aics/chat/controller/ChatController.java`
- [X] T016 [US1] `ChatController` 新增 `/chat/vision`（图片对话，含 imageUrl 白名单校验）在 `ai-cs-chat/src/main/java/com/aics/chat/controller/ChatController.java`
- [X] T017 [US1] `ChatController` 新增 `/chat/vision/sse`（SSE 逐 token 推送）在 `ai-cs-chat/src/main/java/com/aics/chat/controller/ChatController.java`
- [X] T018 [P] [US1] 前端 `ChatView.vue` 新增图片上传按钮 + 消息气泡图片渲染，`api/index.js` 新增 upload-image/vision/vision-sse 接口封装在 `ai-cs-frontend/src/views/ChatView.vue`、`ai-cs-frontend/src/api/index.js`
- [X] T019 [US1] 运行本故事全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：此时用户故事 1 应完全可用并可独立测试（图片 → 看图 → 回答）

---

## 阶段 4：用户故事 2 - 图片理解接入 RAG 链路与降级（优先级：P1）

**目标**：图片理解结果可切换 Hybrid/改写检索；视觉模型不可用时降级不中断；存量文本对话零影响

**独立测试**：图片对话开启 hybrid=true 命中知识库；视觉模型不可用时降级为纯文本/明确提示；普通文本对话行为不变

### 用户故事 2 的测试（必选 - TDD Red 阶段）⚠️

- [X] T020 [P] [US2] 补充降级分支测试：Mock 视觉模型抛异常 → 有文字走纯文本（degraded=true）/ 仅图片返回 `VISION_SERVICE_UNAVAILABLE` 在 `ai-cs-chat/src/test/java/com/aics/chat/service/VisionChatServiceImplTest.java`
- [X] T021 [P] [US2] 编写存量回归测试：普通文本对话 `/chat/send`、`/chat/rag` 行为与视觉能力上线前一致在 `ai-cs-chat/src/test/java/com/aics/chat/controller/ChatControllerTest.java`（或复用已有测试类）

### 用户故事 2 的实施

- [X] T022 [US2] 在 `VisionChatServiceImpl` 完善降级逻辑（视觉不可用 → degraded=true 纯文本 / 仅图片明确错误码，不抛 5xx）
- [X] T023 [US2] 图片对话接入 Hybrid/改写参数透传（hybrid/rewrite 布尔参数控制检索模式）
- [X] T024 [US2] 运行本故事全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：此时用户故事 1 和 2 均应可独立运行

---

## 阶段 5：用户故事 3 - 商品图片自动生成描述（优先级：P2）

**目标**：商品图自动生成文本描述写入 image_description 字段，增强检索；失败不中断商品流程

**独立测试**：调用 `ImageDescriptionService.describe` 返回非空描述；视觉不可用返回 null 且商品创建/更新不中断

### 用户故事 3 的测试（必选 - TDD Red 阶段）⚠️

- [X] T025 [P] [US3] 编写 `SiliconFlowImageDescriptionServiceTest`：Mock 视觉模型 → 成功返回描述 / 失败返回 null在 `ai-cs-product/src/test/java/com/aics/product/service/SiliconFlowImageDescriptionServiceTest.java`

### 用户故事 3 的实施

- [X] T026 [US3] 在 product 服务装配视觉模型 Bean（复用 Nacos `aics.vision.*` 配置，`@Qualifier("visionChatModel")`）在 `ai-cs-product/src/main/java/com/aics/product/config/VisionConfig.java`
- [X] T027 [US3] 实现 `SiliconFlowImageDescriptionService` 替换 `NoopImageDescriptionService`（`describe(imageUrl)` 调视觉模型，失败返回 null）在 `ai-cs-product/src/main/java/com/aics/product/service/impl/SiliconFlowImageDescriptionService.java`
- [X] T028 [US3] product 实体/Mapper 新增 `imageDescription` 字段映射，商品创建/更新触发描述生成并落库在 `ai-cs-product/src/main/java/com/aics/product/entity/Product.java`、对应 Mapper XML
- [X] T029 [US3] 运行本故事全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：所有用户故事此时应均可独立运行

---

## 阶段 6：优化与跨切面关注点

**目的**：影响多个用户故事的改进

- [X] T030 [P] 安全加固：视觉结果 PII 脱敏（手机号/身份证）+ 敏感词过滤在 `ai-cs-chat/src/main/java/com/aics/chat/util/`
- [X] T031 [P] 补充单元测试并确认覆盖率达到宪法第2-1条阈值（行 ≥40%、分支 ≥30%）
- [X] T032 运行 quickstart.md 验证路径（编译 + 上传/图片对话/SSE/商品图描述接口）
- [X] T033 [P] 核对 spec/plan 附录代码位置与实际实现一致

---

## 依赖与执行顺序

### 阶段依赖

- **初始化（阶段 1）**：无依赖 - 可立即开始
- **基础层（阶段 2）**：依赖初始化完成 - 阻塞所有用户故事
- **用户故事（阶段 3/4/5）**：全部依赖基础层阶段完成
  - US1（阶段 3）与 US2（阶段 4）是同一 `VisionChatServiceImpl` 的先后增强，US2 依赖 US1
  - US3（阶段 5）独立，可在基础层完成后与 US1 并行
- **优化（阶段 6）**：依赖所有目标用户故事完成

### 用户故事依赖

- **用户故事 1（P1）**：基础层完成后可开始，无其他故事依赖
- **用户故事 2（P1）**：依赖 US1（同服务的降级与参数增强）
- **用户故事 3（P2）**：基础层完成后可开始，独立于 US1/US2（product 模块）

### 每个用户故事内部

- 测试（必选）必须先编写，并在实施前运行确认失败（Red 证据）
- DTO/校验器先于服务，服务先于控制器端点
- 核心实施先于集成
- 故事完成后再进入下一优先级

### 并行机会

- 阶段 1/2 所有标记 [P] 的任务可并行执行
- 基础层完成后，US1 与 US3 可并行（不同模块）
- US1 内标记 [P] 的测试可并行；DTO/校验器可并行
- 不同用户故事可由不同团队成员并行开发

---

## 并行示例：用户故事 1

```bash
# 同时启动 US1 的测试（Red）：
任务："VisionChatServiceImplTest（两段式编排）"
任务："ChatControllerVisionTest（参数校验/返回结构）"

# 同时启动基础层 DTO/校验器：
任务："VisionChatRequest DTO"
任务："VisionChatResponse DTO"
任务："ImageUrlValidator（SSRF 白名单）"
```

---

## 实施策略

### MVP 优先（用户故事 1）

1. 完成阶段 1：初始化
2. 完成阶段 2：基础层（关键 - 阻塞所有故事）
3. 完成阶段 3：用户故事 1
4. **停止并验证**：独立测试用户故事 1（图片 → 看图 → 回答）
5. 准备好后部署/演示

### 增量交付

1. 完成初始化 + 基础层 → 基础层就绪
2. 添加用户故事 1 → 独立测试 → 部署/演示（MVP！）
3. 添加用户故事 2 → 独立测试（降级 + 存量回归）→ 部署/演示
4. 添加用户故事 3 → 独立测试（商品图描述）→ 部署/演示
5. 每个故事增加价值且不破坏之前的故事

### 并行团队策略

多人开发时：

1. 团队共同完成初始化 + 基础层
2. 基础层完成后：
   - 开发者 A：用户故事 1（chat 图片对话）
   - 开发者 B：用户故事 3（product 商品图描述）
3. 开发者 A 完成 US1 后继续 US2（降级增强）

---

## 备注

- [P] 任务 = 不同文件，无依赖
- [故事] 标签将任务映射到特定用户故事以实现可追溯性
- 每个用户故事应可独立完成和测试
- 实施前必须运行测试并留存失败证据（Red）；实施后必须运行测试确认通过（Green）
- 视觉 LLM 调用（visionChatModel）属 AI 端到端行为，用 Mock 做单测，不强制单元覆盖率
- 每个任务或逻辑分组后提交（Conventional Commits：feat(chat)/feat(product)/feat(frontend)/test/docs）
- 避免：模糊任务、同文件冲突、破坏独立性的跨故事依赖
