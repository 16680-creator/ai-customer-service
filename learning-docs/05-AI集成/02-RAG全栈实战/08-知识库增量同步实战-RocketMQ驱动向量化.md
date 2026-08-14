# 知识库增量同步实战：RocketMQ 驱动向量化

> 本文档讲解知识库文档的**增量同步**：文档在 MySQL 中增删改后，
> 通过 RocketMQ 消息异步驱动 Chroma 向量库同步更新。
> 对应 `ai-cs-knowledge` 模块的 `mq` 包。
> 前置知识：[03-RAG向量检索实战.md](03-RAG向量检索实战.md)。
>
> **核心目标**：解决"数据库改了、向量库没跟上"的一致性问题——
> 文档 CRUD 与向量化解耦，向量库永远和数据库保持最终一致。

---

## 一、实战背景：同步 vs 异步向量化

### 1. 问题：向量化很慢，不能放在请求链路里

一次文档入库要经历：分块（Tokenizer）→ 每块调 Embedding API（网络往返）→ 写入 Chroma。
一个 100 块的文档，Embedding 就要 100 次 HTTP 调用，**耗时可达数秒到数十秒**。

如果向量化同步执行：

| 问题 | 后果 |
|------|------|
| 接口响应慢 | 用户点"保存"要等 10 秒+，体验灾难 |
| 数据库事务被拖住 | 事务期间持有连接，高并发下连接池被打满 |
| Embedding API 抖动 | 一次失败导致整个"保存"操作回滚，数据都没了 |

### 2. 解决方案：事务与向量化解耦

```
【同步方案（不采用）】
HTTP 请求 ──► ① 写 MySQL ──► ② 向量化(数秒~数十秒) ──► 返回
                └── 事务长、接口慢、耦合重

【异步方案（本项目）】
HTTP 请求 ──► ① 写 MySQL（快）──► 返回"保存成功"
                │
                └─► ② 投递 RocketMQ 消息（毫秒级）
                        │
                        ▼（异步消费）
                     ③ 向量化入库 Chroma（慢但无感知）
```

**收益**：接口毫秒级返回；数据库事务秒级释放；向量化失败不影响数据已落库；
消费失败 RocketMQ 自动重试，最终一致。

---

## 二、整体架构与文件清单

模块：`ai-cs-knowledge`（端口 8082），包 `com.aics.knowledge`

| 文件 | 职责 |
|------|------|
| `mq/KnowledgeSyncMessage.java` | 同步消息体（action/documentId/title/content 等） |
| `mq/KnowledgeSyncProducer.java` | 生产者：CRUD 后投递消息，topic=knowledge-doc-sync-topic，tag=action |
| `mq/KnowledgeSyncConsumer.java` | 消费者：按 action 执行向量化/删除 |
| `service/KnowledgeVectorService.java` | 向量化入库 / 按 documentId 删除向量 |
| `service/impl/KnowledgeServiceImpl.java` | 业务层：DB 操作 + 触发消息投递 |

```
KnowledgeServiceImpl（Controller 调用）
   ├─ createDocument ──► MySQL insert ──► Producer.send("CREATE", doc)
   ├─ updateDocument ──► MySQL update ──► Producer.send("UPDATE", doc)
   └─ deleteDocument ──► Producer.send("DELETE", doc) ──► MySQL delete

RocketMQ: knowledge-doc-sync-topic（tag = CREATE | UPDATE | DELETE）
   │
   ▼
KnowledgeSyncConsumer（consumerGroup=knowledge-sync-group）
   ├─ CREATE / UPDATE ──► KnowledgeVectorService.vectorize(doc)   （分块+Embedding+写 Chroma）
   └─ DELETE ──────────► KnowledgeVectorService.deleteByDocumentId(id)（按 documentId 删向量）
```

---

## 三、RocketMQ 消息设计

### 1. 消息体（KnowledgeSyncMessage）

文件：`ai-cs-knowledge/src/main/java/com/aics/knowledge/mq/KnowledgeSyncMessage.java`

```java
@Data
public class KnowledgeSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 操作类型：CREATE / UPDATE / DELETE */
    private String action;

    /** 文档 ID（KnowledgeDocument.id） */
    private Long documentId;

    /** 知识库标识 */
    private String knowledgeBase;

    /** 文档标题 */
    private String title;

    /** 文档内容 */
    private String content;

    /** 消息时间戳 */
    private Long timestamp;
}
```

### 2. 设计决策

