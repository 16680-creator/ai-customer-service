# HTTP 与 TLS

> 对应项目：`ai-cs-gateway/src/main/resources/application.yml:1-3`（网关统一入口 8080）、
> `ai-cs-gateway/src/main/java/com/aics/gateway/config/RouteConfig.java`（路由与韧性三层）、
> `ai-cs-frontend/src/views/ChatView.vue`（SSE 客户端解析）、`ai-cs-notify/src/main/java/com/aics/notify/config/WebSocketConfig.java`。
> **划界声明**：[04-中间件/05-SSE与WebSocket实时通信](../04-中间件/05-SSE与WebSocket实时通信.md) 讲 SSE/WS 的用法与选型，[01-Java基础/07-IO模型与Netty基础](../01-Java基础/07-IO模型与Netty基础.md) §5.1 贴过 SSE 帧格式；本篇讲 **HTTP 协议层**——keep-alive、HTTP/2 队头阻塞、SSE 分帧与网关透传、WS 升级握手、TLS 握手与证书链。

---

## 一、项目请求全景：为什么一切从 8080 说起

```
浏览器(5173, Vue3) ──HTTP──▶ 网关 :8080 ──lb://──▶ 各业务服务
                              │ RouteConfig.java:81-84
                              │   /api/chat/** → lb://ai-cs-chat（stripPrefix(1) → /chat/**）
                              │   /api/notify/** → lb://ai-cs-notify（:119-122）
                              └──── 统一鉴权（AuthFilter）/限流（RequestRateLimiter）/熔断（cb-*）
```

- 前端真实调用：`ChatView.vue:370` `fetch(\`${GATEWAY}/api/chat/${endpoint}?${params}\")`——SSE 也走网关；
- 网关端口：`ai-cs-gateway/application.yml:3` `port: 8080`；
- CORS 全局配置在 Nacos 侧 `tools/nacos-config/ai-cs-gateway.yml`（`globalcors`，`allowed-origin-patterns: "*"` + `allow-credentials: true`）——浏览器发跨域 SSE 前会先有 OPTIONS 预检。

### 1.1 本项目高频状态码的实际语义

| 状态码 | 谁发的 | 项目现场 |
|---|---|---|
| 200 | 各服务 | 普通响应与 SSE 首包（流式响应也是先 200 再持续吐帧） |
| 401/403 | 网关 AuthFilter | JWT 缺失/过期、API Key 不匹配 |
| 429 | 网关 RequestRateLimiter | 令牌耗尽（`replenish-rate=5/burst=10`）；`ChatController.chatSendBlocked`（`:100-104`）在 Sentinel 拦截时也返回 429 语义业务码 |
| 5xx → forward:/gateway-fallback | 网关 CircuitBreaker | 熔断打开时统一降级响应（`RouteConfig.java:179-181`） |
| 101 Switching Protocols | notify 服务 | WebSocket 升级握手（§五） |
| 307/302 | （本项目未用） | 若做 http→https 跳转会用到，加 TLS 时再回来看 |

协议视角下，这条链路涉及 HTTP/1.1 长连接、流式响应（SSE）、协议升级（WS）、（未来某天）TLS。逐一拆开。

---

## 二、HTTP/1.1 keep-alive 与它的队头阻塞

### 2.1 keep-alive 解决了什么

```
HTTP/1.0：每请求一次 TCP 握手 + 慢启动（见 04-TCP §八）+ 四次挥手 → 页面几十个资源就是几十次建连
HTTP/1.1：Connection: keep-alive（默认开启）→ 一条 TCP 连接上串行复用多个请求
```

收益都是 TCP 层的：省握手 RTT、复用已爬坡的拥塞窗口、避免 TIME_WAIT 堆积（[04-TCP协议实战](./04-TCP协议实战.md) §四）。

### 2.2 没解决的：队头阻塞（HOL Blocking）

