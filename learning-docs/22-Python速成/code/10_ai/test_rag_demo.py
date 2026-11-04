"""迷你 RAG 的单元测试"""

# ============================================================
# 本文件演示：给 rag_demo 里的纯函数/类写单元测试。
# 覆盖：分词、余弦相似度边界、切块元数据与重叠、非法参数、
#       检索排序与阈值、提示词与引用编号一致性、空结果兜底。
# Java 对照：JUnit5 的 @Test + assertEquals；pytest.approx <-> assertEquals(double, delta)；
#           pytest.raises <-> assertThrows。
# ============================================================

import pytest

# 从被测模块导入所有需要的符号；pytest 会把测试文件所在目录加入 sys.path
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


# 中文要能切出二元组，英文要能切出完整单词
def test_tokenize_supports_chinese_bigrams():
    tokens = tokenize("退款 refund")
    # 英文词被保留为完整 token
    assert "refund" in tokens
    # 中文"退款"作为二元组出现
    assert "退款" in tokens


# 余弦相似度的三个边界：自己跟自己 = 1、完全无关 = 0、空向量 = 0
def test_cosine_identical_and_disjoint():
    # pytest.approx：浮点比较允许误差 <-> assertEquals(1.0, x, 1e-6)
    assert cosine(vectorize("退款"), vectorize("退款")) == pytest.approx(1.0)
    # 没有任何公共词，点积为 0
    assert cosine(vectorize("退款"), vectorize("xyz")) == 0.0
    # 空文本的向量为空，约定返回 0 而不是抛除零异常
    assert cosine(vectorize(""), vectorize("退款")) == 0.0


# 切块要保留文档元信息，并保证相邻片段之间存在重叠
def test_chunk_preserves_metadata_and_overlap():
    # 26 个字母的文本，窗口 10 字、重叠 2 字
    document = Document("d1", "标题", "abcdefghijklmnopqrstuvwxyz")
    chunks = chunk_document(document, max_chars=10, overlap=2)

    # 步长为 8，切成 3 块：0-10、8-18、16-26
    assert len(chunks) == 3
    # 片段编号按 3 位补零
    assert chunks[0].chunk_id == "d1-c000"
    # document_id 被正确带到片段上
    assert chunks[0].document_id == "d1"
    # 第一块正好是前 10 个字符
    assert chunks[0].text == "abcdefghij"
    # 第二块从下标 8 开始，所以以 "ij" 开头 —— 这正是重叠部分
    assert chunks[1].text.startswith("ij")


# 非法重叠参数必须抛 ValueError <-> assertThrows(IllegalArgumentException.class, ...)
def test_chunk_rejects_invalid_overlap():
    # pytest.raises 作为上下文管理器，断言块内抛出指定异常
    with pytest.raises(ValueError):
        # overlap == max_chars 会让步长为 0，必须拒绝
        chunk_document(Document("d", "t", "text"), max_chars=10, overlap=10)


# 检索应按相关性降序，且退款问题首选退款政策
def test_search_returns_relevant_document_first():
    rag = build_demo_rag()
    hits = rag.search("退款多久到账", top_k=2, min_score=0.01)

    # 至少要有一条结果
    assert hits
    # 第一条必须来自退款政策文档
    assert hits[0].chunk.document_id == "refund-policy"
    # top_k 生效，结果数不超过 2
    assert len(hits) <= 2
    # 排序正确：第一条分数 >= 最后一条分数
    assert hits[0].score >= hits[-1].score


# top_k 与 min_score 两个参数各自生效
def test_search_top_k_and_threshold():
    rag = build_demo_rag()
    # top_k=1 时只返回 1 条
    assert len(rag.search("订单", top_k=1)) == 1
    # 阈值 0.99 极高，无关查询应被全部过滤成空列表
    assert rag.search("完全无关的英文 zzz qqq", min_score=0.99) == []


# 提示词里的 [1] 必须能和 citations[0] 对上 —— 这是引用可回链的前提
def test_prompt_and_citations_share_stable_reference():
    rag = build_demo_rag()
    hits = rag.search("退款政策", top_k=1)
    prompt = build_prompt("怎么退款", hits)
    citations = build_citations(hits)

    # 提示词里出现了固定的引用格式
    assert "[1] 来源：" in prompt
    # 引用元数据的编号从 1 开始
    assert citations[0]["ref"] == 1
    # 引用指向的片段就是排名第一的命中片段
    assert citations[0]["chunk_id"] == hits[0].chunk.chunk_id
    # 且能追溯到正确的源文档
    assert citations[0]["document_id"] == "refund-policy"


# 没有命中时，提示词要明确告诉模型"没资料"，避免编造
def test_empty_search_prompt_says_no_material():
    prompt = build_prompt("问题", [])
    # 空上下文占位文案
    assert "没有检索到可用资料" in prompt
    # 兜底指令依然存在
    assert "资料不足时明确说不知道" in prompt


# 空索引检索应返回空列表而不是报错
def test_empty_rag_search():
    assert MiniRAG().search("任何问题") == []
