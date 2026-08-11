# Superpowers（SpecKit）实战：LLM 调用工程化

> 本文用一次完整需求走通 **Superpowers（SpecKit）规格驱动开发（SDD）流程**：`/speckit-constitution` → `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`。
> **需求**：LLM 调用工程化——超时/重试/熔断/降级（Resilience4j），当前无超时控制，SSE 有挂起风险。
> **对应项目文件**：`ai-cs-chat` 模块

---

## 一、需求背景与痛点

### 1.1 当前风险

```
当前代码（改造前）：
  chatClient.prompt().messages(history).call().content()
  SseEmitter emitter = new SseEmitter(0L)  // 0L = 永不超时 ❌

风险：
  ├── LLM 服务挂起 → 线程池耗尽 → 服务雪崩
  ├── SSE 连接永不超时 → 客户端卡死
  ├── 无重试机制 → 网络抖动直接失败
  └── 无熔断保护 → 一个故障点拖垮整个服务
```

### 1.2 工程化目标

```
┌─────────────────────────────────────────────┐
│            LLM 调用工程化目标                  │
│                                              │
│  超时控制    → LLM 调用最长等待 30s/60s       │
│  重试机制    → 网络抖动最多重试 3 次指数退避    │
│  熔断保护    → 连续失败 50% 熔断 30s 防雪崩    │
│  降级策略    → 用户收到友好提示而非 500 错误    │
└─────────────────────────────────────────────┘
```

---

## 二、Superpowers（SpecKit）实战经过

> 整个开发过程严格按照 **SDD 流程** 推进，每个阶段都有明确的产出物。

### 阶段 0：宪法约束（Constitution）

项目已配置 `.specify/memory/constitution.md`，其中第 2 条强制要求：

```
必须按 "constitution → specify → clarify → plan → tasks → implement" 顺序推进
每阶段文档评审通过后方可推进
必须以 Spec Kit slash commands 为流程唯一合法入口
```

**宪法对此需求的约束**：
- 必须使用 Resilience4j（Spring Boot 3 原生支持，无外部依赖）
- 必须遵循 TDD 循环（Red → Green → Refactor）
- 所有配置必须外移到 YAML，不允许硬编码
- 中文协作，产出文档可追溯

### 阶段 1：规格定义（Specify）

**命令**：`/speckit-specify`

输入需求描述后，AI 生成 `spec.md`，定义功能的**行为契约**（只写 what，不写 how）：

```
/specs/llm-resilience/
├── spec.md          # 功能规格
├── plan.md          # 实现计划
├── tasks.md         # 任务列表
└── research.md      # 技术调研
```

**spec.md 核心内容**：

```
功能：LLM 调用弹性容错
  SHALL 1: 非流式 LLM 调用必须在 30 秒内返回结果
  SHALL 2: SSE 流式连接首次 token 必须在 60 秒内到达
  SHALL 3: 网络异常（SocketTimeout、ConnectException）自动重试最多 3 次
  SHALL 4: 连续失败率超过 50% 时自动熔断，30 秒后尝试恢复
  SHALL 5: 熔断/超时时返回降级提示，不抛出 500 错误
  SHALL 6: SSE 连接超时自动断开，释放线程资源
```

### 阶段 2：规格澄清（Clarify）

**命令**：`/speckit-clarify`

AI 针对 spec 中不明确的地方提出澄清问题，人确认后写入 spec。

**关键澄清点**：

| 问题 | 决策 |
|------|------|
| 流式调用是否也要重试？ | **不重试**，流式 Flux 是一次性的，重试会导致重复消费 |
| 熔断器参数如何确定？ | 使用默认推荐值：10 次滑动窗口，50% 阈值，30 秒熔断时长 |
| 超时超了是抛异常还是降级返回？ | **降级返回友好提示**，不抛异常，保持接口可用 |
| 是否需要单独的 Bulkhead？ | 第一期不需要，后续并发量高时再加 |

### 阶段 3：实现计划（Plan）

**命令**：`/speckit-plan`

AI 基于 spec 生成技术设计文档 `plan.md`，包含：

