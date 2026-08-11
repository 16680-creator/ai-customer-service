## 1. ai-cs-message 提供按 sessionKey 查询能力

- [x] 1.1 `ChatMessageMapper` 增加 `selectBySessionKey(String sessionKey, int limit)` 查询方法（注解或 XML）
- [x] 1.2 `MessageService` / `MessageServiceImpl` 增加 `getMessagesBySessionKey(String sessionKey)`，按 `createTime` 升序返回
- [x] 1.3 `MessageController` 增加 `GET /api/message/session-key/{sessionKey}/messages` 端点
- [ ] 1.4 编译并启动验证 ai-cs-message，确认新端点可用

## 2. ai-cs-chat 引入 Redis 依赖与配置

- [x] 2.1 `ai-cs-chat/pom.xml` 增加 `spring-boot-starter-data-redis` 依赖
- [x] 2.2 `ai-cs-chat` 的 `application.yml` 增加 Redis 连接配置（host/port/password）
- [x] 2.3 提供 `StringRedisTemplate` 注入可用（默认自动装配）

## 3. chat 侧新增会话历史 DTO 与历史存取组件

- [x] 3.1 新增消息 DTO（`role`、`content` 字段），用于 Redis 序列化与回源重建
- [x] 3.2 新增 `ChatHistoryService`：`load(sessionKey)`（Redis LRANGE → 未命中回源 message 表 → 重建写回 Redis）与 `append(sessionKey, role, content)`（RPUSH + 投递 MQ）
- [x] 3.3 新增 `MessageFeignClient`（服务名 `ai-cs-message`），调用按 sessionKey 查询端点
- [x] 3.4 Redis 未命中回源时限制条数（如最近 200 条），并处理 message 查询失败降级为空历史

## 4. 改造 ChatServiceImpl 使用持久化历史

- [x] 4.1 删除 `sessionHistory` 内存 Map 字段及 `computeIfAbsent`/`put` 引用
- [x] 4.2 `chat()` 改为：开始时 `ChatHistoryService.load(sessionId)` 取历史；用户消息 `append`；AI 回复 `append`
- [x] 4.3 `chatStreamSse()` 改为同样的持久化历史读写（保持 AI 回复在流完成后写入的时机）
- [x] 4.4 保留 `compressHistory` 逻辑，但基于持久化加载的历史对象执行

## 5. chat 侧新增历史回看接口

- [x] 5.1 `ChatService` 增加 `getHistory(sessionKey)` 接口
- [x] 5.2 `ChatServiceImpl` 实现 `getHistory`（复用 `ChatHistoryService.load`）
- [x] 5.3 `ChatController` 增加 `GET /chat/history?sessionKey=...` 端点

## 6. 验证与联调

- [x] 6.1 编译通过整个项目
- [ ] 6.2 单实例验证：对话后重启服务，历史仍在（Redis 未清）
- [ ] 6.3 双实例验证：实例 A 写入后实例 B 可读到同一会话历史
- [ ] 6.4 历史回看接口返回完整、按时间排序的消息列表
- [ ] 6.5 Redis 不可用时，对话能降级继续（从空历史开始），不阻塞主流程