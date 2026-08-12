# AI 智能客服平台（AI Customer Service Platform）

---

## 项目概要

**ai-customer-service 1.0.0-SNAPSHOT** — 基于 Spring Cloud 微服务架构的 AI 智能客服平台

- 主分支：`main`
- 定位：提供 AI 对话、知识库 RAG、智能搜索、消息管理、通知推送等一站式智能客服能力
- 前端：Vue 3 + Element Plus + Vite
- 后端：Java 17 / Spring Boot 3.2 / Spring Cloud 2023 / Spring AI 1.0

### 统一规范

- 技术栈：[tech-stack.md](.claude/context/tech-stack.md)
- 编码规范：[coding-rules.md](.claude/rules/coding-rules.md)
- GIT 工作流：[git-workflow.md](.claude/rules/git-workflow.md)

---

## 构建与运行

```bash
# 后端全量编译
mvn clean install -DskipTests

# 后端编译（含测试）
mvn clean verify

# 启动单个微服务（示例：chat 服务）
mvn -pl ai-cs-chat spring-boot:run

# 前端开发
cd ai-cs-frontend && npm install && npm run dev

# 前端构建
cd ai-cs-frontend && npm run build

# Docker Compose 启动基础设施
docker-compose up -d
```

编译成功后访问：
- 前端：http://localhost:5173
- 网关：http://localhost:8080
- Swagger UI（各服务）：http://localhost:{port}/swagger-ui.html

---

## 工程结构

```
ai-customer-service/
├── ai-cs-common       ← 公共模块（统一响应、异常处理、JWT 工具）
├── ai-cs-gateway      ← API 网关（路由、鉴权过滤器）
├── ai-cs-user         ← 用户服务（注册、登录、用户管理）
├── ai-cs-chat         ← AI 对话服务（Spring AI、RAG、Function Calling）
├── ai-cs-knowledge    ← 知识库服务（文档管理、向量化）
├── ai-cs-search       ← 搜索服务（Elasticsearch 全文检索）
├── ai-cs-message      ← 消息服务（RocketMQ 消息生产/消费）
├── ai-cs-notify       ← 通知服务（WebSocket 实时推送）
├── ai-cs-order        ← 订单服务（购物车、订单、优惠计算、支付）
├── ai-cs-product      ← 商品服务（商品CRUD、库存管理、分类管理）
├── ai-cs-frontend     ← 前端工程（Vue 3 + Element Plus + Vite）
├── deploy/            ← 部署配置（Docker Compose、K8s、MySQL 初始化）
├── docs/              ← 项目文档
└── specs/             ← SDD 工作流产物
```

### 依赖方向规则

```
gateway → user / chat / knowledge / search / message / notify / order / product
chat / knowledge / search / message / notify / user / order / product → common
```

**禁止**：
- 微服务之间直接依赖（必须通过网关或 Feign/HTTP 调用）
- common 依赖任何业务模块（循环依赖）

---

## SDD 工作流

本工程预置了完整的 **Spec-Kit / SDD 工作流**，通过命令驱动：

- 宪法：[.specify/memory/constitution.md](.specify/memory/constitution.md)
- 模板：[.specify/templates/](.specify/templates/)
- 命令：`/speckit.specify` → `/speckit.clarify` → `/speckit.plan` → `/speckit.tasks` → `/speckit.implement` → `/speckit.analyze`
- 产物目录：`specs/<feature-id>/`

详见 [specs/README.md](specs/README.md)。

<!-- SPECKIT START -->
当前活动功能：RAG 检索质量升级五件套
- feature-id: `002-rag-quality-upgrade`
- 分支: `002-rag-quality-upgrade`
- 实施计划: [specs/002-rag-quality-upgrade/plan.md](specs/002-rag-quality-upgrade/plan.md)
- 功能规格: [specs/002-rag-quality-upgrade/spec.md](specs/002-rag-quality-upgrade/spec.md)
<!-- SPECKIT END -->

---

## Harness 配置

- `.claude/`：AI Agent skills、agents、commands、rules、memory、context
- `.agents/skills/`：通用 Skill 镜像（供其他 IDE/工具复用）
- `.specify/`：SDD 工作流配置
- `link-claude-to-qoder.ps1`：Claude Code ↔ Qoder 链接脚本

---

## TDD 开发规范

本项目强制执行 TDD（宪法第2-1条），开发任何新功能必须遵循：

### Red → Green → Refactor 循环

```bash
# 1. Red：先写失败测试
mvn -pl ai-cs-order test -Dtest=XxxServiceTest

# 2. Green：写最小实现使测试通过
mvn -pl ai-cs-order test

# 3. Refactor：重构 + 全量回归（含 JaCoCo 覆盖率门禁）
mvn -pl ai-cs-order verify
```

### 覆盖率门禁（JaCoCo）

- 行覆盖率 ≥ 40%
- 分支覆盖率 ≥ 30%
- `mvn verify` 时自动检查，不达标则构建失败

### 测试分层

| 层 | 工具 | 覆盖重点 |
|----|------|----------|
| Service 单元测试 | Mockito + JUnit 5 | 业务逻辑、异常分支、边界值 |
| Controller 单元测试 | Mockito + 直接调用 | 委托正确性、返回结构 |
| 集成测试 | H2 + Embedded Redis | 跨 Service 协作流程 |

### 提交纪律

- 测试代码必须先于（或同次于）实现代码提交
- 禁止后补测试冒充 TDD
- 重构后必须重跑全量测试确认无回归

### 测试环境配置

- 配置文件：`src/test/resources/application.yml`（禁用 Nacos/Redis/RocketMQ）
- H2 建表：`src/test/resources/schema-test.sql`
- JDK 要求：编译需 JDK 17+（本地 JAVA_HOME 指向 JDK 21）

---

## 基础设施

| 组件 | 版本 | 用途 |
|------|------|------|
| MySQL | 8.0 | 业务数据持久化 |
| Redis | 7.x | 缓存、会话、分布式锁 |
| Elasticsearch | 8.12 | 全文搜索、知识库检索 |
| RocketMQ | 5.x | 异步消息、事件驱动 |
| Nacos | 2.3 | 服务注册与配置中心 |

---

## 学习资源

- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/index.html)
- [Spring Cloud Alibaba 文档](https://sca.aliyun.com/docs/2023/user-guide/quickstart/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [Martin Fowler: Harness Engineering](https://martinfowler.com/articles/harness-engineering.html)
