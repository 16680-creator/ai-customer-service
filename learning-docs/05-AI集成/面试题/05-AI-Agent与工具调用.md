# 第5章：AI Agent 与工具调用

> AI Agent 是 2025~2026 年最热门的 AI 工程方向，面试频率极高。

---

## 5.1 Agent 基础概念

### Q1：什么是 AI Agent？它与普通 LLM 调用有什么本质区别？★★★★★

**参考答案：**

**AI Agent** 是能够自主规划、决策、使用工具来完成复杂任务的智能体。与普通 LLM 调用的本质区别：

| 维度 | 普通 LLM 调用 | AI Agent |
|------|--------------|----------|
| 执行方式 | 单轮：输入→输出 | 多轮：规划→执行→观察→反思 |
| 工具使用 | 无 | 可调用外部 API、数据库、代码 |
| 自主性 | 被动响应 | 主动规划、决策 |
| 记忆 | 无状态 | 短期+长期记忆 |
| 错误处理 | 失败就失败 | 自我纠错、重试 |

**Agent 核心组成：**
```
┌────────────────────────────────────────┐
│            AI Agent                    │
├────────────────────────────────────────┤
│  🧠 LLM（大脑）：推理和决策              │
│  📋 Planning（规划）：任务拆解            │
│  🔧 Tools（工具）：执行具体操作           │
│  💾 Memory（记忆）：短期+长期记忆         │
│  👁️ Perception（感知）：接收外部输入      │
└────────────────────────────────────────┘
```

---

### Q2：解释 ReAct 模式（Reasoning + Acting），它是如何工作的？★★★★★

**参考答案：**

**ReAct** 是最主流的 Agent 推理模式，将推理（Reasoning）和行动（Acting）交替进行。

**执行循环：**
```
Thought（思考）：我需要查询订单系统来找到这个订单
   ↓
Action（行动）：调用 getOrder(orderId="12345")
   ↓
Observation（观察）：{"status": "shipped", "tracking": "SF1234"}
   ↓
Thought（思考）：订单已发货，我需要用物流号查物流信息
   ↓
Action（行动）：调用 getTracking(trackingNo="SF1234")
   ↓
Observation（观察）：{"location": "北京转运中心", "status": "运输中"}
   ↓
Thought（思考）：我已经获取到所有信息，可以回答用户了
   ↓
Final Answer：您的订单12345已发货，目前在运输中，位于北京转运中心。
```

**Spring AI 中的 ReAct 实现：**
```java
// 通过 Function Calling 实现 ReAct 循环
String response = ChatClient.create(chatModel)
    .prompt()
    .system("你是一个客服助手，可以查询订单和物流信息。")
    .user("我的订单12345到哪了？")
    .functions("getOrder", "getTracking", "sendNotification")
    .call()
    .content();
// Spring AI 框架自动处理多轮工具调用循环
```

---

## 5.2 Function Calling 深入

### Q3：Function Calling 的底层通信机制是什么？★★★★★

**参考答案：**

**Function Calling 不是 LLM 直接执行函数**，而是一个协议：

```
Step 1：开发者注册函数定义（JSON Schema）给 LLM
┌─────────────────────────────────────────────┐
│ {                                           │
│   "name": "getWeather",                     │
│   "description": "查询城市天气",              │
│   "parameters": {                           │
│     "type": "object",                       │
│     "properties": {                         │
│       "city": {"type": "string"}            │
│     },                                      │
│     "required": ["city"]                    │
│   }                                         │
│ }                                           │
└─────────────────────────────────────────────┘
           ↓ 发送给 LLM
Step 2：LLM 判断需要调用函数，输出结构化参数
┌─────────────────────────────────────────────┐
│ {                                           │
│   "tool_calls": [{                          │
│     "function": "getWeather",               │
│     "arguments": "{\"city\": \"北京\"}"      │
│   }]                                        │
│ }                                           │
└─────────────────────────────────────────────┘
           ↓ 框架解析并执行
Step 3：Spring AI 框架执行函数，获取结果
           ↓ 结果回传给 LLM
Step 4：LLM 基于函数结果生成最终回答
```

