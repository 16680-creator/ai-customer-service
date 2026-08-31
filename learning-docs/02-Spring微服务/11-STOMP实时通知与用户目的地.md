# 11-STOMP 实时通知与用户目的地：从零开始理解 WebSocket 消息系统

> 本文面向第一次接触 WebSocket、STOMP 和 Spring Messaging 的读者。
> 示例来自本项目 `ai-cs-notify`：订单状态变化、转人工通知需要实时推送给指定用户。
>
> 当前实现：新主通道为 `/ws-stomp`，CONNECT 帧携带 JWT，服务端建立 Principal，
> `SimpMessagingTemplate` 通过 user destination 定向推送。旧 `/ws/notify` 暂保留一版灰度兼容，
> 但已经从 `*` 收紧为 `AICS_ALLOWED_ORIGINS` 白名单。

---

## 一、先理解：为什么需要实时通知

普通 HTTP 是“客户端发起请求，服务端返回响应”：

```text
浏览器 ── 请求订单状态 ──> 服务端
浏览器 <── 返回订单状态 ── 服务端
```

如果订单支付成功、客服接单、退款状态变化，服务端没有办法主动把消息推给浏览器。

最简单的替代是轮询：

```text
浏览器每 3 秒请求一次 /order/status
```

问题：

- 大部分请求没有新数据，浪费连接、CPU 和数据库查询
- 最多要等轮询间隔才看到变化
- 用户量上来后会产生大量无意义请求

WebSocket 建立一条长期双向连接：

```text
浏览器 ── 建立一次 WebSocket 连接 ──> 服务端
服务端 ── 任意时刻主动推消息 ────────> 浏览器
浏览器 ── 也可以主动发消息 ──────────> 服务端
```

它适合：

- 订单状态变化
- 转人工排队/客服接入
- 系统通知
- 在线聊天
- 协作编辑
- 监控大屏实时指标

---

## 二、WebSocket 和 STOMP 不是一回事

### 2.1 WebSocket 是传输通道

WebSocket 解决的是：浏览器和服务器如何建立一条长连接，双方如何发送文本或二进制数据。

裸 WebSocket 消息可能只是：

```text
{"event":"HANDOFF","ticketNo":"AS001"}
```

WebSocket 本身不知道：

- 谁订阅什么消息
- 如何广播
- 如何给指定用户发送
- 消息属于哪个业务目的地
- 如何 ACK、取消订阅、建立 session 身份

这些都要自己设计协议和维护会话表。

### 2.2 STOMP 是 WebSocket 上的消息协议

STOMP 全称是 Simple Text Oriented Messaging Protocol。它定义一套文本帧协议，
像 HTTP 有 GET/POST 一样，STOMP 有：

```text
CONNECT      建立 STOMP 会话
CONNECTED    服务端确认连接
SUBSCRIBE    订阅某个 destination
SEND         客户端向服务端发送消息
MESSAGE      服务端向客户端推送消息
DISCONNECT   断开连接
ERROR        协议或鉴权错误
```

可以把关系记成：

```text
TCP 之上可以跑 HTTP
WebSocket 之上可以跑 STOMP
```

本项目选择 STOMP，是为了直接获得 destination、订阅、用户目的地、broker 路由等能力，
而不是自己维护 `userId -> WebSocketSession`。

---

## 三、改造前：裸 WebSocket 的问题

旧实现使用：

```text
/ws/notify?userId=1001
```

服务端维护：

```java
ConcurrentHashMap<String, WebSocketSession> SESSION_MAP
```

推送时：

```java
WebSocketSession session = SESSION_MAP.get(userId);
session.sendMessage(new TextMessage(message));
```

问题如下：

| 问题 | 说明 |
|------|------|
| `userId` 可伪造 | 用户可以连接 `/ws/notify?userId=1001` 冒充别人 |
| session 管理手写 | 多浏览器标签、多设备、断线重连、异常清理都要自己维护 |
| 多实例不支持 | session map 在当前 JVM，notify 横向扩容后不同实例互相看不到连接 |
| 广播/定向语义不统一 | 全靠约定 JSON 字段和静态方法，没有 destination 模型 |
| CORS 风险 | `setAllowedOrigins("*")` 允许任意网页发起跨站连接 |

因此新主路径改为 STOMP，旧通道仅保留短期兼容。

---

## 四、本项目完整调用图

