## Why

当前 ai-cs-chat 使用内存 `ConcurrentHashMap<String,List<Message>>` 缓存会话历史，存在三个严重问题：进程重启即丢失、多实例部署时实例间不共享、无法对外提供历史回看。会话数据已通过 MQ 异步投递到 ai-cs-message 落库到 `chat_session`/`chat_message` 表，但历史读取仍完全依赖内存，未利用这份持久化数据。需要把会话历史的存储与读取改造为「Redis 热缓存 + message 表最终持久化」的稳定方案。

## What Changes

- 在 ai-cs-chat 引入 Redis 作为会话历史热缓存，替代内存 `sessionHistory` Map，实现重启不丢、多实例共享。
  - 新增 Redis 依赖与序列化配置（存储 Spring AI `Message` 列表）。
- 会话历史的数据源改为「Redis → 兜底 message 表」：Redis 未命中时，从 ai-cs-message 按 `sessionKey` 拉取历史重建缓存。
- ai-cs-message 新增「按 `sessionKey` 查询聊天消息」的能力（当前仅支持按数据库主键 `sessionId` 查询），供 chat 侧回看历史。
- 保留并接通现有 MQ 投递链路，使每条消息最终持久化到 `chat_message` 表。
- 在 `ChatService` 新增「查询会话历史」接口，支持历史回看；内部 `chat`/`chatStreamSse` 改为读取持久化历史。
- 移除 `sessionHistory` 内存 Map 及相关直接引用。

## Capabilities

### New Capabilities
- `chat/history-management`: 会话历史的多级存储与读取能力——Redis 热缓存、message 表兜底、按 sessionKey 拉取历史、对外历史回看接口。

### Modified Capabilities
<!-- 本项目尚无 openspec/specs/ 下既有 capability，本次为首次引入。 -->

## Impact

- **ai-cs-chat**：新增 Redis 依赖与配置；修改 `ChatServiceImpl`（历史读写逻辑）、`ChatService`（新增历史接口）、`ChatController`（新增历史端点）；新增 Redis 存取组件。
- **ai-cs-message**：`MessageService`/`MessageServiceImpl` 增加按 `sessionKey` 查询；`MessageController` 增加历史查询端点；`ChatMessageMapper` 增加按 sessionKey 查询方法。
- **API**：chat 侧新增历史查询接口；message 侧新增按 sessionKey 查询接口。
- **依赖**：ai-cs-chat 新增 `spring-boot-starter-data-redis`（或 org.redisson）。
- **系统**：需 Redis 实例可用；依赖 ai-cs-message 服务可访问。