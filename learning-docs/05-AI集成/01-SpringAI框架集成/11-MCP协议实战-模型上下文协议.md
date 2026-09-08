# MCP 协议实战：Model Context Protocol（模型上下文协议）

> 版本锚点：Spring AI **1.1.4**（基于 MCP Java SDK 0.14+，覆盖协议规范 2025-06-18）
> 定位：项目里已经有"内生工具"（`@Tool` 注解的订单查询、NL2SQL），本文讲"外挂工具生态"——MCP。
> 面试速记版在 [`../面试题/05-AI-Agent与工具调用.md`](../面试题/05-AI-Agent与工具调用.md) §5.4，本文是它的展开版。

---

## 一、MCP 是什么：给 AI 装"USB-C 接口"

用一句人话说：

```
MCP = 一个开放协议，规定"AI 应用"和"外部工具/数据源"之间怎么对话。

类比：
  USB-C 之前：每个手机一根专用线（M×N 种线）
  USB-C 之后：所有设备插同一个口（M + N）
  MCP 之前：  每个应用为每个工具写一遍胶水代码（M×N）
  MCP 之后：  工具包成 MCP Server 一次，所有支持 MCP 的应用都能用（M + N）
```

| 角色 | 是什么 | 本项目对应 |
|---|---|---|
| MCP Host | 使用工具的 AI 应用本体 | `ai-cs-chat`（客服对话服务） |
| MCP Client | Host 内部负责连接某个 Server 的"插头"，一个 Server 对应一个 Client 实例 | Spring AI 自动配置创建的 `McpSyncClient` |
| MCP Server | 把一类工具/数据按协议暴露出去的独立程序 | 未来可以包出来的"订单 MCP Server" |

**为什么本项目值得关心这件事？** 现在客服的工具都是写死在 `ai-cs-chat` 里的
（`OrderQueryService`、`Nl2SqlQueryService` 的 `@Tool` 方法）。如果明天要接入
"查快递三方""查工单系统""企业内部 wiki"，每接一个就要在本服务里写一遍集成代码，
模型上下文里还要手工拼接这些工具的声明。MCP 把"工具的发现、声明、调用"标准化成协议，
工具变成**可以独立部署、独立升级、跨应用复用**的组件。

---

## 二、协议核心概念（先看懂，再写代码）

### 2.1 六大原语：谁向谁提供什么

> 原语（primitive）= 协议里预定义的"能力种类"。第一次读记前三行就够。

| 原语 | 由谁提供 | 人话解释 |
|---|---|---|
| **Tools** | Server → Host | "AI 可以调用的函数"。模型**主动**决定调不调（查订单、发邮件） |
| **Resources** | Server → Host | "AI 可以读的数据"。应用侧决定读什么再喂给模型（文件、配置、记录），类似 GET 语义 |
| **Prompts** | Server → Host | 预定义的提示词模板，由**用户**主动选择使用（如"帮我写退款申请"模板） |
| **Sampling** | Client → Server | 反向能力：Server 可以"借"Host 的 LLM 用一下（服务器自己也要总结/生成时） |
| **Roots** | Client → Server | 客户端告诉服务器"你只能在这些目录范围内活动"（文件系统边界） |
| **Elicitation** | Client → Server | 服务器执行中向**用户**要补充信息（"退款需要确认金额，请用户确认"） |

Tools/Resources/Prompts 的区别一句话：**Tools 是模型要用的手，Resources 是喂给模型的料，Prompts 是给用户选的模板。**

### 2.2 传输层：消息怎么搬运

MCP 的消息体是 **JSON-RPC 2.0**（就是 `{"jsonrpc":"2.0","method":"tools/call","params":{...}}` 这种格式），
消息怎么送到对方手里由传输层决定，共三种：

| 传输 | 机制 | 适用场景 |
|---|---|---|
| **STDIO** | Host 把 Server 作为**本地子进程**拉起，走标准输入/输出 | 本机工具（Claude Desktop 接本地 filesystem 就是这种）；无需网络、天然无鉴权问题 |
| **SSE（HTTP+SSE）** | 一个 SSE 长连接下行 + POST 上行 | 2025-03-26 规范前的远程方案，已被取代；1.1.x 仍支持 |
| **Streamable HTTP** | 单一 HTTP 端点，POST 上行 + 可选 SSE 流式下行，支持会话恢复 | 2025-03-26 规范引入，**远程部署的首选**；另有 STATELESS 变体适合微服务/多副本部署 |

