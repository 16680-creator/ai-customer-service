# 18-gRPC 与 Protobuf：从零开始理解跨语言 RPC（认知拓展，工程未落地）

> **定位**：本仓唯一的跨语言边界是 `ai-cs-py-chat`（FastAPI，OpenAI 兼容 HTTP/SSE 协议，见 [10-Python服务/01](../10-Python服务/01-FastAPI对话服务实战.md)）与 Java 服务之间——目前靠 **HTTP + JSON**。gRPC+Protobuf 是跨语言、低延迟、强契约的代表答案。本篇讲原理、建对照、给"如果用，接在哪"的评估；工程不引入。
> 前置阅读：[15-计算机基础/05-HTTP与TLS](../15-计算机基础/05-HTTP与TLS.md)（HTTP/2 多路复用）、[10-Python服务/01](../10-Python服务/01-FastAPI对话服务实战.md)、[04-中间件/15-Dubbo](15-Dubbo与RPC框架.md)（RPC 通用问题域）。

---

## ⚡ 30 秒速记卡

```text
① gRPC = Protobuf(契约+编码) + HTTP/2(传输) + 四种流模式(普通/服务端流/客户端流/双向流)
② Protobuf 快而小：二进制 tag-field_number-varint 编码，无字段名文本；比 JSON 小 3~10 倍
③ 兼容性铁律：字段号(field number)永不复用/不修改；只加不删（删了标 reserved）
④ 四种流：unary / server-streaming(≈你的 SSE) / client-streaming(≈批量上传) / bidi(聊天室)
⑤ HTTP/2 一个连接多条并发流(多路复用) → 解决 HTTP/1.1 队头阻塞；gRPC 的高并发底座
```

---

## 一、先搞懂 Protobuf：为什么它又小又快

JSON 的一条消息：

```json
{"productId": 1001, "quantity": 2}        // 33 字节，字段名全文本
```

Protobuf 的同一条消息（示意）：

```text
0x08 0xE9 0x07   0x10 0x02
 │     │          │    └─ value=2（varint）
 │     │          └─ tag: field=2, wire_type=0（varint）
 │     └─ 1001 的 varint 编码（3 字节：0xE9 0x07）
 └─ tag: field=1, wire_type=0
   → 总共 6 字节；接收方按"接口定义文件(.proto)"里 field 1=productId 还原
```

| 关键点 | 说明 | 记忆 |
|---|---|---|
| **field number 是契约** | 编码里没有字段名，只有编号 → 字段号**永不复用** | 删字段用 `reserved 3;` 占位，防止将来误用造成脏数据解析 |
| **wire type** | 0=varint / 1=64位 / 2=长度前缀（字符串/嵌套）/ 5=32位 | 类型变了也能按 wire type 读出"长度"，但语义兼容要守规则 |
| **varint** | 小数字用 1 字节，每字节最高位表"是否还有下一位" | 金额/ID 这类大数用 64 位类型（int64/sint64） |
| **默认值不传输** | int=0/string="" 不占字节 | 稀疏字段天然省空间 |

> **向后兼容一句话**：**加字段随便加（老端忽略未知号），改字段号=删旧加新，绝不复用。** 这是 Protobuf 能当"跨语言强契约"的原因。

---

## 二、gRPC：在 HTTP/2 上跑的方法调用

### 2.1 四种流模式（对照你已有的 SSE）

| 模式 | 形态 | 本仓对应物 |
|---|---|---|
| Unary | 一请求一响应 | `POST /cart/add` |
| **Server-streaming** | 一请求，N 个响应 | **`/chat/stream/sse`（SseEmitter 逐 token 推送）——语义完全同构** |
| Client-streaming | N 请求一响应 | 批量导入、埋点批量上报 |
| Bidi | 双向随时互发 | 语音对话、聊天室 |

> 所以你的 SSE 接口迁移到 gRPC **没有语义障碍**：`rpc ChatStream(ChatRequest) returns (stream ChatChunk)`。

### 2.2 HTTP/2 给了它什么

- **多路复用**：一条 TCP 连接上并发多条"流"，互不阻塞（对比 HTTP/1.1 同连接排队 → 你 [15-计算机基础/05](../15-计算机基础/05-HTTP与TLS.md) 学过的队头阻塞）；
- **HPACK 头压缩** + 二进制分帧；
- 副作用：gRPC 长连接 + HTTP/2 流控与 K8s/网关的**连接均衡**有坑（连接内均衡失效），生产要配客户端侧负载均衡——面试可当深挖点。

### 2.3 一段 .proto 契约（以你客服场景为例）