HTTP/1.1 允许 pipeline（不等响应连发请求），但**响应必须按请求顺序返回**——第一个响应慢，后面的全排队。更普遍的现实是：keep-alive 下浏览器对同一域名开 6 条连接串行处理，一条被大响应占住就是少一条可用连接。

**项目现场**：SSE 对话流就是"最占"的那种响应——一条流持续几秒到几分钟。k6 的 `sse-chat.js` 用 20 VU 各持一条流，若 HTTP/1.1 连接池只有 6 条复用连接，第 7 个 VU 就得等前一条流结束——这正是压测脚本报"请求排队"时该想到的第一原因。

---

## 三、HTTP/2 与 HTTP/3：多路复用与剩余的阻塞

| 能力 | HTTP/1.1 | HTTP/2 | HTTP/3（QUIC） |
|---|---|---|---|
| 传输层 | TCP | TCP | **UDP** |
| 多路复用 | 无（串行/6 连接） | ✅ 二进制分帧，一条连接多个 Stream 并发 | ✅ 且流之间**独立重传** |
| 头部 | 明文，重复头多 | HPACK 压缩 + 头表 | QPACK |
| 队头阻塞 | 应用层串行 | 应用层解决，**TCP 层仍在**：丢一个包，所有 Stream 等重传 | ✅ 消除：丢包只阻塞受影响的流 |
| 加密 | 可选 | 事实上强制 TLS | TLS 1.3 内建于握手 |
| 连接建立 | TCP(1RTT)+TLS(1~2RTT) | 同左 | **0/1-RTT**（会话恢复时 0-RTT 发数据） |

**QUIC 一句话**：把"可靠传输"从内核 TCP 搬到用户态 UDP 上，换来流级独立重传 + 更快握手 + 连接迁移（换 Wi-Fi 不断流）。

**项目现状（诚实说明）**：全链路 HTTP/1.1（`text/event-stream` 响应、Nacos/MySQL 客户端均 HTTP/1.1 场景），网关与前端之间未启用 h2c/TLS；SSE 单向推送对队头阻塞不敏感（本来一问一答一条流），所以未升级是合理现状，不必为了"先进"上 HTTP/2。

---

## 四、SSE 协议细节：`text/event-stream` 的分帧

### 4.1 响应头与事件帧

```
HTTP/1.1 200 OK
Content-Type: text/event-stream        ← 协议开关
Cache-Control: no-cache                ← 禁止中间缓存
Connection: keep-alive

data: {"content":"你"}                 ← 事件 = 若干 "字段: 值" 行 + 一个空行
                                       ← 空行是帧边界（协议唯一分帧手段）

event: done                            ← 事件类型（缺省 message）
data: {"done":true,"citations":[...]}

id: 42                                 ← 配合 retry: 实现断线重连 Last-Event-ID
retry: 3000
```

规则速记：**字段只有 `data/event/id/retry` 四个；冒号后空格可选；一行太长可拆多行 `data:`；空行 = 事件结束**。

### 4.2 项目为什么用 fetch 而不是 EventSource

浏览器原生 `EventSource` 有两个硬限制：**只支持 GET、不能带自定义请求头**。而项目对话接口是 `POST /api/chat/stream/sse` 且要带 `Authorization: Bearer`（`ChatView.vue:370-373`）——所以前端用 `fetch` + `ReadableStream` 手工解协议：

```js
// ai-cs-frontend/src/views/ChatView.vue:376-394（节选）
const reader = resp.body.getReader()
let buffer = ''
while (true) {
  const { done, value } = await reader.read()      // TCP 字节流（无边界！）
  if (done) break
  buffer += decoder.decode(value, { stream: true })
  let sep
  while ((sep = buffer.indexOf('\n\n')) >= 0) {    // 空行 = SSE 事件边界
    const rawEvent = buffer.slice(0, sep); buffer = buffer.slice(sep + 2)
    for (const line of rawEvent.split('\n')) {
      if (!line.startsWith('data:')) continue      // 只认 data: 行
      const obj = JSON.parse(line.slice(5).trim())
      ...
```