### 2.3 一次工具调用的完整时序

```
用户："帮我查一下订单 ORD20260809001 到哪了"
  │
  ▼
MCP Host（ai-cs-chat）                          MCP Server（订单工具）
  │
  │ ① initialize（握手，协商协议版本/能力）
  │ ② tools/list ────────────────────────────▶ │
  │ ◀──── [{name:"query_order",                │  Server 返回工具清单，
  │         description:"...",                 │  每个工具带 JSON Schema 参数说明
  │         inputSchema:{...}}] ────────────── │
  │
  │ ③ Host 把【system prompt + 用户问题 + 工具清单】发给 LLM
  │    （这一步就是普通的 OpenAI 兼容请求，跟模型厂商无关）
  │
  │ ④ LLM 回复的不是答案，而是工具调用请求：
  │    tool_call: query_order({"orderId":"ORD20260809001"})
  │
  │ ⑤ tools/call {name, arguments} ─────────▶ │
  │                                            │ 执行真实逻辑（查数据库/调三方）
  │ ◀───── {content:[{type:"text",             │
  │          text:"已发货，中通 78xxx…"}]} ─── │
  │
  │ ⑥ Host 把工具结果作为"工具消息"回传 LLM
  │ ⑦ LLM 生成自然语言回答
  ▼
用户："您的订单已发货，中通快递单号 78xxx，预计明天送达。"
```

注意 ①② 是**启动时**做一次的（工具变更时 Server 会发 `notifications/tools/list_changed`
通知客户端刷新），③~⑦ 是每一轮对话发生的。**LLM 从头到尾只见过"工具清单 + 调用结果"，
协议细节对模型完全透明**——这就是 MCP 与模型解耦的关键。

### 2.4 协议版本演进（面试常问）

| 规范版本 | 关键变化 |
|---|---|
| 2024-11-05 | 初版：JSON-RPC、STDIO + HTTP/SSE、三大服务端原语 |
| 2025-03-26 | **Streamable HTTP** 取代 HTTP/SSE；新增 **OAuth 2.1 授权**框架；工具列表变更通知 |
| 2025-06-18 | 工具**结构化输出**（outputSchema）；新增 **Elicitation**（服务器向用户征询）；Resource Links；去掉 JSON-RPC 批处理 |

Spring AI 1.1.x 基于的 MCP Java SDK 已覆盖到 2025-06-18。

---

## 三、Spring AI 集成 MCP Client：把别人的工具接进来

### 3.1 依赖与 starter

```xml
<!-- ai-cs-chat/pom.xml（父 POM 已有 spring-ai-bom 1.1.4，无需写版本号） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

| Starter | 传输实现 | 什么时候用 |
|---|---|---|
| `spring-ai-starter-mcp-client` | STDIO + SSE + Streamable HTTP（JDK HttpClient） | 常规选择 |
| `spring-ai-starter-mcp-client-webflux` | 同上，但 HTTP 部分走 WebFlux | 生产远程连接推荐（非阻塞） |

> Spring AI 1.1.x 的配置项默认值以官方 1.1 文档为准，不同小版本个别默认值可能调整。

### 3.2 三种传输的配置写法（application.yml）

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: aics-chat-mcp-client
        request-timeout: 20s          # 单次 MCP 请求超时（默认 20s）
        type: SYNC                    # SYNC 或 ASYNC，全局二选一，不能混
        toolcallback:
          enabled: true               # 1.1.x 默认 true：自动把 MCP 工具包装成 ToolCallbackProvider

        # ── 方式一：STDIO（拉起本地子进程）──────────────────────
        stdio:
          connections:
            filesystem:
              command: cmd.exe        # Windows 坑！见下方说明
              args: ["/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "D:/docs"]
              env:
                API_KEY: xxx

        # ── 方式二：SSE（旧远程协议，兼容存量 Server）────────────
        sse:
          connections:
            order-server:
              url: http://localhost:8080   # 只写到 host:port
              sse-endpoint: /sse           # 默认 /sse

        # ── 方式三：Streamable HTTP（远程首选）──────────────────
        streamable-http:
          connections:
            order-server:
              url: http://localhost:8080
              endpoint: /mcp               # 默认 /mcp
```

