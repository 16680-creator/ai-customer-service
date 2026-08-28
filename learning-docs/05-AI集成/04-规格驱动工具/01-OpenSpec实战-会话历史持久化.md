# OpenSpec 实战：会话历史持久化（Redis 热缓存 + message 表）

> 本次用一个完整需求走通 **OpenSpec 开发区模式**：`/opsx-propose`（规划）→ `/opsx-apply`（实施）→ `/opsx-archive`（归档）。
> 目录：`learning-docs/05-AI集成/04-规格驱动工具/01-OpenSpec实战-会话历史持久化.md`

---

## 一、需求背景

ai-cs-chat 服务原本用内存 `ConcurrentHashMap<String,List<Message>>` 缓存会话历史，存在三个严重问题：

1. **重启即丢失** —— 历史存在进程内存里，服务一重启全没了。
2. **多实例不共享** —— 实例 A 写的历史，实例 B 读不到。
3. **无法历史回看** —— 没有对外查询接口。

好在会话数据已经通过 MQ 异步投递到 ai-cs-message，落库到 `chat_session` / `chat_message` 表，但读取仍完全依赖内存，没用上这份持久化数据。

### 方案选型

| 方案                             | 说明                                            | 结论     |
| ------------------------------ | --------------------------------------------- | ------ |
| 全量存 Redis                      | 快，但 Redis 也非绝对可靠                              | 不行     |
| 内存 Map → 直接接 message 表         | 每次对话都查库，慢                                     | 不行     |
| **Redis 热缓存 + message 表最终持久化** | 热数据走 Redis（快），冷数据落 message 表（稳），Redis 未命中回源重建 | **选它** |

这个方案能满足：重启不丢、多实例共享、支持历史回看。

---

## 二、OpenSpec 开发经过

### 阶段 1：规划（/opsx-propose）

输入需求描述，AI 生成 4 份规划工件：

```
openspec/changes/chat-history-persistence/
├── proposal.md        # 为什么改、改什么、影响范围、能力划分
├── specs/chat/history-management/spec.md   # 行为契约（delta spec）
├── design.md          # 技术设计（Redis 结构、服务间调用、降级策略）
└── tasks.md           # 拆分成可执行任务清单
```

**关键设计决策（design.md）：**

- Redis 用 **List** 类型存会话历史，key 形如 `chat:history:{sessionKey}`。
- 消息用 **JSON** 序列化为 `ChatHistoryMessage`（`role` + `content`）存入 Redis。
- chat 侧通过 **Feign** 调 ai-cs-message（服务名 `ai-cs-message`）按 sessionKey 拉历史。
- **降级策略**：Redis 不可用 → 降级从 message 表取历史；message 也失败 → 空历史继续对话，不阻塞主流程。

### 阶段 2：实施（/opsx-apply）

按 tasks.md 逐条勾选完成：

| 任务组 | 内容                                | 结果              |
| --- | --------------------------------- | --------------- |
| 1   | ai-cs-message 提供按 sessionKey 查询能力 | 完成              |
| 2   | ai-cs-chat 引入 Redis 依赖与配置         | 完成              |
| 3   | chat 侧新增历史 DTO 与存取组件              | 完成              |
| 4   | 改造 ChatServiceImpl 使用持久化历史        | 完成              |
| 5   | chat 侧新增历史回看接口                    | 完成              |
| 6   | 验证与联调                             | 编译通过（运行时验证留待环境） |

实施中间踩的一个坑：操作中误点"拒绝"回退了一些文件，导致 mapper 和 service 接口被还原，编译报错 `找不到符号 selectBySessionKey`。排查后用 `git status` 对照发现不一致，重新补齐 mapper 的 `@Select` 方法和 service 接口申明后编译通过。

### 阶段 3：归档（/opsx-archive）

1. 先 `git commit` 提交功能代码。
2. 归档时检测到 **主线 spec 未同步**（delta spec 存在但 `openspec/specs/chat/history-management/spec.md` 还没有），选择"同步后归档"。
3. 归档命令把 change 目录移动为 `openspec/changes/archive/2026-08-11-chat-history-persistence/`，同时把 delta spec 合并进主线 `openspec/specs/chat/history-management/spec.md`。
4. 归档时存在 5 个运行环境验证类任务未勾选（如双实例验证），经确认后忽略归档。

---

## 三、生成的配置文件

