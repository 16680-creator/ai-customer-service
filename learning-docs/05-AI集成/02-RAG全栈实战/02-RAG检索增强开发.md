# RAG 检索增强开发实战

> 本文档是 `02-RAG全栈实战` 系列的开发总纲，衔接理论篇 [01-RAG检索增强生成.md](01-RAG检索增强生成.md)
> 与代码篇 [03-RAG向量检索实战.md](03-RAG向量检索实战.md)、[04-RAG进阶实战-Rerank重排序.md](04-RAG进阶实战-Rerank重排序.md)。
> 目标：从「整体架构 → 关键类清单 → 对外接口 → 配置项 → 开发实践」五个维度，讲清楚本项目的 RAG 是怎么搭起来的、怎么改、怎么扩展。

---

## 一、整体架构

本项目 RAG 横跨三个微服务，采用**读写分离 + 异步解耦**的架构：

```
【离线：构建知识库（写路径）】
  知识后台(ai-cs-knowledge)
    ├─ DB 落库 kb_document 表
    └─ 投递 RocketMQ 同步消息(CREATE/UPDATE/DELETE)
            │ 异步
            ▼
        消费者 KnowledgeSyncConsumer
            └─ KnowledgeVectorService.vectorize()
                   ├─ TokenTextSplitter 切块（默认 800 token/块）
                   ├─ 附元数据(knowledgeBase/documentId/title)
                   └─ VectorStore.add() → EmbeddingModel(bge-m3) 向量化 → 写入 Chroma

【在线：RAG 问答（读路径）】
  对话服务(ai-cs-chat)
    用户提问 ─┬─(纯向量)  KnowledgeBaseService.search()：宽召回 Top-20 → Rerank 精排 Top-N
              ├─(混合)    HybridRetriever：ES(BM25) + 向量 + RRF 融合
              ├─(改写)    QueryRewriteService：LLM 改写 / HyDE 提升召回
              └─(图谱)    GraphRagService：知识图谱优先，未命中降级
                   │
                   ▼
            检索编排 retrieveRagDocs(...)
                   │
                   ▼
            拼装上下文 → 注入 Prompt → ResilientAiService 弹性调用 LLM 生成
                   │
                   ▼
            返回 ChatRagResponseDTO(回答 + 引用溯源 citations)

【独立检索服务（ai-cs-search）】
  对外提供：全文检索 / 混合检索(ES+向量+RRF) 能力，供对话侧 HybridRetriever 通过 Feign 调用
```

三个核心角色与接口约定：

| 服务 | 职责 | 关键类 |
|------|------|--------|
| ai-cs-knowledge | 知识库后台：文档 CRUD、向量化入库、增量同步 | `KnowledgeServiceImpl`、`KnowledgeVectorService`、`KnowledgeSyncConsumer` |
| ai-cs-chat | RAG 问答：检索编排、上下文拼装、LLM 生成 | `ChatServiceImpl`、`KnowledgeBaseService`、`HybridRetriever`、`QueryRewriteService`、`GraphRagService` |
| ai-cs-search | 检索底座：向量/ES/混合检索 | `SearchFeignClient`（被 chat 调用） |

---

## 二、关键类清单（按职责）

### 2.1 离线构建（ai-cs-knowledge）
| 类 | 职责 |
|----|------|
| `KnowledgeService` / `KnowledgeServiceImpl` | 文档 CRUD，触发同步消息 |
| `KnowledgeVectorService` | 切块 + Embedding + 写入 Chroma |
| `KnowledgeSyncConsumer` | 消费 RocketMQ 消息，增量同步向量 |
| `DocumentLoader` | 多格式加载：`loadPdf` / `loadText` / `loadTika`（按后缀路由：pdf→loadPdf，docx/xlsx/html/htm→loadTika，其余→loadText） |