> **Windows 踩坑（本项目开发机就是 Windows）**：`npx`/`npm`/`python` 在 Windows 上是
> `.cmd` 批处理文件，Java 的 ProcessBuilder 不能直接执行批处理，必须包一层 `cmd.exe /c`。
> Linux/macOS 直接写 `command: npx` 即可。这是 STDIO 方式最高频的启动失败原因。

也可以直接复用 Claude Desktop 的 JSON 配置格式：

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          servers-configuration: classpath:mcp-servers.json
```

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "cmd.exe",
      "args": ["/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "D:/docs"]
    }
  }
}
```

### 3.3 关键机制：MCP 工具怎么"变成"普通工具回调

这是全文最重要的一张图——它解释了为什么 MCP 可以**零侵入**接入本项目：

```
MCP Server(订单)          Spring AI 自动配置                    项目现有代码
      │                        │                                   │
      │ initialize/list        │                                   │
      ▼                        ▼                                   │
   McpSyncClient ──包装──▶ SyncMcpToolCallbackProvider            │
                            （就是一个 ToolCallbackProvider！）     │
                                   │                               │
                                   ▼                               ▼
                    ChatModelRegistry.rebuild() 里唯一的挂载点：
                    builder.defaultToolCallbacks(toolCallbackProvider)
```

项目里 [ChatModelRegistry.java](../../../ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ChatModelRegistry.java)
的挂载逻辑是这样的：

```java
// 只有声明了 TOOL_CALLING 能力的模型才挂工具，避免不支持函数调用的模型报错
if (definition.getCapabilities().contains(ModelCapability.TOOL_CALLING)) {
    builder = builder.defaultToolCallbacks(toolCallbackProvider);
}
```

对 MCP 工具来说这条链路**一行都不用改**：自动配置注册的
`SyncMcpToolCallbackProvider` 本身就实现了 `ToolCallbackProvider` 接口，
MCP 远端工具在 Spring AI 眼里和本地 `@Tool` 方法是同一种东西
（`ToolCallback`）。模型看到的是一个普通的函数清单，调用时 Spring AI
在内部走 JSON-RPC 转发给 MCP Server——**流式（`.stream()`）与非流式
（`.call()`）共用这条链路**，工具实际执行发生在 Spring AI 内部
（见 [ResilientAiService](../../../ai-cs-chat/src/main/java/com/aics/chat/service/impl/ResilientAiService.java) 的注释）。

也因此，项目的可观测性天然覆盖 MCP 工具：`TraceContext` 里 tools 段记录的
"工具名、参数摘要、结果状态、耗时"对远端工具同样生效（它们走的是同一条
ToolCallback 执行链）。建议在 Server 端工具命名里带上域前缀（如 `order_query`），
排查时一眼能区分工具来源。

### 3.4 必踩的坑：ToolCallbackProvider Bean 冲突

引入 starter 前先看项目现状——[SpringAiConfig.java](../../../ai-cs-chat/src/main/java/com/aics/chat/config/SpringAiConfig.java)
里定义了**唯一的** `ToolCallbackProvider`：

```java
@Bean
public ToolCallbackProvider toolCallbackProvider(OrderQueryService orderQueryService,
                                                 Nl2SqlQueryService nl2SqlQueryService) {
    return MethodToolCallbackProvider.builder()
            .toolObjects(orderQueryService, nl2SqlQueryService)
            .build();
}
```

而 [ChatModelRegistry](../../../ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ChatModelRegistry.java)
是**按单个类型**注入的：`private final ToolCallbackProvider toolCallbackProvider;`。

开启 MCP client 后，自动配置又注册了一个 `SyncMcpToolCallbackProvider`——
容器里出现两个同类型 Bean，`ChatModelRegistry` 按类型注入会直接
`NoUniqueBeanDefinitionException` 启动失败。