三个值得说的点：

| 点 | 协议含义 |
|---|---|
| `buffer.indexOf('\n\n')` | SSE 的**空行分帧**就是它的"防粘包"手段——把 [04-TCP协议实战](./04-TCP协议实战.md) §九的字节流切成事件 |
| `decoder.decode(value, { stream: true })` | 必须流式解码：多字节 UTF-8（中文/emoji）可能被 TCP 分包截断在中间，`stream: true` 会保留半个字符等下一包（编码细节见 [07-字符编码与时区](./07-字符编码与时区.md)） |
| 手工解析而非 `EventSource` | 换来 POST + 鉴权头能力，代价是自己处理重连（`EventSource` 内置自动重连 + Last-Event-ID 就用不上了） |

Python 侧对照组：`ai-cs-py-chat/app/api/chat.py` 的 `StreamingResponse(event_gen(), media_type="text/event-stream")` 逐条 `yield "data: ...\n\n"`——分帧语义与 Java 版完全一致。

### 4.3 网关怎么透传 SSE（真实路由的证据）

SSE 路由就是普通路由：`RouteConfig.java:81-84` 把 `/api/chat/**`（含 `/stream/sse`）转发到 `lb://ai-cs-chat`，**没有任何 SSE 专属配置**。链路上真正与 SSE 相关的是这三层韧性过滤器（`:176-185`）：

| 过滤器 | 对 SSE 的影响 |
|---|---|
| `retry`（`:176-178`，**仅 GET**、仅 500/502） | SSE 是 POST → **天然不会被网关重试**。这是对的：流式请求重试会造成重复推送/重复计费 |
| `circuitBreaker`（`:179-181`，fallback → `forward:/gateway-fallback`） | 熔断打开时流式请求直接落到降级响应，用户看到的是 JSON 兜底而非 SSE 流——前端按 `!resp.ok` 或非 event-stream 处理即可 |
| `requestRateLimiter`（`:181-185`，Redis 令牌桶） | 按 `userKeyResolver` 的用户/IP 维度限流，SSE 与普通请求共享配额 |

另两条工程经验（对照项目现状）：① 若中间架 Nginx，必须 `proxy_buffering off`，否则流被攒成一坨再吐（本项目前端 5173 直连 8080，无此层）；② 空闲断流问题（NAT/设备老化）属传输层，见 [04-TCP协议实战](./04-TCP协议实战.md) §6.3。

---

## 五、WebSocket 握手：一次 HTTP 升级

```
客户端请求（就是普通 HTTP）：
  GET /ws/notify HTTP/1.1
  Upgrade: websocket
  Connection: Upgrade
  Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==

服务端响应：
  HTTP/1.1 101 Switching Protocols
  Upgrade: websocket
  Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
      = base64( SHA-1( Key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11" ) )
```

- **握手之后这条 TCP 连接不再是 HTTP**：改为 WebSocket 帧协议（帧头自带长度，天然无粘包），全双工；
- 固定魔数 GUID 参与计算，防止普通 HTTP 服务误应答（RFC 6455）；
- 项目现场：`WebSocketConfig.java:34-36` 注册 `/ws/notify` 并用 `setAllowedOriginPatterns` 限源（注释明说禁止 `*` 防跨站伪造 userId）；
- **两个值得核对的现状**：① 前端直连 `ws://localhost:8085/ws/notify`（`NotifyView.vue:114`）**绕过了网关**，而 8085 是 message 服务、notify 实际是 8086（`ai-cs-notify/application.yml:3`）——这条直连+疑似端口笔误是真实的接线现状，改造 WS 时先收敛到"网关透传 WS（`/ws/**` 路由 + 不加会破坏升级的过滤器）"再谈其他；② WS 走网关时，重试/改写响应体类的过滤器对 101 握手是危险的。

---

## 六、TLS 1.2 vs 1.3 与证书链

### 6.1 握手对比