**关键理解：**
- LLM 只是"决定调用什么函数、传什么参数"
- 实际执行由 Spring AI 框架在 Java 端完成
- 函数结果再传回 LLM，让 LLM 基于结果生成自然语言回答

---

### Q4：Function Calling 中如何设计好的函数定义？★★★★☆

**参考答案：**

函数定义的质量直接影响 LLM 的调用准确性。

**好的函数定义原则：**

```java
// ❌ 不好：描述模糊，参数不明确
@Description("处理数据")
public Function<DataRequest, DataResponse> processData() { ... }

// ✅ 好：描述清晰，参数有约束
@Description("根据订单ID查询订单详情，包括商品、价格、状态、物流信息")
public Function<OrderQueryRequest, OrderDetailResponse> getOrderDetail() { ... }
```

**设计原则：**
1. **函数名**：动词+名词，语义清晰（getOrder, sendEmail, searchProducts）
2. **描述**：详细说明功能、输入输出、使用场景
3. **参数**：
   - 类型明确（String vs Integer vs Enum）
   - 必填参数标注 required
   - 参数描述清晰（"城市名称，如：北京、上海"）
4. **粒度**：单一职责，一个函数做一件事
5. **返回值**：结构化、简洁，只返回 LLM 需要的信息

---

## 5.3 Multi-Agent（多智能体）

### Q5：什么是 Multi-Agent 系统？有哪些协作模式？★★★★☆

**参考答案：**

**Multi-Agent** 是多个 Agent 协作完成复杂任务的架构。

**常见协作模式：**

**模式1：监督者模式（Supervisor）**
```
用户请求 → Supervisor Agent（分配任务）
                ├── Agent A：订单查询
                ├── Agent B：物流跟踪
                └── Agent C：退款处理
                ↓
         Supervisor 汇总结果 → 返回用户
```

**模式2：链式模式（Sequential/Pipeline）**
```
用户请求 → Agent A（理解意图）→ Agent B（检索知识）→ Agent C（生成回答）→ Agent D（质量审核）
```

**模式3：辩论模式（Debate）**
```
问题 → Agent A（方案1）←→ Agent B（方案2）→ 投票/评估 → 最佳方案
```

**模式4：动态协作（Dynamic）**
```
Agent 之间通过消息总线通信，按需创建和销毁
适合复杂、不确定的任务场景
```

**Java 实现方式：**
- Spring AI + 自定义 Agent 编排框架
- LangGraph4j（LangGraph 的 Java 移植）
- Spring AI Alibaba（多 Agent 编排支持）

---

### Q6：Multi-Agent 与 Single Agent 如何选择？★★★★☆

**参考答案：**

| 场景 | 推荐方案 | 理由 |
|------|----------|------|
| 简单工具调用 | Single Agent | 简单直接，无额外开销 |
| 多领域任务 | Multi-Agent（Supervisor） | 各领域 Agent 专业化 |
| 需要审核/质量把关 | Multi-Agent（链式） | 审核 Agent 把关质量 |
| 复杂推理 | Multi-Agent（辩论） | 多视角减少错误 |
| 长流程任务 | Multi-Agent（Pipeline） | 每步可独立优化 |

**注意事项：**
- Multi-Agent 增加了延迟和复杂度
- Agent 间通信需要设计协议
- 需要处理 Agent 故障和超时
- 成本是 Single Agent 的 N 倍

---

## 5.4 MCP（Model Context Protocol）

### Q7：什么是 MCP（Model Context Protocol）？它解决了什么问题？★★★★★

**参考答案：**

**MCP（Model Context Protocol）** 是由 Anthropic 提出的开放协议，标准化了 LLM 与外部工具/数据源的连接方式。

**解决的问题：**
- **工具碎片化**：每个 AI 框架有自己的工具注册方式
- **重复开发**：同一工具需要在不同框架中重复实现
- **安全问题**：工具调用缺乏统一的权限控制

**MCP 架构：**
```
┌─────────────────────┐
│   MCP Host          │ ← AI 应用（如 Spring AI 应用）
│  ┌────────────────┐ │
│  │  MCP Client    │ │ ← 连接 MCP Server
│  └───────┬────────┘ │
└──────────┼──────────┘
           │ MCP 协议（JSON-RPC over stdio/SSE/HTTP）
    ┌──────┼──────────────────────┐
    │      │                      │
┌───┴──┐ ┌┴───────┐ ┌────────────┐
│MCP   │ │MCP     │ │MCP         │
│Server│ │Server  │ │Server      │
│(DB)  │ │(GitHub)│ │(FileSystem)│
└──────┘ └────────┘ └────────────┘
```