**解法 A（推荐）：改为注入集合，多来源天然共存**

```java
// ChatModelRegistry 构造参数改为 List，Spring 会注入容器里所有 ToolCallbackProvider
private final List<ToolCallbackProvider> toolCallbackProviders;

// 挂载时逐个注册（ChatClient.Builder 的 defaultToolCallbacks 调用多次会累加）
if (definition.getCapabilities().contains(ModelCapability.TOOL_CALLING)) {
    for (ToolCallbackProvider provider : toolCallbackProviders) {
        builder = builder.defaultToolCallbacks(provider);
    }
}
```

**解法 B：关掉自动集成，手动聚合**

```yaml
spring:
  ai:
    mcp:
      client:
        toolcallback:
          enabled: false   # 不让自动配置生成第二个 ToolCallbackProvider
```

```java
// 在 SpringAiConfig 里把本地 @Tool 和 MCP 工具合并成一个 provider
@Bean
public ToolCallbackProvider toolCallbackProvider(OrderQueryService orderQueryService,
                                                 Nl2SqlQueryService nl2SqlQueryService,
                                                 List<McpSyncClient> mcpClients) {
    MethodToolCallbackProvider local = MethodToolCallbackProvider.builder()
            .toolObjects(orderQueryService, nl2SqlQueryService).build();
    if (mcpClients.isEmpty()) {
        return local;
    }
    SyncMcpToolCallbackProvider mcp = new SyncMcpToolCallbackProvider(mcpClients);
    // ToolCallbackProvider 是函数式接口，直接把两个来源拼成一个
    return () -> {
        ToolCallback[] a = local.getToolCallbacks();
        ToolCallback[] b = mcp.getToolCallbacks();
        ToolCallback[] all = new ToolCallback[a.length + b.length];
        System.arraycopy(a, 0, all, 0, a.length);
        System.arraycopy(b, 0, all, a.length, b.length);
        return all;
    };
}
```

> 解法 A 胜在语义清晰（"所有工具来源平权"）；解法 B 胜在对现有单注入点零改动。
> 二选一即可，不要同时上 `@Primary` 魔法——工具来源多了以后，隐式优先级是排查噩梦。

### 3.5 工具过滤与命名：多 Server 接入的两个细节

**① 工具名冲突**：多个 Server 可能提供同名工具（都叫 `search`）。
自动配置的 `DefaultMcpToolNamePrefixGenerator` 会保证唯一：非字母数字统一转下划线
（`my-tool` → `my_tool`），重名的加计数前缀（第二个 `search` → `alt_1_search`），
总长截到 64 字符。注意 **LLM 看到和调用的都是改名后的名字**，日志对不上原始名先想到这个。
也可以自定义 Bean 换掉默认策略（比如统一加 `serverName_` 前缀），或用
`McpToolNamePrefixGenerator.noPrefix()` 关掉前缀（多 Server 同名会直接抛异常，慎用）。

**② 工具过滤**：不希望某类工具进入模型上下文时，实现一个 `McpToolFilter`：

```java
@Component
public class OrderDomainOnlyToolFilter implements McpToolFilter {
    @Override
    public boolean test(McpConnectionInfo info, McpSchema.Tool tool) {
        // 只放行订单域工具，其余（文件、实验性工具）不暴露给模型
        return tool.name().startsWith("order_");
    }
}
```

这个过滤器解决的不只是洁癖问题，而是下一节要讲的**上下文膨胀与攻击面**问题。

### 3.6 流式链路的性能注意

项目的 SSE 流式对话里，工具回调是在订阅线程上**同步执行**的（见
[ResilientAiService](../../../ai-cs-chat/src/main/java/com/aics/chat/service/impl/ResilientAiService.java)
注释：工具回调在订阅线程执行，执行期间不产生 content chunk）。MCP 的 SYNC 客户端
正是阻塞式 HTTP/stdio 往返，意味着：

- 远端工具慢 = 用户的流式输出"卡住"，所以 `request-timeout` 要按最慢工具压测后设置；
- 可能多轮"工具→模型→工具"循环，每轮都是一次 MCP 往返，端到端延迟按乘法估；
- 生产上远程连接建议用 `spring-ai-starter-mcp-client-webflux`（非阻塞传输），
  长任务配合 MCP 的 progress 通知（`progressToken`）向用户反馈进度。

