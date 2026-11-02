# 21 · AI 应用实战：从对话到 RAG

> 你已经有两套可对照实现：Java 的 `ai-cs-chat` / `ai-cs-knowledge`，Python 的 `ai-cs-py-chat`。
> 这篇不讲模型理论，讲 AI 服务落地时最容易漏掉的工程环节。

---

## 一、最小 AI 对话链路

```text
HTTP 请求
  ↓ Pydantic 校验
消息组装：system + history + current
  ↓
LLM Client（OpenAI 兼容协议）
  ↓ 非流式 / SSE 流式
响应模型 / SSE 分帧
```

Python 侧对应：

```text
app/api/chat.py              路由、SSE 分帧
app/schemas/chat.py          请求/响应契约
app/services/chat_service.py 业务编排
app/core/llm.py              AsyncOpenAI 客户端
app/config.py                base_url / api_key / model / timeout
```

Java 侧同一链路多了 RAG、Redis 历史、RocketMQ 异步落库、Resilience4j 等能力。**换语言不会改变这些架构职责，只改变实现方式**。

---

## 二、OpenAI 兼容协议的正确理解

DeepSeek、硅基流动等 OpenAI 兼容服务，可以复用同一客户端：

```python
client = AsyncOpenAI(
    base_url=settings.llm_base_url,
    api_key=settings.llm_api_key,
    timeout=settings.llm_timeout,
)
```

切换供应商通常只改：

- `base_url`
- `api_key`
- `model`

但“兼容”不代表所有能力一致：

- embeddings 是否提供、模型名和维度
- tool calling / JSON mode 的支持程度
- 流式 chunk 结构细节
- 限流、计费、上下文长度

**Embedding 与 Chat 要分别配置**。你项目里 DeepSeek 没有 `/v1/embeddings`，所以 Embedding 单独走硅基流动 BGE-M3；这条约束在 Python 服务里同样成立，不能因为共用 OpenAI 客户端就假设同一供应商什么接口都有。

---

## 三、普通对话必须补的工程能力

| 能力 | 最小实现 | 不能漏掉的原因 |
|---|---|---|
| 超时 | `AsyncOpenAI(timeout=)` / `asyncio.wait_for` | 防止请求无限占用 |
| 重试 | 仅网络/限流/部分 5xx，指数退避 + jitter | 避免瞬时故障 |
| 降级 | 返回明确兜底文案/错误码 | 故障时前端可结束状态 |
| 熔断 | 连续失败后短时拒绝请求 | 防止故障放大 |
| 限流 | 用户/租户/全局 Semaphore | 控制成本和下游压力 |
| 日志 | model、耗时、token 用量、traceId | 定位与成本核算 |
| 脱敏 | 不打完整 key、敏感 prompt | 防泄漏 |

流式响应要谨慎重试：**已经开始输出给用户的内容不能简单重放**。可以在尚未产出首个 delta 前重试；已产出后通常以错误帧 + `[DONE]` 收尾。

---

## 四、历史上下文不是越长越好

```text
system prompt
+ 最近 N 轮消息
+ 当前问题
+ 检索到的知识片段（RAG）
```

需要控制：

- 最大历史轮数/字符数/token 数
- system 与用户内容的角色边界
- 超长历史的摘要策略
- 多租户/多会话隔离

不要简单 `history[-100:]` 就结束。消息数不等于 token 数，检索片段也会挤占上下文。生产上至少记录每次请求的估算 token 与模型返回的 usage。

---

## 五、RAG 的标准链路

```text
离线（写路径）：
文档 → 清洗 → 分块 → Embedding → 向量库 + 文档元数据

在线（读路径）：
问题 →（可选改写）→ 向量检索 TopK → Rerank → 上下文拼装 → LLM → 答案 + 引用
```

### 读写两侧必须一致

- 同一 Embedding 模型
- 同一向量维度与距离度量
- 同一 collection / 租户过滤规则
- 分块元数据保留 document_id、chunk_id、来源位置

你项目里 `chat`（检索）与 `knowledge`（向量化入库）共用 `aics-knowledge` 和同一 Embedding 模型，**擅自换模型会让新旧向量不在同一空间，检索直接失效**。

### 分块不是越小越好

| 块太大 | 块太小 |
|---|---|
| 噪声多、上下文超预算 | 语义不完整、答案断章取义 |
| 相似度被稀释 | 检索数量与拼接成本上升 |

常见起点：按标题/段落优先切分，再按 300~800 中文字左右的窗口控制，并保留少量重叠。最终用真实问题集评估，不背固定数字。

---

## 六、引用溯源是 RAG 的一部分

返回的不应只有答案：

```json
{
  "answer": "退款会在审核后原路退回。",
  "citations": [
    {"document_id": "refund-policy", "chunk_id": "c-003", "score": 0.82}
  ]
}
```

