# 11 STOMP 实时通知与用户目的地：一条消息怎么找到那一个坐席

> **一句话结论**：实时推送里"把消息发出去"永远是最简单的一步；真正花时间的是三件事的对齐——CONNECT 帧里解析出的 `Principal` 名字、客户端 `SUBSCRIBE` 的 `/user` 前缀、服务端 `convertAndSendToUser` 的目标名。三者只要有一处不同源，消息就静默消失在 broker 里：不报错、不重试、不进业务日志。
> **阅读前置**：会写 Spring MVC 的 `@RestController`；知道 HTTP 是"请求-响应"、WebSocket 靠 `Upgrade: websocket` 从 HTTP 升级而来；知道 JWT 是 `header.payload.signature` 三段。STOMP 没接触过也没关系，§2 与 §3.2 会把帧模型补齐。
> **证据基线**：Spring Framework 7.1.0 / Spring Boot 4.2.0 / Spring Cloud 2026.0.0；本项目 `ai-cs-notify`（端口 8086）。⚠️ 本仓库 `24-源码分析/assets/source` 的 905 个摘录里**没有** `spring-messaging` / `spring-websocket` 的任何类（`*Stomp*`、`*UserDestination*`、`*WebSocket*`、`*Handshake*` 全部 0 命中，已列目录确认），所以本篇**不具备 STOMP 类的 A 级源码锚点**；结论由 B 级（本项目源码，行号逐个 read 核对）+ C 级（可复现命令）+ 官方文档支撑。凡落不到本机证据的判断，正文都标注了出处与验证方法。预计阅读 45 分钟。

---

## 0. 先看现象

### 现象 1：页面能打开，WebSocket 一次都没连上

`ai-cs-frontend/src/views/NotifyView.vue` 第 114 行决定了连接目标：

```js
// ai-cs-frontend/src/views/NotifyView.vue#L112-L116
ws = new WebSocket('ws://localhost:8085/ws/notify')
```

8085 并不是通知服务。把三处事实摆在同一屏上（C 级实测，命令与真实输出见 §6.7 手法 1）：

```text
--- 前端连的地址 ---
114: ws = new WebSocket('ws://localhost:8085/ws/notify')
--- notify 实际端口 ---
3: port: 8086
--- 8085 是谁 ---
3: port: 8085
9: name: ai-cs-message
```

浏览器端的表现是握手失败：DevTools 的 Console 里一条 `WebSocket connection to 'ws://localhost:8085/ws/notify' failed`，Network → WS 里状态码不是 `101 Switching Protocols`。原因很直接——8085 上的 `ai-cs-message` 根本没有 `/ws/notify` 这个处理器。更值得注意的是：**它连的甚至不是新的 STOMP 端点 `/ws-stomp`**，而是 `ai-cs-notify` 里那条灰度保留的旧裸 WebSocket 通道。

### 现象 2：服务端日志说"已发送"，订阅端一条 MESSAGE 都没有

即使把地址改成 `ws://localhost:8086/ws-stomp`、STOMP 也握手成功，定向消息仍然可能收不到，因为订单链路的最后一跳根本没走 STOMP：

```java
// ai-cs-notify/src/main/java/com/aics/notify/consumer/OrderNotifyConsumer.java#L32-L41
public class OrderNotifyConsumer implements RocketMQListener<Map<String, String>> {
    // ... onMessage 里取出 userId / message
    NotifyWebSocketHandler.sendMessageToUser(userId, content);
```

`NotifyWebSocketHandler` 是裸 WebSocket 的旧实现，消息进的是它自己的静态 map（`ai-cs-notify/src/main/java/com/aics/notify/websocket/NotifyWebSocketHandler.java#L25`），而前端订阅的是 STOMP broker。两条通道之间没有任何桥接；更糟的是查不到人时它只打一行 warn 就丢弃：

```java
// ai-cs-notify/src/main/java/com/aics/notify/websocket/NotifyWebSocketHandler.java#L91-L103
log.warn("用户不在线，无法发送消息: userId={}", userId);
```

`用户不在线` 是这段代码里真实存在的日志文案。它出现时通常不是"人真的不在线"，而是"人挂在另一条通道上"——或者身份压根没解析出来（旧通道从 URL 的 `?userId=` 取身份，前端没带就是 `null`）。

### 现象 3：单机能收到，起两个实例就随机丢

`ai-cs-notify` 用的是内存 broker：

```java
// ai-cs-notify/src/main/java/com/aics/notify/config/StompWebSocketConfig.java#L21-L26
registry.enableSimpleBroker("/topic", "/queue");
registry.setApplicationDestinationPrefixes("/app");
registry.setUserDestinationPrefix("/user");
```

`enableSimpleBroker` 的订阅表就是 JVM 堆里的一个注册表。用户 A 连到 pod-1，订阅关系只存在 pod-1 内存里；某条订单消息被 pod-2 消费并由 pod-2 的 `SimpMessagingTemplate` 投递——pod-2 的 broker 里没有 A 的订阅，对 pod-2 而言"无人订阅"，直接丢弃。全库检索可以证明本项目从未引入外部 relay：`enableStompBrokerRelay` / `SockJS` / `withSockJS` 命中 **0** 处（C 级，见 §6.7 手法 2）。

这三个现象指向同一件事：**实时推送不是"把消息交给一个 API 就完了"，而是"身份（Principal）、订阅地址（destination）、投递目标（user）三者必须在同一坐标系里"；而这个坐标系一旦跨了进程（多实例）或跨了通道（STOMP vs 裸 WebSocket），默认行为就是静默丢弃。**

---

## 1. 整体地图

```mermaid
flowchart TD
    B["浏览器 NotifyView.vue<br/>原生 WebSocket"]
    T["Tomcat 8086<br/>HTTP Upgrade: websocket"]
    EP["WebSocketHttpRequestHandler<br/>端点 /ws-stomp"]
    SP["SubProtocolWebSocketHandler<br/>子协议 stomp"]
    IC["clientInboundChannel<br/>入站线程池"]
    CI["StompConnectAuthInterceptor<br/>只拦 CONNECT 帧"]
    UD["UserDestinationMessageHandler<br/>改写 /user 前缀"]
    SB["SimpleBrokerMessageHandler<br/>内存订阅表 /topic /queue"]
    OC["clientOutboundChannel<br/>出站线程池"]
    CTL["NotifyController<br/>/api/notify/send|broadcast|handoff"]
    TPL["SimpMessagingTemplate<br/>convertAndSend(ToUser)"]
    ORD["OrderNotifyConsumer<br/>topic notify-topic"]
    OLD["NotifyWebSocketHandler<br/>旧 /ws/notify + SESSION_MAP"]

    B -->|"1 HTTP GET + Upgrade"| T
    T --> EP --> SP --> IC --> CI
    CI -->|"SUBSCRIBE: 绑定 sessionId 与 Principal"| UD
    UD --> SB
    SB --> OC --> B
    CTL --> TPL
    TPL -->|"带 user 前缀"| UD
    TPL -->|"不带 user 前缀"| SB
    ORD --> OLD
    OLD -.->|"无桥接，消息到此为止"| SB
```

> **读图说明**：
> - 图里有**两条发送入口**：`NotifyController` → `SimpMessagingTemplate` 走 STOMP 正确路径；`OrderNotifyConsumer` → `NotifyWebSocketHandler` 走旧裸通道，就是它造成"订单通知前端收不到"。
> - `UserDestinationMessageHandler` 只在目的地**带 user 前缀**时介入。看错这条箭头会得出错误结论："我用 `convertAndSendToUser` 了，为什么消息没进 broker？"——它其实进了，只是在 `UD` 里被改写成一个你不认识的目的地。
> - `CI`（鉴权拦截器）只处理 `CONNECT` 帧，`SUBSCRIBE`/`SEND` 不再校验。"连上之后换身份"这类攻击面由 `sessionId → Principal` 的绑定兜住，不是由拦截器兜住。
> - `OC` 写回 socket 是**异步**的：`convertAndSend` 返回只代表消息交给了 broker，不代表客户端收到。
> - `OLD` 与 `SB` 之间那条虚线是全图唯一一处"看起来连着、实际不通"的地方。