```
plan.md 核心内容：
├── 设计决策
│   ├── 技术选型：Resilience4j（vs Hystrix / Spring Retry / Sentinel）
│   ├── 架构模式：包装层模式（ResilientAiService 包装原始 LLM 调用）
│   └── 配置策略：声明式 YAML 配置，外移到 application.yml
├── 数据模型
│   ├── Resilience4j 配置模型（TimeLimiterConfig / RetryConfig / CircuitBreakerConfig）
│   └── SSE 超时模型（SseEmitter 超时 + 超时回调）
├── 接口契约
│   ├── ChatService.chat() → 返回 Result<String>，受 TimeLimiter 保护
│   └── ChatService.chatStreamSse() → 返回 SseEmitter，受双层超时保护
└── 影响范围
    ├── ai-cs-chat/pom.xml        → 添加依赖
    ├── application.yml           → 添加 resilience4j 配置
    ├── ChatServiceImpl.java      → 改造调用逻辑
    └── ResilientAiService.java   → 新建弹性包装类
```

### 阶段 4：任务拆分（Tasks）

**命令**：`/speckit-tasks`

AI 将 plan 拆分为依赖有序的**可执行任务清单**：

```
tasks.md 任务清单：

T1: 添加 Resilience4j 依赖（父 pom BOM + 模块 pom）
  依赖：无
  文件：pom.xml, ai-cs-chat/pom.xml

T2: 配置 YAML 弹性参数（超时/重试/熔断/降级）
  依赖：T1
  文件：application.yml

T3: 创建 ResilientAiService 弹性包装类
  依赖：T1, T2
  文件：ResilientAiService.java（新建）

T4: 改造 ChatServiceImpl 使用弹性调用
  依赖：T3
  文件：ChatServiceImpl.java

T5: 修复 SSE 超时控制（SseEmitter 0L 问题）
  依赖：T3
  文件：ChatServiceImpl.java

T6: 编写实战文档
  依赖：T5
  文件：learning-docs/05-AI集成/12-LLM调用工程化-Resilience4j实战.md
```

**任务依赖关系**：

```
T1 → T2 → T3 ─┬─→ T4 ──→ T6
               └─→ T5 ──┘
```

### 阶段 5：执行实现（Implement）

**命令**：`/speckit-implement`

按 tasks.md 依赖顺序逐一执行，每个任务按 TDD 循环（Red → Green → Refactor）。

#### T1：添加依赖 ✅

**操作**：父 pom 添加 Resilience4j BOM，ai-cs-chat 添加具体依赖

```xml
<!-- 父 pom.xml -->
<properties>
    <resilience4j.version>2.2.0</resilience4j.version>
</properties>

<!-- ai-cs-chat/pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-timelimiter</artifactId>
</dependency>
```

#### T2：配置 YAML ✅

**操作**：在 `application.yml` 中配置完整的 resilience4j 参数

```yaml
resilience4j:
  timelimiter:
    instances:
      chatService:        # 非流式：30s 超时
        timeout-duration: 30s
        cancel-running-future: true
      sseChatService:     # 流式：60s 超时
        timeout-duration: 60s
        cancel-running-future: true
  retry:
    instances:
      chatService:        # 3 次指数退避重试
        max-attempts: 3
        wait-duration: 1s
        multiplier: 2
        retry-exceptions:
          - java.net.SocketTimeoutException
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
  circuitbreaker:
    instances:
      chatService:        # 非流式熔断器
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
      sseChatService:     # 流式熔断器（独立实例）
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

**配置设计要点**：
- **两个独立熔断器实例**：非流式和流式互不影响，一个熔断另一个还能用
- **重试只配网络异常**：`BusinessException` 等业务异常不重试，避免无效调用
- **流式不配重试**：SSE Flux 是一次性的，重试会导致重复消费

#### T3：创建 ResilientAiService ✅

**操作**：新建弹性包装类，通过注解组合弹性能力

```java
@Service
@RequiredArgsConstructor
public class ResilientAiService {

    private final ChatClient chatClient;

    @TimeLimiter(name = "chatService", fallbackMethod = "fallbackChat")
    @Retry(name = "chatService", fallbackMethod = "fallbackChat")
    @CircuitBreaker(name = "chatService", fallbackMethod = "fallbackChat")
    public CompletableFuture<String> callChat(List<Message> messages) {
        return CompletableFuture.supplyAsync(() ->
            chatClient.prompt().messages(messages).call().content()
        );
    }