```mermaid
sequenceDiagram
    autonumber
    participant Browser as 前端浏览器
    participant WS as /ws-stomp
    participant Auth as StompConnectAuthInterceptor
    participant Broker as Spring Simple Broker
    participant Service as NotifyHandoffService
    participant Template as SimpMessagingTemplate

    Browser->>WS: WebSocket Upgrade
    Browser->>Auth: STOMP CONNECT<br/>Authorization: Bearer JWT
    Auth->>Auth: JwtUtil.validateToken()
    Auth->>Auth: 取 subject=userId
    Auth-->>Broker: setUser(Principal(userId))
    Broker-->>Browser: CONNECTED

    Browser->>Broker: SUBSCRIBE /user/queue/notify
    Note over Broker: 建立 Principal=userId 到 session 的映射

    Service->>Template: convertAndSendToUser(userId,<br/>"/queue/notify", payload)
    Template->>Broker: 按用户目的地路由
    Broker-->>Browser: MESSAGE<br/>destination=/user/queue/notify
```

关键点：浏览器订阅时**不需要、也不能自己传目标 userId**：

```text
正确：/user/queue/notify
错误：/user/1001/queue/notify
```

服务端从 CONNECT 鉴权得到的 Principal 决定浏览器属于哪个用户。

---

## 五、STOMP 中最重要的概念：destination

destination 可以理解为消息的逻辑地址，类似消息系统中的 topic/queue。

本项目约定：

| destination | 用途 | 谁使用 |
|-------------|------|--------|
| `/app/**` | 客户端发给服务端的应用命令 | 浏览器 SEND |
| `/topic/notify` | 广播通知 | 服务端广播，所有订阅者收到 |
| `/queue/notify` | 用户消息队列后缀 | 服务端 `convertAndSendToUser` |
| `/user/queue/notify` | 客户端订阅自己的通知 | 浏览器 SUBSCRIBE |

配置：

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
}
```

逐行理解：

```java
registry.enableSimpleBroker("/topic", "/queue");
```

启动内存简单 broker。消息目标以 `/topic` 或 `/queue` 开头时，由 broker 负责路由给订阅者。

```java
registry.setApplicationDestinationPrefixes("/app");
```

客户端发送 `/app/something` 时，Spring 会把它路由到服务器的 `@MessageMapping` 方法。
当前项目主要是服务端推送通知，暂未实现客户端业务 SEND handler。

```java
registry.setUserDestinationPrefix("/user");
```

开启用户目的地语义。客户端订阅 `/user/queue/notify`，服务端通过
`convertAndSendToUser(userId, "/queue/notify", payload)` 定向路由。

---

## 六、CONNECT 鉴权：为什么不能信任 URL userId

### 6.1 客户端 CONNECT 帧

浏览器在 STOMP CONNECT 时发送：

```text
CONNECT
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
accept-version:1.2
heart-beat:10000,10000
```

本项目在 `StompConnectAuthInterceptor` 中拦截 CONNECT。

### 6.2 服务端处理流程

```java
if (StompCommand.CONNECT.equals(accessor.getCommand())) {
    String authorization = accessor.getFirstNativeHeader("Authorization");
    String token = authorization.substring("Bearer ".length());

    if (!JwtUtil.validateToken(token, jwtSecret)) {
        throw new IllegalArgumentException("STOMP CONNECT Token 无效或已过期");
    }

    String userId = JwtUtil.getSubject(token, jwtSecret);
    accessor.setUser((Principal) () -> userId);
}
```

流程：

```text
CONNECT 帧
  → 读取 Authorization
  → 校验 JWT 签名与过期时间
  → JWT subject 作为 userId
  → setUser(Principal)
  → Spring broker 将 Principal 和当前 session 绑定