| 组件 | 责任 | 谁调用它 |
| --- | --- | --- |
| `WebSocketHttpRequestHandler` / `SubProtocolWebSocketHandler` | 完成握手，按子协议把字节流切成 STOMP 帧 | Tomcat 容器（`spring-websocket` 注册的 handler） |
| `StompConnectAuthInterceptor` | 解 `CONNECT` 帧的 `Authorization`，把 `Principal` 写进消息头 | `clientInboundChannel`（由 `configureClientInboundChannel` 注册） |
| `UserDestinationMessageHandler` + `DefaultUserDestinationResolver` | 把 `/user/queue/notify` 改写成会话私有目的地 | `clientInboundChannel`（订阅侧）/ `brokerChannel`（发送侧） |
| `SimpleBrokerMessageHandler` | 进程内订阅表与扇出 | `brokerChannel` |
| `SimpMessagingTemplate` | 业务代码发消息的门面 | `NotifyServiceImpl`、`NotifyHandoffServiceImpl` |
| `NotifyWebSocketHandler` | 旧裸通道，按 `?userId=` 维护 `SESSION_MAP` | `OrderNotifyConsumer`（MQ 消费） |

## 2. 关键概念与关键类

| 名称 | 位置 | 角色 | 关键方法 / 属性 |
| --- | --- | --- | --- |
| `@EnableWebSocketMessageBroker` | 包 `org.springframework.web.socket.config.annotation` | 开关注解，导入 `DelegatingWebSocketMessageBrokerConfiguration`，装配整套 STOMP 基础设施 | 无方法（导入型注解） |
| `WebSocketMessageBrokerConfigurer` | 同上 | 配置回调接口，三个钩子分别管 broker、端点、通道 | `configureMessageBroker` / `registerStompEndpoints` / `configureClientInboundChannel` |
| `MessageBrokerRegistry` | 同上 | 决定 broker 类型与前缀语义 | `enableSimpleBroker` / `enableStompBrokerRelay` / `setApplicationDestinationPrefixes` / `setUserDestinationPrefix` / `setHeartbeatValue` |
| `StompEndpointRegistry` | 同上 | 注册握手端点与跨域策略 | `addEndpoint("/ws-stomp")`、`setAllowedOriginPatterns`、`withSockJS` |
| `SimpleBrokerMessageHandler` | 包 `org.springframework.messaging.simp.broker` | 内存订阅表 + 广播/定向扇出 | `handleMessage`、内部 `SubscriptionRegistry` |
| `UserDestinationMessageHandler` | 包 `org.springframework.messaging.simp.user` | 识别 user 前缀并委派解析器改写目的地 | `handleMessage` |
| `DefaultUserDestinationResolver` | 同上 | `/user/{name}/queue/xxx` 的解析实现 | `resolveDestination` |
| `SimpMessagingTemplate` | 包 `org.springframework.messaging.simp` | 发送门面，`convertAndSend` 家族 | `convertAndSend` / `convertAndSendToUser` |
| `StompHeaderAccessor` | 包 `org.springframework.messaging.simp.stomp` | 读写 STOMP 帧头，能判帧类型 | `wrap` / `getCommand` / `getFirstNativeHeader` / `setUser` |
| `ChannelInterceptor` | 包 `org.springframework.messaging.support` | 通道级拦截器，消息进通道前可改写或拒绝 | `preSend` / `postSend` / `afterSendCompletion` |
| `Principal` | `java.security` | 只有一个 `getName()` 的身份对象；STOMP 里的"用户"就是它 | `getName()` |
| `HandshakeInterceptor` | 包 `org.springframework.web.socket.server` | 握手（HTTP 升级）阶段拦截，能读 query/header，读不到 STOMP 帧 | `beforeHandshake` / `afterHandshake` |

一个必须记牢的分界：**`HandshakeInterceptor` 看得到 HTTP 请求，看不到 STOMP 帧；`ChannelInterceptor` 看得到 STOMP 帧，看不到原始 HTTP 头。** 本项目选的是后者（`StompConnectAuthInterceptor implements ChannelInterceptor`），原因在 §3.3。

## 3. 原理与证据逐段精读

### 3.1 端点注册：`/ws-stomp` 是谁的路由

**在干什么。** `registerStompEndpoints` 把一个 URL 路径交给 `spring-websocket` 的 handler，从此这个路径上的 GET 请求会被当作"可能的 WebSocket 升级请求"处理。

```java
// ai-cs-notify/src/main/java/com/aics/notify/config/StompWebSocketConfig.java#L29-L32
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-stomp")
            .setAllowedOriginPatterns("${AICS_ALLOWED_ORIGINS:http://localhost:5173}");
}
```

**为什么这么设计。** 握手本质是一次普通 HTTP GET，附带 `Connection: Upgrade`、`Upgrade: websocket`、`Sec-WebSocket-Key`、`Sec-WebSocket-Version: 13`；服务端算一个 `Sec-WebSocket-Accept` 回 `101`，此后同一条 TCP 连接上的字节流不再是 HTTP。正因为"先是 HTTP"，跨域策略（`Origin`）与鉴权（cookie / query token）都发生在这层；也正因为"之后不是 HTTP"，Spring MVC 的拦截器、`Filter`、`@RestController` 都管不到帧。

**换写法会怎样。** 三个替代各有代价：

- 加 `.withSockJS()`：SockJS 会额外注册 `.../info`、`.../xhr_streaming` 等降级路径，URL 变成 `http(s)://` 风格。好处是极老浏览器或严格代理环境可用；代价是 URL 变形、原生 WS 调试面板失效、多一层帧包装。本项目**没加**（C 级：全库 `withSockJS` 命中 0）。
- `@EnableWebSocket` + `TextWebSocketHandler`：就是旧通道 `WebSocketConfig` 的做法，见 §3.7，订阅与路由全要自己实现。
- JSR-356 `@ServerEndpoint` + `ServerEndpointExporter`：脱离 Spring 的消息模型，`SimpMessagingTemplate` 与 user destination 都用不上。

**常见坑。**

1. **别指望 `/ws-stomp` 被网关的 `lb://` 转发。** `ai-cs-gateway/src/main/java/com/aics/gateway/config/RouteConfig.java#L118-L122` 里 notify 只有一条 `/api/notify/**` 的 HTTP 路由，网关侧没有任何 ws 路由（全库检索网关源码，`WebSocket` 关键字 0 命中）。所以前端必须直连 `ws://<notify-host>:8086/ws-stomp`，跨域由 `setAllowedOriginPatterns` 兜。
2. `setAllowedOriginPatterns` 与 `setAllowedOrigins` 语义不同：后者只接受精确 Origin，且 `*` 不能与凭证共存；前者支持通配模式。写 `setAllowedOrigins("*")` 是旧稿里被点名的错误示范。
3. 上面那行用了一个 `${AICS_ALLOWED_ORIGINS:http://localhost:5173}` 占位符（Spring 的"配置项不存在就用冒号后的默认值"语法）。**框架是否把端点注册处这个字符串当作需要解析占位符的配置值，本篇没有取得可断言的证据**：`assets/source` 无 `spring-websocket` 摘录，本地 `.m2` 里该 artifact 只有 pom 没有 sources jar，无法用字节码核对。验证方法见 §6.7 手法 4——看握手响应的 `Access-Control-Allow-Origin` 回显的是 `http://localhost:5173` 还是字面量 `${AICS_ALLOWED_ORIGINS:...}`。

### 3.2 STOMP 帧模型：命令 + 头 + 空行 + NUL

**在干什么。** STOMP 是跑在 WebSocket（或纯 TCP）之上的**文本帧协议**，把"谁订阅了什么、谁发了什么"表达成可读的帧。一个帧 = 命令行 + 若干头 + 空行 + 体 + `\0`。下面是按 STOMP 1.2 规范手写的三段帧，用来对照日志读，不是本项目抓包：

```text
CONNECT
accept-version:1.2
heart-beat:10000,10000
Authorization:Bearer eyJhbGciOiJIUzI1NiJ9...

^@
SUBSCRIBE
id:sub-0
destination:/user/queue/notify

^@
MESSAGE
destination:/queue/notify-user3f8a1c
message-id:7b2
subscription:sub-0
content-type:application/json

{"event":"HANDOFF","ticketNo":"AS20250601001"}
^@
```

（`^@` 表示 NUL 字节 `0x00`，是帧的结束符。）

**为什么这么设计。** 裸 WebSocket 只提供"一条双向字节流"，没有"订阅"概念——要么自己发明一套路由协议，要么每条消息自己判断该给谁。STOMP 用几个命令把这个语义补齐：

