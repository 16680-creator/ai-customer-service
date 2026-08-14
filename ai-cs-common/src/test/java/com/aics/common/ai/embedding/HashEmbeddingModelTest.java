package com.aics.common.ai.embedding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 本地哈希 Embedding 单元测试
 */
class HashEmbeddingModelTest {

    private final HashEmbeddingModel embeddingModel = new HashEmbeddingModel();

    @Test
    @DisplayName("向量维度应为 768")
    void embed_shouldReturn768Dimensions() {
        float[] vector = embeddingModel.embed("无线蓝牙耳机");
        assertEquals(HashEmbeddingModel.DIMENSIONS, vector.length);
    }

    @Test
    @DisplayName("向量应已 L2 归一化")
    void embed_shouldBeNormalized() {
        float[] vector = embeddingModel.embed("高品质降噪蓝牙耳机");
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        assertEquals(1.0, sum, 1e-6);
    }

    @Test
    @DisplayName("相似文本的余弦相似度应高于不相关文本")
    void embed_similarTextsShouldHaveHigherSimilarity() {
        float[] a = embeddingModel.embed("无线蓝牙耳机 降噪");
        float[] b = embeddingModel.embed("蓝牙耳机 无线 降噪");
        float[] c = embeddingModel.embed("机械键盘 有线");

        double similarityAB = cosine(a, b);
        double similarityAC = cosine(a, c);

        assertTrue(similarityAB > similarityAC,
                "AB=" + similarityAB + " 应大于 AC=" + similarityAC);
    }

    @Test
    @DisplayName("embed 批量接口应返回相同数量向量")
    void embedList_shouldReturnSameSize() {
        List<float[]> vectors = embeddingModel.embed(List.of("耳机", "键盘", "鼠标"));
        assertEquals(3, vectors.size());
        for (float[] vector : vectors) {
            assertEquals(HashEmbeddingModel.DIMENSIONS, vector.length);
        }
    }

    @Test
    @DisplayName("空白文本应返回零向量")
    void embed_blankTextShouldReturnZeroVector() {
        float[] vector = embeddingModel.embed("   ");
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        assertEquals(0.0, sum, 1e-9);
    }

    private double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }
}
