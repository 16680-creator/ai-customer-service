# SSE 与 WebSocket 实时通信

> 本项目在 **AI 对话（ai-cs-chat）** 和 **通知推送（ai-cs-notify）** 两个场景用到实时通信：
> - **SSE**：AI 对话流式输出（逐字"打字机"效果）
> - **WebSocket**：站内通知实时推送（`NotifyWebSocketHandler`）
>
> 对应项目文件：`ai-cs-notify/src/main/java/com/aics/notify/websocket/NotifyWebSocketHandler.java`、`ai-cs-notify/src/main/java/com/aics/notify/config/WebSocketConfig.java`

---

## 一、为什么需要实时通信？

```
传统 HTTP（请求-响应）：
客户端 ──请求──▶ 服务端
        ◀──响应── 服务端
  一次只能一问一答，服务端不能主动推送

实时通信：
服务端可以在任意时刻主动把数据推给客户端
```

### 典型场景

| 场景 | 技术要求 | 为什么 |
|------|---------|--------|
| AI 逐字生成回答 | 流式推送 | 传统 HTTP 要等 AI 全部生成完才返回，体验差 |
| 新订单通知 | 服务端主动推送 | 管理员要实时看到新订单 |
| 消息未读数 | 实时更新 | WebSocket 长连接保持 |
| 股票行情 | 高频推送 | 适合 WebSocket |

---

## 二、两种方案的对比

| 维度 | SSE (Server-Sent Events) | WebSocket |
|------|--------------------------|-----------|
| 方向 | **单向**（服务端 → 客户端） | **双向**（客户端 ↔ 服务端） |
| 协议 | 基于 HTTP（复用 HTTP 连接） | 独立协议（ws://，需握手升级） |
| 数据格式 | 纯文本（可自定义） | 文本或二进制 |
| 自动重连 | 浏览器内置重连机制 | 需自己实现 |
| 复杂度 | 低 | 高 |
| 适用 | AI 流式输出、消息推送 | 聊天、游戏、实时协作 |

### 选型建议

```
AI 流式输出（只管往下推）        → SSE
需要双向交互（如实时游戏）        → WebSocket
需要服务端主动+客户端也要发       → WebSocket
```

---

## 三、SSE 实现（AI 流式对话）

### 3.1 概念

SSE 本质还是一个 HTTP 响应，但 `Content-Type: text/event-stream`，服务端可以分多次发送数据，连接保持打开。

```
HTTP 响应头：
Content-Type: text/event-stream

响应体（多次发送）：
data: 你好
data: 我是
data: AI客服
```

### 3.2 Spring AI 流式调用

```java
// 对应 ai-cs-chat/src/main/java/com/aics/chat/service/ChatService.java
// 流式调用模型，返回 Flux（响应式流）
import reactor.core.publisher.Flux;

public Flux<String> chatStream(String sessionId, String message) {
    // ChatClient.stream() 会逐 token 回调
    return chatClient.prompt()
            .user(message)
            .stream()
            .content();      // Flux<String>：每个元素是一个 token
}
```

### 3.3 SSE 控制器（返回 Flux）

```java
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message) {
        return chatService.chatStream("session-1", message);
    }

    // 也可以手动拼 SSE 事件格式
    @PostMapping(value = "/stream2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamManual(@RequestParam String message) {
        return Flux.just("事件1", "事件2", "事件3")
            .map(data -> "data: " + data + "\n\n");  // SSE 格式：data: 内容
    }
}
```

### 3.4 前端接收 SSE

```javascript
// Vue3 中使用 EventSource 接收 SSE
const eventSource = new EventSource('/api/chat/stream?message=你好')

eventSource.onopen = () => console.log('连接已建立')
eventSource.onmessage = (event) => {
  // 每次收到一个 data，追加到 AI 回复框，实现打字机效果
  aiReply.value += event.data
}
eventSource.onerror = () => console.log('连接出错，浏览器会自动重连')
```

---

## 四、WebSocket 实现（通知推送，本项目已实现）

### 4.1 配置类

```java
// ai-cs-notify/src/main/java/com/aics/notify/config/WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new NotifyWebSocketHandler(), "/ws/notify")
                .setAllowedOrigins("*");   // 生产环境应限定来源
    }
}
```

