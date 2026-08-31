# 11-STOMP 实时通知与用户目的地（01-P5 落地记录）

> 2026-08 落地：notify 从裸 `TextWebSocketHandler` 迁主路径到 STOMP；
> CONNECT 帧 JWT 鉴权建立 Principal，转人工通知通过 user destination 定向推送。

## 一、裸 WebSocket 的问题

旧实现：`/ws/notify?userId=1001` 建连，服务端维护静态
`ConcurrentHashMap<userId, WebSocketSession>`，推送时手工查 Map。

问题：

- `userId` 来自 URL 查询参数，可伪造；`setAllowedOrigins("*")` 允许任意网站建连
- 多 session、断线重连、session 清理全部自己维护
- 定向/广播消息只是约定字符串，没有统一目的地语义

旧 `/ws/notify` 暂保留一版灰度，但 CORS 改成显式 `AICS_ALLOWED_ORIGINS` 白名单；
新客户端迁至 `/ws-stomp`。

## 二、STOMP 协议与目的地

STOMP 是 WebSocket 上的文本帧协议：

```text
CONNECT (Authorization: Bearer <jwt>)
SUBSCRIBE destination:/user/queue/notify

服务端：convertAndSendToUser("1001", "/queue/notify", payload)
Broker 内部路由为 /user/1001/queue/notify 的 session 队列
```

配置：

```java
registry.enableSimpleBroker("/topic", "/queue"); // 内存简单 broker
registry.setApplicationDestinationPrefixes("/app");
registry.setUserDestinationPrefix("/user");
registry.addEndpoint("/ws-stomp");
```

- `/topic/notify`：广播
- `/user/{principal}/queue/notify`：点对点（客户端只订阅 `/user/queue/notify`，不带 userId）

## 三、CONNECT 鉴权与 Principal

`StompConnectAuthInterceptor` 在 inbound channel 截获 CONNECT：

1. 取 native header `Authorization: Bearer <jwt>`
2. 复用 common `JwtUtil` 校验签名/过期时间
3. `accessor.setUser((Principal) () -> userId)`
4. 无/非法 token 直接拒绝 CONNECT

`SimpMessagingTemplate.convertAndSendToUser(userId, ...)` 使用这个 Principal 做 session 路由，
因此用户无法用 URL `userId` 冒充别人。

## 四、服务层改造

| 旧实现 | 新实现 |
|--------|--------|
| `NotifyWebSocketHandler.sendMessageToUser` 静态调用 | `SimpMessagingTemplate.convertAndSendToUser` |
| static session map | broker 管理 Principal → sessions |
| `broadcastMessage` | `convertAndSend("/topic/notify", payload)` |
| Handoff JSON 字符串推手工 session | `convertAndSendToUser(userId, "/queue/notify", json)` |

`NotifyServiceImpl.getOnlineCount()` 在 simple broker 模式返回 0：broker 不提供稳定的跨 session
在线计数 API。生产需要此指标时接外部 broker（RabbitMQ STOMP relay）或维护专门的 presence 存储，
不能再在业务服务里偷用 static Map。

## 五、外部 Broker 的扩展边界

当前 `enableSimpleBroker` 适合单实例/学习环境。多 notify 实例时 session 分散，
`convertAndSendToUser` 无法跨实例路由；升级为 RabbitMQ broker relay：

```java
registry.enableStompBrokerRelay("/topic", "/queue")
        .setRelayHost("rabbitmq")
        .setRelayPort(61613);
```

此时 broker 成为消息与 session 路由中心，才支持横向扩容。

## 六、验证

- `mvn -pl ai-cs-notify verify`：25 tests 全绿，JaCoCo 门禁通过
- CONNECT 鉴权测试：合法 Bearer JWT → Principal=userId；无 token → 拒绝
- Handoff 测试：JSON 含 `event=HANDOFF`，定向到 `/queue/notify`
- NotifyService 测试：用户目的地与 topic 广播

前端暂未接 WebSocket，待通知页面需求启用时再加入 `@stomp/stompjs`，
连接时带 `connectHeaders: { Authorization: 'Bearer ' + token }`，订阅 `/user/queue/notify`，
并实现断线重连。

## 七、面试要点

- WebSocket 是传输层，STOMP 是应用层帧协议；STOMP 提供 destination、subscribe、ACK 语义
- user destination 依赖 Principal→session 映射，CONNECT 鉴权是安全核心
- simple broker vs broker relay：单机内存 vs 外部 broker 横向扩展
- WebSocket CORS 不能 `*`；认证 token 应放 CONNECT header，不能依赖 URL userId
