"""纯标准库迷你 RAG：分块 -> 稀疏向量检索 -> 上下文与引用

运行: python 10_ai/rag_demo.py
测试: pytest 10_ai/test_rag_demo.py -q

诚实边界：这里用词频向量演示 RAG 的职责分层，不是语义 Embedding。
生产应替换为真实 Embedding + Chroma/pgvector，并可增加 Rerank。
"""

# ============================================================
# 本文件用"零第三方依赖"的方式实现一个迷你 RAG，目的是讲清职责分层：
#   1) 切块 chunk_document：长文档切成带重叠的小片段
#   2) 向量化 vectorize：把文本变成"词 -> 词频"的稀疏向量
#   3) 检索 MiniRAG.search：按余弦相似度排序，取 top_k
#   4) 组装 build_prompt / build_citations：拼上下文并生成可回链的引用
# Java 对照：相当于 LangChain4j / Spring AI 的 EmbeddingStore + Retriever
#           + PromptTemplate，只是这里手写以便看清每一步到底发生了什么。
# 阅读顺序建议：tokenize -> vectorize -> cosine -> chunk_document -> MiniRAG -> main
# ============================================================

# math.sqrt：算向量模长，余弦相似度的分母 <-> Math.sqrt
import math
# re：正则，用于切分英文单词、提取中文字符 <-> java.util.regex.Pattern
import re
# Counter：带计数的 dict，天然适合统计词频 <-> Map<String, Integer> + merge
from collections import Counter
# dataclass：自动生成 __init__ / __repr__ / __eq__ 的类 <-> Lombok 的 @Data，接近 Java record
from dataclasses import dataclass


# 文档：一篇原始资料的不可变快照
# frozen=True 表示字段只读且实例可哈希 <-> Java 的 record（不可变值对象）
@dataclass(frozen=True)
class Document:
    document_id: str   # 文档唯一标识，用于引用回链
    title: str         # 标题，展示在提示词与引用里
    text: str          # 正文全文


# 片段：文档切块后的最小检索单元，保留回溯到原文档所需的全部信息
@dataclass(frozen=True)
class Chunk:
    document_id: str   # 所属文档 id
    chunk_id: str      # 片段 id，形如 "refund-policy-c000"
    title: str         # 冗余存一份标题，检索时不必再回查文档
    text: str          # 片段正文


# 命中结果：片段 + 相似度分数
@dataclass(frozen=True)
class SearchHit:
    chunk: Chunk       # 命中的片段
    score: float       # 余弦相似度，越大越相关


# 教学用分词：英文按单词、中文按单字 + 相邻二字组（bi-gram）
# 为什么中文要二元组：单字区分度太低（"退" vs "款"），"退款"这类词才能表达语义
def tokenize(text: str) -> list[str]:
    """教学中英文混合分词：英文单词 + 中文单字和二元词组。"""
    # 先转小写，再用正则提取连续的字母/数字/下划线：让 "Refund" 与 "refund" 算同一个词
    tokens = re.findall(r"[a-z0-9_]+", text.lower())
    # 逐个字符提取汉字（\u4e00-\u9fff 是常用汉字范围）
    chinese = re.findall(r"[\u4e00-\u9fff]", text)
    # 把中文单字也作为 token 加入，提高召回
    tokens.extend(chinese)
    # 相邻两字拼成二元组加入：["退","款","多"] -> ["退款","款多"]
    # 生成器表达式配合 extend，避免再建一个中间列表
    tokens.extend(chinese[i] + chinese[i + 1] for i in range(len(chinese) - 1))
    # 返回 token 列表；顺序不重要，下一步 Counter 会做聚合
    return tokens


# 向量化：把文本变成"词 -> 出现次数"的稀疏向量
# 生产环境应替换为 Embedding 模型输出的稠密向量（这也是 RAG 真正抓语义的地方）
def vectorize(text: str) -> Counter[str]:
    # Counter 对 token 序列自动计数 <-> groupingBy + counting
    return Counter(tokenize(text))


