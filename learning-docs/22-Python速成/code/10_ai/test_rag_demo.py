"""迷你 RAG 的单元测试"""

import pytest

from rag_demo import (
    Document,
    MiniRAG,
    build_citations,
    build_demo_rag,
    build_prompt,
    chunk_document,
    cosine,
    tokenize,
    vectorize,
)


def test_tokenize_supports_chinese_bigrams():
    tokens = tokenize("退款 refund")
    assert "refund" in tokens
    assert "退款" in tokens


def test_cosine_identical_and_disjoint():
    assert cosine(vectorize("退款"), vectorize("退款")) == pytest.approx(1.0)
    assert cosine(vectorize("退款"), vectorize("xyz")) == 0.0
    assert cosine(vectorize(""), vectorize("退款")) == 0.0


def test_chunk_preserves_metadata_and_overlap():
    document = Document("d1", "标题", "abcdefghijklmnopqrstuvwxyz")
    chunks = chunk_document(document, max_chars=10, overlap=2)

    assert len(chunks) == 3
    assert chunks[0].chunk_id == "d1-c000"
    assert chunks[0].document_id == "d1"
    assert chunks[0].text == "abcdefghij"
    assert chunks[1].text.startswith("ij")


def test_chunk_rejects_invalid_overlap():
    with pytest.raises(ValueError):
        chunk_document(Document("d", "t", "text"), max_chars=10, overlap=10)


def test_search_returns_relevant_document_first():
    rag = build_demo_rag()
    hits = rag.search("退款多久到账", top_k=2, min_score=0.01)

    assert hits
    assert hits[0].chunk.document_id == "refund-policy"
    assert len(hits) <= 2
    assert hits[0].score >= hits[-1].score


def test_search_top_k_and_threshold():
    rag = build_demo_rag()
    assert len(rag.search("订单", top_k=1)) == 1
    assert rag.search("完全无关的英文 zzz qqq", min_score=0.99) == []


def test_prompt_and_citations_share_stable_reference():
    rag = build_demo_rag()
    hits = rag.search("退款政策", top_k=1)
    prompt = build_prompt("怎么退款", hits)
    citations = build_citations(hits)

    assert "[1] 来源：" in prompt
    assert citations[0]["ref"] == 1
    assert citations[0]["chunk_id"] == hits[0].chunk.chunk_id
    assert citations[0]["document_id"] == "refund-policy"


def test_empty_search_prompt_says_no_material():
    prompt = build_prompt("问题", [])
    assert "没有检索到可用资料" in prompt
    assert "资料不足时明确说不知道" in prompt


def test_empty_rag_search():
    assert MiniRAG().search("任何问题") == []