提示词里给每个片段稳定编号：

```text
[1] 来源：refund-policy / c-003
内容：...

[2] 来源：refund-policy / c-004
内容：...

要求：
- 只能依据给定资料回答，必须用 [数字] 标记引用
- 资料不足时明确说"我无法根据现有资料回答"
```

答案里用 `[1]`、`[2]` 引用，然后解析答案中的数字，反向映射到原始 citations。

**解析映射算法：**

```python
import re

def extract_citations(answer: str, num_references: int) -> list[int]:
    """从答案中提取被引用的参考文献编号"""
    pattern = r'\[(\d+)\]'
    matches = re.findall(pattern, answer)
    return [int(m) for m in matches if 1 <= int(m) <= num_references]

def link_citations(answer: str, chunks: list[dict]) -> list[dict]:
    """
    关联引用
    
    答案: "根据 [1]，退款在 [2] 时间内处理。"
    chunks: [
        {"id": "c-001", "content": "..."},
        {"id": "c-002", "content": "..."},
    ]
    
    返回: [
        {"id": "c-001", "content": "...", "is_cited": True},
        {"id": "c-002", "content": "...", "is_cited": True},
    ]
    """
    citation_indices = extract_citations(answer, len(chunks))
    
    for i, chunk in enumerate(chunks):
        chunk["is_cited"] = (i + 1) in citation_indices
    
    return chunks
```

---

## 七、Embedding 成本与缓存

Embedding 调用通常按 token 计费，成本远低于 Chat，但量大时仍有压力。

### 缓存策略

```python
class EmbeddingCache:
    """
    Embedding 缓存（可选）
    
    权衡：缓存容量 vs 重新计算
    """
    
    def __init__(self, max_size: int = 100000):
        self.cache: dict[str, list[float]] = {}
        self.max_size = max_size
    
    async def embed(self, texts: list[str], model: str) -> list[list[float]]:
        """
        向量化，优先走缓存
        
        对标 Spring Cache 的 @Cacheable
        """
        to_compute = []
        cache_keys = []
        
        for text in texts:
            key = f"{model}:{hash(text)}"
            if key in self.cache:
                # 缓存命中
                to_compute.append(None)
            else:
                # 缓存未命中，需计算
                to_compute.append(text)
                cache_keys.append(key)
        
        # 只对未命中的文本调用 API
        if any(t is not None for t in to_compute):
            computed = await self.embedding_client.embed([t for t in to_compute if t])
            
            # 写入缓存
            for key, embedding in zip(cache_keys, computed):
                if len(self.cache) >= self.max_size:
                    self.cache.pop(next(iter(self.cache)))  # FIFO 淘汰
                self.cache[key] = embedding
        
        # 组合结果
        results = []
        for text, computed_val in zip(texts, to_compute):
            if computed_val is None:
                key = f"{model}:{hash(text)}"
                results.append(self.cache[key])
            else:
                # 从刚计算的结果取
                ...
        
        return results
```

---

## 八、Rerank 的使用时机与降级

Rerank 精度高，但也有成本。不是每次都必须用：

```python
async def retrieve_with_rerank(
    question: str,
    use_rerank: bool = True,
) -> list[dict]:
    """
    检索 → 可选 Rerank → 返回
    
    决策树：
    - 响应要求快（<100ms）？ → 跳过 Rerank
    - 检索结果已足够清晰？ → 跳过 Rerank
    - 用户问题复杂/歧义多？ → 用 Rerank
    """
    
    # 1. 向量检索 Top 50（快）
    candidates = await vector_db.search(question, top_k=50)
    
    if not use_rerank or len(candidates) <= 5:
        return candidates[:10]
    
    # 2. Rerank Top 50 → Top 10（精准但慢）
    try:
        reranked = await rerank_client.rank(question, candidates)
        return reranked[:10]
    except Exception as e:
        # 降级：Rerank 失败直接返回向量检索结果
        logger.warning("Rerank 失败，使用向量检索结果", extra={"error": str(e)})
        return candidates[:10]
```

---

## 九、评估指标与持续改进

### 常见指标

| 指标 | 计算方式 | 改进方向 |
|------|---------|---------|
| **准确率** | 回答正确的问题数 / 总问题数 | 调整提示词、Embedding 模型、分块 |
| **召回率** | 检索到相关文档的问题数 / 总问题数 | 加分块、调参数 TopK、多路检索 |
| **F1** | 2 × (准确 × 召回) / (准确 + 召回) | 综合指标 |
| **响应时间** | P99 延迟 | 缓存、Rerank 降级 |
| **成本** | token 总数 × 单价 | 压缩 context、减 Rerank 频率 |

### 测试集构建