```

这样用户身份由 JWT 决定，而不是客户端随便传一个 `userId`。

### 6.3 无 token 会怎样

当前实现会抛出：

```text
STOMP CONNECT 缺少 Bearer Token
```

连接被拒绝。前端应监听 STOMP 错误回调，跳回登录或提示 token 已过期。

---

## 七、用户定向推送：convertAndSendToUser

普通广播：

```java
messagingTemplate.convertAndSend("/topic/notify", message);
```

所有订阅 `/topic/notify` 的客户端都会收到。

定向推送：

```java
messagingTemplate.convertAndSendToUser(
    userId,
    "/queue/notify",
    message
);
```

项目中的转人工通知：

```java
messagingTemplate.convertAndSendToUser(
    String.valueOf(dto.getUserId()),
    NotifyServiceImpl.USER_NOTIFY_DESTINATION,
    json
);
```

其中：

```java
public static final String USER_NOTIFY_DESTINATION = "/queue/notify";
```

浏览器订阅：

```text
/user/queue/notify
```

注意客户端和服务端 destination 写法不一样：

| 位置 | 写法 |
|------|------|
| 客户端订阅 | `/user/queue/notify` |
| 服务端定向发送 | `convertAndSendToUser(userId, "/queue/notify", payload)` |

原因是 `/user` 是 Spring 用于解析当前 Principal 的逻辑前缀；服务端已经显式提供 userId，
不需要再写 `/user`。

---

## 八、转人工通知的完整业务路径

```text
AI 识别需要人工转接
  → 创建/更新售后工单
  → NotifyHandoffService.sendHandoffNotice(dto)
  → DTO 转 JSON 并增加 event=HANDOFF
  → SimpMessagingTemplate.convertAndSendToUser()
  → 当前用户订阅的 /user/queue/notify 收到消息
  → 前端根据 event=HANDOFF 展示人工转接状态
```

当前 payload 示例：

```json
{
  "ticketNo": "AS20250601001",
  "userId": 1001,
  "priority": "URGENT",
  "orderNo": "ORD20250601001",
  "summary": "用户咨询退款进度",
  "event": "HANDOFF"
}
```

`event` 是前端业务路由标记，不是 STOMP 协议字段。

---

## 九、Simple Broker 和外部 Broker Relay

### 9.1 当前：Simple Broker

```java
registry.enableSimpleBroker("/topic", "/queue");
```

特点：

- 在 notify 服务 JVM 内存里运行
- 不需要 RabbitMQ/ActiveMQ
- 适合单实例、学习环境、低连接量
- 服务重启时连接和订阅全部消失
- 多实例时不同实例之间不会共享 session 路由

### 9.2 扩容：Broker Relay

当 notify 服务有多个实例时：

```text
用户 A 的连接在 notify-1
消息生产者调用 notify-2
notify-2 不知道用户 A 的 session 在 notify-1
```

这时需要让外部 broker 统一路由：

```java
registry.enableStompBrokerRelay("/topic", "/queue")
        .setRelayHost("rabbitmq")
        .setRelayPort(61613);
```

对比：

| 项目 | Simple Broker | RabbitMQ STOMP Relay |
|------|---------------|----------------------|
| 状态位置 | 应用 JVM | RabbitMQ |
| 多实例路由 | 不支持 | 支持 |
| 消息持久化 | 不支持 | 可配置支持 |
| 部署成本 | 很低 | 需要 broker |
| 适用 | 开发/单实例 | 生产多实例通知系统 |

---

## 十、前端接入参考

当前前端没有通知 WebSocket 页面，因此项目没有提前加入未使用的 `@stomp/stompjs` 依赖。
未来通知 UI 开发时可以使用：

```bash
npm install @stomp/stompjs
```

Vue 伪代码：

```javascript
import { Client } from '@stomp/stompjs'

