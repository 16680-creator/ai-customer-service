## Context

动机见 proposal.md - Why。当前 ChatServiceImpl 用内存 `sessionHistory`（ConcurrentHashMap）缓存会话历史，重启即丢、多实例不共享。会话消息已通过 ChatMessageProducer 投递到 MQ，由 ai-cs-message 落库到 `chat_message` 表（`sessionKey` 非空、`sessionId` 为空）。ai-cs-message 目前仅支持按数据库主键 `sessionId` 查询历史，不支持按 `sessionKey` 查询。ai-cs-chat 已具备 OpenFeign + Nacos 服务发现能力，可声明式调用 ai-cs-message。

## Goals / Non-Goals

**Goals:**
- 会话历史改为「Redis 热缓存 + message 表兜底持久化」，实现重启不丢、多实例共享。
- chat 侧对话/流式对话从持久化历史读取上下文，而非内存 Map。
- 提供历史回看接口。
- 保留现有 MQ 落库链路，作为最终持久化事实源。

**Non-Goals:**
- 不改变 Spring AI 对话生成的业务逻辑（RAG、压缩摘要算法不动）。
- 不做 `chat_session` 表的会话创建/关联（本期仅按 `sessionKey` 兜底查询 `chat_message`）。
- 不引入消息时序一致性事务（依赖 MQ 最终一致）。

## Decisions

### D1: Redis 存储结构 —— 每会话一条 List，元素为 JSON 序列化的消息
- 用 Redis `List` 类型，key 形如 `chat:history:{sessionKey}`，按顺序追加每条消息（`RPUSH`），读取用 `LRANGE 0 -1`。
- 元素为「角色 + 内容」的紧凑 DTO（`{role, content}`），避免类型耦合与序列化脆弱性，回源到 message 表时也统一用该 DTO 重建。
- 追加、读取并发安全由 Redis 单命令保证。
- 备选：`Hash`（field=index）不便于顺序读取；序列化整个 `List<Message>` 为单 key 字符串在并发追加时有覆盖风险 → 弃用。

### D2: 依赖选型 —— chat 侧引入 `spring-boot-starter-data-redis`（Lettuce）
- 与 Spring Boot 生态原生集成，`StringRedisTemplate` 即可满足 JSON 存取，无需额外序列化器。
- 备选：Redisson（分布式锁/对象化）→ 本期无锁需求，国营轻量，弃用。

### D3: 历史读取策略 —— 「Redis 优先，未命中回源 message 表」
- 新增 `ChatHistoryService`（chat 侧）封装读逻辑：
  1. `LRANGE` 读 Redis；命中则返回。
  2. 未命中 → 经 Feign 调 ai-cs-message 按 `sessionKey` 拉历史 → 重建写回 Redis → 返回。
- 每次对话请求开始时调用一次，替代 `sessionHistory.computeIfAbsent(...)`。
- 写侧双写：每条消息 `RPUSH` 到 Redis + 同时 `ChatMessageProducer.send(...)` 投递 MQ（现有逻辑，保留）。

### D4: message 侧新增按 sessionKey 查询能力
- `ChatMessageMapper` 增加 `selectBySessionKey(String sessionKey, int limit)` 方法（XML 或注解）。
- `MessageService` 增加 `List<ChatMessage> getMessagesBySessionKey(String sessionKey)`。
- `MessageController` 增加 `GET /api/message/session-key/{sessionKey}/messages` 端点。
- chat 侧新增 Feign 客户端 `MessageFeignClient` 调用该端点（服务名 `ai-cs-message`）。

### D5: chat 侧历史回看接口
- `ChatService` 增加 `Result<List<...>> getHistory(String sessionKey)`。
- `ChatController` 增加 `GET /chat/history?sessionKey=...`。
- 实现复用 `ChatHistoryService` 的读取逻辑（Redis → 回源）。

### D6: 移除内存 Map，改造调用点
- 删除 `sessionHistory` 字段及其 `computeIfAbsent`/`put` 引用。
- `chat()` 与 `chatStreamSse()` 改为：开始处 `history = chatHistoryService.load(sessionId)`；每条消息写入后 `chatHistoryService.append(sessionId, role, content)`（内部 RPUSH + MQ）。
- 注意：AI 回复在流式完成后才写入，保持与现状一致的时机。

## Risks / Trade-offs

- [Redis 与 message 表存在短暂不一致（MQ 异步）] → 以 message 表为最终事实源，Redis 仅作热缓存；一致性由 MQ 投递保障，可容忍秒级延迟。
- [Redis 未配置/不可用时对话受影响] → 加载失败时降级为「本次对话从空历史开始」，并记录告警，不阻断对话主流程。
- [序列化 DTO 与 message 表 schema 演进不同步] → 统一使用紧凑 `{role, content}` DTO，字段稳定，降低耦合。
- [历史无限增长导致 Redis List 过大] → 回源建缓存时限制条数（如最近 200 条），必要时对旧记录设置 TTL 兜底。
- [多实例并发写 Redis List] → Redis `RPUSH` 原子，天然安全；无需额外锁。

## Migration Plan

1. 先在 ai-cs-message 侧新增按 sessionKey 查询能力（独立发布，向后兼容）。
2. 在 ai-cs-chat 引入 Redis 依赖与配置，新增 `ChatHistoryService`、`MessageFeignClient`、历史接口。
3. 改造 `chat()`/`chatStreamSse()` 为走 `ChatHistoryService`，删除内存 Map。
4. 联调验证：单实例重启不丢、双实例共享、历史回看接口。
5. 回滚：若 Redis 异常，降级逻辑保证聊楼主流程可用；改造点集中在 `ChatServiceImpl`，可回退 Git 提交。

## Open Questions

无。