| 命令 | 方向 | 作用 | 本项目是否用到 |
| --- | --- | --- | --- |
| `CONNECT` / `CONNECTED` | C→S / S→C | 建会话、协商版本与心跳 | 用（`StompConnectAuthInterceptor` 只拦这个） |
| `SUBSCRIBE` / `UNSUBSCRIBE` | C→S | 注册/取消"我要这个 destination" | 用（客户端订 `/user/queue/notify`） |
| `SEND` | C→S | 客户端发消息给 `/app/**` 处理器 | **没用**（`@MessageMapping` 全库 0 处） |
| `MESSAGE` | S→C | 服务端投递 | 用 |
| `DISCONNECT` | C→S | 主动断开 | 用（页面卸载时） |
| `ERROR` | S→C | 协议级错误，通常随后断连 | 异常时出现 |

心跳是**没有命令行的空帧**（一个换行符）。`CONNECT` 帧里的 `heart-beat:cx,sy` 表示"我最多每 `cx` 毫秒发一次、希望对方最多每 `sy` 毫秒发一次"，双方各取较大值协商。心跳是唯一能发现"TCP 还连着但对端已死"的手段：NAT 表过期、手机切网、进程被强杀都会留下半开连接。

**换写法会怎样。** 纯裸 WebSocket 就退化成旧通道：只能自定义 `{"type":"notify","userId":1001}` 这类应用层协议，路由、订阅、心跳、语义全自己写。反过来，若只需要单向推送（例如 AI 流式回答），SSE 更省事——见 [SSE 与 WebSocket 实时通信](../04-中间件/05-SSE与WebSocket实时通信.md)。

**常见坑。**

1. **服务端默认不发心跳。** `SimpleBroker` 的心跳需要 `MessageBrokerRegistry#setHeartbeatValue` 与 `setTaskScheduler` 同时配好才生效；本项目 `configureMessageBroker`（第 21~26 行）只设了 broker 和两个前缀，没配心跳，所以服务端侧心跳是关的——能否保活完全取决于客户端请求的 `heart-beat` 与中间代理的空闲超时。
2. 帧体里不能出现裸 NUL；二进制内容必须 base64。
3. `content-type` 决定消息体如何反序列化；缺了它，`@MessageMapping` 的参数绑定会拿不到对象（本项目不走这条路，但读别人的代码会撞上）。
4. 帧头大小写不敏感，但项目自定的头（如 `Authorization`）在 Spring 侧取用时用 `getFirstNativeHeader` 而不是 `getFirstHeaderValue`，取错方法会永远返回 `null`。

### 3.3 身份从哪来：CONNECT 帧 → Principal

**在干什么。** 一个 43 行的拦截器，在帧进入业务处理前把 `Authorization` 解成用户 ID，并挂成 `Principal`：

```java
// ai-cs-notify/src/main/java/com/aics/notify/config/StompConnectAuthInterceptor.java#L27-L41
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("STOMP CONNECT 缺少 Bearer Token");
        }
        String token = authorization.substring("Bearer ".length());
        if (!JwtUtil.validateToken(token, jwtSecret)) {
            throw new IllegalArgumentException("STOMP CONNECT Token 无效或已过期");
        }
        String userId = JwtUtil.getSubject(token, jwtSecret);
        accessor.setUser((Principal) () -> userId);
    }
    return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
}
```

逐段读：

- `StompHeaderAccessor.wrap(message)`：把通用 `Message` 的头包成**可写视图**，这样才能 `getCommand()`、`getFirstNativeHeader()`。`native header` 指 STOMP 帧自带的头（`Authorization`、`heart-beat`），区别于 Spring 内部的 `simpXxx` 头。
- `StompCommand.CONNECT` 判断：只拦握手帧，后续 `SUBSCRIBE`/`SEND` 直接放行，开销接近零。
- 两个 `throw new IllegalArgumentException(...)`：这两句字符串就是**报错原文**。异常不会被吞，会沿 `clientInboundChannel` 冒到 `StompSubProtocolHandler`，转成一条 `ERROR` 帧并关闭连接；客户端在 `onStompError`/`onclose` 里看到的是协议错误类的 close code（`1002` 一类），而不是 HTTP 状态码。
- `JwtUtil.validateToken` / `getSubject`：`ai-cs-common/src/main/java/com/aics/common/util/JwtUtil.java#L127` 与 `#L105`；前者显式捕获 `ExpiredJwtException` 返回 false。
- `accessor.setUser((Principal) () -> userId)`：`Principal` 是只有一个方法的接口，lambda 即可。
- 最后一行 `MessageBuilder.createMessage(...)` 最容易漏：**`setUser` 改的是 accessor 里的头快照，必须用这份新头重新构造 `Message` 返回**。写成 `return message;` 会让身份丢掉，而且不报任何错。

**为什么这么设计（为什么不是 HandshakeInterceptor）。** 两个理由都落在这段代码的形状上：

1. 浏览器原生 `new WebSocket(url, protocols)` **不能自定义请求头**，凭证只能塞 URL query 或 `Sec-WebSocket-Protocol`。走 query 正是旧通道的做法（`extractUserId` 从 `?userId=` 取，可随手伪造）。而 STOMP 的 `CONNECT` 帧是应用层帧，**可以带任意 native header**，`Authorization: Bearer ...` 有地方放。
2. 放在 `clientInboundChannel` 意味着每一条进来的帧都过这里，判断集中在一处；放在握手期则要 `HandshakeInterceptor#beforeHandshake` 读 query/subprotocol、存进 `attributes`、再到别处取出来，链路更长且容易漏传。

两者也可以并用：握手期粗筛（拒绝明显无凭证的连接，省下连接数），帧期做严格校验。

**换写法会怎样。**

- `HandshakeInterceptor` + `?token=xxx`：能工作，但 token 会进 access log 与浏览器历史，且 token 过期后无法基于帧重新校验。
- 继承 `StompSubProtocolHandler` 覆写 `handleMessageFromClient`：能拿到原始 `WebSocketSession`，但要把默认实现换掉，配置更绕。
- Spring Security 的 `@EnableWebSocketSecurity` + `AuthorizationManager`：Security 7 下更规范，能表达"谁能 SUBSCRIBE 哪个目的地"，但要引入 `spring-security-messaging` 并声明 `MessageMatcherDelegatingAuthorizationManager`。本项目没这么做（C 级：`HandshakeInterceptor` 0 命中，安全表达式类未出现）。

**常见坑。**

1. 忘记重建 `Message`（上一条第 6 点），或只往 `accessor.getSessionAttributes()` 里塞了 userId 而没 `setUser`——`DefaultUserDestinationResolver` 认的是 `Principal`，不是 attribute。
2. 异常抛在 `preSend` 里，默认日志只有一行；要区分"缺头"与"过期"得靠两个不同文案，本项目已经分了。
3. JWT 的 `subject` 必须是**用户 ID 字符串本身**：`ai-cs-user/src/main/java/com/aics/user/service/impl/UserServiceImpl.java#L100` 是 `JwtUtil.generateToken(String.valueOf(user.getId()), claims, ...)`，所以 `getName()` 得到 `"1001"`。一旦改成 `"user:1001"` 或 UUID，服务端的 `convertAndSendToUser("1001", ...)` 就再也匹配不上——不报错，静默丢弃。
4. 拦截器是**单例 Bean**，别在里面存"当前用户"这类请求态字段。

### 3.4 `sessionId` 与 user 的绑定：`/user/queue/notify` 被改写成什么

**在干什么。** 客户端订的是 `/user/queue/notify`，而 broker 里存的是一个**带会话标识的私有目的地**。改写由 `UserDestinationMessageHandler` + `DefaultUserDestinationResolver` 完成（依据 Spring Framework Reference 的 "User Destinations" 章节；本篇无该类 A 级源码可引，取证方法见 §6.7 手法 3）。三种写法必须认清：

| 发出方写的 destination | 谁被命中 |
| --- | --- |
| `/user/queue/notify`（客户端 `SUBSCRIBE`） | **当前会话**的 `Principal`，解析成带会话标识的私有目的地（形如 `/queue/notify-user{sessionId}`） |
| `/user/{name}/queue/notify`（任意方，含客户端 `SEND`） | 名为 `{name}` 的用户；扇出范围取决于 `@SendToUser` 的 `broadcast` 语义 |
| `/queue/notify`（没有 user 前缀） | **谁都不命中**——它被当成一个普通队列名 |

服务端 `NotifyServiceImpl` 写的正是**不带 user 前缀**的那种：

```java
// ai-cs-notify/src/main/java/com/aics/notify/service/impl/NotifyServiceImpl.java#L17-L31
public static final String USER_NOTIFY_DESTINATION = "/queue/notify";
...
public void sendToUser(Long userId, String message) {
    log.info("STOMP 定向通知: userId={}", userId);
    messagingTemplate.convertAndSendToUser(userId, USER_NOTIFY_DESTINATION, message);
}
...
messagingTemplate.convertAndSend("/topic/notify", message);
```