```python
# test_rag_quality.py
@pytest.fixture
def test_cases():
    """构建 50 个测试问题"""
    return [
        {
            "question": "退款要多久？",
            "expected_document_ids": ["refund-policy"],
            "expected_keywords": ["3~5 工作日"],
        },
        # ...
    ]

def test_retrieval_recall(test_cases):
    """检索召回率"""
    total = len(test_cases)
    recalled = sum(
        1 for tc in test_cases
        if all(doc_id in retrieved for doc_id in tc["expected_document_ids"])
        for retrieved in [rag_service.retrieve(tc["question"])]
    )
    assert recalled / total >= 0.90  # 门禁

def test_answer_accuracy(test_cases):
    """答案准确率"""
    total = len(test_cases)
    correct = sum(
        1 for tc in test_cases
        if any(kw in answer for kw in tc["expected_keywords"])
        for answer in [rag_service.answer(tc["question"])]
    )
    assert correct / total >= 0.85
```

---

## 十、生产部署清单

```text
□ Embedding 模型与向量库对齐
□ 提示词版本控制（Git + 审批）
□ 超时与重试配置明确
□ 降级兜底文案清晰
□ 成本监控告警
□ 日志里记录 model、tokens、耗时、成本
□ 入库前清洗/审核（防止对抗样本）
□ 读写一致性测试通过
□ Rerank 异常降级验证
□ 引用映射测试通过
□ 多租户隔离
□ 限流/熔断/降级链路完整
□ 灾备方案（Embedding/Vector DB/LLM 任一宕机）
```

---

**上一篇**：[20-部署与生产运行](20-部署与生产运行.md)　**下一篇**：完结
- 结论后标注引用编号，如 [1]
```

**引用编号由你的程序生成并映射回真实元数据**，不要让模型自己编 document_id。

---

## 七、Rerank 与超时

向量召回强调“可能相关”，Rerank 更擅长判断“这段是否能回答当前问题”。但 Rerank 是额外网络调用，必须：

- 限定候选数量
- 设置独立超时
- 失败时降级为原向量排序
- 非流式主链路不阻塞事件循环

```python
try:
    ranked = await asyncio.wait_for(rerank(query, candidates), timeout=2.0)
except (asyncio.TimeoutError, RerankError):
    logger.warning("rerank 失败，降级向量排序")
    ranked = candidates
```

这与 Java 侧“`Mono.fromCallable` 必须显式 `subscribeOn(boundedElastic)`，异常降级向量排序”是同一条工程原则。

---

## 八、结构化输出：必须校验，不要直接信任模型

```python
raw = await llm.chat(...)
try:
    payload = json.loads(raw)
    result = AnswerModel.model_validate(payload)
except (json.JSONDecodeError, ValidationError):
    # 记录原始输出（注意脱敏与长度），走修复重试或降级
    ...
```

建议：

- 输出 schema 尽量小，字段枚举明确
- 要求“只输出 JSON，不要 Markdown 代码块”
- 服务端仍要处理 ```json 包裹、解释文字、缺字段
- 金额、订单号、库存等事实字段不能由模型自由生成

---

## 九、评估：至少准备一个小问题集

```text
问题 / 期望命中的文档 / 必须包含的要点 / 不应出现的回答
```

每次修改 chunk、prompt、TopK、Rerank 后比较：

- 检索命中率
- 有引用且引用正确的比例
- 拒答是否正确
- 平均/P95 延迟
- token 成本

没有评估集的“感觉变准了”不可靠。先做 20~50 条覆盖真实业务的问题，再谈扩大规模。

---

## 十、可运行教学示例

`code/10_ai/rag_demo.py` 实现一个**纯标准库、零外部依赖**的迷你 RAG：

```text
Document -> 分块 -> TF 稀疏向量 -> cosine TopK -> 带编号上下文 -> 引用映射
```

它不是生产向量检索：没有真实 Embedding、没有语义模型、没有 Chroma。作用是让你看清**检索、上下文拼装、引用映射三段职责**。

```bash
cd learning-docs/22-Python速成/code
python 10_ai/rag_demo.py
pytest 10_ai/test_rag_demo.py -q
```

---

## 十一、上线前检查

- [ ] Chat 与 Embedding 供应商/模型分别配置
- [ ] 读写路径使用同一 Embedding 与 collection
- [ ] LLM 与 Rerank 都有超时，Rerank 失败可降级
- [ ] 历史、检索片段、输出都有 token/长度预算
- [ ] 流式未产出前才考虑重试，产出后不重放
- [ ] 引用由服务端编号并映射真实元数据
- [ ] 模型 JSON 输出经过 Pydantic 校验
- [ ] 日志不泄露密钥和敏感 prompt
- [ ] 有真实问题集与检索/答案评估

---

**上一篇**：[20-部署与生产运行](20-部署与生产运行.md)　**下一篇**：[22-Pythonic重构清单](22-Pythonic重构清单.md)