### 4.2 处理器（核心）

```java
// ai-cs-notify/src/main/java/com/aics/notify/websocket/NotifyWebSocketHandler.java
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    /** 在线连接管理：userId -> WebSocketSession（线程安全） */
    private static final ConcurrentHashMap<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    // 1. 连接建立：记录 userId 到 map
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        if (userId != null) {
            SESSION_MAP.put(userId, session);
        }
    }

    // 2. 收到客户端消息：处理心跳
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));  // 心跳，保持连接
        }
    }

    // 3. 连接关闭：移除用户
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = extractUserId(session);
        if (userId != null) {
            SESSION_MAP.remove(userId);
        }
    }

    /** 向指定用户推送消息 */
    public static void sendMessageToUser(String userId, String message) {
        WebSocketSession session = SESSION_MAP.get(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    /** 广播给所有在线用户 */
    public static void broadcastMessage(String message) {
        SESSION_MAP.forEach((userId, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("广播失败: userId={}", userId, e);
            }
        });
    }

    /** 当前在线人数 */
    public static int getOnlineCount() {
        return SESSION_MAP.size();
    }

    /** 从 URI 查询参数解析 userId，如 ws://host/ws/notify?userId=100 */
    private String extractUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if ("userId".equals(kv[0]) && kv.length == 2) {
                    return kv[1];
                }
            }
        }
        return null;
    }
}
```

### 4.3 在业务中主动推送

```java
// 例如新订单创建成功后，实时通知管理员
@Service
public class NotifyService {

    public void notifyNewOrder(Long adminId, String message) {
        // 直接调用静态方法推送
        NotifyWebSocketHandler.sendMessageToUser(
            String.valueOf(adminId),
            "{\"type\":\"NEW_ORDER\",\"message\":\"" + message + "\"}"
        );
    }
}
```

### 4.4 前端连接 WebSocket

```javascript
// Vue3 中建立 WebSocket 连接
const userId = getUserId()
const ws = new WebSocket(`ws://localhost:8085/ws/notify?userId=${userId}`)

ws.onopen = () => {
  // 连接建立后发送心跳
  ws.send('ping')
  // 定时心跳，防止连接被服务端/网关断开
  setInterval(() => ws.send('ping'), 30000)
}

ws.onmessage = (event) => {
  const data = JSON.parse(event.data)
  ElMessage.info(data.message)   // 收到新通知
}

ws.onclose = () => console.log('连接关闭')
```

---

## 五、WebSocket 实践要点

1. **心跳机制**：客户端定时发 `ping`，服务端回 `pong`，防止连接空闲被 Nginx/网关断开
2. **userId 从 URI 传**：代码里用查询参数 `?userId=100` 识别用户（生产可改用 Token 鉴权）
3. **连接管理用 ConcurrentHashMap**：多线程安全，避免并发操作 ConcurrentModificationException
4. **断线重连**：业务上要处理 `onclose` 后定时重连
5. **网关透传**：经过 Spring Cloud Gateway 时，需要配置 WebSocket 路由（`ws:` 协议）

---

## 六、动手练习

1. 启动 `ai-cs-notify`，用在线工具 `wscat` 或浏览器连接 `ws://localhost:8085/ws/notify?userId=100`
2. 调用 Notify 服务的接口触发推送，观察 WebSocket 收到消息
3. 给聊天服务写一个 SSE 接口，返回 `Flux<String>`，前端用 EventSource 接收
4. 对比 SSl 和 WebSocket 在 Nginx/网关下的配置差异

---

## 学习检查清单

- [ ] 理解 SSE 与 WebSocket 的区别和适用场景
- [ ] 理解 SSE 的 `text/event-stream` 格式
- [ ] 会用 Spring AI `chatClient.stream()` 做流式输出
- [ ] 理解 WebSocket 的四步生命周期（建立/收发/心跳/关闭）
- [ ] 会用 ConcurrentHashMap 管理在线连接
- [ ] 理解心跳机制的必要性
- [ ] 能在前端连接 WebSocket 并处理消息

---

## 下一步

→ [05-AI集成/01-SpringAI入门](../05-AI集成/01-SpringAI入门.md)