`convertAndSendToUser(user, destination, payload)` 内部把 user 名字塞进消息头并交给 `UserDestinationMessageHandler`，语义等价于发往 `/user/{user}/queue/notify`。所以**服务端不写 `/user`、客户端必须写 `/user`**；这条不对称是本篇最容易记错的一点。

绑定关系是三件东西的乘积：`sessionId`（WebSocket 会话）、`Principal`（`CONNECT` 帧里设的）、`subscriptionId`（`SUBSCRIBE` 帧的 `id` 头）。`SUBSCRIBE` 到达时，resolver 用**这个会话的 Principal** 把它落成私有目的地；`MESSAGE` 出站时靠 `subscription` 头指回 `subscriptionId`，客户端据此把帧派给对应回调。

**为什么这么设计。** 因为"用户"与"连接"是多对多：一个人可以开三个标签页（三个 session），一个 session 也能换人（重新 CONNECT）。如果把 `/queue/notify` 当"用户 1001 的邮箱"，broker 就得维护全局 `userId → session` 表——这正是 `SimpleBroker` 单机能做、集群做不了的事。把私有目的地做成会话级、再由 resolver 完成"一个名字 → 该名字下所有 session"的扇出，难点就收敛到 resolver 一层。

**换写法会怎样。**

- 客户端也订 `/queue/notify`：退化成"谁先订阅谁抢到"，多标签页只有一个能收到，多实例更不可控。
- 用 `@SendToUser` 代替 `convertAndSendToUser`：那是"服务端处理客户端 `SEND` 后回消息"的场景（`@MessageMapping` 方法的返回值自动投递）。`broadcast=false`（默认）只回给发起的那一个 session，`broadcast=true` 才发给该用户所有 session。本项目没有 `@MessageMapping`（C 级：全库 0 处），用不上。
- 自己维护 `Map<userId, WebSocketSession>`：旧通道 `SESSION_MAP` 的做法，问题清单见 §3.7。

**常见坑。**

1. 客户端 `subscribe('/queue/notify')` 少了 `/user`：永远收不到，这是第一高频故障。
2. 服务端 `convertAndSendToUser("1001", "/user/queue/notify", ...)` 多写了 `/user`：前缀叠加，落不到任何订阅。
3. 同名不同源：`Principal` 来自 JWT subject（`"1001"`），发送方传 `String.valueOf(dto.getUserId())`（`ai-cs-notify/src/main/java/com/aics/notify/service/impl/NotifyHandoffServiceImpl.java#L31`），两边一致才命中；任一侧加了前缀或空格就断链。
4. 同一用户多标签页时，`convertAndSendToUser` 会命中该用户**所有**会话；只想推当前页面得用 `@SendToUser(broadcast=false)` 或按 sessionId 定向。

### 3.5 发送门面：`SimpMessagingTemplate` 的两种语义

**在干什么。** 业务代码只需认识两个发送方法，它们对应两种投递范围：

| 方法 | 目的地形态 | 语义 | 本项目用途 |
| --- | --- | --- | --- |
| `convertAndSend(dest, payload)` | `/topic/notify` | 广播：所有订阅者 | `NotifyServiceImpl#broadcast`（第 28~31 行） |
| `convertAndSendToUser(user, dest, payload)` | 目标名 + `/queue/notify` | 定向：该用户的所有会话 | `sendToUser`（第 22~25 行）、转人工通知 |

方法名里的 "convert" 指用 `MessageConverter`（默认 `MappingJackson2MessageConverter`，Boot 4 下与 Jackson 3 的搭配见 §7）把对象序列化成 JSON 再包成 `Message`。`convertAndSendToUser` 多一层：把 user 名字塞进 `SimpMessageHeaderAccessor`，由 `UserDestinationMessageHandler` 改写目的地。转人工通知还演示了"先自己序列化再发字符串"的写法（`NotifyHandoffServiceImpl` 第 28~32 行：`objectMapper.valueToTree(dto)` → 加 `event=HANDOFF` → `writeValueAsString`）。

**为什么这么设计。** 把"投递范围"表达在**目的地字符串**里而不是方法参数里，好处是 broker 不需要理解业务——它只会"往这个目的地扇出"，前缀语义由 handler 层解释。代价同样明显：**目的地写错不编译报错、不运行时报错**，这是所有坑的总根源。

**换写法会怎样。** 直接用 `messagingTemplate.send(Message)` 手搓 `SimpMessageHeaderAccessor`，能控制 `simpSessionId`、`simpDestination` 等底层头，适合"只投给某个 session"的定制；代价是这些头名属于实现细节，升级时要复核。

**常见坑。**

1. 一次广播给 10 万人：`convertAndSend` 在调用线程里同步遍历订阅、逐条投进 `clientOutboundChannel` 队列；队列满会阻塞，严重时超时丢消息。批量推送应改成 MQ 分发 + 限速，不要在 HTTP 请求线程里循环广播。本项目 `NotifyController#broadcast`（第 38~41 行）就是直接在请求线程里广播——内网演示够用，公网要改。
2. 用字符串 payload 又没设 `content-type`：接收端拿到的是字符串而不是对象，前端 `JSON.parse` 才会失败（错误落在前端，不在服务端日志里）。
3. `convertAndSendToUser` 的首参是 `Object`，传 `Long` 1001 与 `String` "1001" 结果都是 `"1001"`，但传 `null` 会直接发到 `/user/null/...`，无异常。

### 3.6 `SimpleBroker` 的单机边界与 relay 方案

**在干什么。** `enableSimpleBroker("/topic", "/queue")` 装的是 `SimpleBrokerMessageHandler`：订阅表在 JVM 堆里，消息**不落盘、不跨进程**。

| 维度 | SimpleBroker（本项目现状） | 外部 relay（`enableStompBrokerRelay`） |
| --- | --- | --- |
| 订阅表位置 | 本进程内存 | 外部 broker（RabbitMQ STOMP 插件 / Artemis） |
| 多实例 | ❌ 连在 A 节点的用户收不到 B 节点发的消息 | ✅ 各节点连同一个 broker |
| 消息持久化 | 无 | 有（队列 + ack） |
| 断线补偿 | 无 | 依赖 broker 队列语义 |
| 运维成本 | 零 | 多一套中间件与账号/TLS/心跳配置 |
| 本项目证据 | `enableSimpleBroker`（`StompWebSocketConfig` 第 22 行） | `enableStompBrokerRelay` 全库 0 命中（C 级） |

需要说清一件常被混为一谈的事：**Redis 不能被 `enableStompBrokerRelay` 直接当 relay 用**。relay 指"能讲 STOMP 协议的外部 broker"（RabbitMQ 的 stomp 插件、Artemis 的 STOMP acceptor）；Redis 只有 pub/sub，得自己写广播桥。三条现实可行的多实例方案：

1. **MQ + 各节点本地 broker（对本项目改动最小）**：`OrderNotifyConsumer` 消费 `notify-topic` 后调用**本节点**的 `SimpMessagingTemplate.convertAndSendToUser`。前提是消息要能到达每个节点——RocketMQ 的广播消费（`MessageModel.BROADCASTING`）或每节点独立 consumerGroup。若仍用集群消费，消息只会被某一个节点消费、只推给它本地连接的那批用户，这就是"随机丢"的另一种成因。
2. **Redis pub/sub 桥**：节点收到发送请求 → `PUBLISH`；每个节点订阅，收到后只推给本地在线的该用户。实现简单，但不保证送达。
3. **换 STOMP relay**：`enableSimpleBroker` 换成 `enableStompBrokerRelay`，语义最完整（持久队列、ack、跨节点一致），运维成本最高。

**为什么这么设计。** SimpleBroker 的存在意义是"零依赖起步"，让单机开发不必先装一套 broker；代价是把集群问题留给使用者。这也是为什么 Spring 提供了 relay 开关而不是默认外部 broker。

**换写法会怎样。** 见上面三条。注意 relay 模式下 `setUserDestinationPrefix` 的语义**不变**，变的只是订阅表位置——user 前缀那套逻辑仍在 Spring 侧。

**常见坑。**

1. "起两个实例就丢"最常见的原因未必是 broker：可能是负载均衡把同一用户的两个连接分到不同节点，或 MQ 消费组让消息只到一个节点。先查这两处，再怀疑 broker。
2. relay 的 `clientLogin`/`systemLogin`、虚拟主机、心跳都要显式配置；配错时现象是"服务起来了但发消息一直超时"。
3. 从 SimpleBroker 切 relay 前，确认没有代码依赖"本地同步返回"——SimpleBroker 是本地调用，relay 是网络调用，延迟量级不同，压测结论不能直接沿用。