# 余弦相似度：cos = A·B / (|A| * |B|)，词频非负时取值在 0~1
# 直观理解：两个词频向量的"方向"越一致，分数越高
def cosine(left: Counter[str], right: Counter[str]) -> float:
    # 任一为空向量则模长为 0，按约定返回 0，既合理又能避免除零
    if not left or not right:
        return 0.0
    # 只遍历公共词即可：非公共词在点积里的贡献本来就是 0 <-> 稀疏向量点积
    common = set(left) & set(right)
    # 分子：对应维度相乘再求和 <-> dot product
    numerator = sum(left[key] * right[key] for key in common)
    # 分母左：各分量平方和再开根号
    left_norm = math.sqrt(sum(value * value for value in left.values()))
    # 分母右：同理
    right_norm = math.sqrt(sum(value * value for value in right.values()))
    # 双重保护：模长为 0 时返回 0，否则正常计算
    return numerator / (left_norm * right_norm) if left_norm and right_norm else 0.0


# 切块：滑动窗口把长文本切成"最多 max_chars 字、相邻重叠 overlap 字"的片段
# 重叠的作用：避免关键句正好被切断导致语义丢失 <-> 文本切分里的 chunk overlap
def chunk_document(document: Document, max_chars: int = 50, overlap: int = 10) -> list[Chunk]:
    # 参数校验一：窗口长度必须为正
    if max_chars <= 0:
        raise ValueError("max_chars 必须为正")
    # 参数校验二：overlap 不能为负，也不能 >= 窗口（否则步长 <= 0 会死循环）
    if not 0 <= overlap < max_chars:
        raise ValueError("overlap 必须满足 0 <= overlap < max_chars")

    # 去掉首尾空白，避免片段开头/结尾是空白字符
    text = document.text.strip()
    # 收集结果
    chunks = []
    # 每次窗口向右移动 step 个字；step = 窗口长度 - 重叠长度
    step = max_chars - overlap
    # enumerate 同时拿到序号 index 和窗口起点 start <-> for (int i = 0; ...; i += step)
    for index, start in enumerate(range(0, len(text), step)):
        # 切片取 [start, start + max_chars)，再去掉两端空白（切片越界不会报错，自动截断）
        piece = text[start:start + max_chars].strip()
        # 切出空串说明已经越过末尾，直接结束循环
        if not piece:
            break
        # 组装片段；chunk_id 用 3 位补零序号，便于排序、日志辨认
        chunks.append(Chunk(
            document_id=document.document_id,
            chunk_id=f"{document.document_id}-c{index:03d}",
            title=document.title,
            text=piece,
        ))
        # 若本窗口已经覆盖到文本末尾，提前退出，避免多切出一个完全重复的片段
        if start + max_chars >= len(text):
            break
    # 返回全部片段
    return chunks


# 迷你 RAG 索引：内存里维护"片段列表"和"对应向量列表"，两者下标一一对应
class MiniRAG:
    def __init__(self):
        # 所有片段，按加入顺序排列
        self._chunks: list[Chunk] = []
        # 与 _chunks 同下标的向量（第 i 个向量就是第 i 个片段的向量）
        self._vectors: list[Counter[str]] = []

    # 加入一篇文档；参数列表里的 * 表示其后参数"必须写参数名"传递，防止位置传错
    def add_document(self, document: Document, *, max_chars: int = 50, overlap: int = 10) -> int:
        # 先切块
        chunks = chunk_document(document, max_chars=max_chars, overlap=overlap)
        # 逐块入索引：片段与向量成对追加，保证下标始终对齐
        for chunk in chunks:
            self._chunks.append(chunk)
            self._vectors.append(vectorize(chunk.text))
        # 返回本次新增的片段数，方便调用方统计
        return len(chunks)

    # 检索：对查询向量与所有片段向量算余弦相似度，过滤阈值后取 top_k
    def search(self, query: str, *, top_k: int = 3, min_score: float = 0.0) -> list[SearchHit]:
        # top_k 必须为正，否则后面的切片语义会出错
        if top_k <= 0:
            raise ValueError("top_k 必须为正")
        # 把查询文本向量化
        query_vector = vectorize(query)
        # 列表推导式构造全部候选命中；zip 按相同下标把片段与向量配对
        hits = [
            SearchHit(chunk=chunk, score=cosine(query_vector, vector))
            for chunk, vector in zip(self._chunks, self._vectors)
        ]
        # 过滤掉低于阈值的噪声结果（min_score 默认 0，等于保留全部）
        hits = [hit for hit in hits if hit.score >= min_score]
        # 按分数降序排序后取前 top_k 个；lambda 指定排序键
        return sorted(hits, key=lambda hit: hit.score, reverse=True)[:top_k]


