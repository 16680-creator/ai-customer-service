# 07-MongoDB 对话审计归档（02-P2 落地记录）

> 2026-08 落地。实施前重新盘点发现：项目并非“聊天记录只存在 Redis”，实际已有
> Redis 热窗口 + RocketMQ → ai-cs-message `chat_message` 表最终持久化链路。
> 因此 MongoDB 采用**可选第三路审计归档**，不取代已有事实源，避免制造双事实源。

## 一、真实存储现状

```text
ChatHistoryService.append(sessionKey, role, content)
  ├─ Redis List: chat:history:{sessionKey}       热上下文，LLM 多轮对话优先读
  └─ RocketMQ ChatMessageProducer → message 服务 chat_message 表  最终持久化/回源
```

原始计划中“Redis 重启即丢、Mongo 作为唯一持久化”的前提已过期；直接切换会破坏现有
message 服务的查询、MQ 幂等与回源机制。

## 二、Mongo 的正确定位

MongoDB 适合补充以下能力：

- 对话审计原文（文档模式，后续新增模型/token/feedback/metadata 不用反复改表）
- 管理端按 session/user/time 的分页审计
- 长期保留/TTL 索引（后续配置 90 天），减轻 MySQL 运营查询压力

它不是 LLM 热上下文源（Redis 更快），也不是当前业务最终事实源（message 表仍然是）。

## 三、低风险双写模式（默认关闭）

```text
append()
  ├─ Redis RPUSH                 必走，热上下文
  ├─ RocketMQ → message 表        必走，现有最终持久化
  └─ Mongo archive（可选）        最佳努力，失败仅 warn，不阻断 AI 回复
```

配置：

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://127.0.0.1:27017/aics_chat}
aics:
  chat:
    mongo-archive:
      enabled: ${MONGO_ARCHIVE_ENABLED:false}
```

默认 `false`：无 Mongo 环境不影响任何现有对话流程。开启后
`MongoChatHistoryArchiveService` 才注册，`ObjectProvider` 在 ChatHistoryService 中按需取得。

## 四、文档模型与索引

```java
@Document("chat_message_archive")
@CompoundIndex(name = "session_created_idx", def = "{'sessionKey': 1, 'createdAt': -1}")
class ChatMessageArchiveDocument {
  String id; String sessionKey; String userId;
  String role; String content; Instant createdAt;
}
```

复合索引满足会话倒序分页。当前 sessionKey 无稳定 userId 格式，`userId` 先允许为空；
后续认证上下文贯通后在 append 参数或事件中显式携带，不能靠字符串猜解析。

## 五、部署

根 `docker-compose.yml` 新增：

```text
mongo:7.0 → 27017
volume: mongodb-data:/data/db
DATABASE: aics_chat
```

生产 Mongo 至少需要副本集与认证；单容器只适合本地学习。

## 六、验证

`MongoChatHistoryArchiveServiceTest`：

- 正常写入验证 session/role/content/createdAt
- repository 异常验证不外抛（AI 主链路不被审计故障拖垮）

`mvn -pl ai-cs-chat test -Dtest=MongoChatHistoryArchiveServiceTest`：2 tests 全绿。

## 七、面试要点

- 文档库 vs 关系库不是二选一：热缓存/OLTP/审计文档各自负责不同查询模式
- 双写的事实源必须明确；本项目 MySQL message 表是事实源，Mongo 是可重建审计副本
- 为什么归档失败不抛：AI 主回复可用性高于审计完整性；失败需要 metrics/告警/补偿任务
- TTL 索引适合合规保留期，但会异步清理，不应拿来做精确到秒的删除 SLA