### 3.7 旧通道并存：`SESSION_MAP` 的五个问题

**在干什么。** `WebSocketConfig` 把 `/ws/notify` 交给一个 `TextWebSocketHandler` 子类，路由全靠一个静态 map：

```java
// ai-cs-notify/src/main/java/com/aics/notify/websocket/NotifyWebSocketHandler.java#L25
private static final ConcurrentHashMap<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();
```

身份从 URL 查询参数取（第 120~132 行 `extractUserId`），`ping` 回 `pong`（第 39~47 行），找不到人就 warn 丢弃（第 91~103 行）。注册处是 `ai-cs-notify/src/main/java/com/aics/notify/config/WebSocketConfig.java#L29-L30`。

| 问题 | 具体表现 | STOMP 通道里为什么没这个问题 |
| --- | --- | --- |
| 身份可伪造 | `?userId=1001` 随手改 | `Principal` 来自签名过的 JWT |
| 手写会话生命周期 | 断开、半开、重连都要自己管 | `SubProtocolWebSocketHandler` + 心跳 |
| 单机 | `static` map，跨节点不可见 | 同一问题仍在，但边界清晰，可换 relay / MQ 分发 |
| 语义不统一 | 广播与定向各写一套方法 | 目的地前缀统一表达 |
| 静默丢弃 | 找不到 session 只打 warn | 找不到订阅同样静默，但能用 TRACE 看到目的地不匹配 |

**为什么这么设计（历史上）。** 早期只要"能推"就够，裸 WebSocket 最省事；后来要"推给正确的人、要鉴权、要能多说几个业务"，才有 STOMP 通道。两套并存是灰度中的现实状态，不是设计目标。

**换写法会怎样。** 正确收尾只有一条：`OrderNotifyConsumer` 改调 `NotifyService#sendToUser`，然后删掉 `WebSocketConfig` 与 `NotifyWebSocketHandler`。留着的每一天，排查成本都在翻倍——因为"收不到"永远要先问一句"你在哪条通道上"。

**常见坑。**

1. `SESSION_MAP` 是 `static`，同 JVM 内所有实例共享；测试与多 handler 实例场景会互相污染。
2. 旧通道的 `handleTransportError`、`afterConnectionClosed` 都要手动 `remove`，漏一处就内存泄漏（map 里的 session 永不释放）。
3. 两套通道用同一个端口：改 `server.port` 或加 ws 路由时，必须同时考虑两条路径。

### 3.8 身份语义旁证：`Principal` 只有一个名字

**在干什么。** 下表这行是 `assets/source` 里唯一的"身份"相关摘录。**它不是 STOMP 类**（来源是网关限流组件 `spring-cloud-gateway-server-webflux:5.1.0`），此处只借用它说明 `Principal` 的语义：

```java (src: PrincipalNameKeyResolver.java.md#L31-L33)
public Mono<String> resolve(ServerWebExchange exchange) {
    return exchange.getPrincipal().flatMap(p -> Mono.justOrEmpty(p.getName()));
}
```

**为什么这么设计。** Spring 在所有子系统里统一用"一个 `Principal`，一个 `getName()`"表示身份（`[[源码:PrincipalNameKeyResolver.java.md#L23-L33]]`）。STOMP 的 user destination 也一样：它不比对用户对象、不查库，只比对名字。因此"名字的产生规则"（JWT subject）与"名字的消费规则"（`convertAndSendToUser` 首参）必须由同一份约定约束。

**换写法会怎样。** 想按"角色/租户"投递，不要试图改造 `Principal`——在 `convertAndSendToUser` 的层面另建命名空间（例如 `tenantId:role`）或改用 `/topic/{tenant}` 这类目的地区分，语义更清楚。

**常见坑。** 以为 `Principal` 能带用户对象（昵称、手机号）——它只有一个 `getName()`，需要更多信息请放进 JWT claims 或用 user destination 之外的机制传递。

## 4. 一次完整执行时序

场景：用户 1001 打开通知页并订阅，随后运营调 `POST /api/notify/send` 给他发一条定向通知。

```mermaid
sequenceDiagram
    autonumber
    participant C as 浏览器 NotifyView
    participant T as Tomcat 8086
    participant IC as clientInboundChannel
    participant CI as StompConnectAuthInterceptor
    participant UD as UserDestinationMessageHandler
    participant SB as SimpleBroker
    participant CTL as NotifyController
    participant SVC as NotifyServiceImpl
    participant TPL as SimpMessagingTemplate
    participant OC as clientOutboundChannel

    C->>T: GET /ws-stomp + Upgrade: websocket + Origin
    T-->>C: 101 Switching Protocols
    C->>IC: CONNECT(Authorization: Bearer JWT, heart-beat)
    IC->>CI: preSend 判断 StompCommand.CONNECT
    CI->>CI: validateToken / getSubject / setUser
    IC-->>C: CONNECTED(version, heart-beat)
    C->>IC: SUBSCRIBE id=sub-0 destination=/user/queue/notify
    IC->>UD: 带 user 前缀，交 resolveDestination
    UD->>SB: 订 会话私有目的地
    CTL->>SVC: POST /api/notify/send (userId=1001)
    SVC->>TPL: convertAndSendToUser("1001", "/queue/notify", msg)
    TPL->>UD: 目的地加 user 前缀
    UD->>SB: 扇出到 1001 的所有会话
    SB->>OC: MESSAGE(destination, subscription=sub-0)
    OC-->>C: 帧写到 socket
```

> **读图说明**：
> - 第 1~7 步是**连接期**，只在建立时发生一次；第 10~15 步是**发送期**，每条消息都走。排查时两期必须分开：连接期错了（第 3~6 步）表现为"连不上 / 连上即断"，发送期错了（第 11~14 步）表现为"连上了但没消息"。
> - 第 6 步是唯一一处"服务端主动回帧"，`CONNECTED` 之后协议才算就绪；在此之前客户端发的任何帧都不该被处理。
> - 第 15 步的 `subscription=sub-0` 是客户端派发的依据。若第 8 步 destination 少写 `/user`，第 13 步会扇出到 **0 个订阅**，**整条链路无任何错误**。
> - 图中没画 MQ 分支：`OrderNotifyConsumer` → `NotifyWebSocketHandler` 完全不经过 `SB`，这正是要分开检查的原因。

哪一步失败报什么错：

| 失败步骤 | 现象 | 报错 / 依据 |
| --- | --- | --- |
| 1（端口或端点错） | 连接立即失败，无任何 WS 帧 | 浏览器 `WebSocket connection failed`；服务端 access log 404（本项目前端连 8085 即此类，见 §0 现象 1） |
| 1~3（Origin 不在白名单） | 握手被拒 | 响应不带 `Access-Control-Allow-Origin`，`OriginHandshakeInterceptor` 侧有 debug 日志 |
| 3（缺头 / Token 无效） | 连上即刻断开 | `IllegalArgumentException: STOMP CONNECT 缺少 Bearer Token` 或 `STOMP CONNECT Token 无效或已过期`（`StompConnectAuthInterceptor` 第 32、36 行），客户端收到 `ERROR` 帧 |
| 8（destination 少 `/user`） | 连上但永远没有 MESSAGE | 无业务报错；TRACE 里能看到实际订阅的是 `/queue/notify` |
| 11~13（user 名不一致） | 发送"成功"，无人收到 | 无报错；broker 订阅表无匹配 |
| 15（出站缓冲区/会话限制） | 偶发丢失 | `SessionLimitExceededException` 或发送超时（见 §6.6） |
| MQ 分支 | 订单通知前端收不到 | warn `用户不在线，无法发送消息: userId=...`（`NotifyWebSocketHandler` 第 100 行附近） |

## 5. 设计取舍

| 方案 | 上手成本 | 定向能力 | 多实例 | 断线补偿 | 典型适用 |
| --- | --- | --- | --- | --- | --- |
| 轮询 HTTP | 极低 | 天然按用户 | ✅ | 靠下次轮询 | 状态类、低频 |
| SSE | 低 | 每条连接一个用户 | 需自建分发 | 需 `Last-Event-ID` | 服务端单向流（AI 流式回答） |
| 裸 WebSocket 自建协议 | 中 | 自己维护 map | ❌（除非自建） | 自己做 | 极简 demo / 旧灰度通道 |
| STOMP + SimpleBroker（本项目现状） | 中 | user 前缀 | ❌ | 无 | 单机、灰度 |
| STOMP + 外部 relay | 高 | user 前缀 | ✅ | broker 持久队列 | 生产集群 |
| MQ + 各节点本地 broker | 中 | 同 STOMP | ✅（广播消费） | MQ 重试 | 已有 MQ 的平台（对本项目最现实） |

