package com.aics.chat.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 缓存层数学/文本工具测试：余弦相似度 + 问题归一化。
 */
class EmbeddingMathTest {

    @Test
    void 相同向量相似度为1() {
        float[] v = {0.1f, 0.2f, 0.3f};
        assertEquals(1.0, EmbeddingMath.cosine(v, v), 1e-6);
    }

    @Test
    void 正交向量相似度为0() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};
        assertEquals(0.0, EmbeddingMath.cosine(a, b), 1e-6);
    }

    @Test
    void 反向向量相似度为负1() {
        float[] a = {1f, 0f};
        float[] b = {-1f, 0f};
        assertEquals(-1.0, EmbeddingMath.cosine(a, b), 1e-6);
    }

    @Test
    void 维度不一致或零向量返回0() {
        assertEquals(0.0, EmbeddingMath.cosine(new float[]{1f}, new float[]{1f, 2f}), 1e-6);
        assertEquals(0.0, EmbeddingMath.cosine(new float[]{0f, 0f}, new float[]{1f, 2f}), 1e-6);
    }

    @Test
    void 归一化去除标点空白并转小写() {
        assertEquals("howtorefund",
                EmbeddingMath.normalizeQuestion("How to refund?！"));
        assertEquals("如何退款", EmbeddingMath.normalizeQuestion("如何退款？"));
        // 标点/空白差异与大小写差异归一化后应为同一问题
        assertEquals(EmbeddingMath.normalizeQuestion("How To  Refund!"),
                EmbeddingMath.normalizeQuestion("how to refund"));
    }
}