# 组装提示词：把命中片段编号成 [1] [2] ...，并要求模型引用这些编号
def build_prompt(query: str, hits: list[SearchHit]) -> str:
    """编号由程序生成；模型引用 [1] 后，可稳定映射回 hits[0]。"""
    # 没检索到资料时给一句明确占位，避免模型凭空编造
    if not hits:
        context = "（没有检索到可用资料）"
    else:
        # 每个片段渲染成 "[序号] 来源：标题 / chunk_id\n正文"，片段之间空一行
        context = "\n\n".join(
            f"[{index}] 来源：{hit.chunk.title} / {hit.chunk.chunk_id}\n{hit.chunk.text}"
            # enumerate 从 1 开始计数，与人类习惯的引用编号一致 <-> [1]、[2]
            for index, hit in enumerate(hits, start=1)
        )
    # 最终提示词 = 角色 + 约束（只能依据资料 / 不足说不知道 / 标注引用）+ 资料 + 问题
    # 相邻字符串字面量会自动拼接，这是 Python 书写长文本的常见写法
    return (
        "你是客服助手。只能依据给定资料回答；资料不足时明确说不知道。\n"
        "使用资料的结论后标注引用编号，例如 [1]。不要编造来源。\n\n"
        f"资料：\n{context}\n\n用户问题：{query}"
    )


# 生成引用元数据：返回给前端做"来源可点"，与提示词里的编号一一对应
def build_citations(hits: list[SearchHit]) -> list[dict]:
    # 列表推导式：每个命中生成一个 dict，键名即前后端契约
    return [
        {
            # ref 与提示词里的 [n] 完全对应，这是"引用可回链"的关键
            "ref": index,
            # 文档 id：前端可据此跳转到文档详情页
            "document_id": hit.chunk.document_id,
            # 片段 id：可精确定位到具体段落
            "chunk_id": hit.chunk.chunk_id,
            # 分数保留 4 位小数，便于展示与调试
            "score": round(hit.score, 4),
        }
        for index, hit in enumerate(hits, start=1)
    ]


# 构造演示用知识库：三篇客服政策文档
def build_demo_rag() -> MiniRAG:
    rag = MiniRAG()
    # 用列表存放三篇文档；字符串相邻字面量会自动拼接成一个长句
    documents = [
        Document(
            "refund-policy",
            "退款政策",
            "退款申请审核通过后，款项将在三个工作日内原路退回。"
            "如果原支付渠道不可用，客服会联系用户确认新的退款方式。",
        ),
        Document(
            "shipping-policy",
            "配送说明",
            "工作日十六点前的订单通常当天发出。偏远地区的配送时间可能增加两天。"
            "用户可以在订单详情页查看物流轨迹。",
        ),
        Document(
            "stock-policy",
            "库存规则",
            "库存不足时不能创建订单。取消订单或支付超时后，系统会回补已扣减库存。",
        ),
    ]
    # 逐篇入索引；窗口设小一点（42 字）便于演示"一篇文档被切成多个片段"
    for document in documents:
        rag.add_document(document, max_chars=42, overlap=8)
    return rag


# 脚本入口：跑一次完整流程并打印中间结果，便于观察每一步
def main() -> None:
    # 1) 建索引
    rag = build_demo_rag()
    # 2) 用户问题
    query = "退款多久能到账？"
    # 3) 检索：取前 2 条，分数阈值 0.01（滤掉几乎完全不相关的片段）
    hits = rag.search(query, top_k=2, min_score=0.01)

    # 打印分隔线与问题，方便阅读控制台输出
    print("=" * 64)
    print(f"问题：{query}")
    print("=" * 64)
    # 逐条打印命中：序号、分数（4 位小数）、片段 id、片段正文
    for index, hit in enumerate(hits, start=1):
        print(f"[{index}] score={hit.score:.4f} {hit.chunk.chunk_id}")
        print(f"    {hit.chunk.text}")

    print()
    print("返回给前端的引用元数据：")
    # 打印引用元数据，展示"引用编号 -> 文档/片段"的映射关系
    for citation in build_citations(hits):
        print(" ", citation)

    print()
    print("发送给 LLM 的提示词：")
    print("-" * 64)
    # 打印最终提示词，能直观看到检索到的上下文是如何拼进去的
    print(build_prompt(query, hits))

    # 断言：至少召回一条结果 <-> JUnit 的 assertTrue / assertEquals
    assert hits, "至少应召回一个退款相关片段"
    # 断言：最相关的一条必须来自退款政策文档
    assert hits[0].chunk.document_id == "refund-policy"


# 只有直接运行本文件才执行 main；被 import（例如测试）时不执行
if __name__ == "__main__":
    main()