---

## 四、Spring AI 集成 MCP Server：把自己的能力共享出去

### 4.1 starter 与协议选择

| 想暴露成什么 | 依赖 | 配置 |
|---|---|---|
| 本地 STDIO 服务器（给 Claude Desktop 等 Host 用） | `spring-ai-starter-mcp-server` | `spring.ai.mcp.server.stdio=true` |
| 远程 HTTP 服务器（WebMVC 栈，本项目风格） | `spring-ai-starter-mcp-server-webmvc` | `spring.ai.mcp.server.protocol=STREAMABLE` |
| 同上，无状态变体（K8s 多副本友好） | `spring-ai-starter-mcp-server-webmvc` | `spring.ai.mcp.server.protocol=STATELESS` |
| 远程 HTTP 服务器（WebFlux 栈） | `spring-ai-starter-mcp-server-webflux` | `protocol=STREAMABLE` / `STATELESS` |

另有 `protocol=SSE`（旧协议，为兼容存量客户端保留）；`spring.ai.mcp.server.type`
可选 SYNC/ASYNC（默认 SYNC，且 SYNC 服务器只注册同步方法，ASYNC 只注册异步方法）。
Tools/Resources/Prompts 等能力默认全开，可用 `spring.ai.mcp.server.capabilities.*` 收缩。

### 4.2 注解式开发：@McpTool 对比项目里的 @Tool

服务端注解和 Spring AI 本地工具注解几乎是"同一套思想的两个方言"：

```java
// 本地工具（项目现状，OrderQueryService）
@Tool(description = "根据订单号查询订单详情，包括订单状态、商品信息、金额、物流等")
public OrderVO queryOrderById(
        @ToolParam(description = "订单编号，格式如 ORD20260809001") String orderId) { ... }

// MCP Server 工具（暴露给任意 Host）
@McpTool(name = "order_query", description = "根据订单号查询订单详情，包括订单状态、商品信息、金额、物流等")
public String queryOrder(
        @McpToolParam(description = "订单编号，格式如 ORD20260809001", required = true) String orderId) {
    return orderService.getOrderJson(orderId);   // 建议返回 JSON 字符串，对模型最稳
}
```

除了 `@McpTool`，还有 `@McpResource`（URI 模板暴露数据）、`@McpPrompt`（暴露提示词模板）、
`@McpComplete`（参数自动补全）。带注解的 Bean 会被自动扫描注册，无需手工声明。

### 4.3 把 ai-cs-chat 包成"订单 MCP Server"的最小步骤

1. 新建独立模块（如 `ai-cs-mcp-server`，**不要**直接塞进 `ai-cs-chat`——工具暴露边界应与对话服务解耦）；
2. 引入 `spring-ai-starter-mcp-server-webmvc`，配置：

```yaml
spring:
  ai:
    mcp:
      server:
        name: aics-order-mcp-server
        version: 1.0.0
        protocol: STATELESS     # K8s 多副本部署选无状态，见 4.5
server:
  port: 8085
```

3. 写 `@McpTool` 方法（复用 `ai-cs-order` 的查询逻辑，或走内网 RPC 调用）；
4. 任何 MCP Host（Claude Desktop、其他 Spring AI 应用、IDE 工具）配上
   `url: http://mcp-server:8085` 即可使用订单查询能力。

### 4.4 安全红线：默认是"裸奔"的（官方文档原话级警告）

Spring AI 官方文档明确警告：**HTTP 类传输（SSE / Streamable HTTP / STATELESS）默认暴露
一个无鉴权的 JSON-RPC 端点**——任何能访问到端点的客户端都能枚举并调用你注册的全部工具。
把工具注册当成"决定暴露什么"的决策，上线前必须：

| 措施 | 做法 |
|---|---|
| 网络边界 | MCP Server 只挂内网/容器网络，公网入口一律经 `ai-cs-gateway` + 鉴权过滤器 |
| 应用层鉴权 | Spring Security 校验令牌；1.1 的 Streamable HTTP 可通过 `TransportContextExtractor` 把请求头（如 `Authorization`）注入工具执行上下文 |
| 最小工具集 | 用 `McpToolFilter`（客户端侧）/ capability 配置（服务端侧）收缩暴露面 |
| 参数校验 | 模型（或恶意客户端）传什么参数不可信，服务端工具内必须按业务规则校验 |