可扩展点清单（按改动面从小到大）：

1. `OrderNotifyConsumer#onMessage` 改调 `NotifyService#sendToUser`，随后删除旧通道——一行改动，消掉最贵的一类 bug。
2. `NotifyServiceImpl#getOnlineCount` 目前恒返回 0（第 34~37 行）；接 Redis 计数器或监听 `SessionConnectedEvent`/`SessionDisconnectEvent` 才能真正统计在线数。
3. `configureMessageBroker` 补 `setHeartbeatValue` + `setTaskScheduler`，让服务端主动发心跳（否则保活全靠代理超时）。
4. `configureClientOutboundChannel` 换自定义 `TaskExecutor` 并设发送超时，避免大批量广播把入站线程拖死。
5. 引入 `spring-security-messaging`，用 `MessageMatcherDelegatingAuthorizationManager` 对 `SUBSCRIBE` 目的地授权（现在任何人订阅 `/topic/notify` 都无需身份）。
6. 前端补 `@stomp/stompjs`（`ai-cs-frontend` 工程根目录的 `package.json` 目前无该依赖），把页面从原生 `WebSocket` 换成 STOMP 客户端。
7. 加"未读补投"：MQ 消费成功后把通知落库，用户上线/重连时拉取未读，补上简单 broker 完全缺失的离线语义。

## 6. 踩坑与调试手法

### 6.1 三处对齐检查表（收不到时的第一件事）

| 检查点 | 正确值 | 从哪看 |
| --- | --- | --- |
| 客户端订阅 | `/user/queue/notify` | DevTools → Network → WS → Messages 里的 `SUBSCRIBE` 帧 |
| 服务端发送方法 | `convertAndSendToUser(user, "/queue/notify", ...)` | `NotifyServiceImpl` 第 23 行 |
| Principal 名字 | JWT `sub`，如 `"1001"` | `StompConnectAuthInterceptorTest` 断言 `getName()` |
| 发送方 user 名 | 与上一行完全一致 | `NotifyHandoffServiceImpl` 第 31 行 `String.valueOf(...)` |

四行里任何一行不同源，结果都是"无消息、无异常"。

### 6.2 多实例随机丢：先分清三种成因

①用户会话在 A 节点、发送发生在 B 节点的 SimpleBroker；②MQ 消费组把消息只投给一个节点；③负载均衡无会话保持，重连换了节点（用户以为自己一直在线）。判据：分别在两个节点的日志里 grep 同一个 `userId` 的发送记录——只有一侧有，就属于①或②；两侧都有而用户仍报收到少，属于③。

### 6.3 连接被"莫名其妙"断开

- 反向代理/负载均衡的空闲超时（Nginx 默认 `proxy_read_timeout 60s`）会切断长时间无数据的连接；心跳间隔必须小于它，并确保代理透传 `Upgrade`/`Connection` 头。配置细节见 [Nginx 与 OpenResty 反向代理](../04-中间件/13-Nginx与OpenResty反向代理.md)。
- 服务端没配心跳（§3.2 坑 1）时，连接在 NAT 表过期后进入半开状态，客户端 `onclose` 可能十几分钟后才触发，期间所有推送都"发成功、收不到"。
- 网关侧没有 ws 路由（§3.1 坑 1）：一旦有人把前端地址改成 `ws://gateway:8080/ws-stomp`，得到的是 404 而不是 101。

### 6.4 CORS / Origin 白名单

`setAllowedOriginPatterns` 用的是**精确 Origin**（含协议与端口），`http://localhost:5173` 与 `http://127.0.0.1:5173` 是两个值。前端换域名或端口后握手失败，而走网关的 HTTP 接口（由网关侧 CORS 处理）仍正常——这是"接口好使、WebSocket 连不上"的常见解释。跨域机制背景见 [浏览器原理与 HTTP 跨域](../06-前端开发/10-浏览器原理与HTTP跨域.md)。

### 6.5 前端没有 STOMP 客户端

`ai-cs-frontend/src/views/NotifyView.vue` 用的是原生 `WebSocket`，项目里没有 `@stomp/stompjs`（`ai-cs-frontend` 工程根目录的 `package.json` 中 `stomp`/`sockjs` 0 命中）。原生 API 不会自动发 `CONNECT`/`SUBSCRIBE`，所以即使把 URL 改成 `ws://localhost:8086/ws-stomp`，服务端看到的也只是一条既非 STOMP 协议、也非旧协议的连接：握手成功、随后因无合法帧被关掉。要通，必须引入客户端库并显式订阅。

### 6.6 连接数与内存的估算口径

- **线程**：升级完成后 Tomcat 不再为该连接独占工作线程（帧由容器 poller 读、投给 Spring 的执行器）。真正吃线程的是 `clientInboundChannel` / `clientOutboundChannel`，其默认执行器由 `WebSocketMessageBrokerConfigurationSupport` 创建，core 池为 `CPU × 2`；可以在 `configureClientInboundChannel` / `configureClientOutboundChannel` 里换掉。
- **堆**：每条连接至少占一个 socket fd、一个 `WebSocketSession` 实现对象和出入站缓冲。不要背"每个连接 xxKB"的经验数，用 `jcmd <pid> GC.class_histogram` 数 `StandardWebSocketSession` 一类实例、再对比连接前后的 `jcmd <pid> GC.heap_info`，用实测值乘以目标连接数。
- **单连接发送缓冲**：`ConcurrentWebSocketSessionDecorator` 默认发送缓冲上限约 512KB，超限抛 `SessionLimitExceededException`；可用 `setSendBufferSizeLimit`、`setSendTimeLimit` 调整。慢客户端（手机弱网）是最常见的触发者。
- **容器连接上限**：Tomcat NIO 的 `maxConnections` 默认 8192，超过后新连接进入等待或拒绝；Spring Boot 下用 `server.tomcat.max-connections` 调整，同时确认 `server.tomcat.threads.max` 足够处理帧。
- **判据**：如果"消息丢"的现场是 `SessionLimitExceededException`，那是**客户端消费不过来**，与 broker、user destination 都无关——别在这条线上耗时间。

### 6.7 调试手法（命令 + 怎么读）

**手法 1：三处事实交叉核对（30 秒内，不启服务）。** 收不到时先排除"连错服务"：

```powershell
cd D:\Projects\Persion\ai-customer-service
Select-String -Path ai-cs-frontend\src\views\NotifyView.vue -Pattern "new WebSocket"
Select-String -Path ai-cs-notify\src\main\resources\application.yml -Pattern "port:"
Select-String -Path ai-cs-message\src\main\resources\application.yml -Pattern "port:|name:"
```

真实输出：

```text
--- 前端连的地址 ---
114: ws = new WebSocket('ws://localhost:8085/ws/notify')
--- notify 实际端口 ---
3: port: 8086
--- 8085 是谁 ---
3: port: 8085
9: name: ai-cs-message
```

怎么读：第一行是前端写死的目标；第二行说明 notify 在 8086；第三、四行证明 8085 属于 `ai-cs-message`。三者拼起来就是"连错服务、且连的是旧路径"。这个手法适用于所有"前端连不上后端"的问题：**先把双方的地址摆到同一屏上**。

**手法 2：能力面清点（确认文档描述的就是这份代码）。**

```powershell
# 统计 STOMP 相关实现点，排除 target 目录
Get-ChildItem -Recurse -File -Filter *.java |
  Where-Object { $_.FullName -notmatch "\\target\\" } |
  Select-String -Pattern "WebSocketMessageBrokerConfigurer|@MessageMapping|HandshakeInterceptor|enableStompBrokerRelay|withSockJS" -List
```

真实输出（按模式分别计数）：

```text
WebSocketMessageBrokerConfigurer 1
@MessageMapping 0
HandshakeInterceptor 0
enableStompBrokerRelay|SockJS|withSockJS 0
```

怎么读：只有 1 个配置类 ⇒ 全项目就一套 STOMP 配置；`@MessageMapping` 0 ⇒ 没有客户端→服务端的消息入口，`@SendToUser` 也不会出现；`HandshakeInterceptor` 0 ⇒ "握手期取 token"在这份代码里不成立，实际是 `ChannelInterceptor`；relay 0 ⇒ 单机 broker。**先数清楚再下结论**，比读十篇博客可靠。