```
TLS 1.2（2-RTT）                          TLS 1.3（1-RTT，会话恢复 0-RTT）
ClientHello（支持的套件）                 ClientHello + KeyShare（把密钥交换直接带上）
  ◀— ServerHello + 证书 + ServerKeyEx —    ◀— ServerHello + KeyShare + 证书 + Finished
       换密钥 → Finished                    密钥已可算出 → Client Finished + 应用数据
  → Finished                               （1-RTT 即可发数据）
→ 应用数据
```

| 变化 | 1.2 | 1.3 |
|---|---|---|
| 往返 | 2-RTT | **1-RTT**（恢复 0-RTT） |
| 密钥交换 | RSA/静态 DH 可选（可被解密历史流量） | **强制（EC）DHE → 前向保密** |
| 对称算法 | CBC 模式可选 | 只剩 AEAD（AES-GCM / ChaCha20-Poly1305） |
| 压缩/重协商 | 有（历史漏洞源） | 移除 |

### 6.2 证书链

```
根 CA（操作系统/浏览器内置，离线保存）
  └─ 中间 CA（签发机构，随服务器下发）
       └─ 叶子证书（你的域名，CN/SAN 匹配 + 有效期）
服务端下发"叶子 + 中间"两级，客户端沿链验证到内置根：域名匹配 → 有效期 → 签名逐级验 → 未吊销
```

### 6.3 网关终止 TLS（TLS Termination）

```
浏览器 ──TLS(443)──▶ 入口卸载点 ──明文 HTTP──▶ 内网服务
```

- 在入口统一做证书、握手、卸载，内网继续明文——省去每服务配证书，也保住网关（需要读 URL/头做路由与鉴权）的可观测性；
- **项目现状（grep `deploy/k8s` 与网关配置确认）**：全链路 HTTP，无 Ingress/TLS/证书资源，网关 8080 明文。若要加 TLS，正确位置是网关入口（Spring Cloud Gateway 配 `server.ssl` 监听 443）或其前置 LB/Ingress 终止；SSE 与 WS 在 TLS 下自动是 `https://` / `wss://`，协议本身不变。

### 6.4 会话恢复：TLS 的"连接池"

TLS 1.2 用 session ID/ticket、1.3 用 PSK 恢复会话——跳过证书验证与密钥交换，1.3 恢复时甚至 0-RTT 发数据。对本项目的意义：若未来加 TLS，**客户端连接池（HTTP keep-alive + TLS 会话恢复）能省掉大部分握手成本**，这正是 HTTP 层连接复用价值的叠加强化；但 0-RTT 数据有重放风险，只允许幂等 GET。

---

## 七、面试高频问答

**Q1：HTTP/1.1 keep-alive 解决了什么？没解决什么？**
A：解决重复建连：一条 TCP 复用多个请求，省握手 RTT、复用拥塞窗口、减少 TIME_WAIT。没解决队头阻塞——响应必须按序返回（或 6 条连接串行），一个慢响应（如长 SSE 流）占住连接，后面的排队。

**Q2：HTTP/2 的多路复用怎么工作？队头阻塞彻底解决了吗？**
A：二进制分帧把每个请求/响应拆成带 Stream ID 的帧，一条 TCP 连接并发多流 + HPACK 头压缩。应用层 HOL 解决了，但 TCP 层没解决：一个包丢失，所有 Stream 必须等它重传（TCP 是全局有序字节流）。彻底解决要 HTTP/3/QUIC（流级独立重传）。

**Q3：SSE 的帧怎么界定？和 TCP 粘包什么关系？**
A：SSE 用空行作为事件边界，事件内是 `data/event/id/retry` 字段行。TCP 只保证字节流有序到达、不管消息边界，所以客户端要缓存字节流并按 `\n\n` 切分（ChatView.vue:388），这就是应用层定界/防粘包，与 Netty 的分隔符 Decoder 同思想。