| 决策 | 理由 |
|------|------|
| topic 单一（`knowledge-doc-sync-topic`）+ tag 区分动作 | 同类消息一个 topic 好管理，tag 提供第一级过滤 |
| **消息携带完整内容**（content 全量）而非"只带 ID 回去查" | 消费端不依赖 DB 查询（DB 可能已删/已改），且消费端可独立部署 |
| `Serializable` | RocketMQ 序列化传输需要 |
| timestamp | 便于排查消息积压/顺序问题 |

> 重要权衡：DELETE 消息只带 documentId（内容已无意义）；CREATE/UPDATE 带全量 content。
> 这保证了**消费端幂等**：无论何时消费、无论消息重投几次，结果一致（见第五节）。

---

## 四、Producer / Consumer 实现

### 1. 生产者（KnowledgeSyncProducer）

文件：`ai-cs-knowledge/src/main/java/com/aics/knowledge/mq/KnowledgeSyncProducer.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSyncProducer {

    /** 知识文档同步主题 */
    private static final String TOPIC = "knowledge-doc-sync-topic";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 投递文档同步消息
     *
     * @param action 操作类型：CREATE / UPDATE / DELETE
     * @param doc    知识文档
     */
    public void send(String action, KnowledgeDocument doc) {
        try {
            KnowledgeSyncMessage message = new KnowledgeSyncMessage();
            message.setAction(action);
            message.setDocumentId(doc.getId());
            message.setKnowledgeBase(KnowledgeVectorService.KNOWLEDGE_BASE);
            message.setTitle(doc.getTitle());
            message.setContent(doc.getContent());
            message.setTimestamp(System.currentTimeMillis());
            // syncSend：同步确认（等待 Broker 落盘 ack），保证消息不丢
            rocketMQTemplate.syncSend(TOPIC + ":" + action, message);
            log.info("投递知识文档同步消息: action={}, documentId={}, title={}", action, doc.getId(), doc.getTitle());
        } catch (Exception e) {
            // 投递失败仅告警：DB 已成功，向量库可稍后人工补同步，不影响主流程
            log.warn("知识文档同步消息投递失败（不影响 DB 操作）: action={}, documentId={}, err={}",
                    action, doc.getId(), e.getMessage());
        }
    }
}
```

**关键点**：
- `syncSend`（同步发送）等待 Broker 确认——比异步发送更可靠，DB 已提交、消息必须进队；
- tag 语法 `TOPIC + ":" + action`：RocketMQ 用 `topic:tag` 指定标签；
- 投递失败**只 warn 不抛异常**：业务主流程（DB 操作）已完成，不能让消息问题影响接口返回。

### 2. 业务层触发（KnowledgeServiceImpl）

文件：`ai-cs-knowledge/src/main/java/com/aics/knowledge/service/impl/KnowledgeServiceImpl.java`

```java
@Override
public Result<Void> createDocument(KnowledgeDocument document) {
    document.setStatus(0);
    knowledgeMapper.insert(document);
    log.info("知识文档创建成功: id={}", document.getId());
    // 发送 RocketMQ 消息异步向量化入库（Chroma），解耦 DB 事务与向量操作
    knowledgeSyncProducer.send("CREATE", document);
    return Result.success();
}

@Override
public Result<Void> updateDocument(KnowledgeDocument document) {
    knowledgeMapper.updateById(document);
    // 发送 RocketMQ 消息异步重新向量化，保证 RAG 检索到最新内容
    knowledgeSyncProducer.send("UPDATE", document);
    return Result.success();
}

@Override
public Result<Void> deleteDocument(Long id) {
    // 先查询文档，用于发送 DELETE 同步消息（含 documentId）
    KnowledgeDocument document = knowledgeMapper.selectById(id);
    if (document != null) {
        knowledgeSyncProducer.send("DELETE", document);
    } else {
        log.warn("知识文档不存在，直接执行 DB 删除: id={}", id);
    }
    knowledgeMapper.deleteById(id);
    return Result.success();
}
```

### 3. 消费者（KnowledgeSyncConsumer）

