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
# ---- 本段讲：用 AsyncOpenAI 客户端接任意 OpenAI 兼容服务 ----
# 对照 Java：类似 spring-ai-openai 或 langchain4j 里配 OpenAiChatModel；
# base_url/api_key/model 三项全部来自配置（见 20 章的 Settings），不在代码里写死。
client = AsyncOpenAI(                       # Async 开头 = 异步客户端，方法返回协程，需 await；同步版是 OpenAI
    base_url=settings.llm_base_url,         # 指向 DeepSeek/硅基流动等兼容网关地址，默认才用 api.openai.com
    api_key=settings.llm_api_key,           # 密钥从配置注入（SecretStr），绝不硬编码在源码里
    timeout=settings.llm_timeout,           # 请求超时（秒）；LLM 生成慢，必须显式设，否则默认可能长时间挂起
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
# ---- 本段讲：RAG 引用溯源的核心——从答案文本反查引用编号 ----
# 模型只输出 [1]、[2] 这样的编号，真实 document_id/chunk_id 由服务端保存并映射，
# 绝不能让模型自己编造来源。Java 对照：正则 Pattern + 提取后映射 DTO 列表。
import re
# re 是 Python 标准库的正则模块，等价 Java 的 java.util.regex.Pattern/Matcher

def extract_citations(answer: str, num_references: int) -> list[int]:
    """从答案中提取被引用的参考文献编号"""
    pattern = r'\[(\d+)\]'
    # 原始字符串 r'' 避免反斜杠转义问题；\[(\d+)\] 匹配 [数字] 并用括号捕获数字部分
    matches = re.findall(pattern, answer)
    # findall 返回所有捕获组的字符串列表，如 ["1", "2"]；等价 Java 的 Matcher.find() 循环 + group(1)
    return [int(m) for m in matches if 1 <= int(m) <= num_references]
    # 列表推导式：把字符串转 int，并过滤掉越界编号（模型可能幻觉出 [9] 但只有 5 个片段），
    # 防止后续按编号索引时越界，等价 Java 的 stream().map(Integer::parseInt).filter(...)

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
    # 前面这一大段 docstring 是给调用方看的"接口说明"，不影响运行；等价 Java 的 Javadoc
    citation_indices = extract_citations(answer, len(chunks))
    # 第一步：从答案文本里提取出所有合法引用编号，len(chunks) 是编号上限
    # （编号从 1 开始，对应片段下标 0，所以判断时要用 i+1）

    for i, chunk in enumerate(chunks):
        # enumerate 同时给元素和下标，等价 Java 的 for (int i = 0; i < list.size(); i++)
        chunk["is_cited"] = (i + 1) in citation_indices
        # 直接在 dict 上写字段（Python dict 可变，无需新建对象）；
        # is_cited 标记该片段是否被答案实际引用，前端据此展示"答案来自哪条资料"

    return chunks
    # 原地修改后返回同一列表；Java 对照：给 DTO set 一个新字段后返回
```

---

## 七、Embedding 成本与缓存

Embedding 调用通常按 token 计费，成本远低于 Chat，但量大时仍有压力。

### 缓存策略

```python
# ---- 本段讲：Embedding 结果的进程内缓存，减少重复向量化调用 ----
# 同一段文本向量化结果不变，缓存可省 API 费用与延迟。
# Java 对照：Spring Cache 的 @Cacheable / Caffeine 本地缓存；这里是手写版便于看清原理。
class EmbeddingCache:
    """
    Embedding 缓存（可选）
    
    权衡：缓存容量 vs 重新计算
    """
    
    def __init__(self, max_size: int = 100000):
        # 构造函数；self 相当于 Java 的 this，必须显式写在参数里
        self.cache: dict[str, list[float]] = {}
        # 缓存主体：key 是 "模型:文本哈希" 字符串，value 是向量（float 列表）；普通 dict 无容量限制
        self.max_size = max_size
        # 容量上限，超过后用 FIFO 淘汰；生产上应换 LRU（functools.lru_cache 或 Caffeine）

    async def embed(self, texts: list[str], model: str) -> list[list[float]]:
        """
        向量化，优先走缓存
        
        对标 Spring Cache 的 @Cacheable
        """
        # async def：内部要 await 网络 IO，整个方法可在事件循环上并发执行
        to_compute = []
        # 待计算列表：命中的位置放 None 占位，未命中的放原文本，保证与入参 texts 按位置对齐
        cache_keys = []
        # 未命中文本对应的缓存 key，顺序与 to_compute 里非 None 的元素一一对应

        for text in texts:
            key = f"{model}:{hash(text)}"
            # 缓存 key 由模型名 + 文本哈希组成：换 Embedding 模型后哈希不变但向量空间不同，
            # 必须带上 model 避免新旧向量混用；hash() 内置但对 str 有随机化，重启后会变（教学示例可接受）
            if key in self.cache:
                # 缓存命中
                to_compute.append(None)
                # None 占位，表示"这个位置已有缓存，不用算"，同时保住下标对齐
            else:
                # 缓存未命中，需计算
                to_compute.append(text)
                cache_keys.append(key)
        
        # 只对未命中的文本调用 API
        if any(t is not None for t in to_compute):
            # any() 短路求值：只要有一个未命中就发起批量调用；批量 Embedding 比逐条调便宜得多
            computed = await self.embedding_client.embed([t for t in to_compute if t])
            # 列表推导式过滤掉 None 占位后批量请求；await 等待异步网络调用完成
            
            # 写入缓存
            for key, embedding in zip(cache_keys, computed):
                # zip 把 key 序列与结果序列按下标配对，等价 Java 的 IntStream 遍历两个 list
                if len(self.cache) >= self.max_size:
                    self.cache.pop(next(iter(self.cache)))  # FIFO 淘汰
                    # next(iter(dict)) 取第一个插入的 key 并删除，即先进先出淘汰；
                    # Python 3.7+ dict 保持插入序，这里借它实现简易 FIFO
                self.cache[key] = embedding
        
        # 组合结果
        results = []
        for text, computed_val in zip(texts, to_compute):
            # 再次按位置配对：texts 是原始输入，to_compute 记录每个位置是命中还是未命中
            if computed_val is None:
                key = f"{model}:{hash(text)}"
                results.append(self.cache[key])
                # 命中位：从缓存取回已存在的向量
            else:
                # 从刚计算的结果取
                ...
                # 教学片段省略了实现：应按下标从 computed 中取出对应向量 append；
                # 注意此片段是伪代码性质，重点在"命中跳过、未命中批量算"的思路
        
        return results
```

---

## 八、Rerank 的使用时机与降级

Rerank 精度高，但也有成本。不是每次都必须用：

```python
# ---- 本段讲：RAG 在线读路径「向量粗召回 → 可选 Rerank → 失败降级」的完整链路 ----
# 对照 Java：向量检索 ≈ VectorStore.similaritySearch；Rerank ≈ 再调一次精排模型给候选重打分。
# 关键工程点：Rerank 是额外的网络调用，必须有开关、有超时、失败能降级回原向量排序，否则会拖垮主链路。
async def retrieve_with_rerank(           # 异步函数：内部有多处 await 网络调用，不能阻塞事件循环
    question: str,                        # 用户原始问题（查询文本）
    use_rerank: bool = True,              # 是否启用 Rerank 精排；默认开，要求低延迟时可关
) -> list[dict]:                          # 返回命中的片段列表（每项含 id / 内容 / 分数等元数据）
    """
    检索 → 可选 Rerank → 返回
    
    决策树：
    - 响应要求快（<100ms）？ → 跳过 Rerank
    - 检索结果已足够清晰？ → 跳过 Rerank
    - 用户问题复杂/歧义多？ → 用 Rerank
    """
    
    # 1. 向量检索 Top 50（快）
    candidates = await vector_db.search(question, top_k=50)
    # ↑ 粗召回：从向量库取最相似的 50 条，代价低、速度快，但只保证"可能相关"；
    #   等价 Java 的 vectorStore.similaritySearch(question, 50)
    
    if not use_rerank or len(candidates) <= 5:
        return candidates[:10]
    # ↑ 短路返回：不需要精排、或候选本来就少（精排收益低）时，直接取前 10 条，省一次网络调用
    
    # 2. Rerank Top 50 → Top 10（精准但慢）
    try:
        reranked = await rerank_client.rank(question, candidates)
        # ↑ 精排：把"问题 + 候选"一起交给重排序模型打分；它能判断"这段是否真能回答当前问题"，
        #   比单纯向量相似度准，但多一次网络往返、有成本
        return reranked[:10]              # 取精排后的前 10 条作为最终上下文
    except Exception as e:
        # 降级：Rerank 失败直接返回向量检索结果
        logger.warning("Rerank 失败，使用向量检索结果", extra={"error": str(e)})
        # extra= 给日志挂结构化字段；等价 Java 的 log.warn(msg, e)，但更便于日志系统检索
        return candidates[:10]            # 兜底：精排挂了也不能让整条检索链失败，退回向量排序结果
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
# ---- 本段讲：RAG 质量评估——用固定测试集给"召回率 / 准确率"设门禁 ----
# 对照 Java：≈ JUnit 参数化测试 + 断言阈值；这里用 pytest，测试失败即"质量回退"，可接进 CI。
# 核心思想：把"改了 chunk/prompt 后感觉变准了"变成可量化的数字，防止凭感觉上线。
# test_rag_quality.py                       # 文件名：RAG 质量测试
@pytest.fixture                              # fixture：被注入到测试函数参数里的"准备数据"（≈ @BeforeEach 的准备逻辑）
def test_cases():                            # 提供测试集的夹具；所有用例共用同一份问题集
    """构建 50 个测试问题"""                  # docstring：给调用方看的说明，运行时不执行（≈ Javadoc）
    return [                                 # 每个元素是一条"问题 + 期望文档 + 期望关键词"的用例
        {
            "question": "退款要多久？",           # 用户问题
            "expected_document_ids": ["refund-policy"],   # 期望被检索到的文档 ID（用于算召回率）
            "expected_keywords": ["3~5 工作日"],          # 答案里必须出现的关键词（用于算准确率）
        },
        # ...                                # 教学省略：真实测试集应有几十条覆盖真实业务的问题
    ]

def test_retrieval_recall(test_cases):       # 召回率测试；参数名 test_cases 对应上面的夹具，pytest 自动注入
    """检索召回率"""                          # 含义：相关文档被成功检索到的比例
    total = len(test_cases)                   # 问题总数（分母）
    recalled = sum(                           # 命中数（分子）：统计有多少条用例的期望文档都被检索到
        1 for tc in test_cases                 # 每条用例记 1（当下面条件成立时）
        if all(doc_id in retrieved for doc_id in tc["expected_document_ids"])
        # ↑ all(...)：该用例期望的每个 document_id 都出现在检索结果里，才算这条召回成功
        for retrieved in [rag_service.retrieve(tc["question"])]   # 调一次检索，用单元素列表把结果绑成变量 retrieved
    )
    assert recalled / total >= 0.90  # 门禁
    # ↑ 召回率必须 ≥ 90%；不达标测试直接失败，等价 JUnit 里的 assertTrue(ratio >= 0.90)

def test_answer_accuracy(test_cases):        # 准确率测试
    """答案准确率"""                          # 含义：答案里包含期望要点的比例
    total = len(test_cases)                   # 分母
    correct = sum(                            # 分子：答案命中的条数
        1 for tc in test_cases                 # 每条记 1（当下面条件成立时）
        if any(kw in answer for kw in tc["expected_keywords"])
        # ↑ any(...)：答案里只要出现任一期望关键词就算答对（更严格的评估会对齐语义而非关键字）
        for answer in [rag_service.answer(tc["question"])]   # 调一次问答，把结果绑成变量 answer
    )
    assert correct / total >= 0.85            # 门禁：准确率 ≥ 85%
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
# ---- 本段讲：给 Rerank 加独立超时，并在超时/报错时降级回原向量排序 ----
# 对照 Java：asyncio.wait_for 等价 Mono.timeout(Duration)；把 rerank 包一层超时，
# 超时或抛错都不让主链路失败——直接退回向量召回的排序 candidates。
try:                                          # 尝试做精排
    ranked = await asyncio.wait_for(rerank(query, candidates), timeout=2.0)
    # ↑ wait_for 给协程加"真超时"（2 秒到点就取消并抛 TimeoutError）；rerank 是额外网络调用，必须限时
except (asyncio.TimeoutError, RerankError):   # 同时捕获"超时"与"精排服务自身错误"
    logger.warning("rerank 失败，降级向量排序")   # 只告警，不把异常抛给上层
    ranked = candidates                        # 降级：直接用原向量排序结果，保证仍有可用上下文
```

这与 Java 侧“`Mono.fromCallable` 必须显式 `subscribeOn(boundedElastic)`，异常降级向量排序”是同一条工程原则。

---

## 八、结构化输出：必须校验，不要直接信任模型

```python
# ---- 本段讲：结构化输出的"生成 → 解析 → 校验"三步，绝不直接信任模型返回 ----
# 对照 Java：≈ Jackson 反序列化 + Bean Validation；模型可能吐 Markdown 包裹、解释文字或缺字段，
# 所以必须先 json.loads，再用 Pydantic 模型校验，任一步失败都走修复重试或降级。
raw = await llm.chat(...)                      # 让模型"只输出 JSON"；... 表示省略了 messages 等参数
try:                                           # 解析与校验都放进 try，两类失败统一处理
    payload = json.loads(raw)                  # 1) 把字符串解析成 dict；非法 JSON 会抛 JSONDecodeError
    result = AnswerModel.model_validate(payload)
    # ↑ 2) 用 Pydantic 模型校验字段类型/必填项；缺字段或类型不符会抛 ValidationError
except (json.JSONDecodeError, ValidationError):   # 覆盖"JSON 坏了"和"结构不符合契约"两种失败
    # 记录原始输出（注意脱敏与长度），走修复重试或降级
    ...                                        # 省略实现：重试一次或用兜底答案，并留存 raw 便于排查
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