**手法 3：框架侧开 TRACE 看目的地改写。** 在 `ai-cs-notify/src/main/resources/application.yml` 临时加 `logging.level.org.springframework.messaging: TRACE` 与 `logging.level.org.springframework.web.socket: TRACE`，复现一次订阅与一次定向发送。日志里会出现 `SUBSCRIBE ... destination=/user/queue/notify`，以及随后解析出的私有目的地。这是唯一能直接看到"user 前缀被改写成什么"的手段——本篇 §3.4 关于改写形式的表述依据官方文档，正需要这条日志坐实。

**手法 4：看握手响应，不要猜。** DevTools → Network → WS 的握手请求里：状态码必须是 `101`；`Access-Control-Allow-Origin` 应回显你的页面 Origin。返回 `200`/`404` 是路由或端口问题；无 CORS 头或 `403` 是白名单问题——两类问题修法完全不同，别混着改。顺带这一步就能验证 §3.1 坑 3 里那个占位符到底有没有被解析。

**手法 5：卡在"服务端说发了"时，去 broker 侧取证。** SimpleBroker 场景把 `org.springframework.messaging` 开到 TRACE，能看到订阅/取消订阅与 destination；relay 场景用 broker 自带管理界面看目的地的消费者数。判据很硬：**broker 里没有该订阅，发送方无论怎么写都是丢**。

## 7. ⚠️ 版本差异

| 旧写法 | 当前基线（Framework 7.1 / Boot 4.2） | 变化点 | 迁移动作 |
| --- | --- | --- | --- |
| 握手期用 `HandshakeInterceptor` 从 query 取 token | 帧期用 `ChannelInterceptor` 读 `CONNECT` 的 `Authorization` | 浏览器原生 WS 不能带自定义头，STOMP 帧可以；帧期还能复用 JWT 校验 | 注册到 `configureClientInboundChannel`，并用 `MessageBuilder` 重建 `Message`（§3.3） |
| `spring-boot-starter-web` 顺带带上 WebSocket | `spring-boot-starter-websocket` 依赖 `spring-boot-starter-webmvc`；`starter-web` 在 Boot 4 已 deprecated | Boot 4 把 servlet web 与 websocket 拆成独立模块 | notify 的 `ai-cs-notify/pom.xml` 仍用 `starter-web`（第 20~23 行），迁移时换 `starter-webmvc` |
| 统一 Jackson 2（`com.fasterxml.jackson`） | Boot 4 默认 Jackson 3（`tools.jackson`） | 包名不同、`ObjectMapper` 同名不同包 | `NotifyHandoffServiceImpl` 仍 `import com.fasterxml.jackson.databind.ObjectMapper`（第 7 行），迁移时确认 Jackson 2 依赖仍在，或整体改到 `tools.jackson` |
| `setAllowedOrigins("*")` 图省事 | `setAllowedOriginPatterns("<白名单>")` | `*` 与凭证共存被禁；通配需要 pattern API | 用配置项注入白名单，禁止 `*` |
| 客户端 SockJS + 旧版 `stompjs` | 原生 `WebSocket` + `@stomp/stompjs` v7（本项目两端都还没做到） | SockJS 降级路径在新代理环境下多半无用；v7 用 `Client` 类与 `webSocketFactory` | 前端补依赖，`brokerURL` 指向 `ws://host:8086/ws-stomp` |
| `enableSimpleBroker` 一把梭 | 语义不变，但生产应 `enableStompBrokerRelay` 或 MQ 分发 | 集群化后单机 broker 必错 | 见 §3.6 三条方案 |
| JSR-356 `@ServerEndpoint` 教程 | Spring 推荐 `@EnableWebSocket` / `@EnableWebSocketMessageBroker` | JSR-356 与容器强绑定，难与 Spring 消息模型结合 | 旧代码可留灰度，新代码别再用 |
| 无心跳也能"跑起来" | 代理超时 + 半开连接在容器化环境更常见 | k8s Service/Ingress、云 LB 的空闲超时普遍在 60s 量级 | 服务端 `setHeartbeatValue` + `setTaskScheduler`，客户端设 `heartbeatIncoming/Outgoing` |

说明：上表"当前基线"一列中，凡涉及本项目代码的（starter、Jackson 包名、`ChannelInterceptor`、`setAllowedOriginPatterns`、无 relay、`pom.xml` 行号）都由 B 级证据支撑并给了行号；涉及第三方客户端库版本（`@stomp/stompjs` v7）与 Boot 4 模块拆分的部分依据官方文档与本地 `.m2` 中的依赖声明，未做运行期验证。

## 8. 面试高频问与答

1. WebSocket 握手复用了 HTTP 的什么？为什么升级之后 `Filter`、`HandlerInterceptor` 就失效了？
2. STOMP 相比裸 WebSocket 多解决了什么问题？`CONNECT` 帧的头与 HTTP 请求头有什么本质区别？
3. `@EnableWebSocketMessageBroker` 背后装配了哪几个 Bean？一条消息在哪些 Channel 之间流转？
4. `/user/queue/notify` 是怎么被解析成"某个人"的？`Principal` 从哪来、`sessionId` 与它是什么关系？
5. 客户端订阅写 `/queue/notify`、服务端 `convertAndSendToUser("1001","/queue/notify",...)` 会发生什么？为什么没有异常？
6. `@SendToUser` 的 `broadcast` 默认值是什么？它和 `convertAndSendToUser` 有什么区别？
7. `HandshakeInterceptor` 与 `ChannelInterceptor` 各自能拿到什么、拿不到什么？鉴权该放哪一层？
8. SimpleBroker 为什么在多实例下会丢消息？`enableStompBrokerRelay` 解决了什么、代价是什么？Redis 能直接当 relay 吗？
9. 心跳由谁发起、如何协商？服务端不配 `setHeartbeatValue` 会怎样？
10. 同一个用户开三个标签页，`convertAndSendToUser` 会推给几个？只想推当前页面怎么办？
11. JWT 的 `sub` 从 `"1001"` 改成 `"user:1001"` 会破坏哪些地方？为什么不会报错？
12. 断线重连后，离线期间漏掉的消息由谁补？STOMP 协议本身管这件事吗？
13. 一次广播打到 10 万订阅者，瓶颈在哪一层？怎么改？

## 9. 动手练习

1. **修通定向推送。** 把 `ai-cs-frontend/src/views/NotifyView.vue` 的 URL 改成 `ws://localhost:8086/ws-stomp`，引入 `@stomp/stompjs`，`connectHeaders` 带 `Authorization`，订阅 `/user/queue/notify`；同时让 `OrderNotifyConsumer` 改调 `NotifyService#sendToUser`。**验收**：`POST /api/notify/send?userId=<你登录的用户ID>&message=hi` 后页面收到消息，服务端日志出现 `STOMP 定向通知: userId=...` 且无 warn。
2. **故意制造三种"静默失败"。** 分别（a）客户端订阅写 `/queue/notify`；（b）服务端传 `"/user/queue/notify"`；（c）传一个不存在的 userId。**验收**：三种情况页面都收不到、服务端都没有 ERROR，并在笔记里用一句话说清各自错在哪一层。
3. **加服务端心跳。** 在 `configureMessageBroker` 里加 `setHeartbeatValue(new long[]{10000, 10000})` 与 `setTaskScheduler(...)`。**验收**：DevTools → WS → Messages 里能看到周期性空帧；拔网线后 10~20 秒内 `onclose` 触发。
4. **看清一次目的地改写。** 打开 `org.springframework.messaging=TRACE`，复现订阅与定向发送。**验收**：能从日志指出 `/user/queue/notify` 被改写成什么，并解释后缀里那串会话标识与 `sessionId` 的对应关系。
5. **两实例实验。** 用 `--server.port=8086` / `--server.port=8087` 起两个 notify 实例，两个浏览器分别连接，同一 JWT 两边都订阅。**验收**：说明 SimpleBroker 下哪一侧收不到、为什么；并给出 §3.6 中任一条改造方案的最小改动点。
6. **补真实的在线人数。** 让 `NotifyServiceImpl#getOnlineCount` 返回真实值（监听 `SessionConnectedEvent`/`SessionDisconnectEvent`，或用 Redis 计数）。**验收**：`GET /api/notify/online` 在连接/断开后正确增减，多实例下不再恒为 0。
7. **写一条补偿链路。** MQ 消费成功后把通知落库，新增"拉取未读"接口，用户连接建立后调用。**验收**：把页面关掉 5 分钟再打开，期间产生的通知能补收到，且重复推送可去重。