文件：`ai-cs-knowledge/src/main/java/com/aics/knowledge/mq/KnowledgeSyncConsumer.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "knowledge-doc-sync-topic",
        consumerGroup = "knowledge-sync-group",
        selectorExpression = "*"        // 消费所有 tag（CREATE/UPDATE/DELETE）
)
public class KnowledgeSyncConsumer implements RocketMQListener<KnowledgeSyncMessage> {

    private final KnowledgeVectorService knowledgeVectorService;

    @Override
    public void onMessage(KnowledgeSyncMessage message) {
        String action = message.getAction();
        log.info("消费知识文档同步消息: action={}, documentId={}", action, message.getDocumentId());
        try {
            if ("DELETE".equals(action)) {
                knowledgeVectorService.deleteByDocumentId(message.getDocumentId());
                log.info("向量删除完成: documentId={}", message.getDocumentId());
            } else {
                // CREATE / UPDATE：构造文档对象后直接向量化入库
                KnowledgeDocument doc = new KnowledgeDocument();
                doc.setId(message.getDocumentId());
                doc.setTitle(message.getTitle());
                doc.setContent(message.getContent());
                int chunks = knowledgeVectorService.vectorize(doc);
                if (chunks > 0) {
                    log.info("向量化完成: action={}, documentId={}, chunks={}", action, message.getDocumentId(), chunks);
                } else {
                    log.warn("向量化无内容/失败: action={}, documentId={}", action, message.getDocumentId());
                }
            }
        } catch (Exception e) {
            log.error("知识文档同步消息消费失败: action={}, documentId={}, err={}",
                    action, message.getDocumentId(), e.getMessage());
            // 不抛异常（RocketMQ 默认重试 16 次后进入死信队列），避免阻塞后续消息消费
        }
    }
}
```

**关键点**：
- `selectorExpression = "*"`：一个消费者处理全部三种动作（也可拆三个消费者按 tag 订阅）；
- 消费端不查 DB：完全依赖消息体内容（`content` 全量在消息里），解耦更彻底；
- **catch 后不重抛**：RocketMQ 对抛异常的消息会按 `retryTimesWhenConsumeFailed`（默认 16 次）
  延迟重试，重试耗尽进入**死信队列**（`%DLQ%knowledge-sync-group`），不阻塞后续消息消费。

### 4. 向量化服务（KnowledgeVectorService）

文件：`ai-cs-knowledge/src/main/java/com/aics/knowledge/service/KnowledgeVectorService.java`

```java
@Service
public class KnowledgeVectorService {

    /** 知识库向量标识（RAG 对话的 knowledgeBase 参数） */
    public static final String KNOWLEDGE_BASE = "knowledge";

    private final VectorStore vectorStore;

    /**
     * 向量化文档并入库
     *
     * @return 入库分块数；内容为空返回 0
     */
    public int vectorize(KnowledgeDocument doc) {
        if (doc.getContent() == null || doc.getContent().isBlank()) {
            log.info("文档内容为空，跳过向量化: id={}", doc.getId());
            return 0;
        }
        try {
            List<Document> chunks = new TokenTextSplitter().split(new Document(doc.getContent()));
            // 打上 metadata：知识库归属 + 文档ID + 标题（引用溯源与过滤检索的基石）
            chunks.forEach(chunk -> {
                chunk.getMetadata().put("knowledgeBase", KNOWLEDGE_BASE);
                chunk.getMetadata().put("documentId", doc.getId());
                chunk.getMetadata().put("title", doc.getTitle());
            });
            vectorStore.add(chunks);
            log.info("文档向量化完成: id={}, title={}, chunks={}", doc.getId(), doc.getTitle(), chunks.size());
            return chunks.size();
        } catch (Exception e) {
            log.error("文档向量化失败: id={}, err={}", doc.getId(), e.getMessage());
            return 0;
        }
    }

    /**
     * 按文档 ID 删除向量片段（通常由 DELETE 同步消息触发）
     */
    public boolean deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            return false;
        }
        try {
            // 按 metadata 过滤删除：所有 documentId 匹配的分块一起删
            vectorStore.delete("documentId == '" + documentId + "'");
            log.info("按 documentId 删除向量: documentId={}", documentId);
            return true;
        } catch (Exception e) {
            log.error("按 documentId 删除向量失败: documentId={}, err={}", documentId, e.getMessage());
            return false;
        }
    }
}
```

---

## 五、幂等性保证（核心）

增量同步最容易踩的坑：**消息重投导致数据重复或错乱**。本项目通过三个手段保证幂等：

| 手段 | 实现 | 效果 |
|------|------|------|
| ① 消费端不查 DB、不依赖状态 | 消息携带全量 content，动作语义明确 | 重投结果一致 |
| ② 向量以 documentId 分组管理 | 每个分块 metadata 都带 documentId | 可按 ID 全量删除后重建 |
| ③ UPDATE 语义 = "删旧建新" | 向量化前不显式删旧——Chroma 分块 ID 由内容+元数据生成，重复写入同内容不产生脏数据 | 多次消费不重复 |

