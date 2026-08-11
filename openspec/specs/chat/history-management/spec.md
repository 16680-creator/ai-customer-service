## Purpose

将会话历史从进程内存迁移到「Redis 热缓存 + message 表最终持久化」的多级存储，使对话历史在服务重启后不丢失、多实例间共享，并支持历史回看。

## Requirements

### Requirement: 会话历史持久化存储
系统在接收对话消息时，SHALL 将会话历史写入 Redis 热缓存，并 MUST 通过现有 RocketMQ 链路将每条消息最终持久化到 ai-cs-message 的 `chat_message` 表。

#### Scenario: 消息写入 Redis 缓存
- **WHEN** 用户发送一条消息或 AI 返回一条回复
- **THEN** 该消息被追加到对应 `sessionKey` 的 Redis 历史缓存

#### Scenario: 消息最终落库
- **WHEN** 一条会话消息产生
- **THEN** 该消息通过 MQ 投递到 `chat-message-topic`，最终写入 `chat_message` 表

### Requirement: 会话历史多实例共享
系统 SHALL 使用 Redis 作为会话历史的共享存储，使同一会话在不同服务实例上均可读到一致的历史。

#### Scenario: 跨实例读取历史
- **WHEN** 服务实例 A 写入会话历史后，服务实例 B 处理同一 `sessionKey` 的请求
- **THEN** 实例 B 能读到实例 A 写入的历史消息

#### Scenario: 服务重启后历史不丢
- **WHEN** 服务进程重启，且 `sessionKey` 在 Redis 中仍有缓存
- **THEN** 重启后的服务能恢复该会话的历史

### Requirement: 历史缓存兜底回源
系统 SHALL 在 Redis 未命中某会话历史时，从 ai-cs-message 按 `sessionKey` 拉取已持久化的历史消息，并重建 Redis 缓存。

#### Scenario: Redis 缓存未命中时回源
- **WHEN** 某 `sessionKey` 在 Redis 中无缓存，但 `chat_message` 表中存在该会话的历史
- **THEN** 系统从 message 表拉取历史并重建 Redis 缓存，返回完整历史

### Requirement: 历史回看接口
系统 SHALL 提供对外接口，按 `sessionKey` 返回某会话的完整历史消息列表，供历史回看。

#### Scenario: 查询会话历史
- **WHEN** 调用方按 `sessionKey` 请求历史消息
- **THEN** 系统返回该会话按时间排序的完整消息列表（角色 + 内容）

#### Scenario: 查询不存在的会话
- **WHEN** 调用方请求一个没有历史记录的 `sessionKey`
- **THEN** 系统返回空列表，不报错