STDIO 传输是本地子进程、不走网络，没有此问题——这也是"个人助手工具尽量 STDIO"的原因。

### 4.5 用户上下文：ThreadLocal 在这里失效

项目里本地 `@Tool` 方法靠 `ChatUserContext`（ThreadLocal）拿当前用户
（Controller 线程 → Service → `@Tool` 方法同一根线程）。**MCP Server 是另一个进程，
ThreadLocal 天然传不过去**，远端工具方法里读 `ChatUserContext.getUserId()` 会拿到 null。

正确姿势：把用户身份**显式**作为工具参数或请求元数据传递，例如工具入参带 `userId`，
或客户端通过 ToolContext → MCP `_meta` 通道透传（自动配置支持自定义
`ToolContextToMcpMetaConverter`）。服务端按传来的身份做数据权限过滤——
这与本地工具"隐式取上下文"的习惯相反，评审时要专门盯这一点。

---

## 五、MCP vs 直接 Function Calling：怎么选

| 维度 | 本地 `@Tool`（Function Calling） | MCP |
|---|---|---|
| 工具定义 | 应用代码里硬编码 | 独立 Server 提供，`tools/list` 动态发现 |
| 复用范围 | 仅本应用 | 任何支持 MCP 的 Host（跨语言跨框架） |
| 部署 | 同进程，零网络开销 | 独立进程/服务，多一跳网络 |
| 用户上下文 | ThreadLocal 直达 | 必须显式传参（见 4.5） |
| 权限模型 | 应用内自定义 | 协议层 + 应用层共同承担 |
| 失败面 | 方法调用异常 | 网络/超时/会话/协议版本 全都要考虑 |
| 适合 | 本服务强内聚的领域操作（查订单、问数） | 跨应用复用、三方生态接入、个人助手本地工具 |

**决策树：**

```
工具只被 ai-cs-chat 自己用？ ──是──▶ 本地 @Tool（现状，别折腾）
        │否
工具要被多个应用/外部 Host 复用？ ──是──▶ 包成 MCP Server（STREAMABLE/STATELESS）
        │否
只是想给客服加个现成能力（文件搜索/Git/搜索）？ ──▶ 接现成 MCP Server（client starter）
```

一句话：**MCP 不替代 Function Calling**——模型侧的"函数调用"机制没有变，
MCP 解决的是工具的**分发、发现与复用**这一层工程问题（面试这么答最稳）。

---

## 六、生产化检查清单

- [ ] **鉴权**：HTTP 传输默认无鉴权（4.4），经网关 + 令牌；OAuth 2.1 是 2025-03-26 起的规范方向
- [ ] **超时与重试**：`request-timeout`（默认 20s）按最慢工具压测；工具失败是否重试要幂等先行
- [ ] **上下文膨胀**：每个工具的 JSON Schema 都会进入 prompt，接三五个 Server 工具数可能上百，
      token 成本和模型选择错误率双升——`McpToolFilter` 收缩 + 只对 `TOOL_CALLING` 模型挂载（项目已有能力门控）
- [ ] **可观测性**：MCP 工具走同一条 ToolCallback 链，`TraceContext` 的 tools 段自动覆盖；
      工具名带域前缀便于区分来源
- [ ] **多副本部署**：Streamable HTTP 带会话（`Mcp-Session-Id`），负载均衡需会话粘滞；
      微服务多副本场景直接选 `STATELESS` 协议
- [ ] **协议版本**：握手时客户端/服务器协商版本，升级 Server/SDK 前确认对端支持
- [ ] **变更感知**：Server 工具增删会发 `list_changed` 通知，客户端侧消费这些通知刷新工具注册表

---

## 七、常见误区速查