```protobuf
syntax = "proto3";
package aics.chat.v1;

service ChatService {
  rpc Ask (AskRequest) returns (AskReply);                    // unary：普通问答
  rpc AskStream (AskRequest) returns (stream ChatChunk);      // 服务端流：逐 token（=SSE 等价物）
}

message AskRequest {
  int64  user_id      = 1;   // 字段号是契约，永不复用
  string session_id   = 2;
  string message      = 3;
  string knowledge_base = 4;  // 可选字段：不传则不占字节
}
message ChatChunk {
  string delta        = 1;
  bool   done         = 2;
  repeated Citation citations = 3;   // repeated=数组
}
```

同一份 `.proto` 用 protoc 编译出 Java/Python 双端桩代码——**这就是"跨语言"的来源：契约先行，代码生成**。

---

## 三、生态配套：拦截器、超时、重试、负载均衡

| 能力 | gRPC 机制 | 本仓对应 |
|---|---|---|
| 链路上下文传递 | `Metadata`（HTTP/2 头）+ `Context` | `X-User-Id` 头 / Seata `TX_XID` / `TraceContextHolder` |
| 超时 | `deadline` 随调用链**向下传播**（父调用剩多少，子调用上限多少） | Feign 的超时各配各的（没有链路预算概念——这是 gRPC 更先进处） |
| 重试 | 服务端配置 retryPolicy（限 codes/attempts/退避） | Feign 默认不重试（[02-08](../02-Spring微服务/08-Feign熔断与降级语义.md) 的幂等取舍同样适用） |
| 负载均衡 | 客户端内建（round_robin/pick_first）或代理式 | SC Gateway/LoadBalancer |
| 鉴权 | TLS + 拦截器读 Metadata | 网关 JWT + `X-User-*` 透传 |

---

## 四、如果要引入本项目：评估结论（未落地）

| 维度 | 评估 |
|---|---|
| 最合理的接入点 | **Java ↔ py-chat** 的对话链路：gRPC server-streaming 与 SSE 同构，Protobuf 省 token 消息体积；以及 Agent 工具调用（`OrderLocatorTool` 等走 Feign 的） |
| 收益 | 强契约（proto 即文档）、跨语言生成、流式语义统一、deadline 链路传播 |
| 代价 | 网关透传（Nginx/SCG 需支持 gRPC）、调试门槛（要 grpcurl/反射）、前端浏览器无法直连（需 grpc-web 或继续 REST）、py 侧加 grpcio 依赖 |
| 决策口径 | **对外 API 永远 REST/SSE（生态与调试友好）；内部高密度跨语言链路才值得 gRPC**。当前 QPS 水平下，HTTP+JSON 不是瓶颈——属于"知识储备"而非"工程欠账" |

---

## 五、动手实验（15 分钟）

```bash
# 用 grpcurl 直接调一个公共测试服务（无需写代码即可体验契约式调用）
go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest
grpcurl -plaintext -d '{"message":"hi"}' \
  grpc.test.example.com:443 aics.chat.v1.ChatService/Ask

# 本地最小闭环：github.com/grpc/grpc-java/examples（hello world）
# ./gradlew installDist && ./build/install/examples/bin/hello-world-server
# ./build/install/examples/bin/hello-world-client   → 观察双向通信
```

---

## 六、与已学技术的关系图

```text
Feign(02-07)   同问题域：远程调用 —— gRPC 是"契约+二进制+HTTP/2"的答案
SSE(04-05)     server-streaming 的 HTTP 版 —— gRPC 流模式语义同构
15-计算机基础/05  HTTP/2 多路复用是 gRPC 的底座
02-10 自动装配   "契约先行+代码生成" vs "约定先行+反射"两种集成哲学
```

---

## 七、面试要点总结

```text
关键词：
Protobuf 编码：tag(field_number+wire_type)+varint · 默认值不传输 · 字段号永不复用(reserved)
四种流：unary/server/client/bidi —— server-streaming ≈ 本仓 SSE
HTTP/2 多路复用解队头阻塞 · deadline 随链路传播 · Metadata 传上下文（≈身份头/XID）
浏览器不能直连 gRPC（需 grpc-web）· K8s 下注意连接级均衡坑
项目锚点：py-chat OpenAI 兼容 HTTP/SSE（10-01）· Agent 工具 Feign 调用 · /chat/stream/sse
结论口径：对外 REST，内部高密度跨语言链路才上 gRPC；当前非瓶颈
```

## 学习检查清单

- [ ] 能手画"tag+varint"编码示意并解释为什么比 JSON 小
- [ ] 能背出向后兼容三规则（加字段/改号/reserved）
- [ ] 能把 SSE 接口翻译成 gRPC 流式方法签名
- [ ] 能说清 deadline 传播为什么比"各配各的超时"先进
- [ ] 能给本项目一套"何时上 gRPC"的触发条件

## 下一步

- [19-Debezium 与 Canal 的 CDC 对比](19-Debezium与Canal的CDC对比.md)：回到数据同步主题，把你已用的 Canal 放进坐标系；
- [10-Python服务/01-FastAPI对话服务实战](../10-Python服务/01-FastAPI对话服务实战.md)：现有跨语言边界的实现。