### 2.2 在线检索（ai-cs-chat）
| 类 | 职责 |
|----|------|
| `KnowledgeBaseService` | `search(kb, query, topN, recallThreshold)` 宽召回→Rerank；`recall-threshold` 默认 `0.3` |
| `HybridRetriever` | ES(BM25) + 向量 + RRF 融合的混合检索 |
| `QueryRewriteService` | LLM 改写 / HyDE，提升召回 |
| `GraphRagService` | 知识图谱优先，未命中降级纯向量 |
| `RerankService` / `SiliconFlowRerankService` | 硅基流动 Rerank 精排（`RerankProperties`：baseUrl/apiKey/model/topN/minScore/timeoutMs） |
| `retrieveRagDocs(...)` | 统一检索编排：纯向量 / HYBRID / HYBRID_QUERY_REWRITE / GRAPH_RAG 四种模式，增强失败自动降级纯向量 |
| `ChatServiceImpl.chatWithRag(...)` | 签名 `Result<ChatRagResponseDTO> chatWithRag(sessionId, message, knowledgeBase, hybrid, rewrite)`；含输入/输出 Guardrail 审核、RAG ACL 过滤、`ResilientAiService` 弹性调用、在线评估埋点，返回带 **引用溯源** 的 DTO |

### 2.3 检索底座（ai-cs-search）
| 类 | 职责 |
|----|------|
| 向量检索 / ES 检索 / 混合检索 | 提供底层召回能力，经 Feign 供 chat 侧调用 |

---

## 三、对外接口（ai-cs-chat）

| 接口 | 说明 |
|------|------|
| `POST /knowledge/upload` | 上传文档（支持 PDF/TXT/Word/Excel/HTML，经 `DocumentLoader.loadTika` 多格式） |
| `POST /knowledge/search` | 知识库检索，`search(knowledgeBase, query, 5, 0.3)` |
| `POST /chat/rag` | RAG 问答，返回 `ChatRagResponseDTO(content, citations)` |

---

## 四、配置项

```yaml
# ai-cs-chat（Nacos: ai-cs-chat.yml）
aics:
  rag:
    recall-threshold: 0.3        # 召回相似度阈值（宽召回后用，非 0.5）
    hybrid: true                 # 是否启用混合检索
    rewrite: false               # 是否启用查询改写
  rerank:
    base-url: https://api.siliconflow.cn
    api-key: ${SILICONFLOW_API_KEY}
    model: BAAI/bge-reranker-v2-m3
    top-n: 5
    min-score: 0.0
    timeout-ms: 5000
```

---

## 五、开发实践

1. **改切块策略**：调整 `TokenTextSplitter` 块大小/重叠，影响召回粒度，需重新向量化入库。
2. **调召回阈值**：`recall-threshold` 默认 `0.3`（注意不是 0.5），过低会引入噪声，过高会漏召回。
3. **加检索模式**：`retrieveRagDocs` 已支持四种模式，新增模式只需在枚举里扩展并在编排里接入，增强失败需保证降级到纯向量。
4. **接新向量库**：实现 `VectorStore` / `EmbeddingModel` 接口，或在 ai-cs-search 扩展检索底座，chat 侧经 Feign 调用。
5. **引用溯源**：`ChatRagResponseDTO.citations` 来自检索命中的文档元数据（documentId/title），前端可直接展示来源，无需改动检索链路。

---

## 六、与其他篇章的关系

- **理论**：[01-RAG检索增强生成.md](01-RAG检索增强生成.md) 讲 RAG 原理与切分/向量/检索概念。
- **代码**：[03-RAG向量检索实战.md](03-RAG向量检索实战.md) 深入 `KnowledgeBaseService` / `ChatServiceImpl.chatWithRag` 实现细节。
- **进阶**：[04-RAG进阶实战-Rerank重排序.md](04-RAG进阶实战-Rerank重排序.md) 专讲 `SiliconFlowRerankService` 精排与 `RerankProperties` 配置。