const client = new Client({
  brokerURL: 'ws://localhost:8086/ws-stomp',
  connectHeaders: {
    Authorization: `Bearer ${token}`
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,

  onConnect() {
    client.subscribe('/user/queue/notify', (frame) => {
      const notice = JSON.parse(frame.body)
      if (notice.event === 'HANDOFF') {
        // 更新转人工 UI
      }
    })
  },

  onStompError(frame) {
    console.error('STOMP error', frame.headers.message, frame.body)
  }
})

client.activate()
```

注意：

- token 过期后应断开并重新登录，再重连
- `reconnectDelay` 只解决网络中断，不解决 JWT 过期
- Vue 组件卸载时调用 `client.deactivate()`
- 不要把 userId 放进订阅 destination 中

---

## 十一、CORS、Origin 与安全边界

旧配置：

```java
.setAllowedOrigins("*")
```

含义是任何网站都可以从浏览器发起 WebSocket 建连。即使有 token，也不建议长期放开。

当前改为：

```java
.setAllowedOriginPatterns("${AICS_ALLOWED_ORIGINS:http://localhost:5173}")
```

生产配置示例：

```text
AICS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

安全原则：

1. 不信任 URL 中的 userId
2. CONNECT 必须校验 JWT
3. Origin 使用明确白名单
4. 服务端口不直接暴露给公网，统一走网关或 Ingress
5. 不在日志中打印完整 JWT
6. 支持 token 过期后的断连和重新认证

---

## 十二、当前代码结构

```text
ai-cs-notify/
├── config/
│   ├── StompWebSocketConfig.java
│   │   ├── enableSimpleBroker
│   │   ├── /ws-stomp endpoint
│   │   └── inbound channel interceptor
│   ├── StompConnectAuthInterceptor.java
│   │   └── CONNECT JWT → Principal(userId)
│   └── WebSocketConfig.java
│       └── 旧 /ws/notify 灰度通道 + Origin 白名单
├── service/impl/
│   ├── NotifyServiceImpl.java
│   │   ├── convertAndSendToUser
│   │   └── convertAndSend /topic/notify
│   └── NotifyHandoffServiceImpl.java
│       └── HANDOFF JSON → user destination
└── websocket/NotifyWebSocketHandler.java
    └── 旧裸 WebSocket handler，灰度期保留
```

---

## 十三、测试与验证

已执行：

```text
mvn -pl ai-cs-notify verify
```

结果：25 个测试全绿，JaCoCo 门禁通过。

测试覆盖：

| 测试 | 验证内容 |
|------|----------|
| `StompConnectAuthInterceptorTest` | 合法 JWT 建立 Principal；无 token 拒绝 CONNECT |
| `NotifyHandoffServiceTest` | JSON 含 `event=HANDOFF`，发送到指定 user destination |
| `NotifyServiceImplTest` | 定向通知走 `/queue/notify`，广播走 `/topic/notify` |
| `WebSocketConfigTest` | 旧通道仍可注册，供灰度兼容 |

需要真实浏览器/服务环境的验收：

1. 登录获得 JWT
2. 浏览器连 `/ws-stomp`
3. CONNECT 带 Bearer token
4. 订阅 `/user/queue/notify`
5. 触发转人工
6. 确认只由目标用户收到 HANDOFF 消息
7. 用无 token/伪造 userId 测试连接被拒绝或无法收到他人消息

---

## 十四、常见问题

### Q1：客户端为什么订阅 `/user/queue/notify`，不是 `/queue/notify`？

`/queue/notify` 是 broker 的普通队列前缀；`/user/queue/notify` 才表示“当前已认证用户自己的队列”。

### Q2：服务端为什么不写 `/user/queue/notify`？

服务端使用 `convertAndSendToUser(userId, "/queue/notify", payload)`，已经明确提供 userId。
Spring 会自动转换成对应用户 session 的实际 destination。

### Q3：多个浏览器标签页登录同一用户，都会收到吗？

会。一个 Principal 可关联多个 STOMP session，`convertAndSendToUser` 会路由给该用户的全部订阅 session。
这是比旧 `Map<userId, WebSocketSession>` 更合理的行为。

### Q4：为什么 `getOnlineCount()` 现在返回 0？

Simple Broker 不提供可靠的跨 session 在线用户统计 API。用静态 Map 强行统计只在单实例正确。
生产需要在线人数时应使用 presence 存储、broker 指标或专门的连接管理服务。

### Q5：STOMP 能替代 RocketMQ 吗？

不能。STOMP 主要面向在线客户端实时推送；RocketMQ 面向服务间持久化消息、延迟消息、重试、死信。
用户不在线时 STOMP simple broker 不会帮你持久化通知，离线通知仍需数据库/MQ。

### Q6：为什么旧 `/ws/notify` 还没有删除？

为了灰度兼容现有客户端。旧客户端使用量归零、前端 STOMP 正式上线后再删除，避免突然断开旧连接。

---

## 十五、面试要点总结

可以这样描述本项目：

> 项目早期用裸 WebSocket + 静态 session map 推通知，存在 URL userId 可伪造、会话管理手写、
> 多实例无法路由的问题。后来迁为 Spring STOMP：客户端 CONNECT 时带 Bearer JWT，inbound
> channel interceptor 校验 token 并把 JWT subject 设为 Principal；客户端订阅
> `/user/queue/notify`，服务端使用 `SimpMessagingTemplate.convertAndSendToUser` 定向推送。
> 这样用户身份与 session 由 broker 管理，旧通道仅保留灰度兼容。当前 simple broker 适合单实例，
> 多实例时可升级 RabbitMQ STOMP relay 统一路由。

关键词：

```text
WebSocket
STOMP
CONNECT
SUBSCRIBE
Principal
User Destination
SimpMessagingTemplate
Simple Broker
Broker Relay
Origin 白名单
JWT 鉴权
```