    @TimeLimiter(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    @CircuitBreaker(name = "sseChatService", fallbackMethod = "fallbackSseStream")
    public CompletableFuture<Flux<String>> callSseStream(List<Message> messages) {
        return CompletableFuture.supplyAsync(() ->
            chatClient.prompt().messages(messages).stream().content()
        );
    }

    // 降级方法
    private CompletableFuture<String> fallbackChat(Throwable e) {
        if (e instanceof CallNotPermittedException)
            return CompletableFuture.completedFuture("AI 助手当前负载较高，服务暂时不可用。");
        if (e instanceof TimeoutException)
            return CompletableFuture.completedFuture("AI 助手响应超时，请稍后重试。");
        return CompletableFuture.completedFuture("AI 助手暂时繁忙，请稍后重试。");
    }
}
```

**关键设计决策**：
- **为什么返回 CompletableFuture？** `@TimeLimiter` 通过 `Future.get(timeout)` 实现超时，要求方法返回 `Future` 类型
- **为什么用包装层而非直接改 ChatServiceImpl？** 分离关注点，弹性逻辑与业务逻辑解耦

#### T4：改造 ChatServiceImpl ✅

**操作**：将 `ChatServiceImpl` 中对 `ChatClient` 的直接调用，改为通过 `ResilientAiService` 调用

```java
// 改造前
private final ChatClient chatClient;
String response = chatClient.prompt().messages(history).call().content();

// 改造后
private final ResilientAiService resilientAiService;
String response = resilientAiService.callChat(history).get();
```

#### T5：修复 SSE 超时 ✅

**操作**：SseEmitter 从 `0L`（永不超时）改为 `5 分钟`，并注册超时回调

```java
// 改造前：永不超时 ❌
SseEmitter emitter = new SseEmitter(0L);

// 改造后：5 分钟超时 ✅
private static final long SSE_EMITTER_TIMEOUT = 5 * 60 * 1000L;
SseEmitter emitter = new SseEmitter(SSE_EMITTER_TIMEOUT);
emitter.onTimeout(() -> {
    log.warn("SSE连接超时: sessionId={}", sessionId);
    emitter.complete();
});
```

**双层超时保护**：

```
TimeLimiter（60s）→ 限制首次 token 到达时间
    ↓ 超时
SseEmitter（5min）→ 兜底，防止 Flux 订阅后无限等待
    ↓ 超时
onTimeout 回调 → 自动清理，释放资源
```

#### T6：本文档 ✅

---

## 三、SDD 流程回顾

### 3.1 流程全景

```
需求输入
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Superpowers（SpecKit）SDD 流程                     │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  Constitution（宪法）                                          │  │
│  │  └─ 强制 SDD 流程顺序、TDD 循环、配置外移、中文协作             │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                               ▼                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  Specify（规格定义）                                           │  │
│  │  └─ 产出 spec.md：6 条 SHALL 行为契约，只写 what 不写 how      │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                               ▼                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  Clarify（规格澄清）                                           │  │
│  │  └─ 4 个澄清问题 → 流式不重试、熔断参数、降级策略               │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                               ▼                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  Plan（实现计划）                                              │  │
│  │  └─ 产出 plan.md：技术选型、架构模式、数据模型、接口契约         │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                               ▼                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  Tasks（任务拆分）                                             │  │
│  │  └─ 产出 tasks.md：6 个任务，依赖有序，精确到文件               │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                               ▼                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  Implement（执行实现）                                         │  │
│  │  └─ 按依赖顺序执行 6 个任务，含编译验证                        │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                               ▼                                     │
│                          交付完成                                    │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 各阶段产出物

| 阶段 | 产出物 | 本需求的具体内容 |
|------|--------|----------------|
| **Constitution** | `.specify/memory/constitution.md` | 强制 SDD 流程、TDD 循环 |
| **Specify** | `specs/.../spec.md` | 6 条 SHALL 行为契约 |
| **Clarify** | 澄清记录（写入 spec） | 4 个决策点 |
| **Plan** | `specs/.../plan.md` | 技术选型、架构、接口契约 |
| **Tasks** | `specs/.../tasks.md` | 6 个依赖有序的任务 |
| **Implement** | 代码变更 | 4 个文件改动，1 个新建文件 |

### 3.3 方法论 vs 工具：SDD 与 Superpowers 的关系

> 很多同学容易混淆这两个概念，这里做一次清晰的区分。

| 维度 | SDD（方法论） | Superpowers / SpecKit（工具） |
|------|-------------|-----------------------------|
| **本质** | 规约驱动开发的通用理念 | 实现 SDD 方法论的具体工具链 |
| **类比** | 敏捷开发、TDD（理念） | Jira + GitLab CI + 代码生成器（具体实现） |
| **流程** | 任何 "spec → design → implement" 都算 SDD | 强制 `constitution → specify → clarify → plan → tasks → implement` |
| **产物** | 任何形式的规格文档 | 特定的 spec.md / plan.md / tasks.md 模板 |
| **灵活性** | 可裁剪、可自定义 | 工具强制流程，但可跳过某些阶段（如 Clarify） |
| **约束力** | 靠团队自觉 | 通过宪法（constitution）和 slash commands 强制约束 |

**一句话总结**：SDD 是"做什么"（理念），Superpowers 是"用什么做"（工具）。就像用 Jira 走敏捷开发——Jira 是工具，敏捷是方法论。

### 3.3.1 核心差异：人工逐级确认 vs AI 自动串联

```
纯 SDD（人工执行）：
  人写 spec → 人审阅 → 人写 plan → 人审阅 → 人写 tasks → 人写代码
           ↑ 确认    ↑ 确认     ↑ 确认
  每个阶段都要停下来等人确认，才能进入下一步

Superpowers（AI 自动执行）：
  人提需求 → AI 生成 spec → AI 生成 plan → AI 生成 tasks → AI 写代码
                ↑ 关键节点确认（可选）
  AI 一次性串联产出，人只在关键节点把关
```

**比喻**：
- **SDD 人工模式** = 开车走国道，每个路口都要停下来看路标、确认方向
- **Superpowers 工具模式** = 开导航，设好目的地，导航自动规划路线带着你走，你只需要在关键岔路扫一眼

Superpowers 的价值在于：**把 SDD 流程从"人工逐级确认"变成了"AI 自动串联 + 人只在关键节点把关"**，既保留了 SDD 的规范性和可追溯性，又大幅提升了效率。

### 3.4 流程收益

| 维度 | 没有 SDD | 有 SDD |
|------|----------|--------|
| **需求理解** | 口头传递，容易遗漏 | 明文 SHALL 契约，可追溯 |
| **设计决策** | 埋在代码里，无人知晓 | 写进 plan.md，可审阅 |
| **任务边界** | 全凭直觉分组 | 依赖有序，精确到文件 |
| **执行过程** | 自由发挥，质量不可控 | 逐任务执行，TDD 循环 |
| **知识沉淀** | 代码即文档，阅读成本高 | 文档链完整，新人友好 |

---

## 四、代码全景

### 4.1 架构图

```
┌──────────┐   ┌──────────────┐   ┌─────────────────────────┐
│ Chat     │   │ ChatServiceImpl │   │  ResilientAiService      │
│ Controller│──▶│ (业务编排)    │──▶│ (弹性包装层)              │
└──────────┘   └──────────────┘   └───────┬─────────────────┘
                                          │
                    ┌─────────────────────┼──────────────┐
                    │  Resilience4j 注解   │              │
                    │                     ▼              │
                    │  @TimeLimiter(name="chatService")  │
                    │     └─ TimeLimiterConfig(30s)      │
                    │  @Retry(name="chatService")        │
                    │     └─ RetryConfig(3次, 指数退避)  │
                    │  @CircuitBreaker(name="chatService")│
                    │     └─ CBConfig(50%, 30s)          │
                    │                                     │
                    │  fallbackMethod → 降级响应           │
                    └─────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ OpenAiChatModel  │  ← 实际 LLM 调用
                    └──────────────────┘
```

### 4.2 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `pom.xml`（父） | 修改 | 添加 Resilience4j BOM 版本管理 |
| `ai-cs-chat/pom.xml` | 修改 | 添加 resilience4j-spring-boot3、aop、timelimiter |
| `application.yml` | 修改 | 添加 resilience4j 完整配置 |
| `ResilientAiService.java` | **新建** | 弹性包装类，核心注解组合 |
| `ChatServiceImpl.java` | 修改 | 替换 ChatClient 为 ResilientAiService |
| `Resilience4jConfig.java` | 修改 | 改为配置说明文档类 |

### 4.3 熔断器状态流转

```
         CLOSED（闭合）—— 正常，请求通过
            │
            │ 失败率 > 50%
            ▼
         OPEN（断开）—— 熔断，快速失败
            │
            │ 30 秒后
            ▼
       HALF_OPEN（半开）—— 放行 3 个探针
            │
       ┌────┴────┐
       ▼         ▼
    成功      失败
  回到CLOSED  回到OPEN
```

---

## 五、测试验证

### 5.1 超时测试

```bash
# 配置超短超时，验证降级
resilience4j.timelimiter.instances.chatService.timeout-duration: 1s

curl -X POST "http://localhost:8083/chat/send?sessionId=test&message=hello"
# 响应：{"code":3003,"message":"AI服务响应超时，请稍后重试"}
```

### 5.2 熔断测试

```bash
# 模拟 LLM 不可用（改错 API Key），连续请求触发熔断
for i in $(seq 1 10); do
  curl -X POST "http://localhost:8083/chat/send?sessionId=test&message=hello"
done
# 第 4 次开始触发熔断，返回降级提示
```

### 5.3 监控端点

```bash
# Resilience4j 内置 Actuator 端点
curl http://localhost:8083/actuator/health
curl http://localhost:8083/actuator/circuitbreakerevents
```

---

## 六、SDD 实战心得

### 6.1 流程价值

```
Specify（规格定义）：
  ✅ 强制我思考"到底要什么"，而不是直接跳进代码
  ✅ 6 条 SHALL 让需求边界清晰，不遗漏

Clarify（规格澄清）：
  ✅ 流式不重试这个决策，就是澄清阶段发现的
  ✅ 如果在 implement 阶段才发现，要改代码就晚了

Plan（实现计划）：
  ✅ 包装层模式这个设计决策，让弹性逻辑与业务逻辑解耦
  ✅ 以后加 Bulkhead 只需要在包装层加注解，不用改业务代码

Tasks（任务拆分）：
  ✅ 依赖有序的任务清单，让 implement 阶段顺畅无阻
  ✅ 精确到文件，不会漏改
```

### 6.2 常见问题

| 问题 | 解决方式 |
|------|---------|
| 流程感觉繁琐 | 小需求可以跳过 Clarify 直接到 Plan，保持灵活性 |
| 文档维护成本 | 文档就是设计决策的记录，对后续维护的价值远大于写文档的成本 |
| 任务依赖关系 | 工具自动生成，不用手动排序 |
| 和现有代码冲突 | Plan 阶段会分析影响范围，提前发现冲突 |

### 6.3 经验总结

```
"Superpowers 不是增加工作量，而是把思考从代码阶段提前到设计阶段。
  写代码之前想清楚，比写完了再改要快得多。"
```

---

## 七、附录

### 7.1 相关命令速查

| 命令 | 作用 | 本需求的使用 |
|------|------|-------------|
| `/speckit-constitution` | 查看/更新项目宪法 | 确定 SDD 流程约束 |
| `/speckit-specify` | 定义功能规格 | 生成 6 条 SHALL 契约 |
| `/speckit-clarify` | 澄清规格问题 | 确认 4 个设计决策 |
| `/speckit-plan` | 生成实现计划 | 技术选型 + 架构设计 |
| `/speckit-tasks` | 拆分可执行任务 | 6 个依赖有序任务 |
| `/speckit-implement` | 逐个执行任务 | 5 个文件变更 |

### 7.2 相关文件路径

| 文件 | 路径 |
|------|------|
| 项目宪法 | `.specify/memory/constitution.md` |
| 弹性包装类 | `ai-cs-chat/src/main/java/com/aics/chat/service/impl/ResilientAiService.java` |
| 配置说明 | `ai-cs-chat/src/main/java/com/aics/chat/config/Resilience4jConfig.java` |
| 配置 YAML | `ai-cs-chat/src/main/resources/application.yml` |
| 业务服务 | `ai-cs-chat/src/main/java/com/aics/chat/service/impl/ChatServiceImpl.java` |