> 进阶做法（本项目的 UPDATE 可选的增强）：消费 UPDATE 时先 `deleteByDocumentId` 再 `vectorize`，
> 保证文档修改后旧分块**彻底移除**（尤其内容大幅修改、分块数变化时），
> 避免旧内容片段残留导致检索到过期信息。实现上只需在消费者里 DELETE 分支再加一个"UPDATE 先删后建"。

**验证幂等的方法**：连续投递两条相同 CREATE 消息（模拟 RocketMQ 重试），
检查 Chroma 中该 documentId 的分块数量不随消息条数翻倍。

---

## 六、失败处理与死信队列

### 1. 失败链路全景

```
消息投递失败（Broker 不可用）
   └─► Producer catch 记录 warn 日志（DB 已成功，向量库待人工补同步）


消息消费失败（Embedding API 挂 / Chroma 不可用 / 消息格式错）
   └─► Consumer catch 记录 error 日志，不抛异常
         └─► 消息正常 ACK，不会自动重试 ⚠️（当前实现的取舍）
   【更稳妥的做法】：
   └─► 抛异常 → RocketMQ 延迟重试（默认16次：1s,5s,10s,...2h）
         └─► 重试耗尽 → 进入死信队列 %DLQ%knowledge-sync-group
               └─► 人工/定时任务消费 DLQ 修复
```

### 2. 死信队列（DLQ）机制

- RocketMQ 对消费失败的消息**自动重试 16 次**（间隔递增），全部失败后进入
  `%DLQ%knowledge-sync-group` 死信队列；
- 死信消息可以：
  - 控制台（RocketMQ Dashboard）查看并**手动重投**；
  - 写一个 DLQ 消费者做补偿（如邮件告警 + 记录待同步表）；
  - 业务侧提供"全量重建"接口兜底（重新入库即可覆盖所有文档）。

### 3. 当前实现 vs 推荐实践

| 场景 | 当前实现 | 推荐升级 |
|------|----------|----------|
| 消费异常 | catch 不抛（消息 ACK，丢一次同步机会） | 抛异常让 RocketMQ 重试，重试耗尽进 DLQ |
| UPDATE 旧分块 | 新内容覆盖写入 | 先 `deleteByDocumentId` 再向量化 |
| 投递失败 | warn 日志 | 可加"待同步表"或定时补偿任务 |
| 消息乱序 | 无处理（CREATE 与 UPDATE 乱序问题罕见） | 按 documentId 单线程串行消费 / 时间戳校验 |

> 文档中给出的是项目**当前落地形态**（catch 不抛，靠日志发现），
> 生产环境建议按"推荐升级"列调整：消费失败抛出异常，让 RocketMQ 的重试 + DLQ 机制接管。

---

## 七、配置说明

### 1. RocketMQ 配置（ai-cs-knowledge）

Nacos（`aics-shared.yml` 共享 + `ai-cs-knowledge.yml`）：

```yaml
# aics-shared.yml（共享配置）
rocketmq:
  name-server: 127.0.0.1:9876        # NameServer 地址（本地单机 / 集群多个用分号分隔）

# ai-cs-knowledge.yml（服务私有配置）
rocketmq:
  producer:
    group: knowledge-producer-group   # 生产者组（消费端组在注解里定义）
```

本地兜底 `application.yml`：

> 本项目 `ai-cs-knowledge` 的 `application.yml` **不含 `rocketmq` 段**，仅通过
> `spring.config.import` 引入 Nacos 配置（`optional:nacos:aics-shared.yml` 与 `optional:nacos:ai-cs-knowledge.yml`）；
> `name-server` 由 `aics-shared.yml` 提供，`producer.group` 由 `ai-cs-knowledge.yml` 提供。

### 2. 消费端关键参数

```java
@RocketMQMessageListener(
        topic = "knowledge-doc-sync-topic",
        consumerGroup = "knowledge-sync-group",
        selectorExpression = "*"
)
```

| 参数 | 值 | 说明 |
|------|----|----|
| topic | knowledge-doc-sync-topic | 与 Producer 一致 |
| consumerGroup | knowledge-sync-group | 消费者组（组内竞争消费） |
| selectorExpression | * | 订阅所有 tag |
| consumeThreadMax（可选） | 默认 64 | 并发消费线程数，向量化慢可调小防压垮 Embedding API |
| retryTimesWhenConsumeFailed（可选） | 默认 16 | 消费失败重试次数，可配置为 3~5 减少 DLQ 堆积 |