## 小结

- STOMP 的价值不在"能推"，而在把"推给谁"表达成**目的地字符串**：`/topic` 广播、`/queue` 队列、`/user` 定向。
- 身份链路三处必须同源：`CONNECT` 帧的 JWT → `Principal#getName()` → `convertAndSendToUser` 首参；本项目三处都是用户 ID 字符串（`UserServiceImpl` 第 100 行 / `StompConnectAuthInterceptor` 第 38~39 行 / `NotifyHandoffServiceImpl` 第 31 行）。
- 客户端订 `/user/queue/notify`、服务端发 `"/queue/notify"`——**这个不对称是设计，不是笔误**；两边都写 `/user` 或都不写，都是错的。
- 目的地不匹配不会有任何异常：`convertAndSend` 只负责交给 broker，broker 找不到订阅就丢；"收不到"必须靠 TRACE 日志与 WS 帧面板取证，读业务日志是无效的。
- `SimpleBroker` 是进程内订阅表，多实例必丢；要么换 relay，要么"MQ 广播消费 + 各节点本地 broker"，后者对本项目改动最小。
- 本项目还留着一条旧裸通道（`/ws/notify` + `SESSION_MAP`），且 `OrderNotifyConsumer` 仍走在上面——这是"订单通知收不到"的头号原因，也是排查时最容易被忽略的分叉。
- 心跳与代理超时必须成对考虑：服务端没配 `setHeartbeatValue`、代理 `proxy_read_timeout` 又是 60 秒时，连接会在无人察觉时被判死。
- 归纳成一句可操作的话：**下次遇到"消息发了但没到"，按"端点对不对 → 帧有没有 → 订阅带不带 `/user` → 名字同不同源 → broker 在不在同一台"逐层取证，四步之内必定位。**

## 自测题

**1. `@EnableWebSocketMessageBroker` 到底做了什么？**
<details><summary>参考答案</summary>

它通过 `@Import` 引入 `DelegatingWebSocketMessageBrokerConfiguration`，注册 `clientInboundChannel`、`clientOutboundChannel`、`brokerChannel` 三个通道，以及 `SimpleBrokerMessageHandler`（或 relay 版）、`UserDestinationMessageHandler`、`SimpMessagingTemplate` 等 Bean，并把实现 `WebSocketMessageBrokerConfigurer` 的配置类回调进来。
</details>

**2. 为什么浏览器原生 WebSocket 不能带 `Authorization` 头？STOMP 如何绕开这个限制？**
<details><summary>参考答案</summary>

`new WebSocket(url, protocols)` 只接受 URL 与子协议名，没有任何 header 参数（浏览器安全限制），凭证只能放 cookie、query 或子协议里。STOMP 把凭证放进 `CONNECT` 帧的 native header，`StompConnectAuthInterceptor` 第 30 行通过 `getFirstNativeHeader("Authorization")` 取出，帧是应用层数据，不受该限制。
</details>

**3. `StompConnectAuthInterceptor#preSend` 最后一行为什么必须重建 `Message`？**
<details><summary>参考答案</summary>

`setUser` 改的是 `StompHeaderAccessor` 持有的头快照，而 `ChannelInterceptor` 的约定是"返回新的 `Message` 才生效"。直接 `return message` 会让 `Principal` 丢失且不报错，后续 `DefaultUserDestinationResolver` 就认不出订阅者身份。
</details>

**4. 客户端订阅 `/user/queue/notify` 与服务端 `convertAndSendToUser(user, "/queue/notify", msg)` 为什么是配套的？**
<details><summary>参考答案</summary>

`convertAndSendToUser` 内部把 user 名字放进消息头并委派 `UserDestinationMessageHandler`，语义等价于发往 `/user/{user}/queue/notify`；客户端订阅侧写 `/user/queue/notify`，由同一个 resolver 用该会话的 `Principal` 落成私有目的地。分工是"发送方给名字、订阅方给前缀"。
</details>

**5. 目的地不匹配时，`convertAndSendToUser` 会抛异常吗？**
<details><summary>参考答案</summary>

不会。它只负责把消息交给 broker（`SimpleBrokerMessageHandler`），订阅表里没有匹配项就直接丢弃，既不返回状态也不抛异常。所以收不到消息只能靠 TRACE 日志看实际目的地，或看客户端的 `SUBSCRIBE` 帧。
</details>

**6. `DefaultUserDestinationResolver` 认的是 `Principal` 还是 session attribute？**
<details><summary>参考答案</summary>

认 `Principal`（`SimpMessageHeaderAccessor#getUser()`）。往 `accessor.getSessionAttributes()` 里塞 userId 不会影响 user destination 解析——这是把身份放错地方的典型错误。
</details>

**7. `SimpleBroker` 在多实例下为什么丢消息？**
<details><summary>参考答案</summary>

它的订阅表是 JVM 内存里的注册表（`SimpleBrokerMessageHandler` 内部的 `SubscriptionRegistry`）。用户连在节点 A，订阅只存在 A；发送若发生在节点 B，B 的 broker 没有该订阅，消息即被丢弃。解决要么用外部 relay，要么 MQ 广播到各节点后调本节点 `NotifyServiceImpl#sendToUser`。
</details>

**8. Redis 能直接配成 `enableStompBrokerRelay` 吗？**
<details><summary>参考答案</summary>

不能。relay 需要一个能讲 STOMP 协议的外部 broker（RabbitMQ 的 stomp 插件、ActiveMQ Artemis）。Redis 只有 pub/sub，需要自己写"发布/订阅桥"完成跨节点分发，而且不保证送达。
</details>

**9. `@SendToUser` 的 `broadcast` 默认值是什么？**
<details><summary>参考答案</summary>

默认 `false`，即把 `@MessageMapping` 方法的返回值只投给**发起该消息的那个 session**；`broadcast=true` 才投给该用户在该节点上的所有 session。本项目没有 `@MessageMapping`（全库 0 处），所以这条路径用不上。
</details>

**10. 服务端不配 `setHeartbeatValue` / `setTaskScheduler` 会怎样？**
<details><summary>参考答案</summary>

`SimpleBroker` 侧心跳不会启动，服务端不发空帧。连接是否被切断完全取决于客户端 `heart-beat` 请求与代理/负载均衡的空闲超时，容易出现半开连接：服务端认为在线、所有推送"发送成功"，而客户端早已收不到。
</details>

**11. 为什么本项目 `Principal` 的名字是 `"1001"` 这种纯数字字符串？**
<details><summary>参考答案</summary>

因为 `ai-cs-user` 的 `UserServiceImpl` 第 100 行用 `JwtUtil.generateToken(String.valueOf(user.getId()), ...)` 把用户 ID 作为 JWT subject；`StompConnectAuthInterceptor` 第 38 行 `JwtUtil.getSubject` 取出的正是这个字符串，直接当 `Principal#getName()`，于是与 `NotifyHandoffServiceImpl` 第 31 行的 `String.valueOf(dto.getUserId())` 对得上。
</details>

**12. `OrderNotifyConsumer` 消费 MQ 后调的是哪个类？后果是什么？**
<details><summary>参考答案</summary>

调 `NotifyWebSocketHandler.sendMessageToUser`（`ai-cs-notify/src/main/java/com/aics/notify/consumer/OrderNotifyConsumer.java` 第 40 行附近），即旧裸 WebSocket 通道的静态 `SESSION_MAP`，而不是 `NotifyServiceImpl#sendToUser` 的 STOMP 路径。后果是 STOMP 订阅者永远收不到订单通知，且查不到 session 时只打一行 `用户不在线` 并静默丢弃。
</details>

**13. 一次 `/api/notify/broadcast` 打到 10 万订阅者，风险在哪？**
<details><summary>参考答案</summary>

`convertAndSend` 在调用线程里同步遍历订阅、逐条投进 `clientOutboundChannel` 队列；队列满会阻塞或超时丢消息，把 HTTP 请求线程一起拖住。应改为 MQ 分发 + 限速，并给 `clientOutboundChannel` 配独立 `TaskExecutor` 与发送超时。
</details>

**14. 想保证"用户离线期间漏掉的通知"不丢，STOMP 协议本身能保证吗？**
<details><summary>参考答案</summary>

不能。STOMP 只是帧协议，离线消息要靠 broker 的持久队列 + ack（外部 relay）或业务侧落库重投（MQ 重试 + 上线拉取未读）。`SimpleBroker` 完全没有持久化，断线即丢。
</details>