**Q4：为什么本项目用 fetch+ReadableStream 而不用 EventSource？**
A：EventSource 只支持 GET 且不能带自定义头，而对话接口是 POST 且要 `Authorization: Bearer` 头。用 fetch 拿 `resp.body.getReader()` 手工解析 `text/event-stream`，代价是自己处理重连与超时。

**Q5：网关透传 SSE 要注意什么？本项目怎么配的？**
A：①不能缓冲响应（Nginx 场景 `proxy_buffering off`）；②不要对流式请求做重试——本项目 RouteConfig 的 retry 限 GET（:176-178），SSE 是 POST 天然不重试，正确；③熔断 fallback 会把流式请求换成 JSON 兜底，前端要能处理非流响应；④限流按用户/IP 共享配额（RequestRateLimiter）。

**Q6：WebSocket 的握手过程？它还是 HTTP 吗？**
A：借 HTTP 发 `Upgrade: websocket` + `Sec-WebSocket-Key`，服务端回 `101 Switching Protocols` + `Sec-WebSocket-Accept`（base64(SHA1(Key+固定 GUID))，防误应答）。之后同一 TCP 连接切换为 WebSocket 帧协议，全双工、帧自带长度。握手是 HTTP，握手后不是。

**Q7：TLS 1.3 比 1.2 快在哪、安全在哪？**
A：快在握手 2-RTT → 1-RTT（会话恢复 0-RTT）：ClientHello 直接带 KeyShare，一轮就能发数据。安全在：密钥交换强制 (EC)DHE（前向保密，私钥泄露也解不开历史流量）、砍掉 CBC/压缩/重协商、只留 AEAD 算法。

**Q8：证书链怎么验证？为什么根证书要预置？**
A：服务端下发叶子+中间证书，客户端用中间证书公钥验叶子签名、用内置根 CA 验中间签名，逐级到根；同时校验域名（SAN）、有效期、吊销状态。根证书必须离线预置在系统/浏览器信任库——否则"信任的起点"本身无法自证。

**Q9：什么是 TLS 终止？本项目要加 TLS 应该加在哪？**
A：在统一入口（LB/网关）完成 TLS 握手与解密，内网以 HTTP 转发——集中管理证书、保留网关读明文头做路由鉴权的能力、避免每个服务配证书。本项目现状全链路 HTTP；加 TLS 应在网关入口（`server.ssl` 监听 443）或前置 Ingress/LB 终止，SSE/WS 相应变为 https/wss，协议逻辑不变。

---

## 八、动手练习

1. 用 `curl -N -H "Authorization: Bearer <token>" -d "sessionId=t1&message=hi" http://localhost:8080/api/chat/stream/sse` 观察原始 SSE 输出，对照 §4.1 手工切出三个事件帧（data/空行/data），再用浏览器 DevTools 的 Network/EventStream 面板对照。
2. 抓包对比 HTTP/1.1 与 keep-alive：`curl -v http://localhost:8080/api/user/...` 连续两次请求，用 tcpdump 确认第二次没有重新握手；再强制 `Connection: close` 对比。
3. 给前端临时把 ChatView.vue 的 `buffer.indexOf('\n\n')` 改成 `'\n'`，观察消息解析错乱现象，用 §4.2 的分帧规则解释为什么必须双换行。
4. 用 openssl 自签一张证书给网关配 `server.ssl`（443），`curl -vk https://localhost/api/chat/...` 打通 SSE；观察 `Sec-WebSocket-*`/`EventSource` 在 wss 下的变化，再写两行说明"为什么生产要证书链而不是自签"。
5. 读 `RouteConfig.java:174-187` 的 `addResilience`，逐条过滤器判断"如果把 retry 放开到 POST 会怎样"：写出 SSE 流被网关重试后前端会出现什么现象（重复推送？重复计费？），以此论证 `setMethods(GET)` 的必要性。

---

> 上一篇：[04-TCP协议实战](./04-TCP协议实战.md) ｜ 下一篇：[06-容器隔离原理-namespace与cgroup](./06-容器隔离原理-namespace与cgroup.md)