**核心概念：**
- **Tools**：Agent 可调用的函数
- **Resources**：Agent 可读取的数据源
- **Prompts**：预定义的提示词模板

**Spring AI 集成 MCP：**
```java
// 配置 MCP Client
McpClient mcpClient = McpClient.builder()
    .transport(new StdioClientTransport("npx", "-y", "@modelcontextprotocol/server-filesystem"))
    .build();

// 使用 MCP 工具
String response = ChatClient.create(chatModel)
    .prompt()
    .user("读取 /data/report.csv 文件并总结")
    .toolCallbacks(mcpClient.getTools())  // 获取 MCP 工具
    .call()
    .content();
```

---

### Q8：MCP 与直接 Function Calling 有什么区别？★★★★☆

**参考答案：**

| 维度 | Function Calling | MCP |
|------|-----------------|-----|
| 工具定义 | 在应用代码中硬编码 | 独立 MCP Server 提供 |
| 复用性 | 低（与框架绑定） | 高（标准协议，跨框架） |
| 部署 | 与应用同进程 | 独立进程/服务 |
| 权限控制 | 自定义 | MCP 协议级别控制 |
| 发现机制 | 静态配置 | 动态发现可用工具 |
| 生态 | 各自为政 | 统一生态（类似 USB） |

**类比理解：**
- Function Calling = 定制化工具接口
- MCP = 标准化的"USB 接口"，任何工具只要遵循 MCP 协议就能接入

---

## 5.5 Agent 设计模式

### Q9：Agent 开发中有哪些常见的设计模式？★★★★☆

**参考答案：**

**1. Router 模式（意图路由）**
```
用户输入 → 意图识别 LLM → 路由到对应处理链
├── "查订单" → 订单查询 Agent
├── "退款" → 退款处理 Agent
└── "投诉" → 投诉处理 Agent
```

**2. Planner 模式（任务规划）**
```
复杂任务 → Planner Agent 拆解为子任务
├── 子任务1：执行 → 结果
├── 子任务2：执行 → 结果（可能依赖子任务1）
└── 子任务3：执行 → 结果
→ 汇总所有结果
```

**3. Evaluator 模式（评估+自纠）**
```
生成回答 → Evaluator Agent 评估质量
├── 质量好 → 返回
└── 质量差 → 重新生成（带反馈）
```

**4. Orchestrator 模式（编排器）**
```
编排器 Agent 动态决定调用哪些工具和 Agent
根据中间结果动态调整执行计划
最灵活但最复杂
```

---

### Q10：如何设计一个生产级的 AI Agent？需要注意哪些问题？★★★★★

**参考答案：**

**生产级 Agent 设计清单：**

**可靠性：**
- [ ] 设置最大工具调用次数（防止无限循环）
- [ ] 工具调用超时控制（防止死等）
- [ ] 错误重试机制（工具调用失败时）
- [ ] Fallback 降级（Agent 失败时转人工/返回默认回答）

**安全性：**
- [ ] 工具权限控制（Agent 只能调用授权的工具）
- [ ] 参数校验（防止 LLM 传入恶意参数）
- [ ] 敏感操作确认（如删除、支付需要人工确认）
- [ ] 审计日志（记录所有工具调用）

**性能：**
- [ ] 工具调用并行化（无依赖的工具并行执行）
- [ ] 结果缓存（相同参数的调用复用缓存）
- [ ] 流式输出（边推理边返回）

**可观测性：**
- [ ] 每轮 Thought/Action/Observation 日志
- [ ] Token 使用量监控
- [ ] 端到端延迟监控
- [ ] 工具调用成功率监控

---

## 本章小结

**必背 TOP 5：**
1. AI Agent 核心组成和 ReAct 模式
2. Function Calling 底层通信机制
3. MCP 协议概念和解决的问题
4. Multi-Agent 协作模式
5. 生产级 Agent 设计清单