| 误区 | 现实 |
|---|---|
| "MCP 是用来替代 Function Calling 的" | 模型侧永远是 function calling；MCP 管的是工具分发与复用这一层（§五） |
| "接了 MCP Server 就很安全" | HTTP 传输默认零鉴权，攻击面反而变大（§4.4） |
| "stdio 配置照抄 Linux 写法" | Windows 上 npx/npm/python 是 `.cmd`，必须 `cmd.exe /c` 包装（§3.2） |
| "工具名就是我定义的那个" | 多 Server 同名会被改名成 `alt_1_xxx`，LLM 看到的是改名后的（§3.5） |
| "SYNC 和 ASYNC 客户端可以混着用" | starter 全局二选一，且只注册对应风格的注解方法（§3.1/§4.1） |
| "MCP Server 塞进现有微服务里最省事" | 暴露边界与业务服务耦合，鉴权、扩缩容互相绑架（§4.3） |
| "工具越多 AI 越聪明" | Schema 挤占上下文窗口 + 选择错误率上升，要做过滤（§六） |
| "远端工具能像本地 @Tool 一样读 ThreadLocal" | 跨进程上下文失效，身份必须显式传参（§4.5） |

---

## 八、动手练习

1. **接入现成 Server（30 min）**：按 §3.2 配置 filesystem MCP Server（Windows 记得
   `cmd.exe /c`），让客服机器人"读 D:/docs 下的 README 并总结"。重点验证 §3.4 的
   Bean 冲突现象与解法 A。
2. **自建 MCP Server（1 h）**：按 §4.3 把订单查询包成 `@McpTool`，起一个最小 Spring Boot
   应用作为 MCP Client 消费它，抓包观察 `initialize → tools/list → tools/call` 三条消息。
3. **工具治理（30 min）**：写一个只放行 `order_` 前缀的 `McpToolFilter`，对比过滤前后
   LLM 请求体大小（工具 Schema 的 token 占用），体感"上下文膨胀"。

---

## 九、面试衔接

速记问答在 [`../面试题/05-AI-Agent与工具调用.md`](../面试题/05-AI-Agent与工具调用.md) §5.4（Q7/Q8）。
按四层模板的组织方式：

- **结论**：MCP 是 Anthropic 2024 年开源的协议，标准化 AI 应用与工具/数据源的连接，把 M×N 集成变成 M+N；
- **原理**：JSON-RPC 2.0 消息 + 三种传输（STDIO/SSE/Streamable HTTP）+ 六大原语（Tools/Resources/Prompts/Sampling/Roots/Elicitation），工具启动时 `tools/list` 发现、运行时 `tools/call` 调用；
- **权衡**：相比进程内 Function Calling，MCP 换来跨应用复用与动态发现，代价是网络跳数、鉴权面、上下文管理复杂度；
- **实践**：Spring AI 1.1 的 client/server starter 把两端都收敛成 `ToolCallbackProvider` / `@McpTool` 两个概念——本项目挂载点在 `ChatModelRegistry`，接入时注意多 Provider Bean 冲突与 ThreadLocal 失效两个坑。

---

## 十、本项目锚点速查

| 通用概念 | 本项目对应 |
|---|---|
| 工具挂载点 | `ChatModelRegistry.rebuild()`：`defaultToolCallbacks` 仅挂给 `TOOL_CALLING` 能力模型（`ai-cs-chat/.../modelrouter/`） |
| 本地工具来源 | `SpringAiConfig.toolCallbackProvider()`：`MethodToolCallbackProvider` 包装 `OrderQueryService` / `Nl2SqlQueryService` 的 `@Tool` 方法 |
| 流式下的工具执行 | `ResilientAiService.callSseStream`：订阅线程同步执行 ToolCallback，期间无 content chunk |
| 用户上下文 | `ChatUserContext`（ThreadLocal）——MCP Server 进程内不可用，需显式传参 |
| 可观测性 | `TraceContext` tools 段（工具名/参数摘要/状态/耗时）天然覆盖 MCP 工具 |
| Spring AI 版本 | 1.1.4（父 `pom.xml` `spring-ai.version`，BOM 统一管理） |

---

## 十一、来源与更新

| 日期 | 变更 |
|---|---|
| 2026-09-07 | 初版：MCP 协议概念、Spring AI 1.1.x client/server 实战、本项目集成锚点与踩坑清单 |