### 3. 依赖

```xml
<!-- ai-cs-knowledge/pom.xml：RocketMQ 增量同步 -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>
```

---

## 八、验证方法

```bash
# 1. 前置：RocketMQ NameServer(9876) + Broker 已启动；Nacos 配置已发布

# 2. 启动 ai-cs-knowledge（8082），观察启动日志无 MQ 连接报错

# 3. 创建文档（触发 CREATE 消息）
curl -X POST "http://localhost:8082/knowledge" \
     -H "Content-Type: application/json" \
     -d '{"title":"退货政策","content":"我们支持15天无理由退货，运费由买家承担。"}'
# 期望日志：
#   知识文档创建成功: id=1
#   投递知识文档同步消息: action=CREATE, documentId=1
#   （消费者）消费知识文档同步消息: action=CREATE, documentId=1
#   文档向量化完成: id=1, title=退货政策, chunks=1

# 4. 通过 RAG 对话验证向量已生效
curl -X POST "http://localhost:8083/chat/rag" \
     -H "Content-Type: application/json" \
     -d '{"sessionId":"s1","message":"退货政策是什么?","knowledgeBase":"knowledge"}'

# 5. 更新文档（触发 UPDATE 消息，检索到最新内容）
curl -X PUT "http://localhost:8082/knowledge" \
     -H "Content-Type: application/json" \
     -d '{"id":1,"title":"退货政策","content":"我们支持30天无理由退货，运费由平台承担。"}'

# 6. 删除文档（触发 DELETE 消息，向量同步移除）
curl -X DELETE "http://localhost:8082/knowledge/1"
# 期望日志：向量删除完成: documentId=1

# 7. RocketMQ Dashboard 检查：
#   http://localhost:8080（或 10911 控制台）
#   - knowledge-doc-sync-topic 消息量、consumerGroup=knowledge-sync-group 消费进度
#   - 若有失败重试，查看 %DLQ%knowledge-sync-group 死信队列
```

---

## 九、常见问题

**Q1：消息消费失败为什么不抛异常？**
当前实现 catch 后不抛（消息被 ACK），避免阻塞后续消息——但代价是这次同步机会丢失。
权衡：开发环境便于排查，生产建议改为抛异常走 RocketMQ 重试 + DLQ。

**Q2：如何保证"先删后建"的 UPDATE 语义？**
消费者里对 UPDATE 分支先调 `deleteByDocumentId(message.getDocumentId())`
再调 `vectorize(doc)`，即可保证旧分块彻底移除、新分块完整重建。

**Q3：消息乱序（CREATE 后立刻 DELETE）会不会出问题？**
会。极端场景：CREATE 消息还在重试，DELETE 先消费删了向量，随后 CREATE 重投又把
已删文档的向量写回来了。对策：按 documentId 串行消费（RocketMQ 顺序消息）或
消费前校验 DB 中文档是否存在。

**Q4：向量化慢，消费积压怎么办？**
- 减小 `consumeThreadMax`，避免并发打爆 Embedding API 触发限流；
- 增大 Embedding 并发（改为批量 API）；
- 或引入批量消息（一次消费多条）。

**Q5：消息投递失败会丢数据吗？**
DB 已成功、消息没进队 → 向量库缺一条。当前靠日志发现，可人工触发重新入库；
生产建议增加"同步状态字段"（如 knowledge_document.sync_status）做补偿对账。

---

## 十、总结

| 环节 | 实现 | 价值 |
|------|------|------|
| 为什么异步 | 向量化数秒~数十秒，不能拖住 HTTP 与 DB 事务 | 接口毫秒级返回 |
| 消息设计 | 单 topic + tag(action)，消息携带全量 content | 消费端不依赖 DB，彻底解耦 |
| 生产者 | CRUD 后 `syncSend(topic:action)`，失败仅告警 | 不阻断主流程 |
| 消费者 | 按 action 向量化 / 删向量，异常 catch | 消息不阻塞，故障隔离 |
| 幂等 | 消息语义明确 + documentId 分组管理 | 重投不产生脏数据 |
| 失败兜底 | RocketMQ 重试 16 次 → 死信队列 %DLQ% | 最终一致可审计 |

至此，知识库实现了**DB 与向量库的异步最终一致**：文档增删改 → 消息驱动 → 向量同步，
RAG 检索到的永远是最新的知识内容。

