"""纯标准库迷你 RAG：分块 -> 稀疏向量检索 -> 上下文与引用

运行: python 10_ai/rag_demo.py
测试: pytest 10_ai/test_rag_demo.py -q

诚实边界：这里用词频向量演示 RAG 的职责分层，不是语义 Embedding。
生产应替换为真实 Embedding + Chroma/pgvector，并可增加 Rerank。
"""

import math
import re
from collections import Counter
from dataclasses import dataclass


@dataclass(frozen=True)
class Document:
    document_id: str
    title: str
    text: str


@dataclass(frozen=True)
class Chunk:
    document_id: str
    chunk_id: str
    title: str
    text: str


@dataclass(frozen=True)
class SearchHit:
    chunk: Chunk
    score: float


def tokenize(text: str) -> list[str]:
    """教学中英文混合分词：英文单词 + 中文单字和二元词组。"""
    tokens = re.findall(r"[a-z0-9_]+", text.lower())
    chinese = re.findall(r"[\u4e00-\u9fff]", text)
    tokens.extend(chinese)
    tokens.extend(chinese[i] + chinese[i + 1] for i in range(len(chinese) - 1))
    return tokens


def vectorize(text: str) -> Counter[str]:
    return Counter(tokenize(text))


def cosine(left: Counter[str], right: Counter[str]) -> float:
    if not left or not right:
        return 0.0
    common = set(left) & set(right)
    numerator = sum(left[key] * right[key] for key in common)
    left_norm = math.sqrt(sum(value * value for value in left.values()))
    right_norm = math.sqrt(sum(value * value for value in right.values()))
    return numerator / (left_norm * right_norm) if left_norm and right_norm else 0.0


def chunk_document(document: Document, max_chars: int = 50, overlap: int = 10) -> list[Chunk]:
    if max_chars <= 0:
        raise ValueError("max_chars 必须为正")
    if not 0 <= overlap < max_chars:
        raise ValueError("overlap 必须满足 0 <= overlap < max_chars")

    text = document.text.strip()
    chunks = []
    step = max_chars - overlap
    for index, start in enumerate(range(0, len(text), step)):
        piece = text[start:start + max_chars].strip()
        if not piece:
            break
        chunks.append(Chunk(
            document_id=document.document_id,
            chunk_id=f"{document.document_id}-c{index:03d}",
            title=document.title,
            text=piece,
        ))
        if start + max_chars >= len(text):
            break
    return chunks


class MiniRAG:
    def __init__(self):
        self._chunks: list[Chunk] = []
        self._vectors: list[Counter[str]] = []

    def add_document(self, document: Document, *, max_chars: int = 50, overlap: int = 10) -> int:
        chunks = chunk_document(document, max_chars=max_chars, overlap=overlap)
        for chunk in chunks:
            self._chunks.append(chunk)
            self._vectors.append(vectorize(chunk.text))
        return len(chunks)

    def search(self, query: str, *, top_k: int = 3, min_score: float = 0.0) -> list[SearchHit]:
        if top_k <= 0:
            raise ValueError("top_k 必须为正")
        query_vector = vectorize(query)
        hits = [
            SearchHit(chunk=chunk, score=cosine(query_vector, vector))
            for chunk, vector in zip(self._chunks, self._vectors)
        ]
        hits = [hit for hit in hits if hit.score >= min_score]
        return sorted(hits, key=lambda hit: hit.score, reverse=True)[:top_k]


def build_prompt(query: str, hits: list[SearchHit]) -> str:
    """编号由程序生成；模型引用 [1] 后，可稳定映射回 hits[0]。"""
    if not hits:
        context = "（没有检索到可用资料）"
    else:
        context = "\n\n".join(
            f"[{index}] 来源：{hit.chunk.title} / {hit.chunk.chunk_id}\n{hit.chunk.text}"
            for index, hit in enumerate(hits, start=1)
        )
    return (
        "你是客服助手。只能依据给定资料回答；资料不足时明确说不知道。\n"
        "使用资料的结论后标注引用编号，例如 [1]。不要编造来源。\n\n"
        f"资料：\n{context}\n\n用户问题：{query}"
    )


def build_citations(hits: list[SearchHit]) -> list[dict]:
    return [
        {
            "ref": index,
            "document_id": hit.chunk.document_id,
            "chunk_id": hit.chunk.chunk_id,
            "score": round(hit.score, 4),
        }
        for index, hit in enumerate(hits, start=1)
    ]


def build_demo_rag() -> MiniRAG:
    rag = MiniRAG()
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
    for document in documents:
        rag.add_document(document, max_chars=42, overlap=8)
    return rag


def main() -> None:
    rag = build_demo_rag()
    query = "退款多久能到账？"
    hits = rag.search(query, top_k=2, min_score=0.01)

    print("=" * 64)
    print(f"问题：{query}")
    print("=" * 64)
    for index, hit in enumerate(hits, start=1):
        print(f"[{index}] score={hit.score:.4f} {hit.chunk.chunk_id}")
        print(f"    {hit.chunk.text}")

    print()
    print("返回给前端的引用元数据：")
    for citation in build_citations(hits):
        print(" ", citation)

    print()
    print("发送给 LLM 的提示词：")
    print("-" * 64)
    print(build_prompt(query, hits))

    assert hits, "至少应召回一个退款相关片段"
    assert hits[0].chunk.document_id == "refund-policy"


if __name__ == "__main__":
    main()