### 1. ai-cs-chat/pom.xml —— 新增 Redis 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. ai-cs-chat/application.yml —— 新增 Redis 连接配置

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
```

### 3. 新增的 Java 类

| 文件                                         | 作用                                       |
| ------------------------------------------ | ---------------------------------------- |
| `dto/ChatHistoryMessage.java`              | 历史消息 DTO（`role` + `content`），Redis 序列化载体 |
| `feign/MessageFeignClient.java`            | Feign 调用 ai-cs-message 按 sessionKey 查询   |
| `service/ChatHistoryService.java`          | 历史存取接口：`load` / `append`                 |
| `service/impl/ChatHistoryServiceImpl.java` | Redis LRANGE → 回源 message 表 → 重建写回       |

### 4. ai-cs-message 新增能力

- `ChatMessageMapper.selectBySessionKey(sessionKey, limit)`：`@Select` 注解按 sessionKey 查，按 create_time 升序。
- `MessageService.getMessagesBySessionKey(sessionKey)`。
- `MessageController` 新增 `GET /api/message/session-key/{sessionKey}/messages`。

### 5. chat 侧新增历史回看接口

- `ChatService.getHistory(sessionKey)` + `ChatServiceImpl` 实现（复用 `ChatHistoryService.load`）。
- `ChatController` 新增 `GET /chat/history?sessionKey=...`。

### 6. 核心改造：ChatServiceImpl

```java
// 从持久化历史加载（Redis 优先，未命中回源 message 表），替代内存 Map
List<Message> history = toSpringMessages(chatHistoryService.load(sessionId));
history.add(new UserMessage(message));
chatHistoryService.append(sessionId, "user", message);

// 历史超过上限时，压缩旧消息为摘要
if (history.size() > MAX_HISTORY_SIZE) {
    history = compressHistory(history);
}

String response = chatClient.prompt().messages(history).call().content();
response = cleanResponse(response);

// 记录 AI 回复到历史（Redis + MQ 双写）
chatHistoryService.append(sessionId, "assistant", response);
history.add(new AssistantMessage(response));
```

`chatStreamSse()` 同理改造，唯一区别是 AI 回复在**流完成后**才写入历史。

---

## 四、执行过的命令

```bash
# 1. 启动 OpenSpec 规划
/opsx-propose

# 2. 开始实施
/opsx-apply

# 3. 编译验证（带 -am 连带编译依赖模块）
mvn -q -pl ai-cs-common,ai-cs-message,ai-cs-chat -am compile -DskipTests

# 4. 提交功能代码
git add <具体文件路径>   # 只暂存功能代码 + openspec 工件
git commit -m "feat(chat): 会话历史改为 Redis 热缓存 + message 表持久化"

# 5. 归档（含同步主线 spec）
/opsx-archive

# 6. 提交归档与主线 spec
git add openspec/specs/chat/history-management/spec.md openspec/changes/archive/2026-08-11-chat-history-persistence
git commit -m "docs(openspec): 归档 change chat-history-persistence 并同步主线 spec"

# 7. 查看 change 状态
openspec status --change "chat-history-persistence" --json
```

> 注意：Windows PowerShell 不支持 `&&` 和 heredoc，连接多条命令用 `;`，提交信息用单行 `-m`。

---

## 五、遇到的问题与解决

| 问题                                                         | 解决                                                |
| ---------------------------------------------------------- | ------------------------------------------------- |
| 误点拒绝导致 mapper/service 接口被回退，编译报 `找不到符号 selectBySessionKey` | `git status` 对照发现 mapper 和 service 接口被我误操作还原，重新补齐 |
| 编译报"方法不会覆盖或实现超类型的方法"                                       | 同样是接口被回退所致，补齐接口申明后解决                              |
| PowerShell 不支持 `&&` / heredoc                              | 改用 `;` 分隔、单行 `-m` 提交                              |
| Redis 不可用导致对话阻塞                                            | 通过 try-catch 降级为空历史，不阻塞主流程                        |

---

## 六、运行验证（留待本地环境）

以下为运行时验证项，需本地启动 Redis、Nacos、RocketMQ、MySQL 及服务实例后执行：

- [ ] 单实例：对话后重启服务，历史仍在（Redis 未清）。
- [ ] 双实例：实例 A 写入后实例 B 可读到同一会话历史。
- [ ] 历史回看接口返回完整、按时间排序的消息列表。
- [ ] Redis 不可用时，对话能降级继续（从空历史开始），不阻塞。
