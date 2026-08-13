# 实施计划：多模态图生文（VLM）

**分支**: `004-vlm-multimodal` | **日期**: 2026-08-13 | **规格**: [spec.md](spec.md)
**输入**: 来自 `/specs/004-vlm-multimodal/spec.md` 的功能规格

## 摘要

在现有 RAG 对话链路基础上新增多模态图片理解能力，补齐"用户无法发图提问"与"商品图片无文本描述"两大盲区：

1. **图片对话**（US1）：对话支持携带图片 URL，视觉模型（硅基流动 Qwen2.5-VL）理解图片生成文本描述
2. **图片理解接入 RAG 链路**（US2）：描述文本走现有 Hybrid/改写/GraphRAG 检索 + DeepSeek 回答，可降级
3. **商品图片自动描述**（US3）：回填 `ImageDescriptionService`，商品图生成描述写入 `image_description` 字段增强检索

技术方案详见 [research.md](research.md)、[data-model.md](data-model.md)、[contracts/rest-api.md](contracts/rest-api.md)。

## 技术上下文

**语言/版本**：Java 17（本机编译 JDK 21）/ Spring Boot 3.2.5 / Spring Cloud 2023 / Spring AI 1.1.4
**主要依赖**：spring-ai（ChatClient/OpenAiChatModel/Media 多模态消息）、MinIO（FileStorageService）、Resilience4j；前端 Vue 3 + Element Plus + Vite
**存储**：MinIO（图片）、MySQL（product.image_description 新字段）、Chroma（RAG 向量）、Redis（会话）
**测试**：JUnit 5 + Mockito + JaCoCo（行覆盖率 ≥ 40%、分支 ≥ 30%，目标 60%/50%）
**目标平台**：Linux 服务器（微服务）+ 浏览器（前端）
**项目类型**：Web 微服务 + Vue SPA
**性能目标**：视觉理解 ≤ 5s；图片对话 P95 ≤ 8s；流式首 token ≤ 5s
**约束**：不新增微服务模块；两段式架构（VLM 看图 + LLM 回答）；视觉不可用 100% 降级；所有 LLM 调用复用弹性治理；新增代码强制 TDD
**规模/范围**：3 个用户故事，落在 ai-cs-chat / ai-cs-product / ai-cs-frontend / ai-cs-common

## 宪法检查

*门禁：第 0 阶段研究前通过，第 1 阶段设计后复检。*

| 条款 | 结论 | 说明 |
|------|------|------|
| 第2条 SDD 流程 | ✅ | 经 /speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement 顺序推进 |
| 第2-1条 TDD | ✅ 计划中 | tasks 阶段为每个实现任务配置前置测试任务（Red→Green→Refactor）；视觉 LLM 端到端行为纳入集成测试，不强制单元覆盖率 |
| 第12条 文档规范 | ✅ | 中文文档/注释、英文代码命名、SpringDoc 注解、Conventional Commits |
| 第13条 配置安全 | ✅ | 新增视觉 API Key 走 Nacos（aics.vision.*），不硬编码 |
| 第13-2条 公共能力 | ✅ | 图片上传复用 ai-cs-common FileStorageService，不重复造轮子 |
| 第16条 模块架构 | ✅ | 不新增模块；chat 与 product 各自装配视觉 Bean，不互相依赖；SQL 落 deploy/mysql/ |
| 第17条 技术优先 | ✅ | 视觉模型复用 Spring AI OpenAiChatModel（OpenAI 兼容），不引入新 SDK |
| 第20-1条 Spring AI 规范 | ✅ | 视觉 ChatModel 走 ChatClient/OpenAiChatModel 构建；流式调用配置超时+重试 |
| 第21条 领域模型 | ✅ | 新 DTO 命名遵循 *DTO 规范（VisionChatRequest/VisionChatResponse） |
| 第22条 Git 工作流 | ✅ 计划中 | 按功能分类提交（feat(chat)/feat(product)/feat(frontend)/docs/test） |

**门禁结论**：通过，无违规项，可进入 Phase 0。

## 项目结构

### 文档（本功能）

```text
specs/004-vlm-multimodal/
├── plan.md              # 本文件
├── research.md          # 第 0 阶段：技术调研与决策
├── data-model.md        # 第 1 阶段：数据模型设计
├── quickstart.md        # 第 1 阶段：快速启动与验证指南
├── contracts/
│   └── rest-api.md      # 第 1 阶段：REST API 契约
├── checklists/
│   └── requirements.md  # 规格质量检查清单
└── tasks.md             # 第 2 阶段输出（/speckit-tasks 生成）
```

### 源代码（仓库根目录）

```text
ai-cs-chat/src/main/java/com/aics/chat/
├── config/
│   └── SpringAiConfig.java        # 新增 visionChatModel Bean（@Qualifier）+ 视觉配置
├── controller/
│   └── ChatController.java        # 新增 /chat/upload-image、/chat/vision、/chat/vision/sse
├── service/
│   ├── VisionChatService.java     # 图片对话服务接口
│   └── impl/
│       └── VisionChatServiceImpl.java   # 两段式编排：视觉理解 → RAG 检索 → LLM 回答
├── dto/
│   ├── VisionChatRequest.java     # 图片对话请求 DTO
│   └── VisionChatResponse.java    # 图片对话响应 DTO
└── util/
    └── ImageUrlValidator.java     # SSRF 白名单校验 + 图片 URL 安全过滤

ai-cs-product/src/main/java/com/aics/product/
└── service/
    ├── ImageDescriptionService.java            # 已有接口（保留）
    └── impl/
        └── SiliconFlowImageDescriptionService.java  # 替换 Noop 实现（真实视觉模型）

ai-cs-common/src/main/java/com/aics/common/
└── storage/FileStorageService.java   # 复用（无需改动）

ai-cs-frontend/src/
├── views/ChatView.vue                # 输入框图片上传 + 消息气泡图片渲染
└── api/index.js                      # 新增 upload-image / vision / vision-sse 接口封装

deploy/mysql/
└── vlm-multimodal-init.sql           # product 表新增 image_description 字段（幂等）
```

## 复杂度追踪

> 本功能无宪法检查违规项，无需填写复杂度追踪表。

## 关键设计决策（详见 research.md）

| 决策 | 结论 |
|------|------|
| 视觉模型 | 硅基流动 Qwen2.5-VL（OpenAI 兼容协议） |
| 架构 | 两段式：VLM 看图 + LLM 回答，复用 003 RAG 链路 |
| 降级 | 视觉不可用 → 有文字纯文本 / 仅图片明确提示，不抛 5xx |
| 商品图描述 | product 独立装配视觉 Bean，替换 Noop 实现 |
| 安全 | SSRF 白名单 + PII 脱敏 + 敏感词过滤 |
